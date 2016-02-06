package ar.fi.uba.celdas.ias;

import ab.vision.Vision;

import java.util.ArrayList;
import java.util.List;

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
}
