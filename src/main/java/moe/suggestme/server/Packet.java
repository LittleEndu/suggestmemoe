package moe.suggestme.server;

import moe.suggestme.mediums.Anime;
import moe.suggestme.scrapers.NoDocumentException;
import moe.suggestme.user.AnimeEquitable;
import moe.suggestme.user.RecommendedAnime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Endrik on 22-May-17.
 */
public class Packet {
    static class ClientAnimeList {
        public Collection<Anime> animelist;

        public ClientAnimeList(Collection<Anime> animeList) {
            this.animelist = animeList;
        }

        @Override
        public String toString() {
            return "ClientAnimeList{" +
                    "animelist=" + animelist +
                    '}';
        }
    }

    static class UserRecommendations {
        public String type = "recs";
        public Collection<Anime> initial;
        public List<RecommendedAnime> recommendations;

        public UserRecommendations(Collection<Anime> initial) {
            this.initial = initial;
            recommendations = new ArrayList<>();
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        public void addAnime(AnimeEquitable toAdd, float score) {
            if (recommendations.contains(toAdd)) {
                int indexOf = recommendations.indexOf(toAdd);
                if (indexOf == -1) {
                    return;
                }
                RecommendedAnime anime = recommendations.get(indexOf);
                anime.setWeight(anime.getWeight() + (toAdd.getAnime().getScore() / 10 * score / 10));
            } else {
                recommendations.add(new RecommendedAnime(toAdd.getAnime(), (toAdd.getAnime().getScore() / 10 * score / 10)));
            }
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        public List<Anime> getRecommendations() throws NoDocumentException {
            Collections.sort(recommendations);
            Collections.reverse(recommendations);
            List<Anime> toReturn = recommendations.stream().map(RecommendedAnime::getAnime).collect(Collectors.toList());
            toReturn.removeAll(initial);
            return toReturn;
        }
    }
}
