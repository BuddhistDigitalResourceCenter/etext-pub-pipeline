import java.io.*;
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

class DocumentRunnable implements Runnable {
    private DocumentGenerator documentGenerator;
    private boolean generateEpub;
    private boolean generateDocx;

    DocumentRunnable(DocumentGenerator documentGenerator, boolean generateEpub, boolean generateDocx) {
        this.documentGenerator = documentGenerator;
        this.generateEpub = generateEpub;
        this.generateDocx = generateDocx;

    }

    @Override
    public void run() {
        try {
            documentGenerator.generateDocuments(generateEpub, generateDocx);
        } catch(Exception e) {
            System.out.println("Exception generating document");
            System.out.println(e);
        }
    }
}

class Args {
    @Parameter
    public List<String> parameters = new ArrayList<>();

    @Parameter(names={"--sourceDir", "-s"}, order = 0, required = true, description = "The directory that contains the directories containing the .ttl files. (required)")
    public String sourceDir;

    @Parameter(names={"--outputDir", "-o"}, order = 1, description = "The directory where the generated files will be saved. Defaults to ./output")
    public String outputDir;

    @Parameter(names={"--documentFiles", "-df"}, order = 2, description = "The directory that contains files used for the epub and docx. Defaults to ./document_files")
    public String documentFiles;

    @Parameter(names={"--itemId", "-id"}, order = 3, description = "If supplied, only the item with this id will be processed.")
    public String itemId;

    @Parameter(names={"--docx", "-d"}, order = 5, description = "Only generate docx files")
    public boolean docx;

    @Parameter(names={"--epub", "-e"}, order = 5, description = "Only generate epub files")
    public boolean epub;

    @Parameter(names={"--help", "-h"}, order = 6, help = true, description = "Display the usage information.")
    public boolean help;
}

public class TextTool {

    private static int threadCount = 4;
    private static final String ETEXT_TYPE = "ItemEtextPaginated";
    private static final String ETEXT_PREFIX = "E";
    private static final String TERMS_FILENAME = "terms.md";

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

        int processors = Runtime.getRuntime().availableProcessors();
        threadCount = (processors > 1) ? processors - 1 : 1;

        String workingDir = StringUtils.ensureTrailingSlash(System.getProperty("user.dir"));
        String dataPath = StringUtils.ensureTrailingSlash(commandArgs.sourceDir);
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
        outputDirPath = StringUtils.ensureTrailingSlash(outputDirPath) + getOutputDirName();
        String documentFilesDir = workingDir + "document_files";
        if (commandArgs.documentFiles != null) {
            documentFilesDir = StringUtils.ensureTrailingSlash(commandArgs.documentFiles) + "document_files";
        }
        String itemId = commandArgs.itemId;

        boolean createEpub = !commandArgs.docx;
        boolean createDocx = !commandArgs.epub;

        if (itemId != null && itemId.length() > 0) {
            // just process the given item
            processResource(itemId, dataPath, outputDirPath, documentFilesDir, createEpub, createDocx, null);
        } else {
            createEpubsForDirectory(dataPath, outputDirPath, documentFilesDir, createEpub, createDocx);
        }
    }

    private static void createEpubsForDirectory(String sourceDir, String outputDir, String documentFilesDir, boolean createEpub, boolean createDocx)
    {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Ensure the css file is created so it won't cause potential race condition.
        String cssTemplatePath = DocumentGenerator.getEpubCssFilepath(documentFilesDir);
        DocumentGenerator.getEpubCss(cssTemplatePath, outputDir);

        String itemsPath = StringUtils.ensureTrailingSlash(sourceDir) + "items";
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
                        processResource(id, sourceDir, outputDir, documentFilesDir, createEpub, createDocx, executor);
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

    private static void processResource(String id, String sourceDir, String outputDir, String documentFilesDir, boolean createEpub, boolean createDocx, ExecutorService executor)
    {
        documentFilesDir = StringUtils.ensureTrailingSlash(documentFilesDir);
        String terms = StringUtils.getFileText(documentFilesDir + TERMS_FILENAME);
        DocumentGenerator documentGenerator = new DocumentGenerator(id, sourceDir, outputDir, documentFilesDir, terms);
        if (executor == null) {
            documentGenerator.generateDocuments(createEpub, createDocx);
        } else {
            DocumentRunnable runnable = new DocumentRunnable(documentGenerator, createEpub, createDocx);
            executor.execute(runnable);
        }
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
