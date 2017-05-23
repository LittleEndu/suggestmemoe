package moe.suggestme.user;

import moe.suggestme.mediums.Anime;

/**
 * Created by Endrik on 23-May-17.
 */
public class RecommendedAnime implements Comparable<RecommendedAnime>, AnimeEquitable {
    Anime anime;
    private float weight;

    public RecommendedAnime(Anime anime, float weight) {
        this.anime = anime;
        this.weight = weight;
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

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    @Override
    public int compareTo(RecommendedAnime o) {
        return Float.compare(weight, o.getWeight());
    }
}
