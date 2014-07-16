
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.hp.hpl.jena.graph.GraphUtil;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import virtuoso.jdbc4.VirtuosoCallableStatement;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoPreparedStatement;
import virtuoso.jdbc4.VirtuosoResultSet;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

/**
 * Forms appropriate triples from the PostGIS database and inserts them in the virtuoso specified graph.
 * 
 */

//sudo virtuoso-t -f +configfile /usr/local/var/lib/virtuoso/db/virtuoso.ini

public final class VirtuosoImporter {
    
    private static final Logger LOG = Logger.getLogger(VirtuosoImporter.class);
    private static final String DB_URL = "jdbc:postgresql:";
    private static final String WKT = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String HAS_GEOMETRY = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String LAT = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    private static final String LON = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private static final String SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";
    
    private static final String links_graph = "http://localhost:8890/DAV/links";
    private static final String del_wgs_graph = "http://localhost:8890/DAV/del_wgs";
    private static final String del_geom_graph = "http://localhost:8890/DAV/del_geom";
    private static final String clearBulkLoadTbl = "DELETE FROM DB.DBA.load_list";
    
    private final String graphB; 
    private final String graphA;
    private String bulkInsertDir;
    
    public final TripleHandler trh;
    
    private final Connection connection; // Connection to PostGIS
    private final Connection virt_conn; // Connection to Virtuoso
    
    private final String transformationID;
    private final String endpointA;
    private final String endpointB;
    private final String endpointLoc;
    public final VirtGraph set;
    //private final VirtGraph setA;     
    //private final VirtGraph setB;     
    
    public static long getUnsignedInt(int x) {
        return x & 0x00000000ffffffffL;
    }
    
    public VirtuosoImporter(final DBConfig dbConfig, String transformationID, final String fusedGraph, final boolean checkboxIsSelected, final GraphConfig graphConfig) throws SQLException, IOException {

        final String dbName = dbConfig.getDBName();
        final String dbUsername = dbConfig.getDBUsername();
        final String dbPassword = dbConfig.getDBPassword();
        this.connection = DriverManager.getConnection(DB_URL + dbName, dbUsername, dbPassword);
        this.transformationID = transformationID;
        
        graphA = graphConfig.getGraphA();
        graphB = graphConfig.getGraphB();
        endpointA = graphConfig.getEndpointA();
        endpointB = graphConfig.getEndpointB();
        endpointLoc = graphConfig.getEndpointLoc();
        
        long startTime = System.nanoTime();
        set = getVirtuosoSet(fusedGraph, dbConfig.getDBURL(), dbConfig.getUsername(), dbConfig.getPassword());
        long endTime = System.nanoTime();
        //System.out.println("Time to connect : "+(endTime-startTime)/1000000000f);
        
        virt_conn = set.getConnection();
        
        bulkInsertDir = dbConfig.getBulkDir();
        
        trh = new FileBulkLoader(fusedGraph, dbConfig, graphConfig, set);
        trh.init();
    }
       
    //inserts the data in virtuoso
    public void importGeometriesToVirtuoso(final String fusedGraph) {   //imports the geometries in the fused graph                     
                              
        try{
            Statement stmt;
            String deleteQuery;
            String subject;
            String fusedGeometry;
            stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, 
                   ResultSet.CONCUR_READ_ONLY);                        
            
            //select from source dataset in postgis. The transformations will take place from A to B dataset.
            String selectFromPostGIS = "SELECT DISTINCT subject_a, subject_b, ST_AsText(geom) FROM fused_geometries";    
            final ResultSet rs = stmt.executeQuery(selectFromPostGIS);      
                 
            clearBulkLoadHistory();
            
            List<Triple> lst = new ArrayList<>();
            int p = 0;
            while(rs.next()) {
                subject = rs.getString("subject_a"); 
                fusedGeometry = rs.getString("ST_AsText");
                if (!(transformationID.equals("Keep both"))){ 
                    //if transformation is NOT "keep both" -> delete previous geometry
                    
                    trh.deleteAllWgs84(subject);
                    trh.deleteAllGeom(subject);
                    trh.addGeomTriple(subject, fusedGeometry);     
                    //System.out.println(subject + " " + fusedGeometry);
                }
                else {
                    if (rs.isFirst()){
                        trh.deleteAllGeom(subject);
                        trh.addGeomTriple(subject, fusedGeometry);
                    }
                    else {
                        //insert second geometry
                        trh.addGeomTriple(subject, fusedGeometry);                           
                    }
                }
            }  
            rs.close();
        }
        catch(SQLException ex ){
            //out.close();
            LOG.warn(ex.getMessage(), ex);
        }
        
    }
    
    //inserts the data in virtuoso
    public void importGeometriesToVirtuoso() { //imports the geometries in the graph A                       
        importGeometriesToVirtuoso(null);
    }
        
    public void insertLinksMetadataChains(List<Link> links, final String fusedGraph) throws SQLException, IOException{ //metadata go in the new fusedGraph. the method is called from FuserWorker 
    //keep metadata subjects according to the transformation                    //selects from graph B, inserts in fused  graph
        long starttime, endtime;
        createLinksGraph(links);
        createDelWGSGraph(((FileBulkLoader)trh).getDelWGSList());
        createDelGeomGraph(((FileBulkLoader)trh).getDelGeomList());
        String getFromB;
        String getFromA;
        
        starttime =  System.nanoTime();
        if (endpointLoc.equals(endpointA)) {
            getFromA = "SELECT ?s ?p ?o1 ?p4 ?o3 ?p5 ?o4\n"
                + "WHERE\n"
                + "{\n"
                + " GRAPH <http://localhost:8890/DAV/links> { ?s <http://www.w3.org/2002/07/owl#sameAs> _:a } .\n"
                + " GRAPH <"+graphA+"> { {?s ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3. } OPTIONAL { ?o3 ?p5 ?o4 .} }\n"
                + "\n"
                + "  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n"
                + "}";
        } else {
            getFromA = "SELECT ?s ?p ?o1 ?p4 ?o3 ?p5 ?o4\n"
                + "WHERE\n"
                + "{\n"
                + " SERVICE <"+endpointLoc+"> { GRAPH <http://localhost:8890/DAV/links> { ?s <http://www.w3.org/2002/07/owl#sameAs> _:a } } .\n"
                + " GRAPH <"+graphA+"> { {?s ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3. } OPTIONAL { ?o3 ?p5 ?o4 .} }\n"
                + "\n"
                + "  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n"
                + "}";
        }
        if (endpointLoc.equals(endpointB)) {
            getFromB = "SELECT ?s ?p ?o1 ?p4 ?o3 ?p5 ?o4\n"
                + "WHERE\n"
                + "{\n"
                + " GRAPH <http://localhost:8890/DAV/links> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } .\n"
                + " GRAPH <"+graphB+"> { {?o ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3. } OPTIONAL { ?o3 ?p5 ?o4 .} }\n"
                + "\n"
                + "  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n"
                + "}";
        } else {
            getFromB = "SELECT ?s ?p ?o1 ?p4 ?o3 ?p5 ?o4\n"
                + "WHERE\n"
                + "{\n"
                + " SERVICE <"+endpointLoc+"> { GRAPH <http://localhost:8890/DAV/links> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } }\n"
                + " GRAPH <"+graphB+"> { {?o ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3. } OPTIONAL { ?o3 ?p5 ?o4 .} }\n"
                + "\n"
                + "  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n"
                + "}";
        }
        QueryExecution selectFromA = QueryExecutionFactory.sparqlService(endpointA, getFromA);  
        QueryExecution selectFromB = QueryExecutionFactory.sparqlService(endpointB, getFromB); 
        
        final com.hp.hpl.jena.query.ResultSet resultSetFromA = selectFromA.execSelect();
        final com.hp.hpl.jena.query.ResultSet resultSetFromB = selectFromB.execSelect();
        
        endtime =  System.nanoTime();
        //LOG.info("Metadata parsed in "+(endtime-starttime)/1000000000f);
        
        while(resultSetFromA.hasNext()) {
            final QuerySolution querySolution = resultSetFromA.next();
            //final String predicate = querySolution.getResource("?p").getURI();
            RDFNode s, p, o1, p4, o3, p5, o4;
            s = querySolution.get("?s");
            p = querySolution.get("?p");
            o1 = querySolution.get("?o1");
            p4 = querySolution.get("?p4");
            o3 = querySolution.get("?o3");
            p5 = querySolution.get("?p5");
            o4 = querySolution.get("?o4");
            
            if (p != null) {
                trh.addTriple(s.asNode(), p.asNode(), o1.asNode());
            }
            if (p4 != null) {
                trh.addTriple(o1.asNode(), p4.asNode(), o3.asNode());
            }
            if (p5 != null) {
                trh.addTriple(o3.asNode(), p5.asNode(), o4.asNode());
            }
        }
        
        while(resultSetFromB.hasNext()) {
            final QuerySolution querySolution = resultSetFromB.next();
            //final String predicate = querySolution.getResource("?p").getURI();
            RDFNode s, p, o1, p4, o3, p5, o4;
            s = querySolution.get("?s");
            p = querySolution.get("?p");
            o1 = querySolution.get("?o1");
            p4 = querySolution.get("?p4");
            o3 = querySolution.get("?o3");
            p5 = querySolution.get("?p5");
            o4 = querySolution.get("?o4");
            
            if (p != null) {
                trh.addTriple(s.asNode(), p.asNode(), o1.asNode());
            }
            if (p4 != null) {
                trh.addTriple(o1.asNode(), p4.asNode(), o3.asNode());
            }
            if (p5 != null) {
                trh.addTriple(o3.asNode(), p5.asNode(), o4.asNode());
            }
        }
        
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Metadata parsed in "+((endtime-starttime)/1000000000f) +""+ANSI_RESET);
    }
    
    public void insertLinksMetadata(List<Link> links, final String fusedGraph) throws SQLException{ //metadata go in the new fusedGraph. the method is called from FuserWorker 
    
    }
    
    public void insertLinksMetadata(List<Link> links) throws SQLException, IOException{ //metadata go in the graphA, not the new one. the method is called without the fusedGraph from FuserWorker
    //keep metadata subjects according to the transformation    
        long starttime, endtime;
        createLinksGraph(links);
        String getFromB;
        createDelWGSGraph(((FileBulkLoader)trh).getDelWGSList());
        createDelGeomGraph(((FileBulkLoader)trh).getDelGeomList());
        
        starttime = System.nanoTime();
        
        if (endpointLoc.equals(endpointB)) {
            getFromB = "SELECT ?s ?p ?o1 ?p4 ?o3 ?p5 ?o4\n"
                + "WHERE\n"
                + "{\n"
                + " GRAPH <http://localhost:8890/DAV/links> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } .\n"
                + " GRAPH <"+graphB+"> { {?o ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3. } OPTIONAL { ?o3 ?p5 ?o4 .} }\n"
                + "\n"
                + "  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n"
                + "}";
        } else {
            getFromB = "SELECT ?s ?p ?o1 ?p4 ?o3 ?p5 ?o4\n"
                + "WHERE\n"
                + "{\n"
                + " SERVICE <"+endpointLoc+"> { GRAPH <http://localhost:8890/DAV/links> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } }\n"
                + " GRAPH <"+graphB+"> { {?o ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3. } OPTIONAL { ?o3 ?p5 ?o4 .} }\n"
                + "\n"
                + "  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n"
                + "  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n"
                + "}";
        }
        
        QueryExecution selectFromB = QueryExecutionFactory.sparqlService(endpointB, getFromB); 
        
        final com.hp.hpl.jena.query.ResultSet resultSetFromB = selectFromB.execSelect();
                
        while(resultSetFromB.hasNext()) {
            final QuerySolution querySolution = resultSetFromB.next();
            //final String predicate = querySolution.getResource("?p").getURI();
            RDFNode s, p, o1, p4, o3, p5, o4;
            s = querySolution.get("?s");
            p = querySolution.get("?p");
            o1 = querySolution.get("?o1");
            p4 = querySolution.get("?p4");
            o3 = querySolution.get("?o3");
            p5 = querySolution.get("?p5");
            o4 = querySolution.get("?o4");
            
            if (p != null) {
                //System.out.println(querySolution.get("?p").asNode()+" "+querySolution.get("?o1").asNode()+" "+querySolution.get("?o1").asNode().isURI());
                trh.addTriple(s.asNode(), p.asNode(), o1.asNode());
            }
            if (p4 != null) {
                //System.out.println(s.asNode()+" "+o1.asNode()+" "+p4.asNode()+" "+o3.asNode());
                trh.addTriple(o1.asNode(), p4.asNode(), o3.asNode());
            }
            if (p5 != null) {
                //System.out.println(querySolution.get("?p1")+" "+querySolution.get("?o2")+" "+querySolution.get("?o2"));
                trh.addTriple(o3.asNode(), p5.asNode(), o4.asNode());
            }
        }
        
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Metadata parsed in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
    }
    
    
    private void executeVirtuosoUpdate(String updateQuery, VirtGraph set) {                
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(updateQuery, set);
        
        vur.exec();
    }
    
    //query for deleting specific subject with its additional blank node. 
    private String formDeleteGeomQuery(String subject, String graph){      
        return "WITH <" + graph + "> DELETE { <" + subject + "> <" + HAS_GEOMETRY + "> ?a . ?a <" + WKT + "> ?o2 } WHERE { <" + subject + "> <" + HAS_GEOMETRY + "> ?a . ?a <" + WKT + "> ?o2 }";   
    }
    
    //query for inserting a triple set into a virtuoso graph
    private String formInsertQuery(String subject, String fusedGeometry, String graph) { 
        return "INSERT INTO <" + graph + "> { <" + subject + "> <" + HAS_GEOMETRY + "> _:a . _:a <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> }";
    }
    
    private String formInsertQuery(String subject, String predicate, String object, String graph){
              return "WITH <" + graph + "> INSERT { <" + subject +"> <" + predicate +"> " + object +" }";
              //keep left, average of two points 
    }
     
    private String getNextGraphSequence(String fusedGraph) {
        final String nextSequence = "WITH <"+fusedGraph+"> SELECT (<bif:sequence_next> (\"<"+fusedGraph+">\")) as ?id WHERE {}";
        VirtuosoQueryExecution nextSeqQuery = VirtuosoQueryExecutionFactory.create(nextSequence, set);
        final com.hp.hpl.jena.query.ResultSet nextSeqSet = nextSeqQuery.execSelect();
                    
        String id = "";
        while ( nextSeqSet.hasNext() ) {
            final QuerySolution nextSeqSol = nextSeqSet.next();
                        
            id = nextSeqSol.getLiteral("?id").getString();
        }
        
        return id;
    }
    private String formDeleteWgs84query(String subject, String graph){
        return "WITH <" + graph + "> DELETE { <" + subject + "> <" + LAT + "> ?o1 . <" + subject + "> <" + LON + "> ?o2 } WHERE { <" + subject + "> <" + LAT + "> ?o1 . <" + subject + "> <" + LON + "> ?o2 }";
    }
    
    private String formGeometryTriplets(String id, String subject, String fusedGeometry) {
        return "<"+subject+">"+" <"+HAS_GEOMETRY+">"+" <http://geometry"+id+"_"+getUnsignedInt(subject.hashCode())+"> .\n"
                  + "<http://geometry"+id+"_"+getUnsignedInt(subject.hashCode())+"> <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> .";      
    }
    
    private VirtGraph getVirtuosoSet (String graph, String url, String username, String password) throws SQLException {
        //Class.forName("virtuoso.jdbc4.Driver");
        VirtGraph vSet = new VirtGraph (graph, "jdbc:virtuoso://" + url + "/CHARSET=UTF-8", username, password);
        LOG.info(ANSI_YELLOW+"Virtuoso connection established."+ANSI_RESET);
        return vSet;
    }
    
    public void clean() {
        set.close();        
        LOG.info(ANSI_YELLOW+"Virtuoso import is done."+ANSI_RESET);

    }
    
    public void insertMetadataToFusedGraph(List<Link> links, String fusedGraph) {
        for(Link link : links) {
            String subject = link.getNodeA();
            Node s = NodeFactory.createURI(subject);
            
            String selectLinkMetadata = "SELECT ?p0 ?o1 ?p1 ?o2 ?p2 ?o3 ?p3 ?o4 WHERE {"
                    + "<" + subject + "> ?p0 ?o1 ."
                    + "OPTIONAL { ?o1 ?p1 ?o2 .}"
                    + "OPTIONAL { ?o2 ?p2 ?o3 .}"
                    + "OPTIONAL { ?o3 ?p3 ?o4 .}"
                    + "FILTER(!REGEX(?p0, \"" + HAS_GEOMETRY + "\", \"i\"))"
                    + "FILTER(!REGEX(?p1, \"" + WKT + "\", \"i\"))"
                    + "FILTER(!REGEX(?p0, \"" + LAT + "\", \"i\"))" //probably not needed, geometries are already fused in geosparql
                    + "FILTER(!REGEX(?p0, \"" + LON + "\", \"i\"))}";//probably not needed
            //System.out.println(selectLinkMetadata);
            QueryExecution queryExecution = null;
            try {
                
                final Query query = QueryFactory.create(selectLinkMetadata);
                queryExecution = QueryExecutionFactory.sparqlService(endpointA, query, graphA); //selects from graphA (unister)
                final com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();
                
                while(resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();
                    
                    //check how deep is the chain of triples, and insert accordingly                    
                    Node o1,o2,o3,o4, p0, p1, p2, p3;
                    if (querySolution.get("?o1") != null && querySolution.get("?p0") != null){
                        if (querySolution.get("?o2") != null && querySolution.get("?p1") != null){
                            //System.out.println("Level 2");
                        }
                        //System.out.println("Level 1");
                        //add sub p0 o1
                        p0 = querySolution.get("?p0").asNode();
                        o1 = querySolution.get("?o1").asNode();
                                       
                        //trh.addTriple(s, p0, o1);
                    }
                    /*if (querySolution.get("?o1") != null && querySolution.get("?p0") != null){
                        if (querySolution.get("?o2") != null && querySolution.get("?p1") != null){
                               if (querySolution.get("?o3") != null && querySolution.get("?p2") != null){
                                   if (querySolution.get("?o4") != null && querySolution.get("?p3") != null){
                                       //add triple set from sub to o4

                                        p0 = querySolution.getResource("?p0").getURI();
                                        o1 = querySolution.get("?o1");
                                        ob1 = constructObjectNode(o1);
                                       
                                        s1 = querySolution.get("?o1").asResource().getURI();
                                        p1 = querySolution.getResource("?p1").getURI();
                                        o2 = querySolution.get("?o2");
                                        ob2 = constructObjectNode(o2);
                                       
                                        s2 = querySolution.get("?o2").asResource().getURI();
                                        p2 = querySolution.getResource("?p2").getURI();
                                        o3 = querySolution.get("?o3");
                                        ob3 = constructObjectNode(o3);
                                       
                                        s3 = querySolution.get("?o3").asResource().getURI();
                                        p3 = querySolution.getResource("?p3").getURI();
                                        o4 = querySolution.get("?o4");
                                        ob4 = constructObjectNode(o4);
                                        
                                        String insertPattern = "<" + subject + "> <" + p0 + "> " + ob1 + " . <" + s1 + "> <" + p1 + "> " + ob2 + " .  <" + s2 + "> <" + p2 + "> " + ob3 + " . <" + s3 + "> <" + p3 + "> " + ob4 + " .";
                                        String insertQuery = "WITH <" + fusedGraph + "> INSERT { " + insertPattern + " }";
                                        executeVirtuosoUpdate(insertQuery, set);
                                        
                                   }
                                   //add set from sub to o3
                                    p0 = querySolution.getResource("?p0").getURI();
                                    o1 = querySolution.get("?o1");
                                    ob1 = constructObjectNode(o1);

                                    s1 = querySolution.get("?o1").asResource().getURI();
                                    p1 = querySolution.getResource("?p1").getURI();
                                    o2 = querySolution.get("?o2");
                                    ob2 = constructObjectNode(o2);

                                    s2 = querySolution.get("?o2").asResource().getURI();
                                    p2 = querySolution.getResource("?p2").getURI();
                                    o3 = querySolution.get("?o3");
                                    ob3 = constructObjectNode(o3);

                                    String insertPattern = "<" + subject + "> <" + p0 + "> " + ob1 + " . <" + s1 + "> <" + p1 + "> " + ob2 + " .  <" + s2 + "> <" + p2 + "> " + ob3 + " . ";
                                    String insertQuery = "WITH <" + fusedGraph + "> INSERT { " + insertPattern + " }";
                                    executeVirtuosoUpdate(insertQuery, set);
                                   
                                   
                               }
                               //add triple set from sub to o2
                                p0 = querySolution.getResource("?p0").getURI();
                                o1 = querySolution.get("?o1");
                                ob1 = constructObjectNode(o1);

                                s1 = querySolution.get("?o1").asResource().getURI();
                                p1 = querySolution.getResource("?p1").getURI();
                                o2 = querySolution.get("?o2");
                                ob2 = constructObjectNode(o2);
                                
                                String insertPattern = "<" + subject + "> <" + p0 + "> " + ob1 + " . <" + s1 + "> <" + p1 + "> " + ob2 + " . ";
                                String insertQuery = "WITH <" + fusedGraph + "> INSERT { " + insertPattern + " }";
                                executeVirtuosoUpdate(insertQuery, set);                              
                               
                        }
                        //add sub p0 o1
                        p0 = querySolution.getResource("?p0").getURI();
                        o1 = querySolution.get("?o1");
                        ob1 = constructObjectNode(o1);
                        
                        String insertPattern = "<" + subject + "> <" + p0 + "> " + ob1 + " . ";
                        String insertQuery = "WITH <" + fusedGraph + "> INSERT { " + insertPattern + " }";
                        executeVirtuosoUpdate(insertQuery, set);                      
                        
                    }    */       
                    
                }//end while resultset.hasNext
                            
                
            }//end of try
            catch (RuntimeException ex) {
                LOG.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }//end of catch
            finally {
                 if(queryExecution != null) {
                     queryExecution.close();
                }//end of if
            }//end of finally           
        }//end for Links             
    }//end of method
    
    private String constructObjectNode(RDFNode objectNode){
        String object;
                if(objectNode.isResource()) {
                if(objectNode.isAnon()) {

                    object = objectNode.asLiteral().getLexicalForm();                            
                    object =  "<" + object + ">";
                }
                else {

                    object = objectNode.asResource().getURI();
                    object = "<" + object + ">";
                }
        }
        else {

            //object = objectNode.toString();
            object = objectNode.asLiteral().getLexicalForm().toString();
            //RDFDatatype datatype = objectNode.asLiteral().getDatatype();
            //Literal litObj = objectNode.asLiteral();
            object = object.replaceAll("\"", ""); //sorry for this
            object = "\"" + object + "\""; // + datatype;
        }        
        return object;        
    }        

    private void clearBulkLoadHistory() throws SQLException {
        PreparedStatement clearBulkLoadTblStmt;
        clearBulkLoadTblStmt = virt_conn.prepareStatement(clearBulkLoadTbl);                        
        clearBulkLoadTblStmt.executeUpdate();
    }
    
    private void createLinksGraph(List<Link> lst) throws SQLException, IOException {
        final String dropGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/links>";
        final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/links>";
    
        LOG.info(ANSI_YELLOW+"Loaded "+lst.size()+" links"+ANSI_RESET);
        PreparedStatement dropStmt;
        long starttime, endtime;
        dropStmt = virt_conn.prepareStatement(dropGraph);
        dropStmt.execute();
        
        PreparedStatement createStmt;
        createStmt = virt_conn.prepareStatement(createGraph);
        createStmt.execute();
        starttime = System.nanoTime();
        File f = new File(bulkInsertDir+"bulk_inserts/selected_links.nt");
        //f.mkdirs();
        //f.getParentFile().mkdirs();
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+bulkInsertDir+"bulk_inserts/selected_links.nt'), '', "+"'"+links_graph+"')";

        if ( lst.size() > 0 ) {

            for(Link link : lst) {
                String triple = "<"+link.getNodeA()+"> <"+SAME_AS+"> <"+link.getNodeB()+"> .";
        
                out.println(triple);
            }
            out.close();
            
            PreparedStatement uploadBulkFileStmt;
            uploadBulkFileStmt = virt_conn.prepareStatement(bulk_insert);                        
            uploadBulkFileStmt.executeUpdate();
        }
        
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Links Graph created in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
    }
    
    private void createDelWGSGraph(List<String> lst) throws SQLException, IOException {
        final String dropGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/del_wgs>";
        final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/del_wgs>";
    
        PreparedStatement dropStmt;
        long starttime, endtime;
        dropStmt = virt_conn.prepareStatement(dropGraph);
        dropStmt.execute();
        
        PreparedStatement createStmt;
        createStmt = virt_conn.prepareStatement(createGraph);
        createStmt.execute();
        starttime = System.nanoTime();
        File f = new File(bulkInsertDir+"bulk_inserts/deleted_wgs.nt");
        //f.getParentFile().mkdirs();
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+bulkInsertDir+"bulk_inserts/deleted_wgs.nt'), '', "+"'"+del_wgs_graph+"')";

        if ( lst.size() > 0 ) {

            for(String sub : lst) {
                String triple = "<"+sub+"> <del> <a> .";
        
                out.println(triple);
            }
            out.close();
            
            PreparedStatement uploadBulkFileStmt;
            uploadBulkFileStmt = virt_conn.prepareStatement(bulk_insert);                        
            uploadBulkFileStmt.executeUpdate();
        }
        
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Delete WGS Graph created in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
    }
    
    private void createDelGeomGraph(List<String> lst) throws IOException, SQLException {
        final String dropGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/del_geom>";
        final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/del_geom>";
    
        PreparedStatement dropStmt;
        long starttime, endtime;
        dropStmt = virt_conn.prepareStatement(dropGraph);
        dropStmt.execute();
        
        PreparedStatement createStmt;
        createStmt = virt_conn.prepareStatement(createGraph);
        createStmt.execute();
        starttime = System.nanoTime();
        File f = new File(bulkInsertDir+"bulk_inserts/deleted_geom.nt");
        //f.getParentFile().mkdirs();
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('"+bulkInsertDir+"bulk_inserts/deleted_geom.nt'), '', "+"'"+del_geom_graph+"')";

        if ( lst.size() > 0 ) {

            for(String sub : lst) {
                String triple = "<"+sub+"> <del> <"+sub+"_geom> .";
        
                out.println(triple);
            }
            out.close();
            
            PreparedStatement uploadBulkFileStmt;
            uploadBulkFileStmt = virt_conn.prepareStatement(bulk_insert);                        
            uploadBulkFileStmt.executeUpdate();
        }
        
        endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW+"Delete Geom Graph created in "+((endtime-starttime)/1000000000f)+""+ANSI_RESET);
    }
}    