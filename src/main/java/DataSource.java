import java.util.List;

public interface DataSource {
    RDFResource loadResource(String IRI);
    String loadTextContent(String IRI);
    List<String> loadTextContentLines(String IRI);
}
