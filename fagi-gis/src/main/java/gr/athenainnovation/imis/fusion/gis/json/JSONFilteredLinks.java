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
public class JSONFilteredLinks {
    String                  linksHTML;
    JSONRequestResult       result;

    public String getLinksHTML() {
        return linksHTML;
    }

    public void setLinksHTML(String linksHTML) {
        this.linksHTML = linksHTML;
    }

    public JSONRequestResult getResult() {
        return result;
    }

    public void setResult(JSONRequestResult result) {
        this.result = result;
    }
    
}
