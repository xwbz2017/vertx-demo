package me.xwbz.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.Router;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


/**
 * Created by hasee on 2018/1/10.
 */
public class SpeakVerticle extends AbstractVerticle {

    //  填写网页上申请的appkey 如 $apiKey="g8eBUMSokVB1BHGmgxxxxxx"
    private final String appKey = "x8h9aDqSgi8bgcRUB0rdK6lz";

    // 填写网页上申请的APP SECRET 如 $secretKey="94dc99566550d87f8fa8ece112xxxxx"
    private final String secretKey = "48a78a95f5041554a64186077cca6d2f";

    // text 的内容为"欢迎使用百度语音合成"的urlencode,utf-8 编码
    // 可以百度搜索"urlencode"
    private final String text = "hello anyone.so glad meet you";

    // 发音人选择, 0为普通女声，1为普通男生，3为情感合成-度逍遥，4为情感合成-度丫丫，默认为普通女声
    private final int per = 0;
    // 语速，取值0-9，默认为5中语速
    private final int spd = 5;
    // 音调，取值0-9，默认为5中语调
    private final int pit = 5;
    // 音量，取值0-9，默认为5中音量
    private final int vol = 5;

    private String cuid = "1234567JAVA";

    public static String token = "24.408588bcdb510f5761b6ca0e7cd724e0.2592000.1524236901.282335-10972844";

    public static String urlEncode(String str) {
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
                        System.out.println(token);
                        context.response().end(token);
                    } else {
                        System.out.println(res.cause());
                    }
                });
        System.out.println("ok");
    }

    private void body(RoutingContext context) {
        WebClient client = WebClient.create(vertx);
        client.get("tsn.baidu.com", "/text2audio")
                .addQueryParam("tex", urlEncode(text))
                .addQueryParam("per", per+"")
                .addQueryParam("spd", spd+"")
                .addQueryParam("pit", pit+"")
                .addQueryParam("vol", vol+"")
                .addQueryParam("cuid", cuid+"")
                .addQueryParam("tok", token)
                .addQueryParam("lan", "zh")
                .addQueryParam("ctp", "1")
                .send(res -> {
            if (res.succeeded()) {
                context.response().putHeader("content-type", res.result().getHeader("content-type")).end(res.result().bodyAsBuffer());
            } else {
                System.out.println(res.cause());
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
        // 处理一个get方法的rest接口
        router.get("/body").handler(this::body);
        // 创建一个httpserver，监听8080端口，并交由路由器分发处理用户请求
        vertx.createHttpServer().requestHandler(router::accept).listen(80);
    }
}
