
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.hp.hpl.jena.graph.GraphUtil;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoException;
import virtuoso.jdbc4.VirtuosoPreparedStatement;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsasvitsas
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
    private final String dbName;
    private List<Triple> toAdd;
    private List<Triple> toDel;
    private List<GeomTriple> geoms;
    private String endpointT;
    private GraphConfig grConf;
    
    private class GeomTriple {
        public String s;
        public String g;

        public GeomTriple(String s, String g) {
            this.g = g;
            this.s = s;
        }
    }
    
    BulkLoader(GraphConfig grConf, String graph, String dbName,  VirtGraph s, String endT) {
        this.fusedGraph = graph;
        this.grConf = grConf;
        this.set = s;
        this.endpointT = endT;
        this.dbName = dbName;
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
        /*Node s = NodeFactory.createURI(subject);
        Node p = NodeFactory.createURI(HAS_GEOMETRY);
        Node o = NodeFactory.createAnon();

        Node s1 = NodeFactory.createURI(o.getBlankNodeLabel());
        Node p1 = NodeFactory.createURI(WKT);
        
        Node o1 = NodeFactory.createLiteral( fusedGeometry + "^^<http://www.opengis.net/ont/geosparql#wktLiteral>");

        addTriple(s,HAS_GEO_NODE,o);
        addTriple(s1,WKT_NODE,o1);*/
        
        geoms.add(new GeomTriple(subject, fusedGeometry));
    }

    @Override
    public void deleteTriple(Triple tr) {
        //toDel.add(tr);
    }

    @Override
    public void deleteTriple(Node s, Node p, Node o) {
        //toDel.add(new Triple(s,p,o));
    }

    @Override
    public void deleteTriple(String s, String p, String o) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteAllGeom(String subject) {
        /*ExtendedIterator<Node> nd = GraphUtil.listObjects(set, NodeFactory.createURI(subject), HAS_GEO_NODE);
        if ( nd.hasNext() ) {
            //set.remove(NodeFactory.createURI(subject), NodeFactory.createURI(HAS_GEOMETRY), nd.next());
            //set.remove(nd.next(), NodeFactory.createURI(WKT), Node.ANY);
            Node tmp = nd.next();
            deleteTriple(new Triple(NodeFactory.createURI(subject), HAS_GEO_NODE, tmp));
            deleteTriple(new Triple(tmp, WKT_NODE, Node.ANY));
        }*/
    }

    @Override
    public void deleteAllWgs84(String subject) {
        //set.remove(NodeFactory.createURI(subject), NodeFactory.createURI(LAT), Node.ANY);
        //set.remove(NodeFactory.createURI(subject), NodeFactory.createURI(LON), Node.ANY);
        //deleteTriple(NodeFactory.createURI(subject), LAT_NODE, Node.ANY);
        //deleteTriple(NodeFactory.createURI(subject), LON_NODE, Node.ANY);
    }

    @Override
    public void init() {
        toAdd = new ArrayList<>();
        toDel = new ArrayList<>();
        geoms = new ArrayList<>();
    }

    public void updateLocalStore() {
        String s2 = "SPARQL WITH <"+this.grConf.getTargetTempGraph()+"> INSERT { `iri(??)` <"+HAS_GEOMETRY+">  `iri(??)` . `iri(??)`  <"+WKT+"> ?? }";
        VirtuosoConnection conn = (VirtuosoConnection) set.getConnection();
        VirtuosoPreparedStatement stmt = null;
        try {
            conn.setAutoCommit(false);
        } catch (VirtuosoException ex) {
            System.out.println("Virtuoso SQL Exception - Commit to false failed");
        }
        
        try {
            stmt = (VirtuosoPreparedStatement) conn.prepareStatement(s2);
            System.out.println("Testing batch insert");
            //stmt.setString(1, "http://localhost:8890/DAV/osm_demo_asasas");
            for (GeomTriple geom : geoms) {
                final String link = geom.s + "_geom";
                stmt.setString(1, geom.s);
                stmt.setString(2, link);
                stmt.setString(3, link);
                stmt.setString(4, geom.g);
                stmt.addBatch();
            }
            stmt.executeBatchUpdate();
            
            stmt.close();
        } catch (SQLException ex) {
            System.out.println("SQL Exception");
        }
        try {
            conn.commit();
            conn.setAutoCommit(true);
            //StreamRDF destination = null;
            //RDFDataMgr.parse(destination, "http://example/data.ttl") ;
        } catch (VirtuosoException ex) {
            System.out.println("Virtuoso SQL Exception");
        }
    }
    
    @Override
    public void finish() {
        //updateLocalStore();
        /*//GraphUtil.delete(set, toDel);
        //GraphUtil.add(set, toAdd);
        UpdateRequest request = UpdateFactory.create() ;
        request.add("CLEAR GRAPH <"+fusedGraph+">");
        UpdateProcessor up = UpdateExecutionFactory.createRemoteForm(request, endpointT);
            //up.execute();
            
            boolean updating = true;
            int addIdx = 0;
            int cSize = 1;
            int sizeUp = 1;
            System.out.println("Sise "+geoms.size());
            while (updating) {
                try {
                    ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                    queryStr.append("INSERT DATA { GRAPH <"+fusedGraph+"> ");
                    queryStr.append("{");
                    int top = 0;
                    if (cSize >= geoms.size())
                        top = geoms.size();
                    else
                        top = cSize;
                    for (int i = addIdx; i < top; i++) {
                        final String subject = geoms.get(i).s;
                        queryStr.appendIri(subject);
                        queryStr.append(" ");
                        queryStr.appendIri(HAS_GEOMETRY);
                        queryStr.append(" ");
                        queryStr.appendIri(subject+"_geom");
                        queryStr.append(" . ");
                        queryStr.appendIri( subject+"_geom" );
                        queryStr.append(" ");
                        queryStr.appendIri( WKT );
                        queryStr.append(" ");
                        queryStr.appendLiteral(geoms.get(i).g + "^^<http://www.opengis.net/ont/geosparql#wktLiteral>");
                        queryStr.append(" . ");
                    }
                    queryStr.append("}");
                    queryStr.append("}");
                    System.out.println("Print "+queryStr.toString());

                    UpdateRequest q = queryStr.asUpdate();
                    UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, endpointT);
                    insertRemoteB.execute();
                    //System.out.println("Add at "+addIdx+" Size "+cSize);
                    addIdx += (cSize-addIdx);
                    sizeUp *= 2;
                    cSize += sizeUp;
                    if (cSize >= geoms.size()) {
                        cSize = geoms.size();
                    }
                    if (cSize == addIdx)
                        updating = false;
                } catch (org.apache.jena.atlas.web.HttpException ex) {
                    //System.out.println("Failed at "+addIdx+" Size "+cSize);
                    //System.out.println("Crazy Stuff");
                    sizeUp = 1;
                    cSize = addIdx;
                    cSize += sizeUp;
                    if (cSize >= geoms.size())
                        cSize = geoms.size();
                    //System.out.println("Going back at "+addIdx+" Size "+cSize);
                }
            }
    }*/
    //GraphUtil.delete(set, toDel);
        //GraphUtil.add(set, toAdd);
        UpdateRequest request = UpdateFactory.create() ;
        request.add("CLEAR GRAPH <"+fusedGraph+">");
        UpdateProcessor up = UpdateExecutionFactory.createRemoteForm(request, endpointT);
            //up.execute();
            
            boolean updating = true;
            int addIdx = 0;
            int cSize = 1;
            int sizeUp = 1;
            //System.out.println("Sise "+geoms.size());
            while (updating) {
                try {
                    ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                    queryStr.append("WITH <"+fusedGraph+"> ");
                    queryStr.append("DELETE {");
                    int top = 0;
                    if (cSize >= geoms.size())
                        top = geoms.size();
                    else
                        top = cSize;
                    for (int i = addIdx; i < top; i++) {
                        final String subject = geoms.get(i).s;
                        queryStr.appendIri(subject);
                        queryStr.append(" ");
                        queryStr.appendIri(HAS_GEOMETRY);
                        queryStr.append(" ");
                        queryStr.appendIri(subject+"_geom");
                        queryStr.append(" . ");
                        queryStr.appendIri( subject+"_geom" );
                        queryStr.append(" ");
                        queryStr.appendIri( WKT );
                        queryStr.append(" ");
                        queryStr.append("?g");
                        queryStr.append(i);
                        queryStr.append(" . ");
                    }
                    queryStr.append(" } ");
                    /*queryStr.append("INSERT {");
                    for (int i = addIdx; i < top; i++) {
                        final String subject = geoms.get(i).s;
                        queryStr.appendIri(subject);
                        queryStr.append(" ");
                        queryStr.appendIri(HAS_GEOMETRY);
                        queryStr.append(" ");
                        queryStr.appendIri(subject+"_geom");
                        queryStr.append(" . ");
                        queryStr.appendIri( subject+"_geom" );
                        queryStr.append(" ");
                        queryStr.appendIri( WKT );
                        queryStr.append(" ");
                        queryStr.appendLiteral(geoms.get(i).g + "^^<http://www.opengis.net/ont/geosparql#wktLiteral>");
                        queryStr.append(" . ");
                    }
                    queryStr.append(" } ");*/
                    queryStr.append("WHERE {");
                    for (int i = addIdx; i < top; i++) {
                        final String subject = geoms.get(i).s;
                        queryStr.appendIri(subject);
                        queryStr.append(" ");
                        queryStr.appendIri(HAS_GEOMETRY);
                        queryStr.append(" ");
                        queryStr.appendIri(subject+"_geom");
                        queryStr.append(" . ");
                        queryStr.appendIri( subject+"_geom" );
                        queryStr.append(" ");
                        queryStr.appendIri( WKT );
                        queryStr.append(" ");
                        queryStr.append("?g");
                        queryStr.append(i);
                        queryStr.append(" . ");
                    }
                    queryStr.append("}");
                    System.out.println("Print "+queryStr.toString());

                    UpdateRequest q = queryStr.asUpdate();
                    HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                    UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, endpointT, authenticator);
                    insertRemoteB.execute();
                    
                    //System.out.println("Add at "+addIdx+" Size "+cSize);
                    addIdx += (cSize-addIdx);
                    sizeUp *= 2;
                    //cSize += sizeUp;
                    cSize += 4;
                    if (cSize >= geoms.size()) {
                        cSize = geoms.size();
                    }
                    if (cSize == addIdx)
                        updating = false;
                } catch (org.apache.jena.atlas.web.HttpException ex) {
                    System.out.println("Failed at "+addIdx+" Size "+cSize);
                    //System.out.println("Crazy Stuff");
                    System.out.println(ex.getLocalizedMessage());
                    ex.printStackTrace();
                    ex.printStackTrace(System.out);
                    sizeUp = 1;
                    cSize = addIdx;
                    cSize += sizeUp;
                    if (cSize >= geoms.size())
                        cSize = geoms.size();
                    
                    //break;
                    //System.out.println("Going back at "+addIdx+" Size "+cSize);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println(ex.getLocalizedMessage());
                    break;
                }
            }
            
            /*
            updating = true;
            addIdx = 0;
            cSize = 1;
            sizeUp = 1;
            while (updating) {
                try {
                    ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                    //queryStr.append("WITH <"+fusedGraph+"> ");
                    queryStr.append("INSERT DATA { ");
                    queryStr.append("GRAPH <"+fusedGraph+"> { ");
                    int top = 0;
                    if (cSize >= geoms.size())
                        top = geoms.size();
                    else
                        top = cSize;
                    for (int i = addIdx; i < top; i++) {
                        final String subject = geoms.get(i).s;
                        queryStr.appendIri(subject);
                        queryStr.append(" ");
                        queryStr.appendIri(HAS_GEOMETRY);
                        queryStr.append(" ");
                        queryStr.appendIri(subject+"_geom");
                        queryStr.append(" . ");
                        queryStr.appendIri( subject+"_geom" );
                        queryStr.append(" ");
                        queryStr.appendIri( WKT );
                        queryStr.append(" ");
                        queryStr.appendLiteral(geoms.get(i).g + "^^<http://www.openlinksw.com/schemas/virtrdf#Geometry>");
                        queryStr.append(" . ");
                    }
                    queryStr.append("} }");
                    System.out.println("Print "+queryStr.toString());
                    
                    UpdateRequest q = queryStr.asUpdate();
                    HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                    UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, endpointT, authenticator);
                    insertRemoteB.execute();
                    //System.out.println("Add at "+addIdx+" Size "+cSize);
                    addIdx += (cSize-addIdx);
                    sizeUp *= 2;
                    cSize += sizeUp;
                    if (cSize >= geoms.size()) {
                        cSize = geoms.size();
                    }
                    if (cSize == addIdx)
                        updating = false;
                } catch (org.apache.jena.atlas.web.HttpException ex) {
                    System.out.println("There is no SPARQL UPDATE permission on SPARQL user most probably! ");
                    System.out.println("Failed at "+addIdx+" Size "+cSize);
                    System.out.println("Crazy Stuff");
                    System.out.println(ex.getLocalizedMessage());
                    ex.printStackTrace();
                    ex.printStackTrace(System.out);
                    sizeUp = 1;
                    cSize = addIdx;
                    cSize += sizeUp;
                    if (cSize >= geoms.size())
                        cSize = geoms.size();
                    //System.out.println("Going back at "+addIdx+" Size "+cSize);
                    
                    break;
                    //System.out.println("Going back at "+addIdx+" Size "+cSize);
                } catch (Exception ex) {
                    System.out.println(ex.getLocalizedMessage());
                    break;
                }
            }*/
    }
}
