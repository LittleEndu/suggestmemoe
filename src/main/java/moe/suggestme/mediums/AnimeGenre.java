package moe.suggestme.mediums;

/**
 * Created by Endrik on 21-May-17.
 */
public enum AnimeGenre {
    NULL(0),
    ACTION(1),
    ADVENTURE(2),
    CARS(3),
    COMEDY(4),
    DEMENTIA(5),
    DEMONS(6),
    MYSTERY(7),
    DRAMA(8),
    ECCHI(9),
    FANTASY(10),
    GAME(11),
    HENTAI(12),
    HISTORICAL(13),
    HORROR(14),
    KIDS(15),
    MAGIC(16),
    MARTIAL_ARTS(17),
    MECHA(18),
    MUSIC(19),
    PARODY(20),
    SAMURAI(21),
    ROMANCE(22),
    SCHOOL(23),
    SCI_FI(24),
    SHOUJO(25),
    SHOUJO_AI(26),
    SHOUNEN(27),
    SHOUNEN_AI(28),
    SPACE(29),
    SPORTS(30),
    SUPER_POWER(31),
    VAMPIRE(32),
    YAOI(33),
    YURI(34),
    HAREM(35),
    SLICE_OF_LIFE(36),
    SUPERNATURAL(37),
    MILITARY(38),
    POLICE(39),
    PSYCHOLOGICAL(40),
    THRILLER(41),
    SEINEN(42),
    JOSEI(43);

    private final int id;

    AnimeGenre(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static AnimeGenre fromId(int id) {
        for (AnimeGenre genre : AnimeGenre.values()) {
            if (genre.getId() == id) return genre;
        }
        return null;
    }
}
