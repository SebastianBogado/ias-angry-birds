package ar.fi.uba.celdas.ias;

import ab.vision.ABType;
import ab.vision.Vision;
import ar.fi.uba.celdas.utils.Utils;

public class CountTheoryCondition implements TheoryCondition {

    public int count;
    public ABType type;

    public Boolean negate;
    public Boolean greaterOrEqualThan;
    public Boolean lessOrEqualThan;
    public Boolean equalThan;

    public static int ANY_COUNT = -1;

    public CountTheoryCondition() {
        negate = false;

        greaterOrEqualThan = false;
        lessOrEqualThan = false;
        equalThan = true;

        count = ANY_COUNT;
        type = Utils.ANY_TYPE;
    }


    CountTheoryCondition not() {
        negate = true;
        return this;
    }

    CountTheoryCondition atLeast(int _count) {
        setCount(_count, true, false, false);

        return this;
    }

    CountTheoryCondition noMoreThan(int _count) {
        setCount(_count, false, true, false);

        return this;
    }

    CountTheoryCondition exactly(int _count) {
        setCount(_count, false, false, true);

        return this;
    }

    CountTheoryCondition ofType(ABType _type) {
        type = _type;
        return this;
    }

    @Override
    public Boolean satisfies(Vision vision) {
        int totalABObjects = Utils.getTotalABObjects(vision, type);

        // TODO IMPROVE. Hating myself for writing this kind of stuff
        Boolean countSatisfies;
        if (greaterOrEqualThan) {
            countSatisfies = totalABObjects >= count;
        } else if (lessOrEqualThan) {
            countSatisfies = totalABObjects <= count;
        } else if (equalThan) {
            countSatisfies = totalABObjects == count;
        } else {
            System.out.println("CountTheoryCondition: it will never be satisfied");
            countSatisfies = negate;
        }

        return negate ? !countSatisfies : countSatisfies;
    }

    @Override
    public Boolean isMoreSpecific(TheoryCondition other) {
        return this.equals(other);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof CountTheoryCondition))
            return false;

        CountTheoryCondition other = (CountTheoryCondition) obj;

        return this.count == other.count &&
                this.type == other.type &&
                this.negate == other.negate &&
                this.greaterOrEqualThan == other.greaterOrEqualThan &&
                this.lessOrEqualThan == other.lessOrEqualThan &&
                this.equalThan == other.equalThan;
    }

    private void setCount(int _count, Boolean _greaterOrEqualThan, Boolean _lessOrEqualThan, Boolean _equalThan) {
        count = _count;

        greaterOrEqualThan = _greaterOrEqualThan;
        lessOrEqualThan = _lessOrEqualThan;
        equalThan = _equalThan;
    }

    @Override
    public String toString() {
        return "There are: " + (greaterOrEqualThan ? "at least " : "") + (count == ANY_COUNT ? " any " : count) + " of type " + type.name();
    }
}
