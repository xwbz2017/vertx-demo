package me.xwbz;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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

        DeploymentOptions options = new DeploymentOptions();
        options.setConfig(new JsonObject()
                .put("host", "192.168.1.99")
                .put("username", "sc")
                .put("password", "sc")
                .put("database", "ddrs_v5"));
        vertx.deployVerticle(SqlVerticle.class.getName(), options);
    }
}
