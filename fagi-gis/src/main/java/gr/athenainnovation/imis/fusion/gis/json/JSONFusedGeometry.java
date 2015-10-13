/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

/**
 * Holds a single geometry with its associated subject
 * @author Nick Vitsas
 */
public class JSONFusedGeometry {

    String geom;
    String subject;

    public JSONFusedGeometry(String geom, String subject) {
        this.geom = geom;
        this.subject = subject;
    }

    public JSONFusedGeometry() {
    }

    public String getGeom() {
        return geom;
    }

    public void setGeom(String geom) {
        this.geom = geom;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

}
