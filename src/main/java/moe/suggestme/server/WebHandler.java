package moe.suggestme.server;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import moe.suggestme.Runner;
import moe.suggestme.mediums.Anime;
import moe.suggestme.scrapers.AnimeSearchScraper;
import moe.suggestme.scrapers.NoDocumentException;
import moe.suggestme.user.User;
import moe.suggestme.user.UserAnime;
import moe.suggestme.user.UserSuggestions;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by Endrik on 22-May-17.
 */
public class WebHandler {
    void sendRecommendations(HttpServerExchange exchange) {
        int id = Integer.parseInt(exchange.getQueryParameters().get("id").getFirst());
        Anime anime = null;
        try {
            anime = Runner.getDatabase().sendRecommendationsHelper(id);
        } catch (IOException | NoDocumentException e) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("");
            return;
        }
        if (anime == null) {
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.getResponseSender().send("");
            return;
        }
        exchange.getResponseHeaders().add(HttpString.tryFromString("Content-Type"), "application/json");
        exchange.getResponseSender().send(Runner.getGson().toJson(anime));
    }

    void sendUserAnimeList(HttpServerExchange exchange) {
        String name = exchange.getQueryParameters().get("u").getFirst();
        User user;
        try {
            user = Runner.getDatabase().sendUserAnimelistHelper(name);
        } catch (IOException | NoDocumentException e) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("");
            return;
        }
        exchange.getResponseHeaders().add(HttpString.tryFromString("Content-Type"), "application/json");
        while (true) {
            try {
                exchange.getResponseSender().send(Runner.getGson().toJson(user));
                return;
            } catch (ConcurrentModificationException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                    exchange.getResponseSender().send("");
                    return;
                }
            }
        }
    }

    void sendAnimeSearchResults(HttpServerExchange exchange) {
        String query = exchange.getQueryParameters().get("q").getFirst();
        AnimeSearchScraper scraper;
        try {
            scraper = new AnimeSearchScraper(query);
            scraper.scrape();
        } catch (IOException | NoDocumentException e) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("");
            return;
        }
        Anime[] searchResults = new Anime[scraper.searchResults.size()];
        for (int i = 0; i < scraper.searchResults.size(); i++) {
            int index = i;
            Runner.getServer().getServer().getWorker().execute(() -> {
                int id = scraper.searchResults.get(index);
                Anime anime = null;
                try {
                    anime = Runner.getDatabase().getAnime(id);
                } catch (NullPointerException e) {
                    anime = null;
                }
                if (anime == null) {
                    try {
                        Runner.getDatabase().addAnime(id, false);
                        anime = Runner.getDatabase().getAnime(id);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                searchResults[index] = anime;
            });
        }
        while (Arrays.asList(searchResults).contains(null)) {
            int index = Arrays.asList(searchResults).indexOf(null);
            if (index == -1) {
                continue;
            }
            int id = scraper.searchResults.get(index);
            Anime anime;
            try {
                anime = Runner.getDatabase().getAnime(id);
            } catch (NullPointerException e) {
                anime = null;
            }
            if (anime == null) {
                try {
                    Runner.getDatabase().addAnime(id, false);
                    anime = Runner.getDatabase().getAnime(id);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
            searchResults[index] = anime;
        }
        exchange.getResponseHeaders().add(HttpString.tryFromString("Content-Type"), "application/json");
        while (true) {
            try {
                exchange.getResponseSender().send(Runner.getGson().toJson(searchResults));
                return;
            } catch (ConcurrentModificationException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                    exchange.getResponseSender().send("");
                    return;
                }
            }
        }
    }
    void sendSuggestions(HttpServerExchange exchange){
        Thread uninteruptable = Thread.currentThread();
        String name = exchange.getQueryParameters().get("u").getFirst();
        User user;
        UserSuggestions toSend;
        try {
            user = Runner.getDatabase().sendUserAnimelistHelper(name);
            List<SendSuggestionsHelper> helpers = new ArrayList<>();
            toSend = new UserSuggestions(user);
            helpers.addAll(user.getAnimeList().stream().map(anime -> new SendSuggestionsHelper(exchange, anime, toSend, uninteruptable)).collect(Collectors.toList()));
            Map<UserAnime, SendSuggestionsHelper> helperMap = new ConcurrentHashMap<>();
            for (SendSuggestionsHelper help:helpers){
                helperMap.put(help.getAnime(), help);
                Runner.getServer().getServer().getWorker().execute(help);
            }
            for (UserAnime anime:user.getAnimeList()){
                if (helperMap.get(anime).isFinished()) {
                    continue;
                }
                System.out.println(MessageFormat.format("suggestions({0}) {1} left", name, Collections.frequency(helperMap.values().stream().map(SendSuggestionsHelper::isFinished).collect(Collectors.toList()), false)));
                Anime recommendations = Runner.getDatabase().sendRecommendationsHelper(anime.getAnime().getId());
                try {
                    for (Anime toAdd:recommendations.getRecommends()){
                        toSend.addAnime(toAdd, anime.getGivenScore());
                        for (SendSuggestionsHelper help:helpers){
                            help.interrupt(anime);
                        }
                    }
                } catch (NullPointerException e) {
                   continue;
                }
            }
            toSend.getRecommendedAnimes();
        } catch (IOException | NoDocumentException e) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("");
            return;
        }
        exchange.getResponseHeaders().add(HttpString.tryFromString("Content-Type"), "application/json");
        exchange.getResponseSender().send(Runner.getGson().toJson(toSend));
    }

    class SendSuggestionsHelper implements Runnable{
        private Thread currentThread = Thread.currentThread();
        HttpServerExchange exchange;
        UserAnime anime;
        UserSuggestions toSend;
        private boolean finished = false;
        Thread uninterruptable;

        public SendSuggestionsHelper(HttpServerExchange exchange, UserAnime anime, UserSuggestions toSend, Thread uninterruptable) {
            this.exchange = exchange;
            this.anime = anime;
            this.toSend = toSend;
            this.uninterruptable = uninterruptable;
        }

        @Override
        public void run() {
            Anime recommendations;
            try {
                recommendations = Runner.getDatabase().sendRecommendationsHelper(anime.getAnime().getId());
                try {
                    for (Anime toAdd : recommendations.getRecommends()) {
                        toSend.addAnime(toAdd, anime.getGivenScore());
                    }
                } catch (NullPointerException ignored) {
                }
            } catch (IOException | NoDocumentException e) {
                exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                exchange.getResponseSender().send("");
            }
            finished = true;
        }

        public UserAnime getAnime() {
            return anime;
        }

        public boolean isFinished() {
            return finished;
        }

        public void interrupt(UserAnime anime) {
            if (finished) {
                return;
            }
            if (currentThread == uninterruptable) {
                return;
            }
            if (this.anime.equals(anime)){
                finished = true;
                currentThread.interrupt();
            }
        }
    }
}
