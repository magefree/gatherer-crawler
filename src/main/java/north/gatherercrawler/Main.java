    package north.gatherercrawler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import north.gatherercrawler.util.CardsList;
import north.gatherercrawler.util.ParseQueue;
import north.gatherercrawler.util.ParsedList;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author North
 */
public class Main {

    private static void readCardsFromFile() {
        try {
            // Open the file
            FileInputStream fstream = new FileInputStream("cards-data.txt");
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                if (strLine.length() > 0) {
                    Card card = new Card(strLine);
                    CardsList.add(card);
                    ParsedList.add(card.getMultiverseId());
                }
            }
            //Close the input stream
            in.close();
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        List<String> sets = new ArrayList<String>();

        // Change to false if you only want to add a specific set
        if (false) {
            Document doc = Jsoup.connect("http://gatherer.wizards.com/Pages/Advanced.aspx").get();
            Elements select = doc.select("#autoCompleteSourceBoxsetAddText0_InnerTextBoxcontainer a");
            if (!select.isEmpty()) {
                for (Element element : select) {
//                    if (!element.text().equals("Unglued") && !element.text().equals("Unhinged")) {
                    if (element.text().startsWith("Magic: The Gathering")) {
                        sets.add(element.text());
                        System.out.println("SetFound: " + element.text());
                    }
                }
            }
        }
//        sets.add("Invasion");
//        sets.add("Magic:+The+Gathering%E2%80%94Conspiracy");
//        sets.add("Dragons of Tarkir");
//        sets.add("Duel Decks Anthology");
        sets.add("Tempest Remastered");
//        sets.add("Duel Decks: Speed vs. Cunning");

//        sets.add("Starter 2000");
//        sets.add("Masters Edition III");
//        sets.add("Masters Edition IV");

        readCardsFromFile();

        StringBuilder sb = new StringBuilder();
        int added = 0;
        for (String set : sets) {
            sb.append("|[\"").append(set.replace(" ", "+")).append("\"]");
            added++;

            if (added % 20 == 0 || added == sets.size()) {
                int retries = 30;
                boolean done = false;
                int page = 0;
                Integer lastFirstMultiverseId = null;
                while (retries > 0 && !done) {                    
                    boolean newIteration = true;
                    String url = "http://gatherer.wizards.com/Pages/Search/Default.aspx?page=" + page +"&action=advanced&output=checklist&set=" + sb.toString();
                    Connection connection = Jsoup.connect(url);
                    connection.timeout(300000);
                    Document doc = connection.get();
                    System.out.println(url);

                    Elements select = doc.select(".checklist .name a");
                    if (!select.isEmpty()) {
                        for (Element element : select) {
                            Integer multiverseId = Integer.parseInt(element.attr("href").replace("../Card/Details.aspx?multiverseid=", ""));
                            if (newIteration) {
                                if (lastFirstMultiverseId != null && lastFirstMultiverseId.intValue() == multiverseId) {
                                    done = true;
                                    break;
                                }
                                lastFirstMultiverseId = multiverseId;
                                newIteration = false;
                            }
                            System.out.println("multiverseId=" + multiverseId);
                            if (!ParsedList.contains(multiverseId)) {
                                ParseQueue.add(multiverseId);
                            }
                        }
                    }
                    page++;
                    
                }

                if (!done) {
                    System.out.println("Error accured");
                }
                sb = new StringBuilder();
                Thread.sleep(1000);
            }
        }


        Thread t = new ThreadStarter();
        t.start();
    }
}
