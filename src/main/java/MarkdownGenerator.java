import java.util.*;

class MarkdownDocument {
    MarkdownDocument(String markdown, String name, String title)
    {
        this.markdown = markdown;
        this.name = name;
        this.title = title;
    }
    String markdown;
    String name;
    String title;
    String author;
    String inputter;
    int volume;
}

public class MarkdownGenerator {

    public static final String BDR = "http://purl.bdrc.io/resource/";

    private final String id;
    private final String sourceDir;
    private final String outputDir;
    private final FileDataSource ds;
    private final String terms;
    private static int maxSectionSize = 50000;
    private static int linesPerPara = 10;
    private static int maxTitleLength = 240; // Linux max filename excluding extension

    MarkdownGenerator(String id, String sourceDir, String outputDir, String terms)
    {
        this.id = id;
        this.sourceDir = StringUtils.ensureTrailingSlash(sourceDir);
        this.outputDir = StringUtils.ensureTrailingSlash(outputDir);
        this.ds = new FileDataSource(this.sourceDir);
        this.terms = terms;
    }

    public List<MarkdownDocument> generateMarkdownForResource(String id, FileDataSource ds )
    {
        List<MarkdownDocument> markdownDocuments;
        String firstChar = String.valueOf(id.charAt(0));
        switch(firstChar) {
            case "I":
                markdownDocuments = generateItemMarkdown(id, ds);
                break;
            case "U":
                String markdown = generateTextMarkdown(id, ds);
                MarkdownDocument markdownDocument = new MarkdownDocument(markdown, id, id);
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

        if (!item.getType().equals("http://purl.bdrc.io/ontology/core/ItemEtextPaginated")
                && !item.getType().equals("http://purl.bdrc.io/ontology/core/ItemEtextNonPaginated")
                ) {
            return null;
        }

        List<MarkdownDocument> markdownDocuments = new ArrayList<>();
        Map<Integer, List<WorkSection>> volumeSections = new HashMap<>();

        Map<Integer, Etext> etexts = item.getEtexts();
        Work work = item.getWork();
        Map<Integer, List<Work>> workParts = null;
        if (work != null ) {
            workParts = work.getWorkParts();
        }

        int totalVolumes = etexts.keySet().size();
        for (Map.Entry<Integer, Etext> entry : etexts.entrySet()) {
            Etext etext = entry.getValue();
            Integer volumeNumber = entry.getKey();
            List<WorkSection> sections;

            if (work != null && workParts.size() > 0 && etext.getPages() != null && etext.getPages().size() > 0) {
                sections = work.getSections(etext, volumeNumber);
            } else {
                sections = new ArrayList<>();

                WorkSection section = new WorkSection();
                section.work = work;
                section.title = "The Text {.enHeader}";
                section.content = String.join("\n", etext.getContentLines());
                sections.add(section);
            }

            sections.sort((leftSection, rightSection) -> {
                return Integer.compare(leftSection.location.startPage, rightSection.location.startPage);
            });

            volumeSections.put(volumeNumber, sections);
        }

        for (Map.Entry<Integer, List<WorkSection>> entry : volumeSections.entrySet()) {
            int volume = entry.getKey();
            List<WorkSection> sections = entry.getValue();

            StringBuilder docSb = new StringBuilder();

            docSb.append("# ")
                    .append(item.getTitle())
                    .append("\n\n");

            if (totalVolumes > 1) {
                docSb.append("#### པོད ")
                        .append(TibetanUtils.getTibetanNumber(volume))
                        .append(" {.volume}")
                        .append("\n\n");
            }

            docSb.append(terms).append("\n\n");

            for (WorkSection workSection: sections) {
                docSb.append(markdownForSection(workSection, 2));
            }

            String title = item.getTitle();
            String textName = generateTextName(title, item.getId(), item.isOcr(), totalVolumes, volume);

            MarkdownDocument document = new MarkdownDocument(docSb.toString(), textName, title);
            if (totalVolumes > 1) {
                document.volume = volume;
            }

            try {
                int textNameByteCount = textName.getBytes("UTF-8").length;
                if (textNameByteCount > maxTitleLength) {
                    String truncatedTextName = "";
                    for(int i = 0, n = textName.length() ; i < n ; i++) {
                        char c = textName.charAt(i);
                        truncatedTextName += c;
                        if (truncatedTextName.getBytes("UTF-8").length >= maxTitleLength) {
                            break;
                        }
                    }
                    document.name = truncatedTextName;
                }
            } catch(Exception e) {
                System.out.println("Exception truncating name: " + e);
            }

            document.author = item.getAuthor();
            document.inputter = item.getDistributor();
            markdownDocuments.add(document);
        }

        return markdownDocuments;
    }

    private String generateTextName(String title, String id, boolean isOcr, int totalVolumes, int volume)
    {
        String textName = ((isOcr) ? "OCR " : "") + id + ((totalVolumes > 1) ? "_vol_" + volume : "") + " " + title;

        return textName;
    }

    private String markdownForMetadata(List<MetadataItem> metadataItems)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" | \n-|-:\n");
        for (MetadataItem item: metadataItems) {
            sb.append(item.label).append("|").append(String.join(", ", item.values));
            sb.append("\n");
        }

        return sb.toString();
    }

    public String markdownForSection(WorkSection section, int level) {
        String headingBase = "";
        for (int i=1; i < level; i++) {
            headingBase += "#";
        }

        StringBuilder sectionSb = new StringBuilder();

        if (section.title != null) {
            sectionSb.append(headingBase)
                    .append("# ")
                    .append(section.title)
                    .append("\n\n");
        }

        if (section.author != null) {
            sectionSb.append("[")
                    .append(section.author)
                    .append("]{.author}")
                    .append("\n\n");
        }

        if (section.sections != null && section.sections.size() > 0) {
            for (WorkSection workSection: section.sections) {
                sectionSb.append(markdownForSection(workSection, level + 1));
            }
            sectionSb.append("\n\n");
        } else {
            String content = splitMarkdownText(section.content, maxSectionSize);
            sectionSb.append(content).append("\n\n");
        }

        return sectionSb.toString();
    }

    protected String splitMarkdownText(String text, int sectionSize)
    {
        StringBuilder textSb = new StringBuilder();
        List<String> textLines = Arrays.asList(text.split("\n"));
        int sectionLength = 0;
        int paraLine = 0;
        for (String line: textLines) {
            paraLine++;
            if (sectionLength > sectionSize) {
                textSb.append("\n\n").append("### {.empty}").append("\n\n");
                sectionLength = 0;
                paraLine = 0;
            }
            textSb.append(line);
            textSb.append("\n");
            sectionLength += line.length();
            if (paraLine > 0 && paraLine % linesPerPara == 0) {
                // add para break to speed up page rendering
                textSb.append("\n");
                paraLine = 0;
            }
        }

        return textSb.toString();
    }
}
