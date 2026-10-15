package nz.ac.auckland.grocerfy.scraper.woolworths;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;

import javax.net.ssl.SSLSession;

/**
 * Test doubles and fixtures shared by the Woolworths tests. The fixtures in src/test/resources/woolworths
 * are trimmed copies of real woolworths.co.nz responses (no cookies or tokens).
 */
final class WoolworthsFakes {

    static final String GRAPHQL = "https://www.woolworths.co.nz/api/graphql?op-name=";

    private WoolworthsFakes() {
    }

    /** An HttpTransport answering from a queue of canned responses, recording every request sent. */
    static class FakeTransport implements HttpTransport {
        private final Deque<Optional<HttpResponse<String>>> queuedResponses = new ArrayDeque<>();
        final List<HttpRequest> sentRequests = new ArrayList<>();

        void enqueue(int status, String body) {
            queuedResponses.add(Optional.of(new FakeResponse(status, body, Map.of())));
        }

        void enqueueWithCookie(int status, String body, String setCookie) {
            queuedResponses.add(Optional.of(new FakeResponse(status, body, Map.of("Set-Cookie", List.of(setCookie)))));
        }

        void enqueueNoResponse() {
            queuedResponses.add(Optional.empty());
        }

        String uriOf(int requestIndex) {
            return sentRequests.get(requestIndex).uri().toString();
        }

        String bodyOf(int requestIndex) {
            return WoolworthsFakes.bodyOf(sentRequests.get(requestIndex));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<HttpResponse<T>> send(HttpRequest request, BodyHandler<T> handler) {
            sentRequests.add(request);
            Optional<HttpResponse<String>> next = queuedResponses.poll();
            if (next == null) {
                throw new AssertionError("Unexpected extra request to " + request.uri());
            }
            return (Optional<HttpResponse<T>>) (Optional<?>) next;
        }
    }

    /** Minimal HttpResponse carrying a status code, a body and optional headers. */
    static class FakeResponse implements HttpResponse<String> {
        private final int status;
        private final String body;
        private final Map<String, List<String>> headers;

        FakeResponse(int status, String body, Map<String, List<String>> headers) {
            this.status = status;
            this.body = body;
            this.headers = headers;
        }

        @Override public int statusCode() { return status; }
        @Override public String body() { return body; }
        @Override public HttpRequest request() { return null; }
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(headers, (name, value) -> true); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return null; }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }

    static String resource(String path) {
        try (InputStream in = WoolworthsFakes.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException exc) {
            throw new IllegalStateException("test resource " + path + " missing");
        }
    }

    /** A SetCartShoppingMode response confirming the given store. */
    static String storeEcho(String pickupId) {
        return "{\"data\":{\"setCartShoppingMode\":{\"shoppingMode\":{\"mode\":\"Pickup\",\"pickupLocationId\":\""
            + pickupId + "\",\"pickupLocation\":{\"name\":\"Test Woolworths\"}}}}}";
    }

    /** A ProductSearch response containing one ProductSummary per sku, all from the given store. */
    static String searchPage(String storeKey, int totalPages, String... skus) {
        StringBuilder results = new StringBuilder();
        for (String sku : skus) {
            if (results.length() > 0) {
                results.append(',');
            }
            results.append("{\"__typename\":\"ProductSummary\",\"sku\":\"").append(sku)
                .append("\",\"productName\":\"Product ").append(sku).append(" 500g\",\"slug\":\"product-").append(sku)
                .append("\",\"storeKey\":\"").append(storeKey)
                .append("\",\"variants\":[{\"unitOfMeasure\":\"EACH\",\"variantPrice\":{\"sellingPrice\":4.5,\"sellingUnit\":\"EA\"}}]}");
        }
        return "{\"data\":{\"My\":{\"products\":{\"totalPages\":" + totalPages
            + ",\"currentPage\":0,\"results\":[" + results + "]}}}}";
    }

    /** Reads the body a request would send. */
    static String bodyOf(HttpRequest request) {
        if (request.bodyPublisher().isEmpty()) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        request.bodyPublisher().get().subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            @Override public void onNext(ByteBuffer item) { body.append(StandardCharsets.UTF_8.decode(item)); }
            @Override public void onError(Throwable throwable) { throw new AssertionError(throwable); }
            @Override public void onComplete() { }
        });
        return body.toString();
    }
}
