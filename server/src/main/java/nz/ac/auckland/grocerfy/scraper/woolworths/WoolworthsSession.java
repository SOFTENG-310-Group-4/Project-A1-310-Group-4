package nz.ac.auckland.grocerfy.scraper.woolworths;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A Woolworths guest session and the pickup store chosen in it.
 * <p>
 * The site hands out an anonymous "__guest__token" cookie on the first API call. The session keeps that
 * token and sends it back on every API call (it is the only cookie the API needs). The chosen pickup store
 * is tied to the guest session and the token expires after an hour, so refreshing the session always
 * re-applies the current store.
 * <p>
 * Each product's "storeKey" tells us which store's range and prices were returned. It is pinned on the
 * first page after a store change, and a page that disagrees means the session was lost.
 */
class WoolworthsSession {

    private static final Logger LOG = LoggerFactory.getLogger(WoolworthsSession.class);

    /** Guest tokens last 60 minutes; refresh a little early so a request never races the expiry. */
    static final Duration LIFETIME = Duration.ofMinutes(50);

    private static final String GUEST_COOKIE = "__guest__token";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpTransport transport;
    private final Supplier<Instant> clock;

    /** The current guest session's token (null = none yet; the next API call creates one). */
    private String guestToken;
    /** Pickup location id of the store currently selected, re-applied whenever the session is refreshed. */
    private String currentPickupId;
    /** storeKey seen on the first page after the last store change; later pages must match it. */
    private String pinnedStoreKey;
    /** When the current guest session was started (null = no session yet). */
    private Instant startedAt;

    /**
     * @param transport sends the requests
     * @param clock the current time, used to refresh the session before the guest token expires
     */
    WoolworthsSession(HttpTransport transport, Supplier<Instant> clock) {
        this.transport = transport;
        this.clock = clock;
    }

    /**
     * Forgets the session and the store. No request is needed: Woolworths sets the guest token cookie on
     * the first API call, which is the store change that always follows.
     */
    void reset() {
        guestToken = null;
        currentPickupId = null;
        pinnedStoreKey = null;
        startedAt = null;
    }

    /**
     * Selects a pickup store, which decides the product range and prices returned.
     * @param pickupLocationId the store's pickup location id (the "region_cookie" in scrape_targets.json)
     * @return true if Woolworths confirmed the store change
     */
    boolean changeStore(String pickupLocationId) {
        if (!selectStore(pickupLocationId)) {
            return false;
        }
        currentPickupId = pickupLocationId;
        pinnedStoreKey = null; // learnt again from the first page for this store
        return true;
    }

    /**
     * Replaces the guest session (e.g. when the token has expired) and re-selects the current store,
     * since the store choice belongs to the old session.
     * @throws IllegalStateException if the store cannot be selected again
     */
    void refresh() {
        guestToken = null;
        startedAt = null;
        if (currentPickupId != null && !selectStore(currentPickupId)) {
            throw new IllegalStateException("Could not re-select Woolworths store " + currentPickupId
                + " after refreshing the session.");
        }
    }

    /**
     * Refreshes the session ahead of time if the guest token is about to expire.
     */
    void refreshIfExpiring() {
        if (startedAt != null && Duration.between(startedAt, clock.get()).compareTo(LIFETIME) >= 0) {
            LOG.info("Woolworths guest session is nearly an hour old, refreshing...");
            refresh();
        }
    }

    /**
     * Sends a request in the session. If it is rejected, starts a new session on the same store and
     * retries once.
     * @throws IllegalStateException if there is no response, or the retry is rejected too
     */
    <T> HttpResponse<T> sendWithAuth(HttpRequest request, BodyHandler<T> handler) {
        Optional<HttpResponse<T>> responseOptional = exchange(request, handler);
        if (responseOptional.isEmpty()) {
            throw new IllegalStateException("Request returns null, unexpected (is the link valid?) " + request.uri());
        }
        HttpResponse<T> response = responseOptional.get();
        if (response.statusCode() < 400) {
            return response;
        }

        // session expired or rejected: new guest session + same store, then retry once.
        // exchange() attaches the new session's token, so the original request can simply be resent.
        refresh();
        Optional<HttpResponse<T>> retryOptional = exchange(request, handler);
        if (retryOptional.isEmpty() || retryOptional.get().statusCode() >= 400) {
            throw new IllegalStateException("Error with request, does not return after refreshing session: " + request.uri());
        }
        return retryOptional.get();
    }

    /**
     * Checks a page came from the selected store. The first storeKey seen after a store change is pinned;
     * afterwards every storeKey must match it.
     * @param storeKeys the storeKeys of the products on a page
     * @return false if any product came from another store
     */
    boolean matchesPinnedStore(List<String> storeKeys) {
        for (String storeKey : storeKeys) {
            if (pinnedStoreKey == null) {
                pinnedStoreKey = storeKey;
            } else if (!pinnedStoreKey.equals(storeKey)) {
                return false;
            }
        }
        return true;
    }

    String guestToken() {
        return guestToken;
    }

    String pinnedStoreKey() {
        return pinnedStoreKey;
    }

    /**
     * Sends the store change and checks the response echoes the requested store back.
     */
    private boolean selectStore(String pickupLocationId) {
        Optional<HttpResponse<String>> responseOptional = exchange(
            WoolworthsRequests.setStore(pickupLocationId), HttpResponse.BodyHandlers.ofString());
        if (responseOptional.isEmpty() || responseOptional.get().statusCode() >= 400) {
            LOG.warn("Woolworths store change to {} was rejected.", pickupLocationId);
            return false;
        }

        try {
            JsonNode mode = mapper.readTree(responseOptional.get().body())
                .path("data").path("setCartShoppingMode").path("shoppingMode");
            if (!pickupLocationId.equals(mode.path("pickupLocationId").asText())) {
                LOG.warn("Woolworths did not switch to store {}: {}", pickupLocationId, responseOptional.get().body());
                return false;
            }
            LOG.info("Woolworths store set to {}", mode.path("pickupLocation").path("name").asText());
        } catch (JsonProcessingException | IllegalArgumentException exc) {
            LOG.warn("Woolworths store change response unreadable: {}", exc.getLocalizedMessage());
            return false;
        }

        if (startedAt == null) {
            startedAt = clock.get();
        }
        return true;
    }

    /**
     * Sends a request as part of the session: attaches the guest token (if there is one) and remembers
     * any new token the response sets.
     */
    private <T> Optional<HttpResponse<T>> exchange(HttpRequest request, BodyHandler<T> handler) {
        HttpRequest sessionRequest = guestToken == null
            ? request
            : HttpRequest.newBuilder(request, (name, value) -> true)
                .header("Cookie", GUEST_COOKIE + "=" + guestToken)
                .build();
        Optional<HttpResponse<T>> response = transport.send(sessionRequest, handler);
        response.ifPresent(this::rememberGuestToken);
        return response;
    }

    private void rememberGuestToken(HttpResponse<?> response) {
        for (String setCookie : response.headers().allValues("Set-Cookie")) {
            if (setCookie.startsWith(GUEST_COOKIE + "=")) {
                int end = setCookie.indexOf(';');
                guestToken = setCookie.substring(GUEST_COOKIE.length() + 1, end < 0 ? setCookie.length() : end);
            }
        }
    }
}
