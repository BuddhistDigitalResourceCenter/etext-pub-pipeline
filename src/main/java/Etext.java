import java.io.FileNotFoundException;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class Etext {

    private static final String CORE = "http://purl.bdrc.io/ontology/core/";
    private static final String BDR = "http://purl.bdrc.io/resource/";
    private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String PREFERRED_LANGUAGE = "bo";

    private String IRI;
    private DataSource dataSource;

    public Etext(String IRI, DataSource dataSource)
    {
        this.IRI = IRI;
        this.dataSource = dataSource;
    }

    public String generateMarkdown()
    {
        RDFResource etext = dataSource.loadResource(IRI);
        String title = getTitle();
        String name = getPrimaryName(
                getMainAuthor(
                        getWork(
                                getItem(
                                        etext
                                )
                        )
                ),
                PREFERRED_LANGUAGE
        );
        String content = getContent();

        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(title).append("\n\n");
        sb.append("## ").append(name).append(" {.author}").append("\n\n");
        sb.append(content);

        return sb.toString();
    }

    private RDFResource getEtext()
    {
        return dataSource.loadResource(IRI);
    }

    private RDFResource getItem(RDFResource eText)
    {
        RDFResource item = eText.getPropertyResource(CORE+"eTextInItem");

        return dataSource.loadResource(item.getIRI());
    }

    private RDFResource getWork(RDFResource item)
    {
        RDFResource work = item.getPropertyResource(CORE+"itemForWork");

        return dataSource.loadResource(work.getIRI());
    }

    private RDFResource getMainAuthor(RDFResource work)
    {
        RDFResource author = work.getPropertyResource(CORE+"creatorMainAuthor");

        return dataSource.loadResource(author.getIRI());
    }

    private List<RDFResource> getAuthorNames(RDFResource author)
    {
        return author.getPropertyResources(CORE+"personName");
    }

    private String getPrimaryName(RDFResource author, String preferredLanguage)
    {
        List<RDFResource> authorNames = getAuthorNames(author);
        String primaryNameIRI = CORE+"PersonPrimaryName";
        List<RDFResource> primaryNames = authorNames
                .stream()
                .filter(n -> n.getTypeIRI().equals(primaryNameIRI))
                .collect(toList());

        return primaryNames.get(0).getString(RDFS+"label", preferredLanguage);
    }

    private String getTitle()
    {
        String eTextTitleIRI = CORE + "eTextTitle";

        return getEtext().getString(eTextTitleIRI, PREFERRED_LANGUAGE);
    }

    private String getContent()
    {
        return dataSource.loadTextContent(IRI);
    }
}
