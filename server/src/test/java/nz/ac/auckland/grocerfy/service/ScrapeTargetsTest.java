package nz.ac.auckland.grocerfy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import nz.ac.auckland.grocerfy.dto.ScraperConfig;

/**
 * Checks the scrape target files on the classpath. The "woolworths" profile reads
 * scrape_targets_woolworths.json instead of scrape_targets.json, so its Woolworths entry has to stay
 * the same as the one in the main file.
 */
class ScrapeTargetsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private List<ScraperConfig> read(String file) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/" + file)) {
            assertNotNull(in, file + " should be on the classpath");
            return mapper.readValue(in, new TypeReference<List<ScraperConfig>>() {});
        }
    }

    @Test
    void woolworthsTargetsOnlyListWoolworths() throws IOException {
        List<ScraperConfig> targets = read("scrape_targets_woolworths.json");

        assertEquals(1, targets.size());
        assertEquals("Woolworths", targets.get(0).supermarket());
    }

    @Test
    void woolworthsTargetsMatchTheMainTargetsFile() throws IOException {
        ScraperConfig inMainFile = read("scrape_targets.json").stream()
                .filter(config -> "Woolworths".equals(config.supermarket()))
                .findFirst()
                .orElseThrow();

        assertEquals(inMainFile, read("scrape_targets_woolworths.json").get(0));
    }
}
