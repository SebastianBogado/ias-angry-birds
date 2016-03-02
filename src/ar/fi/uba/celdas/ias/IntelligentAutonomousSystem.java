package ar.fi.uba.celdas.ias;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import ab.vision.ABType;
import ab.vision.Vision;
import ar.fi.uba.celdas.utils.IASMarshaller;
import ar.fi.uba.celdas.utils.Utils;

public class IntelligentAutonomousSystem {

    public List<Theory> generalTheories;
    public List<Theory> theories;
    public List<Theory> worthlessTheories;
    private Theory localTheory;
    private int lastScore;
    private IASMarshaller iasMarshaller;

    public IntelligentAutonomousSystem() {
        generalTheories = new ArrayList<>();
        theories = new ArrayList<>();
        worthlessTheories = new ArrayList<>();
        localTheory = null;
        lastScore = 0;
    }

    public IntelligentAutonomousSystem(String filename) {
        this();
        iasMarshaller = new IASMarshaller(filename);
        IntelligentAutonomousSystem persistedIAS = iasMarshaller.getIAS();

        theories.addAll(persistedIAS.theories);
        worthlessTheories.addAll(persistedIAS.worthlessTheories);
    }

    public Point getTarget(Vision vision, int currentLevel) {
        localTheory = findBestTheory(vision);
        localTheory.level = currentLevel;
        System.out.println(localTheory);
        return localTheory.action.getTarget(vision);
    }

    public void confirmLocalTheory(Vision vision, int score) {
        if (localTheory == null) {
            System.out.println("[IAS] No theory to confirm.");
            return;
        }


        localTheory.postconditions.addAll(describeWorld(vision));
        confirmTheory(localTheory, false, vision, score);

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

        System.out.format("[IAS] Local theory did %d score.\n", theoryScore);

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
                    System.out.println("Mutation: ");
                    System.out.println(mutantTheory);
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
        // Hackity hack
        List<TheoryCondition> worldState = new ArrayList<>();

        if (localTheory.isCloned()) worldState.addAll(localTheory.getClonedFrom().postconditions);

        // Retraction heuristic
        Theory retractionTheory = new Theory();
        retractionTheory.preconditions.addAll(localTheory.preconditions);
        retractionTheory.action = localTheory.action;
        retractionTheory.postconditions.addAll(localTheory.postconditions.stream()
                        .filter(postcondition ->
                                        worldState.stream().anyMatch(worldStatePostconditon ->
                                                postcondition.equals(worldStatePostconditon) || postcondition.isMoreSpecific(worldStatePostconditon))
                        )
                        .collect(toList())
        );

        mutantTheories.add(retractionTheory);

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
            return generalTheories.stream()
                    .filter(theory -> theory.satisfiesPreconditions(vision))
                    .sorted(comparing(Theory::successRatioWithScore))
                    .findFirst()
                    .get()
                    .clonePreconditionsAndAction();
        } catch (NoSuchElementException exception) {
            try {
                return theories.stream()
                        .filter(theory -> theory.satisfiesPreconditions(vision))
                        .sorted(comparing(Theory::successRatioWithScore))
                        .findFirst()
                        .get()
                        .clonePreconditionsAndAction();
            } catch (NoSuchElementException exception2) {
                return buildLocalTheory(vision);
            }
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

    public void mutateBestTheories(int currentLevel) {
        mutateBestTheories(currentLevel, currentLevel - 1);
    }

    private void mutateBestTheories(int currentLevel, int otherLevel) {
        if (currentLevel < 2 || otherLevel < 1) {
            System.out.println("Level " + otherLevel + " doesn't exist. Cannot mutate.");
            return;
        }

        Theory bestTheoryFromOtherLevel;
        Theory bestTheoryFromCurrentLevel;

        try {
            bestTheoryFromOtherLevel = theories.stream()
                    .filter(theory -> theory.level == otherLevel)
                    .sorted(comparing(Theory::successRatioWithScore))
                    .findFirst()
                    .get();

        } catch (NoSuchElementException exception) {
            System.out.println("No good enough theories found for current level: " + currentLevel + ". Cannot mutate.");
            return;
        }

        try {
            bestTheoryFromCurrentLevel = theories.stream()
                    .filter(theory -> theory.level == currentLevel)
                    .filter(theory -> theory.action.equals(bestTheoryFromOtherLevel.action) )
                    .sorted(comparing(Theory::successRatioWithScore))
                    .findFirst()
                    .get();

        } catch (NoSuchElementException exception) {
            System.out.println("No theories found for current level (" + currentLevel + ") with same action than " +
                    "the best from the previous level. Will try with two levels before");
            mutateBestTheories(currentLevel, otherLevel - 1);
            return;
        }

        Theory mutatedTheory = generalizeTheories(bestTheoryFromOtherLevel, bestTheoryFromCurrentLevel);

        if (mutatedTheory != null) {
            System.out.println("Mutated theory: ");
            System.out.println(mutatedTheory);
            generalTheories.add(mutatedTheory);
            iasMarshaller.save(this);
        } else {
            System.out.println("Couldn't mutate theories from levels " + currentLevel + " and " + otherLevel);
        }
    }

    private Theory generalizeTheories(Theory theory1, Theory theory2) {
        Theory mutatedTheory = new Theory();

        System.out.println("About to mutate theories: " + theory1 + " and " + theory2);

        // If the theories doesn't have the same action, then we cannot mutate
        if (!theory1.action.equals(theory2.action)) {
            System.out.println("Best theories from current and previous level have different actions. Cannot mutate.");
            return null;
        }

        mutatedTheory.preconditions = new ArrayList<>();
        mutatedTheory.action = theory2.action;
        mutatedTheory.postconditions = new ArrayList<>();

        // For every condition that has the same count of type, create a new condition that requires at least
        // the lesser count of that type
        for (TheoryCondition theoryCondition : theory2.preconditions) {
            CountTheoryCondition countConditionTheory2 = (CountTheoryCondition) theoryCondition;
            CountTheoryCondition countConditionTheory1 = null;
            int i = 0;

            while (countConditionTheory1 == null && i < theory1.preconditions.size()) {
                countConditionTheory1 = (CountTheoryCondition) theory1.preconditions.get(i++);

                if (countConditionTheory2.type != countConditionTheory1.type) {
                    countConditionTheory1 = null;
                }
            }

            if (countConditionTheory1 != null) {
                CountTheoryCondition condition = new CountTheoryCondition()
                        .atLeast(Math.min(countConditionTheory1.count, countConditionTheory2.count))
                        .ofType(countConditionTheory2.type);

                mutatedTheory.preconditions.add(condition);
            }
        }

        // For every condition that has the same count of type, create a new condition that requires at least
        // the lesser count of that type
        for (TheoryCondition theoryCondition : theory2.postconditions) {
            CountTheoryCondition countConditionTheory2 = (CountTheoryCondition) theoryCondition;
            CountTheoryCondition countConditionTheory1 = null;
            int i = 0;

            while (countConditionTheory1 == null && i < theory1.postconditions.size()) {
                countConditionTheory1 = (CountTheoryCondition) theory1.postconditions.get(i++);

                if (countConditionTheory2.type != countConditionTheory1.type) {
                    countConditionTheory1 = null;
                }
            }

            if (countConditionTheory1 != null) {
                CountTheoryCondition condition = new CountTheoryCondition()
                        .atLeast(Math.min(countConditionTheory1.count, countConditionTheory2.count))
                        .ofType(countConditionTheory2.type);

                mutatedTheory.postconditions.add(condition);
            }
        }

        // If the theories doesn't have preconditions or postconditions in common, then we cannot mutate
        if (mutatedTheory.preconditions.size() == 0 || mutatedTheory.postconditions.size() == 0 ) {
            System.out.println("Best theories from current and previous level have no matching conditions. Cannot mutate.");
            return null;
        }

        return mutatedTheory;
    }
}
