package myfirstmodule.pdf;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Color mapping for TitleCode to border colors, matching React widget exactly
 */
public class Colors {
    private static final Map<String, Color> COLOR_BY_CODE = new HashMap<>();
    
    static {
        COLOR_BY_CODE.put("9", hexToColor("#004481"));
        COLOR_BY_CODE.put("12", hexToColor("#5BBEFF"));
        COLOR_BY_CODE.put("40", hexToColor("#5BBEFF"));
        COLOR_BY_CODE.put("13", hexToColor("#2DCCD3"));
        COLOR_BY_CODE.put("39", hexToColor("#2DCCD3"));
        COLOR_BY_CODE.put("11", hexToColor("#D8BE75"));
        COLOR_BY_CODE.put("38", hexToColor("#D8BE75"));
        COLOR_BY_CODE.put("1", hexToColor("#F7893B"));
        COLOR_BY_CODE.put("37", hexToColor("#F7893B"));
        COLOR_BY_CODE.put("4", hexToColor("#F7893B"));
    }
    
    public static Color getBorderColor(String titleCode) {
        if (titleCode == null || titleCode.isEmpty()) {
            return hexToColor("#dddddd");
        }
        return COLOR_BY_CODE.getOrDefault(titleCode, hexToColor("#dddddd"));
    }
    
    public static Color hexToColor(String hex) {
        hex = hex.replace("#", "");
        return new Color(
            Integer.valueOf(hex.substring(0, 2), 16),
            Integer.valueOf(hex.substring(2, 4), 16),
            Integer.valueOf(hex.substring(4, 6), 16)
        );
    }
}


