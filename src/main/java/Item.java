import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Item extends BDRCResource {

    private RDFResource item;
    private Map<String, String>distributors;
    private List<String>ocrDistributors;

    public Item(String IRI, DataSource dataSource)
    {
        super(IRI, dataSource);

        item = dataSource.loadResource(IRI);
        resource = item;

        String prefix = "CP";
        distributors = new HashMap<>();
        distributors.put(prefix+"001", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། ལེགས་བཤད་གླིང་།");
        distributors.put(prefix+"002", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། འབྲི་གུང་ཆེ་ཚང་།");
        distributors.put(prefix+"003", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། ནང་བསྟན་སྲི་ཞུ་ཁང་།");
        distributors.put(prefix+"004", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། གུ་རུ་བླ་མ།");
        distributors.put(prefix+"005", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། ཀརྨ་བདེ་ལེགས།");
        distributors.put(prefix+"006", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། དཔལ་རི་པར་ཁང་།");
        distributors.put(prefix+"007", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། ཞེ་ཆེན་དགོན།");
        distributors.put(prefix+"008", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། སྤྲུལ་སྐུ་གསང་སྔགས།");
        distributors.put(prefix+"009", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། རྦུར་ཁུ་ལེ་སློབ་ཆེན།");
        distributors.put(prefix+"010", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། བཛྲ་བིདྱཱ།");
        distributors.put(prefix+"011", "ཡི་གེ་གཏགས་མའི་ཡོང་ཁུངས། མི་གསལ།");

        ocrDistributors = new ArrayList<>();
        ocrDistributors.add(prefix+"009");
    }

    protected Work getWork()
    {
        if (work == null) {
            RDFResource workResource = item.getPropertyResource(CORE+"itemForWork");
            if (workResource == null) {
                workResource = item.getPropertyResource(CORE+"itemEtextPaginatedForWork");
                if (workResource == null) {
                    workResource = item.getPropertyResource(CORE+"itemEtextNonPaginatedForWork");
                }
            }
            if (workResource != null) {
                workResource = dataSource.loadResource(workResource.getIRI());
                work = new Work(workResource, dataSource);
            }
        }

        return work;
    }

    protected String getTitle()
    {
        if (work == null) {
            return null;
        }

        return work.getTitle();
    }

    public Map<Integer, Etext> getEtexts()
    {
        Map<Integer, RDFResource> texts = getTexts();
        Map<Integer, Etext> etexts = new HashMap<>();
        for (Map.Entry<Integer, RDFResource> entry : texts.entrySet()) {
            RDFResource text = entry.getValue();
            Integer volumeNumber = entry.getKey();
            Etext etext = new Etext(text.getIRI(), this.dataSource, item);
            etexts.put(volumeNumber, etext);
        }

        return etexts;
    }

    private Map<Integer, RDFResource> getTexts()
    {
        List<RDFResource> volumes = getVolumes();
        Map<Integer, RDFResource> texts = new HashMap<>();
        for (RDFResource volume: volumes) {
            Integer volumeInt = volume.getInteger(CORE+"volumeNumber");

            List<RDFResource> volumeTextList = volume.getPropertyResources(CORE+"volumeHasEtext");
            if (volumeTextList != null) {
                for (RDFResource volumeText: volumeTextList) {
                    RDFResource text = volumeText.getPropertyResource(CORE+"eTextResource");
                    if (text != null) {
                        texts.put(volumeInt, text);
                    }
                }
            }
        }

        return texts;
    }

    private List<RDFResource> getVolumes()
    {
        return item.getPropertyResources(CORE+"itemHasVolume");
    }

    public String getDistributorId()
    {
        RDFResource distributor = item.getPropertyResource(CORE+"eTextDistributor");
        Path iriPath = Paths.get(distributor.getIRI());
        Path lastSegment = iriPath.getName(iriPath.getNameCount() - 1);
        String id = lastSegment.toString();

        return id;
    }

    public String getDistributor()
    {
        String id = getDistributorId();
        String name = distributors.get(id);

        if (name != null) {
            return name;
        } else {
            return null;
        }
    }

    public boolean isOcr()
    {
        String id = getDistributorId();
        return ocrDistributors.contains(id);
    }
}
