package me.xwbz;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import me.xwbz.verticle.RedisVerticle;
import me.xwbz.verticle.SpeakVerticle;
import me.xwbz.verticle.SqlVerticle;

/**
 * Created by hasee on 2018/1/10.
 */
public class Main {

    public static void main(String[] args){
        Vertx vertx = Vertx.vertx();

//        vertx.deployVerticle(MyFirstVerticle.class.getName());
        vertx.deployVerticle(SpeakVerticle.class.getName());

        DeploymentOptions sqlOptions = new DeploymentOptions();
        sqlOptions.setConfig(new JsonObject()
                .put("host", "192.168.1.99")
                .put("username", "sc")
                .put("password", "sc")
                .put("database", "ddrs_v5"));
        vertx.deployVerticle(SqlVerticle.class.getName(), sqlOptions);

        DeploymentOptions redisOptions = new DeploymentOptions();
        redisOptions.setConfig(new JsonObject()
                .put("host", "127.0.0.1")
                .put("auth", "redis"));
        vertx.deployVerticle(RedisVerticle.class.getName(), redisOptions, h -> {
            vertx.eventBus().send(RedisVerticle.GET_MSG_ADDR, "test", r -> {
                if(r.succeeded()){
                    System.out.println(r.result().body());
                }else{
                    System.err.println(r.result().body());
                }
            });
        });
    }
}
