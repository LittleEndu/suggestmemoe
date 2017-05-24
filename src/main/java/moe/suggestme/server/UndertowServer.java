package moe.suggestme.server;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import moe.suggestme.Runner;

/**
 * Created by Endrik on 22-May-17.
 */
public class UndertowServer {
    private int port;
    private Undertow server;
    private WebHandler webHandler;

    public UndertowServer(int port) {
        this.port = port;
        webHandler = new WebHandler();
    }

    public void start() {
        PathHandler pathHandler = Handlers.path()
                .addPrefixPath("/suggestpost", webHandler::sendSuggestionsAfterPost)
                .addPrefixPath("/suggest", webHandler::sendSuggestions)
                .addPrefixPath("/search", webHandler::sendAnimeSearchResults)
                .addPrefixPath("/user", webHandler::sendUserAnimeList)
                .addPrefixPath("/recs", webHandler::sendRecommendations)
                .addPrefixPath("/", Handlers.resource(new ClassPathResourceManager(Runner.class.getClassLoader(), "public/")));
        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setIoThreads(32)
                .setWorkerThreads(128)
                .setHandler(new EagerFormParsingHandler().setNext(pathHandler)).build();
        server.start();
    }

    public Undertow getServer() {
        return server;
    }
}
