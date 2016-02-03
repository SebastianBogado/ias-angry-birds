package ar.fi.uba.celdas.ias;

import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.Vision;

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
            selectedTheory = new Theory();
            selectedTheory.preconditions.add(new CountTheoryCondition(1, ABType.Pig));
            selectedTheory.action = new HitAction();
            selectedTheory.postconditions.add(new CountTheoryCondition(0, ABType.Pig));
            selectedTheory.useCount++;
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
}
