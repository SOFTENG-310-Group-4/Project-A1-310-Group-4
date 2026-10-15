package nz.ac.auckland.grocerfy.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.Coordinates;
import nz.ac.auckland.grocerfy.dto.ScraperConfig;

class StoreLocationLookupTest {

    @Test
    void everyBranchHasCoordinatesInsideNewZealand() throws IOException {
        StoreLocationLookup lookup = new StoreLocationLookup();
        try (InputStream in = getClass().getResourceAsStream("/scrape_targets.json")) {
            List<ScraperConfig> configs = new ObjectMapper().readValue(in, new TypeReference<List<ScraperConfig>>() {});
            for (ScraperConfig config : configs) {
                for (Map<String, String> branch : config.branches()) {
                    String name = config.supermarket() + " " + branch.get("store_name");
                    Coordinates c = lookup.find(name);
                    assertNotNull(c, name + " is missing coordinates");
                    assertTrue(c.latitude() > -47.5 && c.latitude() < -34.0, name + " latitude out of NZ range");
                    assertTrue(c.longitude() > 166.0 && c.longitude() < 179.0, name + " longitude out of NZ range");
                }
            }
        }
    }

    @Test
    void albanyBranchesAreDistinct() throws IOException {
        StoreLocationLookup lookup = new StoreLocationLookup();
        assertNotEquals(lookup.find("Pak'nSave Albany"), lookup.find("New World Albany"));
    }

    @Test
    void unknownStoreHasNoCoordinates() throws IOException {
        assertTrue(new StoreLocationLookup().find("Nowhere") == null);
    }
}