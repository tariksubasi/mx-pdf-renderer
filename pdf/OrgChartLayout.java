package myfirstmodule.pdf;

import java.util.HashMap;
import java.util.Map;

/**
 * Layout calculator matching React widget's calculation logic exactly
 */
public class OrgChartLayout {
    
    /**
     * Page dimensions
     */
    public static class PageSize {
        public final float width;
        public final float height;
        
        public PageSize(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Node position and dimensions
     */
    public static class NodeLayout {
        public float x;
        public float y;
        public float width;
        public float height;
        public int level;
        
        public NodeLayout(float x, float y, float width, float height, int level) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.level = level;
        }
    }
    
    /**
     * Calculate page size (matching React widget's calculatePageSize)
     */
    public static PageSize calculatePageSize(Position data) {
        if (data == null) {
            return new PageSize(Style.MIN_PAGE_WIDTH, Style.MIN_PAGE_HEIGHT);
        }
        
        Map<Integer, Integer> widthByLevel = new HashMap<>();
        calculateMaxWidth(data, 0, widthByLevel);
        
        int maxWidth = 0;
        for (Integer count : widthByLevel.values()) {
            if (count > maxWidth) {
                maxWidth = count;
            }
        }
        
        int depth = calculateDepth(data);
        
        // Each node: 85px width + 6px padding (3px each side)
        float nodeWidthEff = Style.NODE_WIDTH_EFFECTIVE;
        // Each level: 60px height + 16px padding + 12px spacing  
        float nodeHeightEff = Style.NODE_HEIGHT_EFFECTIVE;
        
        // More generous calculation to ensure everything fits
        // Add extra padding multiplier for safety
        float calculatedWidth = Math.max(Style.MIN_PAGE_WIDTH, maxWidth * nodeWidthEff * 1.2f + Style.PAGE_WIDTH_EXTRA);
        float calculatedHeight = Math.max(Style.MIN_PAGE_HEIGHT, depth * nodeHeightEff * 1.3f + Style.PAGE_HEIGHT_EXTRA);
        
        // Apply max limits
        float finalWidth = Math.min(calculatedWidth, Style.MAX_PAGE_WIDTH);
        float finalHeight = Math.min(calculatedHeight, Style.MAX_PAGE_HEIGHT);
        
        return new PageSize(finalWidth, finalHeight);
    }
    
    /**
     * Calculate maximum width by level (matching React widget's calculateMaxWidth)
     */
    private static void calculateMaxWidth(Position node, int level, Map<Integer, Integer> widthByLevel) {
        if (node == null) {
            return;
        }
        
        widthByLevel.put(level, widthByLevel.getOrDefault(level, 0) + 1);
        
        if (node.getPositions() != null && !node.getPositions().isEmpty()) {
            for (Position child : node.getPositions()) {
                calculateMaxWidth(child, level + 1, widthByLevel);
            }
        }
    }
    
    /**
     * Calculate tree depth (matching React widget's calculateDepth)
     */
    private static int calculateDepth(Position node) {
        if (node == null || node.getPositions() == null || node.getPositions().isEmpty()) {
            return 1;
        }
        
        int maxChildDepth = 0;
        for (Position child : node.getPositions()) {
            int childDepth = calculateDepth(child);
            if (childDepth > maxChildDepth) {
                maxChildDepth = childDepth;
            }
        }
        
        return 1 + maxChildDepth;
    }
    
    /**
     * Calculate subtree width (number of leaf nodes in this subtree)
     */
    public static int calculateSubtreeWidth(Position node) {
        if (node == null) {
            return 0;
        }
        
        if (node.getPositions() == null || node.getPositions().isEmpty()) {
            return 1;
        }
        
        int total = 0;
        for (Position child : node.getPositions()) {
            total += calculateSubtreeWidth(child);
        }
        
        return total;
    }
    
    /**
     * Layout positions for all nodes in the tree
     */
    public static Map<Position, NodeLayout> layoutTree(Position root, float startX, float startY) {
        Map<Position, NodeLayout> layouts = new HashMap<>();
        
        if (root == null) {
            return layouts;
        }
        
        layoutNode(root, startX, startY, 0, layouts);
        return layouts;
    }
    
    /**
     * Recursively layout a node and its children
     */
    private static float layoutNode(Position node, float x, float y, int level, Map<Position, NodeLayout> layouts) {
        if (node == null) {
            return 0;
        }
        
        float nodeWidth = Style.NODE_WIDTH;
        float nodeHeight = Style.NODE_HEIGHT;
        
        if (node.getPositions() == null || node.getPositions().isEmpty()) {
            // Leaf node - just place it
            layouts.put(node, new NodeLayout(x, y, nodeWidth, nodeHeight, level));
            return Style.NODE_WIDTH_EFFECTIVE;
        }
        
        // Layout children first to determine their total width
        // Y decreases as we go down (PDF coordinates - bottom-left origin)
        float childY = y - nodeHeight - Style.NODE_CONTAINER_PADDING_VERTICAL * 2 - Style.CHILDREN_ROW_MARGIN_TOP;
        
        // Special case: single child should be centered under parent
        if (node.getPositions().size() == 1) {
            Position child = node.getPositions().get(0);
            
            // Place parent first
            layouts.put(node, new NodeLayout(x, y, nodeWidth, nodeHeight, level));
            
            // Layout child centered under parent (same X position)
            layoutNode(child, x, childY, level + 1, layouts);
            
            return Style.NODE_WIDTH_EFFECTIVE;
        }
        
        // Multiple children - spread them out
        float childX = x;
        float totalChildrenWidth = 0;
        
        for (Position child : node.getPositions()) {
            float childWidth = layoutNode(child, childX, childY, level + 1, layouts);
            childX += childWidth;
            totalChildrenWidth += childWidth;
        }
        
        // Center parent over children by aligning to the midpoint between first and last child centers
        Position firstChild = node.getPositions().get(0);
        Position lastChild = node.getPositions().get(node.getPositions().size() - 1);
        NodeLayout firstLayout = layouts.get(firstChild);
        NodeLayout lastLayout = layouts.get(lastChild);
        float firstCenterX = firstLayout != null ? firstLayout.x + firstLayout.width / 2 : x + nodeWidth / 2;
        float lastCenterX = lastLayout != null ? lastLayout.x + lastLayout.width / 2 : x + totalChildrenWidth - nodeWidth / 2;
        float groupCenterX = (firstCenterX + lastCenterX) / 2f;
        float parentX = groupCenterX - nodeWidth / 2f;
        layouts.put(node, new NodeLayout(parentX, y, nodeWidth, nodeHeight, level));
        
        return totalChildrenWidth;
    }
}


