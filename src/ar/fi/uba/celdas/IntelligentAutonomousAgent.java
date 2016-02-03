package ar.fi.uba.celdas;

import ab.demo.other.ActionRobot;
import ab.demo.other.Shot;
import ab.planner.TrajectoryPlanner;
import ab.utils.StateUtil;
import ab.vision.ABObject;
import ab.vision.GameStateExtractor;
import ab.vision.Vision;

import java.awt.*;
import java.awt.List;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Created by seba on 2/2/16.
 */
public class IntelligentAutonomousAgent implements Runnable {

    private ActionRobot aRobot;
    private Random randomGenerator;
    public int currentLevel = 1;
    public static int time_limit = 12;
    private Map<Integer,Integer> scores = new LinkedHashMap<Integer,Integer>();
    TrajectoryPlanner tp;
    private boolean firstShot;
    private Point prevTarget;
    // a standalone implementation of the Naive Agent
    public IntelligentAutonomousAgent() {

        aRobot = new ActionRobot();
        tp = new TrajectoryPlanner();
        prevTarget = null;
        firstShot = true;
        randomGenerator = new Random();
        // --- go to the Poached Eggs episode level selection page ---
        ActionRobot.GoFromMainMenuToLevelSelection();

    }

    @Override
    // run the client
    public void run() {

        aRobot.loadLevel(currentLevel);
        while (true) {
            GameStateExtractor.GameState state = solve();
            if (state == GameStateExtractor.GameState.WON) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int score = StateUtil.getScore(ActionRobot.proxy);
                if(!scores.containsKey(currentLevel))
                    scores.put(currentLevel, score);
                else
                {
                    if(scores.get(currentLevel) < score)
                        scores.put(currentLevel, score);
                }
                int totalScore = 0;
                for(Integer key: scores.keySet()){

                    totalScore += scores.get(key);
                    System.out.println(" Level " + key
                            + " Score: " + scores.get(key) + " ");
                }
                System.out.println("Total Score: " + totalScore);
                aRobot.loadLevel(++currentLevel);
                // make a new trajectory planner whenever a new level is entered
                tp = new TrajectoryPlanner();

                // first shot on this level, try high shot first
                firstShot = true;
            } else if (state == GameStateExtractor.GameState.LOST) {
                System.out.println("Restart");
                aRobot.restartLevel();
            } else if (state == GameStateExtractor.GameState.LEVEL_SELECTION) {
                System.out
                        .println("Unexpected level selection page, go to the last current level : "
                                + currentLevel);
                aRobot.loadLevel(currentLevel);
            } else if (state == GameStateExtractor.GameState.MAIN_MENU) {
                System.out
                        .println("Unexpected main menu page, go to the last current level : "
                                + currentLevel);
                ActionRobot.GoFromMainMenuToLevelSelection();
                aRobot.loadLevel(currentLevel);
            } else if (state == GameStateExtractor.GameState.EPISODE_MENU) {
                System.out
                        .println("Unexpected episode menu page, go to the last current level : "
                                + currentLevel);
                ActionRobot.GoFromMainMenuToLevelSelection();
                aRobot.loadLevel(currentLevel);
            }

        }

    }

    public GameStateExtractor.GameState solve()
    {

        // capture Image
        BufferedImage screenshot = ActionRobot.doScreenShot();

        // process image
        Vision vision = new Vision(screenshot);

        // find the slingshot
        Rectangle sling = vision.findSlingshotMBR();

        // confirm the slingshot
        while (sling == null && aRobot.getState() == GameStateExtractor.GameState.PLAYING) {
            System.out
                    .println("No slingshot detected. Please remove pop up or zoom out");
            ActionRobot.fullyZoomOut();
            screenshot = ActionRobot.doScreenShot();
            vision = new Vision(screenshot);
            sling = vision.findSlingshotMBR();
        }
        // get all the pigs
        java.util.List<ABObject> pigs = vision.findPigsMBR();

        GameStateExtractor.GameState state = aRobot.getState();

        // if there is a sling, then play, otherwise just skip.
        if (sling != null) {

            if (!pigs.isEmpty()) {

                Point releasePoint = null;
                Shot shot = new Shot();
                int dx,dy;
                {
                    // pick up first pig
                    Point pigCenter = pigs.get(0).getCenter();

                    // estimate the trajectory
                    ArrayList<Point> pts = tp.estimateLaunchPoint(sling, pigCenter);

                    releasePoint = pts.get(0);

                    // Get the reference point
                    Point refPoint = tp.getReferencePoint(sling);

                    double releaseAngle = tp.getReleaseAngle(sling, releasePoint);
                    System.out.println("Release Point: " + releasePoint);
                    System.out.println("Release Angle: "
                            + Math.toDegrees(releaseAngle));

                    dx = (int)releasePoint.getX() - refPoint.x;
                    dy = (int)releasePoint.getY() - refPoint.y;
                    shot = new Shot(refPoint.x, refPoint.y, dx, dy);
                }

                // check whether the slingshot is changed. the change of the slingshot indicates a change in the scale.
                {
                    ActionRobot.fullyZoomOut();
                    screenshot = ActionRobot.doScreenShot();
                    vision = new Vision(screenshot);
                    Rectangle _sling = vision.findSlingshotMBR();
                    if(_sling != null)
                    {
                        double scale_diff = Math.pow((sling.width - _sling.width),2) +  Math.pow((sling.height - _sling.height),2);
                        if(scale_diff < 25)
                        {
                            if(dx < 0)
                            {
                                aRobot.cshoot(shot);
                                state = aRobot.getState();
                                if ( state == GameStateExtractor.GameState.PLAYING )
                                {
                                    screenshot = ActionRobot.doScreenShot();
                                    vision = new Vision(screenshot);
                                    java.util.List<Point> traj = vision.findTrajPoints();
                                    tp.adjustTrajectory(traj, sling, releasePoint);
                                    firstShot = false;
                                }
                            }
                        }
                        else
                            System.out.println("Scale is changed, can not execute the shot, will re-segement the image");
                    }
                    else
                        System.out.println("no sling detected, can not execute the shot, will re-segement the image");
                }

            }

        }
        return state;
    }

    public static void main(String args[]) {

        IntelligentAutonomousAgent iaa = new IntelligentAutonomousAgent();
        if (args.length > 0)
            iaa.currentLevel = Integer.parseInt(args[0]);
        iaa.run();

    }
}
