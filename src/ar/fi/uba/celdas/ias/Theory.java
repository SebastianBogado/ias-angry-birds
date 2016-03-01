package ar.fi.uba.celdas.ias;

import ab.vision.Vision;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Theory {

    public List<TheoryCondition> preconditions;
    public Action action;
    public List<TheoryCondition> postconditions;
    public int useCount;
    public int successCount;
    public int accumulatedScore;
    public transient int id;

    static private int idGenerator = 0;
    private boolean cloned;
    private Theory clonedFrom;

    public Theory() {
        preconditions = new ArrayList<>();
        postconditions = new ArrayList<>();
        useCount = 0;
        successCount = 0;
        accumulatedScore = 0;
        id = idGenerator++;
        cloned = false;
        clonedFrom = null;
    }

    public double successRatio() {
        return  (useCount == 0 ? 0 : (float)successCount / useCount);
    }

    public double successRatioWithScore() {
        return accumulatedScore * successRatio();
    }

    public Boolean satisfiesPreconditions(Vision vision) {
        return satisfiesConditions(vision, preconditions);
    }

    public Boolean satisfiesPostconditions(Vision vision) {
        return satisfiesConditions(vision, postconditions);
    }

    private Boolean satisfiesConditions(Vision vision, List<TheoryCondition> conditions) {
        Boolean doesSatisfy = true;
        int i = 0;

        while (doesSatisfy && i < conditions.size())
            doesSatisfy = conditions.get(i++).satisfies(vision);

        return doesSatisfy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Theory other = (Theory) obj;

        Stream<TheoryCondition> otherPreconditionsStream = other.preconditions.stream();
        boolean preconditionsAreEqual = this.preconditions.stream()
                .allMatch(myPrecondition ->
                                other.preconditions.stream().anyMatch(precondition ->
                                        myPrecondition.equals(precondition) || myPrecondition.isMoreSpecific(precondition))
                );


        return preconditionsAreEqual && similar(other);
    }

    public boolean similar(Theory other) {
        boolean actionsAreEqual = this.action.equals(other.action);

        Stream<TheoryCondition> otherPostconditionsStream = other.postconditions.stream();
        boolean postconditionsAreEqual = this.postconditions.stream()
                .allMatch(myPostcondition ->
                                other.postconditions.stream().anyMatch(postcondition ->
                                        myPostcondition.equals(postcondition) || myPostcondition.isMoreSpecific(postcondition))
                );

        return actionsAreEqual && postconditionsAreEqual;
    }

    @Override
    public String toString() {
        if (cloned) return clonedFrom.toString();

        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");

        result.append(this.getClass().getName() + " Object {" + NEW_LINE);
        result.append("\tID: " + id + NEW_LINE);

        result.append("\tPreconditions: " + NEW_LINE);
        for (TheoryCondition theoryCondition : preconditions) {
            result.append("\t\t" + theoryCondition + NEW_LINE);
        }
        result.append("\tAction: " + action + NEW_LINE);

        result.append("\tPostconditions: " + NEW_LINE);
        for (TheoryCondition theoryCondition : postconditions) {
            result.append("\t\t" + theoryCondition + NEW_LINE);
        }


        result.append("\tUse count (K): " + useCount + NEW_LINE);
        result.append("\tSuccess count (P): " + successCount + NEW_LINE);
        result.append("\tAccumulated score: " + accumulatedScore + NEW_LINE);
        result.append("\tSucces ratio: " + successRatio() + NEW_LINE);
        result.append("\tSucces ratio with accumulated score: " + successRatioWithScore() + NEW_LINE);
        result.append("}");

        return result.toString();
    }

    public Theory clonePreconditionsAndAction() {
        Theory theory = new Theory();
        theory.preconditions.addAll(this.preconditions);
        theory.action = this.action;
        theory.cloned = true;
        theory.clonedFrom = this;
        return theory;
    }
}
