import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntResource;

import java.net.URI;

public class RDFUtil {

    public static String getId(String IRI)
    {
        String id = null;
        try {
            URI uri = new URI(IRI);
            String uriPath = uri.getPath();
            String[] components = uriPath.split("/");
            id = components[components.length - 1];
        } catch (Exception e) {}

        return id;
    }

    public static String getReadableName(String IRI)
    {
        String name = getId(IRI);

        return name;
    }

    public static String getOntologyLabel(OntModel ontModel, String IRI)
    {
        OntResource propResource = ontModel.getOntResource(IRI);
        String label = getReadableName(IRI);
        if (propResource != null) {
            label = propResource.getLabel("en");
        }

        return label;
    }
}
