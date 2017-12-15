import java.io.FileNotFoundException;
import java.util.List;

import static java.util.stream.Collectors.toList;

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

        String content = getContent();

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
            item = dataSource.loadResource(item.getIRI());
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

    private String getContent()
    {
        return dataSource.loadTextContent(IRI);
    }
}
