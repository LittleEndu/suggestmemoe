package moe.suggestme.scrapers;

import io.undertow.util.StatusCodes;
import moe.suggestme.Runner;
import moe.suggestme.data.DBAnime;
import moe.suggestme.mediums.Anime;
import moe.suggestme.user.User;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Endrik on 22-May-17.
 */
public class UserAnimeListScraper {
    private Document doc;
    public Map<Integer, Float> results = new ConcurrentHashMap<>();


    public UserAnimeListScraper(User user) throws IOException {
        String url = MessageFormat.format("https://myanimelist.net/malappinfo.php?u={0}&status=all&type=anime", user.getUsername());
        Document doc;
        while (true) {
            try {
                doc = Jsoup.connect(url).timeout(3000).get();
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

    public void scrape() throws NoDocumentException, IOException {
        if (doc == null) {
            throw new NoDocumentException();
        }
        Elements elements = doc.select("anime");
        for (Element element : elements) {
            int id = Integer.parseInt(element.select("series_animedb_id").get(0).text());
            float givenScore;
            try {
                givenScore = Float.parseFloat(element.select("my_score").get(0).text());
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                givenScore = 0;
            }
            results.put(id, givenScore);
        }
    }

}
