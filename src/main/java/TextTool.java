import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.jena.base.Sys;

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

class Args {
    @Parameter
    public List<String> parameters = new ArrayList<>();

    @Parameter(names={"--sourceDir", "-s"}, order = 0, required = true, description = "The directory that contains the directories containing the .ttl files. (required)")
    public String sourceDir;

    @Parameter(names={"--outputDir", "-o"}, order = 1, description = "The directory where the generated files will be saved. Defaults to ./output")
    public String outputDir;

    @Parameter(names={"--epubFiles", "-ef"}, order = 2, description = "The directory that contains files used for the epub. Defaults to ./epub_files")
    public String epubFiles;

    @Parameter(names={"--itemId", "-id"}, order = 3, description = "If supplied, only the item with this id will be processed.")
    public String itemId;

    @Parameter(names={"--help", "-h"}, order = 4, help = true, description = "Display the usage information.")
    public boolean help;
}

public class TextTool {

    private static final int NTHREADS = 8;

    public static void main(String[] args)
    {
        Args commandArgs = new Args();
        JCommander jcommander = JCommander.newBuilder()
                .addObject(commandArgs)
                .build();
        try {
            jcommander.parse(args);
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            jcommander.usage();
            return;
        }

        if (commandArgs.help) {
            jcommander.usage();
            return;
        }

        String workingDir = ensureTrailingSlash(System.getProperty("user.dir"));
        String dataPath = ensureTrailingSlash(commandArgs.sourceDir);
        if (!(new File(dataPath).exists())) {
            System.out.println("Error: Supplied source directory does not exist - " + dataPath);
            return;
        }
        String outputDirPath = workingDir + "output/";
        if (commandArgs.outputDir != null) {
            if (!(new File(commandArgs.outputDir).exists())) {
                System.out.println("Error: Supplied output directory does not exist");
                return;
            }
            outputDirPath = commandArgs.outputDir;
        }
        outputDirPath = ensureTrailingSlash(outputDirPath) + getOutputDirName();
        String epubFilesDir = workingDir + "epub_files";
        if (commandArgs.epubFiles != null) {
            epubFilesDir = ensureTrailingSlash(commandArgs.epubFiles) + "epub_files";
        }
        String itemId = commandArgs.itemId;

        if (itemId != null && itemId.length() > 0) {
            // just process the given item
        } else {
            createEpubsForDirectory(dataPath, outputDirPath, epubFilesDir);
        }
    }

    private static void createEpubsForDirectory(String sourceDir, String outputDir, String epubFilesDir)
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
                        EpubGenerator epubGenerator = new EpubGenerator(id, sourceDir, outputDir, epubFilesDir);
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
