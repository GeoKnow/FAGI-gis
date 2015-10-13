/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Nick Vitsas
 */
public class JSONClusteringResults {
    Map<String, JSONClusteringResult> results;
    int numOfClusters;
    
    public JSONClusteringResults() {
        results = new HashMap<>();
        numOfClusters = 0;
    }

    public int getNumOfClusters() {
        return numOfClusters;
    }

    public void setNumOfClusters(int numOfClusters) {
        this.numOfClusters = numOfClusters;
    }

    public Map<String, JSONClusteringResult> getResults() {
        return results;
    }

    public void setResults(Map<String, JSONClusteringResult> results) {
        this.results = results;
    }
}
