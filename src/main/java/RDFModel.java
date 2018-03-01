import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

interface RDFProperty
{
    String getType();
    boolean isLiteral();
    RDFLiteral asLiteral();
    RDFResource asResource();
    OntModel getOntModel();
}

public class RDFModel {
    private Model model;
    private OntModel ontologyModel;

    public RDFModel(String modelPath, String ontologyPath)
    {
        model = ModelFactory.createDefaultModel();
        model.read(modelPath);

        ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        ontologyModel.read(ontologyPath);
    }

    public RDFResource getResource(String IRI)
    {
        Resource resource = model.getResource(IRI);

        if (resource == null) {
            return null;
        }

        RDFResource rdfResource = new RDFResource(resource, ontologyModel);

        return rdfResource;
    }
}
