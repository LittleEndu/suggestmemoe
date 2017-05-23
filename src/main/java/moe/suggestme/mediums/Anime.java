package moe.suggestme.mediums;

import moe.suggestme.user.AnimeEquitable;

import java.util.List;

/**
 * Created by Endrik on 22-May-17.
 */
public class Anime implements AnimeEquitable{
    private final int id;
    private String name;
    private float score;
    private List<AnimeGenre> genres;
    private List<Anime> recommends;

    public Anime(int id){
        this.id = id;
    }

    public Anime(int id, String name, float score, List<AnimeGenre> genres) {
        this.id = id;
        this.name = name;
        this.score = score;
        this.genres = genres;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AnimeEquitable)) return false;

        AnimeEquitable that = (AnimeEquitable) o;

        return (Integer.valueOf(id).equals(that.getAnime().getId()));
    }

    @Override
    public Anime getAnime() {
        return this;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public float getScore() {
        return score;
    }

    public List<AnimeGenre> getGenres() {
        return genres;
    }

    public List<Anime> getRecommends() {
        return recommends;
    }

    public void setRecommends(List<Anime> recommends) {
        this.recommends = recommends;
    }

    @Override
    public String toString() {
        return "Anime{" +
                "name='" + name + '\'' +
                ", recommends=" + recommends +
                '}';
    }
}
