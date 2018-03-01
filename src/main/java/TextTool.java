import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;

public class TextTool {

    private static final String BDR = "http://purl.bdrc.io/resource/";

    public static void main(String[] args)
    {
        if (args.length < 3) {
            System.out.println("Three arguments are required: data path, text id (leave blank to process every file found) and output dir");
            return;
        }

        String dataPath = ensureTrailingSlash(args[0]);
        String id = args[1];
        String outputDirPath = ensureTrailingSlash(args[2]);
        FileDataSource ds = new FileDataSource(dataPath);
        String dirName = getOutputDirName();

        if (id.length() > 0) {
            // is a file id
        } else {
            createEpubsForDirectory(dataPath, outputDirPath + dirName, ds);
        }
    }

    private static void createEpubsForDirectory(String sourceDir, String outputDir, FileDataSource ds) {
        String itemsPath = ensureTrailingSlash(sourceDir) + "items";
        File[] dirs = new File(itemsPath).listFiles();
        int count = 0;
        for (File dir: dirs) {
            if (dir.isDirectory() && !dir.getName().startsWith(".")) {
                File[] items = dir.listFiles();
                for (File item: items) {
                    if (item.getName().endsWith(".ttl")) {
                        String id = item.getName().replaceAll("\\.ttl", "");
                        String markdown = generateMarkdownForResource(id, ds);
                        String markdownFilePath = outputDir +  "/" + dir.getName() + "/" + id + ".md";

                        if (markdown != null) {
                            saveStringToFile(markdown, markdownFilePath);
                            String epubFilepath = generateEpub(sourceDir, outputDir + "/", markdownFilePath, id);
                        }
                    }
                }
                count++;
                if (count % 10 == 0) {
                    break;
                }

            }

        }
    }

    private static String generateMarkdownForResource(String id, FileDataSource ds ) {
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

    private static void setupEpubFiles(String dataPath, String outputDir)
    {
        dataPath = ensureTrailingSlash(dataPath);
        Path sourceEpubCss = new File(dataPath + "epub_files/epub.css").toPath();
        Path targetEpubCss = new File(ensureTrailingSlash(outputDir) + "epub.css").toPath();
        try {
            Files.copy(sourceEpubCss, targetEpubCss, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.out.println("Error copying epub css file");
        }
    }

    private static String generateEpub(String dataPath, String outputDir, String markdownFilePath, String filename)
    {
        String pandocPath = executeCommand("command -v pandoc");

        if (pandocPath == null) return null;

        dataPath = ensureTrailingSlash(dataPath);
        Path sourceEpubCss = new File(dataPath + "epub_files/epub.css").toPath();

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
        executeCommand(epubCommand);

        return epubFilepath;
    }

    private static String executeCommand(String command)
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

    private static String generateTextMarkdown(String textId, DataSource ds)
    {
        String etextIRI = BDR + textId;
        Etext etext = new Etext(etextIRI, ds);

        return etext.generateMarkdown();
    }

    private static String generateItemMarkdown(String itemId, DataSource ds)
    {
        String itemIRI = BDR + itemId;
        Item item = new Item(itemIRI, ds);

        if (item.getType().equals("http://purl.bdrc.io/ontology/core/ItemEtextPaginated")) {
            return item.generateMarkdown();
        } else {
            return null;
        }

    }

    private static boolean saveStringToFile(String text, String filePath)
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

    private static String ensureTrailingSlash(String path)
    {
        return path.endsWith("/") ? path : path + "/";
    }

    private static String getOutputDirName()
    {
        LocalDate date = LocalDate.now();
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        LocalTime time = LocalTime.now();
        int hour = time.getHour();
        int minute = time.getMinute();
        int second = time.getSecond();

        return String.format("%d-%02d-%02d %02d-%02d-%02d", year, month, day, hour, minute, second);
    }
}
