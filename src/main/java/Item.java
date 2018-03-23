import org.apache.jena.base.Sys;

import java.util.*;

public class Item extends BDRCResource {

    private RDFResource item;
    private static int maxSectionSize = 10000;

    public Item(String IRI, DataSource dataSource)
    {
        super(IRI, dataSource);

        item = dataSource.loadResource(IRI);
        resource = item;
    }

//
//    public List<MarkdownDocument> generateMarkdown(boolean titleAsFilename)
//    {
//        List<MarkdownDocument> markdownDocuments = new ArrayList<>();
//        Map<Integer, List<WorkSection>> volumeSections = new HashMap<>();
//
//        Map<Integer, Etext> etexts = getEtexts();
//        Work work = getWork();
//        Map<Integer, List<Work>> workParts = null;
//        if (work != null ) {
//            //System.out.println("Processing parts for " + IRI + " in work: " + work.IRI);
//            workParts = work.getWorkParts();
//        }
//
//        int totalVolumes = etexts.keySet().size();
//        for (Map.Entry<Integer, Etext> entry : etexts.entrySet()) {
//            Etext etext = entry.getValue();
//            Integer volumeNumber = entry.getKey();
//            List<WorkSection> sections;
//
//            if (work != null && workParts.size() > 0) {
//                sections = work.getSections(etext, volumeNumber);
//            } else {
//                sections = new ArrayList<>();
//
//                WorkSection section = new WorkSection();
//                section.title = "The Text";
//                section.content = String.join("\n", etext.getContentLines());
//                sections.add(section);
//            }
//
//            volumeSections.put(volumeNumber, sections);
//        }
//
//        for (Map.Entry<Integer, List<WorkSection>> entry : volumeSections.entrySet()) {
//            int volume = entry.getKey();
//            List<WorkSection> sections = entry.getValue();
//
//            StringBuilder docSb = new StringBuilder();
//
//            docSb.append("# ")
//                    .append(getTitle())
//                    .append("\n\n");
//
//            if (totalVolumes > 1) {
//                docSb.append("[Volume ")
//                    .append(volume)
//                    .append("]{.volume}")
//                    .append("\n\n");
//            }
//
//            for (WorkSection workSection: sections) {
//                docSb.append(markdownForSection(workSection, 2));
//            }
//
//            String textName = getId();
//            String title = getTitle();
//            if (titleAsFilename) {
//                textName = getTitle();
//            }
//            if (totalVolumes > 1) {
//                textName += "_vol_" + volume;
//                title += " Volume " + volume;
//            }
//            MarkdownDocument document = new MarkdownDocument(docSb.toString(), textName, title);
//            markdownDocuments.add(document);
//        }
//
//        return markdownDocuments;
//    }
//
//    public String markdownForSection(WorkSection section, int level) {
//        String headingBase = "";
//        for (int i=1; i < level; i++) {
//            headingBase += "#";
//        }
//
//        StringBuilder sectionSb = new StringBuilder();
//
//        if (section.title != null) {
//            sectionSb.append(headingBase)
//                    .append("# ")
//                    .append(section.title)
//                    .append("\n\n");
//        }
//
//        if (section.author != null) {
//            sectionSb.append("[")
//                    .append(section.author)
//                    .append("]{.author}")
//                    .append("\n\n");
//        }
//
//        if (section.sections != null && section.sections.size() > 0) {
//            for (WorkSection workSection: section.sections) {
//                sectionSb.append(markdownForSection(workSection, level + 1));
//            }
//            sectionSb.append("\n\n");
//        } else {
//            String content = splitMarkdownText(section.content, maxSectionSize);
//            sectionSb.append(content).append("\n\n");
//        }
//
//        return sectionSb.toString();
//    }

//    public List<MarkdownDocument> generateMarkdownOrig(boolean titleAsFilename)
//    {
//        List<MarkdownDocument> markdownDocuments = new ArrayList<>();
//
//        Map<Integer, Etext> etexts = getEtexts();
//        Work work = getWork();
//        Map<Integer, List<Work>> workParts = null;
//        if (work != null ) {
//            System.out.println("Processing parts for " + IRI + " in work: " + work.IRI);
//            workParts = work.getWorkParts();
//        }
//
//        int totalVolumes = etexts.keySet().size();
//        for (Map.Entry<Integer, Etext> entry : etexts.entrySet()) {
//            Etext etext = entry.getValue();
//            Integer volumeNumber = entry.getKey();
//
//            if (work != null) {
//                List<WorkSection> sections = work.getSections(etext, volumeNumber);
//                System.out.println("Sections: ");
////                String output = Arrays.toString(sections);
//                System.out.println(sections);
//            }
//
//            List<Work> works = null;
//            if (workParts != null) works = workParts.get(volumeNumber);
//
//            if (work == null) {
//                System.out.println("No work for " + IRI);
//                MarkdownDocument document = new MarkdownDocument(etext.generateMarkdown(), etext.getId());
//                markdownDocuments.add(document);
//            } else if (works != null && works.size() > 0) {
//                System.out.println("Got works, size: " + works.size());
//                StringBuilder textSb = new StringBuilder();
//                textSb.append("# ").append(getTitle()).append("\n\n");
//                if (totalVolumes > 1) {
//                    textSb.append("[Volume ")
//                            .append(volumeNumber)
//                            .append("]{.volume}")
//                            .append("\n\n");
//                }
//                String author = work.getPrimaryName(work.getMainAuthor(), "bo");
//                if (author != null) {
//                    textSb.append("[")
//                            .append(author)
//                            .append("]{.author}")
//                            .append("\n\n");
//                }
//
//                for (Work textWork : works) {
//                    WorkLocation location = textWork.getLocation();
//                    Map<Integer, String> pagesContent = etext.getPageContent();
//                    if (location != null && pagesContent != null) {
//                        if (pagesContent.containsKey(location.startPage) &&
//                                pagesContent.containsKey(location.endPage)) {
//                            StringBuilder sb = new StringBuilder();
//                            for(int i=location.startPage; i < location.endPage; i++) {
//                                sb.append(pagesContent.get(i));
//                            }
//                            String content = sb.toString();
//                            content = splitMarkdownText(content, maxSectionSize);
//                            String title = textWork.getTitle();
//
//                            textSb.append("## ")
//                                    .append(title)
//                                    .append("\n\n");
//
//                            String textWorkAuthor = textWork.getPrimaryName(textWork.getMainAuthor(), "bo");
//                            if (textWorkAuthor != null && !author.equals(textWorkAuthor)) {
//                                textSb.append("[")
//                                        .append(textWorkAuthor)
//                                        .append("]{.subAuthor}")
//                                        .append("\n\n");
//                            }
//
//                            textSb.append(content).append("\n\n");
//                        }
//                    } else {
//                        if (location == null) {
//                            System.out.printf("Missing location for work: %s %n", textWork.IRI);
//                        }
//                        if (pagesContent == null) {
//                            System.out.printf("Missing page data for %s in %s %n", textWork.IRI, IRI);
//                        }
//                    }
//                }
//
//                String textName = getId();
//                if (titleAsFilename) {
//                    textName = getTitle();
//                }
//
//                if (totalVolumes > 1) textName += "_vol_" + volumeNumber;
//                MarkdownDocument document = new MarkdownDocument(textSb.toString(), textName);
//                markdownDocuments.add(document);
//            } else {
//                String textName = getId();
//                if (titleAsFilename) {
//                    textName = getTitle();
//                }
//                if (totalVolumes > 1) textName += "_vol_" + volumeNumber;
//                MarkdownDocument document = new MarkdownDocument(etext.generateMarkdown(), textName);
//                markdownDocuments.add(document);
//            }
//        }
//
//        return markdownDocuments;
//    }

//    protected String splitMarkdownText(String text, int sectionSize)
//    {
//        StringBuilder textSb = new StringBuilder();
//        List<String> textLines = Arrays.asList(text.split("\n"));
//        int sectionLength = 0;
//        for (String line: textLines) {
//            if (sectionLength > sectionSize) {
//                textSb.append("\n\n").append("### {.empty}").append("\n\n");
//                sectionLength = 0;
//            }
//            textSb.append(line);
//            sectionLength += line.length();
//        }
//
//        return textSb.toString();
//    }

    protected Work getWork()
    {
        if (work == null) {
            RDFResource workResource = item.getPropertyResource(CORE+"itemForWork");
            if (workResource == null) {
                workResource = item.getPropertyResource(CORE+"itemEtextPaginatedForWork");
            }
            if (workResource != null) {
                workResource = dataSource.loadResource(workResource.getIRI());
                work = new Work(workResource, dataSource);
            }
        }

        return work;
    }

    protected String getTitle()
    {
        if (work == null) {
            return null;
        }

        return work.getTitle();
    }

    public Map<Integer, Etext> getEtexts()
    {
        Map<Integer, RDFResource> texts = getTexts();
        Map<Integer, Etext> etexts = new HashMap<>();
        for (Map.Entry<Integer, RDFResource> entry : texts.entrySet()) {
            RDFResource text = entry.getValue();
            Integer volumeNumber = entry.getKey();
            Etext etext = new Etext(text.getIRI(), this.dataSource, item);
            etexts.put(volumeNumber, etext);
        }

        return etexts;
    }

    private Map<Integer, RDFResource> getTexts()
    {
        List<RDFResource> volumes = getVolumes();
        Map<Integer, RDFResource> texts = new HashMap<>();
        for (RDFResource volume: volumes) {
            Integer volumeInt = volume.getInteger(CORE+"volumeNumber");

            List<RDFResource> volumeTextList = volume.getPropertyResources(CORE+"volumeHasEtext");
            if (volumeTextList != null) {
                for (RDFResource volumeText: volumeTextList) {
                    RDFResource text = volumeText.getPropertyResource(CORE+"eTextResource");
                    if (text != null) {
                        texts.put(volumeInt, text);
                    }
                }
            }
        }

        return texts;
    }

    private List<RDFResource> getVolumes()
    {
        return item.getPropertyResources(CORE+"itemHasVolume");
    }
}
