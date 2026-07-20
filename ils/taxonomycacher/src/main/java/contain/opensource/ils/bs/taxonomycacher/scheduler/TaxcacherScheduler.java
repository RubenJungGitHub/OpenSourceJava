package contain.opensource.ils.bs.taxonomycacher.scheduler;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

@Component
public class TaxcacherScheduler {

    // ANSI codes voor de opmaak
    String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    String ANSI_BLACK_TEXT = "\u001B[30m";
    String ANSI_RESET = "\u001B[0m";
    private final CacheTaxonomies cacheTaxonomies;

    public TaxcacherScheduler(CacheTaxonomies cacheTaxonomies) {
        this.cacheTaxonomies = cacheTaxonomies;
    }

     @Scheduled(fixedDelay = 600_000) //-> 10 minutes
    //@Scheduled(fixedDelay = 10_000)
    public void maintain() {
        try {

            System.out.println(ANSI_GREEN_BACKGROUND + ANSI_BLACK_TEXT +
                    "[CONSOLE] [NEW CACHE] Interval voor Taxcacher is bereikt. Uitvoeren van caching..." +
                    ANSI_RESET);

                    cacheTaxonomies.process();

        } catch (Exception e) {
            System.err.println("Subscription maintenance failure " + e.getMessage());
        }
    }
}
