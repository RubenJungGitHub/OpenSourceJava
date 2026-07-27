package contain.opensource.ils.bs.taxonomycacher.scheduler;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TaxcacherScheduler {

    // ANSI codes voor de opmaak
    String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    String ANSI_BLACK_TEXT = "\u001B[30m";
    String ANSI_RESET = "\u001B[0m";
    private final CacheTaxonomies cacheTaxonomies;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final long interVal = 600_000; // 10 minutes in milliseconds

    public TaxcacherScheduler(CacheTaxonomies cacheTaxonomies) {
        this.cacheTaxonomies = cacheTaxonomies;
    }

    @Scheduled(fixedDelay = interVal) // -> 10 minutes
    // @Scheduled(fixedDelay = 10_000)
    public void maintain() {
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

            System.out.println(ANSI_GREEN_BACKGROUND + ANSI_BLACK_TEXT +
                    "[" + timestamp + "] [CONSOLE] [NEW CACHE] Interval of " + interVal / 1000
                    + " seconds voor Taxcacher is bereikt. Uitvoeren van caching..." +
                    ANSI_RESET);

            cacheTaxonomies.process();
        } catch (Exception e) {
            System.err.println("Subscription maintenance failure " + e.getMessage());
        }
    }
}
