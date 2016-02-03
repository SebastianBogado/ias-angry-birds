package ar.fi.uba.celdas.ias;

import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.Vision;

/**
 * Created by seba on 2/3/16.
 */
public class CountTheoryCondition implements TheoryCondition {

    int count;
    ABType type;

    public CountTheoryCondition(int _count) {
        this(_count, null);
    }

    public CountTheoryCondition(int _count, ABType _type) {
        count = _count;
        type = _type;
    }

    @Override
    public Boolean satisfies(Vision vision) {
        int totalABObjects = countABObjectsOfType(vision);
        return totalABObjects == count;
    }

    int countABObjectsOfType(Vision vision) {
        int totalABObjects = 0;

        if (type != null) {
            if (type == ABType.Pig) {
                totalABObjects = vision.findPigsMBR().size();
            } else if (type == ABType.Ice || type == ABType.Wood || type == ABType.Stone) {

                for (ABObject abObject : vision.findBlocksMBR())
                    if (abObject.type == type)
                        totalABObjects++;

            }
        } else {
            totalABObjects = vision.findBlocksMBR().size() + vision.findPigsMBR().size();
        }

        return totalABObjects;
    }
}
