package moe.suggestme.server;


import io.undertow.websockets.core.WebSocketChannel;
import moe.suggestme.Runner;
import moe.suggestme.mediums.Anime;
import moe.suggestme.scrapers.NoDocumentException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
            List<AnimeListHelper> helpers = obj.animelist.stream().map(anime -> new AnimeListHelper(channel, anime, toSend, Thread.currentThread())).collect(Collectors.toList());
            Map<Anime, AnimeListHelper> helperMap = new ConcurrentHashMap<>();
            for (AnimeListHelper helper : helpers) {
                helperMap.put(helper.getAnime(), helper);
                Runner.getServer().getServer().getWorker().execute(helper);
            }
            for (Anime anime : obj.animelist) {
                if (helperMap.get(anime).isFinished()) {
                    continue;
                }
                Anime animeWithRecs;
                try {
                    animeWithRecs = Runner.getDatabase().sendRecommendationsHelper(anime.getAnime().getId());
                    for (Anime rec : animeWithRecs.getRecommends()) {
                        toSend.addAnime(rec, animeWithRecs.getScore());
                    }
                } catch (IOException | NoDocumentException ignore) {
                }
                for (AnimeListHelper help : helpers) {
                    help.interrupt(anime);
                }
            }
            server.send(channel, toSend.getRecommendations());
        } catch (NoDocumentException e) {
            server.send(channel, new ArrayList<Anime>());
        }
    }

    private class AnimeListHelper implements Runnable {
        private Thread currentThread = Thread.currentThread();
        WebSocketChannel channel;
        Anime anime;
        Packet.UserRecommendations toSend;
        private boolean finished = false;
        Thread uninterruptable;

        public AnimeListHelper(WebSocketChannel channel, Anime anime, Packet.UserRecommendations toSend, Thread uninterruptable) {
            this.channel = channel;
            this.anime = anime;
            this.toSend = toSend;
            this.uninterruptable = uninterruptable;
        }

        @Override
        public void run() {
            Anime animeWithRecs;
            try {
                animeWithRecs = Runner.getDatabase().sendRecommendationsHelper(anime.getAnime().getId());
                for (Anime rec : animeWithRecs.getRecommends()) {
                    toSend.addAnime(rec, animeWithRecs.getScore());
                }
            } catch (IOException | NoDocumentException ignored) {
            }
            finished = true;
        }

        public Anime getAnime() {
            return anime;
        }

        public boolean isFinished() {
            return finished;
        }

        public void interrupt(Anime anime) {
            if (finished) {
                return;
            }
            if (currentThread == uninterruptable) {
                return;
            }
            if (this.anime.equals(anime)) {
                finished = true;
                currentThread.interrupt();
            }
        }
    }
}
