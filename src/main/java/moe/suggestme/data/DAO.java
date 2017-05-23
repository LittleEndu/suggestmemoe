package moe.suggestme.data;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlCall;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.io.Closeable;

/**
 * Created by Endrik on 22-May-17.
 */
@RegisterMapper(DBAnime.Mapper.class)
public interface DAO extends Closeable {
    @SqlUpdate("CREATE TABLE IF NOT EXISTS animes(" +
            "id INT UNSIGNED NOT NULL PRIMARY KEY," +
            "name VARCHAR(255)," +
            "score FLOAT UNSIGNED," +
            "last_updated TIMESTAMP NOT NULL DEFAULT now(6)," +
            "last_recommended TIMESTAMP)")
    void createAnimeTable();

    @SqlUpdate("CREATE VIEW IF NOT EXISTS v_animes AS SELECT id, name, score, last_updated, last_recommended FROM animes")
    void createAnimesView();

    @SqlUpdate("CREATE TABLE IF NOT EXISTS genres(" +
            "anime_id INT UNSIGNED NOT NULL," +
            "genre_id INT UNSIGNED NOT NULL," +
            "PRIMARY KEY (anime_id, genre_id)," +
            "FOREIGN KEY (anime_id) REFERENCES animes (id))")
    void createGenresTable();

    @SqlUpdate("CREATE VIEW IF NOT EXISTS v_genres AS SELECT anime_id, genre_id FROM genres")
    void createGenresView();

    @SqlUpdate("CREATE TABLE IF NOT EXISTS recommendations(" +
            "id1 INT UNSIGNED NOT NULL," +
            "id2 INT UNSIGNED NOT NULL," +
            "PRIMARY KEY (id1, id2)," +
            "FOREIGN KEY (id1) REFERENCES animes (id)," +
            "FOREIGN KEY (id2) REFERENCES animes (id))")
    void createRecommendationsTable();

    @SqlUpdate("CREATE VIEW IF NOT EXISTS v_recommendations AS SELECT id1, id2 FROM recommendations")
    void createRecommendationsView();

    @SqlUpdate("CREATE TABLE IF NOT EXISTS users(" +
            "id INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT," +
            "name VARCHAR(255) NOT NULL)")
    void createUsersTable();

    @SqlUpdate("CREATE VIEW IF NOT EXISTS v_users AS SELECT id, name FROM users")
    void createUsersView();

    @SqlUpdate("CREATE TABLE IF NOT EXISTS animelist(" +
            "user_id INT UNSIGNED NOT NULL," +
            "anime_id INT UNSIGNED NOT NULL," +
            "given_score FLOAT NOT NULL," +
            "PRIMARY KEY (user_id, anime_id)," +
            "FOREIGN KEY (anime_id) REFERENCES animes (id)," +
            "FOREIGN KEY (user_id) REFERENCES users (id))")
    void createAnimelistTable();

    @SqlUpdate("CREATE VIEW IF NOT EXISTS v_animelist AS SELECT user_id, anime_id, given_score FROM animelist")
    void createAnimelistView();

    @SqlUpdate("CREATE PROCEDURE IF NOT EXISTS add_anime (IN in_id INT, IN in_name VARCHAR(255), IN in_score FLOAT UNSIGNED)" +
            "BEGIN" +
            "   INSERT INTO animes (id, name, score) VALUES (in_id, in_name, in_score)" +
            "   ON DUPLICATE KEY UPDATE score=in_score, last_updated=now(6);" +
            "END")
    void createAnimeAddingProcedure();

    @SqlUpdate("CREATE PROCEDURE IF NOT EXISTS update_last_recommended (IN in_id INT)" +
            "BEGIN" +
            "   UPDATE animes SET last_recommended = now(6) WHERE id=in_id;" +
            "END")
    void createAnimeUpdatingProcedure();

    @SqlUpdate("CREATE PROCEDURE IF NOT EXISTS add_recommendation (IN in_id1 INT, IN in_id2 INT)" +
            "BEGIN" +
            "   INSERT IGNORE INTO recommendations (id1, id2) VALUES (in_id1, in_id2);" +
            "END")
    void createRecommendationAddingProcedure();

    @SqlUpdate("CREATE PROCEDURE IF NOT EXISTS add_genre (IN in_anime_id INT, IN in_genre_id INT)" +
            "BEGIN" +
            "   INSERT IGNORE INTO genres (anime_id, genre_id) VALUES (in_anime_id, in_genre_id);" +
            "END")
    void createGenreAddingProcedure();

    @SqlUpdate("CREATE PROCEDURE IF NOT EXISTS add_user (IN in_name VARCHAR(255))" +
            "BEGIN" +
            "   INSERT IGNORE INTO users (name) VALUES (in_name);" +
            "END")
    void createUserAddingProcedure();

    @SqlUpdate("CREATE PROCEDURE IF NOT EXISTS add_animelist (IN in_user_id INT, IN in_anime_id INT, IN in_given_score FLOAT)" +
            "BEGIN" +
            "   INSERT INTO animelist (user_id, anime_id, given_score) VALUES (in_user_id, in_anime_id, in_given_score)" +
            "   ON DUPLICATE KEY UPDATE given_score=in_given_score;" +
            "END")
    void createAnimelistAddingProcedure();

    @SqlQuery("SELECT * FROM v_animes WHERE id=:id")
    DBAnime getAnime(@Bind("id") int id);

    @SqlCall("CALL add_anime(:id, :name, :score)")
    void addAnime(@Bind("id") int id, @Bind("name") String name, @Bind("score") float score);

    @SqlCall("CALL update_last_recommended(:id)")
    void updateLastRecommended(@Bind("id") int animeId);

    @SqlCall("CALL add_genre(:anime, :genre)")
    void addGenre(@Bind("anime") int animeId, @Bind("genre") int genreId);

    @SqlCall("CALL add_recommendation (:id1, :id2)")
    void addRecommendation(@Bind("id1") int id1, @Bind("id2") int id2);

    @SqlCall("CALL add_user (:name)")
    void addUser(@Bind("name") String name);

    @SqlCall("CALL add_animelist(:user_id, :anime_id, :given_score)")
    void addAnimelist(@Bind("user_id") int userId, @Bind("anime_id") int animeIs, @Bind("given_score") float givenScore);

    void close();
}
