package myfirstmodule.pdf;

import com.mendix.core.Core;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSDictionary;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * PDF renderer using PDFBox, matching React widget's visual output exactly
 */
public class OrgChartRenderer {
    
    // Structure to hold calculated layout and bounds
    private static class LayoutResult {
        final Map<Position, OrgChartLayout.NodeLayout> layouts;
        final float minX, maxX, minY, maxY;
        final float width, height;

        LayoutResult(Map<Position, OrgChartLayout.NodeLayout> layouts) {
            this.layouts = layouts;
            
            float minX = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float minY = Float.MAX_VALUE;
            float maxY = Float.MIN_VALUE;

            if (layouts == null || layouts.isEmpty()) {
                minX = maxX = minY = maxY = 0;
            } else {
                for (OrgChartLayout.NodeLayout layout : layouts.values()) {
                    if (layout.x < minX) minX = layout.x;
                    if (layout.x + layout.width > maxX) maxX = layout.x + layout.width;
                    if (layout.y - layout.height < minY) minY = layout.y - layout.height;
                    if (layout.y > maxY) maxY = layout.y;
                }
            }
            
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.width = maxX - minX;
            this.height = maxY - minY;
        }
    }

    private PDDocument document;
    private PDFont fontRegular;
    private PDFont fontBold;
    
    public OrgChartRenderer() throws Exception {
        this.document = new PDDocument();
        // PDFBox 3.x - use Helvetica (closest to Roboto/Benton in PDF standard fonts)
        this.fontRegular = createFont("Helvetica");
        this.fontBold = createFont("Helvetica-Bold");
    }
    
    /**
     * Create a Type1 font using COSDictionary (PDFBox 3.x way)
     */
    private PDFont createFont(String baseFontName) throws Exception {
        COSDictionary fontDict = new COSDictionary();
        fontDict.setItem(COSName.TYPE, COSName.FONT);
        fontDict.setItem(COSName.SUBTYPE, COSName.TYPE1);
        fontDict.setItem(COSName.BASE_FONT, COSName.getPDFName(baseFontName));
        return new PDType1Font(fontDict);
    }
    
    /**
     * Get regular font
     */
    private PDFont getRegularFont(PDPage page) {
        return fontRegular;
    }
    
    /**
     * Get bold font
     */
    private PDFont getBoldFont(PDPage page) {
        return fontBold;
    }
    
    /**
     * Render complete PDF with TR and optional EN pages
     */
    public byte[] render(
            Position dataTR, String titleTR, String totalNormLabelTR,
            String footerPreparedByTR, String footerDocDateTR, String footerLastUpdateTR,
            String footerImageUrlTR,
            Position dataEN, String titleEN, String totalNormLabelEN,
            String footerPreparedByEN, String footerDocDateEN, String footerLastUpdateEN,
            String footerImageUrlEN
    ) throws Exception {
        
        // Render Turkish page
        if (dataTR != null) {
            // 1. First, perform layout calculation
            Map<Position, OrgChartLayout.NodeLayout> layoutsTR = OrgChartLayout.layoutTree(dataTR, 0, 0);
            LayoutResult layoutResultTR = new LayoutResult(layoutsTR);

            // 2. Then, render the page using the calculated layout
            renderPage(dataTR, layoutResultTR, titleTR, totalNormLabelTR,
                    footerPreparedByTR, footerDocDateTR, footerLastUpdateTR,
                    footerImageUrlTR);
        }
        
        // Render English page
        if (dataEN != null) {
            // 1. First, perform layout calculation
            Map<Position, OrgChartLayout.NodeLayout> layoutsEN = OrgChartLayout.layoutTree(dataEN, 0, 0);
            LayoutResult layoutResultEN = new LayoutResult(layoutsEN);

            // 2. Then, render the page using the calculated layout
            renderPage(dataEN, layoutResultEN, titleEN, totalNormLabelEN,
                    footerPreparedByEN, footerDocDateEN, footerLastUpdateEN,
                    footerImageUrlEN);
        }
        
        // Save to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();
        
        return baos.toByteArray();
    }
    
    /**
     * Render a single page (TR or EN)
     */
    private void renderPage(
            Position data, LayoutResult layoutResult, String title, String totalNormLabel,
            String footerPreparedBy, String footerDocDate, String footerLastUpdate,
            String footerImageUrl
    ) throws Exception {
        
        // Calculate dynamic page size based on content
        float headerHeight = Style.HEADER_FONT_SIZE + Style.HEADER_MARGIN_BOTTOM + 
                             Style.TOTAL_NORM_FONT_SIZE + Style.TOTAL_NORM_MARGIN_BOTTOM;
        float footerHeight = Style.FOOTER_IMAGE_HEIGHT + Style.FOOTER_MARGIN_TOP;
        
        float requiredWidth = layoutResult.width + (Style.PAGE_PADDING * 2) + (Style.PAGE_EXTRA_HORIZONTAL_PADDING * 2);
        float requiredHeight = layoutResult.height + headerHeight + footerHeight + (Style.PAGE_PADDING * 2);

        float pageWidth = Math.max(Style.MIN_PAGE_WIDTH, requiredWidth);
        float pageHeight = Math.max(Style.MIN_PAGE_HEIGHT, requiredHeight);
        
        // Create page with custom size
        PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
        document.addPage(page);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        // Draw white background
        contentStream.setNonStrokingColor(Style.PAGE_BACKGROUND);
        contentStream.addRect(0, 0, pageWidth, pageHeight);
        contentStream.fill();
        
        float currentY = pageHeight - Style.PAGE_PADDING;
        
        // Draw header
        currentY = drawHeader(contentStream, title, currentY, pageWidth, page);
        
        // Draw total norm label if provided (label already contains the number)
        if (totalNormLabel != null && !totalNormLabel.trim().isEmpty()) {
            currentY = drawTotalNorm(contentStream, totalNormLabel, currentY, page);
        }
        
        // Layout and draw organization tree
        drawOrganizationTree(contentStream, data, layoutResult, currentY, pageWidth);
        
        // Draw footer
        drawFooter(contentStream, footerPreparedBy, footerDocDate, footerLastUpdate,
                footerImageUrl, pageWidth, pageHeight);
        
        contentStream.close();
    }
    
    /**
     * Draw header text
     */
    private float drawHeader(PDPageContentStream contentStream, String title, float y, float pageWidth, PDPage page) throws Exception {
        if (title == null || title.isEmpty()) {
            title = "Organization Chart";
        }
        
        // Sanitize text for Courier font (remove Turkish characters)
        title = sanitizeText(title);
        
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.beginText();
        contentStream.setFont(getBoldFont(page), Style.HEADER_FONT_SIZE);
        contentStream.newLineAtOffset(Style.PAGE_PADDING, y - Style.HEADER_FONT_SIZE);
        contentStream.showText(title);
        contentStream.endText();
        
        return y - Style.HEADER_FONT_SIZE - Style.HEADER_MARGIN_BOTTOM;
    }
    
    /**
     * Sanitize text to remove Turkish/special characters that Courier doesn't support
     */
    private String sanitizeText(String text) {
        if (text == null) return "";
        return text
            // Replace newline characters with a space to prevent font errors
            .replaceAll("[\\r\\n]+", " ")
            .replace("ş", "s").replace("Ş", "S")
            .replace("ğ", "g").replace("Ğ", "G")
            .replace("ü", "u").replace("Ü", "U")
            .replace("ö", "o").replace("Ö", "O")
            .replace("ç", "c").replace("Ç", "C")
            .replace("ı", "i").replace("İ", "I")
            .replaceAll("[^\\x00-\\x7F]", "?"); // Replace any other non-ASCII with ?
    }
    
    /**
     * Draw a rounded rectangle with proper bezier curves
     */
    private void drawRoundedRect(PDPageContentStream contentStream, float x, float y, float width, float height, float radius) throws Exception {
        // Ensure radius doesn't exceed half of width or height
        float r = Math.min(radius, Math.min(width / 2, height / 2));
        
        // Magic number for circle approximation with bezier curves
        float k = 0.552284749831f;
        
        // Start from bottom-left, going clockwise
        contentStream.moveTo(x + r, y);
        
        // Bottom edge
        contentStream.lineTo(x + width - r, y);
        
        // Bottom-right corner
        contentStream.curveTo(
            x + width - r + (k * r), y,
            x + width, y + r - (k * r),
            x + width, y + r
        );
        
        // Right edge
        contentStream.lineTo(x + width, y + height - r);
        
        // Top-right corner
        contentStream.curveTo(
            x + width, y + height - r + (k * r),
            x + width - r + (k * r), y + height,
            x + width - r, y + height
        );
        
        // Top edge
        contentStream.lineTo(x + r, y + height);
        
        // Top-left corner
        contentStream.curveTo(
            x + r - (k * r), y + height,
            x, y + height - r + (k * r),
            x, y + height - r
        );
        
        // Left edge
        contentStream.lineTo(x, y + r);
        
        // Bottom-left corner
        contentStream.curveTo(
            x, y + r - (k * r),
            x + r - (k * r), y,
            x + r, y
        );
        
        contentStream.fill();
    }
    
    /**
     * Draw left border stripe with rounded corners on left side only
     */
    private void drawLeftBorderStripe(PDPageContentStream contentStream, float x, float y, float width, float height, float radius) throws Exception {
        // Ensure radius doesn't exceed dimensions
        float r = Math.min(radius, Math.min(width, height / 2));
        
        // Magic number for circle approximation
        float k = 0.552284749831f;
        
        // Draw left border with rounded left corners, straight right edge
        // Start from bottom-left rounded corner
        contentStream.moveTo(x + r, y);
        
        // Bottom edge of stripe
        contentStream.lineTo(x + width, y);
        
        // Straight right edge going up
        contentStream.lineTo(x + width, y + height);
        
        // Top edge of stripe
        contentStream.lineTo(x + r, y + height);
        
        // Top-left rounded corner
        contentStream.curveTo(
            x + r - (k * r), y + height,
            x, y + height - r + (k * r),
            x, y + height - r
        );
        
        // Left edge
        contentStream.lineTo(x, y + r);
        
        // Bottom-left rounded corner
        contentStream.curveTo(
            x, y + r - (k * r),
            x + r - (k * r), y,
            x + r, y
        );
        
        contentStream.fill();
    }
    
    /**
     * Draw total norm text (label already contains the number)
     */
    private float drawTotalNorm(PDPageContentStream contentStream, String label, float y, PDPage page) throws Exception {
        String text = sanitizeText(label);
        
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.beginText();
        contentStream.setFont(getRegularFont(page), Style.TOTAL_NORM_FONT_SIZE);
        contentStream.newLineAtOffset(Style.PAGE_PADDING, y - Style.TOTAL_NORM_FONT_SIZE);
        contentStream.showText(text);
        contentStream.endText();
        
        return y - Style.TOTAL_NORM_FONT_SIZE;
    }
    
    /**
     * Draw organization tree
     */
    private void drawOrganizationTree(PDPageContentStream contentStream, Position root, LayoutResult layoutResult, float startY, float pageWidth) throws Exception {
        if (root == null || layoutResult == null || layoutResult.layouts.isEmpty()) {
            return;
        }

        // The layout is already calculated, we just need to position it correctly on the page.
        Map<Position, OrgChartLayout.NodeLayout> layouts = layoutResult.layouts;

        // The offset needed to center the tree horizontally.
        float offsetX = (pageWidth - layoutResult.width) / 2f - layoutResult.minX;
        
        // The offset needed to place the tree correctly vertically after the header.
        float offsetY = startY - layoutResult.maxY;

        // We need a new map for the final, adjusted layouts.
        Map<Position, OrgChartLayout.NodeLayout> finalLayouts = new java.util.HashMap<>();
        for(Map.Entry<Position, OrgChartLayout.NodeLayout> entry : layouts.entrySet()) {
            OrgChartLayout.NodeLayout original = entry.getValue();
            // Create a new layout object with adjusted coordinates
            finalLayouts.put(entry.getKey(), new OrgChartLayout.NodeLayout(
                original.x + offsetX,
                original.y + offsetY,
                original.width,
                original.height,
                original.level
            ));
        }

        // Draw connecting lines first (so they appear behind nodes)
        drawConnectingLines(contentStream, root, finalLayouts);

        // Draw nodes on top
        for (Map.Entry<Position, OrgChartLayout.NodeLayout> entry : finalLayouts.entrySet()) {
            drawNode(contentStream, entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Draw connecting lines between parent and children
     */
    private void drawConnectingLines(PDPageContentStream contentStream, Position node, Map<Position, OrgChartLayout.NodeLayout> layouts) throws Exception {
        if (node == null || node.getPositions() == null || node.getPositions().isEmpty()) {
            return;
        }
        
        OrgChartLayout.NodeLayout nodeLayout = layouts.get(node);
        if (nodeLayout == null) {
            return;
        }
        
        List<Position> children = node.getPositions();
        
        float parentCenterX = nodeLayout.x + nodeLayout.width / 2;
        float parentBottomY = nodeLayout.y - nodeLayout.height;
        
        // Use thicker, more corporate-looking lines
        contentStream.setStrokingColor(Style.LINE_COLOR);
        contentStream.setLineWidth(2.0f);
        contentStream.setLineDashPattern(new float[]{}, 0);
        
        if (children.size() == 1) {
            // Single child - draw one straight line from parent to child
            OrgChartLayout.NodeLayout childLayout = layouts.get(children.get(0));
            if (childLayout != null) {
                float childCenterX = childLayout.x + childLayout.width / 2;
                float childTopY = childLayout.y;
                
                contentStream.moveTo(parentCenterX, parentBottomY);
                contentStream.lineTo(childCenterX, childTopY);
                contentStream.stroke();
            }
            
            // Recursively draw lines for this child
            drawConnectingLines(contentStream, children.get(0), layouts);
            
        } else {
            // Multiple children - draw T-shape connection
            float verticalLineEndY = parentBottomY - Style.CHILDREN_ROW_MARGIN_TOP;
            
            // Vertical line from parent down
            contentStream.moveTo(parentCenterX, parentBottomY);
            contentStream.lineTo(parentCenterX, verticalLineEndY);
            contentStream.stroke();
            
            // Horizontal line connecting all children
            OrgChartLayout.NodeLayout firstChildLayout = layouts.get(children.get(0));
            OrgChartLayout.NodeLayout lastChildLayout = layouts.get(children.get(children.size() - 1));
            
            if (firstChildLayout != null && lastChildLayout != null) {
                float firstChildCenterX = firstChildLayout.x + firstChildLayout.width / 2;
                float lastChildCenterX = lastChildLayout.x + lastChildLayout.width / 2;
                
                contentStream.moveTo(firstChildCenterX, verticalLineEndY);
                contentStream.lineTo(lastChildCenterX, verticalLineEndY);
                contentStream.stroke();
            }
            
            // Vertical lines down to each child
            for (Position child : children) {
                OrgChartLayout.NodeLayout childLayout = layouts.get(child);
                if (childLayout != null) {
                    float childCenterX = childLayout.x + childLayout.width / 2;
                    float childTopY = childLayout.y;
                    
                    contentStream.moveTo(childCenterX, verticalLineEndY);
                    contentStream.lineTo(childCenterX, childTopY);
                    contentStream.stroke();
                }
                
                // Recursively draw lines for this child
                drawConnectingLines(contentStream, child, layouts);
            }
        }
    }
    
    /**
     * Draw a single node box with text
     */
    private void drawNode(PDPageContentStream contentStream, Position position, OrgChartLayout.NodeLayout layout) throws Exception {
        // Get border color based on TitleCode
        Color borderColor = Colors.getBorderColor(position.getTitleCode());
        
        // Draw node background with rounded corners (approximated)
        contentStream.setNonStrokingColor(Style.NODE_BACKGROUND);
        float x = layout.x;
        float y = layout.y;
        float w = layout.width;
        float h = layout.height;
        
        // Draw rounded rectangle background
        contentStream.setNonStrokingColor(Style.NODE_BACKGROUND);
        drawRoundedRect(contentStream, x, y - h, w, h, Style.NODE_BORDER_RADIUS);
        
        // Draw left border stripe with rounded corners
        contentStream.setNonStrokingColor(borderColor);
        drawLeftBorderStripe(contentStream, x, y - h, Style.NODE_BORDER_LEFT_WIDTH, h, Style.NODE_BORDER_RADIUS);
        
        // Draw position name (centered, possibly multi-line)  
        String positionName = position.getPositionName() != null ? position.getPositionName() : "";
        positionName = sanitizeText(positionName);
        
        // If norm is 0, center the text vertically, otherwise leave room for norm at bottom
        // Explicitly parse and convert to int for proper integer comparison
        Integer normValue = position.getNorm();
        int norm = (normValue != null) ? normValue.intValue() : 0;
        Core.getLogger("OrgChartRenderer").info("Rendering node '" + positionName + "'. Norm value from position: " + normValue + ", Converted to int: " + norm + ", Will display: " + (norm > 0));
        if (norm > 0) {
            // Reserve bottom area for norm (padding + margin + font height)
            float reservedBottom = Style.NODE_PADDING + Style.NODE_NORM_MARGIN_TOP + Style.NODE_NORM_FONT_SIZE + Style.NODE_PADDING;
            float textAreaHeight = Math.max(h - reservedBottom, Style.NODE_TEXT_FONT_SIZE * 1.3f);
            // Center name inside the top text area (from y down to y - textAreaHeight)
            drawCenteredText(contentStream, positionName, x, y, w, textAreaHeight, fontRegular, Style.NODE_TEXT_FONT_SIZE, Color.BLACK, true);

            // Draw norm (centered, at bottom) with margin above
            String normText = String.valueOf(norm);
            float normY = y - h + Style.NODE_PADDING + Style.NODE_NORM_MARGIN_TOP + Style.NODE_NORM_FONT_SIZE;
            drawCenteredText(contentStream, normText, x, normY, w, 0, fontRegular, Style.NODE_NORM_FONT_SIZE, Color.BLACK, false);
        } else {
            // No norm - center position name vertically in the whole box
            // The 'y' coordinate is the top of the node, and 'h' is its full height
            drawCenteredText(contentStream, positionName, x, y, w, h, fontRegular, Style.NODE_TEXT_FONT_SIZE, Color.BLACK, true);
        }
    }
    
    /**
     * Draw centered text (with word wrapping for multi-line)
     */
    private void drawCenteredText(PDPageContentStream contentStream, String text, float x, float y, float w, float h, PDFont font, float fontSize, Color color, boolean multiline) throws Exception {
        contentStream.setNonStrokingColor(color);
        
        if (multiline) {
            // Simple word wrapping
            List<String> lines = wrapText(text, font, fontSize, w - Style.NODE_PADDING * 2);
            float lineHeight = fontSize * Style.NODE_TEXT_LINE_HEIGHT;
            // Max number of lines that can fit in the given height
            int maxLines = Math.max(1, (int) Math.floor(h / lineHeight));
            if (lines.size() > maxLines) {
                lines = new ArrayList<>(lines.subList(0, maxLines));
            }
            float totalHeight = lines.size() * lineHeight;
            float startY = y - (h - totalHeight) / 2 - fontSize;
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                float textWidth = font.getStringWidth(line) / 1000 * fontSize;
                float textX = x + (w - textWidth) / 2;
                float textY = startY - i * lineHeight;
                
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(textX, textY);
                contentStream.showText(line);
                contentStream.endText();
            }
        } else {
            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            float textX = x + (w - textWidth) / 2;
            
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(textX, y);
            contentStream.showText(text);
            contentStream.endText();
        }
    }
    
    /**
     * Wrap text to fit within maxWidth - NO HYPHENATION
     */
    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws Exception {
        List<String> lines = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return lines;
        }
        
        // Split by spaces only - no hyphenation
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;
            
            // If word is too long for a single line, just put it anyway (no hyphenation)
            if (testWidth > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * Draw footer with text and image - both at bottom, same baseline
     */
    private void drawFooter(PDPageContentStream contentStream, String preparedBy, String docDate, String lastUpdate, String imageUrl, float pageWidth, float pageHeight) throws Exception {
        // --- REFACTORED FOOTER LOGIC ---

        // 1. Prepare footer content and calculate required heights
        String[] footerLines = {
            preparedBy != null ? preparedBy : "",
            docDate != null ? docDate : "",
            lastUpdate != null ? lastUpdate : ""
        };

        int nonEmptyLines = 0;
        for (String l : footerLines) {
            if (l != null && !l.isEmpty()) {
                nonEmptyLines++;
            }
        }

        float textBlockHeight = (nonEmptyLines > 0)
            ? ((nonEmptyLines - 1) * Style.FOOTER_TEXT_FONT_SIZE * 1.4f) + Style.FOOTER_TEXT_FONT_SIZE
            : 0f;
        float imageHeight = Style.FOOTER_IMAGE_HEIGHT;
        float contentHeight = Math.max(textBlockHeight, imageHeight);
        float footerBottomMargin = 10f; // Moves the entire footer block closer to the bottom edge.

        // 2. Determine the Y position for the top border and draw it first
        float borderY = footerBottomMargin + contentHeight + 15f; // 15f padding between content and border
        contentStream.setStrokingColor(new Color(0xDD, 0xDD, 0xDD));
        contentStream.setLineWidth(1f);
        contentStream.moveTo(Style.PAGE_PADDING, borderY);
        contentStream.lineTo(pageWidth - Style.PAGE_PADDING, borderY);
        contentStream.stroke();

        // 3. Position the content block below the border
        float contentBottomY = borderY - 15f - contentHeight;

        // 4. Draw the footer image (right side) from Base64 string
        try {
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                String base64Image = imageUrl;
                // Remove data URI prefix if present
                if (base64Image.contains(",")) {
                    base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                }
                
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "footer-image");
                
                float calculatedImageWidth = image.getWidth() * (imageHeight / image.getHeight());
                float imageX = pageWidth - Style.PAGE_PADDING - calculatedImageWidth;
                
                // Align image bottom to the content area bottom
                contentStream.drawImage(image, imageX, contentBottomY, calculatedImageWidth, imageHeight);
            } else {
                 throw new Exception("Image URL (Base64) is empty.");
            }
        } catch (Exception e) {
            Core.getLogger("OrgChartRenderer").warn("Failed to decode or draw Base64 image: " + e.getMessage());
            contentStream.setNonStrokingColor(Color.GRAY);
            contentStream.beginText();
            contentStream.setFont(fontRegular, 12);
            contentStream.newLineAtOffset(pageWidth - 150, contentBottomY + 20);
            contentStream.showText("[Logo]");
            contentStream.endText();
        }
        
        // 5. Draw the footer text (left side), stacking upwards from the bottom
        contentStream.setNonStrokingColor(Style.FOOTER_TEXT_COLOR);
        float textY = contentBottomY + Style.FOOTER_TEXT_FONT_SIZE;
        // Draw lines in reverse order to stack them from the bottom up
        for (int i = footerLines.length - 1; i >= 0; i--) {
            String line = footerLines[i];
            if (!line.isEmpty()) {
                contentStream.beginText();
                contentStream.setFont(fontRegular, Style.FOOTER_TEXT_FONT_SIZE);
                contentStream.newLineAtOffset(Style.PAGE_PADDING, textY);
                contentStream.showText(sanitizeText(line));
                contentStream.endText();
                textY += Style.FOOTER_TEXT_FONT_SIZE * 1.4f;
            }
        }
    }
}

