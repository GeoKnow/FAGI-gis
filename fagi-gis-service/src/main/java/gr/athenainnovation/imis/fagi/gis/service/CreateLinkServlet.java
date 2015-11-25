/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Maps;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import gr.athenainnovation.imis.fusion.gis.utils.Utilities;
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
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.log4j.Logger;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoException;
import virtuoso.jdbc4.VirtuosoPreparedStatement;
import virtuoso.jdbc4.VirtuosoResultSet;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "CreateLinkServlet", urlPatterns = {"/CreateLinkServlet"})
public class CreateLinkServlet extends HttpServlet {
    
    private static final Logger LOG = Log.getClassFAGILogger(CreateLinkServlet.class);    

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
            throws ServletException, SQLException {
        response.setContentType("text/html;charset=UTF-8");
        
        // per requsest state
        PrintWriter             out = null;
        DBConfig                dbConf;
        GraphConfig             grConf;
        VirtGraph               vSet = null;
        PreparedStatement       stmt = null;
        PreparedStatement       stmtAddGeomA = null;
        PreparedStatement       stmtAddGeomB = null;
        Connection              dbConn = null;
        ResultSet               rs = null;
        List<FusionState>       fs = null;
        String                  tGraph ;
        String                  nodeA ;
        String                  nodeB ;
        String                  dom ;
        String                  domSub ;
        HttpSession             sess ;
        ObjectMapper            mapper = new ObjectMapper();
        long                    startTime, endTime;
        JSONRequestResult       res;  
        boolean                 insertResult;
        
        try {
            /* TODO output your page here. You may use following sample code. */
            out = response.getWriter();
            
            res = new JSONRequestResult();
            
            sess = request.getSession(false);
            
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            tGraph = (String)sess.getAttribute("t_graph");
            
            nodeA = request.getParameter("subA");
            nodeB = request.getParameter("subB");
            
            System.out.println("Sub A : "+nodeA);
            System.out.println("Sub B : "+nodeB);
            
            startTime = System.nanoTime();
            
            dbConn = initGeomConnection(dbConf, out);
            vSet = insertLink(vSet, sess, dbConn, grConf, dbConf, nodeA, nodeB);
            if ( !Constants.LATE_FETCH )
                insertMetadata(vSet, dbConf, grConf,tGraph, nodeA, nodeB);
            
            
            insertResult = insertGeometry(dbConn, nodeA, grConf.getGraphA(), grConf.getEndpointA(), Constants.DATASET_A);
            if (!insertResult) {
                LOG.info("Dataset A has no geometric information ");

                res.setStatusCode(-1);
                res.setMessage("Dataset A has no geometric information");

                out.println(mapper.writeValueAsString(res));

                return;
            }
            
            insertResult = insertGeometry(dbConn, nodeB, grConf.getGraphB(), grConf.getEndpointB(), Constants.DATASET_B);
            
            if (!insertResult) {
                LOG.info("Dataset B has no geometric information ");

                res.setStatusCode(-1);
                res.setMessage("Dataset B has no geometric information");

                out.println(mapper.writeValueAsString(res));

                return;
            }
            
            vSet.close();
            
            endTime = System.nanoTime();
        
            LOG.info("Link Creation lasted " + (endTime - startTime ) / Constants.NANOS_PER_SECOND);
          
            res.setStatusCode(0);
            res.setMessage("Link Created!!");
            
            out.println(mapper.writeValueAsString(res));
            
        } catch (java.lang.OutOfMemoryError oome) {
            LOG.trace("OutOfMemoryError thrown");
            LOG.debug("OutOfMemoryError thrown : " + oome.getMessage());
            
            throw new ServletException("OutOfMemoryError thrown by Tomcat");
        } catch (JsonProcessingException ex) {
            LOG.trace("JsonProcessingException thrown");
            LOG.debug("JsonProcessingException thrown : " + ex.getMessage());
            
            throw new ServletException("JsonProcessingException thrown by Tomcat");
        } catch (IOException ex) {
            LOG.trace("IOException thrown");
            LOG.debug("IOException thrown : " + ex.getMessage());
            
            throw new ServletException("IOException opening the servlet writer");
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                }
            }
            if (stmtAddGeomA != null) {
                try {
                    stmtAddGeomA.close();
                } catch (SQLException ex) {
                }
            }
            if (stmtAddGeomA != null) {
                try {
                    stmtAddGeomA.close();
                } catch (SQLException ex) {
                }
            }
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                }
            }
        }
    }

    private Boolean validateLinking(HttpSession sess, String lsub, String rsub, VirtGraph vSet, GraphConfig grConf) {
        Connection virt_conn = vSet.getConnection();

        String checkA = "SELECT * WHERE { GRAPH <" + grConf.getGraphA() + "> {<" + lsub + "> ?p ?o } }";
        String checkB = "SELECT * WHERE { GRAPH <" + grConf.getGraphB() + "> {<" + lsub + "> ?p ?o } }";
       
        boolean foundInA = false;
        boolean foundInB = false;
        
        try {
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            QueryEngineHTTP qeh = QueryExecutionFactory.createServiceRequest(grConf.getEndpointA(), QueryFactory.create(checkA), authenticator);
            qeh.setSelectContentType((String) sess.getAttribute("content-type"));
            com.hp.hpl.jena.query.ResultSet resultSet = qeh.execSelect();

            if (resultSet.hasNext()) {
                foundInA = true;
            }

            qeh.close();

            qeh = QueryExecutionFactory.createServiceRequest(grConf.getEndpointB(), QueryFactory.create(checkB), authenticator);
            qeh.setSelectContentType((String) sess.getAttribute("content-type"));
            resultSet = qeh.execSelect();

            if (resultSet.hasNext()) {
                foundInB = true;
            }

            qeh.close();

        } catch (QueryException qex) {
            LOG.trace("QueryException thrown during link validation");
            LOG.debug("QueryException thrown during link validation : \n" + qex.getMessage());

            return null;
        }
        
        if (foundInA) {
            return false;
        } else if ( foundInB ) {
            LOG.info("Need to swap link subjects order");
            return true;
        } else {
            return null;
        }
    }
    
    VirtGraph insertLink(VirtGraph vSet, HttpSession sess, Connection dbConn, GraphConfig grConf, DBConfig dbConf, String nodeA, String nodeB) {
        System.out.println("Linking ");

        if (vSet == null) {
            try {
                System.out.println("Trying " + dbConf.getDBURL());

                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                LOG.trace("JenaException thrown during connection for link creation");
                LOG.debug("JenaException thrown during connection for link creation : \n" + connEx.getMessage());
            
                return null;
            }
        }
        try {
            System.out.println("reached ");
            ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
            //queryStr.append("WITH <"+fusedGraph+"> ");
            queryStr.append("INSERT DATA { ");
            queryStr.append("GRAPH <" + grConf.getAllLinksGraph() + "> { ");

            boolean makeSwap = validateLinking(sess, nodeA, nodeB, vSet, grConf);

            String subject = nodeA;
            String subjectB = nodeB;
            if (makeSwap) {
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
            if (links == null) {
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
            queryStr.appendIri(Constants.SAME_AS);
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
            queryStr.append("GRAPH <" + grConf.getAllLinksGraph() + "> { ");

            queryStr.appendIri(subject);
            queryStr.append(" ");
            queryStr.appendIri(Constants.SAME_AS);
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
        } catch (org.apache.jena.atlas.web.HttpException qex) {
            LOG.trace("HttpException thrown during link creation");
            LOG.debug("HttpException thrown during link creation : \n" + qex.getMessage());
            
            return null;
        } catch (SQLException qex) {
            LOG.trace("SQLException thrown during link creation");
            LOG.debug("SQLException thrown during link creation : \n" + qex.getMessage());
            LOG.debug("SQLException thrown during link creation : \n" + qex.getSQLState());

            return null;
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
        
        boolean updated = false;
        PreparedStatement stmt = null;
        while (!updated) {
            try {
                StringBuilder getFromA = new StringBuilder();
                String endpointLoc2 = grConf.getEndpointA();
                //System.out.println("is local "+isLocalEndpoint(endpointA));
                if (endpointLoc2.equals(grConf.getEndpointA())) {
                    getFromA.append("sparql INSERT\n");
                    getFromA.append("  { GRAPH <").append(grConf.getMetadataGraphA()).append("> {\n");
                    if (grConf.isDominantA()) {
                        getFromA.append(" <" + nodeA + "> ?p ?o1 . \n");
                    } else {
                        getFromA.append(" <" + nodeB + "> ?p ?o1 . \n");
                    }
                    getFromA.append(" ?o1 ?p4 ?o3 .\n");
                    getFromA.append(" ?o3 ?p5 ?o4 .\n");
                    getFromA.append(" ?o4 ?p6 ?o5\n");
                    getFromA.append("} }\nWHERE\n");
                    getFromA.append("{\n");
                    getFromA.append(" GRAPH <").append(grConf.getGraphA()).append("> { {<" + nodeA + "> ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3 . OPTIONAL { ?o3 ?p5 ?o4 . OPTIONAL { ?o4 ?p6 ?o5 .} } } }\n");
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
                    System.out.println("Get from A " + getFromA);
                }

                StringBuilder getFromB = new StringBuilder();
                endpointLoc2 = grConf.getEndpointB();
                //System.out.println("is local "+isLocalEndpoint(endpointA));
                if (endpointLoc2.equals(grConf.getEndpointB())) {
                    getFromB.append("sparql INSERT\n");
                    getFromB.append("  { GRAPH <").append(grConf.getMetadataGraphB()).append("> {\n");
                    if (grConf.isDominantA()) {
                        getFromB.append(" <" + nodeA + "> ?p ?o1 . \n");
                    } else {
                        getFromB.append(" <" + nodeB + "> ?p ?o1 . \n");
                    }
                    getFromB.append(" ?o1 ?p4 ?o3 .\n");
                    getFromB.append(" ?o3 ?p5 ?o4 .\n");
                    getFromB.append(" ?o4 ?p6 ?o5\n");
                    getFromB.append("} }\nWHERE\n");
                    getFromB.append("{\n");
                    getFromB.append(" GRAPH <").append(grConf.getGraphB()).append("> { { <" + nodeB + "> ?p ?o1} OPTIONAL { ?o1 ?p4 ?o3 . OPTIONAL { ?o3 ?p5 ?o4 . OPTIONAL { ?o4 ?p6 ?o5 .} } } }\n");
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
                    System.out.println("Get from B " + getFromB);
                }

                Connection virt_conn = vSet.getConnection();

                stmt = virt_conn.prepareStatement(getFromA.toString());
                stmt.executeUpdate();

                stmt.close();

                stmt = virt_conn.prepareStatement(getFromB.toString());
                stmt.executeUpdate();

                stmt.close();
                
                updated = true;
            } catch (VirtuosoException ve) {
                if (ve.getLocalizedMessage().contains("deadlock")) {
                    System.out.println("Deadlocked " + ve.getMessage());
                    updated = false;
                } else {
                    System.out.println(ve.getMessage());
                }
            }
        }

        if ( stmt != null )
            stmt.close();
    }
    
    Connection initGeomConnection(DBConfig dbConf, PrintWriter out) {
        try {
            String url = Constants.DB_URL.concat(dbConf.getDBName());
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
        if ( DS_ID == Constants.DATASET_A ) {
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
        if ( DS_ID == Constants.DATASET_A ) {
            stmt.executeBatch();
        } else {
            stmt.executeBatch();
        }
        dbConn.commit();
    }
    
    
    /**
     * This method fetches all triples with a subject matching parameter subjectRegex and a two triple chain with a predicate matching {@link Importer#HAS_GEOMETRY_REGEX} in the first triple and
     * {@link Importer#AS_WKT_REGEX} in the second triple.
     * Those triples are then imported into a PostGIS database using an instance of {@link PostGISImporter}.
     * 
     * @param datasetIdent {@link PostGISImporter#DATASET_A} for dataset A or {@link PostGISImporter#DATASET_B} for dataset B
     * @param sourceDataset source dataset from which to extract triples
     * @return success
     */
    
    /*
    boolean insertGeometry(VirtGraph vSet, Connection dbConn, GraphConfig grConf, String node, String sourceGraph, String sourceEndpoint, int DS_ID) {
        boolean success = true;
        
        long endTime, startTime;
        
        VirtuosoConnection virt_conn = (VirtuosoConnection)vSet.getConnection();
        VirtuosoPreparedStatement virt_stmt;
        //check the serialisation of the dataset with a count query. If it doesn t find WKT the serialisation, the serialisation is wgs84        
        
        //wgs84
        final String restrictionForWgs = 
                "<"+node+"> ?p1 ?o1 . "+node+" ?p2 ?o2 "
                + "FILTER(regex(?p1, \"" + Constants.LAT_REGEX + "\", \"i\")) "
                + "FILTER(regex(?p2, \"" + Constants.LONG_REGEX + "\", \"i\"))";
        
        final String restrictionForWKT = "<"+node+"> ?p1 _:a . _:a <"+Constants.AS_WKT_REGEX+"> ?g ";
        
        // Should we use the SERVICE keyword?
        boolean isEndpointLocal = Utilities.isURLToLocalInstance(sourceEndpoint);
       
            final String queryWGS;
        if (DS_ID == Constants.DATASET_A) {
            if (isEndpointLocal) {
                queryWGS = "SELECT ?o1 ?o2 "
                        + "WHERE { "
                        + "GRAPH <" + sourceGraph + "> {" + restrictionForWgs + "} }";
            } else {
                queryWGS = "SELECT ?o1 ?o2 "
                        + "WHERE { "
                        + "SERVICE <" + sourceEndpoint + "> "
                        + "{ GRAPH <" + sourceGraph + "> {" + restrictionForWgs + "}"
                        + " } }";
            }
        } else {
            if (isEndpointLocal) {
                queryWGS = "SELECT ?o1 ?o2 "
                        + "WHERE { "
                        + "GRAPH <" + sourceGraph + "> {" + restrictionForWgs + "} }";
            } else {
                queryWGS = "SELECT ?o1 ?o2 "
                        + "WHERE { "
                        + "SERVICE <" + sourceEndpoint + "> "
                        + "{ GRAPH <" + sourceGraph + "> {" + restrictionForWgs + "}"
                        + " } }";
            }
        }
        
        startTime = System.nanoTime();
        boolean countWgs = checkForWGS(sourceEndpoint, sourceGraph, restrictionForWgs, "?s");     
        endTime = System.nanoTime();
        LOG.info("check WGS lasted " + (endTime - startTime ) / Constants.NANOS_PER_SECOND);
          
        startTime = System.nanoTime();
        boolean countWKT = checkForWKT(sourceEndpoint, sourceGraph, restrictionForWKT, "?os");
        endTime = System.nanoTime();
        LOG.info("check WKT lasted " + (endTime - startTime ) / Constants.NANOS_PER_SECOND);
          
        //int countWKT = 1;
        //int countWgs = 0;
        
        final String queryWKT;
        if (Constants.LATE_FETCH) {
            if (DS_ID == Constants.DATASET_A) {
                if (isEndpointLocal) {
                    queryWKT = "SELECT ?g WHERE { GRAPH <" + sourceGraph + "> {" + restrictionForWKT + " } }";
                } else {
                    queryWKT = "SELECT ?g WHERE { SERVICE <" + sourceEndpoint + "> { GRAPH <" + sourceGraph + "> {" + restrictionForWKT + "} } }";
                }
            } else {
                if (isEndpointLocal) {
                    queryWKT = "SELECT ?g WHERE { GRAPH <" + sourceGraph + "> {" + restrictionForWKT + " } }";
                } else {
                    queryWKT = "SELECT ?g WHERE { SERVICE <" + sourceEndpoint + "> { GRAPH <" + sourceGraph + "> {" + restrictionForWKT + "} } }";
                }
            }
        } else {
            if (DS_ID == Constants.DATASET_A) {
                if (isEndpointLocal) {
                    queryWKT = "SELECT ?g WHERE { GRAPH <" + sourceGraph + "> {" + restrictionForWKT + " } }";
                } else {
                    queryWKT = "SELECT ?g WHERE { SERVICE <" + sourceEndpoint + "> { GRAPH <" + sourceGraph + "> {" + restrictionForWKT + "} } }";
                }
            } else {
                if (isEndpointLocal) {
                    queryWKT = "SELECT ?g WHERE { GRAPH <" + sourceGraph + "> {" + restrictionForWKT + " } }";
                } else {
                    queryWKT = "SELECT ?g WHERE { SERVICE <" + sourceEndpoint + "> { GRAPH <" + sourceGraph + "> {" + restrictionForWKT + "} } }";
                }
            }
        }
        
        QueryExecution queryExecution = null;
        if ( !countWKT ){ //if geosparql geometry doesn' t exist        
            try {
                PreparedStatement stmt = null;
                if ( DS_ID == Constants.DATASET_A ) {
                    stmt = initGeomStatementA(DS_ID, dbConn);
                } else {
                    stmt = initGeomStatementB(DS_ID, dbConn);
                }
                
                virt_stmt = (VirtuosoPreparedStatement)virt_conn.prepareStatement("SPARQL " + queryWGS);
                VirtuosoResultSet rs = (VirtuosoResultSet) virt_stmt.executeQuery();
                
                startTime = System.nanoTime();

                while(rs.next()) {
                    
                    final double latitude = Double.parseDouble(rs.getString(1));
                    final double longitude = Double.parseDouble(rs.getString(2));
                    final String geometry = "POINT ("+ longitude + " " + latitude +")";

                    //success = postGISImporter.loadGeometry(datasetIdent, subject, geometry);
                    addGeom(DS_ID, stmt, node, geometry);
                }
                
                rs.close();
                virt_stmt.close();
               
                //success = postGISImporter.finishUpdates();
                //System.out.println("Count : "+currentCount);
                endTime = System.nanoTime();
                
                //postGISImporter.finishUpdates();
                
                LOG.info("Loading WGS lasted "+Utilities.nanoToSeconds(endTime-startTime));
            }
            catch (SQLException ex) {
                LOG.trace("SQLException thrown during WGS geometry update");
                LOG.debug("SQLException thrown during WGS geometry update : \n" + ex.getMessage());
                LOG.debug("SQLException thrown during WGS geometry update : \n" + ex.getSQLState());

                SQLException exception = (SQLException) ex;
                while(exception != null) {
                    LOG.trace("SQLException expanded (see DEBUG) ");
                    LOG.debug("SQLException expanded : \n" + exception.getMessage());
                    LOG.debug("SQLException expanded : \n" + exception.getSQLState());
                }

                success = false;
            }
        } 
        
        if ( countWKT ) { //if geosparql geometry exists
            //System.out.println("geosparql");
            try {
                PreparedStatement stmt = null;
                if ( DS_ID == Constants.DATASET_A ) {
                    stmt = initGeomStatementA(DS_ID, dbConn);
                } else {
                    stmt = initGeomStatementB(DS_ID, dbConn);
                }
                
                startTime = System.nanoTime();
        
                virt_stmt = (VirtuosoPreparedStatement)virt_conn.prepareStatement("SPARQL " + queryWKT);
                VirtuosoResultSet rs = (VirtuosoResultSet) virt_stmt.executeQuery();
                
                startTime =  System.nanoTime();
                
                while(rs.next()) {
                    
                    final String geometry = rs.getString(1);
                    
                    //success = postGISImporter.loadGeometry(datasetIdent, subject, geometry);
                    addGeom(DS_ID, stmt, node, geometry);
                }
                
                rs.close();
                virt_stmt.close();
               
                endTime = System.nanoTime();
                LOG.info("check WGS lasted " + (endTime - startTime ) / Constants.NANOS_PER_SECOND);
                          //success = postGISImporter.finishUpdates();
                //System.out.println("Count : "+currentCount);
                endTime = System.nanoTime();
                  
                LOG.info("Loading WKT lasted "+Utilities.nanoToSeconds(endTime-startTime));

            }
            catch (SQLException ex) {
                LOG.trace("SQLException thrown during WKT geometry update");
                LOG.debug("SQLException thrown during WKT geometry update : \n" + ex.getMessage());
                LOG.debug("SQLException thrown during WKT geometry update : \n" + ex.getSQLState());

                SQLException exception = (SQLException) ex;
                while(exception != null) {
                    LOG.trace("SQLException expanded (see DEBUG) ");
                    LOG.debug("SQLException expanded : \n" + exception.getMessage());
                    LOG.debug("SQLException expanded : \n" + exception.getSQLState());
                }

                success = false;
            }
        }    

        return true;
    }
    */
    
    private boolean checkForWGS(final String sourceEndpoint, final String sourceGraph, final String restriction, final String sub) {
        boolean result = false;
        
        final String queryString = "ASK { ?s <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?o1 . ?s <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?o2 }";
        QueryExecution queryExecution = null;
        try {
            final Query query = QueryFactory.create(queryString);
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            //queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph, authenticator);
            System.out.println("source endpoint: " + sourceEndpoint + " query: " + query + "sourceGraph: " + sourceGraph);

            QueryEngineHTTP qeh = QueryExecutionFactory.createServiceRequest(sourceEndpoint, query, authenticator);
            qeh.addDefaultGraph(sourceGraph);
            //QueryExecution queryExecution = qeh;
            qeh.setSelectContentType(QueryEngineHTTP.supportedSelectContentTypes[3]);
            boolean rs = qeh.execAsk();

            System.out.println("Has WGS ------- " + rs);
            return rs;
        }
        catch (RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
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
        
        final String queryString = "ASK { ?os ?p1 _:a . _:a <http://www.opengis.net/ont/geosparql#asWKT> ?g }";
        QueryExecution queryExecution = null;
        try {
            final Query query = QueryFactory.create(queryString);
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            //queryExecution = QueryExecutionFactory.sparqlService(sourceEndpoint, query, sourceGraph, authenticator);
            System.out.println("source endpoint: " + sourceEndpoint + " query: " + query + "sourceGraph: " + sourceGraph);

            QueryEngineHTTP qeh = QueryExecutionFactory.createServiceRequest(sourceEndpoint, query, authenticator);
            qeh.addDefaultGraph(sourceGraph);
            //QueryExecution queryExecution = qeh;
            qeh.setSelectContentType(QueryEngineHTTP.supportedAskContentTypes[3]);
            System.out.println("ASK Type " + QueryEngineHTTP.supportedAskContentTypes[3]);

            boolean rs = qeh.execAsk();

            System.out.println("Has WKT ------- " + rs);
            return rs;
        } catch (RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
        } finally {
            if(queryExecution != null) {
                queryExecution.close();
            }
        }
        return result;
    }
    
    boolean insertGeometry(Connection dbConn, String node, String sourceGraph, String sourceEndpoint, int DS_ID) {
        boolean foundAtLeastOneTypeOfGeometry = false;
        String prevType = "NONE";
        final int pointPrec = Constants.GEOM_TYPE_PRECEDENCE_TABLE.get("POINT");
        final String restrictionForWgs = "<"+node+"> ?p1 ?o1 . <"+node+"> ?p2 ?o2 "
                + "FILTER(regex(?p1, \"" + Constants.LAT_REGEX + "\", \"i\"))"
                + "FILTER(regex(?p2, \"" + Constants.LONG_REGEX + "\", \"i\"))";
        
        final String restriction = "<"+node+"> ?p1 _:a . _:a <"+Constants.AS_WKT_REGEX+"> ?g ";
        final String queryString1 = "SELECT ?o1 ?o2 WHERE { GRAPH <"+sourceGraph+"> {" + restrictionForWgs + "} }";
        
        boolean countWgs = checkForWGS(sourceEndpoint, sourceGraph, restrictionForWgs, "?s");  
        boolean countWKT = checkForWKT(sourceEndpoint, sourceGraph, restriction, "?os");
        
        // Is at least one type of geometry existant
        foundAtLeastOneTypeOfGeometry = countWgs | countWKT;
                
        final String queryString;
        queryString = "SELECT ?g WHERE { GRAPH <"+sourceGraph+"> {" + restriction + " } }";
                
        QueryExecution queryExecution = null;
        PreparedStatement stmt = null;
        try {
            if (DS_ID == Constants.DATASET_A) {
                stmt = initGeomStatementA(DS_ID, dbConn);
            } else {
                stmt = initGeomStatementB(DS_ID, dbConn);
            }
        } catch (SQLException ex) {
            foundAtLeastOneTypeOfGeometry = false;
            
            return foundAtLeastOneTypeOfGeometry;
        }
        
        if (countWgs){ //if geosparql geometry doesn' t exist    
            try {
                if ( DS_ID == Constants.DATASET_A ) {
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
                    
                    System.out.println("THE OLD TYPE " + prevType + " " + Constants.GEOM_TYPE_PRECEDENCE_TABLE.get(prevType));
                    
                    if (Constants.GEOM_TYPE_PRECEDENCE_TABLE.get(prevType)
                            < pointPrec) {
                        continue;
                    }

                    //System.out.println("Subject "+subject);
                    final RDFNode objectNode1 = querySolution.get("?o1"); //lat
                    final RDFNode objectNode2 = querySolution.get("?o2"); //long

                    if (objectNode1.isLiteral() && objectNode2.isLiteral()) {
                        final double latitude = objectNode1.asLiteral().getDouble();
                        final double longitude = objectNode2.asLiteral().getDouble();

                        //construct wkt serialization
                        String geometry = "POINT (" + longitude + " " + latitude + ")";
                        //postGISImporter.loadGeometry(datasetIdent, subject, geometry);
                        addGeom(DS_ID, stmt, node, geometry);
                        prevType = "POINT";
                    } else {
                        //LOG.warn("Resource found where geometry serialisation literal expected.");
                    }
                    //if (callback != null )
                    //callback.publishGeometryProgress((int) (0.5 + (100 * (double) currentCount++ / (double) countWgs)));                                       
                }
                                
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
        
        if ( countWKT ) { //if geosparql geometry exists
            try {
                if ( DS_ID == Constants.DATASET_A ) {
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
                    
                    final RDFNode objectNode = querySolution.get("?g");
                    if (objectNode.isLiteral()) {
                        final String geometry = objectNode.asLiteral().getLexicalForm();
                        final String newType = geometry.substring(0, geometry.indexOf("("));
                        
                        if ( Constants.GEOM_TYPE_PRECEDENCE_TABLE.get(prevType) 
                            < Constants.GEOM_TYPE_PRECEDENCE_TABLE.get(newType) )
                            continue;
                        
                        addGeom(DS_ID, stmt, node, geometry);
                        prevType = newType;
                    } else {
                        //LOG.warn("Resource found where geometry serialisation literal expected.");
                    }
                    //if (callback != null )
                    //callback.publishGeometryProgress((int) (0.5 + (100 * (double) currentCount++ / (double) countWKT)));
                }
                
                //finishGeomUpload(DS_ID, stmt, dbConn);
                
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
        
        try {
            finishGeomUpload(DS_ID, stmt, dbConn);
        } catch (SQLException ex) {
        }
        
        if ( stmt != null ) {
            try {
                stmt.close();
            } catch (SQLException ex) {
            }
        }
        
        return foundAtLeastOneTypeOfGeometry;
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
