package nz.ac.auckland.grocerfy.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.Coordinates;
import nz.ac.auckland.grocerfy.dto.ScraperConfig;

/**
 * Read-only lookup of store coordinates, taken from scrape_targets.json.
 * Keys match the store names the scraper saves: "<supermarket> <store_name>".
 * Always reads the main targets file, whichever profile is active, so every store is known.
 */
@Component
public class StoreLocationLookup {

    private final Map<String, Coordinates> byStoreName = new HashMap<>();

    @Autowired
    public StoreLocationLookup() throws IOException {
        this(new ClassPathResource("scrape_targets.json"));
    }

    // package private so tests can supply their own file
    StoreLocationLookup(Resource targets) throws IOException {
        try (InputStream in = targets.getInputStream()) {
            List<ScraperConfig> configs = new ObjectMapper().readValue(in, new TypeReference<List<ScraperConfig>>() {});
            for (ScraperConfig config : configs) {
                for (Map<String, String> branch : config.branches()) {
                    String latitude = branch.get("latitude");
                    String longitude = branch.get("longitude");
                    if (latitude != null && longitude != null) {
                        byStoreName.put(config.supermarket() + " " + branch.get("store_name"),
                                new Coordinates(Double.parseDouble(latitude), Double.parseDouble(longitude)));
                    }
                }
            }
        }
    }

    /** @return the store's coordinates, or null if it has none */
    public Coordinates find(String storeName) {
        return byStoreName.get(storeName);
    }
}