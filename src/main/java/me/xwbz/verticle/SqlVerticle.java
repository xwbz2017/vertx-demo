package me.xwbz.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.ext.asyncsql.MySQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  连接数据库测试
 * @author zgq
 * Created by zgq on 2018/3/23.
 */
public class SqlVerticle extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private  SQLClient client;

    @Override
    public void start() {
        client = MySQLClient.createShared(vertx, config());
        client.getConnection(h -> {
            if(h.succeeded()){
                SQLConnection connection = h.result();
                connection.close();
                logger.info("---------数据库连接成功--------");
            }else{
                h.cause().printStackTrace();
            }
        });
        // 实例化一个路由器出来，用来路由不同的rest接口
        Router router = Router.router(vertx);
        // 增加一个处理器，将请求的上下文信息，放到RoutingContext中
        router.route().handler(BodyHandler.create());
        // 处理一个post方法的rest接口
        router.get("/all").handler(this::getAll);
        // 创建一个httpserver，监听8080端口，并交由路由器分发处理用户请求
        vertx.createHttpServer().requestHandler(router::accept).listen(81);
    }

    @Override
    public void stop() {
        if(client != null) {
            client.close();
        }
    }

    private void getAll(RoutingContext routingContext) {
        client.getConnection(ar -> {
            if(ar.succeeded()) {
                SQLConnection connection = ar.result();
                connection.query("SELECT * FROM xx_ad", result -> {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodeToBuffer(result.result().getRows()));
                    connection.close(); // Close the connection
                });
            }else {
                ar.cause().printStackTrace();
            }
        });
    }
}
