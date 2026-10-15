package nz.ac.auckland.grocerfy.apiproxy;

/**
 * Output from the API proxy. {@code content} is the raw text returned by the model; callers parse
 * it into their own DTOs.
 */
public record ApiProxyResponse(String content) {}
