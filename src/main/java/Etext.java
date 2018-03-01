import org.apache.jena.ontology.OntModel;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.io.InputStream;
import java.io.FileOutputStream;


import io.bdrc.xmltoldmigration.xml2files.TibetanStringChunker;

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

    public Etext(String IRI, DataSource dataSource)
    {
        this(IRI, dataSource, null);
    }

    public Etext(String IRI, DataSource dataSource, RDFResource item) {
        super(IRI, dataSource);

        this.etext = dataSource.loadResource(IRI);
        this.resource = this.etext;
        this.item = item;
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
        System.out.println("title: " + title);
        RDFResource author = null;
        author = getMainAuthor();
        String name = null;
        if (author != null) {
            name = getPrimaryName(author, PREFERRED_LANGUAGE);
        }

        String content;

        List<EtextPage> pages = getPages();
        if (pages.size() > 0) {
            List<String> contentLines = getContentLines();
            HashMap<String, EtextPage> pageData = getPageData(pages);
            int currentLine = 0;
            StringBuilder contentSb = new StringBuilder();
            for (String line: contentLines) {
                currentLine++;
                int currentChar = 0;
                for (int c: line.codePoints().toArray()) {
                    currentChar++;
                    String key = String.valueOf(currentLine + "_" + currentChar);
                    if (pageData.containsKey(key)) {
                        EtextPage page = pageData.get(key);
                        contentSb.append(" \\[").append(page.page).append("\\] ");
                    }
                    contentSb.appendCodePoint(c);
                }
                contentSb.append("\n");
            }

            content = contentSb.toString();
        } else {
            content = getContent();
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

        if (limitSectionSize) {
            List<Integer>[] breaks = TibetanStringChunker.getAllBreakingCharsIndexes(content);
            int prevBreakIndex = 0;
            int prevIndex = 0;
            sb.append("\n\n## The Text\n\n");
            for (final int charBreakIndex : breaks[0]) {
                if (charBreakIndex - prevBreakIndex > maxSectionSize) {
//                    String sectionText = content.substring(prevBreakIndex, charBreakIndex);
                    prevBreakIndex = charBreakIndex;
                    sb.append("\n\n### {.empty}\n\n");
                }
                String breakText = content.substring(prevIndex, charBreakIndex);
                sb.append(breakText).append("\n");
                prevIndex = charBreakIndex;
            }
        } else {
            sb.append(content);
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
                sb.append("**").append(key).append("**\n");
                List<String> keyItems = item.get(key);
                for (String keyItem: keyItems) {
                    sb.append(keyItem).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    // TODO: use page-break attribute?
    // See: http://sketchytech.blogspot.co.nz/2017/01/when-is-page-break-not-page-break-epub.html
    public String getContentWithPages(List<EtextPage> pages) {
        String content;
        if (pages.size() > 0) {
            List<String> contentLines = getContentLines();
            HashMap<String, EtextPage> pageData = getPageData(pages);
            int currentLine = 0;
            StringBuilder contentSb = new StringBuilder();
            for (String line: contentLines) {
                currentLine++;
                int currentChar = 0;
                for (int c: line.codePoints().toArray()) {
                    currentChar++;
                    String key = String.valueOf(currentLine + "_" + currentChar);
                    if (pageData.containsKey(key)) {
                        EtextPage page = pageData.get(key);
                        contentSb.append(" \\[").append(page.page).append("\\] ");
                    }
                    contentSb.appendCodePoint(c);
                }
                contentSb.append("\n");
            }

            content = contentSb.toString();
        } else {
            content = getContent();
        }

        return content;
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

    protected RDFResource getWork()
    {
        if (work == null && getItem() != null) {
            work = item.getPropertyResource(CORE+"itemForWork");
            if (work != null) {
                work = dataSource.loadResource(work.getIRI());
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
        List<RDFResource> pages = etext.getPropertyResources(CORE+"eTextHasPage");
        List<EtextPage> etextPages = new ArrayList<>();

        for(RDFResource page: pages) {
            EtextPage etextPage = new EtextPage();
            etextPage.page = page.getInteger(CORE+"seqNum");
            etextPage.startChar = page.getInteger(CORE+"sliceStartChar");
            etextPage.startLine = page.getInteger(CORE+"sliceStartChunk");
            etextPage.endChar = page.getInteger(CORE+"sliceEndChar");
            etextPage.endLine = page.getInteger(CORE+"sliceEndChunk");

            etextPages.add(etextPage);
        }

        return etextPages;
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

        RDFResource work = getWork();
        if (work != null) {
            String[] IRIs = {
                    CORE+"workCatalogInfo"
            };

            for (String propIRI: IRIs) {
                List<RDFProperty> props = work.getProperties(propIRI);

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

    private List<String> getContentLines()
    {
        return dataSource.loadTextContentLines(IRI);
    }
}
