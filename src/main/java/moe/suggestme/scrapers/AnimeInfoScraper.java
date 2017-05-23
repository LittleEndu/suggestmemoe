package moe.suggestme.scrapers;

import io.undertow.util.StatusCodes;
import moe.suggestme.mediums.AnimeGenre;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Endrik on 21-May-17.
 */
public class AnimeInfoScraper {
    private Document doc;
    public int id;
    public String name;
    public float score;
    public List<AnimeGenre> genres = new ArrayList<>();

    public AnimeInfoScraper(int id) throws IOException {
        String url = "https://myanimelist.net/anime/" + Integer.toString(id);
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
                    System.out.println("Interrupted sleep");
                }
            }
        }
        this.doc = doc;
    }

    public void scrape() throws NoDocumentException {
        if (doc==null){
            throw new NoDocumentException();
        }
        // get id from url
        id = Helper.findId(doc.location());
        // get name from item property
        name = doc.select("[itemprop=\"name\"]").get(0).text().replaceAll("[^\\x00-\\x7F]", " ");
        System.out.println("\t" + name);
        // get everything else from info panel
        Elements infoPanelContainer = doc.select("div.js-scrollfix-bottom");
        for (Element infoPanel : infoPanelContainer) {
            Elements darkTextContainer = infoPanel.select("span.dark_text");
            for (Element darkText : darkTextContainer) {
                String s = darkText.text();
                if (s.equals("Genres:")) {
                    for (Element link : darkText.parent().select("a")) {
                        int genreId = Helper.findId(link.attr("href"));
                        genres.add(AnimeGenre.fromId(genreId));
                    }
                } else if (s.equals("Score:")) {
                    String[] textSplit = darkText.parent().text().split(":");
                    try {
                        score = Float.parseFloat(textSplit[textSplit.length - 1].split("\\(")[0]);
                    } catch (NumberFormatException e) {
                        score = 0;
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "AnimeInfoScraper{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", score=" + score +
                ", genres=" + genres +
                '}';
    }
}
