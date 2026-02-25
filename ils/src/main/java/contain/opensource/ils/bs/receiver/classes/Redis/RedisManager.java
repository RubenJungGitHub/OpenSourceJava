package contain.opensource.ils.bs.receiver.classes.Redis;

import java.util.HashMap;
import java.util.Map;
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
    /**
     * Initializes the RedisManager by establishing a connection to a Redis server.
     * 
     * This method reads the Redis host and port from the environment variables
     * {@code SPRING_REDIS_HOST} and {@code SPRING_REDIS_PORT}. If these are not
     * set,
     * it defaults to {@code localhost} and port {@code 6379}.
     *
     * 
     * If the RedisManager is already initialized, this method returns immediately.
     * Any exceptions during initialization are caught and logged to
     * {@code System.err},
     * but not rethrown, allowing the class to load even if Redis is unavailable.
     *
     * 
     * This method is synchronized to ensure thread safety during initialization.
     *
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
    /**
     * Stores a value in Redis with a specified TTL (Time To Live) using the given
     * field as the key.
     * 
     * Note: Instead of storing the value as a hash field under {@code hashKey},
     * this method stores the value
     * as a simple key-value pair with {@code field} as the key and sets an
     * expiration time. This avoids the
     * issue where setting a TTL on a Redis hash deletes the entire hash, which may
     * be undesirable if other
     * important data is stored in the same hash.
     *
     *
     * param hashKey The Redis hash key (not used for storage in this
     * implementation).
     * param field The field name to be used as the Redis key.
     * param value The value to store.
     * param TimeSpanSecs The expiration time in seconds for the key-value pair.
     */
    public static void putHash(String hashKey, String field, String value, Integer TimeSpanSecs) {
        init();
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
        init();
        if (redis != null) {
            // This returns value from key
            redis.expire(uuid, 60); // reset TTL to 60 seconds
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
        init();
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
            putHash(hashKey, uuid, Hash, 120); // cache in Redis
            redis.setex(uuid, 120, value); // TTL 10 minutes
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
}