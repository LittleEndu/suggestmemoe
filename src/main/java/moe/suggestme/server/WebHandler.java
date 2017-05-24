package moe.suggestme.server;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import moe.suggestme.Runner;
import moe.suggestme.data.DBAnime;
import moe.suggestme.data.Database;
import moe.suggestme.mediums.Anime;
import moe.suggestme.scrapers.AnimeSearchScraper;
import moe.suggestme.scrapers.NoDocumentException;
import moe.suggestme.scrapers.UserAnimeListScraper;
import moe.suggestme.user.RecommendedAnime;
import moe.suggestme.user.User;
import moe.suggestme.user.UserAnime;
import moe.suggestme.user.UserSuggestions;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
            for (SendSuggestionsHelper help:helpers){
                Runner.getServer().getServer().getWorker().execute(help);
            }
            for (UserAnime anime:user.getAnimeList()){
                Map<UserAnime, Boolean> runningMap = new ConcurrentHashMap<>();
                for (SendSuggestionsHelper help:helpers){
                    runningMap.put(help.getAnime(),help.isRunning());
                }
                if (!runningMap.get(anime)){
                    continue;
                }
                System.out.println(MessageFormat.format("suggestions({0}) {1} left", name, Collections.frequency(runningMap.values(), true)));
                Anime recommendations = Runner.getDatabase().sendRecommendationsHelper(anime.getAnime().getId());
                try {
                    for (Anime toAdd:recommendations.getRecommends()){
                        toSend.addAnime(toAdd, anime.getGivenScore());
                        for (SendSuggestionsHelper help:helpers){
                            help.interupt(anime);
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

    void sendSuggestionsAfterPost(HttpServerExchange exchange) {
        Thread uninteruptable = Thread.currentThread();
        Reader r = null;
        try {
            r = new InputStreamReader(exchange.getInputStream(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("");
            return;
        }
        User user = Runner.getGson().fromJson(r, User.class);
        UserSuggestions toSend;
        try {
            List<SendSuggestionsHelper> helpers = new ArrayList<>();
            toSend = new UserSuggestions(user);
            helpers.addAll(user.getAnimeList().stream().map(anime -> new SendSuggestionsHelper(exchange, anime, toSend, uninteruptable)).collect(Collectors.toList()));
            for (SendSuggestionsHelper help : helpers) {
                Runner.getServer().getServer().getWorker().execute(help);
            }
            for (UserAnime anime : user.getAnimeList()) {
                Map<UserAnime, Boolean> runningMap = new ConcurrentHashMap<>();
                for (SendSuggestionsHelper help : helpers) {
                    runningMap.put(help.getAnime(), help.isRunning());
                }
                if (!runningMap.get(anime)) {
                    continue;
                }
                System.out.println(MessageFormat.format("suggestions from POST {0} left", Collections.frequency(runningMap.values(), true)));
                Anime recommendations = Runner.getDatabase().sendRecommendationsHelper(anime.getAnime().getId());
                try {
                    for (Anime toAdd : recommendations.getRecommends()) {
                        toSend.addAnime(toAdd, anime.getGivenScore());
                        for (SendSuggestionsHelper help : helpers) {
                            help.interupt(anime);
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
        private Thread currentThead = Thread.currentThread();
        HttpServerExchange exchange;
        UserAnime anime;
        UserSuggestions toSend;
        private boolean isRunning = false;
        Thread uninteruptable;

        public SendSuggestionsHelper(HttpServerExchange exchange, UserAnime anime, UserSuggestions toSend, Thread uninteruptable) {
            this.exchange = exchange;
            this.anime = anime;
            this.toSend = toSend;
            this.uninteruptable = uninteruptable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SendSuggestionsHelper helper = (SendSuggestionsHelper) o;

            return anime.equals(helper.anime);

        }

        @Override
        public int hashCode() {
            return anime.hashCode();
        }

        @Override
        public void run() {
            isRunning = true;
            Anime recommendations;
            try {
                recommendations = Runner.getDatabase().sendRecommendationsHelper(anime.getAnime().getId());
            } catch (IOException | NoDocumentException e) {
                exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                exchange.getResponseSender().send("");
                isRunning = false;
                return;
            }
            try {
                for (Anime toAdd:recommendations.getRecommends()){
                    toSend.addAnime(toAdd, anime.getGivenScore());
                }
            } catch (NullPointerException e) {
            }
            isRunning = false;
        }

        public UserAnime getAnime() {
            return anime;
        }

        public boolean isRunning(){
            return isRunning;
        }

        public void interupt(UserAnime anime){
            if (currentThead==uninteruptable) {
                return;
            }
            if (this.anime.equals(anime)){
                currentThead.interrupt();
                isRunning = false;
            }
        }
    }
}
