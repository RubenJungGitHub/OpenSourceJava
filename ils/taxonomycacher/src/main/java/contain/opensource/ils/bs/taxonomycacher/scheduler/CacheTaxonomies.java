package contain.opensource.ils.bs.taxonomycacher.scheduler;

import org.springframework.stereotype.Service;
import contain.opensource.ils.bs.receiver.services.TaxonomyServiceClient;
import contain.opensource.ils.bs.receiver.classes.redis.RedisManager;
import contain.opensource.ils.bs.receiver.model.TaxonomyResponse;

@Service // Mark this as a Spring-managed service
public class CacheTaxonomies {

    private final TaxonomyServiceClient taxonomyServiceClient;
    private final RedisManager redisManager;

    public CacheTaxonomies(TaxonomyServiceClient taxonomyServiceClient, RedisManager redisManager) {
        this.taxonomyServiceClient = taxonomyServiceClient;
        this.taxonomyServiceClient.includeDepricated = true;
        this.redisManager = redisManager;
    }

    public void process() {
        // Now you have access to the service
        // get classifications
         taxonomyServiceClient.includeDepricated = true;
        var classifications = taxonomyServiceClient.GetTaxonomies("classification", "en");
        var markings = taxonomyServiceClient.GetTaxonomies("marking", "en");

        // Update redis

        if (classifications != null && classifications.options != null) {
            for (TaxonomyResponse.Option option : classifications.options) {
                // Example: Key = "taxonomy:classification:c_ab456850", Value = "Confidential"
                String key = "taxonomy:classification:" + option.value;
                String CheckIndRedis = redisManager.getHashField(key, key);
                // if (CheckIndRedis == null || CheckIndRedis.isEmpty()) {
                redisManager.putHash("taxonomies", option.value, option.label, 60);
                // }
                System.out.println("Cached to Redis: " + key + " = " + option.label);
            }

            if (markings != null && markings.options != null) {
                for (TaxonomyResponse.Option option : markings.options) {
                    // Example: Key = "taxonomy:classification:c_ab456850", Value = "Confidential"
                    String key = "taxonomy:markings:" + option.value;
                    String CheckIndRedis = redisManager.getHashField(key, key);
                    // if (CheckIndRedis == null || CheckIndRedis.isEmpty()) {
                    redisManager.putHash("taxonomies", option.value, option.label, 60);
                    // }
                    System.out.println("Cached to Redis: " + key + " = " + option.label);
                }
            }

        }
    }
}