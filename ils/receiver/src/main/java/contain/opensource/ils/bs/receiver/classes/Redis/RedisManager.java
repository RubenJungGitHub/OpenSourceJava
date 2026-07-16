package contain.opensource.ils.bs.receiver.classes.redis;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * RedisManager is a utility class for managing Redis connections and
 * operations.
 * 
 * This class provides static methods for initializing a Redis connection,
 * performing hash and key-value operations, and handling cache with optional
 * database fallback. It uses lazy initialization and environment variables
 * to configure the Redis host and port.
 *
 * 
 * Features:
 *
 * Lazy initialization of Redis client and connection
 * Hash operations: put, get, get all, delete fields
 * Key-value operations with TTL support
 * Optional fallback to database when cache miss occurs
 * Graceful shutdown of Redis resources
 *
 *
 * 
 * Note: This class is not intended to be instantiated.
 *
 */
@Component
// @ConditionalOnProperty(name = "redis.enabled", havingValue = "true",
// matchIfMissing = false)
// @ConditionalOnMissingBean(RedisManager.class)
public final class RedisManager {
    private static final String REDIS_URI = "redis://localhost:6379"; // Redis protocol port

    public static RedisClient client;
    public static StatefulRedisConnection<String, String> connection;
    public static RedisCommands<String, String> redis;
    private final RedisConfigProperties config;

    // Private constructor to prevent instantiation
    private RedisManager(RedisConfigProperties config) {
        this.config = config;
    }

      public static void putHash(String hashKey, String field, String value, Integer TimeSpanSecs) {
        // init();
        // As hashjey TTL will delete entire hashkey .
        // This is undesired because we want to keep the essential info in cache..
        // Downside is there is no control over cache deletion if items are deleted.
        // Specifically in case of SP because the reference to the IO gets lost
        if (redis != null) {
            // Store as KVP. No ttl
            // redis.hset(hashKey, field, value);
            // Store as jey. Has ttl
            redis.setex(field, TimeSpanSecs, value);
        }
    }

    /** Get a field from a hash */
    /**
     * Retrieves the value associated with the specified UUID key from Redis.
     * Resets the key's time-to-live (TTL) to 60 seconds if the key exists.
     *
     * param uuid the unique identifier used as the key in Redis
     * 
     * @return the value associated with the given UUID key, or {@code null} if
     *         Redis is unavailable
     */
    public static String getHashField(String uuid) {
        // init();
        if (redis != null) {
            // This returns value from key
            redis.expire(uuid, 1200); // reset TTL
            return redis.get(uuid);
        }
        return null;
    }

    /** Get a field from a hash */
    /**
     * Retrieves the value associated with the specified field from a Redis hash.
     *
     * param hashKey the key of the Redis hash
     * param field the field within the hash whose value is to be retrieved
     * 
     * @return the value associated with the specified field, or {@code null} if the
     *         field does not exist or Redis is unavailable
     */
    public static String getHashField(String hashKey, String field) {
        // init();
        if (redis != null)
            // This returs the valu from Keyvaluepair
            return redis.hget(hashKey, field);
        return null;
    }

    /** Get all fields from a hash */
    /**
     * Retrieves all fields and values of a hash stored at the specified key from
     * Redis.
     *
     * param hashKey the key of the hash in Redis
     * 
     * @return a map containing all fields and their corresponding values from the
     *         hash,
     *         or an empty map if the Redis connection is not available
     */
    public static Map<String, String> getAllHash(String hashKey) {
        // init();
        if (redis != null)
            return redis.hgetall(hashKey);
        return new HashMap<>();
    }

    /** Delete a field from a hash */
    public static void deleteHashField(String hashKey, String field) {
        // init();
        if (redis != null)
            redis.hdel(hashKey, field);
    }

    // ===============================
    // Database fallback
    // ===============================

    /**
     * Retrieve a hash field with optional fallback
     * 
     * param hashKey Redis hash key
     * param field Redis field
     * param fallback Fallback function to load from DB if Redis is empty
     * 
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
            putHash(hashKey, uuid, Hash, 1200); // cache in Redis
            redis.setex(uuid, 1200, value); // TTL 10 minutes
            value = fallback.get();
        }
        return value;
    }

    public static void deleteEntry(String uuid) {
        redis.del(uuid);
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

    @PostConstruct
    public void init() {
        try
        {
        // Automatically runs once when the bean is created
        String host = config.getHost() != null ? config.getHost() : "localhost";
        int port = config.getPort() != 0 ? config.getPort() : 6379;

        String redisUri = "redis://" + host + ":" + port;

        this.client = RedisClient.create(redisUri);
        this.connection = client.connect();
        this.redis = connection.sync();
         System.out.println(contain.opensource.shared.constants.AlfrescoConstants.BRIGHT_GREEN
                    + ("REDISCONNECTION INITIALIZED: " + redis.toString())
                    + contain.opensource.shared.constants.AlfrescoConstants.RESET);
                    }
        catch (Exception e) {
            System.err.println("Failed to initialize RedisManager!");
            e.printStackTrace();
        }
    }

    public RedisCommands<String, String> getCommands() {
        return this.redis;
    }
}