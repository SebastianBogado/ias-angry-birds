package ar.fi.uba.celdas.ias;

import ab.vision.Vision;

import java.awt.*;

public interface Action {
    Point getTarget(Vision vision);
}
