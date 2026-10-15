package nz.ac.auckland.grocerfy.apiproxy;

/** Runtime exception for failures in the API proxy layer. */
public class ApiProxyException extends RuntimeException {

  public ApiProxyException(String message) {
    super(message);
  }

  public ApiProxyException(String message, Throwable cause) {
    super(message, cause);
  }
}
