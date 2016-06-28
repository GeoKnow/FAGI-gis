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
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONLinkMatches;
import gr.athenainnovation.imis.fusion.gis.json.JSONLinkMatching;
import gr.athenainnovation.imis.fusion.gis.json.JSONLinkProperties;
import gr.athenainnovation.imis.fusion.gis.json.JSONProperty;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import gr.athenainnovation.imis.fusion.gis.virtuoso.SchemaMatchState;
import gr.athenainnovation.imis.fusion.gis.virtuoso.ScoredMatch;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.didion.jwnl.JWNLException;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryParser.ParseException;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "LinkSchemasServlet", urlPatterns = {"/LinkSchemasServlet"})
public class LinkSchemasServlet extends HttpServlet {

    private static final org.apache.log4j.Logger LOG = Log.getClassFAGILogger(LinkSchemasServlet.class);    

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
        JSONLinkMatches                 matches = null;
        HttpSession                     sess;
        
        JSONLinkMatching                lm;
        JSONLinkProperties              lp;
        HashMap<String, String>         links;
        PrintWriter                     out = null;
        String                          s;
        String                          targetGraph;
        GraphConfig                     grConf;
        VirtGraph                       vSet = null;
        DBConfig                        dbConf;
        ObjectMapper                    mapper = new ObjectMapper();
        
        response.setContentType("application/json");
        try {
            out = response.getWriter();
            
            sess = request.getSession(false);

            if (sess == null) {
                out.print("{}");

                return;
            }
        
            //ObjectMapper mapper = new ObjectMapper();
            //mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            //mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            //SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            //mapper.setDateFormat(outputFormat);
            //mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        
            lm = new JSONLinkMatching();
            lp = new JSONLinkProperties();
            links = (HashMap<String, String>) sess.getAttribute("links");

            s = request.getParameter("subject");
            targetGraph = (String) sess.getAttribute("t_graph");
            grConf = (GraphConfig) sess.getAttribute("gr_conf");
            dbConf = (DBConfig) sess.getAttribute("db_conf");
            matches = new JSONLinkMatches();
            
            try {
                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                //System.out.println(connEx.getMessage());
                out.println("Connection to virtuoso failed");
                out.close();

                return;
            }
            
            //System.out.println("Links Hash Map : " + links);
            
            Connection virt_conn = vSet.getConnection();
            lp.setNodeA( s );
            lp.setNodeB( links.get(s) );
            //System.out.println("Link : " + lp.getNodeA() + "    " + lp.getNodeB() );
            
            VirtuosoImporter virtImp = (VirtuosoImporter) sess.getAttribute("virt_imp");
            SchemaMatchState sms = virtImp.scanProperties(3, lp.getNodeA(), lp.getNodeB(), (Boolean)sess.getAttribute("make-swap"));
            matches.setFoundA( sms.foundA );
            matches.setFoundB( sms.foundB );
            
            sess.setAttribute("link_property_patternsA", sms.getPropertyList("A"));
            sess.setAttribute("link_property_patternsB", sms.getPropertyList("B"));
            sess.setAttribute("link_predicates_matches", sms);

            
            if ( grConf.isDominantA() ) 
                s = lp.getNodeA();
            else
                s = lp.getNodeB();
            
            for (int i = 0; i < 4; i++) {
                StringBuilder query = new StringBuilder();
                query.append("SPARQL SELECT ?pa1 ?oa1 ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append("?pa").append(ind).append(" ?oa").append(ind).append(" ");
                }
                query.append("?pb1 ?ob1 ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append("?pb").append(ind).append(" ?ob").append(ind).append(" ");
                }
                query.append("\nWHERE\n{\n\n"
                        + " { GRAPH <").append(grConf.getMetadataGraphA()).append("> {<" + s + "> ?pa1 ?oa1 ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append(" . ?oa").append(prev).append(" ?pa").append(ind).append(" ?oa").append(ind).append("  ");
                }
                //for (int j = 0; j < i; j++) {
                    query.append(" } ");
                //}
                query.append("\n } UNION { \n" + "   GRAPH <").append(grConf.getMetadataGraphB()).append("> {<" + s + "> ?pb1 ?ob1 ");
                for (int j = 0; j < i; j++) {
                    int ind = j + 2;
                    int prev = ind - 1;
                    query.append(" . ?ob").append(prev).append(" ?pb").append(ind).append(" ?ob").append(ind).append("  ");
                }
                for (int j = 0; j < i; j++) {
                    //query.append(" } ");
                }
                query.append("} }\n"
                        + "}\n"
                        + "");

                //System.out.println(query.toString());
                
                try ( PreparedStatement fetchProperties = virt_conn.prepareStatement(query.toString());
                      ResultSet propertiesRS = fetchProperties.executeQuery() ) {

                    //String prevSubject = "";
                    while (propertiesRS.next()) {
                        StringBuilder chainA = new StringBuilder();
                        StringBuilder chainB = new StringBuilder();
                        StringBuilder chainLongA = new StringBuilder();
                        StringBuilder chainLongB = new StringBuilder();
                        for (int j = 0; j <= i; j++) {
                    //int ind = j+2;
                            //int prev = ind - 1;
                            int step_over = 2 * (i + 1);

                            String predicateA = propertiesRS.getString(2 * (j) + 1);
                            String objectA = propertiesRS.getString(2 * (j + 1));
                            String predicateB = propertiesRS.getString(2 * (j) + 1 + step_over);
                            String objectB = propertiesRS.getString(2 * (j) + 2 + step_over);

                            if (predicateA != null) {
                                if (predicateA.contains("posSeq")) {
                                    //continue;
                                }
                            }
                            if (predicateB != null) {
                                if (predicateB.contains("posSeq")) {
                                    //continue;
                                }
                            }

                            if (predicateA != null) {
                                chainLongA.append(predicateA + ",");
                                String main = StringUtils.substringAfter(predicateA, "#");
                                if (main.equals("")) {
                                    main = StringUtils.substring(predicateA, StringUtils.lastIndexOf(predicateA, "/") + 1);
                                }
                                chainA.append(main + ",");
                            }

                            if (predicateB != null) {
                                chainLongB.append(predicateB + ",");
                                String main = StringUtils.substringAfter(predicateB, "#");
                                if (main.equals("")) {
                                    main = StringUtils.substring(predicateB, StringUtils.lastIndexOf(predicateB, "/") + 1);
                                }
                                chainB.append(main + ",");
                            }

                        }

                        //System.out.println("Chain A " + chainA);

                        if (chainA.length() > 0) {
                            int new_len = chainA.length() - 1;
                            chainA.setLength(new_len);
                            new_len = chainLongA.length() - 1;
                            chainLongA.setLength(new_len);
                            if (!lp.getPropsA().contains(chainA.toString())) {
                                lp.getPropsA().add(chainA.toString());
                                lp.getPropsLongA().add(chainLongA.toString());
                                lp.getPropsFullA().add((new JSONProperty(chainA.toString(), chainLongA.toString())));
                            }
                        }

                        if (chainB.length() > 0) {
                            int new_len = chainB.length() - 1;
                            chainB.setLength(new_len);
                            new_len = chainLongB.length() - 1;
                            chainLongB.setLength(new_len);
                            if (!lp.getPropsB().contains(chainB.toString())) {
                                lp.getPropsB().add(chainB.toString());
                                lp.getPropsLongB().add(chainLongB.toString());
                                lp.getPropsFullB().add((new JSONProperty(chainB.toString(), chainLongB.toString())));
                            }
                        }
                    }

                } catch (SQLException ex) {
                }
                
            }
            
            lm.setP( lp );
            lm.setM( matches );
            //System.out.println(lp.propsA);
            //System.out.println(lp.propsB);
            //System.out.println(mapper.writeValueAsString(lm));
            out.println(mapper.writeValueAsString(lm));
            /* TODO output your page here. You may use following sample code. */
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
            if (vSet != null) {
                vSet.close();
            }
            if (out != null )
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
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } finally {
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
        } finally {

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
