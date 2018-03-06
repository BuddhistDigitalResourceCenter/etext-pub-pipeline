import io.bdrc.ewtsconverter.EwtsConverter;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;

public class RDFLiteral implements RDFProperty {

    Literal literal;
    OntModel ontModel;
    private EwtsConverter wc;

    static final String WYLIE_LANG_CODE = "bo-x-ewts";

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

    private EwtsConverter getEwtsConverter()
    {
        if (wc == null) {
            wc = new EwtsConverter();
        }

        return wc;
    }

    public String getString()
    {
        if (language().equals(WYLIE_LANG_CODE)) {
            String wylie = literal.getString();
            String tibetan = getEwtsConverter().toUnicode(wylie);
            return tibetan;
        } else {
            return literal.getString();
        }
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
