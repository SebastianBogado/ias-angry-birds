package ar.fi.uba.celdas.utils;

import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.Vision;

import java.util.*;

/**
 * Created by seba on 2/6/16.
 */
public class Utils {
    // Using ABType.Unknown as 'any' because it's not used, at least during the first levels
    public static ABType ANY_TYPE = ABType.Unknown;

    public static int getTotalABObjects(Vision vision) {
        return getTotalABObjects(vision, ANY_TYPE);
    }

    public static int getTotalABObjects(Vision vision, ABType type) {
        int totalABObjects = 0;

        if (type != ANY_TYPE) {
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


    public static ABType getRandomAvailableType(Vision vision) {
        Set<ABType> availableTypes = new HashSet<ABType>();
        List<ABObject> everyObject = new ArrayList<ABObject>(vision.findBlocksMBR());
        everyObject.addAll(vision.findPigsMBR());

        for (ABObject abObject : everyObject)
            availableTypes.add(abObject.type);

        int idx = new Random().nextInt(availableTypes.size());
        int i = 0;
        for(ABType type : availableTypes) {
            if (i++ == idx)
                return type;
        }

        return ABType.Pig;
    }
}
