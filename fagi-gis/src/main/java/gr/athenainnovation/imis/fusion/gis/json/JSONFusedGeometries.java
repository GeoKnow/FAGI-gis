/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nick Vitsas
 */
public class JSONFusedGeometries {

    List<JSONFusedGeometry>         geoms;
    JSONRequestResult               result;
    
    public JSONFusedGeometries() {
        geoms = new ArrayList<>();
    }

    public List<JSONFusedGeometry> getGeoms() {
        return geoms;
    }

    public void setGeoms(List<JSONFusedGeometry> geoms) {
        this.geoms = geoms;
    }

    public JSONRequestResult getResult() {
        return result;
    }

    public void setResult(JSONRequestResult result) {
        this.result = result;
    }

}
