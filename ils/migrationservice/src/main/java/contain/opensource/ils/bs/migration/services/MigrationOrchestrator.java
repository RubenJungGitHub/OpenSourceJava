package contain.opensource.ils.bs.migration.services;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import contain.opensource.ils.bs.migration.models.MigrationRequest;
import contain.opensource.ils.bs.receiver.classes.alfresco.AlfrescoNodeController;
import contain.opensource.ils.bs.receiver.services.GraphService;
import jakarta.annotation.PostConstruct;

@Component
public class MigrationOrchestrator {

    private final BlockingQueue<MigrationRequest> queue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, Semaphore> ioTokens = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(5); // adjustable

    @PostConstruct
    public void startWorker() {
        executor.submit(() -> {
            while (true) {
                //sHOULD BE PER PLATFORM 
                MigrationRequest request = queue.take(); // blocks if empty
                Semaphore token = ioTokens.computeIfAbsent(request.getIoId(), k -> new Semaphore(1));
                token.acquire(); // wait if another migration is in progress
                try {
                    performMigration(request);
                } finally {
                    token.release();
                }
            }
        });
    }

    public void submitMigration(MigrationRequest request) {
        queue.offer(request);
    }

    private void performMigration(MigrationRequest request) {
        // Decide source/target controller
       // if (request.getSourcePlatform() == Platform.ALFRESCO) {
       //     alfrescoController.RelocateIO(request);
       // } else {
       //     spController.RelocateIO(request);
       // }
    }

    @Autowired @Lazy private AlfrescoNodeController alfrescoController;
    @Autowired @Lazy private GraphService spController;
}

