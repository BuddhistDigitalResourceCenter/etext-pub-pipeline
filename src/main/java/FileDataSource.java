import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileDataSource implements DataSource {

    private static String ontologyFileName = "bdrc.owl";
    private String dataPath;

    /**
     *
     * @param dataPath Directory data is stored - must end with a slash.
     */
    public FileDataSource(String dataPath)
    {
        this.dataPath = dataPath;
    }

    @Override
    public RDFResource loadResource(String IRI)
    {
        String id = RDFUtil.getId(IRI);
        RDFModel model = new RDFModel(getDataFilePath(id), getOntologyPath());
        RDFResource resource = model.getResource(IRI);

        return resource;
    }

    @Override
    public String loadTextContent(String IRI)
    {
        List<String> lines = loadTextContentLines(IRI);
        if (lines != null) {
            return String.join("", lines);
        } else {
            return "";
        }
    }

    @Override
    public List<String> loadTextContentLines(String IRI)
    {
        String id = RDFUtil.getId(IRI);
        String textContentPath = getTextFilePath(id);
        List<String> textContentLines;
        try {
            textContentLines = Files.readAllLines(
                    Paths.get(textContentPath), StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            System.out.println("Error loading text content at " + textContentPath);
            return null;
        }

        return textContentLines;
    }

    private String getDataFilePath(String id)
    {
        return this.dataPath + id + ".ttl";
    }

    private String getOntologyPath()
    {
        return this.dataPath + ontologyFileName;
    }

    private String getTextFilePath(String id)
    {
        return this.dataPath + id + ".txt";
    }
}
