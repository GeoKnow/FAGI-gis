/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import static com.hp.hpl.jena.query.ResultSetFactory.result;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import static com.vividsolutions.jts.triangulate.quadedge.QuadEdgeTriangle.nextIndex;
import gr.athenainnovation.imis.fusion.gis.cli.FusionGISCLI;
import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.listeners.ErrorListener;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.Dataset;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_RESET;
import static gr.athenainnovation.imis.fusion.gis.gui.workers.FusionState.ANSI_YELLOW;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.ImporterWorker;
import gr.athenainnovation.imis.fusion.gis.json.JSONLoadLinksResult;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import gr.athenainnovation.imis.fusion.gis.utils.SPARQLUtilities;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import net.didion.jwnl.JWNLException;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;
import virtuoso.jdbc4.VirtuosoConnection;
import virtuoso.jdbc4.VirtuosoException;
import virtuoso.jdbc4.VirtuosoPreparedStatement;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "LinksServlet", urlPatterns = {"/LinksServlet"})
@MultipartConfig
public class LinksServlet extends HttpServlet {

    private static final Logger LOG = Log.getClassFAGILogger(LinksServlet.class);
    
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

    /**
     * Fetch links from provided endpoint and store them in memory
     *
     * @param sess servlet session
     * @param e endpoint URL
     * @param g graph name
     * @return InputStream to read links
     */
    private InputStream fetchLinkFromEndpoint(HttpSession sess, String e, String g) {
        StringBuilder sb = new StringBuilder(1024);
        // This query remives the limitation of having only SAME_AS predicates
        // but we will need additional checks
        String q = "SELECT * WHERE { GRAPH <"+g+"> { ?s ?p ?o } }";
        
        try {
            final Query query = QueryFactory.create(q);
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            QueryEngineHTTP qeh = QueryExecutionFactory.createServiceRequest(e, QueryFactory.create(query), authenticator);
            qeh.setSelectContentType((String) sess.getAttribute("content-type"));
            final com.hp.hpl.jena.query.ResultSet resultSet = qeh.execSelect();

            while (resultSet.hasNext()) {
                final QuerySolution querySolution = resultSet.next();
                final RDFNode subject = querySolution.get("?s");
                final RDFNode objectNode1 = querySolution.get("?p"); //lat
                final RDFNode objectNode2 = querySolution.get("?o"); //long

                sb.append("<" + subject + ">");
                sb.append(" ");
                sb.append("<" + Constants.SAME_AS + ">");
                sb.append(" ");
                sb.append("<" + objectNode2 + ">");
                sb.append(" . ");
            }
        } catch (QueryException qex) {
            LOG.trace("QueryException thrown");
            LOG.debug("QueryException thrown : \n" + qex.getMessage() );
            
            return null;
        }
        
        // Return UTF-8 formatted byte array
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
    
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
            throws ServletException {
        response.setContentType("text/html;charset=UTF-8");
        
        // Per request state
        PrintWriter             out = null;
        DBConfig                dbConf = null;
        GraphConfig             grConf =  null;
        Boolean                 makeSwap = false;
        VirtGraph               vSet = null;
        ObjectMapper            mapper = new ObjectMapper();
        HttpSession             sess;
        JSONLoadLinksResult     ret;
        JSONRequestResult       res;
        boolean                 succeeded = false;
        
        try {
            try {
                out = response.getWriter();
            } catch (IOException ex) {
                LOG.trace("IOException thrown in servlet Writer");
                LOG.debug("IOException thrown in servlet Writer : \n" + ex.getMessage() );
                
                return;
            }
            sess = request.getSession(false);
            
            StringBuilder htmlCode = new StringBuilder();
            ret = new JSONLoadLinksResult();
            res = new JSONRequestResult();
            ret.setResult(res);
            
            if ( sess == null ) {
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Failed to get session");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }
            
            grConf = (GraphConfig) sess.getAttribute("gr_conf");
            dbConf = (DBConfig) sess.getAttribute("db_conf");
            for ( String s : QueryEngineHTTP.supportedSelectContentTypes) {
                if ( s.contains("xml"))
                    sess.setAttribute("content-type", s);
            }
            try {
                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                LOG.trace("JenaException thrown");
                LOG.debug("JenaException thrown : \n" + connEx.getMessage() );
                //out.println("Connection to virtuoso failed");
                //out.close();
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Connection to virtuoso failed");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
            
                return;
            }
            
            // Checking Content Type allows to know if there is a file provided
            InputStream filecontent = null;
            if (request.getContentType() != null) {
                Part filePart;
                try {
                    filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
                    String filename = getFilename(filePart);
                    filecontent = filePart.getInputStream();
                } catch (IOException ex) {
                    LOG.trace("IOException thrown during file upload");
                    LOG.debug("IOException thrown during file upload : \n" + ex.getMessage());

                    ret.getResult().setStatusCode(-1);
                    ret.getResult().setMessage("File Upload failed");

                    out.println(mapper.writeValueAsString(ret));

                    out.close();

                    return;
                }
            } else {
                filecontent = fetchLinkFromEndpoint(sess, grConf.getEndpointL(), grConf.getGraphL());
            }
            
            if ( filecontent == null ) {
                LOG.trace("NULL InputStream");
                LOG.debug("NULL InputStream");
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem creating input stream for links");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
            }
            
            // Helps to keep links as both a List and Ma ap
            List<Link> output = new ArrayList<Link>();
            HashMap<String, String> linksHashed = (HashMap<String, String>)sess.getAttribute("links");
            
            if ( linksHashed == null )
                linksHashed = Maps.newHashMap();
            
            linksHashed.clear();
                    
            Model model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, filecontent, "", Lang.NTRIPLES);
            StmtIterator iter = model.listStatements();
            while (iter.hasNext()) {
                final Statement statement = iter.nextStatement();
                String nodeA = statement.getSubject().getURI();
                String nodeB = "";
                final RDFNode object = statement.getObject();
                if (object.isResource()) {
                    nodeB = object.asResource().getURI();
                }
                makeSwap = validateLinking(sess, nodeA, nodeB, vSet, grConf);

                break;
            }
            iter.close();

            iter = model.listStatements();
            while (iter.hasNext()) {
                final Statement statement = iter.nextStatement();
                final String nodeA = statement.getSubject().getURI();
            
                final String nodeB;
                final RDFNode object = statement.getObject();
                if (object.isResource()) {
                    nodeB = object.asResource().getURI();
                } else {
                    nodeB = "";
                }

                if (!makeSwap) {
                    if ( linksHashed.containsKey(nodeA) )
                        continue;
                } else {
                    if ( linksHashed.containsKey(nodeB) )
                        continue;
                }
                
                Link l;
                if (!makeSwap) {
                    l = new Link(nodeA, nodeB);
                    linksHashed.put(nodeA, nodeB);
                } else {
                    l = new Link(nodeB, nodeA);
                    linksHashed.put(nodeB, nodeA);
                }

                //System.out.println(nodeA+" linked with "+nodeB);
                output.add(l);
            }
            
            HashSet<String> fetchedGeomsA = (HashSet<String>) sess.getAttribute("fetchedGeomsA");
            HashSet<String> fetchedGeomsB = (HashSet<String>) sess.getAttribute("fetchedGeomsB");
            
            if ( fetchedGeomsA == null ) {
                fetchedGeomsA = new HashSet<>();
                sess.setAttribute("fetchedGeomsA", fetchedGeomsA);
            }
            
            if ( fetchedGeomsB == null ) {
                fetchedGeomsB = new HashSet<>();
                sess.setAttribute("fetchedGeomsB", fetchedGeomsB);
            }
            
            fetchedGeomsA.clear();
            fetchedGeomsB.clear();
            
            // Save both containers of links
            sess.setAttribute("links", linksHashed);
            sess.setAttribute("links_list", output);
            
            // Create the HTML for the link list
            // [TODO] Could be inproved ( Too much work on the server )
            int i = 0;
            for (Link l : output) {
                fetchedGeomsA.add(l.getNodeA());
                fetchedGeomsA.add(l.getNodeB());
                String check = "chk" + i;
                htmlCode.append("<li><div>");
                htmlCode.append("<label for=\"" + check + "\"><input type=\"checkbox\" value=\"\"name=\"" + check + "\" id=\"" + check + "\" />" + l.getNodeA() + "<-->" + l.getNodeB() + "</label>");
                htmlCode.append("</div>\n</li>");
                i++;
            }
            
            // Set the links HTML and reset buffer
            ret.setLinkListHTML(htmlCode.toString());
            htmlCode.setLength(0);
            

            // Upload Links to PostGIS
            final GeometryFuser geometryFuser = new GeometryFuser();
                
            succeeded = geometryFuser.connect(dbConf);
            if ( !succeeded ) {
                LOG.trace("Connection for link upload failed");
                LOG.debug("Connection for link upload failed");
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem connecting to PostGIS for link upload");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }
            
            succeeded = geometryFuser.loadLinks(output);
            if ( !succeeded ) {
                LOG.trace("Link upload failed");
                LOG.debug("Link upload failed");
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem with PostGIS link upload");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }
            
            succeeded = geometryFuser.clean();
            if ( !succeeded ) {
                LOG.trace("Cleanup failed");
                LOG.debug("Cleanup failed");
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem with link upload cleanup");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }

            // Upload links to Virtusoso graph
            succeeded = SPARQLUtilities.createLinksGraph(output, vSet.getConnection(), grConf, dbConf.getBulkDir());
            if ( !succeeded ) {
                LOG.trace("Link graph creation failed");
                LOG.debug("Link graph creation failed");
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem with link upload to Virtuoso");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }
            //final ImporterWorker datasetAImportWorker = new ImporterWorker(dbConfig, PostGISImporter.DATASET_A, sourceDatasetA, datasetAStatusField, errorListener);
            Dataset sourceADataset = new Dataset(grConf.getEndpointA(), grConf.getGraphA(), "");
            final ImporterWorker datasetAImportWorker = new ImporterWorker(dbConf, grConf, PostGISImporter.DATASET_A, sourceADataset, null, null);
            datasetAImportWorker.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("progress".equals(evt.getPropertyName())) {
                        //System.out.println("Tom");
                    }
                }
            });

            Dataset sourceBDataset = new Dataset(grConf.getEndpointB(), grConf.getGraphB(), "");
            final ImporterWorker datasetBImportWorker = new ImporterWorker(dbConf, grConf, PostGISImporter.DATASET_B, sourceBDataset, null, null);
            datasetBImportWorker.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("progress".equals(evt.getPropertyName())) {
                        //System.out.println("Tom2");
                    }
                }
            });

            //startTime = System.nanoTime();
            //System.out.println("Execute");
            datasetAImportWorker.execute();
            datasetBImportWorker.execute();
            //System.out.println("Print");
            Boolean retA, retB;
            try {
                retB = datasetAImportWorker.get();
                retA = datasetBImportWorker.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOG.trace("Thread execution failed");
                LOG.debug("Thread execution failed");
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Upload thread execution failed");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }
            System.out.println("The bool " + retA);
            System.out.println(retB);
            if ( ( retA == false ) || ( retB == false ) ) {
                LOG.trace("Thread execution failed");
                LOG.debug("Thread execution failed");
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem with link upload cleanup");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }
                LOG.trace("SQLException after");
            
            VirtuosoImporter virtImp = new VirtuosoImporter(dbConf, null, (String) sess.getAttribute("t_graph"), true, grConf);
            Connection virt_conn = vSet.getConnection();
                LOG.trace("SQLException before");
            // Recreate target temp graph
            final String dropTempGraph = "SPARQL DROP SILENT GRAPH <"+ grConf.getTargetTempGraph()+  ">";
            final String createTempGraph = "SPARQL CREATE GRAPH <"+ grConf.getTargetTempGraph()+ ">";

            PreparedStatement stmt = null;
            try {
                stmt = virt_conn.prepareStatement(dropTempGraph);
                stmt.execute();

                stmt = virt_conn.prepareStatement(dropTempGraph);
                stmt.execute();

                stmt.close();

            } catch (SQLException ex) {
                LOG.trace("SQLException thrown");
                LOG.debug("SQLException thrown : "+ex.getMessage());
                LOG.debug("SQLException thrown : "+ex.getSQLState());
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem with destroying Target Temporary graph");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            } finally {
                try {
                    if ( stmt != null )
                        stmt.close();
                } catch (SQLException ex) {
                    LOG.trace("SQLException thrown during statement close");
                    LOG.debug("SQLException thrown during statement close : "+ex.getMessage());
                    LOG.debug("SQLException thrown during statement close : "+ex.getSQLState());
                }
            }
            
            sess.setAttribute("virt_imp", virtImp);
            //virtImp.createLinksGraph(output);

            System.out.println(mapper.writeValueAsString(ret));
            //virtImp.importGeometriesToVirtuoso((String) sess.getAttribute("t_graph"));
            virtImp.insertLinksMetadataChains(output, (String) sess.getAttribute("t_graph"), true);
            final String createGraph = "sparql CREATE GRAPH <"+ grConf.getAllLinksGraph()+  ">";

            String fetchFiltersA;
            String fetchFiltersB;
            if (grConf.isDominantA()) {
                fetchFiltersA = "SPARQL SELECT distinct(?o1) WHERE { GRAPH <"+ grConf.getAllLinksGraph()+ "> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } . GRAPH <" + grConf.getMetadataGraphA() +"> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o1 } }";
                fetchFiltersB = "SPARQL SELECT distinct(?o1) WHERE { GRAPH <"+ grConf.getAllLinksGraph()+ "> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } . GRAPH <" + grConf.getMetadataGraphB() + "> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o1 } }";
            } else {
                fetchFiltersA = "SPARQL SELECT distinct(?o1) WHERE { GRAPH <"+ grConf.getAllLinksGraph()+ "> { ?o <http://www.w3.org/2002/07/owl#sameAs> ?s } . GRAPH <" + grConf.getMetadataGraphA() + "> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o1 } }";
                fetchFiltersB = "SPARQL SELECT distinct(?o1) WHERE { GRAPH <"+ grConf.getAllLinksGraph()+ "> { ?o <http://www.w3.org/2002/07/owl#sameAs> ?s } . GRAPH <" + grConf.getMetadataGraphB() + "> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o1 } }";
            }

            System.out.println("Fetch from graph A : "+fetchFiltersA);
            System.out.println("Fetch from graph B : " + fetchFiltersB);
            System.out.println("Graph A : " + grConf.getGraphA());
            System.out.println("Graph B : " + grConf.getGraphB());

            PreparedStatement filtersStmt = null;
            ResultSet rs = null;
            try {
                filtersStmt = virt_conn.prepareStatement(fetchFiltersA);
                rs = filtersStmt.executeQuery();

                if (rs.isBeforeFirst()) {
                    if (rs.next()) {
                        String prop = rs.getString(1);
                        htmlCode.append("<option value=\"" + prop + "\" selected=\"selected\">" + prop + "</option>");
                        //System.out.println(prop);
                        while (rs.next()) {
                            prop = rs.getString(1);
                            htmlCode.append("<option value=\"" + prop + "\">" + prop + "</option>");
                            System.out.println(prop);
                        }
                    }
                }

                ret.setFiltersListAHTML(htmlCode.toString());
                htmlCode.setLength(0);

                filtersStmt = virt_conn.prepareStatement(fetchFiltersB);
                rs = filtersStmt.executeQuery();

                if (rs.isBeforeFirst()) {
                    if (rs.next()) {
                        String prop = rs.getString(1);
                        htmlCode.append("<option value=\"" + prop + "\" selected=\"selected\">" + prop + "</option>");
                        //System.out.println(prop);
                        while (rs.next()) {
                            prop = rs.getString(1);
                            htmlCode.append("<option value=\"" + prop + "\">" + prop + "</option>");
                            System.out.println(prop);
                        }
                    }
                }

                ret.setFiltersListBHTML(htmlCode.toString());

            } catch (SQLException ex) {
                LOG.trace("SQLException thrown");
                LOG.debug("SQLException thrown : " + ex.getMessage());
                LOG.debug("SQLException thrown : " + ex.getSQLState());
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Problem with destroying Target Temporary graph");

                out.println(mapper.writeValueAsString(ret));

                out.close();

                return;
            } finally {
                try {
                    if ( rs != null ) 
                        rs.close();
                    if (stmt != null) {
                        stmt.close();
                    }
                } catch (SQLException ex) {
                    LOG.trace("SQLException thrown during statement and result set close");
                    LOG.debug("SQLException thrown during statement and result set close : " + ex.getMessage());
                    LOG.debug("SQLException thrown during statement and result set close : " + ex.getSQLState());
                }
            }
            
            LOG.trace("Thread execution failed");
            ret.getResult().setStatusCode(0);
            ret.getResult().setMessage("Link fetching done");
                
            out.println(mapper.writeValueAsString(ret));
            
        } catch (JsonProcessingException ex) {
            LOG.trace("JsonProcessingException thrown");
            LOG.debug("JsonProcessingException thrown : " + ex.getMessage());
            if (out != null) {
                out.println("{}");

                out.close();
            }
        } catch ( java.lang.OutOfMemoryError oome) {
            LOG.trace("OutOfMemoryError thrown");
            LOG.debug("OutOfMemoryError thrown : " + oome.getMessage());
            if (out != null) {
                out.println("{}");

                out.close();
            }
        } finally {
            if ( vSet != null ) {
                vSet.close();
            }
            
            if ( out != null )
                out.close();
        }
    }

    private static String getFilename(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1); // MSIE fix.
            }
        }
        return null;
    }

    private String createBulkLoadDir(String dir) {
        //dir = dir.replace("\\", "/");
        //dir = "/"+dir;
        //dir = dir.replace(":","");
        System.out.println("Seps " + dir + " " + File.separator + " " + File.separatorChar);
        String ret = dir;
        int lastSlash = dir.lastIndexOf(File.separator);
        if (lastSlash != (dir.length() - 1)) {
            System.out.println("Isxuei");
            ret = dir.concat(File.separator);
        }
        System.out.println("Seps " + ret + " " + File.separator + " " + File.separatorChar);
        File file = new File(ret);
        if (!file.exists()) {
            System.out.println("creating directory: " + ret);
            boolean result = false;

            try {
                file.mkdir();
                result = true;
            } catch (SecurityException se) {
                //handle it
            }
            if (result) {
                System.out.println("DIR created");
            }
        }

        file = new File(ret + "bulk_inserts/");
        if (!file.exists()) {
            System.out.println("creating directory: " + ret);
            boolean result = false;

            try {
                file.mkdir();
                result = true;
            } catch (SecurityException se) {
                //handle it
            }
            if (result) {
                System.out.println("DIR created");
            }
        }
        return ret;
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            processRequest(request, response);
        } catch (ServletException ex) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            } catch (IOException ex1) {
                
            }
            
            LOG.trace("ServletException thrown");
            LOG.debug("ServletException thrown : " + ex.getMessage());
            
            throw ex;
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            processRequest(request, response);
        } catch (ServletException ex) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            } catch (IOException ex1) {
                
            }
            
            LOG.trace("ServletException thrown");
            LOG.debug("ServletException thrown : " + ex.getMessage());
            
            throw ex;
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
