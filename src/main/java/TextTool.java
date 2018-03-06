import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class EpubRunnable implements Runnable {
    private EpubGenerator epubGenerator;

    EpubRunnable(EpubGenerator epubGenerator) {
        this.epubGenerator = epubGenerator;
    }

    @Override
    public void run() {
        epubGenerator.generateEpub();
    }
}


public class TextTool {

    private static final int NTHREADS = 8;

    public static void main(String[] args)
    {
        if (args.length < 3) {
            System.out.println("Three arguments are required: data path, text id (leave blank to process every file found) and output dir");
            return;
        }

        String dataPath = ensureTrailingSlash(args[0]);
        String id = args[1];
        String outputDirPath = ensureTrailingSlash(args[2]);
        String dirName = getOutputDirName();

        if (id.length() > 0) {
            // is a file id
        } else {
            createEpubsForDirectory(dataPath, outputDirPath + dirName);
        }
    }

    private static void createEpubsForDirectory(String sourceDir, String outputDir)
    {
        ExecutorService executor = Executors.newFixedThreadPool(NTHREADS);

        String itemsPath = ensureTrailingSlash(sourceDir) + "items";
        File[] dirs = new File(itemsPath).listFiles();
        int count = 0;
        for (File dir: dirs) {
            if (dir.isDirectory() && !dir.getName().startsWith(".")) {
                File[] items = dir.listFiles();
                for (File item: items) {
                    if (item.getName().endsWith(".ttl")) {

                        String id = item.getName().replaceAll("\\.ttl", "");
                        EpubGenerator epubGenerator = new EpubGenerator(id, sourceDir, outputDir, dir);
                        EpubRunnable runnable = new EpubRunnable(epubGenerator);
                        executor.execute(runnable);
                    }
                }
                count++;
                if (count % 10 == 0) {
                    break;
                }

            }

        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.out.println("Interrupted epub generation");
        }
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
