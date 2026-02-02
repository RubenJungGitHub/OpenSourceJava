package contain.opensource.ils.bs.receiver.classes.Redis;

import java.util.HashMap;
import java.util.Map;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Static RedisManager
 * - Lazily initialized
 * - Stores hashes persistently (requires Redis AOF or RDB)
 * - Provides static access from anywhere
 */
public final class RedisManager {

    private static final String REDIS_URI = "redis://localhost:6379"; // Redis protocol port

    public static RedisClient client;
    public static StatefulRedisConnection<String, String> connection;
    public static RedisCommands<String, String> redis;

    // Private constructor to prevent instantiation
    private RedisManager() {
    }

    /**
     * Lazy initialization
     */
    public static synchronized void init() {
        if (redis != null)
            return; // already initialized
        try {
            String host = System.getenv("SPRING_REDIS_HOST");
            if (host == null || host.isBlank()) {
                host = "localhost";
            }

            String portStr = System.getenv("SPRING_REDIS_PORT");
            int port = 6379; // container default
            if (portStr != null && !portStr.isBlank()) {
                port = Integer.parseInt(portStr);
            }

            String redisUri = "redis://" + host + ":" + port;
            System.out.println("[RedisManager] Connecting to " + redisUri);

            client = RedisClient.create(redisUri);
            connection = client.connect();
            redis = connection.sync();
            System.out.println(">>> RedisManager initialized");
        } catch (Exception e) {
            System.err.println("Failed to initialize RedisManager!");
            e.printStackTrace();
            // Do not throw to allow class to load
        }
    }

    // ===============================
    // Hash operations (persistent)
    // ===============================

    /** Store a field in a hash */
    public static void putHash(String hashKey, String field, String value) {
        init();
        // As hashjey TTL will delete entire hashkey .
        // This is undesired because we want to keep the essential info in cache..
        // Downside is there is no control over cache deletion if items are deleted.
        // Specifically in case of SP because the reference to the IO gets lost
        if (redis != null) {
            // Store as KVP. No ttl
            // redis.hset(hashKey, field, value);
            // Store as jey. Has ttl
            redis.setex(field, 60, value); // TTL 10 minutes
        }

        // Also add to redis with TTL. This is less efficetn but IO;s that are not
        // references frequetly or get deleted will eventuaklly go out of scip no longer
        // consuming memory.
        // TTL to be configured
        // Integer ttl = 30;
        // redis.expire(hashKey, ttl); ..This will remove the entire key. We don't want
        // that.
    }

    /** Get a field from a hash */
    public static String getHashField(String uuid) {
        init();
        if (redis != null) {
            // This returns value from key
            redis.expire(uuid, 60); // reset TTL to 60 seconds
            return redis.get(uuid);
        }
        return null;
    }

    /** Get a field from a hash */
    public static String getHashField(String hashKey, String field) {
        init();
        if (redis != null)
            // This returs the valu from Keyvaluepair
            return redis.hget(hashKey, field);
        return null;
    }

    /** Get all fields from a hash */
    public static Map<String, String> getAllHash(String hashKey) {
        init();
        if (redis != null)
            return redis.hgetall(hashKey);
        return new HashMap<>();
    }

    /** Delete a field from a hash */
    public static void deleteHashField(String hashKey, String field) {
        init();
        if (redis != null)
            redis.hdel(hashKey, field);
    }

    // ===============================
    // Optional database fallback
    // ===============================

    /**
     * Retrieve a hash field with optional fallback
     * 
     * @param hashKey  Redis hash key
     * @param field    Redis field
     * @param fallback Fallback function to load from DB if Redis is empty
     * @return value
     */
    public static String getHashField(String hashKey, String uuid, String Hash,
            java.util.function.Supplier<String> fallback) {

        // From KVP
        String value = getHashField(hashKey, uuid);

        // From Key

        if (value != null)
            return value;

        // Fallback to DB
        value = fallback.get();

        if (value != null) {
            putHash(hashKey, uuid, Hash); // cache in Redis
            redis.setex(uuid, 60, value); // TTL 10 minutes
            value = fallback.get();
        }
        return value;
    }

    // ===============================
    // Shutdown
    // ===============================
    public static void shutdown() {
        if (connection != null)
            connection.close();
        if (client != null)
            client.shutdown();
    }
}