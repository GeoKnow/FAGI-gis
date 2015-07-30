/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.clustering;

/**
 *
 * @author nick
 */
public class ClusteringVector {
    public String nodeA;
    public String nodeB;
    public DoubleWrapper dist;
    public Vector2D v;
    public int intersects;
    
    public ClusteringVector() {
    }

    public ClusteringVector(String nodeA, String nodeB) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
    }
    
}
