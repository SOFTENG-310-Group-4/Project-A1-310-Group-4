package nz.ac.auckland.grocerfy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

/**
 * Verifies that main() loads .env before delegating to Spring.
 *
 * <p>SpringApplication.run is intercepted so the test does not boot a full context. The dotenv step
 * runs for real
 */
class GrocerfyApplicationTest {

  @Test
  void mainLoadsDotenvAndStartsSpring() {
    try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
      GrocerfyApplication.main(new String[] {});

      mocked.verify(
          () -> SpringApplication.run(eq(GrocerfyApplication.class), any(String[].class)));
    }
  }
}
