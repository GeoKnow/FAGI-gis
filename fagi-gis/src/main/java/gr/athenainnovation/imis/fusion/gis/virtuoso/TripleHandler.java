

package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 *
 * @author Nick Vitsasvitsas
 */
public interface TripleHandler {
    void addTriple(Triple tr);
    void addTriple(Node s, Node p, Node o);
    void addTriple(String s, String p, String o);
    
    void addGeomTriple(String subject, String fusedGeometry);
    
    void deleteTriple(Triple tr);
    void deleteTriple(Node s, Node p, Node o);
    void deleteTriple(String s, String p, String o);
    
    void init();
    void finish();
    
    void deleteAllGeom(String subject);
    void deleteAllWgs84(String subject);
}
