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
public class JSONDatasetConfigResult {
    boolean                 remoteLinks;
    JSONRequestResult       result;

    public boolean HasRemoteLinks() {
        return remoteLinks;
    }

    public void setHasRemoteLinks(boolean hasRemoteLinks) {
        this.remoteLinks = hasRemoteLinks;
    }

    public JSONRequestResult getResult() {
        return result;
    }

    public void setResult(JSONRequestResult result) {
        this.result = result;
    }
    
    
}
