package contain.opensource.ils.bs.receiver.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.Optional;

//import contain.opensource.ils.bs.receiver.classes.Logger.IOLogPostgress;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLog;
//import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbak;
import contain.opensource.ils.bs.receiver.classes.Logger.IOLogBallenbakMongo;
import contain.opensource.ils.bs.receiver.classes.redis.RedisManager;

import org.springframework.web.bind.annotation.RequestParam;
@RestController
@RequestMapping("/api") // class-level base path
public class RedisController {
    public RedisController() {
    }

    @GetMapping("/RedisFlush")
    public void RedisFlush() {
        RedisManager.redis.flushall();
    }

    @GetMapping(value = "/GetIOUUIDLatestBindingHash")
    ResponseEntity<String> GetIOUUIDLatestBindingHash(@RequestParam String uuid) {
        long startTime = System.currentTimeMillis();
        // if (RedisManager.getHashField("IOLogs", uuid) != null) { this is from KVP
        String hash = RedisManager.getHashField(uuid); // From keyvalue
        if (hash != null) {
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime; // duration in milliseconds
            return ResponseEntity.ok("Hash from REDIS for IO : " + uuid + " -> " + hash
                    + " timespan in MS : " + durationMs);
        } else {
            // retrieve UUID from Datastore and cache in redis
            Optional<IOLogBallenbakMongo> Logentry = IOLog.GetLog(uuid);
            if (Logentry.isPresent()) {
                hash = Logentry.get().getPkiHash();
                RedisManager.putHash("IOLogs", uuid, hash, 1200);
                // From KVP
                // pkiHash = RedisManager.getHashField("IOLogs", uuid);
                // from key
                hash = RedisManager.getHashField(uuid); // From keyvalue
                long endTime = System.currentTimeMillis();
                long durationMs = endTime - startTime; // duration in milliseconds
                return ResponseEntity.ok("Latest hash from datastore for IO : " + uuid + " -> " + hash
                        + " timespan in MS : " + durationMs);
            } else {
                return ResponseEntity.ok("No log entry found for UUID: " + uuid);
            }
        }
    }
}