package sample;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.*;
import java.util.stream.Collectors;

public class Main extends Application {

    private final ObservableList<Button> buttons = FXCollections.observableArrayList();
    private ObservableList<String> stylesheets;
    private StringProperty cssPath = new SimpleStringProperty();
    private static final String CSS = "E:/Main.css";
    private ListView<Button> list;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = createRoot(primaryStage);

        Scene scene = new Scene(root, 650, 350);
        cssPath.set(CSS);
        stylesheets = scene.getStylesheets();
        stylesheets.add("file:///" + CSS);


        primaryStage.setTitle("JavaFX Bootstrap Buttons");

        primaryStage.setScene(scene);
        primaryStage.show();

        final FileService service = watch();
        service.start();
        cssPath.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && Files.exists(Paths.get(newValue))) {
                stylesheets.clear();
                stylesheets.add("file:///" + cssPath.get());
                service.restart();
            }
        });

    }

    private Parent createRoot(Stage stage) {
        return create1(stage);
    }

    private Parent create1(Stage stage) {


        HBox topBox = new HBox();
        topBox.setAlignment(Pos.BASELINE_CENTER);
        topBox.setSpacing(5);
        topBox.setPadding(new Insets(10));

        list = new ListView<>();
        list.setItems(buttons);
        list.setOrientation(Orientation.HORIZONTAL);

        HBox bottomBox = new HBox();
        bottomBox.setSpacing(5);
        bottomBox.setPadding(new Insets(10));

        createButton("JavaFX");
        createButton("Default", "btn", "btn-default");
        createButton("Primary", "btn", "btn-primary");
        createButton("Success", "btn", "btn-success");
        createButton("Danger", "btn", "btn-danger");
        createButton("Warning", "btn", "btn-warning");
        createButton("Info", "btn", "btn-info");


        Button add = new Button("Add");

        Button remove = new Button("Remove");

        TextField buttonText = new TextField();
        TextField style = new TextField();
        Button update = new Button("Update");

        topBox.getChildren().addAll(add, remove, new Label("Text: "), buttonText, new Label("StyleClass: "), style, update);

        TextField styleSheetPath = new TextField();
        styleSheetPath.textProperty().bind(cssPath);
        Button browse = new Button("...");

        HBox.setHgrow(styleSheetPath, Priority.ALWAYS);

        bottomBox.getChildren().addAll(styleSheetPath, browse);

        add.setOnAction(e -> {

            Button button = createButton(buttonText.getText());
            button.getStyleClass().clear();
            button.getStyleClass().addAll(style.getText().split(" "));

        });
        update.setOnAction(e -> {
            Button button = list.getSelectionModel().getSelectedItem();
            button.setText(buttonText.getText());
            button.getStyleClass().clear();
            button.getStyleClass().addAll(style.getText().split(" "));
        });

        remove.setOnAction(e -> {
            Button button = list.getSelectionModel().getSelectedItem();
            buttons.remove(button);
        });

        browse.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(Paths.get(CSS).getParent().toFile());
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSS", "*.css"), new FileChooser.ExtensionFilter("All", "*.*"));
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                cssPath.set(file.toPath().toString().replace("\\", "/"));
            }

        });

        list.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends Button> ov, Button oldVal, Button newVal) -> {
                    String styleClass = newVal.getStyleClass().stream().collect(Collectors.joining(" "));
                    style.setText(styleClass);
                    buttonText.setText(newVal.getText());
                });


        BorderPane border = new BorderPane();
        border.setTop(topBox);
        border.setCenter(list);
        border.setBottom(bottomBox);

        return border;
    }

    private Button createButton(String name, String... classes) {
        Button button = new Button(name);
        button.getStyleClass().addAll(classes);

        buttons.add(button);

        button.setOnAction(ev -> list.getSelectionModel().select(button));
        return button;
    }

    private FileService watch() throws Exception {
        FileService service = new FileService(cssPath);
        service.setDelay(new Duration(1000));

        service.setOnSucceeded(t -> {
            Boolean result = (Boolean) t.getSource().getValue();
            if (result) {
                stylesheets.clear();
                stylesheets.add("file:///" + cssPath.get());
            }

        });

        return service;
    }


    public static void main(String[] args) {

        launch(args);

    }

    class FileService extends ScheduledService<Boolean> {

        private StringProperty path;

        public FileService(StringProperty path) {
            this.path = path;
        }

        public StringProperty getPath() {
            return path;
        }


        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    Path path = FileSystems.getDefault().getPath(getPath().get());

                    System.out.println("Watching : " + path.getFileName() + " of " + path.getParent());

                    WatchService watchService = FileSystems.getDefault().newWatchService();
                    path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                    final WatchKey wk = watchService.take();
                    for (WatchEvent<?> event : wk.pollEvents()) {
                        final Path changed = (Path) event.context();
                        System.out.println(changed);
                        if (changed.endsWith(path.getFileName())) {
                            System.out.println("My file has changed.");
                            return true;
                        }
                    }

                    return false;
                }
            };
        }
    }


}
