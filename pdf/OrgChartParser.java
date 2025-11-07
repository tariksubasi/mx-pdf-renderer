package myfirstmodule.pdf;

import com.mendix.core.Core;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.util.*;

/**
 * JSON parser with merge logic matching React widget exactly
 */
public class OrgChartParser {
    
    /**
     * Parse JSON string - handles flat array structure where each Position has direct children in Positions array
     * (not recursive nested structure)
     */
    public static Position parse(String jsonString) throws ParseException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        
        JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
        Object parsed = parser.parse(jsonString);
        
        List<Position> allPositions = new ArrayList<>();
        
        // Parse all positions from the flat array (not recursive)
        if (parsed instanceof JSONArray) {
            JSONArray arr = (JSONArray) parsed;
            for (Object obj : arr) {
                if (obj instanceof JSONObject) {
                    allPositions.add(parsePositionFlat((JSONObject) obj));
                }
            }
        } else if (parsed instanceof JSONObject) {
            allPositions.add(parsePositionFlat((JSONObject) parsed));
        }
        
        if (allPositions.isEmpty()) {
            return null;
        }
        
        // Build tree structure using HierarchyCode relationships
        return buildTreeFromFlatList(allPositions);
    }
    
    /**
     * Parse a single Position object from JSON (flat structure - not recursive)
     * Positions array contains only direct children, not nested recursively
     */
    private static Position parsePositionFlat(JSONObject json) {
        Position pos = new Position();
        
        if (json.containsKey("PositionID")) {
            Object id = json.get("PositionID");
            pos.setPositionID(id instanceof Number ? ((Number) id).intValue() : null);
        }
        
        if (json.containsKey("PositionName")) {
            pos.setPositionName(json.getAsString("PositionName"));
        }
        
        if (json.containsKey("HierarchyCode")) {
            pos.setHierarchyCode(json.getAsString("HierarchyCode"));
        }
        
        if (json.containsKey("ParentHierarchyCode")) {
            pos.setParentHierarchyCode(json.getAsString("ParentHierarchyCode"));
        }
        
        if (json.containsKey("Norm")) {
            Object norm = json.get("Norm");
            // Explicitly parse to integer - handle Number, String, or null
            Integer normValue = null;
            if (norm instanceof Number) {
                int parsed = ((Number) norm).intValue();
                normValue = (parsed > 0) ? parsed : null; // Only store if > 0
            } else if (norm instanceof String) {
                try {
                    String trimmed = ((String) norm).trim();
                    if (!trimmed.isEmpty()) {
                        int parsed = Integer.parseInt(trimmed);
                        normValue = (parsed > 0) ? parsed : null; // Only store if > 0
                    }
                } catch (NumberFormatException e) {
                    normValue = null; // Invalid string, set to null
                    Core.getLogger("OrgChartParser").warn("Failed to parse Norm value '" + norm + "' for position '" + pos.getPositionName() + "': " + e.getMessage());
                }
            }
            pos.setNorm(normValue);
            Core.getLogger("OrgChartParser").info("Parsing Norm for position '" + pos.getPositionName() + "'. Raw value: '" + norm + "', Parsed value: " + normValue);
        } else {
             Core.getLogger("OrgChartParser").info("No 'Norm' key found for position '" + pos.getPositionName() + "'.");
        }
        
        if (json.containsKey("TitleCode")) {
            Object tc = json.get("TitleCode");
            pos.setTitleCode(tc != null ? tc.toString() : null);
        }
        
        // Parse direct children from Positions array (not recursive - just one level)
        if (json.containsKey("Positions")) {
            Object positions = json.get("Positions");
            if (positions instanceof JSONArray) {
                List<Position> children = new ArrayList<>();
                for (Object child : (JSONArray) positions) {
                    if (child instanceof JSONObject) {
                        // Parse child but don't recursively parse its Positions - we'll build tree from flat list
                        Position childPos = parsePositionFlat((JSONObject) child);
                        // Clear child's Positions - we'll rebuild tree structure later
                        childPos.setPositions(new ArrayList<>());
                        children.add(childPos);
                    }
                }
                pos.setPositions(children);
            }
        }
        
        return pos;
    }
    
    /**
     * Build tree structure from flat list of positions using HierarchyCode relationships
     */
    private static Position buildTreeFromFlatList(List<Position> allPositions) {
        if (allPositions == null || allPositions.isEmpty()) {
            return null;
        }
        
        // Step 1: Merge positions with same PositionID and collect all unique positions
        Map<Integer, Position> positionMap = new HashMap<>();
        Map<String, List<Position>> positionsByHierarchyCode = new HashMap<>();
        
        for (Position pos : allPositions) {
            Integer posId = pos.getPositionID();
            String hierarchyCode = pos.getHierarchyCode();
            
            if (posId != null) {
                if (positionMap.containsKey(posId)) {
                    // Merge with existing position
                    Position existing = positionMap.get(posId);
                    // Merge children from Positions array
                    existing.setPositions(mergePositions(existing.getPositions(), pos.getPositions()));
                    // Preserve positive Norm if existing doesn't have it
                    Integer existingNorm = existing.getNorm();
                    Integer newNorm = pos.getNorm();
                    if ((existingNorm == null || existingNorm.intValue() <= 0) && newNorm != null && newNorm.intValue() > 0) {
                        existing.setNorm(newNorm);
                    }
                } else {
                    // New position - clone it
                    Position cloned = clonePosition(pos);
                    positionMap.put(posId, cloned);
                    
                    // Also index by HierarchyCode for building tree
                    if (hierarchyCode != null && !hierarchyCode.isEmpty()) {
                        positionsByHierarchyCode.computeIfAbsent(hierarchyCode, k -> new ArrayList<>()).add(cloned);
                    }
                }
            }
        }
        
        // Step 2: Build tree structure using HierarchyCode relationships
        // Clear all Positions lists first - we'll rebuild them
        for (Position pos : positionMap.values()) {
            pos.setPositions(new ArrayList<>());
        }
        
        // Build parent-child relationships
        for (Position pos : positionMap.values()) {
            String parentHierarchyCode = pos.getParentHierarchyCode();
            if (parentHierarchyCode != null && !parentHierarchyCode.isEmpty()) {
                // Find parent by matching HierarchyCode
                List<Position> potentialParents = positionsByHierarchyCode.get(parentHierarchyCode);
                if (potentialParents != null && !potentialParents.isEmpty()) {
                    // Use first parent found (should be unique by HierarchyCode)
                    Position parent = potentialParents.get(0);
                    parent.getPositions().add(pos);
                }
            }
        }
        
        // Step 3: Find root (position with no parent or shortest HierarchyCode)
        Position root = null;
        int shortestLength = Integer.MAX_VALUE;
        
        for (Position pos : positionMap.values()) {
            String parentHierarchyCode = pos.getParentHierarchyCode();
            String hierarchyCode = pos.getHierarchyCode();
            
            if (parentHierarchyCode == null || parentHierarchyCode.isEmpty() || 
                parentHierarchyCode.equals("/") || parentHierarchyCode.equals("")) {
                // This is a root candidate
                if (hierarchyCode != null) {
                    int length = hierarchyCode.split("/").length;
                    if (length < shortestLength) {
                        shortestLength = length;
                        root = pos;
                    }
                } else if (root == null) {
                    root = pos;
                }
            }
        }
        
        // If no root found by parent check, use position with shortest HierarchyCode
        if (root == null) {
            for (Position pos : positionMap.values()) {
                String hierarchyCode = pos.getHierarchyCode();
                if (hierarchyCode != null) {
                    int length = hierarchyCode.split("/").length;
                    if (length < shortestLength) {
                        shortestLength = length;
                        root = pos;
                    }
                }
            }
        }
        
        // Fallback: use first position if still no root found
        if (root == null && !positionMap.isEmpty()) {
            root = positionMap.values().iterator().next();
        }
        
        return root;
    }
    
    /**
     * Merge positions with same PositionID (matching React widget's mergePositions)
     */
    private static List<Position> mergePositions(List<Position> arr1, List<Position> arr2) {
        List<Position> merged = new ArrayList<>(arr1 != null ? arr1 : new ArrayList<>());
        Set<Integer> existingIds = new HashSet<>();
        
        for (Position p : merged) {
            existingIds.add(p.getPositionID());
        }
        
        if (arr2 != null) {
            for (Position item : arr2) {
                if (!existingIds.contains(item.getPositionID())) {
                    merged.add(item);
                }
            }
        }
        
        return merged;
    }
    
    /**
     * Clone a Position with its immediate children list
     */
    private static Position clonePosition(Position original) {
        Position cloned = new Position();
        cloned.setPositionID(original.getPositionID());
        cloned.setPositionName(original.getPositionName());
        cloned.setHierarchyCode(original.getHierarchyCode());
        cloned.setParentHierarchyCode(original.getParentHierarchyCode());
        cloned.setNorm(original.getNorm());
        cloned.setTitleCode(original.getTitleCode());
        cloned.setPositions(new ArrayList<>(original.getPositions()));
        return cloned;
    }
    
    /**
     * Calculate total norm (matching React widget's sumNorm)
     */
    public static int sumNorm(Position node) {
        if (node == null) {
            return 0;
        }
        
        // Explicitly convert to int for integer arithmetic
        Integer normValue = node.getNorm();
        int total = (normValue != null) ? normValue.intValue() : 0;
        for (Position child : node.getPositions()) {
            total += sumNorm(child);
        }
        
        return total;
    }
}

