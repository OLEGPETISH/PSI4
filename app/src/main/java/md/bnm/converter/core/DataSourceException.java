package md.bnm.converter.core;

/**
 * Domain exception thrown when the data source (BNM XML API, parser, or historical dataset)
 * is unreachable, empty, or malformed.
 */
public class DataSourceException extends RuntimeException {
    private final boolean emptyResponse;

    public DataSourceException(String message) {
        this(message, false, null);
    }

    public DataSourceException(String message, boolean emptyResponse) {
        this(message, emptyResponse, null);
    }

    public DataSourceException(String message, Throwable cause) {
        this(message, false, cause);
    }

    public DataSourceException(String message, boolean emptyResponse, Throwable cause) {
        super(message, cause);
        this.emptyResponse = emptyResponse;
    }

    public boolean isEmptyResponse() {
        return emptyResponse;
    }
}
