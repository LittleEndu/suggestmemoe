package moe.suggestme.server;

import moe.suggestme.mediums.Anime;

import java.util.Collection;

/**
 * Created by Endrik on 22-May-17.
 */
public class Packet {
    static class JsonRecommendations{
        Anime mainAnime;
        Collection<Anime> recommendations;
        public JsonRecommendations(Anime mainAnime, Collection<Anime> recommendations) {
            this.mainAnime = mainAnime;
            this.recommendations = recommendations;
        }
    }
}
