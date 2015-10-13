/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

/**
 * Contain the results from LinkServlet
 * @author Nick Vitsas
 */
public class JSONLoadLinksResult {
    
    String                      linkListHTML;
    String                      filtersListAHTML;
    String                      filtersListBHTML;

    JSONRequestResult           result;
    
    public String getLinkListHTML() {
        return linkListHTML;
    }

    public void setLinkListHTML(String linkListHTML) {
        this.linkListHTML = linkListHTML;
    }

    public String getFiltersListAHTML() {
        return filtersListAHTML;
    }

    public void setFiltersListAHTML(String filtersListAHTML) {
        this.filtersListAHTML = filtersListAHTML;
    }

    public String getFiltersListBHTML() {
        return filtersListBHTML;
    }

    public void setFiltersListBHTML(String filtersListBHTML) {
        this.filtersListBHTML = filtersListBHTML;
    }

    public JSONRequestResult getResult() {
        return result;
    }

    public void setResult(JSONRequestResult result) {
        this.result = result;
    }
    
}
