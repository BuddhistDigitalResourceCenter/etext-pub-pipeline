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
    private static final int SMALL_FONT_SIZE = 80;
    private static final int SMALLER_FONT_SIZE = 60;
    private static final int COVER_WIDTH = 1600;
    private static final int COVER_HEIGHT = 2400;
    private static final int TITLE_WIDTH = 1200;
    private static final int TITLE_TOP = 300;
    private static final String TIBETAN_FONT = "Qomolangma-UchenSarchen.ttf";
    private static final String TIBETAN_FONT_NAME = "Qomolangma-Uchen Sarchen";
    private static final String LATIN_FONT = "EBGaramond-SemiBold.ttf";
    private static Font coverFontTibetan;
    private static Font coverFontLatin;
    private static FontMetrics fontMetrics;
    private BufferedImage image;
    private String outputFilePath;
    private static final Color backgroundColor = Color.WHITE;
    private static Color bottomColor = new Color(129, 25, 38);
    private static Color bottomTypeColor = Color.WHITE;
    private static final int LOGO_TOP = 2100;
    private static final int LOGO_WIDTH = 170;
    private static final int TEXT_MARGIN_SIDE = 50;
    private static final int TEXT_MARGIN_TOPBOTTOM = 50;
    private Image logoImage;

    public CoverGenerator(String documentFilesPath, String logoPath)
    {
        try {
            image = new BufferedImage(COVER_WIDTH, COVER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setPaint(backgroundColor);
            graphics.fillRect(0, 0, COVER_WIDTH, COVER_HEIGHT);
            documentFilesPath = StringUtils.ensureTrailingSlash(documentFilesPath);

            if (coverFontTibetan == null) {
                coverFontTibetan = getFont(documentFilesPath + TIBETAN_FONT, TIBETAN_FONT_NAME, Font.PLAIN, FONT_SIZE);
            }

            if (coverFontLatin == null) {
                String latinFontName = LATIN_FONT.replace(".ttf", "");
                coverFontLatin = getFont(documentFilesPath + LATIN_FONT, latinFontName, Font.PLAIN, 80);
            }

            if (fontMetrics == null) {
                fontMetrics = graphics.getFontMetrics(coverFontTibetan);
            }

            graphics.dispose();

            logoImage = ImageIO.read(new File(logoPath));
            logoImage = logoImage.getScaledInstance(LOGO_WIDTH, -1, Image.SCALE_DEFAULT);
        } catch(Exception e) {
            System.out.printf("Exception generating cover: %s", e.toString());
        }
    }

    public void generateCover(String title, String author, String inputter, int volume, String outputFilePath)
    {
        // Need to add spacing for the Qomolangma-UchenSarchen font.
        // Other fonts won't require it.
        title = title.replace("༼", " ༼\u00A0\u00A0\u00A0\u00A0");
        title = title.replace("༽", "\u00A0\u00A0༽ ");

        this.outputFilePath = outputFilePath;
        List<String> titleLines = StringUtils.wrap(title, fontMetrics, TITLE_WIDTH);
        StringBuilder titleHtmlBuilder = new StringBuilder();
        for (String line: titleLines) {
            titleHtmlBuilder.append("<p style='padding-bottom: 20px'>").append(line).append("</p>");
        }
        if (volume > 0) {
            String tibetanVolume = TibetanUtils.getTibetanNumber(volume);
            titleHtmlBuilder.append("<p style='padding-top: 100px'>").append(tibetanVolume).append("</p>");
        }
        String titleHtml = titleHtmlBuilder.toString();
        JLabel label = new JLabel("<html><body style='width: " + TITLE_WIDTH + "px; padding: 0px; text-align: center'>" + titleHtml + "</body></html>");
        label.setFont(coverFontTibetan);
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

        Font textTibetanFont = new Font(TIBETAN_FONT_NAME, Font.PLAIN, SMALL_FONT_SIZE);

        // Author
        if (author != null) {
            FontRenderContext frc = coverGraphics.getFontRenderContext();
            TextLayout authorLayout = new TextLayout(author, textTibetanFont, frc);
            Rectangle2D authorTextBounds = authorLayout.getBounds();
            int authorWidth = (int)authorTextBounds.getWidth();
            int x = COVER_WIDTH - authorWidth - TEXT_MARGIN_SIDE;
            int y = COVER_HEIGHT - bottomColourHeight - TEXT_MARGIN_TOPBOTTOM;
            coverGraphics.setColor(bottomColor);
            authorLayout.draw(coverGraphics, x, y);
        }

        // Inputter
        if (inputter != null) {
            Font inputterTibetanFont = new Font(TIBETAN_FONT_NAME, Font.PLAIN, SMALLER_FONT_SIZE);
            FontRenderContext frc = coverGraphics.getFontRenderContext();
            TextLayout inputterLayout = new TextLayout(inputter, inputterTibetanFont, frc);
            Rectangle2D inputterTextBounds = inputterLayout.getBounds();
            int inputterWidth = (int)inputterTextBounds.getWidth();
            int inputterHeight = (int)inputterTextBounds.getHeight();
            int x = COVER_WIDTH - inputterWidth - TEXT_MARGIN_SIDE;
            int y = COVER_HEIGHT - bottomColourHeight + inputterHeight;
            coverGraphics.setColor(Color.WHITE);
            inputterLayout.draw(coverGraphics, x, y);
        }

        // Logo and logo text
        FontRenderContext frc = coverGraphics.getFontRenderContext();
        TextLayout logoLayout = new TextLayout("BDRC eBooks", coverFontLatin, frc);
        Rectangle2D logoTextBounds = logoLayout.getBounds();

        int logoRightMargin = 40;

        int logoWidth = LOGO_WIDTH + logoRightMargin + (int)logoTextBounds.getWidth();
        int logoLeft = (COVER_WIDTH - logoWidth) / 2;
        coverGraphics.drawImage(logoImage, logoLeft, LOGO_TOP, null);

        int logoHeight = logoImage.getHeight(null);
        int logoTextBaseline = LOGO_TOP + (logoHeight / 2) + (int)(logoTextBounds.getHeight() / 2);
        coverGraphics.setColor(bottomTypeColor);
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
            System.out.println("Failed to get font: " + fontPath);
            fontName = TIBETAN_FONT;
        }

        return new Font(fontName, fontStyle, fontSize);
    }
}