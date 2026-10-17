package nz.ac.auckland.grocerfy.scraper.woolworths;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends one HTTP request for the Woolworths scraper. The scraper only talks to the network through
 * this interface, so tests can pass in a fake that answers from canned responses.
 */
interface HttpTransport {

    /**
     * @return the response, or Optional.empty() if the request could not be sent
     */
    <T> Optional<HttpResponse<T>> send(HttpRequest request, BodyHandler<T> handler);

    /**
     * The real transport. Woolworths gets its own client without the shared cookie jar: the session is
     * just the guest token, sent as a plain "Cookie: __guest__token=..." header. (The shared jar would also
     * send the site's bot-detection cookies in RFC 2965 "$Version" format, which risks the token not
     * being read.)
     */
    static HttpTransport overNetwork() {
        HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        Logger log = LoggerFactory.getLogger(HttpTransport.class);
        return new HttpTransport() {
            @Override
            public <T> Optional<HttpResponse<T>> send(HttpRequest request, BodyHandler<T> handler) {
                try {
                    HttpResponse<T> response = client.send(request, handler);
                    if (response.statusCode() >= 400) {
                        log.warn("This link has returned an error code {}: {}", response.statusCode(), request.uri());
                    }
                    return Optional.of(response);
                } catch (InterruptedException exc) {
                    log.warn("Caught interrupt from {}, no data returned.", request.uri());
                    Thread.currentThread().interrupt();
                } catch (IOException exc) {
                    log.warn("Caught IO error from {}, no data returned.", request.uri());
                }
                return Optional.empty();
            }
        };
    }
}
