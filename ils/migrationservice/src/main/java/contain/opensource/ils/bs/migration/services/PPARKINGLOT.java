package contain.opensource.ils.bs.migration.services;
import java.util.concurrent.BlockingQueue;
import  java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import contain.opensource.ils.bs.migration.models.MigrationRequest;

public class PPARKINGLOT {

    private final BlockingQueue<MigrationRequest> queue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, Semaphore> ioTokens = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public PPARKINGLOT(int workerCount) {
        // Start worker threads
        executor = Executors.newFixedThreadPool(workerCount);
        for (int i = 0; i < workerCount; i++) {
            executor.submit(this::processQueue);
        }
    }

    public void submitMigration(MigrationRequest request) {
        queue.add(request); // will wait in line automatically
    }

    private void processQueue() {
        while (true) {
            try {
                // Take next migration request (blocks if queue is empty)
                MigrationRequest request = queue.take();
                Semaphore token          = null;
                // Generate a token key per IO + platform pair
                //String key = request.getIoId() + ":" + request.getFromPlatform() + "->" + request.getToPlatform();
                //token = ioTokens.computeIfAbsent(key, k -> new Semaphore(1));

                // Acquire the semaphore (blocks until available)
                token.acquire();
                try {
                    performMigration(request); // your actual migration logic
                } finally {
                    token.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // exit worker if interrupted
            } catch (Exception e) {
                e.printStackTrace(); // log failures
            }
        }
    }

    private void performMigration(MigrationRequest request) {
        // Actual migration logic: Alfresco <-> SharePoint etc.
   //     System.out.println("Migrating IO " + request.getIoId() + " from "
   //             + request.getFromPlatform() + " to " + request.getToPlatform());
        // simulate work
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}