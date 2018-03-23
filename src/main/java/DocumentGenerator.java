import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;

public class DocumentGenerator {

    public static final String BDR = "http://purl.bdrc.io/resource/";

    private final String id;
    private final String sourceDir;
    private final String outputDir;
    private final FileDataSource ds;
    private final String documentFilesDir;
    private static String pandocPath;
    private final boolean titleAsFilename;
    private final String epubFontFilename = "Jomolhari-ID.ttf";
    private final String epubFontName = "Jomolhari-ID";
    private final String logoFilename = "BDRC-logo-750.png";

    DocumentGenerator(String id, String sourceDir, String outputDir, String documentFilesDir, boolean titleAsFilename)
    {
        this.id = id;
        this.sourceDir = StringUtils.ensureTrailingSlash(sourceDir);
        this.outputDir = StringUtils.ensureTrailingSlash(outputDir);
        this.ds = new FileDataSource(this.sourceDir);
        this.documentFilesDir = StringUtils.ensureTrailingSlash(documentFilesDir);
        this.titleAsFilename = titleAsFilename;
    }

    public void generateDocuments(boolean generateEpub, boolean generateDocx)
    {
        MarkdownGenerator markdownGenerator = new MarkdownGenerator(id, sourceDir, outputDir, titleAsFilename);
        List<MarkdownDocument> markdownDocuments = markdownGenerator.generateMarkdownForResource(id, ds);

        if (markdownDocuments != null) {
            for (MarkdownDocument markdownDocument: markdownDocuments) {
                String markdownFilePath = outputDir + "markdown/" + markdownDocument.name + ".md";
                saveStringToFile(markdownDocument.markdown, markdownFilePath);
                createOutputDirs(outputDir, generateEpub, generateDocx);

                if (generateEpub) {
                    String fontPath = documentFilesDir + epubFontFilename;
                    String logoPath = documentFilesDir + logoFilename;
                    CoverGenerator coverGenerator = new CoverGenerator(fontPath, epubFontName, logoPath);
                    String coverFilename = outputDir + "covers/" + markdownDocument.name + ".png";
                    coverGenerator.generateCover(markdownDocument.title, coverFilename);

                    String epubCommand = generateEpubCommand(sourceDir, outputDir, markdownFilePath, markdownDocument.name, coverFilename);
                    executeCommand(epubCommand);
                }

                if (generateDocx) {
                    String docxCommand = generateDocxCommand(outputDir, markdownFilePath, markdownDocument.name);
                    executeCommand(docxCommand);
                }
            }
        }
    }

    public void createOutputDirs(String outputDir, boolean epubFiles, boolean docxFiles)
    {
        if (epubFiles) {
            String epubOutputDir = outputDir + "epub/";
            File directory = new File(epubOutputDir);
            if (! directory.exists()){
                directory.mkdir();
            }
        }

        if (docxFiles) {
            String docxOutputDir = outputDir + "docx/";
            File directory = new File(docxOutputDir);
            if (! directory.exists()){
                directory.mkdir();
            }
        }
    }

    private boolean saveStringToFile(String text, String filePath)
    {
        File outputFile = new File(filePath);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        try {
            outputFile.createNewFile();
        } catch (Exception e) {
            System.out.println("Failed to create new markdown file");
            System.out.println(e);
            return false;
        }

        try {
            try(PrintWriter out = new PrintWriter(outputFile)) {
                out.print(text);
            }
        } catch(Exception e) {
            System.out.println("Failed to write to markdown file.");
            System.out.println(e);
            return false;
        }

        return true;
    }

    private String getPandocPath()
    {
        if (pandocPath == null) {
            pandocPath = executeCommand("command -v pandoc");
        }

        return pandocPath;
    }

    private String generateEpubCommand(String dataPath, String outputDir, String markdownFilePath, String filename, String coverPath)
    {
        String pandocPath = getPandocPath();
        if (pandocPath == null) return null;

        dataPath = StringUtils.ensureTrailingSlash(dataPath);
        Path sourceEpubCss = new File(documentFilesDir + "epub.css").toPath();

        String epubFilepath = outputDir + "epub/" + filename + ".epub";
        String epubCommand = pandocPath + " " +
                "-f markdown " +
                "-t epub3 " +
                "\"" + markdownFilePath + "\" " +
                "-o \"" + epubFilepath + "\" " +
                "--toc-depth=2 " +
                "--epub-chapter-level=3 " +
                "--epub-stylesheet=\""+sourceEpubCss.toString()+"\" " +
                "--epub-embed-font=\""+dataPath + "document_files/Jomolhari.ttf"+"\" " +
                "--epub-cover=\""+coverPath+"\" "
                ;

        return epubCommand;
    }

    private String generateDocxCommand(String outputDir, String markdownFilePath, String filename)
    {
        String pandocPath = getPandocPath();
        if (pandocPath == null) return null;

        String docxFilepath = outputDir + "docx/" + filename + ".docx";
        String docxReference = documentFilesDir + "reference.docx";
        String docxCommand = pandocPath + " " +
                "-f markdown " +
                "-t docx " +
                "\"" + markdownFilePath + "\" " +
                "-o \"" + docxFilepath + "\" " +
                "--toc-depth=2 " +
                "--reference-doc=\"" + docxReference + "\""
                ;

        return docxCommand;
    }

    private String executeCommand(String command)
    {
        Process proc;
        String output = null;
        try {
            proc = Runtime.getRuntime().exec(new String[] {"bash", "-c", command});
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            while ((output = stdInput.readLine()) != null) {
                break;
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.toString());
        }

        return output;
    }
}
