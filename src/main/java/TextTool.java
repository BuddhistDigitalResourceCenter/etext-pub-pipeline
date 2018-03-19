import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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

    @Parameter(names={"--titleAsFilename", "-t"}, order = 4, description = "Use the text title as the filename. Otherwise, the text's ID is used.")
    public boolean titleAsFilename;

    @Parameter(names={"--help", "-h"}, order = 5, help = true, description = "Display the usage information.")
    public boolean help;
}

public class TextTool {

    private static int threadCount = 4;
    private static final String ETEXT_TYPE = "ItemEtextPaginated";
    private static final String ETEXT_PREFIX = "E";
    private static boolean titleAsFilename = false;

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

        titleAsFilename = commandArgs.titleAsFilename;

        int processors = Runtime.getRuntime().availableProcessors();
        threadCount = (processors > 1) ? processors - 1 : 1;

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
            processResource(itemId, dataPath, outputDirPath, epubFilesDir, null);
        } else {
            createEpubsForDirectory(dataPath, outputDirPath, epubFilesDir);
        }
    }

    private static void createEpubsForDirectory(String sourceDir, String outputDir, String epubFilesDir)
    {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        String itemsPath = ensureTrailingSlash(sourceDir) + "items";
        File[] dirs = new File(itemsPath).listFiles();
        List<String> etextItemPaths = new ArrayList<>();
        for (File dir: dirs) {
            if (dir.isDirectory() && !dir.getName().startsWith(".")) {
                File[] items = dir.listFiles();
                for (File item: items) {
                    if (item.getName().endsWith(".ttl")) {
                        String id = item.getName().replaceAll("\\.ttl", "");
                        String kind = id.split("_")[1].substring(0, 1);

                        // Only process files that are etexts
                        if (!kind.equals(ETEXT_PREFIX)) {
                            continue;
                        }

                        etextItemPaths.add(item.getAbsolutePath());
                        processResource(id, sourceDir, outputDir, epubFilesDir, executor);
                    }
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

    private static void processResource(String id, String sourceDir, String outputDir, String epubFilesDir, ExecutorService executor)
    {
        EpubGenerator epubGenerator = new EpubGenerator(id, sourceDir, outputDir, epubFilesDir, titleAsFilename);
        if (executor == null) {
            epubGenerator.generateEpub();
        } else {
            EpubRunnable runnable = new EpubRunnable(epubGenerator);
            executor.execute(runnable);
        }
    }

    private static boolean fileContainsString(String filepath, String string)
    {
        File file = new File(filepath);
        boolean containsString = false;
        try {
            Scanner scanner = new Scanner(file);
            while(scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(string)) {
                    containsString = true;
                    break;
                }
            }
        } catch(Exception e) {
            System.out.println("Error searching file for string: " + filepath);
        }

        return containsString;
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
