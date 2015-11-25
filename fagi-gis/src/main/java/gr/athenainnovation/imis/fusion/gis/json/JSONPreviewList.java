/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

import java.util.List;

/**
 *
 * @author nick
 */
public class JSONPreviewList {
    List<JSONPreviewLink>       links;
    JSONRequestResult           result;

    public List<JSONPreviewLink> getLinks() {
        return links;
    }

    public void setLinks(List<JSONPreviewLink> links) {
        this.links = links;
    }

    public JSONRequestResult getResult() {
        return result;
    }

    public void setResult(JSONRequestResult result) {
        this.result = result;
    }
    
}
