import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.stream.Collectors.toList;

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

    public Etext(String IRI, DataSource dataSource)
    {
        this(IRI, dataSource, null);
    }

    public Etext(String IRI, DataSource dataSource, RDFResource item) {
        super(IRI, dataSource);

        this.etext = dataSource.loadResource(IRI);
        this.item = item;
    }

    public String generateMarkdown()
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
        sb.append(content);

        return sb.toString();
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
            work = dataSource.loadResource(work.getIRI());
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
    private String getContent()
    {
        return dataSource.loadTextContent(IRI);
    }

    private List<String> getContentLines()
    {
        return dataSource.loadTextContentLines(IRI);
    }
}
