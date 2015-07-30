

package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.hp.hpl.jena.graph.GraphUtil;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author nickvitsas
 */
public class VirtLoader implements TripleHandler{
    
    private static final String DB_URL = "jdbc:postgresql:";
    private static final String WKT = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String HAS_GEOMETRY = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String LAT = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    private static final String LON = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private static final String clearBulkLoadTbl = "DELETE FROM DB.DBA.load_list";
    
    private final String fusedGraph;
    private final VirtGraph set;
        
    VirtLoader(String graph, VirtGraph s) {
        this.fusedGraph = graph;
        this.set = s;
    }
    
    @Override
    public void addTriple(Triple tr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addTriple(Node s, Node p, Node o) {
            set.add(new Triple(s,p,o));
    }

    @Override
    public void addTriple(String s, String p, String o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addGeomTriple(String subject, String fusedGeometry) {
        Node s = NodeFactory.createURI(subject);
        Node p = NodeFactory.createURI(HAS_GEOMETRY);
        Node o = NodeFactory.createAnon();

        Node s1 = NodeFactory.createURI(o.getBlankNodeLabel());
        Node p1 = NodeFactory.createURI(WKT);
        Node o1 = NodeFactory.createLiteral("\"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral");

        addTriple(s,p,o);
        addTriple(s1,p1,o1);
    }

    @Override
    public void deleteTriple(Triple tr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteTriple(Node s, Node p, Node o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteTriple(String s, String p, String o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteAllGeom(String subject) {
        ExtendedIterator<Node> nd = GraphUtil.listObjects(set, NodeFactory.createURI(subject), NodeFactory.createURI(HAS_GEOMETRY));
        if ( nd.hasNext() ) {
            set.remove(NodeFactory.createURI(subject), NodeFactory.createURI(HAS_GEOMETRY), nd.next());
            set.remove(nd.next(), NodeFactory.createURI(WKT), Node.ANY);
        }
    }

    @Override
    public void deleteAllWgs84(String subject) {
        set.remove(NodeFactory.createURI(subject), NodeFactory.createURI(LAT), Node.ANY);
        set.remove(NodeFactory.createURI(subject), NodeFactory.createURI(LON), Node.ANY);
    }

    @Override
    public void init() {
    }

    @Override
    public void finish() {
    }
    
}
