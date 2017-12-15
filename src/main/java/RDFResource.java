import org.apache.jena.rdf.model.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class RDFResource implements RDFProperty {

    private Resource resource;
    private Model model;

    static final String TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public RDFResource(Resource resource)
    {
        this.resource = resource;
        model = resource.getModel();
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

    public List<RDFProperty> getProperties(String IRI)
    {
        Property prop = property(IRI);
        if (resource.hasProperty(prop)) {
            List<RDFProperty> properties = new ArrayList<>();
            StmtIterator statements = resource.listProperties(prop);
            while(statements.hasNext()) {
                Statement statement = statements.nextStatement();

                RDFProperty property;
                if (statement.getObject().isLiteral()) {
                    property = new RDFLiteral(statement.getLiteral());
                } else {
                    property = new RDFResource(statement.getResource());
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
                    if (preferredLanguage == null || literal.language().equals(preferredLanguage)) {
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
