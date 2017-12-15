import org.apache.jena.rdf.model.Literal;

public class RDFLiteral implements RDFProperty {

    Literal literal;

    @Override
    public String getType() {
        return literal.getDatatypeURI();
    }

    public RDFLiteral(Literal literal)
    {
        this.literal = literal;
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
