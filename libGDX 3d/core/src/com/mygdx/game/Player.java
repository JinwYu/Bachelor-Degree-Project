package com.mygdx.game;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;

import javax.swing.plaf.synth.SynthEditorPaneUI;

/**
 * Created by sofiekhullar on 16-04-08.
 */
public class Player extends BaseBulletTest implements Comparable<Player> {
    private BulletEntity bulletEntity;
    public BulletConstructor bulletConstructor;
    private Vector3 position;
    private Model model;
    private int score = 0;
    public int impulseFactor = 0;
    public int weight = 0;
    private String name;

    // Variables
//    private int weightFootball = 3;
//    private int weightApple = 1;
//    private int weightPeach = 20;
    private int weightFootball = 2;
    private int weightApple = 2;
    private int weightPeach = 2;

//    private int impulsefootball = 3;
//    private int impulseApple = 2;
//    private int impulsePeach = 1;
    private int impulseFootball = 6;
    private int impulseApple = 6;
    private int impulsePeach = 6;

    public Player(Model model, String name){
        this.model = model;
        this.name = name;

        weight = setweight(name);
        impulseFactor = setImpulseFactor(name);

        bulletConstructor = initBulletConstructor(model, weight);
        position = new Vector3(0, 0.5f, 0.5f);
        score = 0;
    }

    public BulletConstructor initBulletConstructor(Model model, float weight) {
        disposables.add(model);
        BulletConstructor bulletConstructor = (new BulletConstructor(model, weight, new btSphereShape(0.8f)));
        return bulletConstructor;
    }


    // Set the weith depending on the model
    public int setweight(String name){
        if(name == "football"){
            return weightFootball;
        }
        if(name == "apple"){
            return weightApple;
        }
        if(name == "peach"){
            return weightPeach;
        }
        else return 0;
    }

    // Set the impulse strength depending on the model
    public int setImpulseFactor(String name){
        if(name == "football"){
            return impulseFootball;
        }
        if(name == "apple"){
            return impulseApple;
        }
        if(name == "peach"){
            return impulsePeach;
        }
        else return 0;
    }

    public void setScore(int points){
         score += points;
    }

    public int getScore(){
        return score;
    }

    public String getModelName(){return name;}

    // Compare scores in descending order.
    @Override
    public int compareTo(Player comparesTo){
        int compareScore = ((Player)comparesTo).getScore();
        return compareScore - this.score;
    }
}
