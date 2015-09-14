/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.google.common.collect.Maps;
import com.hp.hpl.jena.graph.BulkUpdateHandler;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
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
import gr.athenainnovation.imis.fusion.gis.postgis.DatabaseInitialiser;
import gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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
import java.util.logging.Logger;
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
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author nick
 */
@WebServlet(name = "LinksServlet", urlPatterns = {"/LinksServlet"})
@MultipartConfig
public class LinksServlet extends HttpServlet {

    private static final String SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FusionGISCLI.class);
    DBConfig dbConf;
    GraphConfig grConf;
    Boolean makeSwap;
    VirtGraph vSet = null;

    private class FAGILogger implements ErrorListener {

        @Override
        public void notifyError(String message) {
            System.out.println("ERROR:" + message);
        }

    }

    private static FAGILogger errListen;

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private boolean validateLinking(String lsub, String rsub) throws SQLException {
        Connection virt_conn = vSet.getConnection();
        PreparedStatement stmt;
        ResultSet rs;

        String checkA = "SELECT * WHERE { GRAPH <" + grConf.getGraphA() + "> {<" + lsub + "> ?p ?o } }";
        String checkB = "SELECT * WHERE { GRAPH <" + grConf.getGraphB() + "> {<" + lsub + "> ?p ?o } }";
        System.out.println("Found in A : " + checkA + " B : " + checkB);
        System.out.println("Left sub : " + lsub + " Right sub : " + rsub);
        
        HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
        //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
        QueryEngineHTTP qeh =  QueryExecutionFactory.createServiceRequest(grConf.getEndpointA(), QueryFactory.create(checkA), authenticator);
        //qeh.addDefaultGraph(grConf.getGraphA());
        //QueryExecution queryExecution = qeh;
        qeh.setSelectContentType(QueryEngineHTTP.supportedSelectContentTypes[3]);
        System.out.println(QueryEngineHTTP.supportedSelectContentTypes[3]);
        com.hp.hpl.jena.query.ResultSet resultSet = qeh.execSelect();
            
        boolean foundInA = false;
        boolean foundInB = false;
        while (resultSet.hasNext()) {
            foundInA = true;
            break;
        }
        
        qeh.close();
        
        qeh =  QueryExecutionFactory.createServiceRequest(grConf.getEndpointB(), QueryFactory.create(checkB), authenticator);
        qeh.setSelectContentType(QueryEngineHTTP.supportedSelectContentTypes[3]);
        //qeh = new QueryEngineHTTP(grConf.getEndpointA(), checkA, authenticator);
        //qeh.addDefaultGraph(grConf.getGraphB());
        //queryExecution = qeh;
        resultSet = qeh.execSelect();
        
        while (resultSet.hasNext()) {
            foundInB = true;
            break;
        }

        qeh.close();

        System.out.println("Found in A : " + foundInA + " B : " + foundInB);
        if (foundInA) {
            return false;
        } else {
            return true;
        }
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
            throws ServletException, IOException, SQLException, InterruptedException, JWNLException, ExecutionException, Exception {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            HttpSession sess = request.getSession(true);
            grConf = (GraphConfig) sess.getAttribute("gr_conf");
            dbConf = (DBConfig) sess.getAttribute("db_conf");
            //String ret = createBulkLoadDir((String)sess.getAttribute("bulk"));
            //dbConf.setBulkDir(ret);

            System.out.println(dbConf.getDBName());

            try {
                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                System.out.println(connEx.getMessage());
                out.println("Connection to virtuoso failed");
                out.close();

                return;
            }

            Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
            String filename = getFilename(filePart);
            InputStream filecontent = filePart.getInputStream();

    //Scanner sc = new Scanner(filecontent);
            //System.out.println("Filename "+filename);
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
                makeSwap = validateLinking(nodeA, nodeB);

                break;
            }
            iter.close();

            iter = model.listStatements();
            while (iter.hasNext()) {
                final Statement statement = iter.nextStatement();
                final String nodeA = statement.getSubject().getURI();
            //System.out.println(nodeA);
                //System.in.r;
                final String nodeB;
                final RDFNode object = statement.getObject();
                if (object.isResource()) {
                    nodeB = object.asResource().getURI();
                    //System.out.println(nodeB);
                } else {
                    nodeB = "";
                    //throw new ParseException("Failed to parse link (object not a resource): " + statement.toString(), 0);
                }

                if (!makeSwap) {
                    if ( linksHashed.containsKey(nodeA) )
                        continue;
                } else {
                    if ( linksHashed.containsKey(nodeB) )
                        continue;
                }
                
                Link l;
                //System.out.println("Make swap is " + makeSwap);
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
            int i = 0;
            for (Link l : output) {
                fetchedGeomsB.add(l.getNodeB());
                fetchedGeomsA.add(l.getNodeA());
                fetchedGeomsA.add(l.getNodeB());
                String check = "chk" + i;
                out.println("<li><div>");
                out.println("<label for=\"" + check + "\"><input type=\"checkbox\" value=\"\"name=\"" + check + "\" id=\"" + check + "\" />" + l.getNodeA() + "<-->" + l.getNodeB() + "</label>");
                out.println("</div>\n</li>");
                i++;
            }
            out.print("+>>>+");

            final GeometryFuser geometryFuser = new GeometryFuser();
            try {
                geometryFuser.connect(dbConf);
                geometryFuser.loadLinks(output);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            } finally {
                geometryFuser.clean();
            }

            createLinksGraph(output, vSet.getConnection(), dbConf.getBulkDir());

            //final ImporterWorker datasetAImportWorker = new ImporterWorker(dbConfig, PostGISImporter.DATASET_A, sourceDatasetA, datasetAStatusField, errorListener);
            Dataset sourceADataset = new Dataset(grConf.getEndpointA(), grConf.getGraphA(), "");
            final ImporterWorker datasetAImportWorker = new ImporterWorker(dbConf, PostGISImporter.DATASET_A, sourceADataset, null, errListen);
            datasetAImportWorker.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("progress".equals(evt.getPropertyName())) {
                        //System.out.println("Tom");
                    }
                }
            });

            Dataset sourceBDataset = new Dataset(grConf.getEndpointB(), grConf.getGraphB(), "");
            final ImporterWorker datasetBImportWorker = new ImporterWorker(dbConf, PostGISImporter.DATASET_B, sourceBDataset, null, errListen);
            datasetBImportWorker.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("progress".equals(evt.getPropertyName())) {
                        //System.out.println("Tom2");
                    }
                }
            });

            //startTime = System.nanoTime();
            System.out.println("Execute");
            datasetAImportWorker.execute();
            datasetBImportWorker.execute();
            System.out.println("Print");
            datasetAImportWorker.get();
            datasetBImportWorker.get();

                //endTime = System.nanoTime();
            VirtuosoImporter virtImp = new VirtuosoImporter(dbConf, null, (String) sess.getAttribute("t_graph"), true, grConf);
            Connection virt_conn = vSet.getConnection();

            sess.setAttribute("virt_imp", virtImp);
            virtImp.createLinksGraph(output);

            //virtImp.importGeometriesToVirtuoso((String) sess.getAttribute("t_graph"));
            virtImp.insertLinksMetadataChains(output, (String) sess.getAttribute("t_graph"), true);
            final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/all_links_" + dbConf.getDBName() + ">";

            String fetchFiltersA = "";
            String fetchFiltersB = "";
            if (grConf.isDominantA()) {
                fetchFiltersA = "sparql select distinct(?o1) where { GRAPH <http://localhost:8890/DAV/all_links_" + dbConf.getDBName() + "> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } . GRAPH <" + (String) sess.getAttribute("t_graph") + "_" + dbConf.getDBName() + "A> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o1 } }";
                fetchFiltersB = "sparql select distinct(?o1) where { GRAPH <http://localhost:8890/DAV/all_links_" + dbConf.getDBName() + "> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } . GRAPH <" + (String) sess.getAttribute("t_graph") + "_" + dbConf.getDBName() + "B> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o1 } }";
            } else {
                fetchFiltersA = "sparql select distinct(?o1) where { GRAPH <http://localhost:8890/DAV/all_links_" + dbConf.getDBName() + "> { ?o <http://www.w3.org/2002/07/owl#sameAs> ?s } . GRAPH <" + (String) sess.getAttribute("t_graph") + "_" + dbConf.getDBName() + "A> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o1 } }";
                fetchFiltersB = "sparql select distinct(?o1) where { GRAPH <http://localhost:8890/DAV/all_links_" + dbConf.getDBName() + "> { ?o <http://www.w3.org/2002/07/owl#sameAs> ?s } . GRAPH <" + (String) sess.getAttribute("t_graph") + "_" + dbConf.getDBName() + "B> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?o1 } }";
            }

            System.out.println(fetchFiltersA);
            System.out.println(fetchFiltersB);
            System.out.println(grConf.getGraphA());
            System.out.println(grConf.getGraphB());

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
                        out.println("<option value=\"" + prop + "\">" + prop + "</option>");
                        //System.out.println(prop);
                    }
                }
            }
            rs.close();
            filtersStmt.close();
            System.out.println("Printing out");
            System.out.println(out.toString());
            out.print("+>>>+");

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
        //HttpSession sess = request.getSession(true);
            //sess.setAttribute("linksList", output );

            System.out.println(out.toString());
        } finally {
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

    private String createBulkLoadDir(String dir) throws IOException {
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

    public void createLinksGraph(List<Link> lst, Connection virt_conn, String bulkInsertDir) throws SQLException, IOException {
        final String dropGraph = "sparql DROP SILENT GRAPH <http://localhost:8890/DAV/all_links_" + dbConf.getDBName() + ">";
        final String createGraph = "sparql CREATE GRAPH <http://localhost:8890/DAV/all_links_" + dbConf.getDBName() + ">";
        //final String endDesc = "sparql LOAD SERVICE <"+endpointA+"> DATA";

        //PreparedStatement endStmt;
        //endStmt = virt_conn.prepareStatement(endDesc);
        //endStmt.execute();
        PreparedStatement dropStmt;
        long starttime, endtime;
        dropStmt = virt_conn.prepareStatement(dropGraph);
        dropStmt.execute();

        PreparedStatement createStmt;
        createStmt = virt_conn.prepareStatement(createGraph);
        createStmt.execute();
        
        //bulkInsertLinks(lst, virt_conn, bulkInsertDir);
        SPARQLInsertLink(lst);
    }

    private void SPARQLInsertLink(List<Link> l) {
        boolean updating = true;
        int addIdx = 0;
        int cSize = 1;
        int sizeUp = 1;
        while (updating) {
            try {
                ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                //queryStr.append("WITH <"+fusedGraph+"> ");
                queryStr.append("INSERT DATA { ");
                queryStr.append("GRAPH <http://localhost:8890/DAV/all_links_" + dbConf.getDBName() + "> { ");
                int top = 0;
                if (cSize >= l.size()) {
                    top = l.size();
                } else {
                    top = cSize;
                }
                for (int i = addIdx; i < top; i++) {
                    final String subject = l.get(i).getNodeA();
                    final String subjectB = l.get(i).getNodeB();
                    queryStr.appendIri(subject);
                    queryStr.append(" ");
                    queryStr.appendIri(SAME_AS);
                    queryStr.append(" ");
                    queryStr.appendIri(subjectB);
                    queryStr.append(" ");
                    queryStr.append(".");
                    queryStr.append(" ");
                }
                queryStr.append("} }");
                //System.out.println("Print "+queryStr.toString());

                UpdateRequest q = queryStr.asUpdate();
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
                insertRemoteB.execute();
                //System.out.println("Add at "+addIdx+" Size "+cSize);
                addIdx += (cSize - addIdx);
                sizeUp *= 2;
                cSize += sizeUp;
                if (cSize >= l.size()) {
                    cSize = l.size();
                }
                if (cSize == addIdx) {
                    updating = false;
                }
            } catch (org.apache.jena.atlas.web.HttpException ex) {
                System.out.println("Failed at " + addIdx + " Size " + cSize);
                System.out.println("Crazy Stuff");
                System.out.println(ex.getLocalizedMessage());
                ex.printStackTrace();
                ex.printStackTrace(System.out);
                sizeUp = 1;
                cSize = addIdx;
                cSize += sizeUp;
                if (cSize >= l.size()) {
                    cSize = l.size();
                }
                //System.out.println("Going back at "+addIdx+" Size "+cSize);

                break;
                //System.out.println("Going back at "+addIdx+" Size "+cSize);
            } catch (Exception ex) {
                System.out.println(ex.getLocalizedMessage());
                break;
            }
        }
    }

    private void bulkInsertLinks(List<Link> lst, Connection virt_conn, String bulkInsertDir) throws FileNotFoundException, SQLException {
        long starttime, endtime;
        /*
         set2 = getVirtuosoSet("http://localhost:8890/DAV/all_links", db_c.getDBURL(), db_c.getUsername(), db_c.getPassword());
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
                + "'" + "http://localhost:8890/DAV/all_links_" + dbConf.getDBName() + "')";
        //int stop = 0;
        if (lst.size() > 0) {

            for (Link link : lst) {
                //if (stop++ > 1000) break;
                String triple = "<" + link.getNodeA() + "> <" + SAME_AS + "> <" + link.getNodeB() + "> .";

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
            throws ServletException, IOException {
        try {
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
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JWNLException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(LinksServlet.class.getName()).log(Level.SEVERE, null, ex);
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
