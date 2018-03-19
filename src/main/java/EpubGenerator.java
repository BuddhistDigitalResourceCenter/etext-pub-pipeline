import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EpubGenerator {

    public static final String BDR = "http://purl.bdrc.io/resource/";

    private final String id;
    private final String sourceDir;
    private final String outputDir;
    private final FileDataSource ds;
    private final String epubFilesDir;
    private static String pandocPath;
    private final boolean titleAsFilename;

    EpubGenerator(String id, String sourceDir, String outputDir, String epubFilesDir, boolean titleAsFilename) {
        this.id = id;
        this.sourceDir = ensureTrailingSlash(sourceDir);
        this.outputDir = ensureTrailingSlash(outputDir);
        this.ds = new FileDataSource(this.sourceDir);
        this.epubFilesDir = ensureTrailingSlash(epubFilesDir);
        this.titleAsFilename = titleAsFilename;
    }

    public void generateEpub()
    {
        List<MarkdownDocument> markdownDocuments = generateMarkdownForResource(id, ds);

        if (markdownDocuments != null) {
            for (MarkdownDocument markdownDocument: markdownDocuments) {
                String markdownFilePath = outputDir + "markdown/" + markdownDocument.name + ".md";
                saveStringToFile(markdownDocument.markdown, markdownFilePath);
                String epubCommand = generateEpubCommand(sourceDir, outputDir, markdownFilePath, markdownDocument.name);
                executeCommand(epubCommand);
            }
        }
    }

    private List<MarkdownDocument> generateMarkdownForResource(String id, FileDataSource ds )
    {
        List<MarkdownDocument> markdownDocuments;
        String firstChar = String.valueOf(id.charAt(0));
        switch(firstChar) {
            case "I":
                markdownDocuments = generateItemMarkdown(id, ds);
                break;
            case "U":
                String markdown = generateTextMarkdown(id, ds);
                MarkdownDocument markdownDocument = new MarkdownDocument(markdown, id);
                markdownDocuments = new ArrayList<>();
                markdownDocuments.add(markdownDocument);
                break;
            default:
                System.out.println("Passed unknown resource type: " + firstChar);
                markdownDocuments = null;
        }

        return markdownDocuments;
    }

    private String generateTextMarkdown(String textId, DataSource ds)
    {
        String etextIRI = BDR + textId;
        Etext etext = new Etext(etextIRI, ds);

        return etext.generateMarkdown();
    }

    private List<MarkdownDocument> generateItemMarkdown(String itemId, DataSource ds)
    {
        String itemIRI = BDR + itemId;
        Item item = new Item(itemIRI, ds);

        if (item.getType().equals("http://purl.bdrc.io/ontology/core/ItemEtextPaginated")) {
            return item.generateMarkdown(titleAsFilename);
        } else {
            return null;
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

    private String generateEpubCommand(String dataPath, String outputDir, String markdownFilePath, String filename)
    {
        String pandocPath = getPandocPath();

        if (pandocPath == null) return null;

        dataPath = ensureTrailingSlash(dataPath);
        Path sourceEpubCss = new File(epubFilesDir + "epub.css").toPath();

        String epubFilepath = outputDir  + filename + ".epub";
        String epubCommand = pandocPath + " " +
                "-f markdown " +
                "-t epub3 " +
                "\"" + markdownFilePath + "\" " +
                "-o \"" + epubFilepath + "\" " +
                "--toc-depth=2 " +
                "--epub-chapter-level=3 " +
                "--epub-stylesheet=\""+sourceEpubCss.toString()+"\" " +
                "--epub-embed-font=\""+dataPath + "epub_files/Jomolhari.ttf"+"\" "
                ;

        return epubCommand;
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

    private String ensureTrailingSlash(String path)
    {
        return path.endsWith("/") ? path : path + "/";
    }
}
