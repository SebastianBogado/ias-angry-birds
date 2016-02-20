package ar.fi.uba.celdas.ias;

import ab.vision.ABType;
import ab.vision.Vision;
import ar.fi.uba.celdas.utils.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * Created by seba on 2/2/16.
 */
public class IntelligentAutonomousSystem {

    List<Theory> theories;
    List<Theory> worthlessTheories;
    Theory localTheory;
    int lastScore;

    public IntelligentAutonomousSystem() {
        theories = new ArrayList<>();
        worthlessTheories = new ArrayList<>();
        localTheory = null;
        lastScore = 0;
    }

    public Point getTarget(Vision vision) {
        localTheory = findBestTheory(vision);
        return localTheory.action.getTarget(vision);
    }

    public void confirmLocalTheory(Vision vision, int score) {
        if (localTheory == null) {
            System.out.println("[IAS] No theory to confirm.");
            return;
        }

        confirmTheory(localTheory, true, vision, score);

        localTheory = null;
    }

    private void confirmTheory(Theory theory, Boolean mutate, Vision vision, int score) {
        int localTheoryScore;
        if (score < lastScore) {
            localTheoryScore = score;
            lastScore = 0;
        } else {
            localTheoryScore = score - lastScore;
            lastScore = score;
        }

        if (localTheoryScore == 0) {
            System.out.println("[IAS] Local theory did nothing (0 score). It's not worth it.");
            worthlessTheories.add(localTheory);
            localTheory.useCount = 1;
            return;
        }

        theory.postconditions.addAll(describeWorld(vision));

        List<Theory> equalTheories = getEqualTheories(theory);
        List<Theory> similarTheories = getSimilarTheories(theory);
        List<Theory> mutantTheories;

        if (!equalTheories.isEmpty()) {
            System.out.format("[IAS] Found %d equal theories\n", equalTheories.size());

            for (Theory equalTheory : equalTheories) {
                equalTheory.successCount += 1;
                equalTheory.useCount += 1;
                equalTheory.acummulatedScore += localTheoryScore;
            }

            for (Theory similarTheory : similarTheories) {
                similarTheory.useCount += 1;
                similarTheory.acummulatedScore += localTheoryScore;
            }

        } else if (!similarTheories.isEmpty()) {
            System.out.format("[IAS] Found %d similar theories\n", similarTheories.size());

            theories.add(localTheory);
            localTheory.successCount = 1;
            localTheory.useCount = similarTheories.get(0).useCount + 1;
            localTheory.acummulatedScore += localTheoryScore;

            for (Theory similarTheory : similarTheories) {
                similarTheory.useCount += 1;
            }

            if (mutate) {
                mutantTheories = generateMutantTheories(localTheory);

                for (Theory mutantTheory : mutantTheories) {
                    confirmTheory(mutantTheory, false, vision, localTheoryScore);
                }
            }

        } else {
            System.out.println("[IAS] Found nothing similar to this theory. Adding it to the list");
            theories.add(localTheory);
            localTheory.successCount = 1;
            localTheory.useCount = 1;
            localTheory.acummulatedScore += localTheoryScore;
        }
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

    /**
     * This method finds the best theory or builds one if there are not matches.
     * Best theory is the one with that satisfies all preconditions and has the higher
     * successRatio, i.e., successCount / useCount
     * @param vision
     * @return
     */
    private Theory findBestTheory(Vision vision) {
        Theory localTheory = buildLocalTheory(vision);

        return theories.stream()
                .filter(theory -> theory.satisfiesPreconditions(vision))
                .sorted(comparing(Theory::successRatio))
                .findFirst()
                .orElse(localTheory);
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


    private List<TheoryCondition> describeWorld(Vision vision) {
        List<TheoryCondition> conditions = new ArrayList<>();

        for (ABType type : Utils.getAvailableTypes(vision)) {
            TheoryCondition condition = new CountTheoryCondition()
                    .exactly(Utils.getTotalABObjects(vision, type))
                    .ofType(type);

            conditions.add(condition);
        }

        return conditions;
    }
}
