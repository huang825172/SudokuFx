import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

public class SudokuFx extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        new Sudoku(stage);
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}

class Sudoku {
    Sudoku(Stage stage) throws Exception {
        new UI(stage, new Status());
    }

    private static class Puzzle {
        private final Storage current;
        private final File saveFile;
        private boolean modified;

        Puzzle(int blackCount, File file) throws Exception {
            this.current = newPuzzle(blackCount);
            this.saveFile = file;
            savePuzzle();
        }

        Puzzle(File file) throws Exception {
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream objIn = new ObjectInputStream(fileIn);
            this.current = (Storage) objIn.readObject();
            this.saveFile = file;
            objIn.close();
            fileIn.close();
        }

        public void savePuzzle() throws Exception {
            FileOutputStream fileOut = new FileOutputStream(this.saveFile);
            ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
            objOut.writeObject(this.current);
            objOut.close();
            fileOut.close();
            modified = false;
        }

        private Storage newPuzzle(int blankCount) {
            if (blankCount > 81) return null;
            Random rand = new Random();
            int[][] puzzle;
            do {
                puzzle = new int[9][9];
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        int x = rand.nextInt(3);
                        int y = rand.nextInt(3);
                        puzzle[i * 3 + x][j * 3 + y] = i * 3 + j + 1;
                    }
                }
                puzzle = getSolve(puzzle);
            } while (puzzle == null);
            for (int i = 0; i < blankCount; i++) {
                while (true) {
                    int x = rand.nextInt(9);
                    int y = rand.nextInt(9);
                    if (puzzle[x][y] != 0) {
                        puzzle[x][y] = 0;
                        break;
                    }
                }
            }
            Storage newOne = new Storage();
            newOne.puzzle = puzzle;
            return newOne;
        }

        public boolean checkPuzzle() {
            int[][] puzzle = getMergedPuzzle();
            return getCheck(puzzle);
        }

        public boolean solvePuzzle() {
            int[][] solve = getSolve(getPuzzle()[0]);
            if (solve != null) {
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        modifyPuzzle(i, j, solve[i][j]);
                    }
                }
                this.current.isAutoSolved = true;
                return true;
            } else {
                return false;
            }
        }

        public int[][][] getPuzzle() {
            int[][][] merged = new int[2][9][9];
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    merged[0][i][j] = this.current.puzzle[i][j];
                    merged[1][i][j] = this.current.answer[i][j];
                }
            }
            return merged;
        }

        public int[][] getMergedPuzzle() {
            int[][] merged = new int[9][9];
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (this.current.puzzle[i][j] != 0) {
                        merged[i][j] = this.current.puzzle[i][j];
                    } else {
                        merged[i][j] = this.current.answer[i][j];
                    }
                }
            }
            return merged;
        }

        public void clearPuzzle() {
            this.current.answer = new int[9][9];
            this.current.isAutoSolved = false;
            modified = true;
            clearTimer();
        }

        public void modifyPuzzle(int x, int y, int value) {
            if (0 <= x && x <= 8 && 0 <= y && y <= 8) {
                if (0 <= value && value <= 9) {
                    if (this.current.puzzle[x][y] == 0) {
                        this.current.answer[x][y] = value;
                        modified = true;
                    }
                }
            }
        }

        public int getTimer() {
            return this.current.timer;
        }

        public void tikTok() {
            this.current.timer++;
            modified = true;
        }

        public void clearTimer() {
            this.current.timer = 0;
            modified = true;
        }

        public boolean isAutoSolved() {
            return this.current.isAutoSolved;
        }

        public void setTimerRun(boolean run) {
            this.current.isTimerRun = run;
        }

        public boolean isTimerRun() {
            return this.current.isTimerRun;
        }

        public boolean isModified() {
            return modified;
        }

        private boolean blockInvalid(int i, int j, int[][] puzzle) {
            int x = (j - (j) % 3) / 3;
            int y = (i - (i) % 3) / 3;
            boolean[] check_box = new boolean[9];
            for (int coords_x = 0; coords_x < 3; coords_x++) {
                for (int coords_y = 0; coords_y < 3; coords_y++) {
                    int cx = x * 3 + coords_x;
                    int cy = y * 3 + coords_y;
                    if (puzzle[cy][cx] > 9 || puzzle[cy][cx] < 0) return true;
                    if (puzzle[cy][cx] == 0) continue;
                    if (check_box[puzzle[cy][cx] - 1]) return true;
                    check_box[puzzle[cy][cx] - 1] = true;
                }
            }
            boolean[] check_column = new boolean[9];
            boolean[] check_row = new boolean[9];
            for (int coords = 0; coords < 9; coords++) {
                if (puzzle[i][coords] != 0) {
                    if (puzzle[i][coords] > 9
                            || puzzle[i][coords] < 0
                            || check_column[puzzle[i][coords] - 1]) return true;
                    check_column[puzzle[i][coords] - 1] = true;
                }
                if (puzzle[coords][j] != 0) {
                    if (puzzle[coords][j] > 9
                            || puzzle[coords][j] < 0
                            || check_row[puzzle[coords][j] - 1]) return true;
                    check_row[puzzle[coords][j] - 1] = true;
                }
            }
            return false;
        }

        private int[][] getFreeCellList(int[][] puzzle) {
            int freeCellCount = 0;
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (puzzle[i][j] == 0) {
                        freeCellCount++;
                    }
                }
            }
            int[][] freeCellList = new int[freeCellCount][2];
            freeCellCount = 0;
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (puzzle[i][j] == 0) {
                        freeCellList[freeCellCount][0] = i;
                        freeCellList[freeCellCount][1] = j;
                        freeCellCount++;
                    }
                }
            }
            return freeCellList;
        }

        private int[][] getSolve(int[][] puzzle) {
            int[][] freeCellList = getFreeCellList(puzzle);

            int k = 0;
            while (k < freeCellList.length) {
                if (puzzle[freeCellList[k][0]][freeCellList[k][1]] == 0) {
                    do {
                        puzzle[freeCellList[k][0]][freeCellList[k][1]]++;
                        if (puzzle[freeCellList[k][0]][freeCellList[k][1]] == 10) break;
                    } while (blockInvalid(freeCellList[k][0], freeCellList[k][1], puzzle));
                    if (blockInvalid(freeCellList[k][0], freeCellList[k][1], puzzle)) {
                        if (k == 0) return null;
                        puzzle[freeCellList[k][0]][freeCellList[k][1]] = 0;
                        k--;
                    } else {
                        k++;
                    }
                } else {
                    puzzle[freeCellList[k][0]][freeCellList[k][1]]++;
                    if (puzzle[freeCellList[k][0]][freeCellList[k][1]] == 10) {
                        if (k == 0) return null;
                        puzzle[freeCellList[k][0]][freeCellList[k][1]] = 0;
                        k--;
                    } else {
                        k++;
                    }
                }
            }

            if (!getCheck(puzzle)) return null;
            return puzzle;
        }

        private boolean getCheck(int[][] puzzle) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (puzzle[i][j] == 0) return false;
                    if (blockInvalid(i, j, puzzle)) return false;
                }
            }
            return true;
        }

        public static class Storage implements Serializable {
            public int[][] puzzle = new int[9][9];
            public int[][] answer = new int[9][9];
            public int timer;
            public boolean isAutoSolved;
            public boolean isTimerRun;
        }
    }

    private static class Status {
        private final Storage current;
        private final String storageUrl = "sudoku.status";
        private final String[][] themeList = {
                {"default", "file:assets/theme/default.css"},
                {"forest", "file:assets/theme/rainforest.css"},
                {"dark", "file:assets/theme/dark.css"}
        };
        private final String[][] keymapList = {
                {"normal", "0,1,2,3,4,5,6,7,8,9"},
                {"speed", "Space,z,x,c,a,s,d,q,w,e"}
        };
        private final KeyCode[][] keymap = {
                {
                        KeyCode.DIGIT0,
                        KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3,
                        KeyCode.DIGIT4, KeyCode.DIGIT5, KeyCode.DIGIT6,
                        KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9
                },
                {
                        KeyCode.SPACE,
                        KeyCode.Z, KeyCode.X, KeyCode.C,
                        KeyCode.A, KeyCode.S, KeyCode.D,
                        KeyCode.Q, KeyCode.W, KeyCode.E
                }
        };
        private final String[][] musicList = {
                {"none", ""},
                {"Lifeline", "assets/music/Lifeline.mp3"},
                {"Dans la maison", "assets/music/Dans_la_maison.mp3"},
                {"Yae Sakura", "assets/music/Yae_Sakura_ver_Piano.mp3"}
        };
        private MediaPlayer mp = null;
        private Thread mpT = null;
        private String playing = "";

        public Puzzle loadedPuzzle = null;

        Status() throws Exception {
            Storage current;
            File cfgFile = new File(storageUrl);
            if (!cfgFile.exists()) {
                if (!cfgFile.createNewFile()) {
                    throw new FileNotFoundException();
                }
                current = new Storage();
                saveStatus();
            } else {
                FileInputStream fileIn = new FileInputStream(storageUrl);
                ObjectInputStream objIn = new ObjectInputStream(fileIn);
                try {
                    current = (Storage) objIn.readObject();
                } catch (Exception e) {
                    File wrongFile = new File(storageUrl);
                    if (!wrongFile.delete()) {
                        throw new Exception("Cannot delete status storage.");
                    }
                    if (!wrongFile.createNewFile()) {
                        throw new FileNotFoundException();
                    }
                    current = (Storage) objIn.readObject();
                }
                objIn.close();
                fileIn.close();
            }
            this.current = current;
        }

        public void saveStatus() throws Exception {
            FileOutputStream fileOut = new FileOutputStream(storageUrl);
            ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
            objOut.writeObject(this.current);
            objOut.close();
            fileOut.close();
        }

        public String[][][] getLists() {
            return new String[][][]{
                    themeList,
                    keymapList,
                    musicList
            };
        }

        public KeyCode[][] getKeymap() {
            return keymap;
        }

        public short getThemeIdx() {
            return this.current.theme;
        }

        public short getKeymapIdx() {
            return this.current.keymap;
        }

        public short getMusicIdx() {
            return this.current.music;
        }

        public void setTheme(short theme) {
            if (0 <= theme && theme <= themeList.length - 1) {
                this.current.theme = theme;
            }
        }

        public void setKeymap(short keymap) {
            if (0 <= keymap && keymap <= keymapList.length - 1) {
                this.current.keymap = keymap;
            }
        }

        public void setMusic(short music) {
            if (0 <= music && music <= musicList.length - 1) {
                this.current.music = music;
            }
        }

        public void updatePlaying() {
            String file = musicList[this.current.music][1];
            if (playing.equals(file)) return;
            if (mp != null) {
                mp.stop();
                mp = null;
            }
            if (mpT != null) {
                mpT.interrupt();
                mpT = null;
            }
            playing = file;
            if (file.equals("")) return;
            mp = new MediaPlayer(
                    new Media(
                            new File(file).toURI().toString()));
            mp.setCycleCount(MediaPlayer.INDEFINITE);
            mp.setStartTime(Duration.ZERO);
            mp.setStopTime(mp.getTotalDuration());
            mpT = new Thread(() -> mp.play());
            mpT.start();
        }

        public static class Storage implements Serializable {
            public short theme = 0;
            public short keymap = 0;
            public short music = 0;
        }
    }

    private static class UI {
        public static final double uiWidth = 320;
        public static final double uiHeight = 80;

        private double xOffset = 0;
        private double yOffset = 0;
        private final Stage stage;

        UI(Stage stage, Status status) {
            this.stage = stage;

            status.updatePlaying();

            Pane root = new Pane();
            root.setStyle("-fx-background-color: transparent");

            Splash splash = new Splash();
            Tool tool = new Tool();
            Game game = new Game(status, stage);
            splash.translateYProperty()
                    .bind(tool.translateYProperty().subtract(uiHeight));
            root.getChildren().addAll(game, splash, tool);


            tool.getNewBtn().setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    if (status.loadedPuzzle == null) {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Create .sudoku File");
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter(
                                        "Puzzle Storage (*.sudoku)", "*.sudoku"));
                        File file = fileChooser.showSaveDialog(stage);
                        if (file != null) {
                            if (!file.getName().contains(".sudoku")) {
                                file = new File(file.getAbsolutePath() + ".sudoku");
                            }
                            try {
                                status.loadedPuzzle = new Puzzle(51, file);
                                status.loadedPuzzle.setTimerRun(true);
                                tool.fileOpened(true);
                                tool.settingsShowed(false);
                                game.showPlayground(true);
                                game.expand(true);
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                    } else {
                        ConfirmDialog confirm = new ConfirmDialog(status,
                                "Puzzle will not be saved automatically.");
                        if (status.loadedPuzzle.isModified()) {
                            confirm.showAndWait();
                        }
                        if (!status.loadedPuzzle.isModified() || confirm.getResult() == ButtonType.OK) {
                            try {
                                status.loadedPuzzle = null;
                                tool.fileOpened(false);
                                tool.settingsShowed(false);
                                game.expand(false);
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                    }
                }
            });
            tool.getSaveBtn().setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    if (status.loadedPuzzle == null) {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Select .sudoku File");
                        fileChooser.getExtensionFilters().add(
                                new FileChooser.ExtensionFilter(
                                        "Puzzle Storage (*.sudoku)", "*.sudoku"));
                        File file = fileChooser.showOpenDialog(stage);
                        if (file != null) {
                            try {
                                status.loadedPuzzle = new Puzzle(file);
                                tool.fileOpened(true);
                                tool.settingsShowed(false);
                                game.showPlayground(true);
                                game.expand(true);
                            } catch (Exception exception) {
                                Alert info = new InfoDialog(status,
                                        "Open",
                                        "Open puzzle FAIL.");
                                info.showAndWait();
                            }
                        }
                    } else {
                        try {
                            status.loadedPuzzle.savePuzzle();
                            tool.fileOpened(true);
                            tool.settingsShowed(false);
                            game.showPlayground(true);
                            game.expand(true);
                            InfoDialog info = new InfoDialog(status,
                                    "Save",
                                    "Puzzle saved.");
                            info.showWithTimerPause();
                        } catch (Exception exception) {
                            InfoDialog info = new InfoDialog(status,
                                    "Save",
                                    "Save puzzle FAIL.");
                            info.showWithTimerPause();
                        }
                    }
                }
            });
            tool.getCfgBtn().setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    if (tool.getIfSetting()) {
                        tool.settingsShowed(false);
                        game.showPlayground(status.loadedPuzzle != null);
                        game.expand(status.loadedPuzzle != null);
                    } else {
                        tool.settingsShowed(true);
                        game.showPlayground(false);
                        game.expand(true);
                    }
                }
            });
            tool.getExitBtn().setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    if (status.loadedPuzzle != null) {
                        ConfirmDialog confirm = new ConfirmDialog(status,
                                "Puzzle will NOT be saved automatically.");
                        if (status.loadedPuzzle.isModified()) {
                            confirm.showAndWait();
                        }
                        if (!status.loadedPuzzle.isModified() || confirm.getResult() == ButtonType.OK) {
                            game.stopTimer();
                            Platform.exit();
                        }
                    } else {
                        game.stopTimer();
                        Platform.exit();
                    }
                }
            });

            enableDrag(tool.getNewBtn());
            enableDrag(tool.getSaveBtn());
            enableDrag(tool.getCfgBtn());
            enableDrag(tool.getExitBtn());

            Scene scene = new Scene(root, uiWidth, uiHeight, true, SceneAntialiasing.BALANCED);
            scene.setFill(null);

            scene.getStylesheets().add(status.getLists()[0][status.getThemeIdx()][1]);

            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.getIcons().add(new Image("file:assets/pic/sudoku.png"));
            stage.show();
        }

        private void enableDrag(Button btn) {
            btn.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    xOffset = e.getSceneX();
                    yOffset = e.getSceneY();
                }
            });
            btn.setOnMouseDragged(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    stage.setX(e.getScreenX() - xOffset);
                    stage.setY(e.getScreenY() - yOffset);
                }
            });
        }

        private static class MovableDialog extends Alert {
            private double xOffset = 0;
            private double yOffset = 0;

            MovableDialog(Status status, AlertType type) {
                super(type);
                this.getDialogPane().getStylesheets().add(status.getLists()[0][status.getThemeIdx()][1]);
                this.setGraphic(null);
                this.initStyle(StageStyle.UNDECORATED);
                this.getDialogPane().setOnMousePressed(e -> {
                    if (e.getButton() == MouseButton.SECONDARY) {
                        xOffset = e.getSceneX();
                        yOffset = e.getSceneY();
                    }
                });
                this.getDialogPane().setOnMouseDragged(e -> {
                    if (e.getButton() == MouseButton.SECONDARY) {
                        this.setX(e.getScreenX() - xOffset);
                        this.setY(e.getScreenY() - yOffset);
                    }
                });
            }
        }

        private static class ConfirmDialog extends MovableDialog {
            public ConfirmDialog(Status status, String content) {
                super(status, AlertType.CONFIRMATION);
                this.setHeaderText("Notice");
                this.setContentText(content);
            }
        }

        private static class InfoDialog extends MovableDialog {
            private final Status status;

            public InfoDialog(Status status, String title, String content) {
                super(status, AlertType.INFORMATION);
                this.status = status;
                this.getDialogPane().getStylesheets().add(status.getLists()[0][status.getThemeIdx()][1]);
                this.setGraphic(null);
                this.initStyle(StageStyle.UNDECORATED);
                this.setHeaderText(title);
                this.setContentText(content);
            }

            public void showWithTimerPause() {
                boolean t;
                if (status.loadedPuzzle != null) {
                    t = status.loadedPuzzle.isTimerRun();
                    status.loadedPuzzle.setTimerRun(false);
                    this.showAndWait();
                    status.loadedPuzzle.setTimerRun(t);
                } else {
                    this.showAndWait();
                }
            }
        }

        private static class Splash extends StackPane {
            Splash() {
                this.setId("splash");
                this.setPrefSize(uiWidth, uiHeight);
                this.setAlignment(Pos.CENTER);

                this.setOpacity(0);
                new Thread(() -> {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Timeline ame = new Timeline();
                    KeyValue kv = new KeyValue(this.opacityProperty(), 1, Interpolator.EASE_BOTH);
                    KeyFrame kf = new KeyFrame(Duration.seconds(1.7), kv);
                    ame.getKeyFrames().add(kf);
                    ame.play();
                }).start();

                Label title = new Label("Sudoku");
                title.setId("splashTitle");
                title.setFont(Font.font(uiHeight * 0.5));
                this.getChildren().add(title);
            }
        }

        private static class Tool extends HBox {
            private final Button newBtn;
            private final Button saveBtn;
            private final Button cfgBtn;
            private final Button exitBtn;

            private boolean setting;

            Tool() {
                this.setId("tool");
                this.setPrefSize(uiWidth, uiHeight);
                this.setTranslateY(uiHeight);

                newBtn = createImageButton("file:assets/pic/new.png");
                newBtn.setId("toolNew");
                bindTooltips(newBtn, "New Puzzle");
                saveBtn = createImageButton("file:assets/pic/open.png");
                saveBtn.setId("toolSave");
                bindTooltips(saveBtn, "Open Puzzle");
                cfgBtn = createImageButton("file:assets/pic/config.png");
                cfgBtn.setId("toolConfig");
                bindTooltips(cfgBtn, "Settings");
                exitBtn = createImageButton("file:assets/pic/exit.png");
                exitBtn.setId("toolExit");
                bindTooltips(exitBtn, "Exit");
                this.getChildren().addAll(newBtn, saveBtn, cfgBtn, exitBtn);

                new Thread(() -> {
                    try {
                        Thread.sleep(2400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Timeline ame = new Timeline();
                    ame.getKeyFrames().add(
                            new KeyFrame(
                                    Duration.seconds(0.5),
                                    new KeyValue(
                                            this.translateYProperty(),
                                            0, Interpolator.EASE_BOTH)
                            )
                    );
                    ame.play();
                }).start();
            }

            private Button createImageButton(String url) {
                Button btn = new Button();
                btn.setPrefSize(uiHeight, uiHeight);
                btn.setGraphic(
                        new ImageView(
                                new Image(
                                        url, uiHeight * 0.5, uiHeight * 0.5,
                                        false, false)));
                btn.setStyle("-fx-background-radius: 0; -fx-background-insets: 0 0 -1 0, 0, 1, 2;");
                return btn;
            }

            private void bindTooltips(Control c, String s) {
                Tooltip t = new Tooltip(s);
                // t.setShowDelay(new Duration(200));
                c.setTooltip(t);
            }

            private void fileOpened(boolean open) {
                Image newIcon, saveIcon;
                if (open) {
                    newIcon = new Image(
                            "file:assets/pic/close.png",
                            uiHeight * 0.5, uiHeight * 0.5,
                            false, false);
                    saveIcon = new Image(
                            "file:assets/pic/save.png",
                            uiHeight * 0.5, uiHeight * 0.5,
                            false, false);
                } else {
                    newIcon = new Image(
                            "file:assets/pic/new.png",
                            uiHeight * 0.5, uiHeight * 0.5,
                            false, false);
                    saveIcon = new Image(
                            "file:assets/pic/open.png",
                            uiHeight * 0.5, uiHeight * 0.5,
                            false, false);
                }
                newBtn.setGraphic(new ImageView(newIcon));
                saveBtn.setGraphic(new ImageView(saveIcon));
                bindTooltips(newBtn, open ? "Close Puzzle" : "New Puzzle");
                bindTooltips(saveBtn, open ? "Save Puzzle" : "Open Puzzle");
            }

            private void settingsShowed(boolean show) {
                Image settingsIcon;
                if (show) {
                    settingsIcon = new Image(
                            "file:assets/pic/back.png",
                            uiHeight * 0.5, uiHeight * 0.5,
                            false, false);
                } else {
                    settingsIcon = new Image(
                            "file:assets/pic/config.png",
                            uiHeight * 0.5, uiHeight * 0.5,
                            false, false);
                }
                cfgBtn.setGraphic(new ImageView(settingsIcon));
                bindTooltips(cfgBtn, show ? "Back" : "Settings");
                setting = show;
            }

            public boolean getIfSetting() {
                return setting;
            }

            public Button getNewBtn() {
                return newBtn;
            }

            public Button getSaveBtn() {
                return saveBtn;
            }

            public Button getCfgBtn() {
                return cfgBtn;
            }

            public Button getExitBtn() {
                return exitBtn;
            }
        }

        private static class Game extends Pane {
            private final Playground playground;
            private final Status status;
            private final Stage stage;

            private final Thread timer;
            private boolean timerPaused;

            Game(Status status, Stage stage) {
                this.status = status;
                this.stage = stage;

                this.setId("game");
                this.setPrefSize(uiWidth, uiWidth + uiHeight);
                this.setTranslateY(-uiWidth);
                this.setOpacity(0);

                playground = new Playground(status);
                Settings settings = new Settings(status, stage);
                settings.translateXProperty().bind(playground.translateXProperty().add(uiWidth));
                this.getChildren().addAll(playground, settings);

                timer = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        synchronized (this) {
                            try {
                                wait(1000);
                                if (status.loadedPuzzle != null) {
                                    if (status.loadedPuzzle.isTimerRun()) {
                                        status.loadedPuzzle.tikTok();
                                    }
                                    Platform.runLater(playground::refreshTimer);
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                });
                timer.start();
            }

            public void expand(boolean status) {
                Timeline ame = new Timeline();
                double targetY;
                if (!status) {
                    targetY = -uiWidth;
                    ame.setOnFinished(e -> stage.setHeight(uiHeight));
                } else {
                    targetY = uiHeight;
                    stage.setHeight(uiWidth + uiHeight * 2);
                    this.setOpacity(1);
                }
                ame.getKeyFrames().add(
                        new KeyFrame(
                                Duration.seconds(0.2),
                                new KeyValue(
                                        this.translateYProperty(),
                                        targetY, Interpolator.EASE_BOTH)
                        )
                );
                ame.play();
            }

            public void showPlayground(boolean show) {
                Timeline ame = new Timeline();
                ame.getKeyFrames().add(
                        new KeyFrame(
                                Duration.seconds(0.1),
                                new KeyValue(
                                        playground.translateXProperty(),
                                        show ? 0 : -uiWidth,
                                        Interpolator.EASE_BOTH)
                        )
                );
                ame.play();
                if (show) playground.refreshPuzzle();
                if (status.loadedPuzzle != null) {
                    if (status.loadedPuzzle.isTimerRun() && !show) {
                        timerPaused = true;
                        status.loadedPuzzle.setTimerRun(false);
                    }
                    if (!status.loadedPuzzle.isTimerRun() && show && timerPaused) {
                        timerPaused = false;
                        status.loadedPuzzle.setTimerRun(true);
                    }
                }
            }

            public void stopTimer() {
                timer.interrupt();
            }

            private static class Playground extends Pane {
                private final Status status;
                private final Label timerText;
                private static final Button[][] sudokuBlocks = new Button[9][9];

                Playground(Status status) {
                    this.status = status;

                    this.setId("gamePlayground");
                    this.setPrefSize(uiWidth, uiWidth);
                    this.setTranslateX(0);

                    HBox toolBar = new HBox();
                    StackPane timerPane = new StackPane();
                    timerPane.setPrefSize(uiHeight, uiHeight);
                    timerText = new Label();
                    timerText.setId("gamePlaygroundTimer");
                    refreshTimer();
                    timerPane.getChildren().add(timerText);
                    Button checkBtn = createImageButton("file:assets/pic/check.png");
                    checkBtn.setId("gamePlaygroundCheck");
                    bindTooltips(checkBtn, "Check Puzzle");
                    Button solveBtn = createImageButton("file:assets/pic/solve.png");
                    solveBtn.setId("gamePlaygroundSolve");
                    bindTooltips(solveBtn, "Solve Puzzle Automatically");
                    Button clearBtn = createImageButton("file:assets/pic/clear.png");
                    clearBtn.setId("gamePlaygroundClear");
                    bindTooltips(clearBtn, "Clear All Answers");
                    toolBar.getChildren().addAll(timerPane, checkBtn, solveBtn, clearBtn);

                    toolBar.setPrefSize(uiWidth, uiHeight);
                    toolBar.setLayoutY(uiWidth);
                    this.getChildren().add(toolBar);

                    GridPane sudokuGrid = new GridPane();
                    for (int i = 0; i < 9; i++) {
                        for (int j = 0; j < 9; j++) {
                            sudokuBlocks[i][j] = new Button("0");
                            sudokuBlocks[i][j].setOnKeyPressed(new blockHandler(i, j, status));
                            sudokuBlocks[i][j].getStyleClass().add("sudokuBtn");
                            sudokuBlocks[i][j].setPrefSize(34, 35);
                            sudokuBlocks[i][j].setStyle("-fx-background-radius: 0; -fx-background-insets: 0 0 -1 0, 0, 1, 2;");
                            sudokuGrid.add(sudokuBlocks[i][j], i, j);
                        }
                    }
                    sudokuGrid.setLayoutX(7);
                    sudokuGrid.setLayoutY(3);
                    this.getChildren().add(sudokuGrid);

                    checkBtn.setOnMouseClicked(e -> {
                        if (e.getButton() == MouseButton.PRIMARY) {
                            if (status.loadedPuzzle.isAutoSolved()) {
                                InfoDialog info = new InfoDialog(status,
                                        "Oops!",
                                        "The puzzle is solved automatically.\nClear to reset.");
                                info.showWithTimerPause();
                            } else {
                                if (status.loadedPuzzle.checkPuzzle()) {
                                    InfoDialog info = new InfoDialog(status,
                                            "Congratulations!",
                                            "Your answer is correct! Timer stopped.");
                                    info.showWithTimerPause();
                                } else {
                                    InfoDialog info = new InfoDialog(status,
                                            "Oops!",
                                            "Your answer is wrong.");
                                    info.showWithTimerPause();
                                }
                            }
                        }
                    });
                    solveBtn.setOnMouseClicked(e -> {
                        if (e.getButton() == MouseButton.PRIMARY) {
                            ConfirmDialog confirm = new ConfirmDialog(status,
                                    "Auto solving will CLEAR & STOP the timer!");
                            confirm.showAndWait();
                            if (confirm.getResult() == ButtonType.OK) {
                                if (status.loadedPuzzle.solvePuzzle()) {
                                    status.loadedPuzzle.clearTimer();
                                    status.loadedPuzzle.setTimerRun(false);
                                    refreshPuzzle();
                                } else {
                                    InfoDialog info = new InfoDialog(status,
                                            "Oops!",
                                            "The puzzle is unsolvable.");
                                    info.showWithTimerPause();
                                }
                            }
                        }
                    });
                    clearBtn.setOnMouseClicked(e -> {
                        if (e.getButton() == MouseButton.PRIMARY) {
                            ConfirmDialog confirm = new ConfirmDialog(status,
                                    "Clear will CLEAR the answer and timer!");
                            confirm.showAndWait();
                            if (confirm.getResult() == ButtonType.OK) {
                                status.loadedPuzzle.clearPuzzle();
                                status.loadedPuzzle.setTimerRun(true);
                                refreshPuzzle();
                            }
                        }
                    });
                }

                public void refreshPuzzle() {
                    if (status.loadedPuzzle != null) {
                        int[][][] puzzle = status.loadedPuzzle.getPuzzle();
                        for (int i = 0; i < 9; i++) {
                            for (int j = 0; j < 9; j++) {
                                sudokuBlocks[i][j].getStyleClass().remove("puzzleBlock");
                                if (puzzle[0][i][j] != 0) {
                                    sudokuBlocks[i][j].setText(Integer.toString(puzzle[0][i][j]));
                                    sudokuBlocks[i][j].getStyleClass().add("puzzleBlock");
                                } else {
                                    if (puzzle[1][i][j] != 0) {
                                        sudokuBlocks[i][j].setText(Integer.toString(puzzle[1][i][j]));
                                    } else {
                                        sudokuBlocks[i][j].setText("");
                                    }
                                }
                            }
                        }
                    }
                }

                public void refreshTimer() {
                    int time = 0;
                    if (status.loadedPuzzle != null) {
                        time = status.loadedPuzzle.getTimer();
                    }
                    timerText.setText(time / 60 + " Min(s)\n" + time % 60 + " Sec(s)");
                }

                private Button createImageButton(String url) {
                    Button btn = new Button();
                    btn.setPrefSize(uiHeight, uiHeight);
                    btn.setGraphic(
                            new ImageView(
                                    new Image(
                                            url, uiHeight * 0.5, uiHeight * 0.5,
                                            false, false)));
                    btn.setStyle("-fx-background-radius: 0; -fx-background-insets: 0 0 -1 0, 0, 1, 2;");
                    return btn;
                }

                private void bindTooltips(Control c, String s) {
                    Tooltip t = new Tooltip(s);
                    // t.setShowDelay(new Duration(200));
                    c.setTooltip(t);
                }

                private static class blockHandler implements EventHandler<KeyEvent> {
                    private final int x;
                    private final int y;
                    private final Status status;

                    blockHandler(int x, int y, Status status) {
                        this.x = x;
                        this.y = y;
                        this.status = status;
                    }

                    @Override
                    public void handle(KeyEvent keyEvent) {
                        KeyCode keyCode = keyEvent.getCode();
                        KeyCode[] keyMap = status.getKeymap()[status.getKeymapIdx()];
                        for (int i = 0; i < keyMap.length; i++) {
                            if (keyCode == keyMap[i]) {
                                status.loadedPuzzle.modifyPuzzle(x, y, i);
                                refreshBlock();
                                break;
                            }
                        }
                    }

                    public void refreshBlock() {
                        int[][][] puzzle = status.loadedPuzzle.getPuzzle();
                        if (puzzle[0][x][y] == 0) {
                            if (puzzle[1][x][y] != 0) {
                                sudokuBlocks[x][y].setText(Integer.toString(puzzle[1][x][y]));
                            } else {
                                sudokuBlocks[x][y].setText("");
                            }
                            sudokuBlocks[x][y].getStyleClass().remove("puzzleBlock");
                        }
                    }
                }
            }

            private static class Settings extends Pane {
                private final Status status;
                private final Stage stage;
                private final ChoiceBox<String> themeChoice;
                private final ChoiceBox<String> keymapChoice;
                private final ChoiceBox<String> musicChoice;

                Settings(Status status, Stage stage) {
                    this.status = status;
                    this.stage = stage;

                    this.setId("gameSettings");
                    this.setPrefSize(uiWidth, uiWidth + uiHeight);
                    String[][][] lists = status.getLists();

                    themeChoice = createImageChoiceBox(
                            lists[0], status.getThemeIdx(),
                            "file:assets/pic/theme.png", "Set Theme",
                            80, this);
                    themeChoice.setId("gameSettingsThemeChoice");
                    keymapChoice = createImageChoiceBox(
                            lists[1], status.getKeymapIdx(),
                            "file:assets/pic/keymap.png", "Set Keymap",
                            180, this);
                    keymapChoice.setId("gameSettingsKeymapChoice");
                    musicChoice = createImageChoiceBox(
                            lists[2], status.getMusicIdx(),
                            "file:assets/pic/music.png", "Set Music",
                            280, this);
                    musicChoice.setId("gameSettingsMusicChoice");
                }

                private ChoiceBox<String> createImageChoiceBox(
                        String[][] choices, int focus, String img, String tooltips, double y, Pane p) {
                    ChoiceBox<String> choiceBox = new ChoiceBox<>();
                    for (String[] choice : choices) {
                        choiceBox.getItems().add(choice[0]);
                    }

                    choiceBox.setValue(choices[focus][0]);
                    choiceBox.setMinWidth(140);
                    Tooltip tooltip = new Tooltip(tooltips);
                    // tooltip.setShowDelay(new Duration(200));
                    choiceBox.setTooltip(tooltip);
                    choiceBox.setLayoutX(110);
                    choiceBox.setLayoutY(y);
                    choiceBox.setOnAction(e -> saveSettings());

                    ImageView label = new ImageView(new Image(img, 28, 28, false, false));
                    label.setLayoutX(60);
                    label.setLayoutY(y);

                    p.getChildren().addAll(label, choiceBox);
                    return choiceBox;
                }

                public void saveSettings() {
                    String[][][] lists = status.getLists();
                    for (short i = 0; i < lists[0].length; i++) {
                        if (lists[0][i][0].equals(themeChoice.getValue())) {
                            status.setTheme(i);
                            stage.getScene().getStylesheets().clear();
                            stage.getScene().getStylesheets().add(lists[0][i][1]);
                            break;
                        }
                    }
                    for (short i = 0; i < lists[1].length; i++) {
                        if (lists[1][i][0].equals(keymapChoice.getValue())) {
                            status.setKeymap(i);
                            break;
                        }
                    }
                    for (short i = 0; i < lists[2].length; i++) {
                        if (lists[2][i][0].equals(musicChoice.getValue())) {
                            status.setMusic(i);
                            status.updatePlaying();
                            break;
                        }
                    }
                    try {
                        status.saveStatus();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
