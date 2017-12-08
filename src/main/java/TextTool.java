import org.apache.jena.base.Sys;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Calendar;

public class TextTool {

    private static final String BDR = "http://purl.bdrc.io/resource/";

    public static void main(String[] args)
    {

        if (args.length < 3) {
            System.out.println("Three arguments are required: data path, text id and output dir");
            return;
        }

        String dataPath = ensureTrailingSlash(args[0]);
        String textId = args[1];
        String outputDirPath = ensureTrailingSlash(args[2]);

        FileDataSource ds = new FileDataSource(dataPath);

        String etextIRI = BDR + textId;
        Etext etext = new Etext(etextIRI, ds);

        String dirName = getOutputDirName();
        String markdownFilePath = outputDirPath + dirName + "/" + textId + ".md";

        saveStringToFile(etext.generateMarkdown(), markdownFilePath);

        System.out.println("Generated markdown file: " + markdownFilePath);
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
        Calendar cal = Calendar.getInstance();
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
