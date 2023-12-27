package com.example.multiplayergame2;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Game extends Application implements Initializable {

    @FXML
    AnchorPane scene;

    private Player me, opponent;
    private double oX, oY;
    private int playerID;

    private BooleanProperty wPressed = new SimpleBooleanProperty();
    private BooleanProperty sPressed = new SimpleBooleanProperty();
    private BooleanProperty aPressed = new SimpleBooleanProperty();
    private BooleanProperty dPressed = new SimpleBooleanProperty();

    private int movementVariable = 5;

    private Socket socket;

    private ReadFromServer readFromServer;
    private WriteToServer writeToServer;


    public Game() {

    }

    AnimationTimer opponentMoves = new AnimationTimer() {
        @Override
        public void handle(long l) {
            if(opponent != null) {
                opponent.setX(opponent.getX());
                opponent.setY(opponent.getY());
            }
        }
    };

    private class ReadFromServer implements Runnable {

        private DataInputStream dataInputStream;

        public ReadFromServer(DataInputStream dataInputStream) {
            this.dataInputStream = dataInputStream;
            System.out.println("RFS Runnable created");
        }

        @Override
        public void run() {
            while (true) {
                if (opponent != null) {
                    try {
                        final double newX = dataInputStream.readDouble();
                        final double newY = dataInputStream.readDouble();

                        Platform.runLater(() -> {
                            double deltaX = newX - opponent.getX();
                            double deltaY = newY - opponent.getY();
                            opponent.moveX(deltaX);
                            opponent.moveY(deltaY);
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        public void waitForStartMsg() {
            try {
                String startMsg = dataInputStream.readUTF();
                System.out.println("message from server " + startMsg);

                if (playerID == 1) {
                    opponent = new Player(400, 100, 35, 35, Color.RED);
                    scene.getChildren().add(opponent.createPlayer());
                }else if (playerID == 2) {
                    opponent = new Player(100, 100,35, 35, Color.BLUE);
                    scene.getChildren().add(opponent.createPlayer());
                }

                Thread readThread = new Thread(readFromServer);
                Thread writeThread = new Thread(writeToServer);
                readThread.start();
                writeThread.start();
            }catch (IOException e) {
                System.out.println("IOException from waitForStartMsg()");
            }
        }
    }

    private class WriteToServer implements Runnable {

        private DataOutputStream dataOutputStream;

        public WriteToServer(DataOutputStream dataOutputStream) {
            this.dataOutputStream = dataOutputStream;
            System.out.println("WTS Runnable created");
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (me != null) {
                        dataOutputStream.writeDouble(me.getX());
                        dataOutputStream.writeDouble(me.getY());
                        dataOutputStream.flush();
                    }
                    try {
                        Thread.sleep(10);
                    }catch (InterruptedException e) {
                        System.out.println("Interrupted Exception From WTS run()");
                    }
                }
            }catch (IOException e) {
                System.out.println("IOException From WTS run()");
            }
        }
    }

    public void connectToServer() {
        try {
            socket = new Socket("localhost", 1234);
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            playerID = dataInputStream.readInt();
            System.out.println("Your are player#" + playerID);
            if (playerID == 1) {
                System.out.println("Waiting for player number 2");
            }
            readFromServer = new ReadFromServer(dataInputStream);
            writeToServer = new WriteToServer(dataOutputStream);
            readFromServer.waitForStartMsg();

        }catch (IOException e) {
            System.out.println("IOException from connectToServer");
        }
    }

    AnimationTimer moves = new AnimationTimer() {
        @Override
        public void handle(long l) {
            if (wPressed.get()) {
                me.moveY(-movementVariable);
            }
            if (sPressed.get()) {
                me.moveY(movementVariable);
            }
            if (aPressed.get()) {
                me.moveX(-movementVariable);
            }
            if (dPressed.get()) {
                me.moveX(movementVariable);
            }
        }
    };


    public void movementSetup() {
        scene.setOnKeyPressed(keyEvent -> {
            switch (keyEvent.getCode()) {
                case W:
                    wPressed.set(true);
                    break;
                case S:
                    sPressed.set(true);
                    break;
                case A:
                    aPressed.set(true);
                    break;
                case D:
                    dPressed.set(true);
                    break;
                default:
                    break;
            }
        });

        scene.setOnKeyReleased(keyEvent -> {
            switch (keyEvent.getCode()) {
                case W:
                    wPressed.set(false);
                    break;
                case S:
                    sPressed.set(false);
                    break;
                case A:
                    aPressed.set(false);
                    break;
                case D:
                    dPressed.set(false);
                    break;
                default:
                    break;
            }
        });
    }

    public void addPlayer() {

        if (playerID == 1) {
            me = new Player(100, 100, 35, 35, Color.BLUE);
        }else if (playerID == 2){
            me = new Player(400, 100, 35, 35, Color.RED);
        }
        scene.getChildren().add(me.createPlayer());
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(Game.class.getResource("scene.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("BLABLA");
        stage.setScene(scene);
        scene.getRoot().requestFocus();

        stage.show();
    }

    public static void main (String[] args) {
        launch(args);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        connectToServer();
        addPlayer();
        movementSetup();
        moves.start();
        opponentMoves.start();
    }
}
