package com.mygdx.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.collision.ContactCache;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btPersistentManifold;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.StretchViewport;

import java.util.ArrayList;

public class GameScreenTeams extends BaseBulletTest implements Screen {

    // TODO:
    /*  1. Två lag
        2. Slumpa lagen (kan nedprioriteras)
        3. En text i samma färg ovanför varje boll så man ser vilka lag man är i. (Nedprioriteras)
        4. Det är poängsystemet som ska utvecklas.
            - Enbart 2 score variabler för varje lag.
            - Poäng nere i rendergrejen är allt. Du ska fixa setScore.


           Om man tar ner sin egna så får man minuspoäng
           Om man tar ner opponent så

           Om alla från opponent team är nere så vinner det andra laget, printa i consolen Team 1 wins.


           Glöm inte ifall typ båda ramlar samtidigt så förlorar båda.



     */


    //public AssetManager assets;
    boolean loading;
    BulletEntity player1, player2, player3, player4;
    private Stage stage;
    private Stage scoreStage;

    ClosestRayResultCallback rayTestCB;
    Vector3 rayFrom = new Vector3();
    Vector3 rayTo = new Vector3();

    ModelInstance instance;

    // Game related variables.
    float gameOverTimer = 0;
    public float scoreTimer;
    float contactTime = 0.2f;
    boolean collisionHappened = false;
    boolean gameOverGameScreen = false;
    boolean playerCreated = false;

    // UI variables.
    private Label LabelScorePlayer1,LabelScorePlayer2,LabelScorePlayer3, LabelScorePlayer4;
    private Label.LabelStyle labelStyle;
    private BitmapFont font;

    private Table table;

    // App reference
    private final BaseGame app;

    public static float time;
    final boolean USE_CONTACT_CACHE = true;
    TestContactCache contactCache;

    private Player player_1, player_2, player_3, player_4;

    // Sound
    static GameSound gameSound;
    int collisionUserId0, collisionUserId1;

    // Game mode variables.
    ArrayList<BulletEntity> team1;
    ArrayList<BulletEntity> team2;

    public GameScreenTeams(final BaseGame app)
    {
        this.app = app;
        this.create();
    }

    public class TestContactCache extends ContactCache {
        public Array<BulletEntity> entities;
        @Override
        public void onContactStarted (btPersistentManifold manifold, boolean match0, boolean match1) {
            final int userValue0 = manifold.getBody0().getUserValue();
            final int userValue1 = manifold.getBody1().getUserValue();

            // Take the positions of the colliding balls. Used in the handling of sounds.
            Vector3 p1 = ((btRigidBody) manifold.getBody0()).getCenterOfMassPosition();
            Vector3 p2 = ((btRigidBody) manifold.getBody1()).getCenterOfMassPosition();

            // Set the time which the player1 can receive a points after a collision has happened.
            // 1 second = 30f
            scoreTimer = 210f;  // 210/30 = 7 seconds
            collisionHappened = true;

            if((entities.get(userValue0) != entities.get(0))){
                if (entities.get(userValue0) == entities.get(1) || entities.get(userValue1) == entities.get(1)) {
                    if (match0) {
                        final BulletEntity e = (BulletEntity) (entities.get(userValue0));
                        e.setColor(Color.BLUE);
                        Gdx.app.log(Float.toString(time), "Contact started " + userValue0);
                        collisionUserId0 = userValue0;
                    }
                    if (match1) {
                        final BulletEntity e = (BulletEntity) (entities.get(userValue1));
                        e.setColor(Color.RED);
                        Gdx.app.log(Float.toString(time), "Contact started " + userValue1);
                        collisionUserId1 = userValue1;
                    }
                    // Play the collision sound.
//                    gameSound.playCollisionSound(p1, p2);
                }
            }
        }

        @Override
        public void onContactEnded (btCollisionObject colObj0, boolean match0, btCollisionObject colObj1, boolean match1) {
            final int userValue0 = colObj0.getUserValue();
            final int userValue1 = colObj1.getUserValue();

            if (entities.get(userValue0) == entities.get(1)|| entities.get(userValue1) == entities.get(1)) {
                if (match0) {
                    final BulletEntity e = (BulletEntity) (entities.get(userValue0));
                    e.setColor(Color.BLACK);
                    Gdx.app.log(Float.toString(time), "Contact ended " + collisionUserId1);
                }
                if (match1) {
                    final BulletEntity e = (BulletEntity) (entities.get(userValue1));
                    e.setColor(Color.BLACK);
                    Gdx.app.log(Float.toString(time), "Contact ended " + collisionUserId0);
                }
            }
        }
    }

    @Override
    public void create () {
        super.create();
        this.stage = new Stage(new StretchViewport(Gdx.graphics.getHeight(), Gdx.graphics.getHeight()));
        this.scoreStage = new Stage(new StretchViewport(Gdx.graphics.getHeight(), Gdx.graphics.getHeight()));

        // Create the entities
        world.add("ground", 0f, 0f, 0f).setColor(0.25f + 0.5f * (float) Math.random(), 0.25f + 0.5f * (float) Math.random(), 0.25f + 0.5f * (float) Math.random(), 1f);

        // Load models
        app.assets.load("3d/football2.g3dj", Model.class);
        app.assets.load("3d/apple.g3dj", Model.class);
        app.assets.load("3d/peach.g3dj", Model.class);
        loading = true;

        font = new BitmapFont();
        rayTestCB = new ClosestRayResultCallback(Vector3.Zero, Vector3.Z);

        // Init Score lables.
        labelStyle = new Label.LabelStyle(font, Color.PINK);
        LabelScorePlayer1 = new Label("", labelStyle);
        LabelScorePlayer1.setPosition(20, Gdx.graphics.getHeight() - Gdx.graphics.getHeight() / 20);
        LabelScorePlayer2 = new Label("", labelStyle);
        LabelScorePlayer2.setPosition(20, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20)*2);
        LabelScorePlayer3 = new Label("", labelStyle);
        LabelScorePlayer3.setPosition(20, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20)*3);
        LabelScorePlayer4 = new Label("", labelStyle);
        LabelScorePlayer4.setPosition(20, Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() / 20)*4);

        stage.addActor(LabelScorePlayer1);
        stage.addActor(LabelScorePlayer2);
        stage.addActor(LabelScorePlayer3);
        stage.addActor(LabelScorePlayer4);

        Actor scoreActor = new Image(new Sprite(new Texture(Gdx.files.internal("img/scorebg1.png"))));
        scoreActor.setPosition(0, 0);
        scoreActor.setSize((stage.getWidth()), stage.getHeight());
        scoreStage.addActor(scoreActor);

        scoreStage.getRoot().setPosition(0, stage.getHeight());
        Gdx.input.setInputProcessor(this);

        if (USE_CONTACT_CACHE) {
            contactCache = new TestContactCache();
            contactCache.entities = world.entities;
            // contactCache.setCacheTime(contactTime); // Change the contact time
        }

        // Sound
        gameSound = new GameSound();
        // Play background music.
        // gameSound.playBackgroundMusic(0.45f);

        setTeams();

    }

    @Override
    public boolean tap (float x, float y, int count, int button) {
        shoot(x, y);
        Gdx.app.log("TAP", "Tap");
        return true;
    }

    // Touch controls for the game.
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // shoot(screenX, screenY);
        // Gdx.app.log("SHOOT", "SHOOT");
        Ray ray = camera.getPickRay(screenX, screenY);
        rayFrom.set(ray.origin);
        rayTo.set(ray.direction).scl(50f).add(rayFrom); // 50 meters max from the origin

        // Because we reuse the ClosestRayResultCallback, we need reset it's values
        rayTestCB.setCollisionObject(null);
        rayTestCB.setClosestHitFraction(1f);
        rayTestCB.setRayFromWorld(rayFrom);
        rayTestCB.setRayToWorld(rayTo);

        world.collisionWorld.rayTest(rayFrom, rayTo, rayTestCB);

        if (rayTestCB.hasHit() && (((btRigidBody) player1.body).getCenterOfMassPosition() != null)) {
            rayTestCB.getHitPointWorld(tmpV1);

            //Gdx.app.log("BANG", "BANG");
            Model model;
            ModelBuilder modelBuilder = new ModelBuilder();
            modelBuilder.begin();
            modelBuilder.part("ball", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates,
                    new Material("diffuseGreen", ColorAttribute.createDiffuse(Color.RED))).sphere(1f, 1f, 1f, 10, 10);
            model = modelBuilder.end();

            instance = new ModelInstance(model,tmpV1);

            Vector3 vec = new Vector3((tmpV1.x - ((btRigidBody) player1.body).getCenterOfMassPosition().x), 0, (tmpV1.z - ((btRigidBody) player1.body).getCenterOfMassPosition().z));

            float normFactor = player_1.impulseFactor / vec.len();
            Vector3 normVec = new Vector3(normFactor * vec.x, normFactor * vec.y, normFactor * vec.z);

            player1.body.activate();
            ((btRigidBody) player1.body).applyCentralImpulse(normVec);
        }
        return true;
    }

    boolean up, down, left, right;
    @Override
    public boolean keyDown (int keycode) {
        player2.body.activate();
        Vector3 moveDown = new Vector3(5f, 0f, 0f);
        Vector3 moveUp = new Vector3(-5f, 0f, 0f);
        Vector3 moveLeft = new Vector3(0f, 0f, 5f);
        Vector3 moveRight = new Vector3(0f, 0f, -5f);

        switch(keycode) {
            case Input.Keys.UP: up = true;
                ((btRigidBody) player2.body).applyCentralImpulse(moveUp);
                break;
            case Input.Keys.DOWN: down = true;
                ((btRigidBody) player2.body).applyCentralImpulse(moveDown);
                break;
            case Input.Keys.LEFT: left = true;
                ((btRigidBody) player2.body).applyCentralImpulse(moveLeft);
                break;
            case Input.Keys.RIGHT: right = true;
                ((btRigidBody) player2.body).applyCentralImpulse(moveRight);
                break;
            default: return false;
        }
        return true;
    }

    @Override
    public boolean keyUp (int keycode) {
        super.keyUp(keycode);
        switch(keycode) {
            case Input.Keys.UP: up = false; break;
            case Input.Keys.DOWN: down = false; break;
            case Input.Keys.LEFT: left = false; break;
            case Input.Keys.RIGHT: right = false; break;
            default: return false;
        }
        Gdx.app.log("RAY pick", "RAY pick");
        return true;
    }

    @Override
    public void render () {
        super.render();

        if(instance != null) {
            modelBatch.begin(camera);
            modelBatch.render(instance);
            modelBatch.end();
        }

        if (app.assets.update() && loading) {

            // Load assets for the balls.
            Model football = app.assets.get("3d/football2.g3dj", Model.class);
            String id = football.nodes.get(0).id;

            Model apple = app.assets.get("3d/apple.g3dj", Model.class);
            String id2 = apple.nodes.get(0).id;
            Node node = apple.getNode(id2);
            node.scale.set(0.8f, 0.8f, 0.8f);

            Model peach = app.assets.get("3d/peach.g3dj", Model.class);
            String id3 = peach.nodes.get(0).id;
            Node node2 = peach.getNode(id3);

            // Create the players.
            player_1 = new Player(football, "football");
            world.addConstructor("test1", player_1.bulletConstructor);
            player1 = world.add("test1", 0, 3.5f, 2.5f);
            player1.body.setContactCallbackFlag(1);
            player1.body.setContactCallbackFilter(1);

            player_2 = new Player(football, "football");
            world.addConstructor("test2", player_2.bulletConstructor);
            player2 = world.add("test2", 0, 3.5f, 0.5f);
            player2.body.setContactCallbackFilter(1);

            player_3 = new Player(peach, "peach");
            world.addConstructor("test3", player_3.bulletConstructor);
            player3 = world.add("test3", 0, 3.5f, -2.5f);
            player3.body.setContactCallbackFilter(1);

            player_4 = new Player(peach, "peach");
            world.addConstructor("test4", player_4.bulletConstructor);
            player4 = world.add("test4", 0, 3.5f, -2.5f);
            player4.body.setContactCallbackFilter(1);


            Gdx.app.log("Loaded", "LOADED");
            loading = false;
            playerCreated = true;
        }

        // Count the score timer down.
        if(collisionHappened){
            scoreTimer -= 1f;
            if(scoreTimer < 0) { collisionHappened = false; }
        }

//        if(app.assets.update()){
//            player1.body.setContactCallbackFilter(2);
//            player1.body.setContactCallbackFilter(3);
//            player1.body.setContactCallbackFilter(4);
//
//            player2.body.setContactCallbackFilter(2);
//            player2.body.setContactCallbackFilter(3);
//            player2.body.setContactCallbackFilter(4);
//
//            player3.body.setContactCallbackFilter(2);
//            player3.body.setContactCallbackFilter(3);
//            player3.body.setContactCallbackFilter(4);
//
//            player4.body.setContactCallbackFilter(2);
//            player4.body.setContactCallbackFilter(3);
//            player4.body.setContactCallbackFilter(4);
//        }


        // Points
        if(app.assets.update() && scoreTimer > 0){

            // If player one falls,
            if((((btRigidBody) player1.body).getCenterOfMassPosition().y < 0) && (collisionUserId0 == 1 || collisionUserId1 == 1)
                    && (!(collisionUserId0 == 2) || !(collisionUserId1 == 2)) ){

                // Give the individual points to the player that knocked the opponent down.
                if(collisionUserId0 == 3 || collisionUserId1 == 3){
                    player_3.setScore(1);
                    Gdx.app.log("Team 2 scored", "Player 3 scored.");
                }
                else if(collisionUserId0 == 4 || collisionUserId1 == 4){
                    player_4.setScore(1);
                    Gdx.app.log("Team 2 scored", "Player 4 scored.");
                }

                teamScore2 = player_3.getScore() + player_4.getScore();

            }

            // If player two falls
            if((((btRigidBody) player2.body).getCenterOfMassPosition().y < 0) && (collisionUserId0 == 2 || collisionUserId1 == 2)
                    && (!(collisionUserId0 == 1) || !(collisionUserId1 == 1)) ){ // Not teammate

                // Give the individual points to the player that knocked the opponent down.
                if(collisionUserId0 == 3 || collisionUserId1 == 3){
                    player_3.setScore(1);
                    Gdx.app.log("Team 2 scored", "Player 3 scored.");
                }
                else if(collisionUserId0 == 4 || collisionUserId1 == 4){
                    player_4.setScore(1);
                    Gdx.app.log("Team 2 scored", "Player 4 scored.");
                }

                teamScore2 = player_3.getScore() + player_4.getScore();
            }

            // If player 3 falls
            if((((btRigidBody) player3.body).getCenterOfMassPosition().y < 0) && (collisionUserId0 == 3 || collisionUserId1 == 3)
                && (!(collisionUserId0 == 4) || !(collisionUserId1 == 4)) ){

                // Give the individual points to the player that knocked the opponent down.
                if(collisionUserId0 == 1 || collisionUserId1 == 1){
                    player_1.setScore(1);
                    Gdx.app.log("Team 1 scored", "Player 1 scored.");
                }
                else if(collisionUserId0 == 2 || collisionUserId1 == 2){
                    player_2.setScore(1);
                    Gdx.app.log("Team 1 scored", "Player 2 scored.");
                }

                teamScore1 = player_1.getScore() + player_2.getScore();
            }

            // If player 4 falls
            if((((btRigidBody) player4.body).getCenterOfMassPosition().y < 0) && (collisionUserId0 == 4 || collisionUserId1 == 4)
                    && (!(collisionUserId0 == 3) || !(collisionUserId1 == 3)) ){

                // Give the individual points to the player that knocked the opponent down.
                if(collisionUserId0 == 1 || collisionUserId1 == 1){
                    player_1.setScore(1);
                    Gdx.app.log("Team 1 scored", "Player 1 scored.");
                }
                else if(collisionUserId0 == 2 || collisionUserId1 == 2){
                    player_2.setScore(1);
                    Gdx.app.log("Team 1 scored", "Player 2 scored.");
                }

                teamScore1 = player_1.getScore() + player_2.getScore();

//                // DEBUG, reloads the game when player one falls.
//                if(!gameOverGameScreen){
//                    gameOverGameScreen = true;
//                }
            }

            if(gameOverGameScreen)
                startGameOverTimer();
        }

        // Set the score
        if(playerCreated) { //TODO: sätt teamscore istället.
            LabelScorePlayer1.setText("Score player 1: " + player_1.getScore());
            LabelScorePlayer2.setText("Score player 2: " + player_2.getScore());
            LabelScorePlayer3.setText("Score player 3: " + player_3.getScore());
            LabelScorePlayer4.setText("Score player 4: " + player_4.getScore());
        }

        stage.draw();
        scoreStage.draw();
    }

    @Override
    public void update () {
        float delta = Gdx.graphics.getRawDeltaTime();
        time += delta;
        super.update();
        if (contactCache != null) contactCache.update(delta);
    }

    @Override
    public void show() {


    }

    @Override
    public void render(float delta) {
        render();
    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose () {
        super.dispose();
        //stage.dispose();
        if (rayTestCB != null) {rayTestCB.dispose(); rayTestCB = null;}
        //scoreStage.dispose(); // Borde disposas men det blir hack till nästa screen
    }

    private void startGameOverTimer(){

        scoreStage.act();

        gameOverTimer += Gdx.graphics.getDeltaTime();

        if(gameOverTimer > 0.5)
        {
            super.setGameOver();
            scoreStage.getRoot().addAction(Actions.sequence(Actions.delay(1.2f), Actions.moveTo(0, 0, 0.5f), Actions.delay(1),
                    Actions.run(new Runnable() {
                        public void run() {
                            app.setScreen(new ScoreScreen(app));
                            dispose();
                        }
                    })));
        }
    }

    public int teamScore1 = 0, teamScore2 = 0;

    private void setTeams(){
        // Randomize lagen här, nu är det hårdkodat.

        // slumpa bollarna som skapas på nåt sätt.



//        teamScore1 = player_1.getScore() + player_2.getScore();
//        teamScore2 = player_3.getScore();

//        team1 = new ArrayList<BulletEntity>();
//        team2 = new ArrayList<BulletEntity>();
//        team1.add(player1);
//        team1.add(player2);
//        team2.add(player3);

//        boolean team1Identifier = team1.contains(player1);
//        boolean team2Identifier = team2.contains(player3);
    }

    private void giveScores(){


    }
}