package gr.athenainnovation.imis.fusion.gis.gui.workers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Keeps info about graph names and given endpoints.
 */
public class GraphConfig {
    private String graphA, graphB, endpointA, endpointB, endpointLoc;
    
    
    public GraphConfig(String graphA, String graphB, String endpointA, String endpointB){
        this.graphA = checkNotNull(graphA, "graph name cannot be null.");
        this.graphB = checkNotNull(graphB, "graph name cannot be null.");
        this.endpointA = checkNotNull(endpointA, "endpoint cannot be null.");
        this.endpointB = checkNotNull(endpointB, "endpoint cannot be null.");
        this.endpointLoc = "";
    }
    
    public GraphConfig(String graphA, String graphB, String endpointA, String endpointB, String endLoc){
        this.graphA = checkNotNull(graphA, "graph name cannot be null.");
        this.graphB = checkNotNull(graphB, "graph name cannot be null.");
        this.endpointA = checkNotNull(endpointA, "endpoint cannot be null.");
        this.endpointB = checkNotNull(endpointB, "endpoint cannot be null.");
        this.endpointLoc = endLoc;
    }

    public String getEndpointLoc() {
        return endpointLoc;
    }

    public void setEndpointLoc(String endpointLoc) {
        this.endpointLoc = endpointLoc;
    }
    
    public String getGraphA() {
        return graphA;
    }
     
    public String getGraphB() {
        return graphB;
    }
    
    public String getEndpointA() {
        return endpointA;
    }
    
    public String getEndpointB() {
        return endpointB;
    }

    public void setGraphA(String graphA) {
        this.graphA = graphA;
    }

    public void setGraphB(String graphB) {
        this.graphB = graphB;
    }

    public void setEndpointA(String endpointA) {
        this.endpointA = endpointA;
    }

    public void setEndpointB(String endpointB) {
        this.endpointB = endpointB;
    }
        
    
}
