package cs1302.api;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.net.URLEncoder;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.stage.Window;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import java.util.Random;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.BorderPane;

/**
 * Search for a specific movie by querying the OMDb API, which will return
 * the title of the move, the year it was released, the director, the IMDb ID,
 * and the movie poster. The IMDb ID retrieved from this first API is then
 * used to query the second API, IMDb. The IMDb API returns images related
 * to the movie; the result of which is then displayed in the app.
 */
public class ApiApp extends Application {

    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible
        .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP
        .build();                                     // builds and returns a HttpClient object

    /** Google {@code Gson} object for parsing JSON-formatted strings. */
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        // enable nice output when printing
        .create();                                    // builds and returns a Gson object

    Stage stage;
    Scene scene;
    VBox root;

    HBox urlLayer;
    Button play;
    Text search;
    TextField textField;
    Button get;
    Text titleText;
    ImageView pos;
    ImageView trail;
    OMDbResponse oresponse;
    IMDbResponse iresponse;
    Text directorText;
    Text imdbText;
    HBox top;
    Text yearText;
    ImageView[] imageArray;
    TilePane pictures;
    BorderPane border;

    /**
     * Constructs an {@code ApiApp} object. This default (i.e., no argument)
     * constructor is executed in Step 2 of the JavaFX Application Life-Cycle.
     */
    public ApiApp() {
        root = new VBox();

        this.urlLayer = new HBox(6);
        this.play = new Button("Play");
        this.search = new Text("Search");
        this.textField = new TextField("Gravity");
        this.get = new Button("Go!");
        this.titleText = new Text("Type in a movie and hit \"GO!\" to see details and "
                                    + "related images.");
        this.pos = new ImageView();
        this.directorText = new Text(" ");
        this.imdbText = new Text(" ");
        this.yearText = new Text(" ");
        this.oresponse = new OMDbResponse();
        this.iresponse = new IMDbResponse();
        this.top = new HBox(6);
        this.imageArray = new ImageView[6];
        this.pictures = new TilePane();
        this.border = new BorderPane();
    } // ApiApp

    /**
     * puts together scene graph.
     */
    public void init() {
        urlLayer.getChildren().addAll(play, search, textField, get);
        play.setDisable(true);
        urlLayer.setHgrow(textField, Priority.ALWAYS);
        urlLayer.setAlignment(Pos.CENTER_LEFT);
        urlLayer.setPadding(new Insets(3, 3, 3, 3));

        top.getChildren().addAll(titleText, yearText, directorText, imdbText);
        top.setSpacing(10);

        pictures.setPrefColumns(3);
        pictures.setMinHeight(300);
        defaultImages();

        Runnable task = () -> {
            this.search();
        };
        Runnable two = () -> {
            Runnable r = () -> this.rotate();
            if (play.getText() == "Pause") {
                Platform.runLater(() -> {
                    this.play.setText("Play");
                    thread(r);
                });
            } else {
                Platform.runLater(() -> {
                    this.play.setText("Pause");
                    thread(r);
                });
            }
        };
        get.setOnAction(event -> this.runInNewThread(task));
        play.setOnAction(event -> this.thread(two));

        System.out.println("init() called");
    } // init

    /**
     * Sets the default images and their sizes.
     */
    private void defaultImages() {
        Image def = new Image("file:resources/c1ae864b0ea941be0362c6d45fad10af.jpg");
        Image bottom = new Image("file:resources/HD-wallpaper-dark-black-plain-whole.jpg");

        // add default black background image to TilePane
        for (int i = 0; i < imageArray.length; i++) {
            imageArray[i] = new ImageView();
            imageArray[i].setImage(bottom);
            imageArray[i].setFitHeight(150);
            imageArray[i].setFitWidth(250);
            imageArray[i].setSmooth(true);
            pictures.getChildren().add(imageArray[i]);
        }
        pos.setImage(def);
        pos.setFitWidth(200);
        pos.setFitHeight(300);
        pos.setSmooth(true);

        ImageView left = new ImageView("file:resources/HD-wallpaper-dark-black-plain-whole.jpg");
        ImageView right = new ImageView("file:resources/HD-wallpaper-dark-black-plain-whole.jpg");

        // sets dimensions and images in BorderPane
        left.setFitHeight(300);
        left.setFitWidth(275);
        right.setFitHeight(300);
        right.setFitWidth(275);
        border.setLeft(left);
        border.setRight(right);
        border.setCenter(pos);
        border.setBottom(pictures);
    } // default

    /**
     * New Thread to run search() for the first API which subsequently runs getImage(),
     * secondSearch(), and images().
     *
     * @param task
     */
    public void runInNewThread(Runnable task) {
        Thread taskThread = new Thread(task);
        taskThread.setDaemon(true);
        taskThread.start();
    } // runInNewThread

    /**
     * A new thread to run rotate().
     *
     * @param two
     */
    public void thread(Runnable two) {
        Thread twoThread = new Thread(two);
        twoThread.setDaemon(true);
        twoThread.start();
    } // thread

    /**
     * Creates a URI based off of search query and searches for
     * results from the first API.
     */
    private void search() {
        if (play.getText() == "Pause") {
            Platform.runLater(() -> play.setText("Play"));
        }
        this.get.setDisable(true);
        this.titleText.setText("Loading...");
        this.yearText.setText(" ");
        this.directorText.setText(" ");
        this.imdbText.setText(" ");

        pos.setImage(new Image("file:resources/launch-product.gif"));

        try {
            Thread.sleep(4500);
        } catch (InterruptedException i) {
            Thread.currentThread().interrupt();
        }

        String t = this.textField.getText();     // saves the text in search bar

        String term = URLEncoder.encode(t, StandardCharsets.UTF_8);
        String query = String.format("t=%s", term);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://www.omdbapi.com/?apikey=64af9025&" + query))
            .build();
        String responseBody = " ";

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
            responseBody = response.body();
        } catch (IOException | InterruptedException e) {
            System.out.print("An error occured.");
        }
        oresponse = GSON.fromJson(responseBody, OMDbResponse.class);

        if (oresponse.response == false) {
            titleText.setText("movie not found.");
            pos.setImage(new Image("file:resources/comic-speech-bubble-oh-no-"
                                    + "text-illustration-216308835.jpg"));
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR, "Movie: " + textField.getText() +
                                        "\n Was not found.");
                if (get.isDisable() == true || play.isDisable() == false) {
                    get.setDisable(false);
                    play.setDisable(true);
                }
                if (play.getText() == "Pause") {
                    play.setText("Play");
                }
                alert.show();
            });
        } else {
            getImage(oresponse.poster);
            secondSearch(oresponse.imdbID);
        }
    } // search

    /**
     * Creates a URI based off of the response of the
     * first API and searches for results from the second API.
     *
     * @param imdbID - received by the first API's response
     */
    private void secondSearch(String imdbID) {
        String query = String.format("%s/Full", imdbID);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://imdb-api.com/en/API/Images/k_6bmsx08x/" + query))
            .build();
        String responseBody = " ";

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
            responseBody = response.body();
        } catch (IOException | InterruptedException e) {
            System.out.print("An error occured.");
        }
        iresponse = GSON.fromJson(responseBody, IMDbResponse.class);

        images(iresponse.items);
    } // secondSearch

    /**
     * Loads the movie poster retrieved from the first
     * API into the scene graph.
     *
     * @param poster
     */
    public void getImage(String poster) {
        try {
            pos.setImage(null);
            Image img = new Image(poster);
            pos.setFitWidth(200);
            pos.setFitHeight(300);
            pos.setImage(img);
        } catch (IllegalArgumentException i) {
            System.out.println("An Error has occurred. :(");
        }
        this.get.setDisable(false);
        Platform.runLater(() -> titleText.setText("Title: " + oresponse.title));
        Platform.runLater(() -> yearText.setText("Year: " + oresponse.year));
        Platform.runLater(() -> directorText.setText("Directed by " + oresponse.director));
        Platform.runLater(() -> imdbText.setText("IMDBID : " + oresponse.imdbID));
    } // getImage

    /**
     * Loads images from imageArray[] into TilePane.
     *
     * @param j - from for loop in loadImage()
     *
     * @return r - Runnable event
     */
    private Runnable createRunnable(int j) {
        Runnable r = () -> {
            pictures.getChildren().add(imageArray[j]);
        };
        return r;
    } //createRunnable

    /**
     * Saves the images returned by the second api into an ImageView array, then
     * displays the result.
     *
     * @param items
     */
    private void images(IMDbResult[] items) {
        try {
            Platform.runLater(() -> pictures.getChildren().clear());
            for (int i = 0; i < imageArray.length; i++) {
                imageArray[i] = new ImageView(items[i].image);
                imageArray[i].setFitHeight(150);
                imageArray[i].setFitWidth(250);
                imageArray[i].setSmooth(true);
                Runnable run = createRunnable(i);
                Platform.runLater(run);
            }

        } catch (ArrayIndexOutOfBoundsException a) {
            System.out.println("Error: insufficient images found.");

            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR, "Insufficient images found.");
                if (get.isDisable() == true || play.isDisable() == false) {
                    get.setDisable(false);
                    play.setDisable(true);
                }
                if (play.getText() == "Pause") {
                    play.setText("Play");
                }
                alert.show();
            });
        } catch (NullPointerException n) {
            System.out.println("items is null.");

            Platform.runLater(() -> {
                Alert alert2 = new Alert(AlertType.ERROR, "Insufficient images found.");
                if (get.isDisable() == true || play.isDisable() == false) {
                    get.setDisable(false);
                    play.setDisable(true);
                }
                if (play.getText() == "Pause") {
                    play.setText("Play");
                }
                alert2.show();
            });
        }
        this.play.setDisable(false);
    } // images

    /**
     * Rotates through the images from IMDb API.
     */
    private void rotate() {
        Random random = new Random();
        while (this.play.getText() == "Pause") {
            int x = random.nextInt(6);
            int range = (iresponse.items.length - 8);
            int y = (int)(Math.random() * range) + 7;

            Image img = new Image(iresponse.items[y].image);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException i) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> imageArray[x].setImage(img));
        } // while
    } // rotate

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {

        this.stage = stage;

        // setup scene
        root.getChildren().addAll(urlLayer,top, border);
        scene = new Scene(root);

        // setup stage
        stage.setTitle("ApiApp!");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.setMinHeight(600);
        stage.setMinWidth(600);
        stage.show();
        Platform.runLater(() -> this.stage.setResizable(false));
    } // start
} // ApiApp
