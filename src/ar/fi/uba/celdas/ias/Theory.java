package ar.fi.uba.celdas.ias;

import ab.vision.Vision;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by seba on 2/2/16.
 */
public class Theory {

    public List<TheoryCondition> preconditions;
    public Action action;
    public List<TheoryCondition> postconditions;
    public int useCount;
    public int successCount;
    public int acummulatedScore;

    public Theory() {
        preconditions = new ArrayList<TheoryCondition>();
        postconditions = new ArrayList<TheoryCondition>();
        useCount = 0;
        successCount = 0;
        acummulatedScore = 0;
    }

    public double successRatio() {
        return (float)successCount / useCount;
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
                                otherPreconditionsStream.anyMatch(precondition ->
                                        myPrecondition.equals(precondition) || myPrecondition.isMoreSpecific(precondition))
                );


        return preconditionsAreEqual && similar(other);
    }

    public boolean similar(Theory other) {
        boolean actionsAreEqual = this.action.equals(other.action);

        Stream<TheoryCondition> otherPostconditionsStream = other.postconditions.stream();
        boolean postconditionsAreEqual = this.postconditions.stream()
                .allMatch(myPostcondition ->
                                otherPostconditionsStream.anyMatch(postcondition ->
                                        myPostcondition.equals(postcondition) || myPostcondition.isMoreSpecific(postcondition))
                );

        return actionsAreEqual && postconditionsAreEqual;
    }
}
