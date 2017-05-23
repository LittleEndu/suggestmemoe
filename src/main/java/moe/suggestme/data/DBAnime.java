package moe.suggestme.data;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Created by Endrik on 22-May-17.
 */
public class DBAnime {
    public final int id;
    public final String name;
    public final float score;
    public final long lastUpdate;
    public final long lastRecommend;

    public DBAnime(int id, String name, float score, long lastUpdate, long lastRecommend) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.lastUpdate = lastUpdate;
        this.lastRecommend = lastRecommend;
    }

    public static class Mapper implements ResultSetMapper<DBAnime> {
        @Override
        public DBAnime map(int index, ResultSet r, StatementContext ctx) throws SQLException {
            Timestamp lastRecommended = r.getTimestamp("last_recommended");
            return new DBAnime(
                    r.getInt("id"),
                    r.getString("name"),
                    r.getFloat("score"),
                    r.getTimestamp("last_updated").getTime(),
                    lastRecommended == null ? 0 : r.getTimestamp("last_recommended").getTime()
            );
        }
    }
}
