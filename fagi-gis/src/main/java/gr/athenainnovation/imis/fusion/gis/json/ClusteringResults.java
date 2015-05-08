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
 * @author nick
 */
public class ClusteringResults {
    Map<String, ClusteringResult> results;
    int numOfClusters;
    
    public ClusteringResults() {
        results = new HashMap<>();
        numOfClusters = 0;
    }

    public int getNumOfClusters() {
        return numOfClusters;
    }

    public void setNumOfClusters(int numOfClusters) {
        this.numOfClusters = numOfClusters;
    }

    public Map<String, ClusteringResult> getResults() {
        return results;
    }

    public void setResults(Map<String, ClusteringResult> results) {
        this.results = results;
    }
}
