/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
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

    private class JSONLinkMatching {

        JSONMatches m;
        JSONLinkProperties p;

        public JSONLinkMatching() {
        }

        public JSONMatches getM() {
            return m;
        }

        public void setM(JSONMatches m) {
            this.m = m;
        }

        public JSONLinkProperties getP() {
            return p;
        }

        public void setP(JSONLinkProperties p) {
            this.p = p;
        }

    }

    private class JSONMatches {

        HashMap<String, HashSet<ScoredMatch>> foundA;
        HashMap<String, HashSet<ScoredMatch>> foundB;

        public JSONMatches() {
        }

        public HashMap<String, HashSet<ScoredMatch>> getFoundA() {
            return foundA;
        }

        public void setFoundA(HashMap<String, HashSet<ScoredMatch>> foundA) {
            this.foundA = foundA;
        }

        public HashMap<String, HashSet<ScoredMatch>> getFoundB() {
            return foundB;
        }

        public void setFoundB(HashMap<String, HashSet<ScoredMatch>> foundB) {
            this.foundB = foundB;
        }

    }

    private class JSONProperty {

        String short_rep;
        String long_rep;

        public JSONProperty() {
            this.short_rep = "";
            this.long_rep = "";
        }

        public JSONProperty(String short_rep, String long_rep) {
            this.short_rep = short_rep;
            this.long_rep = long_rep;
        }

        public String getShort_rep() {
            return short_rep;
        }

        public void setShort_rep(String short_rep) {
            this.short_rep = short_rep;
        }

        public String getLong_rep() {
            return long_rep;
        }

        public void setLong_rep(String long_rep) {
            this.long_rep = long_rep;
        }

    }

    private class JSONLinkProperties {

        String nodeA;
        String nodeB;
        HashSet<String> propsA;
        HashSet<String> propsB;
        HashSet<String> propsLongA;
        HashSet<String> propsLongB;
        HashSet<JSONProperty> propsFullB;
        HashSet<JSONProperty> propsFullA;

        public JSONLinkProperties() {
            propsA = new HashSet<>();
            propsB = new HashSet<>();
            propsLongA = new HashSet<>();
            propsLongB = new HashSet<>();
            propsFullA = new HashSet<>();
            propsFullB = new HashSet<>();
        }

        public String getNodeA() {
            return nodeA;
        }

        public void setNodeA(String nodeA) {
            this.nodeA = nodeA;
        }

        public String getNodeB() {
            return nodeB;
        }

        public void setNodeB(String nodeB) {
            this.nodeB = nodeB;
        }

        public HashSet<String> getPropsA() {
            return propsA;
        }

        public void setPropsA(HashSet<String> propsA) {
            this.propsA = propsA;
        }

        public HashSet<String> getPropsB() {
            return propsB;
        }

        public void setPropsB(HashSet<String> propsB) {
            this.propsB = propsB;
        }

        public HashSet<String> getPropsLongA() {
            return propsLongA;
        }

        public void setPropsLongA(HashSet<String> propsLongA) {
            this.propsLongA = propsLongA;
        }

        public HashSet<String> getPropsLongB() {
            return propsLongB;
        }

        public void setPropsLongB(HashSet<String> propsLongB) {
            this.propsLongB = propsLongB;
        }

        public HashSet<JSONProperty> getPropsFullB() {
            return propsFullB;
        }

        public void setPropsFullB(HashSet<JSONProperty> propsFullB) {
            this.propsFullB = propsFullB;
        }

        public HashSet<JSONProperty> getPropsFullA() {
            return propsFullA;
        }

        public void setPropsFullA(HashSet<JSONProperty> propsFullA) {
            this.propsFullA = propsFullA;
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
            throws ServletException, IOException, SQLException, JWNLException, FileNotFoundException, ParseException {
        response.setContentType("text/html;charset=UTF-8");

        JSONMatches matches = null;
        HttpSession sess;
        
        JSONLinkMatching lm;
        JSONLinkProperties lp;
        HashMap<String, String> links;

        String s;
        String targetGraph;
        GraphConfig grConf;
        
        response.setContentType("application/json");
        try (PrintWriter out = response.getWriter()) {
            
            sess = request.getSession(false);

            if (sess == null) {
                out.print("{}");

                return;
            }
        
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            mapper.setDateFormat(outputFormat);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        
            lm = new JSONLinkMatching();
            lp = new JSONLinkProperties();
            links = (HashMap<String, String>) sess.getAttribute("links");

            s = request.getParameter("subject");
            targetGraph = (String) sess.getAttribute("t_graph");
            grConf = (GraphConfig) sess.getAttribute("gr_conf");
            matches = new JSONMatches();
            VirtGraph vSet = null;
            DBConfig dbConf = (DBConfig) sess.getAttribute("db_conf");
            
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
            
            //System.out.println("Links Hash Map : " + links);
            Connection virt_conn = vSet.getConnection();
            lp.nodeA = s;
            lp.nodeB = links.get(s);

            VirtuosoImporter virtImp = (VirtuosoImporter) sess.getAttribute("virt_imp");
            SchemaMatchState sms = virtImp.scanProperties(3, s, (Boolean)sess.getAttribute("make-swap"));
            matches.foundA = sms.foundA;
            matches.foundB = sms.foundB;
            
            sess.setAttribute("link_property_patternsA", sms.getPropertyList("A"));
            sess.setAttribute("link_property_patternsB", sms.getPropertyList("B"));
            sess.setAttribute("link_predicates_matches", sms);

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

                System.out.println(query.toString());
                
                PreparedStatement fetchProperties;
                fetchProperties = virt_conn.prepareStatement(query.toString());
                ResultSet propertiesRS = fetchProperties.executeQuery();

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
                    
                    System.out.println("Chain A " + chainA);

                    if (chainA.length() > 0) {
                        int new_len = chainA.length() - 1;
                        chainA.setLength(new_len);
                        new_len = chainLongA.length() - 1;
                        chainLongA.setLength(new_len);
                        if (!lp.propsA.contains(chainA.toString())) {
                            lp.propsA.add(chainA.toString());
                            lp.propsLongA.add(chainLongA.toString());
                            lp.propsFullA.add((new JSONProperty(chainA.toString(), chainLongA.toString())));
                        }
                    }

                    if (chainB.length() > 0) {
                        int new_len = chainB.length() - 1;
                        chainB.setLength(new_len);
                        new_len = chainLongB.length() - 1;
                        chainLongB.setLength(new_len);
                        if (!lp.propsB.contains(chainB.toString())) {
                            lp.propsB.add(chainB.toString());
                            lp.propsLongB.add(chainLongB.toString());
                            lp.propsFullB.add((new JSONProperty(chainB.toString(), chainLongB.toString())));
                        }
                    }
                }
                
                propertiesRS.close();
                fetchProperties.close();
            }
            
            if ( vSet != null ) 
                vSet.close();
            
            
            lm.p = lp;
            lm.m = matches;
            //System.out.println(lp.propsA);
            //System.out.println(lp.propsB);
            System.out.println(mapper.writeValueAsString(lm));
            out.println(mapper.writeValueAsString(lm));
            /* TODO output your page here. You may use following sample code. */
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
        } catch (SQLException ex) {
            Logger.getLogger(LinkSchemasServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JWNLException ex) {
            Logger.getLogger(LinkSchemasServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LinkSchemasServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(LinkSchemasServlet.class.getName()).log(Level.SEVERE, null, ex);
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
        } catch (SQLException ex) {
            Logger.getLogger(LinkSchemasServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JWNLException ex) {
            Logger.getLogger(LinkSchemasServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LinkSchemasServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(LinkSchemasServlet.class.getName()).log(Level.SEVERE, null, ex);
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
