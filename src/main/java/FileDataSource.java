import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileDataSource implements DataSource {

    private static String ontologyFileName = "bdrc.owl";
    private String dataPath;
    private MessageDigest messageDigest;
    private Map<String, String> resourceTypes;
    private Pattern resourceTypePattern;

    /**
     *
     * @param dataPath Directory data is stored - must end with a slash.
     */
    public FileDataSource(String dataPath)
    {
        this.dataPath = dataPath;
        try {
            this.messageDigest = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println("Can't get MD5 instance");
        }

        resourceTypes = new HashMap<>();
        resourceTypes.put("C", "corporations");
        resourceTypes.put("UT", "etexts");
        resourceTypes.put("I", "items");
        resourceTypes.put("L", "lineages");
        resourceTypes.put("R", "offices");
        resourceTypes.put("P", "persons");
        resourceTypes.put("G", "places");
        resourceTypes.put("PR", "products");
        resourceTypes.put("T", "topics");
        resourceTypes.put("W", "works");

        resourceTypePattern = Pattern.compile("^([A-Z]{0,2})");
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

    private String getResourceTypeDir(String idType)
    {
        return resourceTypes.get(idType);
    }

    private String getMd5(String message)
    {
        byte[] idBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] digest = messageDigest.digest(idBytes);
        BigInteger bigInt = new BigInteger(1,digest);
        String digestString = String.format("%032x", bigInt);

        return digestString;
    }

    private String getParentDir(String id)
    {
        String idBase = id;
        final int underscoreIndex = idBase.indexOf('_');
        if (underscoreIndex != -1) {
            idBase = id.substring(0, underscoreIndex);
        }

        String digestString = getMd5(idBase);
        String subDir = digestString.substring(0, 2);

        return subDir;
    }

    private String getResourceParentDir(String id)
    {
        Matcher matcher = resourceTypePattern.matcher(id);
        String idType;
        if (matcher.find()) {
            idType = matcher.group(1);
        } else {
            idType = "";
        }
        String mainDir = getResourceTypeDir(idType);
        String subDir = getParentDir(id);

        return dataPath + mainDir + "/" + subDir;
    }

    private String getDataFilePath(String id)
    {
        String resourceDir = getResourceParentDir(id);
        return resourceDir + "/" + id + ".ttl";
    }

    private String getOntologyPath()
    {
        return this.dataPath + ontologyFileName;
    }

    private String getTextFilePath(String id)
    {
        String subDir = getParentDir(id);
        return this.dataPath + "etextcontents/" + subDir + "/" + id + ".txt";
    }
}
