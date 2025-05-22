package is.hagi.andri;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * SKUScraper class for scraping SKUs from the Snickers Workwear website.
 * Reads filter links from a CSV, and saves resulting SKUs to a csv.
 *
 * @author Andri Fannar Kristjánsson, Andri@Hilti.is
 * @since 22/05/2025
 * @version 1.0
 */
public class SKUScraper
{
    private static final String PATH_TO_FILTER_LINKS = "FilterSites.csv";
    private static final String PATH_TO_MASTER_CSV = "Filters.csv";
	private static final String DOC_SELECTOR = "article div div span";
		
    /**
     * Main method for the SKUScraper class.
     *
     * @param args Command line arguments. Not used.
     */
    public static void main(String[] args)
    {
        System.out.println("[INFO] Starting Snickers Workwear Filter Scraper...");

        try
        {
            Map<String, String> filterLinks = readFilterLinks(PATH_TO_FILTER_LINKS);
            Map<List<String>, String> scrapedResults = new LinkedHashMap<>();

            for (Map.Entry<String, String> entry : filterLinks.entrySet()) {
                String filterKey = entry.getKey();
                String url = entry.getValue();
                System.out.println("[INFO] Scraping " + filterKey + " -> " + url);
                List<String> skus = getSKUs(url);
                scrapedResults.put(Arrays.asList(filterKey.split("\\|", 2)), String.join(", ", skus));
            }

            updateCSV(PATH_TO_MASTER_CSV, scrapedResults);
            System.out.println("[INFO] CSV update complete.");
        }
        catch (Exception e)
        {
            System.out.println("[Error] Unexpected failure: " + e);
        }
    }


    /**
     * Get SKUs from a given Snickers Workwear URL.
     *
     * @param urlString URL to scrape SKUs from.
     * @return List of SKUs found on the given URL.
     */
    private static List<String> getSKUs(String urlString)
    {
        String content = fetchSite(urlString);
        if (content == null) return Collections.emptyList();

        System.out.println("[INFO] Parsing content...");
        Document doc = Jsoup.parse(content);
        Elements spans = doc.select(DOC_SELECTOR);

        List<String> skus = new ArrayList<>();
        for (Element span : spans)
        {
            String text = span.text();
            if(text.matches("\\d{4}"))  {
                System.out.println("[INFO] Found new SKU: " + text);
                skus.add(text);
            }
        }

        System.out.println("\n[INFO] Content processed.");
        System.out.println("[INFO] Total SKUs: " + (skus.size()));
        return skus;
    }

    /**
     * Connects to the given site and returns resulting HTML.
     * 
     * @param urlString URL of site to connect to.
     * @return HTML of the site, if the connection was successful. Otherwise, returns null.
     */
    private static String fetchSite(String urlString)
    {
        System.out.println("\n[INFO] Attempting to connect to: " + urlString + "...");

        try
        {
            URL url =  new URI(urlString).toURL();
            URLConnection connection = url.openConnection();
            try (Scanner scanner = new Scanner(connection.getInputStream())) {
                scanner.useDelimiter("\\Z");
                System.out.println("[INFO] Connection successful.");
                return scanner.next();
            }
        }
        catch ( Exception e ) {
            System.out.println("[Error] Failed to fetch site: " + e);
            return null;
        }
    }

    /**
     * Reads filter links from a file.
     *
     * @param filePath Path to the file containing the filters
     * @return Returns a map of the filters.
     */
    private static Map<String, String> readFilterLinks(String filePath) {
        System.out.println("[INFO] Reading filter links...");

        Map<String, String> filterLinks = new LinkedHashMap<>();
        try (Scanner scanner = new Scanner(new File(filePath))) {
            boolean isFirstLine = true;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] parts = line.split(",", 3);
                if (parts.length >= 3) {
                    String key = parts[0].trim() + "|" + parts[1].trim();
                    filterLinks.put(key, parts[2].trim() + "&page=30");
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to read filter links: " + e);
        }
        return filterLinks;
    }

    /**
     * Updates the given CSV with the given data.
     *
     * @param csvPath - Path to the CSV to update
     * @param newData - The new data to update the CSV with.
     */
    private static void updateCSV(String csvPath, Map<List<String>, String> newData) {
        System.out.println("[INFO] Updating CSV...");
        System.out.println("[INFO] Backing up CSV...");
        try {
            Files.copy(Path.of(csvPath), Path.of(csvPath + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to back up CSV. Aborting... Error: " + e);
            return;
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM ''yy"));
        String newHeader = "SKUs (" + today + ")";
        List<String[]> updatedRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            String header = reader.readLine();
            if (header == null) throw new IOException("CSV file is empty");

            String[] headerParts = header.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            int newColumnIndex = headerParts.length;
            header += "," + newHeader;
            updatedRows.add(header.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                String group = parts[0].trim();
                String filter = parts[1].trim();

                Set<String> skuSet = new LinkedHashSet<>();
                String oldCell = parts.length > newColumnIndex ? parts[newColumnIndex].trim().replaceAll("^\"|\"$", "") : "";
                if (!oldCell.isEmpty()) {
                    skuSet.addAll(Arrays.asList(oldCell.split(",\\s*")));
                }

                String newSkus = newData.getOrDefault(Arrays.asList(group, filter), "");
                if (!newSkus.isEmpty()) {
                    skuSet.addAll(Arrays.asList(newSkus.split(",\\s*")));
                }

                List<String> sortedSkus = new ArrayList<>(skuSet);
                Collections.sort(sortedSkus);
                String mergedCell = String.join(", ", sortedSkus);
                parts = Arrays.copyOf(parts, newColumnIndex + 1);
                parts[newColumnIndex] = "\"" + mergedCell + "\"";
                updatedRows.add(parts);
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(csvPath))) {
                for (String[] row : updatedRows) {
                    writer.println(String.join(",", row));
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to update CSV: " + e);
        }
        System.out.println("[INFO] CSV updated.");
    }
}