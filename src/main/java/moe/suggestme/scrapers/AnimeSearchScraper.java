package moe.suggestme.scrapers;

import io.undertow.util.StatusCodes;
import moe.suggestme.Runner;
import moe.suggestme.mediums.Anime;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Endrik on 23-May-17.
 */
public class AnimeSearchScraper {
    private Document doc;
    public List<Integer> searchResults = new ArrayList<>();

    public AnimeSearchScraper(String query) throws IOException {
        String url = "https://myanimelist.net/anime.php?q=" + query;
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

    public void scrape() throws NoDocumentException, IOException {
        if (doc == null) {
            throw new NoDocumentException();
        }
        Elements animes = doc.select("div.js-categories-seasonal.js-block-list.list")
                .get(0).select(".hoverinfo_trigger.fw-b.fl-l");
        for (Element element:animes){
            int id = Helper.findId(element.attr("href"));
            searchResults.add(id);
        }
    }
}
