package moe.suggestme.user;

import moe.suggestme.mediums.Anime;
import moe.suggestme.scrapers.NoDocumentException;
import moe.suggestme.scrapers.UserAnimeListScraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Endrik on 22-May-17.
 */
public class User {
    private String username;
    private List<UserAnime> animeList = new ArrayList<>();

    public User(String username, List<UserAnime> animeList) {
        this.username = username;
        this.animeList = animeList;
    }

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public List<UserAnime> getAnimeList() throws NoDocumentException {
        if (animeList == null) {
            try {
                UserAnimeListScraper listScraper = new UserAnimeListScraper(this);
                listScraper.scrape();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return animeList;
    }

    public void addAnime(UserAnime anime) {
        animeList.add(anime);
    }

    public void addAnime(Anime anime, float score) {
        animeList.add(new UserAnime(anime, score));
    }

    public void addAll(Collection<UserAnime> animes) {
        animeList.addAll(animes);
    }
}
