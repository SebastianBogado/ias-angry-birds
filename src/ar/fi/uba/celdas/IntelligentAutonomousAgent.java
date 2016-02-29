package ar.fi.uba.celdas;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ab.demo.other.ActionRobot;
import ab.demo.other.Shot;
import ab.planner.TrajectoryPlanner;
import ab.utils.StateUtil;
import ab.vision.ABObject;
import ab.vision.GameStateExtractor;
import ab.vision.Vision;
import ar.fi.uba.celdas.ias.IntelligentAutonomousSystem;

public class IntelligentAutonomousAgent implements Runnable {

    private final ActionRobot aRobot = new ActionRobot();
    private final Random randomGenerator = new Random();
    private Map<Integer,Integer> scoreByLevel = new LinkedHashMap<Integer,Integer>();
    private int currentLevel = 1;
    private TrajectoryPlanner tp;
    private IntelligentAutonomousSystem ias;

    public static String THEORIES_FILE = "theories.json";

    // a standalone implementation of the Intelligent Autonomous Agent
    public IntelligentAutonomousAgent() {

        tp = new TrajectoryPlanner();
        ias = new IntelligentAutonomousSystem(THEORIES_FILE);

        // --- go to the Poached Eggs episode level selection page ---
        ActionRobot.GoFromMainMenuToLevelSelection();
    }

    @Override
    // run the client
    public void run() {

        aRobot.loadLevel(currentLevel);
        while (true) {
            switch (solve()){
                case WON:
                    int totalScore = processWin();
                    //loadLevel("Total Score: " + totalScore, ++currentLevel);
                    // Keep iterating over same level to improve learning
                    loadLevel("Total Score: " + totalScore, currentLevel);
                    break;
                case LOST:
                    restartLevel();
                    break;
                case MAIN_MENU:
                    reloadFromMenu("Unexpected main menu page, go to the last current level : " + currentLevel, currentLevel);
                    break;
                case EPISODE_MENU:
                    reloadFromMenu("Unexpected episode menu page, go to the last current level : "+ currentLevel, currentLevel);
                    break;
                case LEVEL_SELECTION:
                    loadLevel("Unexpected level selection page, go to the last current level : " + currentLevel, currentLevel);
                    break;
            }
        }
    }

    private void reloadFromMenu(String loadMessage, int currentLevel) {
        System.out.println(loadMessage);
        ActionRobot.GoFromMainMenuToLevelSelection();
        aRobot.loadLevel(currentLevel);
    }

    private void loadLevel(String loadMessage, int level) {
        System.out.println(loadMessage);
        aRobot.loadLevel(level);
    }

    private void restartLevel() {
        System.out.println("Restart");
        aRobot.restartLevel();
    }

    private int processWin() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int score = StateUtil.getScore(ActionRobot.proxy);
        if(!scoreByLevel.containsKey(currentLevel)) {
            scoreByLevel.put(currentLevel, score);
        } else {
            if(scoreByLevel.get(currentLevel) < score){
                scoreByLevel.put(currentLevel, score);
            }
        }

        int totalScore = 0;
        for(Integer key: scoreByLevel.keySet()){
            totalScore += scoreByLevel.get(key);
            System.out.println(" Level " + key + " Score: " + scoreByLevel.get(key) + " ");
        }

        // make a new trajectory planner whenever a new level is entered
        tp = new TrajectoryPlanner();

        return totalScore;
    }

    public GameStateExtractor.GameState solve() {
        // capture Image
        BufferedImage screenshot = ActionRobot.doScreenShot();

        // process image
        Vision vision = new Vision(screenshot);

        // find the slingshot
        Rectangle sling = vision.findSlingshotMBR();

        // confirm the slingshot
        while (sling == null && aRobot.getState() == GameStateExtractor.GameState.PLAYING) {
            System.out.println("No slingshot detected. Please remove pop up or zoom out");
            ActionRobot.fullyZoomOut();
            screenshot = ActionRobot.doScreenShot();
            vision = new Vision(screenshot);
            sling = vision.findSlingshotMBR();
        }

        // get all the pigs
        List<ABObject> pigs = vision.findPigsMBR();

        GameStateExtractor.GameState state = aRobot.getState();

        // if there is a sling, then play, otherwise just skip.
        if (sling != null) {
            ias.confirmLocalTheory(vision, new GameStateExtractor().getScoreInGame(screenshot));

            if (!pigs.isEmpty()) {
                Point releasePoint;
                Shot shot;
                int dx,dy;
                {
                    // let the IAS decide where to shoot
                    Point target = ias.getTarget(vision);

                    // estimate the trajectory
                    ArrayList<Point> pts = tp.estimateLaunchPoint(sling, target);

                    if (pts.isEmpty()) {
                        System.out.println("No release point found for the target");
                        System.out.println("Try a shot with 45 degree");
                        releasePoint = tp.findReleasePoint(sling, Math.PI / 4);
                    } else {
                        // never choose high shots
                        releasePoint = pts.get(0);
                    }

                    // Get the reference point
                    Point refPoint = tp.getReferencePoint(sling);

                    //Calculate the tapping time according the bird type
                    if (releasePoint != null) {
                        double releaseAngle = tp.getReleaseAngle(sling, releasePoint);
                        System.out.println("Release Point: " + releasePoint);
                        System.out.println("Release Angle: " + Math.toDegrees(releaseAngle));
                        int tapInterval;
                        switch (aRobot.getBirdTypeOnSling()){
                            case RedBird:
                                tapInterval = 0; break;               // start of trajectory
                            case YellowBird:
                                tapInterval = 65 + randomGenerator.nextInt(25);break; // 65-90% of the way
                            case WhiteBird:
                                tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
                            case BlackBird:
                                tapInterval =  70 + randomGenerator.nextInt(20);break; // 70-90% of the way
                            case BlueBird:
                                tapInterval =  65 + randomGenerator.nextInt(20);break; // 65-85% of the way
                            default:
                                tapInterval =  60;
                        }

                        int tapTime = tp.getTapTime(sling, releasePoint, target, tapInterval);
                        dx = (int)releasePoint.getX() - refPoint.x;
                        dy = (int)releasePoint.getY() - refPoint.y;
                        shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);

                    } else {
                        System.err.println("No Release Point Found");
                        return state;
                    }
                }

                // check whether the slingshot is changed. the change of the slingshot indicates a change in the scale.
                {
                    ActionRobot.fullyZoomOut();
                    screenshot = ActionRobot.doScreenShot();
                    vision = new Vision(screenshot);
                    Rectangle _sling = vision.findSlingshotMBR();
                    if(_sling != null) {
                        double scale_diff = Math.pow((sling.width - _sling.width),2) +  Math.pow((sling.height - _sling.height),2);
                        if(scale_diff < 25) {
                            if(dx < 0){
                                aRobot.cshoot(shot);
                                state = aRobot.getState();
                                if ( state == GameStateExtractor.GameState.PLAYING ){
                                    screenshot = ActionRobot.doScreenShot();
                                    vision = new Vision(screenshot);
                                    java.util.List<Point> traj = vision.findTrajPoints();
                                    tp.adjustTrajectory(traj, sling, releasePoint);
                                }
                            }
                        } else {
                            System.out.println("Scale is changed, can not execute the shot, will re-segement the image");
                        }
                    } else {
                        System.out.println("no sling detected, can not execute the shot, will re-segement the image");
                    }
                }

            }

        }
        return state;
    }

    public static void main(String args[]) {

        IntelligentAutonomousAgent iaa = new IntelligentAutonomousAgent();
        if (args.length > 0){
            iaa.currentLevel = Integer.parseInt(args[0]);
        }
        iaa.run();

    }
}
