package contain.opensource.ils.bs.receiver.classes.Redis;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
public class redisLogger {

    public static void main(String[] args) {
        RedisClient client = RedisClient.create("redis://localhost:8001");

        StatefulRedisConnection<String, String> connection = client.connect();
        RedisCommands<String, String> redis = connection.sync();

        redis.set("key", "value");
        String value = redis.get("key");

        System.out.println(value);

        connection.close();
        client.shutdown();
    }
}