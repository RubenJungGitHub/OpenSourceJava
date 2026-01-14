package contain.opensource.ils.bs.receiver.classes.Redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.HashMap;
import java.util.Map;

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
    private RedisManager() {}

    /**
     * Lazy initialization
     */
    public static synchronized void init() {
        if (redis != null) return; // already initialized
        try {
            client = RedisClient.create(REDIS_URI);
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
        if (redis != null) redis.hset(hashKey, field,value);
        // TTL to be configured
        //Integer ttl = 30;
        //redis.expire(hashKey,  ttl);  ..This will remove the entire key. We don't want that.
    }

    /** Get a field from a hash */
    public static String getHashField(String hashKey, String field) {
        init();
        if (redis != null) return redis.hget(hashKey, field);
        return null;
    }

    /** Get all fields from a hash */
    public static Map<String, String> getAllHash(String hashKey) {
        init();
        if (redis != null) return redis.hgetall(hashKey);
        return new HashMap<>();
    }

    /** Delete a field from a hash */
    public static void deleteHashField(String hashKey, String field) {
        init();
        if (redis != null) redis.hdel(hashKey, field);
    }

    // ===============================
    // Optional database fallback
    // ===============================

    /**
     * Retrieve a hash field with optional fallback
     * @param hashKey Redis hash key
     * @param field Redis field
     * @param fallback Fallback function to load from DB if Redis is empty
     * @return value
     */
    public static String getHashField(String hashKey, String uuid, String Hash, java.util.function.Supplier<String> fallback) {
        String value = getHashField(hashKey, uuid);
        
        if (value != null) return value;

        // Fallback to DB
        value = fallback.get();

        if (value != null) {
            putHash(hashKey, uuid, Hash); // cache in Redis
            value = fallback.get();
        }
        return value;
    }

    

    // ===============================
    // Shutdown
    // ===============================
    public static void shutdown() {
        if (connection != null) connection.close();
        if (client != null) client.shutdown();
    }
}