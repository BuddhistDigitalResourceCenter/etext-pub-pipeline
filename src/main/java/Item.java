import java.util.ArrayList;
import java.util.List;

public class Item extends BDRCResource {

    private RDFResource item;

    public Item(String IRI, DataSource dataSource)
    {
        super(IRI, dataSource);

        item = dataSource.loadResource(IRI);
    }

    public String generateMarkdown()
    {
        StringBuilder sb = new StringBuilder();

        String title = getTitle();
        if (title == null) {
            title = IRI;
        }
        sb.append("# ").append(title).append("\n\n");

        String author = getAuthor();
        if (author != null) {
            sb.append("## ").append(author).append(" {.author}").append("\n\n");
        }

        List<Etext> etexts = getEtexts();
        for (Etext etext: etexts) {
            sb.append("\n\n");
            sb.append(etext.generateMarkdown());
        }

        return sb.toString();
    }

    protected RDFResource getWork()
    {
        if (work == null) {
            work = item.getPropertyResource(CORE+"itemForWork");
            work = dataSource.loadResource(work.getIRI());
        }

        return work;
    }

    private List<Etext> getEtexts()
    {
        List<RDFResource> texts = getTexts();
        List<Etext> etexts = new ArrayList<>();
        for (RDFResource text: texts) {
            Etext etext = new Etext(text.getIRI(), this.dataSource, item);
            etexts.add(etext);
        }

        return etexts;
    }

    protected String getTitle()
    {
        RDFResource work = getWork();
        if (work == null) {
            return null;
        }

        String preferredLabel;
        preferredLabel = work.getString(SKOS.concat("prefLabel"));

        return preferredLabel;
    }

    private List<RDFResource> getTexts()
    {
        return getTexts(0);
    }

    private List<RDFResource> getTexts(int volumeNumber)
    {
        List<RDFResource> volumes = getVolumes();
        List<RDFResource> texts = new ArrayList<>();
        for (RDFResource volume: volumes) {
            if (volumeNumber > 0) {
                Integer volumeInt = volume.getInteger(CORE+"volumeNumber");
                if (volumeInt == null || volumeInt != volumeNumber) {
                    continue;
                }
            }

            List<RDFResource> volumeTextList = volume.getPropertyResources(CORE+"volumeHasEtext");
            if (volumeTextList != null) {
                for (RDFResource volumeText: volumeTextList) {
                    RDFResource text = volumeText.getPropertyResource(CORE+"eTextResource");
                    if (text != null) {
                        texts.add(text);
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
}
