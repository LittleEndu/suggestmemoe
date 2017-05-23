package moe.suggestme.user;

import moe.suggestme.scrapers.NoDocumentException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by Endrik on 23-May-17.
 */
public class UserSuggestions{
    User user;
    List<RecommendedAnime> recommendedAnimes;

    public UserSuggestions(User user, List<RecommendedAnime> recommendedAnimes) {
        this.user = user;
        this.recommendedAnimes = recommendedAnimes;
    }

    public UserSuggestions(User user){
        this.user = user;
        this.recommendedAnimes = new ArrayList<>();
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public void addAnime(AnimeEquitable toAdd, float score){
        if (recommendedAnimes.contains(toAdd)) {
            RecommendedAnime anime = recommendedAnimes.get(recommendedAnimes.indexOf(toAdd));
            anime.setWeight(anime.getWeight()+(toAdd.getAnime().getScore()/10*score/10));
        } else {
            recommendedAnimes.add(new RecommendedAnime(toAdd.getAnime(), (toAdd.getAnime().getScore()/10*score/10)));
        }
    }

    public List<RecommendedAnime> getRecommendedAnimes() throws NoDocumentException {
        recommendedAnimes.removeAll(user.getAnimeList());
        Collections.sort(recommendedAnimes);
        Collections.reverse(recommendedAnimes);
        return recommendedAnimes;
    }
}
