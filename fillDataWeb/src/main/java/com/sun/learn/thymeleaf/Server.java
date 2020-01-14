package com.sun.learn.thymeleaf;

import com.sun.learn.util.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;

/**
 * @author zcm
 */
public class Server extends AbstractVerticle {

    public static void main(String[] args) {
        Runner.runExample(Server.class);
    }

    @Override
    public void start() throws Exception {

        Router router = Router.router(vertx);

        ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(vertx);

        router.get().handler(routingContext -> {
            JsonObject data = new JsonObject().put("welcome","hi there");

            engine.render(data,"templates/index.html",bufferAsyncResult -> {
                if (bufferAsyncResult.succeeded()){
                    routingContext.response().end(bufferAsyncResult.result());
                }else{
                    routingContext.fail(bufferAsyncResult.cause());
                }
            });
        });

        vertx.createHttpServer().requestHandler(router).listen(8888);
    }
}
