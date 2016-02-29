package ar.fi.uba.celdas.ias;

import ab.vision.Vision;

public interface TheoryCondition {
    Boolean satisfies(Vision vision);

    Boolean isMoreSpecific(TheoryCondition theoryCondition);
}
