package cs1302.api;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a response from the OMDb API. This is used by Gson to
 * create an object from the JSON response body.
 */
public class OMDbResponse {
    @SerializedName("Title") String title;
    @SerializedName("Director") String director;
    @SerializedName("Poster") String poster;
    @SerializedName("Year") String year;
    String imdbID;
    @SerializedName("Response") Boolean response;
} // OMDbResponse
