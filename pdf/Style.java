package myfirstmodule.pdf;

import java.awt.Color;

/**
 * Style constants matching React widget's StyleSheet exactly (1:1 mapping)
 */
public class Style {
    // Page styles
    public static final Color PAGE_BACKGROUND = Color.WHITE;
    public static final float PAGE_PADDING = 20f;
    
    // Header styles - made very readable (same as footer)
    public static final float HEADER_FONT_SIZE = 28f;  // Large and prominent
    public static final float HEADER_MARGIN_BOTTOM = 20f;  // More spacing
    
    // Total norm styles (same size as footer)
    public static final float TOTAL_NORM_FONT_SIZE = 24f;  // Same as footer text
    public static final float TOTAL_NORM_MARGIN_BOTTOM = 30f;  // More spacing
    
    // Node container styles
    public static final float NODE_CONTAINER_PADDING_VERTICAL = 15f;  // Increased from 10 for better spacing
    public static final float NODE_CONTAINER_PADDING_HORIZONTAL = 8f;  // Increased from 5
    
    // Node box styles - made bigger (130% increase - 30% bigger)
    public static final Color NODE_BACKGROUND = Colors.hexToColor("#F6F8FF");
    public static final float NODE_PADDING = 10f;  // 8 * 1.3 = 10.4 ~10
    public static final float NODE_BORDER_RADIUS = 13f;  // 10 * 1.3 = 13
    public static final float NODE_WIDTH = 173f;  // 133 * 1.3 = 172.9 ~173
    public static final float NODE_HEIGHT = 180f;  // Further increased to safely fit long titles + norm
    public static final float NODE_BORDER_LEFT_WIDTH = 8f;  // 6 * 1.3 = 7.8 ~8
    
    // Node text styles - bigger fonts (120% more increase)
    public static final float NODE_TEXT_FONT_SIZE = 17f;  // 14 * 1.2 = 16.8 ~17
    public static final float NODE_TEXT_LINE_HEIGHT = 1.3f;  // Keep same
    
    // Node norm styles (120% more increase)
    public static final float NODE_NORM_FONT_SIZE = 17f;  // 14 * 1.2 = 16.8 ~17
    public static final float NODE_NORM_MARGIN_TOP = 6f;  // 5 * 1.2 = 6
    
    // Line styles
    public static final float HORIZONTAL_LINE_HEIGHT = 1f;
    public static final Color LINE_COLOR = Colors.hexToColor("#9b9999");
    public static final float HORIZONTAL_LINE_TOP = 12f;
    public static final float VERTICAL_LINE_WIDTH = 1f;
    
    // Children row styles
    public static final float CHILDREN_ROW_MARGIN_TOP = 30f;  // More vertical space between levels
    
    // Footer styles - made very readable
    public static final float FOOTER_MARGIN_TOP = 40f;  // Increased from 30
    public static final float FOOTER_PADDING_HORIZONTAL = 0f;  // No extra padding - align with title
    public static final float FOOTER_PADDING_VERTICAL = 20f;  // Increased from 15
    public static final float FOOTER_TEXT_FONT_SIZE = 22f;  // Slightly smaller than header
    public static final Color FOOTER_TEXT_COLOR = Colors.hexToColor("#333333");
    public static final float FOOTER_IMAGE_HEIGHT = 150f;  // Reasonable size for logo
    
    // Layout calculation constants (matching React widget exactly)
    public static final float NODE_WIDTH_EFFECTIVE = NODE_WIDTH + (NODE_CONTAINER_PADDING_HORIZONTAL * 2);
    public static final float NODE_HEIGHT_EFFECTIVE = NODE_HEIGHT + (NODE_CONTAINER_PADDING_VERTICAL * 2) + CHILDREN_ROW_MARGIN_TOP;
    
    // Page size constants - unlimited, will auto-fit
    public static final float MIN_PAGE_WIDTH = 1190f;
    public static final float MIN_PAGE_HEIGHT = 842f;
    public static final float MAX_PAGE_WIDTH = 20000f;  // Very large, effectively unlimited
    public static final float MAX_PAGE_HEIGHT = 20000f; // Very large, effectively unlimited
    public static final float PAGE_WIDTH_EXTRA = 500f;  // Increased for margins
    public static final float PAGE_HEIGHT_EXTRA = 500f; // Increased for margins
    
    // Font weights (Roboto variants)
    public static final String FONT_LIGHT = "Roboto-Light";
    public static final String FONT_REGULAR = "Roboto-Regular";
    public static final String FONT_MEDIUM = "Roboto-Medium";
    public static final String FONT_BOLD = "Roboto-Bold";
}


