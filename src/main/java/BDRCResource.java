import java.util.List;

import static java.util.stream.Collectors.toList;

abstract class BDRCResource {

    protected static final String CORE = "http://purl.bdrc.io/ontology/core/";
    protected static final String BDR = "http://purl.bdrc.io/resource/";
    protected static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    protected static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    protected static final String SKOS = "http://www.w3.org/2004/02/skos/core#";
    protected static final String PREFERRED_LANGUAGE = "bo";

    protected String IRI;
    protected DataSource dataSource;
    protected RDFResource work;
    protected RDFResource resource;

    public BDRCResource(String IRI, DataSource dataSource)
    {
        this.IRI = IRI;
        this.dataSource = dataSource;
    }

    protected String getType()
    {
        return resource.getTypeIRI();
    }

    abstract RDFResource getWork();

    abstract String getTitle();

    protected String getAuthor()
    {
        RDFResource mainAuthor = getMainAuthor();
        if (mainAuthor != null) {
            return getPrimaryName(mainAuthor, PREFERRED_LANGUAGE);
        } else {
            return null;
        }
    }

    protected RDFResource getMainAuthor()
    {
        RDFResource work = getWork();
        if (work == null) return null;

        RDFResource author = work.getPropertyResource(CORE+"creatorMainAuthor");
        if (author != null) {
            try {
                author = dataSource.loadResource(author.getIRI());
            } catch (Exception e) {
                author = null;
            }
        }

        return author;
    }

    protected List<RDFResource> getAuthorNames(RDFResource author)
    {
        return author.getPropertyResources(CORE+"personName");
    }

    protected String getPrimaryName(RDFResource author, String preferredLanguage)
    {
        List<RDFResource> authorNames = getAuthorNames(author);
        String primaryNameIRI = CORE+"PersonPrimaryName";
        List<RDFResource> primaryNames = authorNames
                .stream()
                .filter(n -> n.getTypeIRI().equals(primaryNameIRI))
                .collect(toList());

        return primaryNames.get(0).getString(RDFS+"label", preferredLanguage);
    }
}
