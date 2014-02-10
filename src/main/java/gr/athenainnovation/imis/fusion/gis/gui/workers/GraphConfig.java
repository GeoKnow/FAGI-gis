package gr.athenainnovation.imis.fusion.gis.gui.workers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Keeps info about graph names and given endpoints.
 */
public class GraphConfig {
    private final String graphA, graphB, endpointA, endpointB;
    
    
    public GraphConfig(String graphA, String graphB, String endpointA, String endpointB){
        this.graphA = checkNotNull(graphA, "graph name cannot be null.");
        this.graphB = checkNotNull(graphB, "graph name cannot be null.");
        this.endpointA = checkNotNull(endpointA, "endpoint cannot be null.");
        this.endpointB = checkNotNull(endpointB, "endpoint cannot be null.");
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
        
}
