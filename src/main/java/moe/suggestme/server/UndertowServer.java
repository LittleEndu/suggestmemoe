package moe.suggestme.server;

import com.google.gson.JsonObject;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderValues;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import moe.suggestme.Runner;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.SocketHandler;

/**
 * Created by Endrik on 22-May-17.
 */
public class UndertowServer {
    private int port;
    private Undertow server;
    private WebHandler webHandler;
    private PacketHandler socketHandler;
    private Set<WebSocketChannel> connections;

    public UndertowServer(int port) {
        this.port = port;
        webHandler = new WebHandler();
        socketHandler = new PacketHandler(this);
    }

    public void start() {
        PathHandler pathHandler = Handlers.path()
                .addPrefixPath("/ws", Handlers.websocket(this::webSocketHandler))
                .addPrefixPath("/suggest", webHandler::sendSuggestions)
                .addPrefixPath("/search", webHandler::sendAnimeSearchResults)
                .addPrefixPath("/user", webHandler::sendUserAnimeList)
                .addPrefixPath("/recs", webHandler::sendRecommendations)
                .addPrefixPath("/", Handlers.resource(new ClassPathResourceManager(Runner.class.getClassLoader(), "public/")));
        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setIoThreads(32)
                .setWorkerThreads(128)
                .setHandler(new IPReader(new EagerFormParsingHandler().setNext(pathHandler))).build();
        server.start();
    }

    public Undertow getServer() {
        return server;
    }

    private void webSocketHandler(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        connections = exchange.getPeerConnections();
        String ip = exchange.getAttachment(IPReader.IP);

        channel.getReceiveSetter().set(new AbstractReceiveListener() {
            @Override
            protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                super.onFullTextMessage(channel, message);

                String data = message.getData();

                JsonObject jsonObj = Runner.getGson().fromJson(data, JsonObject.class);
                String type = jsonObj.get("type").getAsString();

                Object obj = null;
                if (type.equals("animelist")) obj = Runner.getGson().fromJson(jsonObj, Packet.ClientAnimeList.class);

                if (obj != null) {
                    socketHandler.accept(channel, obj, ip);
                }
            }
        });

        channel.resumeReceives();
    }

    public void send(WebSocketChannel channel, Object obj) {
        sendRaw(channel, Runner.getGson().toJson(obj));
    }

    private void sendRaw(WebSocketChannel channel, String str) {
        WebSockets.sendText(str, channel, null);
    }
}
