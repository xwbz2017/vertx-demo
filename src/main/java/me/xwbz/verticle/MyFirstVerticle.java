package me.xwbz.verticle;

import io.vertx.core.AbstractVerticle;

/**
 * Created by hasee on 2018/1/10.
 */
public class MyFirstVerticle extends AbstractVerticle {

    public void start() {
        vertx.createHttpServer().requestHandler(req -> {
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("Hello World!");
        }).listen(8080);
    }
}
