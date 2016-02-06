package ar.fi.uba.celdas.ias;

import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.Vision;
import ar.fi.uba.celdas.utils.Utils;

import java.awt.*;
import java.util.List;

/**
 * Created by seba on 2/2/16.
 */
public class IntelligentAutonomousSystem {

    List<Theory> theories;
    Theory lastTheory;

    public Point getTarget(Vision vision) {
        Theory selectedTheory;
//        if (theories.isEmpty()) {
            selectedTheory = buildTheory(vision);
//        }

        if (selectedTheory.satisfiesPreconditions(vision)) {
            System.out.println("Theory zero satisfies");
        }

        lastTheory = selectedTheory;
        return selectedTheory.action.getTarget(vision);
    }

    public void confirmLastTheory(Vision vision) {

        if (lastTheory != null) {
            if (lastTheory.satisfiesPostconditions(vision)) {
                System.out.println("Theory zero successful");
                lastTheory.successCount++;
            } else {
                System.out.println("Theory zero was not successful");
            }
            lastTheory = null;
        }

    }

    /**
     * This method builds a theory that will hit a random type and expects to kill it.
     * It should work always, and if it doesn't then we're screwed. There's probably something
     * in the way and we're not considering high trajectories, only direct hits.
     * @param vision
     * @return
     */
    private Theory buildTheory(Vision vision) {
        ABType typeToHit = Utils.getRandomAvailableType(vision);
        int totalABOjectsOfTypeToHit = Utils.getTotalABObjects(vision, typeToHit);

        System.out.println("Theory Zero: going to hit " + typeToHit.name() + ", of which there are " + totalABOjectsOfTypeToHit + " total");

        Theory newTheory = new Theory();

        TheoryCondition atLeastOneABObjectCondition = new CountTheoryCondition()
                .atLeast(totalABOjectsOfTypeToHit)
                .ofType(typeToHit);

        Action action = new HitAction()
                .furtherToTheLeft()
                .ofType(typeToHit);

        TheoryCondition atLeastOneABObjecLessCondition = new CountTheoryCondition()
                .noMoreThan(totalABOjectsOfTypeToHit - 1)
                .ofType(typeToHit);


        newTheory.preconditions.add(atLeastOneABObjectCondition);
        newTheory.action = action;
        newTheory.postconditions.add(atLeastOneABObjecLessCondition);
        newTheory.useCount++;

        return newTheory;
    }
}
