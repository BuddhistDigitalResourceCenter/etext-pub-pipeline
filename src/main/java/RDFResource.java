import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class RDFResource implements RDFProperty {

    private Resource resource;
    private Model model;
    private OntModel ontModel;

    private static final String TIBETAN_LANG_CODE = "bo";
    private static final String WYLIE_LANG_CODE = "bo-x-ewts";
    static final String TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public RDFResource(Resource resource, OntModel ontModel)
    {
        this.resource = resource;
        model = resource.getModel();
        this.ontModel = ontModel;
    }

    @Override
    public String getType()
    {
        return getTypeIRI();
    }

    @Override
    public OntModel getOntModel()
    {
        return ontModel;
    }

    public String getTypeIRI()
    {
        String typeIRI = null;
        RDFResource type = getPropertyResource(TYPE);
        if (type != null) {
            typeIRI = type.getIRI();
        }

        return typeIRI;
    }

    private Property property(String IRI)
    {
        return resource.getModel().getProperty(IRI);

    }

    public List<RDFProperty> getAllProperties()
    {
        List<RDFProperty> properties = new ArrayList<>();
        StmtIterator statements = resource.listProperties();
        while(statements.hasNext()) {
            Statement statement = statements.nextStatement();

            RDFProperty property;
            if (statement.getObject().isLiteral()) {
                property = new RDFLiteral(statement.getLiteral(), ontModel);
            } else {
                property = new RDFResource(statement.getResource(), ontModel);
            }

            properties.add(property);
        }

        return properties;
    }

    public List<RDFProperty> getProperties(String IRI)
    {
        Property prop = property(IRI);
        if (resource.hasProperty(prop)) {
            List<RDFProperty> properties = new ArrayList<>();
            StmtIterator statements = resource.listProperties(prop);
            while(statements.hasNext()) {
                Statement statement = statements.nextStatement();
//                System.out.println(statement.getPredicate());
                RDFProperty property;
                if (statement.getObject().isLiteral()) {
                    property = new RDFLiteral(statement.getLiteral(), ontModel);
                } else {
                    property = new RDFResource(statement.getResource(), ontModel);
                }

                properties.add(property);
            }

            return properties;
        }

        return null;
    }

    public List<RDFResource> getPropertyResources(String IRI)
    {
        List<RDFResource> resources = new ArrayList<>();
        List<RDFProperty> properties = getProperties(IRI);
        if (properties != null) {
            for (RDFProperty property : properties) {
                if (!property.isLiteral()) {
                    resources.add(property.asResource());
                }
            }
        }

        return resources;
    }

    public RDFResource getPropertyResource(String IRI)
    {
        List<RDFResource> resources = getPropertyResources(IRI);
        if (resources.size() > 0) {
            return resources.get(0);
        } else {
            return null;
        }
    }

    public Integer getInteger(String IRI)
    {
        String value = getString(IRI);
        if (value == null) {
            return null;
        } else {
            return new Integer(value);
        }
    }

    public String getString(String IRI)
    {
        return getString(IRI, null);
    }

    public String getString(String IRI, String preferredLanguage)
    {
        if (hasLiteral(IRI)) {

            List<RDFProperty> properties = getProperties(IRI);
            RDFLiteral literal = null;
            for (RDFProperty prop : properties) {
                if (prop.isLiteral()) {
                    literal = prop.asLiteral();
                    if (preferredLanguage == null
                        || isPreferredLanguage(literal.language(), preferredLanguage)
                    ) {
                        return literal.getString();
                    }
                }
            }

            if (literal != null) {
                return literal.getString();
            }
        }

        return null;
    }

    private boolean isPreferredLanguage(String language, String preferredLanguage)
    {
        if (language.equals(preferredLanguage)) {
            return true;
        } else if (language.equals(WYLIE_LANG_CODE) && preferredLanguage.equals(TIBETAN_LANG_CODE)) {
            return  true;
        } else {
            return false;
        }
    }

    public String getIRI()
    {
        return resource.getURI();
    }

    public boolean hasLiteral(String IRI)
    {
        if (resource.hasProperty(property(IRI))) {
            List<RDFProperty> props = getProperties(IRI);
            for (RDFProperty prop : props) {
                if (prop.isLiteral()) {
                    return true;
                }
            }
        }

        return false;
    }

    public void allStatements()
    {
        StmtIterator statements = model.listStatements();
        while(statements.hasNext()) {
            System.out.println(statements.nextStatement());
        }
    }

    @Override
    public boolean isLiteral()
    {
        return false;
    }

    @Override
    public RDFResource asResource() {
        return this;
    }

    @Override
    public RDFLiteral asLiteral() {
        return null;
    }
}
