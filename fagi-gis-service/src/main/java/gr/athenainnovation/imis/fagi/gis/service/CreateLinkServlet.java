/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Maps;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author nick
 */
@WebServlet(name = "CreateLinkServlet", urlPatterns = {"/CreateLinkServlet"})
public class CreateLinkServlet extends HttpServlet {
    private static final String HAS_GEOMETRY_REGEX = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private static final String AS_WKT_REGEX = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String LONG_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    private static final String LAT_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    private static final String SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";
    private static final int DATASET_A = 0;
    private static final int DATASET_B = 1;
    private static final String DB_URL = "jdbc:postgresql:";
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        response.setContentType("text/html;charset=UTF-8");
        DBConfig dbConf;
        GraphConfig grConf;
        VirtGraph vSet = null;
        PreparedStatement stmt = null;
        PreparedStatement stmtAddGeomA = null;
        PreparedStatement stmtAddGeomB = null;
        Connection dbConn = null;
        ResultSet rs = null;
        List<FusionState> fs = null;
        String tGraph = null;
        String nodeA = null;
        String nodeB = null;
        String dom = null;
        String domSub = null;
        HttpSession sess = null;
    
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
           
            sess = request.getSession(true);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
            //mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            mapper.setDateFormat(outputFormat);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            tGraph = (String)sess.getAttribute("t_graph");
            
            nodeA = request.getParameter("subA");
            nodeB = request.getParameter("subB");
            
            System.out.println("Sub A : "+nodeA);
            System.out.println("Sub B : "+nodeB);
            
            dbConn = initGeomConnection(dbConf, out);
            vSet = insertLink(vSet, sess, dbConn, grConf, dbConf, nodeA, nodeB);
            insertMetadata(vSet, dbConf, grConf,tGraph, nodeA, nodeB);
            insertGeometry(dbConn, nodeA, grConf.getGraphA(), grConf.getEndpointA(), DATASET_A);
            insertGeometry(dbConn, nodeB, grConf.getGraphB(), grConf.getEndpointB(), DATASET_B);
            
            dbConn.close();
            vSet.close();
            
            out.println("{}");
        }
    }

    private boolean validateLinking(GraphConfig grConf, VirtGraph vSet, String lsub, String rsub) throws SQLException {
        Connection virt_conn = vSet.getConnection();
        PreparedStatement stmt;
        java.sql.ResultSet rs;

        String checkA = "SPARQL SELECT * WHERE { GRAPH <" + grConf.getGraphA() + "> {<" + lsub + "> ?p ?o } }";
        String checkB = "SPARQL SELECT * WHERE { GRAPH <" + grConf.getGraphB() + "> {<" + lsub + "> ?p ?o } }";
        System.out.println("Found in A : " + checkA + " B : " + checkB);
        System.out.println("Left sub : " + lsub + " Right sub : " + rsub);
        stmt = virt_conn.prepareStatement(checkA);
        rs = stmt.executeQuery();

        boolean foundInA = false;
        boolean foundInB = false;
        while (rs.next()) {
            foundInA = true;
            break;
        }

        rs.close();
        stmt.close();

        stmt = virt_conn.prepareStatement(checkB);
        rs = stmt.executeQuery();

        while (rs.next()) {
            foundInB = true;
            break;
        }

        rs.close();
        stmt.close();

        System.out.println("Found in A : " + foundInA + " B : " + foundInB);
        if (foundInA) {
            return false;
        } else {
            return true;
        }
    }
    
    VirtGraph insertLink(VirtGraph vSet, HttpSession sess, Connection dbConn, GraphConfig grConf, DBConfig dbConf, String nodeA, String nodeB) {
                System.out.println("Linking ");
                
        if (vSet == null) {
            try {
                                System.out.println("Trying "+dbConf.getDBURL());

                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                connEx.printStackTrace();
                System.out.println(connEx.getMessage());
                System.out.println("Connected ");
                return null;
            }
        }
        try {
            System.out.println("reached ");
            ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
            //queryStr.append("WITH <"+fusedGraph+"> ");
            queryStr.append("INSERT DATA { ");
            queryStr.append("GRAPH <http://localhost:8890/DAV/links_" + dbConf.getDBName() + "> { ");

            boolean makeSwap = validateLinking(grConf, vSet, nodeA, nodeB);
            
            String subject = nodeA;
            String subjectB = nodeB;
            if ( makeSwap ) {
                subject = nodeB;
                subjectB = nodeA;
            }
            
            String insertLinkQuery = "INSERT INTO links (nodea, nodeb) VALUES (?,?)";
            final PreparedStatement insertLinkStmt = dbConn.prepareStatement(insertLinkQuery);
            Link link = new Link(subject, subjectB);
            insertLinkStmt.setString(1, link.getNodeA());
            insertLinkStmt.setString(2, link.getNodeB());

            insertLinkStmt.addBatch();
            insertLinkStmt.executeBatch();

            dbConn.commit();
        
            HashMap<String, String> links = (HashMap<String, String>) sess.getAttribute("links");
            if ( links == null ) {
                links = Maps.newHashMap();
                links.put(subject, subjectB);
                System.out.println("Creating link : " + subject + "    " + subjectB);
                sess.setAttribute("links", links);
            } else {
                System.out.println("Creating link : " + subject + "    " + subjectB);
                links.put(subject, subjectB);
            }
            
            queryStr.appendIri(subject);
            queryStr.append(" ");
            queryStr.appendIri(SAME_AS);
            queryStr.append(" ");
            queryStr.appendIri(subjectB);
            queryStr.append(" ");
            queryStr.append(".");
            queryStr.append(" ");
            queryStr.append("} }");
            System.out.println("Print " + queryStr.toString());

            UpdateRequest q = queryStr.asUpdate();
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
            insertRemoteB.execute();
            
            queryStr = new ParameterizedSparqlString();
            //queryStr.append("WITH <"+fusedGraph+"> ");
            queryStr.append("INSERT DATA { ");
            queryStr.append("GRAPH <http://localhost:8890/DAV/all_links_" + dbConf.getDBName() + "> { ");
            
            queryStr.appendIri(subject);
            queryStr.append(" ");
            queryStr.appendIri(SAME_AS);
            queryStr.append(" ");
            queryStr.appendIri(subjectB);
            queryStr.append(" ");
            queryStr.append(".");
            queryStr.append(" ");
            queryStr.append("} }");
            System.out.println("Print " + queryStr.toString());

            q = queryStr.asUpdate();
            insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
            insertRemoteB.execute();
            //System.out.println("Add at "+addIdx+" Size "+cSize);
        } catch (org.apache.jena.atlas.web.HttpException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return vSet;
    }

    void insertMetadata(VirtGraph vSet, DBConfig dbConf, GraphConfig grConf, String tGraph, String nodeA, String nodeB) throws SQLException {
        if (vSet == null) {
            try {
                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                System.out.println(connEx.getMessage());

                return;
            }
        }
        
        StringBuilder getFromA = new StringBuilder();
        String endpointLoc2 = grConf.getEndpointA();
        //System.out.println("is local "+isLocalEndpoint(endpointA));
        if (endpointLoc2.equals(grConf.getEndpointA())) {
            getFromA.append("sparql INSERT\n");
            getFromA.append("  { GRAPH <").append(tGraph).append("_"+dbConf.getDBName()+"A"+"> {\n");
            if (grConf.isDominantA())
                getFromA.append(" <"+nodeA+"> ?p ?o1 . \n");
            else
                getFromA.append(" <"+nodeB+"> ?p ?o1 . \n");
            getFromA.append(" ?o1 ?p4 ?o3 .\n");
            getFromA.append(" ?o3 ?p5 ?o4 .\n");
            getFromA.append(" ?o4 ?p6 ?o5\n");
            getFromA.append("} }\nWHERE\n");
            getFromA.append("{\n");
            getFromA.append(" GRAPH <").append(grConf.getGraphA()).append("> { {<"+nodeA+"> ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3 . OPTIONAL { ?o3 ?p5 ?o4 . OPTIONAL { ?o4 ?p6 ?o5 .} } } }\n");
            getFromA.append("\n");
            getFromA.append("  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n");
            getFromA.append("  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n");
            getFromA.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n");
            getFromA.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n");
            getFromA.append("  FILTER(!regex(?p4,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n");
            getFromA.append("  FILTER(!regex(?p4, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n");
            getFromA.append("  FILTER(!regex(?p4, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n");
            getFromA.append("  FILTER(!regex(?p4, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n");
            getFromA.append("}");
            System.out.println("Get from A "+getFromA);
        }
        
        StringBuilder getFromB = new StringBuilder();
        endpointLoc2 = grConf.getEndpointB();
        //System.out.println("is local "+isLocalEndpoint(endpointA));
        if (endpointLoc2.equals(grConf.getEndpointB())) {
            getFromB.append("sparql INSERT\n");
            getFromB.append("  { GRAPH <").append(tGraph).append("_"+dbConf.getDBName()+"B"+"> {\n");
            if (grConf.isDominantA())
                getFromB.append(" <"+nodeA+"> ?p ?o1 . \n");
            else
                getFromB.append(" <"+nodeB+"> ?p ?o1 . \n");
            getFromB.append(" ?o1 ?p4 ?o3 .\n");
            getFromB.append(" ?o3 ?p5 ?o4 .\n");
            getFromB.append(" ?o4 ?p6 ?o5\n");
            getFromB.append("} }\nWHERE\n");
            getFromB.append("{\n");
            getFromB.append(" GRAPH <").append(grConf.getGraphB()).append("> { { <"+nodeB+"> ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3 . OPTIONAL { ?o3 ?p5 ?o4 . OPTIONAL { ?o4 ?p6 ?o5 .} } } }\n");
            getFromB.append("\n");
            getFromB.append("  FILTER(!regex(?p,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n");
            getFromB.append("  FILTER(!regex(?p, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n");
            getFromB.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n");
            getFromB.append("  FILTER(!regex(?p, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n");
            getFromB.append("  FILTER(!regex(?p4,\"http://www.opengis.net/ont/geosparql#hasGeometry\",\"i\")) \n");
            getFromB.append("  FILTER(!regex(?p4, \"http://www.opengis.net/ont/geosparql#asWKT\", \"i\"))\n");
            getFromB.append("  FILTER(!regex(?p4, \"http://www.w3.org/2003/01/geo/wgs84_pos#lat\", \"i\")) \n");
            getFromB.append("  FILTER(!regex(?p4, \"http://www.w3.org/2003/01/geo/wgs84_pos#long\", \"i\"))\n");
            getFromB.append("}");
            System.out.println("Get from B "+getFromB);
        }
        
        Connection virt_conn = vSet.getConnection();
        
        PreparedStatement stmt = virt_conn.prepareStatement(getFromA.toString());
        stmt.executeUpdate();

        stmt.close();
        
        stmt = virt_conn.prepareStatement(getFromB.toString());
        stmt.executeUpdate();
        
        stmt.close();
    }
    
    Connection initGeomConnection(DBConfig dbConf, PrintWriter out) {
        try {
            String url = DB_URL.concat(dbConf.getDBName());
            Connection dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
            dbConn.setAutoCommit(false);
            return dbConn;
        } catch (SQLException sqlex) {
            System.out.println(sqlex.getMessage());
            out.println("Connection to postgis failed");
            out.close();
        }
        
        return null;
    }
    
    PreparedStatement initGeomStatementA(int DS_ID, Connection dbConn) throws SQLException {
        final String insertGeometryAString = "INSERT INTO dataset_a_geometries (subject, geom) VALUES (?, ST_GeometryFromText(?, 4326))";
        return dbConn.prepareStatement(insertGeometryAString);
    }

    PreparedStatement initGeomStatementB(int DS_ID, Connection dbConn) throws SQLException {
        final String insertGeometryBString = "INSERT INTO dataset_b_geometries (subject, geom) VALUES (?, ST_GeometryFromText(?, 4326))";
        return dbConn.prepareStatement(insertGeometryBString);
    }
    
    void initGeomStatement(int DS_ID, Connection dbConn) throws SQLException {
        /*
        if ( DS_ID == DATASET_A ) {
            final String insertGeometryAString = "INSERT INTO dataset_a_geometries (subject, geom) VALUES (?, ST_GeometryFromText(?, 4326))";
            stmtAddGeomA = dbConn.prepareStatement(insertGeometryAString);
        } else {
            final String insertGeometryBString = "INSERT INTO dataset_b_geometries (subject, geom) VALUES (?, ST_GeometryFromText(?, 4326))";
            stmtAddGeomB = dbConn.prepareStatement(insertGeometryBString);
        }
        */
    }
    
    void addGeom ( int DS_ID,  PreparedStatement stmt, String sub, String geom ) throws SQLException {
        if ( DS_ID == DATASET_A ) {
            stmt.setString(1, sub);
            stmt.setString(2, geom);
            stmt.addBatch();
        } else {
            stmt.setString(1, sub);
            stmt.setString(2, geom);
            stmt.addBatch();
        }
    }
    
    void finishGeomUpload(int DS_ID, PreparedStatement stmt, Connection dbConn) throws SQLException {
        if ( DS_ID == DATASET_A ) {
            stmt.executeBatch();
        } else {
            stmt.executeBatch();
        }
        dbConn.commit();
    }
    
    void insertGeometry(Connection dbConn, String node, String sourceGraph, String sourceEndpoint, int DS_ID) {
        final String restrictionForWgs = "<"+node+"> ?p1 ?o1 . <"+node+"> ?p2 ?o2 "
                + "FILTER(regex(?p1, \"" + LAT_REGEX + "\", \"i\"))"
                + "FILTER(regex(?p2, \"" + LONG_REGEX + "\", \"i\"))";
        
        final String restriction = "<"+node+"> ?p1 _:a . _:a <"+AS_WKT_REGEX+"> ?g ";
        final String queryString1 = "SELECT ?o1 ?o2 WHERE { GRAPH <"+sourceGraph+"> {" + restrictionForWgs + "} }";
        
        boolean countWgs = checkForWGS(sourceEndpoint, sourceGraph, restrictionForWgs, "?s");  
        boolean countWKT = checkForWKT(sourceEndpoint, sourceGraph, restriction, "?os");
        
        final String queryString;
        queryString = "SELECT ?g WHERE { GRAPH <"+sourceGraph+"> {" + restriction + " } }";
                
        QueryExecution queryExecution = null;
        if (!countWKT){ //if geosparql geometry doesn' t exist        
            try {
                PreparedStatement stmt = null;
                if ( DS_ID == DATASET_A ) {
                    stmt = initGeomStatementA(DS_ID, dbConn);
                } else {
                    stmt = initGeomStatementB(DS_ID, dbConn);
                }
                final Query query = QueryFactory.create(queryString1);
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, authenticator);
                final ResultSet resultSet = queryExecution.execSelect();
                long startTime =  System.nanoTime();
                while(resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();
                    
                    //System.out.println("Subject "+subject);
                    final RDFNode objectNode1 = querySolution.get("?o1"); //lat
                    final RDFNode objectNode2 = querySolution.get("?o2"); //long
                    
                    if(objectNode1.isLiteral() && objectNode2.isLiteral()) {
                        final double latitude = objectNode1.asLiteral().getDouble();
                        final double longitude = objectNode2.asLiteral().getDouble();
                        
                        //construct wkt serialization
                        String geometry = "POINT ("+ longitude + " " + latitude +")";
                        //postGISImporter.loadGeometry(datasetIdent, subject, geometry);
                        addGeom(DS_ID, stmt, node, geometry);
                    }
                    else {
                        //LOG.warn("Resource found where geometry serialisation literal expected.");
                    }
                    
                    //if (callback != null )
                        //callback.publishGeometryProgress((int) (0.5 + (100 * (double) currentCount++ / (double) countWgs)));                                       
                }
                queryExecution.close();
                finishGeomUpload(DS_ID, stmt, dbConn);
                stmt.close();
                //postGISImporter.finishUpdates();
                //System.out.println("PostGISImporter finishedUpdates");
                //long endTime =  System.nanoTime();
                //setElapsedTime((endTime-startTime)/1000000000f);
            }
            catch (SQLException | RuntimeException ex) {
                //LOG.error(ex.getMessage(), ex);
                throw new RuntimeException(ex);
            }
            finally {
                if(queryExecution != null) {
                    queryExecution.close();
                }
            }
        }                                   
        else { //if geosparql geometry exists
            //System.out.println("geosparql");
            try {
                PreparedStatement stmt = null;
                if ( DS_ID == DATASET_A ) {
                    stmt = initGeomStatementA(DS_ID, dbConn);
                } else {
                    stmt = initGeomStatementB(DS_ID, dbConn);
                }
                //System.out.println("Geosparql Query String "+queryString);
                final Query query = QueryFactory.create(queryString);
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, authenticator);
                //System.out.println("query: \n" + query + "\nendpoint: " + sourceEndpoint + " sourceGraph: " + sourceGraph);
                final ResultSet resultSet = queryExecution.execSelect();
                long startTime = System.nanoTime();
                while (resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();

                    //final String subject = querySolution.getResource("?os").getURI();
                    //System.out.println(subject);
                    final RDFNode objectNode = querySolution.get("?g");
                    if (objectNode.isLiteral()) {
                        final String geometry = objectNode.asLiteral().getLexicalForm();
                        //System.out.println("Virtuoso geometry objectNode.asLiteral().getLexicalForm():   "+ geometry);

                        //postGISImporter.loadGeometry(datasetIdent, subject, geometry);
                        addGeom(DS_ID, stmt, node, geometry);
                    } else {
                        //LOG.warn("Resource found where geometry serialisation literal expected.");
                    }
                    //if (callback != null )
                    //callback.publishGeometryProgress((int) (0.5 + (100 * (double) currentCount++ / (double) countWKT)));
                }
                queryExecution.close();
                finishGeomUpload(DS_ID, stmt, dbConn);
                stmt.close();
                //postGISImporter.finishUpdates();
                //System.out.println("Count : "+currentCount);
                //long endTime =  System.nanoTime();
                //setElapsedTime((endTime-startTime)/1000000000f);
            } catch (SQLException | RuntimeException ex) {
                //LOG.error(ex.getMessage(), ex);
                //java.util.logging.Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
                if (ex instanceof SQLException) {
                    SQLException exception = (SQLException) ex;
                    while (exception != null) {
                        //java.util.logging.Logger.getLogger(FusionGISCLI.class.getName()).log(Level.SEVERE, null, exception);
                        exception = exception.getNextException();
                    }
                }
                //System.out.println("throwing runtime ex");
                //throw new RuntimeException(ex);
            } finally {
                if (queryExecution != null) {
                    queryExecution.close();
                }
            }
        }
    }
          
    private boolean checkForWGS(final String sourceEndpoint, final String sourceGraph, final String restriction, final String sub) {
        boolean result = false;
    //ASK WHERE { ?s <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?o1 . ?s <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?o2 }
        final String queryString = "ASK WHERE { ?s <"+LAT_REGEX+"> ?o1 . ?s <"+LONG_REGEX+"> ?o2 }";
        QueryExecution queryExecution = null;
        try {
            final Query query = QueryFactory.create(queryString);
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph, authenticator);
            //System.out.println("source endpoint: " +sourceEndpoint +" query: "+ query + "sourceGraph: " + sourceGraph);

            result = queryExecution.execAsk();
        }
        catch (RuntimeException ex) {
            //LOG.warn(ex.getMessage(), ex);
        }
        finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }
        return result;
    }
    
    private boolean checkForWKT(final String sourceEndpoint, final String sourceGraph, final String restriction, final String sub) {
        boolean result = false;
        
        final String queryString = "ASK WHERE { ?os ?p1 _:a . _:a <"+AS_WKT_REGEX+"> ?g }";
        QueryExecution queryExecution = null;
        try {
            final Query query = QueryFactory.create(queryString);
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph, authenticator);
            //System.out.println("source endpoint: " +sourceEndpoint +" query: "+ query + "sourceGraph: " + sourceGraph);

            result = queryExecution.execAsk();
        }
        catch (RuntimeException ex) {
            //LOG.warn(ex.getMessage(), ex);
        }
        finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }
        return result;
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            /*
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (stmtAddGeomA != null) {
                try {
                    stmtAddGeomA.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (stmtAddGeomA != null) {
                try {
                    stmtAddGeomA.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            */
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            /*
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (stmtAddGeomA != null) {
                try {
                    stmtAddGeomA.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (stmtAddGeomA != null) {
                try {
                    stmtAddGeomA.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            */
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
