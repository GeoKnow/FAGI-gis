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
public class JSONGeomLink {

    String subA, geomA;
    String subB, geomB;
    double dist, jIndex;

    public JSONGeomLink() {
    }

    public JSONGeomLink(String subA, String geomA, String subB, String geomB) {
        this.subA = subA;
        this.geomA = geomA;
        this.subB = subB;
        this.geomB = geomB;
    }

    public JSONGeomLink(String subA, String geomA, String subB, String geomB, double dist, double jIndex) {
        this.subA = subA;
        this.geomA = geomA;
        this.subB = subB;
        this.geomB = geomB;
        this.dist = dist;
        this.jIndex = jIndex;
    }

    public String getSubA() {
        return subA;
    }

    public void setSubA(String subA) {
        this.subA = subA;
    }

    public String getGeomA() {
        return geomA;
    }

    public void setGeomA(String geomA) {
        this.geomA = geomA;
    }

    public String getSubB() {
        return subB;
    }

    public void setSubB(String subB) {
        this.subB = subB;
    }

    public String getGeomB() {
        return geomB;
    }

    public void setGeomB(String geomB) {
        this.geomB = geomB;
    }

    public double getDist() {
        return dist;
    }

    public void setDist(double dist) {
        this.dist = dist;
    }

    public double getjIndex() {
        return jIndex;
    }

    public void setjIndex(double jIndex) {
        this.jIndex = jIndex;
    }

}
