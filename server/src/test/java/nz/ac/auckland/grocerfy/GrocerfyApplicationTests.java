package nz.ac.auckland.grocerfy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GrocerfyApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	/**
	 * Test to ensure that the seed data is loaded correctly into the database. It
	 * checks the counts of products, stores, and store prices to verify that they
	 * match the expected values.
	 * This test assumes that the seed data contains 10 products, 3 stores, and 30
	 * store prices. If the counts do not match, the test will fail, indicating that
	 * the seed data may not have been loaded correctly.
	 */
	@Test
	void seedDataIsLoaded() {
		Integer productCount = jdbcTemplate.queryForObject("select count(*) from products", Integer.class);
		Integer storeCount = jdbcTemplate.queryForObject("select count(*) from stores", Integer.class);
		Integer priceCount = jdbcTemplate.queryForObject("select count(*) from store_prices", Integer.class);

		assertThat(productCount).isEqualTo(10);
		assertThat(storeCount).isEqualTo(3);
		assertThat(priceCount).isEqualTo(30);
	}

}
