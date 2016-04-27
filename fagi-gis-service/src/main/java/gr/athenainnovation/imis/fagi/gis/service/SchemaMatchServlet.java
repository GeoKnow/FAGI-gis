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
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.Dataset;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.ImporterWorker;
import gr.athenainnovation.imis.fusion.gis.json.JSONLoadLinksResult;
import gr.athenainnovation.imis.fusion.gis.json.JSONMatches;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.postgis.PostGISImporter;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import gr.athenainnovation.imis.fusion.gis.utils.SPARQLUtilities;
import gr.athenainnovation.imis.fusion.gis.virtuoso.SchemaMatchState;
import gr.athenainnovation.imis.fusion.gis.virtuoso.ScoredMatch;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.lucene.queryParser.ParseException;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "SchemaMatchServlet", urlPatterns = {"/SchemaMatchServlet"})
public class SchemaMatchServlet extends HttpServlet {
        
    private static final org.apache.log4j.Logger LOG = Log.getClassFAGILogger(SchemaMatchServlet.class);
    
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
        
        response.setContentType("application/json");
        
        // Per request state
        PrintWriter             out = null;
        JSONMatches             matches;
        JSONRequestResult       res;
        DBConfig                dbConf;
        GraphConfig             grConf;
        Connection              virt_conn;
        VirtGraph               vSet = null;
        HttpSession             sess;
        ObjectMapper            mapper = new ObjectMapper();
        boolean                 success = true;
        
        try {
            
            try {
                out = response.getWriter();
            } catch (IOException ex) {
                LOG.trace("IOException thrown in servlet Writer");
                LOG.debug("IOException thrown in servlet Writer : \n" + ex.getMessage() );
                
                return;
            }
            
            sess = request.getSession(false);
            matches = new JSONMatches();
            res = new JSONRequestResult();
            matches.setResult(res);
            
            if ( sess == null ) {
                LOG.trace("Not a valid session");
                LOG.debug("Not a valid session" );
                
                matches.getResult().setMessage("Failed to create session!");
                matches.getResult().setStatusCode(-1);
                
                out.println(mapper.writeValueAsString(matches));

                out.close();

                return;
            }
            
            //mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            //mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            //SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            //mapper.setDateFormat(outputFormat);
            //mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            
            String[] selectedLinks = request.getParameterValues("links[]");
            
            List<Link> lst = new ArrayList<>();
            for(String s : selectedLinks) {
                final String subs[] = s.split("<-->");
                final Link l = new Link(subs[0], subs[1]);
                lst.add(l);
            }
            sess.setAttribute("links_list_chosen", lst);
            
            if (vSet == null) {
                try {
                    vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                            dbConf.getUsername(),
                            dbConf.getPassword());
                } catch (JenaException connEx) {
                    LOG.trace("Failed to create Jena VirtGraph");
                    LOG.trace("Failed to create Jena VirtGraph : " + connEx.getMessage());

                    matches.getResult().setMessage("Failed to perform property matching!");
                    matches.getResult().setStatusCode(-1);
                
                    out.println(mapper.writeValueAsString(matches));

                    out.close();
                
                    return;
                }
            }
            
            virt_conn = vSet.getConnection();
            success = SPARQLUtilities.createLinksGraph(lst, grConf.getLinksGraph(), virt_conn, grConf, "");
            
            if ( !success ) {
                LOG.trace("Failed to create Links Graph for matching");
                LOG.trace("Failed to create Links Graph for matching");

                matches.getResult().setMessage("Failed to perform property matching!");
                matches.getResult().setStatusCode(-1);

                out.println(mapper.writeValueAsString(matches));

                out.close();

                return;
            }
            //2105779425
            VirtuosoImporter virtImp = (VirtuosoImporter)sess.getAttribute("virt_imp");
            if (Constants.LATE_FETCH) {

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

                // Fire threads for uploading
                datasetAImportWorker.execute();
                datasetBImportWorker.execute();

                // Get thread run results
                HashMap<String, String> retA, retB;
                try {
                    retB = datasetAImportWorker.get();
                    retA = datasetBImportWorker.get();
                } catch (InterruptedException | ExecutionException ex) {
                    LOG.trace("Thread execution failed");
                    LOG.debug("Thread execution failed");
                    matches.getResult().setStatusCode(-1);
                    matches.getResult().setMessage("Upload thread execution failed");

                    out.println(mapper.writeValueAsString(matches));

                    out.close();

                    return;
                }

                if ((retA == null) || (retB == null)) {
                    LOG.trace("Thread execution failed");
                    LOG.debug("Thread execution failed");
                    matches.getResult().setStatusCode(-1);
                    matches.getResult().setMessage("Problem with link upload cleanup");

                    out.println(mapper.writeValueAsString(matches));

                    out.close();

                    return;
                }
                LOG.trace("SQLException after");

                LOG.trace("SQLException before");
                // Recreate target temp graph
                final String dropTempGraph = "SPARQL DROP SILENT GRAPH <" + grConf.getTargetTempGraph() + ">";
                final String createTempGraph = "SPARQL CREATE GRAPH <" + grConf.getTargetTempGraph() + ">";

                PreparedStatement stmt = null;
                try {
                    stmt = virt_conn.prepareStatement(dropTempGraph);
                    stmt.execute();

                    stmt = virt_conn.prepareStatement(createTempGraph);
                    stmt.execute();

                    stmt.close();

                } catch (SQLException ex) {
                    LOG.trace("SQLException thrown");
                    LOG.debug("SQLException thrown : " + ex.getMessage());
                    LOG.debug("SQLException thrown : " + ex.getSQLState());
                    matches.getResult().setStatusCode(-1);
                    matches.getResult().setMessage("Problem with destroying Target Temporary graph");

                    out.println(mapper.writeValueAsString(matches));

                    out.close();

                    return;
                } finally {
                    try {
                        if (stmt != null) {
                            stmt.close();
                        }
                    } catch (SQLException ex) {
                        LOG.trace("SQLException thrown during statement close");
                        LOG.debug("SQLException thrown during statement close : " + ex.getMessage());
                        LOG.debug("SQLException thrown during statement close : " + ex.getSQLState());
                    }
                }
                
            }
            
            SchemaMatchState sms = virtImp.scanProperties(3, null, null, (Boolean)sess.getAttribute("make-swap"));
            
            if ( sms == null ) {
                LOG.trace("Failed to create SchemaMatchState");
                LOG.trace("Failed to create SchemaMatchState");
                
                matches.getResult().setMessage("Failed to perform property matching!");
                matches.getResult().setStatusCode(-1);
                
                out.println(mapper.writeValueAsString(matches));

                out.close();

                return;
            }
            
            System.out.println("Dom A "+sms.domOntoA+" Dom B "+sms.domOntoB);
            sess.setAttribute("domA", sms.domOntoA);
            sess.setAttribute("domB", sms.domOntoB);
            
            matches.setFoundA(sms.foundA);
            matches.setFoundB(sms.foundB);
            matches.setOtherPropertiesA(sms.otherPropertiesA);
            matches.setOtherPropertiesB(sms.otherPropertiesB);
            matches.setGeomTransforms(sms.geomTransforms);
            matches.setMetaTransforms(sms.metaTransforms);
            
            List<String> lstProp = sms.getPropertyList("A");
            //System.out.println("\n\n\n\n\nPROPERTIES A\n\n\n\n\n\n\n");
            for ( String prope : lstProp ) {
                //System.out.println(prope);
            }
            lstProp = sms.getPropertyList("B");
            //System.out.println("\n\n\n\n\nPROPERTIES B\n\n\n\n\n\n\n");
            for ( String prope : lstProp ) {
                //System.out.println(prope);
            }
            sess.setAttribute("property_patternsA", sms.getPropertyList("A"));
            sess.setAttribute("property_patternsB", sms.getPropertyList("B"));
            sess.setAttribute("predicates_matches", sms);
            
            out.println(mapper.writeValueAsString(matches));
        } catch (JsonProcessingException ex) {
            LOG.trace("JsonProcessingException thrown");
            LOG.debug("JsonProcessingException thrown : " + ex.getMessage());
            
            throw new ServletException("JsonProcessingException thrown by Tomcat");
        } catch ( java.lang.OutOfMemoryError oome) {
            LOG.trace("OutOfMemoryError thrown");
            LOG.debug("OutOfMemoryError thrown : " + oome.getMessage());
            
            throw new ServletException("OutOfMemoryError thrown by Tomcat");
        } finally {
            if ( vSet != null ) {
                vSet.close();
            }
            
            if ( out != null )
                out.close();
        }
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
            throws ServletException {
        processRequest(request, response);
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
