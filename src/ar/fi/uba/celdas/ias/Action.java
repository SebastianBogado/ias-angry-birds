package ar.fi.uba.celdas.ias;

import ab.vision.Vision;

import java.awt.Point;

public interface Action {
    Point getTarget(Vision vision);
}
