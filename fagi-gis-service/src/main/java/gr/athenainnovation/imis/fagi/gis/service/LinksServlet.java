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
 * @author nick
 */
@WebServlet(name = "LinksServlet", urlPatterns = {"/LinksServlet"})
@MultipartConfig
public class LinksServlet extends HttpServlet {

    private static final Logger LOG = Log.getClassFAGILogger(LinksServlet.class);
    
    private boolean validateLinking(HttpSession sess, String lsub, String rsub, VirtGraph vSet, GraphConfig grConf) throws SQLException {
        Connection virt_conn = vSet.getConnection();
        PreparedStatement stmt;
        ResultSet rs;

        String checkA = "SELECT * WHERE { GRAPH <" + grConf.getGraphA() + "> {<" + lsub + "> ?p ?o } }";
        String checkB = "SELECT * WHERE { GRAPH <" + grConf.getGraphB() + "> {<" + lsub + "> ?p ?o } }";
        //System.out.println("Found in A : " + checkA + " B : " + checkB);
        //System.out.println("Left sub : " + lsub + " Right sub : " + rsub);
        
        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
        QueryEngineHTTP qeh =  QueryExecutionFactory.createServiceRequest(grConf.getEndpointA(), QueryFactory.create(checkA), authenticator);
        qeh.setSelectContentType((String)sess.getAttribute("content-type"));
        //System.out.println((String)sess.getAttribute("content-type"));
        com.hp.hpl.jena.query.ResultSet resultSet = qeh.execSelect();
            
        boolean foundInA = false;
        boolean foundInB = false;
        
        //System.out.println(ResultSetFormatter.asText(resultSet));
        //ResultSetFormatter.outputAsRDF(System.out, "RDF/XML", resultSet);

        if (resultSet.hasNext())
            foundInA = true;
        
        qeh.close();
        
        qeh = QueryExecutionFactory.createServiceRequest(grConf.getEndpointB(), QueryFactory.create(checkB), authenticator);
        qeh.setSelectContentType((String)sess.getAttribute("content-type"));
        resultSet = qeh.execSelect();
        
        if (resultSet.hasNext())
            foundInB = true;

        qeh.close();

        if (foundInA) {
            return false;
        } else {
            LOG.info("Need to swap link subjects order");
            return true;
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
            
            if ( sess == null ) {
                ret.getResult().setStatusCode(-1);
                ret.getResult().setMessage("Failed to get session");
                
                out.println(mapper.writeValueAsString(ret));
            
                out.close();
                
                return;
            }
            
            StringBuilder htmlCode = new StringBuilder();
            ret = new JSONLoadLinksResult();
            res = new JSONRequestResult();
            ret.setResult(res);
            
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
            InputStream filecontent;
            if (request.getContentType() != null) {
                Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
                String filename = getFilename(filePart);
                filecontent = filePart.getInputStream();
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
            
            sess.setAttribute("links", linksHashed);
            sess.setAttribute("links_list", output);
            int i = 0;
            for (Link l : output) {
                fetchedGeomsA.add(l.getNodeA());
                fetchedGeomsA.add(l.getNodeB());
                String check = "chk" + i;
                htmlCode.append("<li><div>");
                htmlCode.append("<label for=\"" + check + "\"><input type=\"checkbox\" value=\"\"name=\"" + check + "\" id=\"" + check + "\" />" + l.getNodeA() + "<-->" + l.getNodeB() + "</label>");
                htmlCode.append("</div>\n</li>");
                //out.println("<li><div>");
                //out.println("<label for=\"" + check + "\"><input type=\"checkbox\" value=\"\"name=\"" + check + "\" id=\"" + check + "\" />" + l.getNodeA() + "<-->" + l.getNodeB() + "</label>");
                //out.println("</div>\n</li>");
                i++;
            }
            
            ret.setLinkListHTML(htmlCode.toString());
            htmlCode.setLength(0);
            
            //out.print("+>>>+");

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
            createLinksGraph(output, vSet.getConnection(), grConf, dbConf.getBulkDir());

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
            datasetAImportWorker.get();
            datasetBImportWorker.get();

            VirtuosoImporter virtImp = new VirtuosoImporter(dbConf, null, (String) sess.getAttribute("t_graph"), true, grConf);
            Connection virt_conn = vSet.getConnection();
            
            // Recreate target temp graph
            final String dropTempGraph = "SPARQL DROP SILENT GRAPH <"+ grConf.getTargetTempGraph()+  ">";
            final String createTempGraph = "SPARQL CREATE GRAPH <"+ grConf.getTargetTempGraph()+ ">";

            try {
                PreparedStatement stmt;
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
            }
            
            sess.setAttribute("virt_imp", virtImp);
            virtImp.createLinksGraph(output);

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

            //System.out.println("Fetch from graph A : "+fetchFiltersA);
            //System.out.println("Fetch from graph B : " + fetchFiltersB);
            //System.out.println("Graph A : " + grConf.getGraphA());
            //System.out.println("Graph B : " + grConf.getGraphB());

            PreparedStatement filtersStmt;
            filtersStmt = virt_conn.prepareStatement(fetchFiltersA);
            ResultSet rs = filtersStmt.executeQuery();

            if (rs.isBeforeFirst()) {
                if (rs.next()) {
                    String prop = rs.getString(1);
                    out.println("<option value=\"" + prop + "\" selected=\"selected\">" + prop + "</option>");
                    //System.out.println(prop);
                    while (rs.next()) {
                        prop = rs.getString(1);
                        htmlCode.append("<option value=\"" + prop + "\">" + prop + "</option>");
                        //out.println("<option value=\"" + prop + "\">" + prop + "</option>");
                        //System.out.println(prop);
                    }
                }
            }
            
            rs.close();
            filtersStmt.close();
            
            ret.setFiltersListAHTML(htmlCode.toString());
            htmlCode.setLength(0);
            
            //out.print("+>>>+");

            filtersStmt = virt_conn.prepareStatement(fetchFiltersB);
            rs = filtersStmt.executeQuery();

            if (rs.isBeforeFirst()) {
                if (rs.next()) {
                    String prop = rs.getString(1);
                    out.println("<option value=\"" + prop + "\" selected=\"selected\">" + prop + "</option>");
                    //System.out.println(prop);
                    while (rs.next()) {
                        prop = rs.getString(1);
                        out.println("<option value=\"" + prop + "\">" + prop + "</option>");
                        //System.out.println(prop);
                    }
                }
            }
            
            ret.setFiltersListBHTML(htmlCode.toString());
            
            rs.close();
            filtersStmt.close();
            
        } catch (JsonProcessingException ex) {
            LOG.trace("JsonProcessingException thrown");
            LOG.debug("JsonProcessingException thrown : " + ex.getMessage());
            out.println("{}");

            out.close();

            return;
        } finally {
            if ( vSet != null ) {
                vSet.close();
            }
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

    public boolean createLinksGraph(List<Link> lst, Connection virt_conn, GraphConfig grConf, String bulkInsertDir) {
        final String dropGraph = "sparql DROP SILENT GRAPH <"+ grConf.getAllLinksGraph()+  ">";
        final String createGraph = "sparql CREATE GRAPH <"+ grConf.getAllLinksGraph()+  ">";
        
        boolean success = true;

        //final String endDesc = "sparql LOAD SERVICE <"+endpointA+"> DATA";

        //PreparedStatement endStmt;
        //endStmt = virt_conn.prepareStatement(endDesc);
        //endStmt.execute();
        PreparedStatement dropStmt;
        long starttime, endtime;
        try {
            dropStmt = virt_conn.prepareStatement(dropGraph);
            dropStmt.execute();
            dropStmt.close();
            
        } catch (SQLException ex) {
            LOG.trace("Dropping "+grConf.getAllLinksGraph()+" failed");
            LOG.debug("Dropping "+grConf.getAllLinksGraph()+" failed");
            
            success = false;
            return success;
        }

        PreparedStatement createStmt;
        try {
            createStmt = virt_conn.prepareStatement(createGraph);
            createStmt.execute();
            createStmt.close();
            
        } catch (SQLException ex) {
            LOG.trace("Creating "+grConf.getAllLinksGraph()+" failed");
            LOG.debug("Creating "+grConf.getAllLinksGraph()+" failed");
            
            success = false;
            return success;
        }
        
        
        //bulkInsertLinks(lst, virt_conn, bulkInsertDir);
        success = SPARQLInsertLink(lst, grConf, virt_conn);
        
        return success;
    }

    private boolean SPARQLInsertLink(List<Link> l, GraphConfig grConf, Connection virt_conn) {
        boolean success = true;
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("SPARQL WITH <" + grConf.getAllLinksGraph() + "> INSERT {");
            sb.append("`iri(??)` <" + Constants.SAME_AS + "> `iri(??)` . } ");
            System.out.println("Statement " + sb.toString());
            VirtuosoConnection conn = (VirtuosoConnection) virt_conn;
            VirtuosoPreparedStatement vstmt;
            vstmt = (VirtuosoPreparedStatement) conn.prepareStatement(sb.toString());

            int start = 0;
            int end = l.size();

            for (int i = start; i < end; ++i) {
                Link link = l.get(i);
                vstmt.setString(1, link.getNodeA());
                vstmt.setString(2, link.getNodeB());

                vstmt.addBatch();
            }

            vstmt.executeBatch();

            vstmt.close();

        } catch (VirtuosoException ex) {
            LOG.trace("VirtuosoException on "+grConf.getAllLinksGraph()+" failed");
            LOG.debug("VirtuosoException on "+grConf.getAllLinksGraph()+" failed : " + ex.getMessage());
            LOG.debug("VirtuosoException on "+grConf.getAllLinksGraph()+" failed : " + ex.getSQLState());
            
            success = false;
            return success;
        } catch (BatchUpdateException ex) {
            LOG.trace("BatchUpdateException on "+grConf.getAllLinksGraph()+" failed");
            LOG.debug("BatchUpdateException on "+grConf.getAllLinksGraph()+" failed : " + ex.getMessage());
            LOG.debug("BatchUpdateException on "+grConf.getAllLinksGraph()+" failed : " + ex.getSQLState());
            
            success = false;
            return success;
        }
        
        return success;
    }

    private void bulkInsertLinks(List<Link> lst, Connection virt_conn, GraphConfig grConf, String bulkInsertDir) throws FileNotFoundException, SQLException {
        long starttime, endtime;
        /*
         set2 = getVirtuosoSet("+ grConf.getAllLinksGraph()+ , db_c.getDBURL(), db_c.getUsername(), db_c.getPassword());
         BulkUpdateHandler buh2 = set2.getBulkUpdateHandler();*/
        LOG.info(ANSI_YELLOW + "Loaded " + lst.size() + " links" + ANSI_RESET);

        starttime = System.nanoTime();
        System.out.println("FILE " + bulkInsertDir + "bulk_inserts" + File.separator + "selected_links.nt");
        //File f = new File(bulkInsertDir+"bulk_inserts/selected_links.nt");
        //f.mkdirs();
        //f.getParentFile().mkdirs();
        //PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(bulkInsertDir+"bulk_inserts/selected_links.nt")));
        String dir = bulkInsertDir.replace("\\", "/");
        System.out.println("DIR " + dir);
        //dir = "/"+dir;
        //dir = dir.replace(":","");
        PrintWriter out = new PrintWriter(bulkInsertDir + "bulk_inserts/selected_links.nt");
        final String bulk_insert = "DB.DBA.TTLP_MT (file_to_string_output ('" + dir + "bulk_inserts/selected_links.nt'), '', "
                + "'" + grConf.getAllLinksGraph()+ "')";
        //int stop = 0;
        if (lst.size() > 0) {

            for (Link link : lst) {
                //if (stop++ > 1000) break;
                String triple = "<" + link.getNodeA() + "> <" + Constants.SAME_AS + "> <" + link.getNodeB() + "> .";

                out.println(triple);
            }
            out.close();

            PreparedStatement uploadBulkFileStmt;
            uploadBulkFileStmt = virt_conn.prepareStatement(bulk_insert);
            uploadBulkFileStmt.executeUpdate();
        }

        endtime = System.nanoTime();
        LOG.info(ANSI_YELLOW + "Links Graph created in " + ((endtime - starttime) / 1000000000f) + "" + ANSI_RESET);

        starttime = System.nanoTime();

        virt_conn.commit();
        //endtime =  System.nanoTime();
        LOG.info(ANSI_YELLOW + "Links Graph created in " + ((endtime - starttime) / 1000000000f) + "" + ANSI_RESET);
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
            throws ServletException {
            processRequest(request, response);
        /*try {
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JWNLException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        }*/
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
            processRequest(request, response);
        /*try {
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JWNLException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        }*/
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
