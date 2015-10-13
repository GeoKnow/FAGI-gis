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
public class JSONPreviewResult {
    
    String                      geos;

    JSONRequestResult           result;

    public String getGeos() {
        return geos;
    }

    public void setGeos(String geos) {
        this.geos = geos;
    }

    public JSONRequestResult getResult() {
        return result;
    }

    public void setResult(JSONRequestResult result) {
        this.result = result;
    }
        
}
