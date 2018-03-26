import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class WorkLocation {
    int volume;
    int startPage;
    int endPage;
}

class WorkSection {
    Work work;
    String title;
    String content;
    String author;
    int volume;
    WorkLocation location;
    List<WorkSection> sections;

    public String toString()
    {
        String sectionsString = "";
        if (sections!= null && sections.size() > 0) sectionsString = sections.toString();
        return super.toString() + ", sections: " + sectionsString + "\n";
    }
}

public class Work extends BDRCResource {

    private RDFResource work;

    public Work(String IRI, DataSource dataSource)
    {
        super(IRI, dataSource);

        work = dataSource.loadResource(IRI);
        resource = work;
    }

    public Work(RDFResource work, DataSource dataSource)
    {
        super(work.getIRI(), dataSource);

        this.work = work;
        resource = work;
    }

    @Override
    Work getWork() {
        return this;
    }

    @Override
    String getTitle() {
        return work.getString(SKOS.concat("prefLabel"));
    }

    /**
     *
     * @return Volume number is the key
     */
    public Map<Integer, List<Work>> getWorkParts()
    {
        List<RDFResource> workPartResources = work.getPropertyResources(CORE+"workHasPart");
        HashMap<Integer, List<Work>> workParts = new HashMap<>();

        for (RDFResource workPartResource: workPartResources) {
            Work workPart = new Work(workPartResource, dataSource);
            WorkLocation location = workPart.getLocation();
            if (location != null) {
                List<Work> volumeParts = workParts.get(location.volume);
                if (volumeParts == null) {
                    volumeParts = new ArrayList<>();
                }
                volumeParts.add(workPart);
                workParts.put(location.volume, volumeParts);
            }
        }

        return workParts;
    }

    public WorkLocation getLocation()
    {
        WorkLocation location = null;
        RDFResource locationData = work.getPropertyResource(CORE+"workLocation");
        if (locationData != null) {
            location = new WorkLocation();
            try {
                location.startPage = locationData.getInteger(CORE + "workLocationPage");
            } catch (Exception e) {}
            try {
                location.endPage = locationData.getInteger(CORE+"workLocationEndPage");
            } catch (Exception e) {
                location.endPage = location.startPage;
            }
            try {
                location.volume = locationData.getInteger(CORE + "workLocationVolume");
            } catch (Exception e) {
                location.volume = 1;
            }
        }

        return location;
    }

    public RDFResource getMainAuthor()
    {
        RDFResource author = work.getPropertyResource(CORE+"creatorMainAuthor");
        if (author != null) {
            try {
                author = dataSource.loadResource(author.getIRI());
            } catch (Exception e) {
                author = null;
            }
        }

        return author;
    }

    public List<WorkSection> getSections(Etext etext, int volume)
    {
        List<WorkSection> sections = new ArrayList<>();
        Map<Integer, List<Work>> workParts = getWorkParts();
        List<Work> works = null;
        if (workParts != null) works = workParts.get(volume);
        String author = getAuthor();

        if (works != null && works.size() > 0) {
            for (Work textWork : works) {
                WorkSection section = new WorkSection();

                WorkLocation location = textWork.getLocation();
                section.location = location;
                Map<Integer, String> pagesContent = etext.getPageContent();
                if (location != null && pagesContent != null) {
                    if (pagesContent.containsKey(location.startPage) &&
                            pagesContent.containsKey(location.endPage)) {
                        StringBuilder sb = new StringBuilder();
                        for(int i=location.startPage; i < location.endPage; i++) {
                            sb.append(pagesContent.get(i));
                        }
                        String content = sb.toString();
                        String title = textWork.getTitle();
                        section.content = content;
                        section.title = title;
                        List<WorkSection> workSections = new Work(textWork.work, dataSource).getSections(etext, volume);
                        if (workSections.size() > 0) {
                            section.sections = workSections;
                        }

                        sections.add(section);

                        String textWorkAuthor = textWork.getAuthor();
                        if (author == null || (textWorkAuthor != null && !author.equals(textWorkAuthor))) {
                            section.author = textWorkAuthor;
                        }
                    }
                } else {
                    if (location == null) {
                        System.out.printf("Missing location for work: %s %n", textWork.IRI);
                    }
                    if (pagesContent == null) {
                        System.out.printf("Missing page data for work: %s %n", textWork.IRI);
                    }
                }
            }
        }

        return sections;
    }
}
