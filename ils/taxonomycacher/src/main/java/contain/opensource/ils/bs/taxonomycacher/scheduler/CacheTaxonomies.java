package contain.opensource.ils.bs.taxonomycacher.scheduler;
import org.springframework.stereotype.Service;
import contain.opensource.ils.bs.receiver.services.TaxonomyServiceClient;

@Service // Mark this as a Spring-managed service
public class CacheTaxonomies {

    private final TaxonomyServiceClient taxonomyServiceClient;

    public CacheTaxonomies(TaxonomyServiceClient taxonomyServiceClient) {
        this.taxonomyServiceClient = taxonomyServiceClient;
    }

    public void process() {
        // Now you have access to the service
        //get classifications
        var classifications = taxonomyServiceClient.GetTaxonomies("classification","en");
        var markings = taxonomyServiceClient.GetTaxonomies("marking","en");

        //To do update redis 

    }
}