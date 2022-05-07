package com.game.savetheflorest;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class Game extends Application {

    private static final Random RAND = new Random();
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int PLAYER_SIZE = 60;
    static final int SMOKE_W = 128;
    static final int SMOKE_ROWS = 3;
    static final int SMOKE_COL = 3;
    static final int SMOKE_H = 128;
    static final int EXPLOSION_STEPS = 15;
    static final Image PLAYER_IMG = new Image("file:src/main/java/images/fire-extinguisher.png");
    static final Image CLOUD_IMG = new Image("file:src/main/java/images/cloud.png");
    static final Image SMOKING_IMG = new Image("file:src/main/java/images/explosion.png");
    static final Image GRASS_IMG = new Image("file:src/main/java/images/grass.png");

    static final Image[] FIRES_IMG = {
            new Image("file:src/main/java/images/1.png"),
            new Image("file:src/main/java/images/2.png"),
            new Image("file:src/main/java/images/3.png"),
            new Image("file:src/main/java/images/4.png"),
            new Image("file:src/main/java/images/5.png"),
            new Image("file:src/main/java/images/6.png"),
    };

    final int MAX_FIRES = 10,  MAX_SHOTS = MAX_FIRES * 2;
    boolean gameOver = false;
    private GraphicsContext gc;

    Extinguisher player;
    Grass grass;
    List<Shot> shots;
    List<Sky> sky;
    List<Fire> Fires;

    private double mouseX;
    private int score;
    private int recorde;

    //start
    public void start(Stage stage) throws Exception {
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> run(gc)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        canvas.setCursor(Cursor.MOVE);
        canvas.setOnMouseMoved(e -> mouseX = e.getX());
        canvas.setOnMouseClicked(e -> {
            if(shots.size() < MAX_SHOTS) shots.add(player.shoot());
            if(gameOver) {
                gameOver = false;
                setup();
            }
        });
        setup();
        stage.setScene(new Scene(new StackPane(canvas)));
        stage.setTitle("Save the florest");
        stage.show();

    }

    //setup the game
    private void setup() {
        sky = new ArrayList<>();
        shots = new ArrayList<>();
        Fires = new ArrayList<>();
        player = new Extinguisher(WIDTH / 2, 500, PLAYER_SIZE, PLAYER_IMG);
        grass = new Grass();
        score = 0;
        IntStream.range(0, MAX_FIRES).mapToObj(i -> this.newFire()).forEach(Fires::add);
    }

    //run Graphics
    private void run(GraphicsContext gc) {
        gc.setFill(Color.rgb(90, 172, 182));
        gc.fillRect(0, 0, WIDTH, HEIGHT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font(20));
        gc.setFill(Color.WHITE);
        gc.fillText("Pontos: " + score, 60,  20);

        if(gameOver) {
            gc.setFont(Font.font(35));
            gc.setFill(Color.BLACK);

            if (recorde != 0){
                if(recorde <= score){
                    gc.fillText("A selva queimou \n Sua pontuação é: " + score + "\n Seu novo recorde: " + recorde +" \n Clique na tela para jogar", WIDTH / 2, HEIGHT /2.5);
                } else {
                    gc.fillText("A selva queimou \n Sua pontuação é: " + score + "\n Seu recorde: " + recorde + " \n Clique na tela para jogar", WIDTH / 2, HEIGHT /2.5);
                }
            } else {
                gc.fillText("A selva queimou \n Sua pontuação é: " + score + " \n Clique na tela para jogar", WIDTH / 2, HEIGHT /2.5);
            }
        }

        sky.forEach(Sky::draw);
        grass.draw();
        player.update();
        player.draw();
        player.posX = (int) mouseX;

        Fires.stream().peek(Extinguisher::update).peek(Extinguisher::draw).forEach(e -> {
            if(player.colide(e) && !player.cleaning) {
                player.puttingOutFire();
            }
        });

        if(recorde <= score){
            recorde = score;
        }

        for (int i = shots.size() - 1; i >=0 ; i--) {
            Shot shot = shots.get(i);
            if(shot.posY < 0 || shot.toRemove)  {
                shots.remove(i);
                continue;
            }
            shot.update();
            shot.draw();
            for (Fire fire : Fires) {
                if(shot.colide(fire) && !fire.cleaning) {
                    if(!player.clean){
                        score++;
                        fire.puttingOutFire();
                        shot.toRemove = true;
                    }
                }
            }
        }

        for (int i = Fires.size() - 1; i >= 0; i--){
            if(Fires.get(i).clean)  {
                Fires.set(i, newFire());
            }
        }

        gameOver = player.clean;
        if(RAND.nextInt(400) < 5) {
            sky.add(new Sky());
        }

        for (int i = 0; i < sky.size(); i++) {
            if(sky.get(i).posY > HEIGHT)
                sky.remove(i);
        }
    }

    public class Extinguisher {

        int posX, posY, size;
        boolean cleaning, clean;
        int cleaningStep = 0;
        Image img;

        public Extinguisher(int posX, int posY, int size, Image image) {
            this.posX = posX;
            this.posY = posY;
            this.size = size;
            img = image;
        }

        public Shot shoot() {
            return new Shot(posX + size / 2 - Shot.size / 2, posY - Shot.size);
        }

        public void update() {
            if(cleaning) cleaningStep++;
            clean = cleaningStep > EXPLOSION_STEPS;
        }

        public void draw() {
            if(cleaning) {
                gc.drawImage(SMOKING_IMG, cleaningStep % SMOKE_COL * SMOKE_W, (cleaningStep / SMOKE_ROWS) * SMOKE_H + 1,
                        SMOKE_W, SMOKE_H,
                        posX, posY, size, size);
            }
            else {
                gc.drawImage(img, posX, posY, size, size);
            }
        }

        public boolean colide(Extinguisher other) {
            int d = distance(this.posX + size / 2, this.posY + size /2,
                    other.posX + other.size / 2, other.posY + other.size / 2);
            return d < other.size / 2 + this.size / 2 ;
        }

        public void puttingOutFire() {
            cleaning = true;
            cleaningStep = -1;
        }

    }

    public class Fire extends Extinguisher {
        int SPEED = (score/5) + 2;

        public Fire(int posX, int posY, int size, Image image) {
            super(posX, posY, size, image);
        }

        public void update() {
            super.update();
            if(!cleaning && !clean) posY += SPEED;
            if(posY > HEIGHT) clean = true;
        }
    }

    public class Shot {

        public boolean toRemove;
        int posX, posY, speed = 10;
        static final int size = 6;

        public Shot(int posX, int posY) {
            this.posX = posX;
            this.posY = posY;
        }

        public void update() {
            posY-=speed;
        }

        public void draw() {
            gc.setFill(Color.WHITESMOKE);
            if (score >=50 && score<=70 || score>=120) {
                gc.setFill(Color.GREY);
                speed = 80;
                gc.fillRect(posX-5, posY-10, size+10, size+30);
            } else {
                gc.fillOval(posX, posY, size, size);
            }
        }

        public boolean colide(Extinguisher extinguisher) {
            int distance = distance(this.posX + size / 2, this.posY + size / 2,
                    extinguisher.posX + extinguisher.size / 2, extinguisher.posY + extinguisher.size / 2);
            return distance  < extinguisher.size / 2 + size / 2;
        }
    }

    public class Sky {
        int posX, posY;
        private int h, w, size;

        public Sky() {
            posX = 0;
            posY = RAND.nextInt(HEIGHT);
            size = RAND.nextInt(50);
            w = size + 130;
            h = size + 130;
        }

        public void draw() {
            gc.drawImage(CLOUD_IMG, posX, posY, w, h);
            posX+=3;
        }
    }

    public class Grass {
        int posX, posY = 0;

        public void draw(){
            gc.drawImage(GRASS_IMG, posX, posY);
        }
    }

    Fire newFire() {
        return new Fire(50 + RAND.nextInt(WIDTH - 100), 0, PLAYER_SIZE, FIRES_IMG[RAND.nextInt(FIRES_IMG.length)]);
    }

    int distance(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    public static void main(String[] args) {
        launch();
    }
}