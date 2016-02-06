package ar.fi.uba.celdas.ias;

import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.Vision;
import ar.fi.uba.celdas.utils.Utils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by seba on 2/3/16.
 */
public class HitAction implements Action {

    ABType type;

    Boolean furtherToTheLeft;

    public HitAction() {
        type = Utils.ANY_TYPE;
        furtherToTheLeft = true;
    }

    @Override
    public Point getTarget(Vision vision) {
        List<ABObject> abObjectsOfType = new ArrayList<ABObject>();

        List<ABObject> everyObject = new ArrayList<ABObject>(vision.findBlocksMBR());
        everyObject.addAll(vision.findPigsMBR());

        for (ABObject abObject : everyObject) {
            if (type == Utils.ANY_TYPE ||  abObject.type == type) {
                abObjectsOfType.add(abObject);
            }
        }

        ABObject selectedABObject = abObjectsOfType.get(0);

        for (ABObject abObjectOfType : abObjectsOfType) {
            if (abObjectOfType.getMinX() < selectedABObject.getMinX()) {
                selectedABObject = abObjectOfType;
            }
        }

        return selectedABObject.getCenter();
    }

    public HitAction ofType(ABType _type) {
        type = _type;

        return this;
    }

    public HitAction furtherToTheLeft() {
        furtherToTheLeft = true;

        return this;
    }
}
