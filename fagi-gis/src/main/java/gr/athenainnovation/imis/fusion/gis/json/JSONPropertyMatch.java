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
public class JSONPropertyMatch {

    // For easy construction
    public String           valueA;
    public String           valueB;
    
    String                  property;
    String                  propertyLong;
    String                  result;

    public JSONPropertyMatch() {
        valueA = "";
        valueB = "";
        property = "";
        propertyLong = "";
        result = "";
    }

    public String getPropertyLong() {
        return propertyLong;
    }

    public void setPropertyLong(String propertyLong) {
        this.propertyLong = propertyLong;
    }

    public String getValueA() {
        return valueA;
    }

    public void setValueA(String valueA) {
        this.valueA = valueA;
    }

    public String getValueB() {
        return valueB;
    }

    public void setValueB(String valueB) {
        this.valueB = valueB;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

}
