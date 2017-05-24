package moe.suggestme.server;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderValues;
import moe.suggestme.Runner;

import java.util.List;

/**
 * Created by Endrik on 24-May-17.
 */
public class IPReader implements HttpHandler {
    public static AttachmentKey<String> IP = AttachmentKey.create(String.class);

    private HttpHandler next;

    public IPReader(HttpHandler next) {
        this.next = next;
    }


    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String addr = exchange.getSourceAddress().getAddress().getHostAddress();

        List<String> locals = Runner.getConfig().getStringList("server.proxy.localhosts");
        for (String headerName : Runner.getConfig().getStringList("server.proxy.headers")) {
            if (locals.contains(addr)) {
                HeaderValues header = exchange.getRequestHeaders().get(headerName);
                if (header != null && !header.isEmpty()) {
                    addr = header.element();
                }
            }
        }

        exchange.putAttachment(IP, addr);
        next.handleRequest(exchange);
    }
}
