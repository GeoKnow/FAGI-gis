/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

/**
 *
 * @author Nick Vitsas
 */
public class JSONShiftFactors {

    Float shift;
    Float scaleFact;
    Float rotateFact;

    public JSONShiftFactors() {
    }

    public Float getShift() {
        return shift;
    }

    public void setShift(Float shift) {
        this.shift = shift;
    }

    public Float getScaleFact() {
        return scaleFact;
    }

    public void setScaleFact(Float scaleFact) {
        this.scaleFact = scaleFact;
    }

    public Float getRotateFact() {
        return rotateFact;
    }

    public void setRotateFact(Float rotateFact) {
        this.rotateFact = rotateFact;
    }

}
