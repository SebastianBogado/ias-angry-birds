package ar.fi.uba.celdas.ias;

import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.Vision;
import ar.fi.uba.celdas.utils.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * Created by seba on 2/2/16.
 */
public class IntelligentAutonomousSystem {

    List<Theory> theories;
    Theory lastTheory;

    public IntelligentAutonomousSystem() {
        theories = new ArrayList<>();
        lastTheory = null;
    }

    public Point getTarget(Vision vision) {
        Theory localTheory = buildLocalTheory(vision);
        lastTheory = localTheory;
        return localTheory.action.getTarget(vision);
    }

    public void confirmLastTheory(Vision vision, int score) {
        if (lastTheory == null) {
            System.out.println("ERROR: there's no theory to confirm. TODO: define what to do here");
            return;
        }

        confirmTheory(lastTheory, true, vision, score);

        lastTheory = null;
    }

    private void confirmTheory(Theory theory, Boolean mutate, Vision vision, int score) {
        theory.postconditions.addAll(describeWorld(vision));

        List<Theory> equalTheories = getEqualTheories(theory);
        List<Theory> similarTheories = getSimilarTheories(theory);
        List<Theory> mutantTheories;

        if (!equalTheories.isEmpty()) {

            for (Theory equalTheory : equalTheories) {
                equalTheory.successCount += 1;
                equalTheory.useCount += 1;
                equalTheory.acummulatedScore += score;
            }

            for (Theory similarTheory : similarTheories) {
                similarTheory.useCount += 1;
                similarTheory.acummulatedScore += score;
            }

        } else if (!similarTheories.isEmpty()) {
            theories.add(lastTheory);
            lastTheory.successCount = 1;
            lastTheory.useCount = similarTheories.get(0).useCount + 1;
            lastTheory.acummulatedScore += score;

            for (Theory similarTheory : similarTheories) {
                similarTheory.useCount += 1;
            }

            if (mutate) {
                mutantTheories = generateMutantTheories(lastTheory);

                for (Theory mutantTheory : mutantTheories) {
                    confirmTheory(mutantTheory, false, vision, score);
                }
            }

        } else {
            System.out.println("ERROR: there are no similar theories. TODO: define what to do here");
        }
    }


    private Theory buildLocalTheory(Vision vision) {
        Theory localTheory;

        ABType typeToHit = Utils.getRandomAvailableType(vision);
        localTheory = new Theory();

        Action action = new HitAction()
                .furtherToTheLeft()
                .ofType(typeToHit);

        localTheory.preconditions.addAll(describeWorld(vision));
        localTheory.action = action;

        return localTheory;
    }

    private List<Theory> getEqualTheories(Theory localTheory) {
        return theories.stream()
                .filter(theory -> theory.equals(localTheory))
                .collect(toList());
    }

    private List<Theory> getSimilarTheories(Theory localTheory) {
        return theories.stream()
                .filter(theory -> theory.similar(localTheory))
                .collect(toList());
    }

    private List<Theory> generateMutantTheories(Theory localTheory) {
        List<Theory> mutantTheories = new ArrayList<>();
        return mutantTheories;
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

    private Theory findBestTheory(Vision vision) {

        Optional<Theory> theoryOptional = theories.stream()
                .filter(theory -> theory.satisfiesPreconditions(vision))
                .sorted(comparing(Theory::successRatio))
                .findFirst();

        return theoryOptional.isPresent() ? theoryOptional.get() : null;
    }


    private List<TheoryCondition> describeWorld(Vision vision) {
        List<TheoryCondition> conditions = new ArrayList<TheoryCondition>();

        for (ABType type : Utils.getAvailableTypes(vision)) {
            TheoryCondition condition = new CountTheoryCondition()
                    .exactly(Utils.getTotalABObjects(vision, type))
                    .ofType(type);

            conditions.add(condition);
        }

        return conditions;
    }
}
