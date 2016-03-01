package ar.fi.uba.celdas.ias;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.Vision;
import ar.fi.uba.celdas.utils.Utils;

public class HitAction implements Action {

    public ABType type;

    public Boolean furtherToTheLeft;

    public HitAction() {
        type = Utils.ANY_TYPE;
        furtherToTheLeft = true;
    }

    @Override
    public Point getTarget(Vision vision) {
        List<ABObject> abObjectsOfType = new ArrayList<>();

        List<ABObject> everyObject = new ArrayList<>(vision.findBlocksMBR());
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HitAction other = (HitAction) obj;

        return this.type == other.type &&
                this.furtherToTheLeft == other.furtherToTheLeft;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");

//        result.append(this.getClass().getName() + " Object {" + NEW_LINE);
        result.append("Hit further to the left of type " + type.name());

        return result.toString();
    }
}
