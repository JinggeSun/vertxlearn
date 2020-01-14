package com.sun.learn.oauth2;

import com.sun.learn.util.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.handler.impl.CookieHandlerImpl;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;

/**
 * @author  zcm
 */
public class Server extends AbstractVerticle {

    public static void main(String[] args) {
        Runner.runExample(Server.class);
    }

    // 客户端id
    private static final String CLIENT_ID = "57cdaa1952a3f4ee3df8";
    // 客户端密钥
    private static final String CLIENT_SECRET = "3155eafd33fc947e0fe9f44127055ce1fe876704";

    /**
     * 模版引擎
     */
    private final HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create(vertx);

    @Override
    public void start() throws Exception {

        final Router router = Router.router(vertx);

        //cookie
        router.route().handler(new CookieHandlerImpl());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        //oauth2
        OAuth2Auth oAuth2Auth = GithubAuth.create(vertx,CLIENT_ID,CLIENT_SECRET);

        router.route().handler(UserSessionHandler.create(oAuth2Auth));

        router.route("/protected").handler(
            OAuth2AuthHandler.create(oAuth2Auth).setupCallback(router.route("/callback")).addAuthority("user:email")
        );

        router.get("/").handler(ctx->{
                JsonObject data = new JsonObject().put("client_id",CLIENT_ID);
            engine.render(data, "views/index.hbs", res -> {
                if (res.succeeded()) {
                    ctx.response()
                            .putHeader("Content-Type", "text/html")
                            .end(res.result());
                } else {
                    ctx.fail(res.cause());
                }
            });

        });


        router.get("/protected").handler(ctx->{
            AccessToken user = (AccessToken) ctx.user();
            user.userInfo(res->{
               if (res.failed()){
                   ctx.session().destroy();
                   ctx.fail(res.cause());
               }else{
                   final JsonObject userInfo = res.result();
                   user.fetch("https://api.github.com/user/emails",res2->{if (res2.failed()) {
                       // request didn't succeed because the token was revoked so we
                       // invalidate the token stored in the session and render the
                       // index page so that the user can start the OAuth flow again
                       ctx.session().destroy();
                       ctx.fail(res2.cause());
                   } else {
                       userInfo.put("private_emails", res2.result().jsonArray());
                       // we pass the client info to the template
                       JsonObject data = new JsonObject()
                               .put("userInfo", userInfo);
                       // and now delegate to the engine to render it.
                       engine.render(data, "views/advanced.hbs", res3 -> {
                           if (res3.succeeded()) {
                               ctx.response()
                                       .putHeader("Content-Type", "text/html")
                                       .end(res3.result());
                           } else {
                               ctx.fail(res3.cause());
                           }
                       });
                   }
                   });
               }
            });
        });

        vertx.createHttpServer().requestHandler(router).listen(8888);

    }
}
