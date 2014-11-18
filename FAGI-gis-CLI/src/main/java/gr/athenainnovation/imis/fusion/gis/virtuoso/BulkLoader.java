
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.hp.hpl.jena.graph.GraphUtil;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.util.ArrayList;
import java.util.List;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author nickvitsas
 */
public class BulkLoader implements TripleHandler {
    
    private static final String DB_URL = "jdbc:postgresql:";
    private static final String WKT = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String HAS_GEOMETRY = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String LAT = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    private static final String LON = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private static final String clearBulkLoadTbl = "DELETE FROM DB.DBA.load_list";
    
    private static final Node LAT_NODE = NodeFactory.createURI(LAT);
    private static final Node LON_NODE = NodeFactory.createURI(LON);
    private static final Node HAS_GEO_NODE = NodeFactory.createURI(HAS_GEOMETRY);
    private static final Node WKT_NODE = NodeFactory.createURI(WKT);
            
    private final String fusedGraph;
    private final VirtGraph set;
    private List<Triple> toAdd;
    private List<Triple> toDel;
    
    BulkLoader(String graph, VirtGraph s) {
        this.fusedGraph = graph;
        this.set = s;
    }
    
    @Override
    public void addTriple(Triple tr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addTriple(Node s, Node p, Node o) {
        //set.add(new Triple(s,p,o));
        toAdd.add(new Triple(s,p,o));
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
        
        Node o1 = NodeFactory.createLiteral( fusedGeometry + "^^<http://www.opengis.net/ont/geosparql#wktLiteral>");

        addTriple(s,HAS_GEO_NODE,o);
        addTriple(s1,WKT_NODE,o1);
    }

    @Override
    public void deleteTriple(Triple tr) {
        toDel.add(tr);
    }

    @Override
    public void deleteTriple(Node s, Node p, Node o) {
        toDel.add(new Triple(s,p,o));
    }

    @Override
    public void deleteTriple(String s, String p, String o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteAllGeom(String subject) {
        ExtendedIterator<Node> nd = GraphUtil.listObjects(set, NodeFactory.createURI(subject), HAS_GEO_NODE);
        if ( nd.hasNext() ) {
            //set.remove(NodeFactory.createURI(subject), NodeFactory.createURI(HAS_GEOMETRY), nd.next());
            //set.remove(nd.next(), NodeFactory.createURI(WKT), Node.ANY);
            Node tmp = nd.next();
            deleteTriple(new Triple(NodeFactory.createURI(subject), HAS_GEO_NODE, tmp));
            deleteTriple(new Triple(tmp, WKT_NODE, Node.ANY));
        }
    }

    @Override
    public void deleteAllWgs84(String subject) {
        //set.remove(NodeFactory.createURI(subject), NodeFactory.createURI(LAT), Node.ANY);
        //set.remove(NodeFactory.createURI(subject), NodeFactory.createURI(LON), Node.ANY);
        deleteTriple(NodeFactory.createURI(subject), LAT_NODE, Node.ANY);
        deleteTriple(NodeFactory.createURI(subject), LON_NODE, Node.ANY);
    }

    @Override
    public void init() {
        toAdd = new ArrayList<>();
        toDel = new ArrayList<>();
    }

    @Override
    public void finish() {
        GraphUtil.delete(set, toDel);
        GraphUtil.add(set, toAdd);
    }
    
}
