package ar.fi.uba.celdas.ias;

import ab.vision.Vision;

import java.awt.*;

/**
 * Created by seba on 2/3/16.
 */
public class HitAction implements Action {
    @Override
    public Point getTarget(Vision vision) {
        return vision.findPigsMBR().get(0).getCenter();
    }
}
