package me.xwbz.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 语音朗读
 * Created by hasee on 2018/1/10.
 */
public class SpeakVerticle extends AbstractVerticle {

    /**
     * 填写网页上申请的appkey
     */
    private static final String appKey = "x8h9aDqSgi8bgcRUB0rdK6lz";

    /**
     * 填写网页上申请的APP SECRET
     */
    private static final String secretKey = "48a78a95f5041554a64186077cca6d2f";

    /**
     * 发音人选择, 0为普通女声，1为普通男生，3为情感合成-度逍遥，4为情感合成-度丫丫，默认为普通女声
     */
    private final int per = 4;
    /**
     * 语速，取值0-9，默认为5中语速
     */
    private final int spd = 5;
    /**
     * 音调，取值0-9，默认为5中语调
     */
    private final int pit = 9;
    /**
     * 音量，取值0-9，默认为5中音量
     */
    private final int vol = 5;

    private String cuid = "1234567JAVA";

    private String temp = System.getProperty("java.io.tmpdir");

    private final static Map<String, String> FILE_MAP = new ConcurrentHashMap<>();

    private static String token = "24.408588bcdb510f5761b6ca0e7cd724e0.2592000.1524236901.282335-10972844";

    private static String urlEncode(String str) {
        String result = null;
        try {
            result = URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void run(RoutingContext context) {
        WebClient client = WebClient.create(vertx);
        client.get("openapi.baidu.com", "/oauth/2.0/token")
                .addQueryParam("grant_type", "client_credentials")
                .addQueryParam("client_id", urlEncode(appKey))
                .addQueryParam("client_secret", urlEncode(secretKey))
                .send(res -> {
                    if (res.succeeded()) {
                        JsonObject obj = res.result().bodyAsJsonObject();
                        token = obj.getString("access_token");
                        System.out.println(obj.toString());
                        context.response().end(token);
                    } else {
                        res.cause().printStackTrace();
                    }
                });
        System.out.println("ok");
    }

    private void body(RoutingContext context) {
        String txt = context.request().getParam("txt");
        if (FILE_MAP.containsKey(txt)) {
            context.response().end(FILE_MAP.get(txt));
            return;
        }
        WebClient client = WebClient.create(vertx);
        Future<HttpResponse<Buffer>> getFuture = Future.future();
        Future<String> fileFuture = Future.future();
        FileSystem fs = vertx.fileSystem();
//        CompositeFuture.all

        client.get("tsn.baidu.com", "/text2audio")
                .addQueryParam("tex", urlEncode(txt))
                .addQueryParam("per", per + "")
                .addQueryParam("spd", spd + "")
                .addQueryParam("pit", pit + "")
                .addQueryParam("vol", vol + "")
                .addQueryParam("cuid", cuid + "")
                .addQueryParam("tok", token)
                .addQueryParam("lan", "zh")
                .addQueryParam("ctp", "1")
                .send(getFuture.completer());

        getFuture.compose(h -> {
            String fkey = UUID.randomUUID().toString().replace("-", "") + ".mp3";
            String fname = temp + "/" + fkey;
            fs.writeFile(fname, h.bodyAsBuffer(), rr -> {
                if (rr.succeeded()) {
                    FILE_MAP.put(txt, fkey);
                    fileFuture.complete(fkey);
                } else {
                    rr.cause().printStackTrace();
                    fileFuture.fail(rr.cause());
                }
            });
        }, fileFuture);

        fileFuture.setHandler(r -> {
            if (r.succeeded()) {
                context.response().end(r.result());
            } else {
                context.response().end(r.cause().getMessage());
            }
        });
    }

    private void html(RoutingContext context) {
        FileSystem fs = vertx.fileSystem();
        fs.readFile("index.html", res -> {
            if (res.succeeded()) {
                context.response()
                        .putHeader("content-type", "text/html;charset=utf-8")
                        .end(res.result());
            } else {
                res.cause().printStackTrace();
            }
        });
    }

    private void audio(RoutingContext context) {
        String fkey = context.request().getParam("key");
        FileSystem fs = vertx.fileSystem();
        String fname = temp + "/" + fkey;
        fs.readFile(fname, res -> {
            if (res.succeeded()) {
                context.response()
                        .end(res.result());
            } else {
                res.cause().printStackTrace();
            }
        });
    }

    @Override
    public void start() {
        // 实例化一个路由器出来，用来路由不同的rest接口
        Router router = Router.router(vertx);
        // 增加一个处理器，将请求的上下文信息，放到RoutingContext中
        router.route().handler(BodyHandler.create());
        // 处理一个post方法的rest接口
        router.get("/init").handler(this::run);
        // 将访问“/audio/*”的请求route到“audio”目录下的资源
        router.route("/audio/:key").handler(this::audio);
        // 处理一个get方法的rest接口
        router.get("/body").handler(this::body);
        router.get("/html").handler(this::html);
        // 创建一个httpserver，监听8080端口，并交由路由器分发处理用户请求
        vertx.createHttpServer().requestHandler(router::accept).listen(80);
    }
}
