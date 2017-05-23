package moe.suggestme.user;

import moe.suggestme.mediums.Anime;

/**
 * Created by Endrik on 22-May-17.
 */
public class UserAnime implements AnimeEquitable {
    private Anime anime;
    private float givenScore;

    public UserAnime(Anime anime, float givenScore) {
        this.anime = anime;
        this.givenScore = givenScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AnimeEquitable)) return false;

        AnimeEquitable that = (AnimeEquitable) o;

        return anime.equals(that.getAnime());
    }

    @Override
    public int hashCode() {
        return anime.hashCode();
    }

    public Anime getAnime() {
        return anime;
    }

    public float getGivenScore() {
        return givenScore;
    }
}
