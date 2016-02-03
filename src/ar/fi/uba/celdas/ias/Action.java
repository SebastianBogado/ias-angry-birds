package ar.fi.uba.celdas.ias;

import ab.vision.Vision;

import java.awt.*;

/**
 * Created by seba on 2/3/16.
 */
public interface Action {
    Point getTarget(Vision vision);
}
