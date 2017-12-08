import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

interface RDFProperty
{
    public boolean isLiteral();
    public RDFLiteral asLiteral();
    public RDFResource asResource();
}

public class RDFModel {
    private Model model;

    public RDFModel(String modelPath)
    {
        model = ModelFactory.createDefaultModel();
        model.read(modelPath);

//        ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
//        ontologyModel.read(ontologyPath);
    }

    public RDFResource getResource(String IRI)
    {
        Resource resource = model.getResource(IRI);

        if (resource == null) {
            return null;
        }

        RDFResource rdfResource = new RDFResource(resource);

        return rdfResource;
    }
}
