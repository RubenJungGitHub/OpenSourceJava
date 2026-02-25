package contain.opensource.ils.bs.receiver.classes.Redis;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class redisLogger {

    public static void main(String[] args) {
        // Get Redis host/port from environment variables
        String redisHost = System.getenv("SPRING_REDIS_HOST");
        if (redisHost == null || redisHost.isEmpty()) {
            redisHost = "localhost"; // default for dev
        }

        String redisPortStr = System.getenv("SPRING_REDIS_PORT");
        int redisPort = 8001; // default
        if (redisPortStr != null && !redisPortStr.isEmpty()) {
            redisPort = Integer.parseInt(redisPortStr);
        }

        String redisUri = String.format("redis://%s:%d", redisHost, redisPort);
        System.out.println("Connecting to Redis at: " + redisUri);

        RedisClient client = RedisClient.create(redisUri);

        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisCommands<String, String> redis = connection.sync();

            redis.set("key", "value");
            String value = redis.get("key");

            System.out.println("Redis GET key = " + value);
        }

        client.shutdown();
    }
}
