package com.sun.learn.cookie;

import com.sun.learn.util.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.StaticHandler;

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

        // This cookie handler will be called for all routes
        router.route().handler(CookieHandler.create());

        // on every path increment the counter
        router.route().handler(ctx -> {
            Cookie someCookie = ctx.getCookie("visits");

            long visits = 0;
            if (someCookie != null) {
                String cookieValue = someCookie.getValue();
                try {
                    visits = Long.parseLong(cookieValue);
                } catch (NumberFormatException e) {
                    visits = 0l;
                }
            }

            // increment the tracking
            visits++;

            // Add a cookie - this will get written back in the response automatically
            ctx.addCookie(Cookie.cookie("visits", "" + visits));

            ctx.next();
        });

        // Serve the static resources
        router.route().handler(StaticHandler.create());

        vertx.createHttpServer().requestHandler(router).listen(8890);
    }
}
