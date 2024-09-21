package cs1302.api;

/**
 * Represents a response from the IMDb API. This is used by Gson to
 * create an object from the JSON response body.
 */
public class IMDbResponse {
    IMDbResult[] items;
} // IMDbResponse
