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
}
