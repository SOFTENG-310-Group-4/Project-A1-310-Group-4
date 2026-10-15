package nz.ac.auckland.grocerfy.apiproxy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the API proxy client, bound from properties with prefix {@code
 * apiproxy}.
 */
@Component
@ConfigurationProperties(prefix = "apiproxy")
public class ApiProxyConfig {

  private String apiKey;
  private String url;
  private String model;

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }
}
