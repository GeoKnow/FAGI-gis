
package gr.athenainnovation.imis.fusion.gis.virtuoso;

//import com.google.common.base.Optional;
//import com.hp.hpl.jena.graph.Graph;
//import com.hp.hpl.jena.graph.Node;
//import com.hp.hpl.jena.graph.Triple;
//import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
//import com.hp.hpl.jena.rdf.model.Literal;
//import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import java.sql.Statement;
import java.sql.ResultSet;
//import com.hp.hpl.jena.query.ResultSet;
import org.apache.log4j.Logger;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
//import gr.athenainnovation.imis.fusion.gis.gui.workers.Dataset;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;
//for postGIS
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
//import java.util.Iterator;
import java.util.List;
//import virtuoso.jena.driver.VirtuosoQueryExecution;
//import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
//import java.util.List;


/**
 * Forms appropriate triples from the PostGIS database and inserts them in the virtuoso specified graph.
 * 
 */
public final class VirtuosoImporter {
    
    private static final Logger LOG = Logger.getLogger(VirtuosoImporter.class);
    //private final List<Link> links;
    private static final String DB_URL = "jdbc:postgresql:";
    private static final String WKT = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String HAS_GEOMETRY = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String LAT = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    private static final String LON = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private final String graphB;//  
    private final  String graphA;

    //private final GraphConfig graphConfig;
    private final Connection connection;
    private final String transformationID;
    private final String endpointA;
    private final String endpointB;
    private final VirtGraph set;
    //private ImporterPanel importerPanel;
    private final boolean checkboxIsSelected;
    
      
            
    public VirtuosoImporter(final DBConfig dbConfig, String transformationID, final String fusedGraph, final boolean checkboxIsSelected, final GraphConfig graphConfig) throws SQLException {
        //this.dbConfig = dbConfig;
        //this.links = links;
        final String dbName = dbConfig.getDBName();
        final String dbUsername = dbConfig.getDBUsername();
        final String dbPassword = dbConfig.getDBPassword();
        this.connection = DriverManager.getConnection(DB_URL + dbName, dbUsername, dbPassword);
        this.transformationID = transformationID;
        this.checkboxIsSelected = checkboxIsSelected;
        //this.graphConfig = graphConfig;
        
        graphA = graphConfig.getGraphA();
        graphB = graphConfig.getGraphB();
        endpointA = graphConfig.getEndpointA();
        endpointB = graphConfig.getEndpointB();
        set = getVirtuosoSet(dbConfig.getDBURL(), dbConfig.getUsername(), dbConfig.getPassword());

    }
              
    //inserts the data in virtuoso
    public void importGeometriesToVirtuoso(final String fusedGraph) {   //imports the geometries in the fused graph                     
                              
        try{
            Statement stmt;
            String deleteQuery;
            String subject;
            stmt = connection.createStatement();                        
            //clearGraphFirst(set);
            
            //select from source dataset in postgis. The transformations will take place from A to B dataset.
            String selectFromPostGIS = "SELECT DISTINCT subject_a, subject_b, ST_AsText(geom) FROM fused_geometries";    
            final ResultSet rs = stmt.executeQuery(selectFromPostGIS);      
                        
            while(rs.next()){                            
                if (!(transformationID.equals("Keep both"))){ 
                    
                    //if transformation is "keep both" -> don't delete previous geometry
                    String deleteWgs84 = formDeleteWgs84query(rs.getString("subject_a"), fusedGraph);
                    executeVirtuosoUpdate(deleteWgs84, set);
                    
                    deleteQuery = formDeleteGeomQuery(rs.getString("subject_a"), fusedGraph); //delete subject_a if it exists from previous transformations
                    executeVirtuosoUpdate(deleteQuery, set);
                               
                    subject = rs.getString("subject_a");               
                    String fusedGeometry = rs.getString("ST_AsText");    
                    String updateQuery = formInsertQuery(subject, fusedGeometry, fusedGraph);
                    executeVirtuosoUpdate(updateQuery, set);   
                }
                else {
                        if (rs.isFirst()){
                            deleteQuery = formDeleteGeomQuery(rs.getString("subject_a"), fusedGraph); //delete subject_a if it exists from previous transformations
                            executeVirtuosoUpdate(deleteQuery, set);
                            subject = rs.getString("subject_a");               
                            String fusedGeometry = rs.getString("ST_AsText");    
                            String updateQuery = formInsertQuery(subject, fusedGeometry, fusedGraph);
                            executeVirtuosoUpdate(updateQuery, set); 
                            //delete previous geometry here and prevent the delete action, 
                            //if we have inserted new geometry from the keep both transformation           
                        }
                        else {
                            
                            //insert second geometry
                            subject = rs.getString("subject_a");               
                            String fusedGeometry = rs.getString("ST_AsText");    
                            String updateQuery = formInsertQuery(subject, fusedGeometry, fusedGraph);
                            executeVirtuosoUpdate(updateQuery, set);                            
                        }
                    }
            }                       
    
            
        }
        catch(SQLException ex ){
            LOG.warn(ex.getMessage(), ex);
        }
    }
    
    //inserts the data in virtuoso
    public void importGeometriesToVirtuoso() { //imports the geometries in the graph A                       
                              
        try{
            Statement stmt;
            String deleteQuery;
            String subject;
            stmt = connection.createStatement();                        
            //clearGraphFirst(set);
            
            //select from source dataset in postgis. The transformations will take place from A to B dataset.
            String selectFromPostGIS = "SELECT DISTINCT subject_a, subject_b, ST_AsText(geom) FROM fused_geometries";    
            final ResultSet rs = stmt.executeQuery(selectFromPostGIS);      
                        
            while(rs.next()){                            
                if (!(transformationID.equals("Keep both"))){ 
                    
                    //if transformation is "keep both" -> don't delete previous geometry
                    String deleteWgs84 = formDeleteWgs84query(rs.getString("subject_a"), graphA);
                    executeVirtuosoUpdate(deleteWgs84, set);
                    
                    deleteQuery = formDeleteGeomQuery(rs.getString("subject_a"), graphA); //delete subject_b if it exists from previous transformations
                    executeVirtuosoUpdate(deleteQuery, set);
                               
                    subject = rs.getString("subject_a");               
                    String fusedGeometry = rs.getString("ST_AsText");    
                    String updateQuery = formInsertQuery(subject, fusedGeometry, graphA);
                    executeVirtuosoUpdate(updateQuery, set);   
                }
                else {
                        if (rs.isFirst()){
                            String deleteWgs84 = formDeleteWgs84query(rs.getString("subject_a"), graphA);
                            executeVirtuosoUpdate(deleteWgs84, set);
                            deleteQuery = formDeleteGeomQuery(rs.getString("subject_a"), graphA); //delete subject_a if it exists from previous transformations
                            executeVirtuosoUpdate(deleteQuery, set);
                            subject = rs.getString("subject_a");               
                            String fusedGeometry = rs.getString("ST_AsText");    
                            String updateQuery = formInsertQuery(subject, fusedGeometry, graphA);
                            executeVirtuosoUpdate(updateQuery, set); 
                            //delete previous geometry here and prevent the delete action, 
                            //if we have inserted new geometry from the keep both transformation           
                        }
                        else {
                            //insert second geometry
                            subject = rs.getString("subject_a");               
                            String fusedGeometry = rs.getString("ST_AsText");    
                            String updateQuery = formInsertQuery(subject, fusedGeometry, graphA);
                            executeVirtuosoUpdate(updateQuery, set);                            
                        }
                    }
            }                       
    
            
        }
        catch(SQLException ex ){
            LOG.warn(ex.getMessage(), ex);
        }
    }
    

    
    
    public void insertLinksMetadata(List<Link> links, final String fusedGraph){ //metadata go in the new fusedGraph. the method is called from FuserWorker 
    //keep metadata subjects according to the transformation                    //selects from graph B, inserts in fused  graph
        for(Link link : links) {          
            
            //String selectSpecificLinkQuery = "SELECT ?s ?p ?o WHERE { ?s ?p ?o "
                 //   + "FILTER(?s IN (<" + link.getNodeA() + ">, <" + link.getNodeB() + ">)) FILTER(!regex(?p,\"" + HAS_GEOMETRY + "\",\"i\")) FILTER(!regex(?p, \"" +  WKT +"\", \"i\")) FILTER(!regex(?p, \"" +  LAT +"\", \"i\")) FILTER(!regex(?p, \"" + LON + "\", \"i\"))}";
            
            //new select without nodeA
            String selectSpecificLinkQuery = "SELECT ?p ?o WHERE { <" + link.getNodeB() + "> ?p ?o "
                    + "FILTER(!regex(?p,\"" + HAS_GEOMETRY + "\",\"i\")) FILTER(!regex(?p, \"" +  WKT +"\", \"i\")) FILTER(!regex(?p, \"" +  LAT +"\", \"i\")) FILTER(!regex(?p, \"" + LON + "\", \"i\"))}";
            
            //set of triples metadata query
            //String selectSpecificLinkQuery = "SELECT ?p ?o WHERE { <" + link.getNodeB() + "> ?p1 ?o . ?o ?p2 ?o2 "
                //    + "FILTER(!regex(?p1,\"" + HAS_GEOMETRY + "\",\"i\")) FILTER(!regex(?p2, \"" +  WKT +"\", \"i\")) }"; //FILTER(!regex(?p1, \"" +  LAT +"\", \"i\")) FILTER(!regex(?p2, \"" + LON + "\", \"i\")) these filters not needed in geosparql
            

            String subjectA = link.getNodeA();
            QueryExecution queryExecution = null;
            try {
           
                final Query query = QueryFactory.create(selectSpecificLinkQuery);
                queryExecution = QueryExecutionFactory.sparqlService(endpointA, query, graphB);             
                
                final com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();

                while(resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();
                    final String predicate = querySolution.getResource("?p").getURI();
                    String object;
                    final RDFNode objectNode = querySolution.get("?o");
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
                        //object1 = objectNode.asLiteral().getLexicalForm();
                        object = objectNode.toString(); 
                        object = "\"" + object + "\"";
                    }
                    
                    String insertMetadata = formInsertQuery(subjectA, predicate, object, fusedGraph);
                    executeVirtuosoUpdate(insertMetadata, set);
                                                            
                }                                      
            }
            catch (RuntimeException ex) {
                LOG.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
            finally {
                 if(queryExecution != null) {
                     queryExecution.close();
                }
            }
            
         }
    }
    
    
    public void insertLinksMetadata(List<Link> links){ //metadata go in the graphA, not the new one. the method is called without the fusedGraph from FuserWorker
    //keep metadata subjects according to the transformation    
        for(Link link : links) {
            
            
            //String selectSpecificLinkQuery = "SELECT ?s ?p ?o WHERE { ?s ?p ?o "
                 //   + "FILTER(?s IN (<" + link.getNodeA() + ">, <" + link.getNodeB() + ">)) FILTER(!regex(?p,\"" + HAS_GEOMETRY + "\",\"i\")) FILTER(!regex(?p, \"" +  WKT +"\", \"i\")) FILTER(!regex(?p, \"" +  LAT +"\", \"i\")) FILTER(!regex(?p, \"" + LON + "\", \"i\"))}";
            
            //new select without nodeA
            String selectSpecificLinkQuery = "SELECT ?p ?o WHERE { <" + link.getNodeB() + "> ?p ?o "
                    + "FILTER(!regex(?p,\"" + HAS_GEOMETRY + "\",\"i\")) FILTER(!regex(?p, \"" +  WKT +"\", \"i\")) FILTER(!regex(?p, \"" +  LAT +"\", \"i\")) FILTER(!regex(?p, \"" + LON + "\", \"i\"))}";
            
            //set of triples metadata query
            //String selectSpecificLinkQuery = "SELECT ?p ?o WHERE { <" + link.getNodeB() + "> ?p1 ?o . ?o ?p2 ?o2 "
                //    + "FILTER(!regex(?p1,\"" + HAS_GEOMETRY + "\",\"i\")) FILTER(!regex(?p2, \"" +  WKT +"\", \"i\")) }"; //FILTER(!regex(?p1, \"" +  LAT +"\", \"i\")) FILTER(!regex(?p2, \"" + LON + "\", \"i\")) these filters not needed in geosparql
            
            
            //System.out.println("select for every link:\n" + selectSpecificLinkQuery);
            QueryExecution queryExecution = null;
            try {
           
                final Query query = QueryFactory.create(selectSpecificLinkQuery);                 
                queryExecution = QueryExecutionFactory.sparqlService(endpointA, query, graphB);               
                final com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();
                
                while(resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();
                    //String subject = querySolution.getResource("?s").toString(); //subject is  nodeA
                    final String predicate = querySolution.getResource("?p").getURI();
                    String object;
                    final RDFNode objectNode = querySolution.get("?o");
                    
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

                        //object1 = objectNode.asLiteral().getLexicalForm();
                        object = objectNode.toString(); 
                        object = "\"" + object + "\"";
                    }
                    String subjectA;
                    subjectA = link.getNodeA();
                    String insertMetadata = formInsertQuery(subjectA, predicate, object, graphA);
                    executeVirtuosoUpdate(insertMetadata, set);
                                                            
                }                                      
            }
            catch (RuntimeException ex) {
                LOG.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
            finally {
                 if(queryExecution != null) {
                     queryExecution.close();
                }
            }
            
         }
    }
    
    
    private void executeVirtuosoUpdate(String updateQuery, VirtGraph set) {                
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(updateQuery, set);
        vur.exec();
    }
    
    
    //query for deleting specific subject with its additional blank node. 
    private String formDeleteGeomQuery(String subject, String graph){
        //get the second graph. fused geometries will go there
        
        return "WITH <" + graph + "> DELETE { <" + subject + "> <" + HAS_GEOMETRY + "> ?a . ?a <" + WKT + "> ?o2 } WHERE { <" + subject + "> <" + HAS_GEOMETRY + "> ?a . ?a <" + WKT + "> ?o2 }";
        //delete old values of geometries
        //return "DELETE FROM GRAPH <" + graphB + "> { <" + subject + "> ?p1 ?a . ?a ?p2 ?o2 } WHERE { <" + subject + "> ?p1 ?a . ?a ?p2 ?o2 }";
              
    }
    
    //query for inserting a triple set into a virtuoso graph
    private String formInsertQuery(String subject, String fusedGeometry, String graph) { 
        //System.out.println("geometry query\n INSERT INTO <" + graphB + "> { <" + subject + "> <" + HAS_GEOMETRY + "> _:a . _:a <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> }");
        return "INSERT INTO <" + graph + "> { <" + subject + "> <" + HAS_GEOMETRY + "> _:a . _:a <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> }";
    }
     
    private String formInsertQuery(String subject, String predicate, String object, String graph){
              //System.out.println("WITH <" + graph + "> INSERT { <" + subject +"> <" + predicate +"> " + object +" }");
              return "WITH <" + graph + "> INSERT { <" + subject +"> <" + predicate +"> " + object +" }";
              //keep left, average of two points 
    }
     
    private String formDeleteWgs84query(String subject, String graph){
        //System.out.println("WITH <" + graphA + "> DELETE { <" + subject + "> <" + LAT + "> ?o1 . <" + subject + "> <" + LON + "> ?o2 } WHERE { ");
        return "WITH <" + graph + "> DELETE { <" + subject + "> <" + LAT + "> ?o1 . <" + subject + "> <" + LON + "> ?o2 } WHERE { <" + subject + "> <" + LAT + "> ?o1 . <" + subject + "> <" + LON + "> ?o2 }";
    }
    
    private VirtGraph getVirtuosoSet (String url, String username, String password) throws SQLException {
        //Class.forName("virtuoso.jdbc4.Driver");
        VirtGraph vSet = new VirtGraph ("jdbc:virtuoso://" + url + "/CHARSET=UTF-8", username, password);
        LOG.info("Virtuoso connection established.");
        return vSet;
    }
    
    
    public void clean() {
        set.close();        
        LOG.info("Virtuoso import is done.");

    }
    
    public void insertMetadataToFusedGraph(List<Link> links, String fusedGraph) {
        for(Link link : links) {
            String subject = link.getNodeA();
            
            String selectLinkMetadata = "SELECT ?p0 ?o1 ?p1 ?o2 ?p2 ?o3 ?p3 ?o4 WHERE {"
                    + "<" + subject + "> ?p0 ?o1 ."
                    + "OPTIONAL { ?o1 ?p1 ?o2 .}"
                    + "OPTIONAL { ?o2 ?p2 ?o3 .}"
                    + "OPTIONAL { ?o3 ?p3 ?o4 .}"
                    + "FILTER(!REGEX(?p0, \"" + HAS_GEOMETRY + "\", \"i\"))"
                    + "FILTER(!REGEX(?p1, \"" + WKT + "\", \"i\"))"
                    + "FILTER(!REGEX(?p0, \"" + LAT + "\", \"i\"))" //probably not needed, geometries are already fused in geosparql
                    + "FILTER(!REGEX(?p0, \"" + LON + "\", \"i\"))}";//probably not needed
                       
            System.out.println(selectLinkMetadata);
            QueryExecution queryExecution = null;
            try {
                
                final Query query = QueryFactory.create(selectLinkMetadata);
                queryExecution = QueryExecutionFactory.sparqlService(endpointA, query, graphA); //selects from graphA (unister)
                final com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();
                
                while(resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();
                    
                    //final String predicate0 = querySolution.getResource("?p0").getURI();   //if first triple not null insert;
                    //final RDFNode objectNode1 = querySolution.get("?o1");
                    //check how deep is the chain of triples, and insert accordingly
                    
                    RDFNode o1,o2,o3,o4;
                    String p0,p1,p2,p3, object, s1, s2, s3, s4 , ob1, ob2, ob3, ob4;
                    if (querySolution.get("?o1") != null && querySolution.get("?p0") != null){
                        if (querySolution.get("?o2") != null && querySolution.get("?p1") != null){
                               if (querySolution.get("?o3") != null && querySolution.get("?p2") != null){
                                   if (querySolution.get("?o4") != null && querySolution.get("?p3") != null){
                                       //add set from sub to o4

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
                                        //System.out.println(insertQuery);
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
                                    //System.out.println(insertQuery);
                                    executeVirtuosoUpdate(insertQuery, set);
                                   
                                   
                               }
                               //add set from sub to o2
                                p0 = querySolution.getResource("?p0").getURI();
                                o1 = querySolution.get("?o1");
                                ob1 = constructObjectNode(o1);

                                s1 = querySolution.get("?o1").asResource().getURI();
                                p1 = querySolution.getResource("?p1").getURI();
                                o2 = querySolution.get("?o2");
                                ob2 = constructObjectNode(o2);
                                
                                String insertPattern = "<" + subject + "> <" + p0 + "> " + ob1 + " . <" + s1 + "> <" + p1 + "> " + ob2 + " . ";
                                String insertQuery = "WITH <" + fusedGraph + "> INSERT { " + insertPattern + " }";
                                //System.out.println(insertQuery);
                                executeVirtuosoUpdate(insertQuery, set);                              
                               
                        }
                        //add sub p0 o1
                        p0 = querySolution.getResource("?p0").getURI();
                        o1 = querySolution.get("?o1");
                        ob1 = constructObjectNode(o1);
                        
                        String insertPattern = "<" + subject + "> <" + p0 + "> " + ob1 + " . ";
                        String insertQuery = "WITH <" + fusedGraph + "> INSERT { " + insertPattern + " }";
                        //System.out.println(insertQuery);
                        executeVirtuosoUpdate(insertQuery, set);                      
                        
                    }
            
                    
                    /*
                    RDFNode o1,o2,o3,o4;
                    String p0,p1,p2,p3, object;
                    if (querySolution.get("?o1") != null && querySolution.get("?p0") != null){
                        //construct triple subject p0 o1
                        p0 = querySolution.getResource("?p0").getURI();
                        o1 = querySolution.get("?o1");
                        object = constructObjectNode(o1);
                        String insertMetadataTripleQuery = formInsertQuery(subject, p0, object, fusedGraph);
                        executeVirtuosoUpdate(insertMetadataTripleQuery, set);
                    }
                    if (querySolution.get("?o2") != null && querySolution.get("?p1") != null){
                        //construct triple o1 p1 o2
                        subject = querySolution.get("?o1").asResource().getURI();
                        System.out.println("subject o1 getURI = " + subject);
                        p1 = querySolution.getResource("?p1").getURI();
                        o2 = querySolution.get("?o2");
                        object = constructObjectNode(o2);                        
                        String insertMetadataTripleQuery = formInsertQuery(subject, p1, object, fusedGraph);
                        executeVirtuosoUpdate(insertMetadataTripleQuery, set);
                    }
                    if (querySolution.get("?o3") != null && querySolution.get("?p2") != null){
                        //construct triple o2 p2 o3
                        subject = querySolution.get("?o2").asResource().getURI();
                        p2 = querySolution.getResource("?p2").getURI();
                        o3 = querySolution.get("?o3");
                        object = constructObjectNode(o3);
                        String insertMetadataTripleQuery = formInsertQuery(subject, p2, object, fusedGraph);
                        executeVirtuosoUpdate(insertMetadataTripleQuery, set);
                    
                        if (querySolution.get("?o4") != null && querySolution.get("?p3") != null){
                            //construct triple o3 p3 o4
                            subject = querySolution.get("?o3").asResource().getURI();
                            p3 = querySolution.getResource("?p3").getURI();
                            o4 = querySolution.get("?o4");
                            object = constructObjectNode(o4);
                            insertMetadataTripleQuery = formInsertQuery(subject, p3, object, fusedGraph);
                            executeVirtuosoUpdate(insertMetadataTripleQuery, set);
                        }  
                    }*/

                    
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

            //object1 = objectNode.toString();
            object = objectNode.asLiteral().getLexicalForm();
            //RDFDatatype datatype = objectNode.asLiteral().getDatatype();
            //Literal litObj = objectNode.asLiteral();
            object = "\"" + object + "\""; // + datatype;
        }        
        return object;        
    }
    
    
    
    /*
    public void insertMetadataToFusedGraph(List<Link> links) {
         //keep metadata subjects according to the transformation    
        for(Link link : links) {                       
        String constructMetadataQuery = formConstructMetadataQuery(link.getNodeA());
        System.out.print(constructMetadataQuery);
            QueryExecution queryExecution = null;
            try {
           
                Query query = QueryFactory.create(constructMetadataQuery);
                //queryExecution = QueryExecutionFactory.sparqlService(endpointA, query, graphA);  //

                
                VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(query, set);
                Model model = vqe.execConstruct();
 	        Graph g = model.getGraph();
                
                System.out.println("\nCONSTRUCT results:");
                System.out.println("\nCONSTRUCT results:");
	        for (Iterator i = g.find(Node.ANY, Node.ANY, Node.ANY); i.hasNext();) 
	           {
                      System.out.println("GOT INTO iterator");
	              Triple t = (Triple)i.next();
                      System.out.println("as triple:\n");
                      System.out.println(t.asTriple());
		      System.out.println(" { " + t.getSubject() + " " + 
		      				 t.getPredicate() + " " + 
		      				 t.getObject() + " . }");
	        }
                
                while(resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();
                    
                    final String p = querySolution.getResource("?p").getURI();
                    final String p1 = querySolution.getResource("?p1").getURI();
                                       
                    String object;
                    final RDFNode o1 = querySolution.get("?o1");
                    final RDFNode o2 = querySolution.get("?o2");
                    if(o1.isResource()) {
                            if(o1.isAnon()) {

                                object = o1.asLiteral().getLexicalForm();                            
                                object =  "<" + object + ">";
                            }
                            else {

                                object = o1.asResource().getURI();
                                object = "<" + object + ">";
                            }
                    }
                    else {

                        //object1 = objectNode.asLiteral().getLexicalForm();
                        object = o1.toString(); 
                        object = "\"" + object + "\"";
                    }
                    String subjectA;
                    subjectA = link.getNodeA();
                    String insertMetadata = formInsertQuery(subjectA, p, object);
                    executeVirtuosoUpdate(insertMetadata, set);
                                                            
                }                                      
            }
            catch (RuntimeException ex) {
                LOG.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
            finally {
                 if(queryExecution != null) {
                     queryExecution.close();
                }
            }
            
         }
        
        
    }*/
    
    /*
    private String formConstructMetadataQuery(String subject){
        
        return "CONSTRUCT { \n" +
                "<" + subject + "> ?p ?o1 .\n" +
                " ?o1 ?p1 ?o2 .\n" +
                " ?o2 ?p2 ?o3 .\n" +
                " ?o3 ?p3 ?o4 .\n" +
                " ?o4 ?p4 ?o5 .\n" +
                " ?o5 ?p5 ?o6 .\n" +
                " ?o6 ?p6 ?o7 .\n" +
                " ?o7 ?p7 ?o8 .\n" +
                " ?o8 ?p8 ?o9 .\n" +
                "}\n" +
                "WHERE\n" +
                "{\n" +
                " GRAPH <http://localhost:8890/unister> {\n" +
                " <" + subject + "> ?p ?o1 .\n" +
                " OPTIONAL { ?o1 ?p1 ?o2 .}\n" +
                " OPTIONAL { ?o2 ?p2 ?o3 .}\n" +
                " OPTIONAL { ?o3 ?p3 ?o4 .}\n" +
                " OPTIONAL { ?o4 ?p4 ?o5 .}\n" +
                " OPTIONAL { ?o5 ?p5 ?o6 .}\n" +
                " OPTIONAL { ?o6 ?p6 ?o7 .}\n" +
                " OPTIONAL { ?o7 ?p7 ?o8 .}\n" +
                " OPTIONAL { ?o8 ?p8 ?o9 .}\n" +
                " FILTER(!REGEX(?p, \"http://www.opengis.net/ont/geosparql#hasGeometry\", \"i\"))\n" +
                " FILTER(!REGEX(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n" +
                "}}";
    }
    */
    
     /*
    private String formMetadataInsertQuery(String subject, String predicate, String object){
        //System.out.println("metadata query\n INSERT INTO <" + graphB + "> { <" + subject + "> <" + predicate + "> " + object + " }");
        return "INSERT INTO <" + graphB + "> { <" + subject + "> <" + predicate + "> " + object + " }";
    }
    
    private String formDeleteMetadataQuery(String subject, String predicate, String object){
        return "DELETE FROM GRAPH <" + graphB + "> { ?s ?p ?o } WHERE { <" + subject + "> <" + predicate + "> <" + object + "> }";
    }
    
    private String formUpdateMetadataQuery(String subjectInsert, String subjectDelete, String predicate, String object){
      return "WITH <" + graphB + "> DELETE { <" +  subjectDelete +"> <" + predicate +"> " + object +" }"
              + "INSERT { <" +  subjectInsert +"> <" + predicate +"> " + object +" }";
              //keep left, average of two points 
      
    }*/
    
        //commented out, we don' t want this for now
    /*
    public void importAllMetadataToVirtuoso() {

       //original String querySelectMetadata = "SELECT ?s ?p ?o WHERE { ?s ?p ?o FILTER(!regex(?p,<" + hasGeometry + ">,\"i\")) FILTER(!regex(?p,<" + WKT + ">,\"i\"))}";
       //System.out.println("metadata " + querySelectMetadata);
       String querySelectMetadata = "SELECT ?s ?p ?o WHERE { ?s ?p ?o FILTER(!regex(?p,\"" + HAS_GEOMETRY + "\",\"i\")) FILTER(!regex(?p, \"" +  WKT +"\", \"i\")) FILTER(!regex(?p, \"" +  LAT +"\", \"i\")) FILTER(!regex(?p, \"" + LON + "\", \"i\"))}";
       //System.out.println("SELECT ?s ?p ?o WHERE { ?s ?p ?o FILTER(!regex(?p,<" + hasGeometry + ">,\"i\")) FILTER(!regex(?p1, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) FILTER(!regex(?p1, \"http://www.w3.org/2003/01/geo/wgs84_pos#lon\", \"i\"))}");
       
       QueryExecution queryExecution = null;
       try {
           
            final Query query = QueryFactory.create(querySelectMetadata);
            queryExecution = QueryExecutionFactory.sparqlService(endpointA, query, graphA);           
            final com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();
            
            while(resultSet.hasNext()) {
                final QuerySolution querySolution = resultSet.next();
                final String subject = querySolution.getResource("?s").toString();
                final String predicate = querySolution.getResource("?p").getURI();
                String object;
                final RDFNode objectNode = querySolution.get("?o");
                
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
                    
                    //object1 = objectNode.asLiteral().getLexicalForm();
                    object = objectNode.toString(); 
                    object = "\"" + object + "\"";
                }                
                
                          
                String metadataInsertQuery = formMetadataInsertQuery(subject, predicate, object);
                executeVirtuosoUpdate(metadataInsertQuery, set);
            }
       }
       catch (RuntimeException ex) {
            LOG.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
        finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }
    }
    */
    
    //no longer needed
    /*
    private void clearGraphFirst(VirtGraph set){
    //clears the specified graph in order to insert new fused geometries
        try{       
            String updateQuery = "CLEAR GRAPH <" + graph + ">";
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(updateQuery, set);
            vur.exec();
        }
        catch (Exception ex){
            System.out.println("Something went wrong at clearing the graph");
        }        
    }
    */
    
}    