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
public class JSONProperty {

    String short_rep;
    String long_rep;

    public JSONProperty() {
        this.short_rep = "";
        this.long_rep = "";
    }

    public JSONProperty(String short_rep, String long_rep) {
        this.short_rep = short_rep;
        this.long_rep = long_rep;
    }

    public String getShort_rep() {
        return short_rep;
    }

    public void setShort_rep(String short_rep) {
        this.short_rep = short_rep;
    }

    public String getLong_rep() {
        return long_rep;
    }

    public void setLong_rep(String long_rep) {
        this.long_rep = long_rep;
    }

}