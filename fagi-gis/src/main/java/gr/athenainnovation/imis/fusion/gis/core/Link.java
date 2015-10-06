package gr.athenainnovation.imis.fusion.gis.core;

import java.io.Serializable;

/**
 * Represents a link between two RDF nodes.
 * @author Thomas Maroulis
 */
public class Link implements Serializable {
    private String nodeA, nodeB;
    
    /**
     * Constructs a new link between two nodes.
     * @param nodeA URI of first node
     * @param nodeB URI of second node
     */
    public Link(final String nodeA, final String nodeB) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
    }
    
    /**
     *
     * @return URI of first node
     */
    public String getNodeA() {
        return nodeA;
    }
    
    /**
     *
     * @return URI of second node
     */
    public String getNodeB() {
        return nodeB;
    }
    
    /**
     * 
     * @return a key identifying this link
     */
    public String getKey() {
        return nodeA + " <--> " + nodeB;
    } 
}
