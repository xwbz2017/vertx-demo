package me.xwbz;

import io.vertx.core.Vertx;
import me.xwbz.verticle.SpeakVerticle;

/**
 * Created by hasee on 2018/1/10.
 */
public class Main {

    public static void main(String[] args){
        Vertx vertx = Vertx.vertx();

//        vertx.deployVerticle(MyFirstVerticle.class.getName());
        vertx.deployVerticle(SpeakVerticle.class.getName());
    }
}
