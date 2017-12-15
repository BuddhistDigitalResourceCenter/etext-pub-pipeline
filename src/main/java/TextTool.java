import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class TextTool {

    private static final String BDR = "http://purl.bdrc.io/resource/";

    public static void main(String[] args)
    {
        if (args.length < 3) {
            System.out.println("Three arguments are required: data path, text id and output dir");
            return;
        }

        String dataPath = ensureTrailingSlash(args[0]);
        String id = args[1];
        String outputDirPath = ensureTrailingSlash(args[2]);
        FileDataSource ds = new FileDataSource(dataPath);
        String dirName = getOutputDirName();
        String markdownFilePath = outputDirPath + dirName + "/" + id + ".md";

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

        if (markdown != null) {
            saveStringToFile(markdown, markdownFilePath);
            System.out.println("Generated markdown file: " + markdownFilePath);
        } else {
            System.out.println("Nothing generated");
        }
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

        return item.generateMarkdown();
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
