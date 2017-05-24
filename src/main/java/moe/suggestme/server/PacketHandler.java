package moe.suggestme.server;


import io.undertow.websockets.core.WebSocketChannel;
import moe.suggestme.Runner;
import moe.suggestme.mediums.Anime;
import moe.suggestme.scrapers.NoDocumentException;
import moe.suggestme.user.UserSuggestions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PacketHandler {
    private UndertowServer server;

    public PacketHandler(UndertowServer server) {
        this.server = server;
    }

    public void accept(WebSocketChannel channel, Object obj, String ip) {
        if (obj instanceof Packet.ClientAnimeList) handleAnimelist(channel, (Packet.ClientAnimeList) obj);
    }

    private void handleAnimelist(WebSocketChannel channel, Packet.ClientAnimeList obj) {
        try {
            Packet.UserRecommendations toSend = new Packet.UserRecommendations(obj.animelist);
            for (Anime anime : obj.animelist) {
                Anime animeWithRecs;
                try {
                    animeWithRecs = Runner.getDatabase().sendRecommendationsHelper(anime.getAnime().getId());
                } catch (IOException | NoDocumentException e) {
                    e.printStackTrace();
                    server.send(channel, toSend.getRecommendations());
                    return;
                }
                for (Anime rec : animeWithRecs.getRecommends()) {
                    toSend.addAnime(rec, animeWithRecs.getScore());
                }
            }
            server.send(channel, toSend.getRecommendations());
        } catch (NoDocumentException e) {
            server.send(channel, new ArrayList<Anime>());
        }
    }
}
