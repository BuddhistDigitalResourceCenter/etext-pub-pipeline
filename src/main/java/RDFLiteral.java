import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;

public class RDFLiteral implements RDFProperty {

    Literal literal;
    OntModel ontModel;

    @Override
    public String getType() {
        return literal.getDatatypeURI();
    }

    @Override
    public OntModel getOntModel()
    {
        return ontModel;
    }

    public RDFLiteral(Literal literal, OntModel ontModel)
    {
        this.literal = literal;
        this.ontModel = ontModel;

    }

    public String getString()
    {
        return literal.getString();
    }

    public String language()
    {
        return literal.getLanguage();
    }

    @Override
    public boolean isLiteral()
    {
        return true;
    }

    @Override
    public RDFResource asResource() {
        return null;
    }

    @Override
    public RDFLiteral asLiteral() {
        return this;
    }
}
