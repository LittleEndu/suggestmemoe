package moe.suggestme.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import moe.suggestme.Runner;
import moe.suggestme.mediums.Anime;
import moe.suggestme.mediums.AnimeGenre;
import moe.suggestme.scrapers.*;
import moe.suggestme.user.User;
import moe.suggestme.user.UserAnime;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.xnio.XnioWorker;

import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Endrik on 22-May-17.
 */
public class Database implements Closeable {
    private final DBI dbi;
    private Handle listHandle;


    public class DatabaseHandle {
        public DAO dao;
        public long lastUse;
    }

    public static class DatabaseTimer extends TimerTask {
        private static List<Database> databases = new ArrayList<>();

        public static void addDatabase(Database database) {
            databases.add(database);
        }

        public void run() {
            for (Database d : databases) {
                d.cleanup();
            }
        }
    }

    private Map<Thread, DatabaseHandle> handles = new ConcurrentHashMap<>();

    public Database() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Runner.getConfig().getString("database.url"));
        config.setUsername(Runner.getConfig().getString("database.user"));
        config.setPassword(Runner.getConfig().getString("database.pass"));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("allowMultiQueries", "true");
        config.setMaximumPoolSize(200);

        dbi = new DBI(new HikariDataSource(config));
        listHandle = dbi.open();


        getHandle().createAnimeTable();
        getHandle().createGenresTable();
        getHandle().createRecommendationsTable();
        getHandle().createUsersTable();
        getHandle().createAnimelistTable();


        getHandle().createAnimesView();
        getHandle().createGenresView();
        getHandle().createRecommendationsView();
        getHandle().createUsersView();
        getHandle().createAnimelistView();

        getHandle().createAnimeAddingProcedure();
        getHandle().createAnimeUpdatingProcedure();
        getHandle().createRecommendationAddingProcedure();
        getHandle().createGenreAddingProcedure();
        getHandle().createUserAddingProcedure();
        getHandle().createAnimelistAddingProcedure();
    }

    private DAO getHandle() {
        Thread t = Thread.currentThread();
        DatabaseHandle h = handles.get(t);
        if (h != null) {
            h.lastUse = System.currentTimeMillis();
            return h.dao;
        }
        h = new DatabaseHandle();
        h.dao = dbi.open(DAO.class);
        h.lastUse = System.currentTimeMillis();
        System.out.println("Creating new database connection: " + h);
        handles.put(t, h);
        return h.dao;
    }

    public void cleanup() {
        for (Thread t : handles.keySet()) {
            DatabaseHandle d = handles.get(t);
            if (d.lastUse + (1000 * 60 * 5) < System.currentTimeMillis()) {
                System.out.println("Destroying database connection: " + d.dao);
                d.dao.close();
                handles.remove(t);
            }
        }
    }

    public DBAnime getDBAnime(int id) {
        return getHandle().getAnime(id);
    }

    public Anime getAnime(int id) {
        DBAnime anime = getDBAnime(id);
        return new Anime(id, anime.name, anime.score, getGenres(id));
    }

    public boolean addAnime(int id, boolean withRecommendations) throws IOException {
        AnimeInfoScraper animeInfo = new AnimeInfoScraper(id);
        try {
            try {
                if (!(getDBAnime(id).lastUpdate > System.currentTimeMillis() - Duration.ofDays(7).getSeconds() * 1000)) {
                    animeInfo.scrape();
                }
            } catch (NullPointerException e) {
                animeInfo.scrape();
            }
        } catch (NoDocumentException e) {
            return false;
        }
        getHandle().addAnime(animeInfo.id, animeInfo.name, animeInfo.score);
        for (AnimeGenre genre : animeInfo.genres) {
            addGenre(id, genre);
        }
        return !withRecommendations || addAnimeRecommendations(id);
    }

    public void addGenre(int animeId, AnimeGenre genre) {
        addGenre(getAnime(animeId), genre);
    }

    public void addGenre(Anime anime, AnimeGenre genre) {
        getHandle().addGenre(anime.getId(), genre.getId());
    }

    public List<AnimeGenre> getGenres(int id) {
        List<AnimeGenre> genres = new ArrayList<>();
        List<Map<String, Object>> output2 = listHandle
                .createQuery("SELECT * FROM v_genres WHERE anime_id=:id")
                .bind("id", id)
                .list();
        for (Map<String, Object> entry2 : output2) {
            genres.add(AnimeGenre.fromId(Math.toIntExact((long) entry2.get("genre_id"))));
        }
        return genres;
    }

    public Anime sendRecommendationsHelper(int id) throws IOException, NoDocumentException {
        if (!addAnime(id, true)) {
            return null;
        }
        Anime toReturn = getAnime(id);
        List<Anime> recommendations = new ArrayList<>();
        if (!(getDBAnime(id).lastUpdate > System.currentTimeMillis() - Duration.ofDays(7).getSeconds() * 1000)) {
            new AnimeRecommendsScraper(id).scrape();
        }
        List<Map<String, Object>> output = listHandle
                .createQuery("SELECT DISTINCT * FROM (SELECT id1 as id FROM v_recommendations AS vr1 WHERE vr1.id2=:id UNION SELECT id2 as id FROM v_recommendations AS vr2 WHERE vr2.id1=:id) AS alias")
                .bind("id", id)
                .list();
        recommendations.addAll(output.stream().map(entry -> getAnime(Math.toIntExact((long) entry.get("id")))).collect(Collectors.toList()));
        toReturn.setRecommends(recommendations);
        return toReturn;
    }

    public boolean addAnimeRecommendations(int id) throws IOException {
        DBAnime anime = getDBAnime(id);
        if (anime.lastRecommend > System.currentTimeMillis() - Duration.ofDays(7).getSeconds() * 1000) {
            System.out.println(MessageFormat.format("Skipping animerecs({0}): fresh enough", id));
            return true;
        }
        System.out.println(MessageFormat.format("Starting animerecs({0})", id));
        AnimeRecommendsScraper animeRecs = new AnimeRecommendsScraper(id);
        try {
            animeRecs.scrape();
        } catch (NoDocumentException e) {
            return false;
        }
        Boolean[] done = new Boolean[animeRecs.recommendations.size()];
        for (int i=0; i<animeRecs.recommendations.size(); i++) {
            int finalI = i;
            Runner.getServer().getServer().getWorker().execute(() -> {
                int id2 = animeRecs.recommendations.get(finalI);
                try {
                    getHandle().addRecommendation(id, getAnime(id2).getId());
                } catch (NullPointerException e) {
                    try {
                        addAnime(id2, false);
                        getHandle().addRecommendation(id, id2);
                    } catch (IOException e1) {
                        return;
                    }
                }
                done[finalI] = true;
            });
        }
        while (Arrays.asList(done).contains(false)){
            System.out.println(MessageFormat.format("recs({0}) {1} recs left", id, Collections.frequency(Arrays.asList(done), false)));
            int index = Arrays.asList(done).indexOf(false);
            int id2 = animeRecs.recommendations.get(index);
            try {
                getHandle().addRecommendation(id, getAnime(id2).getId());
                break;
            } catch (NullPointerException e) {
                try {
                    addAnime(id2, false);
                    getHandle().addRecommendation(id, id2);
                } catch (IOException e1) {
                    continue;
                }
            }
            done[index] = true;
        }
        System.out.println(MessageFormat.format("Done with animerecs({0})", id));
        getHandle().updateLastRecommended(id);
        return true;
    }


    public User sendUserAnimelistHelper(String name) throws IOException, NoDocumentException {
        System.out.println(MessageFormat.format("Starting list user({0})", name));
        User toReturn = new User(name);
        UserAnimeListScraper scraper = new UserAnimeListScraper(toReturn);
        scraper.scrape();
        Map<Integer, Float> results = scraper.results;
        List<Integer> keys = new ArrayList<>(results.keySet());
        UserAnime[] toAdd = new UserAnime[results.keySet().size()];
        for (int i = 0; i < keys.size(); i++) {
            int finalI = i;
            Runner.getServer().getServer().getWorker().execute(() -> {
                int id = keys.get(finalI);
                Anime anime = null;
                try {
                    anime = getAnime(id);
                } catch (NullPointerException e) {
                    try {
                        if (addAnime(id, true)) {
                            anime = getAnime(id);
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        return;
                    }
                }
                toAdd[finalI] = new UserAnime(anime, results.get(id));
            });
        }
        while (Arrays.asList(toAdd).contains(null)) {
            System.out.println(MessageFormat.format("user({0}) {1} left", name, Collections.frequency(Arrays.asList(toAdd), null)));
            int index = Arrays.asList(toAdd).indexOf(null);
            if (index == -1) {
                continue;
            }
            int id = keys.get(index);
            Anime anime = null;
            try {
                anime = getAnime(id);
            } catch (NullPointerException e) {
                try {
                    if (addAnime(id, true)) {
                        anime = getAnime(id);
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                    continue;
                }
            }
            toAdd[index] = new UserAnime(anime, results.get(id));
        }
        toReturn.addAll(Arrays.asList(toAdd));
        System.out.println(MessageFormat.format("Done with list user({0})", name));
        return toReturn;
    }

    public void close() {
        getHandle().close();
    }

}
