import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

class CoverGenerator {

    private static final int FONT_SIZE = 90;
    private static final int COVER_WIDTH = 1600;
    private static final int COVER_HEIGHT = 2400;
    private static final int TITLE_WIDTH = 1200;
    private static final int TITLE_TOP = 200;
    private static final String DEFAULT_FONT = "Ximalaya";
    private static Font coverFont;
    private static FontMetrics fontMetrics;
    private BufferedImage image;
    private String outputFilePath;
    private static final Color backgroundColor = Color.WHITE;
    private static Color bottomColor = new Color(255, 153, 0);
    private static final int LOGO_TOP = 2200;
    private static final int LOGO_WIDTH = 200;
    private Image logoImage;

    public CoverGenerator(String fontPath, String fontName, String logoPath)
    {
        try {
            image = new BufferedImage(COVER_WIDTH, COVER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setPaint(backgroundColor);
            graphics.fillRect(0, 0, COVER_WIDTH, COVER_HEIGHT);

            if (coverFont == null) {
                coverFont = getFont(fontPath, fontName, Font.PLAIN, FONT_SIZE);
            }

            if (fontMetrics == null) {
                fontMetrics = graphics.getFontMetrics(coverFont);
            }

            graphics.dispose();

            logoImage = ImageIO.read(new File(logoPath));
            logoImage = logoImage.getScaledInstance(LOGO_WIDTH, -1, Image.SCALE_DEFAULT);
        } catch(Exception e) {
            System.out.printf("Exception generating cover: %s", e.toString());
        }
    }

    public void generateCover(String title, String outputFilePath)
    {
        title = splitTitle(title);
        this.outputFilePath = outputFilePath;
        List<String> titleLines = StringUtils.wrap(title, fontMetrics, TITLE_WIDTH);
        System.out.println("titleLines: " + titleLines);
        String titleHtml = String.join("<br/>", titleLines);
        JLabel label = new JLabel("<html><body style='width: " + TITLE_WIDTH + "px; padding: 0px; text-align: center'><p>" + titleHtml + "</p></body></html>");
        label.setFont(coverFont);
        Dimension labelSize = label.getPreferredSize();
        label.setSize(labelSize);
        BufferedImage titleImage = new BufferedImage(labelSize.width, labelSize.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D titleGraphics = titleImage.createGraphics();
        label.printAll(titleGraphics);
        titleGraphics.dispose();

        int titleMargins = (COVER_WIDTH - labelSize.width) / 2;

        Graphics2D coverGraphics = image.createGraphics();
        coverGraphics.drawImage(titleImage, titleMargins, TITLE_TOP, null);

        int bottomColourHeight = 800;
        BufferedImage bottomColourImage = new BufferedImage(COVER_WIDTH, bottomColourHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bottomColourGraphics = bottomColourImage.createGraphics();
        bottomColourGraphics.setPaint(bottomColor);
        bottomColourGraphics.fillRect(0, 0, COVER_WIDTH, bottomColourHeight);
        bottomColourGraphics.dispose();

        coverGraphics.drawImage(bottomColourImage, 0, COVER_HEIGHT - bottomColourHeight, null);

        Font logoFont = new Font("Arial", Font.PLAIN, 80);
        FontRenderContext frc = coverGraphics.getFontRenderContext();
        TextLayout logoLayout = new TextLayout("BDRC eBooks", logoFont, frc);
        Rectangle2D logoTextBounds = logoLayout.getBounds();

        int logoRightMargin = 40;

        int logoWidth = LOGO_WIDTH + logoRightMargin + (int)logoTextBounds.getWidth();
        int logoLeft = (COVER_WIDTH - logoWidth) / 2;
        coverGraphics.drawImage(logoImage, logoLeft, LOGO_TOP, null);

        int logoHeight = logoImage.getHeight(null);
        int logoTextBaseline = LOGO_TOP + (logoHeight / 2) + (int)(logoTextBounds.getHeight() / 2);
        coverGraphics.setColor(Color.BLACK);
        logoLayout.draw(coverGraphics, logoLeft + LOGO_WIDTH + logoRightMargin, logoTextBaseline);

        saveCover();
    }

    /**
     * Splits the title so any english get puts on a separate line
     * @param title Title of the text
     * @return The title with linee breaks inserted if required
     */
    private String splitTitle(String title)
    {
        return title.replaceAll("([\\u0F00-\\u0FFF]) ([A-Za-z])", "$1\n$2");
    }

    private void saveCover()
    {
        try {
            File imageFile = new File(outputFilePath);
            if (imageFile.getParentFile() != null) {
                imageFile.getParentFile().mkdirs();
            }
            ImageIO.write(image, "png", imageFile);
        } catch (Exception e) {
            System.out.println("Failed to save cover: " + e);
        }
    }

    private Font getFont(String fontPath, String fontName, int fontStyle, int fontSize)
    {
        try {
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch(Exception e) {
            fontName = DEFAULT_FONT;
        }

        return new Font(fontName, fontStyle, fontSize);
    }
}