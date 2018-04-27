import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.service.MediatypeService;
import org.apache.jena.ontology.OntModel;

import java.util.*;


class EtextPage {
    int page;
    int startChar;
    int startLine;
    int endChar;
    int endLine;
}

public class Etext extends BDRCResource {

    private RDFResource etext;
    private RDFResource item;
    private static int maxSectionSize = 10000;
    private List<EtextPage> pages;

    public Etext(String IRI, DataSource dataSource)
    {
        this(IRI, dataSource, null);
    }

    public Etext(String IRI, DataSource dataSource, RDFResource item) {
        super(IRI, dataSource);

        this.etext = dataSource.loadResource(IRI);
        this.resource = this.etext;
        this.item = item;
        this.pages = null;
    }

    public String generateMarkdown() {
        return generateMarkdown(true);
    }

    public String generateMarkdown(boolean limitSectionSize)
    {
        if (etext == null) {
            return null;
        }

        String title = getTitle();
        RDFResource author = null;
        author = getMainAuthor();
        String name = null;
        if (author != null) {
            name = getPrimaryName(author, PREFERRED_LANGUAGE);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        if (name != null) {
            sb.append("## ").append(name).append(" {.author}").append("\n\n");
        }

        String metadata = generateMetadataMarkdown();
        if (metadata != null) {
            sb.append(metadata).append("\n\n");
        }

        List<String> contentLines = getContentLines();
        List<EtextPage> pages = getPages();
        if (pages != null && pages.size() > 0) {
            contentLines = getContentLinesWithPages(contentLines, pages);
        }

        if (limitSectionSize) {
            sb.append("\n\n## The Text {.enHeader}\n\n");
            int sectionLength = 0;
            for (String line: contentLines) {
                if (sectionLength > maxSectionSize) {
                    sectionLength = 0;
                    sb.append("\n\n### {.empty}\n\n");
                }
                sb.append(line).append("\n");
                sectionLength += line.length();
            }
        } else {
            sb.append(String.join("\n", contentLines));
        }

        return sb.toString();
    }

    public String generateMetadataMarkdown()
    {
        List<HashMap<String, List<String>>> metadataItems = getMetadata();
        if (metadataItems.size() == 0) {
            System.out.println("No metadata for " + IRI);
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## Metadata").append("\n\n");
        for (HashMap<String, List<String>> item: metadataItems) {
            for (String key: item.keySet()) {
                String heading = capitalizeFirstLetter(key);
                sb.append("**").append(heading).append("**\n\n");
                List<String> keyItems = item.get(key);
                for (String keyItem: keyItems) {
                    sb.append(keyItem).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String capitalizeFirstLetter(String string)
    {
        return new StringBuilder().append(string.substring(0, 1).toUpperCase())
                .append(string.substring(1))
                .toString();
    }

    // TODO: use page-break attribute?
    // See: http://sketchytech.blogspot.co.nz/2017/01/when-is-page-break-not-page-break-epub.html
    private List<String> getContentLinesWithPages(List<String>contentLines, List<EtextPage> pages)
    {
        List<String> contentPagedLines = new ArrayList<>();
        HashMap<String, EtextPage> pageData = getPageData(pages);
        int currentLine = 0;
        for (String line: contentLines) {
            StringBuilder lineSb = new StringBuilder();
            currentLine++;
            int currentChar = 0;
            for (int c: line.codePoints().toArray()) {
                currentChar++;
                String key = String.valueOf(currentLine + "_" + currentChar);
                if (pageData.containsKey(key)) {
                    EtextPage page = pageData.get(key);
                    lineSb.append(" [\\[").append(page.page).append("\\]]{.origPageNum} ");
                }
                lineSb.appendCodePoint(c);
            }
            contentPagedLines.add(lineSb.toString());
        }

        return contentPagedLines;
    }

    public Map<Integer, String> getPageContent()
    {
        List<EtextPage> pages = getPages();
        if (pages == null) return null;

        HashMap<String, EtextPage> pageData = getPageData(pages);

        HashMap<Integer, String> pageContent = new HashMap<>();
        List<String> contentLines = getContentLines();
        // Note: lines and chars are 1-indexed
        int currentLine = 0;
        int currentPage = 0;
        StringBuilder currentPageData = new StringBuilder();
        for (String line: contentLines) {
            currentLine++;
            int currentChar = 0;
            for (int c: line.codePoints().toArray()) {
                currentChar++;
                String key = String.valueOf(currentLine + "_" + currentChar);
                if (pageData.containsKey(key)) {
                    EtextPage page = pageData.get(key);

                    pageContent.put(currentPage, currentPageData.toString());

                    currentPageData = new StringBuilder();
                    currentPage = page.page;
                }
                currentPageData.appendCodePoint(c);
            }

            currentPageData.append("\n");
        }

        return pageContent;
    }

    private RDFResource getItem()
    {
        if (item == null && etext != null) {
            item = etext.getPropertyResource(CORE+"eTextInItem");
            if (item != null) {
                item = dataSource.loadResource(item.getIRI());
            }
        }

        return item;
    }

    protected Work getWork()
    {
        if (work == null && getItem() != null) {
            RDFResource workResource = item.getPropertyResource(CORE+"itemForWork");
            if (workResource == null) {
                workResource = item.getPropertyResource(CORE+"itemEtextPaginatedForWork");
                if (workResource == null) {
                    workResource = item.getPropertyResource(CORE+"itemEtextNonPaginatedForWork");
                }
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
        String eTextTitleIRI = CORE + "eTextTitle";

        return etext.getString(eTextTitleIRI, PREFERRED_LANGUAGE);
    }

    protected List<EtextPage> getPages()
    {
        if (this.pages == null) {
            List<RDFResource> pages = etext.getPropertyResources(CORE + "eTextHasPage");
            List<EtextPage> etextPages = new ArrayList<>();

            for (RDFResource page : pages) {
                EtextPage etextPage = new EtextPage();
                try {
                    etextPage.page = page.getInteger(CORE + "seqNum");
                    etextPage.startChar = page.getInteger(CORE + "sliceStartChar");
                    etextPage.startLine = page.getInteger(CORE + "sliceStartChunk");
                    etextPage.endChar = page.getInteger(CORE + "sliceEndChar");
                    etextPage.endLine = page.getInteger(CORE + "sliceEndChunk");
                    etextPages.add(etextPage);
                } catch (Exception e) {
                    System.out.println("Exception getting page for " + etext.getIRI());
                    return null;
                }

            }

            etextPages.sort((leftPage, rightPage) -> {
                return Integer.compare(leftPage.page, rightPage.page);
            });

            this.pages = etextPages;
        }

        return this.pages;
    }

    private HashMap<String, EtextPage> getPageData(List<EtextPage> pages)
    {
        HashMap<String, EtextPage> data = new HashMap<>();
        for (EtextPage page: pages) {
            String key = String.valueOf(page.startLine) + "_" + String.valueOf(page.startChar);
            data.put(key, page);
        }

        return data;
    }

    private List<HashMap<String, List<String>>> getMetadata()
    {
        List<HashMap<String, List<String>>> metadataItems = new ArrayList<>();

        Work work = getWork();
        if (work != null) {
            String[] IRIs = {
                    CORE+"workCatalogInfo"
            };

            for (String propIRI: IRIs) {
                List<RDFProperty> props = work.resource.getProperties(propIRI);
                if (props == null) {
                    System.out.println("No workCatalogInfo for work: " + work.resource.getIRI());
                    continue;
                }

                OntModel ontModel = etext.getOntModel();
                String label = RDFUtil.getOntologyLabel(ontModel, propIRI);
                List<String> propItems = new ArrayList<>();
                for (RDFProperty prop: props) {
                    if (prop.isLiteral()) {
                        RDFLiteral lit = prop.asLiteral();
                        propItems.add(lit.getString());
                    } else {
                        RDFResource resource = prop.asResource();
                        for (RDFProperty resProp: resource.getAllProperties()) {
                            if (resProp.isLiteral()) {
                                propItems.add(resProp.asLiteral().getString());
                            } else {
                                String resPropLabel = resProp.asResource().getString(RDFS+"label");
                                if (resPropLabel != null) {
                                    propItems.add(resPropLabel);
                                }
                            }
                        }
                    }
                }
                HashMap<String, List<String>> metadataItem = new HashMap<>();
                metadataItem.put(label, propItems);
                metadataItems.add(metadataItem);
            }

        }

        return metadataItems;
    }


    private String getContent()
    {
        return dataSource.loadTextContent(IRI);
    }

    protected List<String> getContentLines()
    {
        return dataSource.loadTextContentLines(IRI);
    }
}
