package moe.suggestme;

import com.google.gson.Gson;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import moe.suggestme.data.Database;
import moe.suggestme.mediums.Anime;
import moe.suggestme.server.UndertowServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/**
 * Created by Endrik on 21-May-17.
 */


public class Runner {
    private static Config config;
    private static Gson gson;
    private static Database database;
    private static Logger logger;
    private static UndertowServer server;
    private static List<Anime> animeList = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        config = ConfigFactory.parseFile(new File("config.conf")).withFallback(ConfigFactory.load());
        config.checkValid(ConfigFactory.load());

        gson = new Gson();

        database = new Database();
        Database.DatabaseTimer.addDatabase(database);
        new Timer().schedule(new Database.DatabaseTimer(), 0, 1000 * 60 * 2);

        logger = LogManager.getLogger("Moe"); //TODO: Fix the logger!

        server = new UndertowServer(config.getInt("server.port"));
        server.start();

        /*Document doc = Jsoup.connect("https://myanimelist.net/anime/32901/Eromanga-sensei/userrecs").get();
        Elements eles = doc.select("div.js-scrollfix-bottom");
        for (Element ele:eles){
            Elements eleles = ele.select("span.dark_text");
            for (Element elele:eleles){
                System.out.println(elele.text());
                System.out.println(elele.parent().text());
            }
        }*/

        // database.addAnime(32901, true);

        /*AnimeRecommendsScraper test = new AnimeRecommendsScraper("https://myanimelist.net/anime/31953/a/userrecs");
        test.scrape();
        System.out.println(test);*/
    }

    public static Config getConfig() {
        return config;
    }

    public static Gson getGson() {
        return gson;
    }

    public static Database getDatabase() {
        return database;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static UndertowServer getServer() {
        return server;
    }

    public static List<Anime> getAnimeList() {
        return animeList;
    }
}
