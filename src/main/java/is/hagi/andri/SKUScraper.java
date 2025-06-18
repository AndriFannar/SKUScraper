package is.hagi.andri;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.Jsoup;

import java.nio.file.StandardCopyOption;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.net.URI;
import java.util.*;
import java.io.*;

/**
 * SKUScraper class for scraping SKUs from the Snickers Workwear website.
 * Reads filter links from a CSV, and saves resulting SKUs to a csv.
 *
 * @author Andri Fannar Kristjánsson, Andri@Hilti.is
 * @since 22/05/2025
 * @version 1.1
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
        System.out.println("[INFO] Starting SKUScraper Version 1.1... \n");

        try
        {
            Map<String, String> filterLinks = readFilterLinks(PATH_TO_FILTER_LINKS);
            Map<List<String>, String> scrapedResults = new LinkedHashMap<>();

            int counter = 1;
            for (Map.Entry<String, String> entry : filterLinks.entrySet()) {
                String filterKey = entry.getKey();
                String url = entry.getValue();

                System.out.println("[INFO] (" + counter++ + "/" + filterLinks.size() + ") Scraping " + entry.getKey() + "...");

                List<String> skus = getSKUs(url);
                if (skus.isEmpty()) {
                    System.out.println("[WARN] No SKUs found for " + filterKey);
                }
                scrapedResults.put(Arrays.asList(filterKey.split("\\|", 2)), String.join(", ", skus));
            }


            updateCSV(PATH_TO_MASTER_CSV, scrapedResults);
            System.out.println("[INFO] Processed " + scrapedResults.size() + " filters. CSV updated.");
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

        System.out.println("[INFO] Content processed.");
        System.out.println("[INFO] Total SKUs: " + (skus.size()) + "\n");
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
        System.out.println("[INFO] Attempting to connect to: " + urlString + "...");

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
        System.out.println("[INFO] Reading filter links... \n");

        Map<String, String> filterLinks = new LinkedHashMap<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] parts;
            boolean isFirstLine = true;
            while ((parts = reader.readNext()) != null) {
                if (isFirstLine) {
                    if (!"Group".equalsIgnoreCase(parts[0].trim()) || !"Filter".equalsIgnoreCase(parts[1].trim())) {
                        System.out.println("[ERROR] Unexpected header format in FilterSites.csv. Expected 'Group' and 'Filter' as first columns.");
                        return filterLinks;
                    }

                    isFirstLine = false;
                    continue;
                }
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
        List<String[]> updatedRows = new ArrayList<>();
        int rowsUpdated = 0;

        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) throw new IOException("CSV file is empty");

            String[] headerParts = allRows.get(0);
            if (!"Group".equalsIgnoreCase(headerParts[0].trim()) || !"Filter".equalsIgnoreCase(headerParts[1].trim())) {
                System.out.println("[ERROR] Unexpected header format in Filters.csv. Expected 'Group' and 'Filter' as first columns.");
                return;
            }

            int skuColumnIndex = -1;
            for (int i = headerParts.length - 1; i >= 0; i--) {
                if (headerParts[i].trim().startsWith("SKUs")) {
                    skuColumnIndex = i;
                    break;
                }
            }
            if (skuColumnIndex == -1) {
                System.out.println("[ERROR] No existing SKU column found. Aborting.");
                return;
            }

            System.out.println("[INFO] Found SKU column at index " + skuColumnIndex);
            updatedRows.add(headerParts);

            for (int i = 1; i < allRows.size(); i++) {
                String[] parts = allRows.get(i);
                String group = parts[0].trim();
                String filter = parts[1].trim();

                Set<String> skuSet = new LinkedHashSet<>();
                String oldCell = parts.length > skuColumnIndex ? parts[skuColumnIndex].trim() : "";
                int originalSize = 0;
                if (!oldCell.isEmpty()) {
                    skuSet.addAll(Arrays.asList(oldCell.split(",\\s*")));
                    originalSize = skuSet.size();
                }

                String newSkus = newData.getOrDefault(Arrays.asList(group, filter), "");
                if (!newSkus.isEmpty()) {
                    skuSet.addAll(Arrays.asList(newSkus.split(",\\s*")));
                }

                if (skuSet.size() > originalSize) {
                    System.out.println("[INFO] Added SKUs for row: Group = \"" + group + "\", Filter = \"" + filter + "\"");
                    rowsUpdated++;
                }

                List<String> sortedSkus = new ArrayList<>(skuSet);
                Collections.sort(sortedSkus);
                String mergedCell = String.join(", ", sortedSkus);
                parts = Arrays.copyOf(parts, Math.max(parts.length, skuColumnIndex + 1));
                parts[skuColumnIndex] = mergedCell;
                updatedRows.add(parts);
            }

            try (CSVWriter writer = new CSVWriter(new FileWriter(csvPath))) {
                for (String[] row : updatedRows) {
                    writer.writeNext(row);
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to update CSV: " + e);
        }
        System.out.println("[INFO] CSV updated. " + rowsUpdated + " row(s) were changed.");
    }
}