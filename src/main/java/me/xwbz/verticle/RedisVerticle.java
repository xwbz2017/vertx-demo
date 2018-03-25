package me.xwbz.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class RedisVerticle extends AbstractVerticle {

    public static final String GET_MSG_ADDR = "redis.get";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private RedisClient client;

    @Override
    public void start() {
        RedisOptions options = new RedisOptions(config());
        client = RedisClient.create(vertx, options);

        client.set("test", LocalDateTime.now().toString(), h -> {
            if (h.succeeded()) {
                logger.info("测试数据已加载");
            } else {
                h.cause().printStackTrace();
            }
        });

        vertx.eventBus().consumer(GET_MSG_ADDR, msg -> {
            String key = msg.body().toString();
            logger.info("GET: " + key);
            client.get(key, h -> {
                if (h.succeeded()) {
                    msg.reply(h.result());
                } else {
                    h.cause().printStackTrace();
                    msg.fail(-1, h.cause().getMessage());
                }
            });
        });
    }
}
