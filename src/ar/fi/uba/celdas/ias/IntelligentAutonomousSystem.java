package ar.fi.uba.celdas.ias;

import ab.vision.ABType;
import ab.vision.Vision;
import ar.fi.uba.celdas.utils.IASMarshaller;
import ar.fi.uba.celdas.utils.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * Created by seba on 2/2/16.
 */
public class IntelligentAutonomousSystem {

    public List<Theory> theories;
    public List<Theory> worthlessTheories;
    private Theory localTheory;
    private int lastScore;
    private IASMarshaller iasMarshaller;

    public IntelligentAutonomousSystem() {
        theories = new ArrayList<>();
        worthlessTheories = new ArrayList<>();
        localTheory = null;
        lastScore = 0;
    }

    public IntelligentAutonomousSystem(String filename) {
        this();
        iasMarshaller = new IASMarshaller(filename);
        IntelligentAutonomousSystem persistedIAS = iasMarshaller.getIAS();

        if (persistedIAS == null) System.out.println("LOL");

        theories.addAll(persistedIAS.theories);
        worthlessTheories.addAll(persistedIAS.worthlessTheories);
    }

    public Point getTarget(Vision vision) {
        localTheory = findBestTheory(vision);
        System.out.println(localTheory);
        return localTheory.action.getTarget(vision);
    }

    public void confirmLocalTheory(Vision vision, int score) {
        if (localTheory == null) {
            System.out.println("[IAS] No theory to confirm.");
            return;
        }

        confirmTheory(localTheory, true, vision, score);

        iasMarshaller.save(this);

        localTheory = null;
    }

    private void confirmTheory(Theory theory, Boolean mutate, Vision vision, int score) {
        int theoryScore;
        if (score < lastScore) {
            theoryScore = score;
            lastScore = 0;
        } else {
            theoryScore = score - lastScore;
            lastScore = score;
        }

        if (theoryScore == 0) {
            System.out.println("[IAS] Local theory did nothing (0 score). It's not worth it.");
            worthlessTheories.add(theory);
            theory.useCount = 1;
            return;
        }

        System.out.format("[IAS] Local theory did %d score.", theoryScore);

        theory.postconditions.addAll(describeWorld(vision));

        List<Theory> equalTheories = getEqualTheories(theory);
        List<Theory> similarTheories = getSimilarTheories(theory);
        List<Theory> mutantTheories;

        if (!equalTheories.isEmpty()) {
            System.out.format("[IAS] Found %d equal theories\n", equalTheories.size());

            for (Theory equalTheory : equalTheories) {
                equalTheory.successCount += 1;
                equalTheory.useCount += 1;
                equalTheory.accumulatedScore += theoryScore;
            }

            for (Theory similarTheory : similarTheories) {
                similarTheory.useCount += 1;
                similarTheory.accumulatedScore += theoryScore;
            }

        } else if (!similarTheories.isEmpty()) {
            System.out.format("[IAS] Found %d similar theories\n", similarTheories.size());

            theories.add(theory);
            theory.successCount = 1;
            theory.useCount = similarTheories.get(0).useCount + 1;
            theory.accumulatedScore += theoryScore;

            for (Theory similarTheory : similarTheories) {
                similarTheory.useCount += 1;
            }

            if (mutate) {
                mutantTheories = generateMutantTheories(theory);

                for (Theory mutantTheory : mutantTheories) {
                    confirmTheory(mutantTheory, false, vision, theoryScore);
                }
            }

        } else {
            System.out.println("[IAS] Found nothing similar to this theory. Adding it to the list");
            theories.add(theory);
            theory.successCount = 1;
            theory.useCount = 1;
            theory.accumulatedScore += theoryScore;
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
     * This method finds the best theory and returns a clone or builds one if there are no matches.
     * Best theory is the one with that satisfies all preconditions and has the higher
     * successRatio, i.e., successCount / useCount
     * @param vision
     * @return
     */
    private Theory findBestTheory(Vision vision) {
        try {
            return theories.stream()
                    .filter(theory -> theory.satisfiesPreconditions(vision))
//                    .sorted(comparing(Theory::successRatio))
                    .sorted(comparing(Theory::successRatioWithScore))
                    .findFirst()
                    .get()
                    .clonePreconditionsAndAction();
        } catch (NoSuchElementException exception) {
            return buildLocalTheory(vision);
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
