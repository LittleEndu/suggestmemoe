package moe.suggestme.scrapers;

import io.undertow.util.StatusCodes;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Endrik on 22-May-17.
 */
public class AnimeRecommendsScraper {
    private Document doc;
    public List<Integer> recommendations = new ArrayList<>();

    public AnimeRecommendsScraper(int id) throws IOException {
        String url = "https://myanimelist.net/anime/" + Integer.toString(id) + "/a/userrecs";
        Connection connection = Jsoup.connect(url);
        Document doc;
        while (true) {
            try {
                doc = connection.timeout(3000).get();
                break;
            } catch (HttpStatusException e) {
                if (e.getStatusCode() == StatusCodes.NOT_FOUND){
                    doc = null;
                    break;
                }
            } catch (IOException e){
                System.out.println("Connection timed out");
            } finally {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        this.doc = doc;
    }

    public void scrape() throws NoDocumentException {
        if (doc==null){
            throw new NoDocumentException();
        }
        Elements recommendationHolders = doc.select("div.borderClass");
        recommendationHolders.stream().filter(rec -> rec.parent().className().equals("js-scrollfix-bottom-rel")).forEach(rec -> {
            String url = rec.select("div[style=\"margin-bottom: 2px;\"]").get(0).select("a").get(0).attr("href");
            Integer id = Helper.findId(url);
            recommendations.add(id);
        });
    }

    @Override
    public String toString() {
        return "AnimeRecommendsScraper{" +
                "recommendations=" + recommendations +
                '}';
    }
}
