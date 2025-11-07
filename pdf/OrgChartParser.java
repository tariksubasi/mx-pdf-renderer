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
     * Parse JSON string and merge hierarchies (matching React widget's parseAndMergeOrgData)
     */
    public static Position parse(String jsonString) throws ParseException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        
        JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
        Object parsed = parser.parse(jsonString);
        
        List<Position> dataArray = new ArrayList<>();
        
        if (parsed instanceof JSONArray) {
            JSONArray arr = (JSONArray) parsed;
            for (Object obj : arr) {
                if (obj instanceof JSONObject) {
                    dataArray.add(parsePosition((JSONObject) obj));
                }
            }
        } else if (parsed instanceof JSONObject) {
            dataArray.add(parsePosition((JSONObject) parsed));
        }
        
        if (dataArray.isEmpty()) {
            return null;
        }
        
        Integer rootId = dataArray.get(0).getPositionID();
        return mergeHierarchies(dataArray, rootId);
    }
    
    /**
     * Parse a single Position object from JSON
     */
    private static Position parsePosition(JSONObject json) {
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
        
        if (json.containsKey("Positions")) {
            Object positions = json.get("Positions");
            if (positions instanceof JSONArray) {
                List<Position> children = new ArrayList<>();
                for (Object child : (JSONArray) positions) {
                    if (child instanceof JSONObject) {
                        children.add(parsePosition((JSONObject) child));
                    }
                }
                pos.setPositions(children);
            }
        }
        
        return pos;
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
     * Merge hierarchies (matching React widget's mergeHierarchies)
     */
    private static Position mergeHierarchies(List<Position> roleArrays, Integer positionId) {
        Map<Integer, Position> positionMap = new HashMap<>();
        
        // First pass: clone all positions and collect in map
        for (Position pos : roleArrays) {
            Position cloned = clonePosition(pos);
            
            if (positionMap.containsKey(pos.getPositionID())) {
                Position existing = positionMap.get(pos.getPositionID());
                existing.setPositions(mergePositions(existing.getPositions(), cloned.getPositions()));
                // Preserve a positive Norm if existing doesn't have it
                Integer existingNorm = existing.getNorm();
                Integer newNorm = cloned.getNorm();
                if ((existingNorm == null || existingNorm.intValue() <= 0) && newNorm != null && newNorm.intValue() > 0) {
                    existing.setNorm(newNorm);
                }
            } else {
                positionMap.put(pos.getPositionID(), cloned);
            }
        }
        
        // Second pass: update children references
        for (Map.Entry<Integer, Position> entry : positionMap.entrySet()) {
            Position posObj = entry.getValue();
            List<Position> updated = new ArrayList<>();
            for (Position child : posObj.getPositions()) {
                Position mappedChild = positionMap.get(child.getPositionID());
                updated.add(mappedChild != null ? mappedChild : child);
            }
            posObj.setPositions(updated);
        }
        
        Position root = positionMap.get(positionId);
        return root != null ? root : (!roleArrays.isEmpty() ? roleArrays.get(0) : null);
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

