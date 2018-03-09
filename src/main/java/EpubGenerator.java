import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;

public class EpubGenerator {

    private static final String BDR = "http://purl.bdrc.io/resource/";

    private final String id;
    private final String sourceDir;
    private final String outputDir;
    private final FileDataSource ds;
    private final String epubFilesDir;

    EpubGenerator(String id, String sourceDir, String outputDir, String epubFilesDir) {
        this.id = id;
        this.sourceDir = ensureTrailingSlash(sourceDir);
        this.outputDir = ensureTrailingSlash(outputDir);
        this.ds = new FileDataSource(this.sourceDir);
        this.epubFilesDir = ensureTrailingSlash(epubFilesDir);
    }

    public void generateEpub()
    {
        String markdown = generateMarkdownForResource(id, ds);
        String markdownFilePath = outputDir + "markdown/" + id + ".md";

        if (markdown != null) {
            saveStringToFile(markdown, markdownFilePath);
            String epubCommand = generateEpubCommand(sourceDir, outputDir, markdownFilePath, id);
            executeCommand(epubCommand);
        }
    }

    private String generateMarkdownForResource(String id, FileDataSource ds )
    {
        String markdown;
        String firstChar = String.valueOf(id.charAt(0));
        switch(firstChar) {
            case "I":
                markdown = generateItemMarkdown(id, ds);
                break;
            case "U":
                markdown = generateTextMarkdown(id, ds);
                break;
            default:
                markdown = null;
        }

        return markdown;
    }

    private String generateTextMarkdown(String textId, DataSource ds)
    {
        String etextIRI = BDR + textId;
        Etext etext = new Etext(etextIRI, ds);

        return etext.generateMarkdown();
    }

    private String generateItemMarkdown(String itemId, DataSource ds)
    {
        String itemIRI = BDR + itemId;
        Item item = new Item(itemIRI, ds);

        if (item.getType().equals("http://purl.bdrc.io/ontology/core/ItemEtextPaginated")) {
            return item.generateMarkdown();
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

    private String generateEpubCommand(String dataPath, String outputDir, String markdownFilePath, String filename)
    {
        String pandocPath = executeCommand("command -v pandoc");

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
        System.out.println(epubCommand);

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
