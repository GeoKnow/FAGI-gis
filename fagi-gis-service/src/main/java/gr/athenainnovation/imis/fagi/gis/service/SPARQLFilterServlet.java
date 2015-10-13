/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "SPARQLFilterServlet", urlPatterns = {"/SPARQLFilterServlet"})
public class SPARQLFilterServlet extends HttpServlet {
    //private final String selectVerify = "^\b[Ss][Ee][Ll][Ee][Cc][Tt]\b\\?subjectA\b\\?subjectB\b[Ww][Hh][Ee][Rr][Ee]\b\\{";
    private final String selectVerify = "\\s*[Ss][Ee][Ll][Ee][Cc][Tt]\\s+\\?subjectA\\s+\\?subjectB\\s+[Ww][Hh][Ee][Rr][Ee]\\s+\\{";
    private final String linkGraph = "^\b[Ss][Ee][Ll][Ee][Cc][Tt]\b\\?subjectA\b\\?subjectB\b[Ww][Hh][Ee][Rr][Ee]\b{";

    private String queryA;
    private String queryB;
    private HttpSession sess = null;
    private GraphConfig grConf;
    private String tGraph = null;
    private DBConfig dbConf = null;
    private VirtGraph vSet = null;
    private Connection dbConn = null;
    
    private String queryANormalized;
    private String queryBNormalized;
    
    private Connection virt_conn = null;
    private PreparedStatement stmt = null;
    private ResultSet rs = null;
    
    private JSONLinks ret = null;
    private JSONHtmlLinks retHtml;
    
    private ObjectMapper mapper = new ObjectMapper();
    
    private class JSONHtmlLinks {
        String links;
        String err;
        
        public JSONHtmlLinks(String links) {
            this.links = links;
            this.err = "";
        }

        public String getLinks() {
            return links;
        }

        public void setLinks(String links) {
            this.links = links;
        }

        public String getErr() {
            return err;
        }

        public void setErr(String err) {
            this.err = err;
        }

    }
    
    private class JSONLinks {
         Map<String, String> links;

        public JSONLinks() {
            links = new HashMap<>();
        }

        public Map<String, String> getLinks() {
            return links;
        }

        public void setLinks(Map<String, String> links) {
            this.links = links;
        }
         
    }
    
    private class JSONLink {
        String subA;
        String subB;

        public JSONLink(String subA, String subB) {
            this.subA = subA;
            this.subB = subB;
        }

        public String getSubA() {
            return subA;
        }

        public void setSubA(String subA) {
            this.subA = subA;
        }

        public String getSubB() {
            return subB;
        }

        public void setSubB(String subB) {
            this.subB = subB;
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
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            
            sess = request.getSession(false);
            
            if ( sess == null ) {
                out.print("{\"error\":\"invalid session\"}");
                out.close();
                
                return;
            }
            
            ret = new JSONLinks();
            
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            dbConf = (DBConfig) sess.getAttribute("db_conf");
            tGraph = (String) sess.getAttribute("t_graph");
            
            queryA = request.getParameter("queryA");
            queryB = request.getParameter("queryB");
            
            System.out.println(queryA);
            System.out.println(queryB);
            
            String queryAll = "";
            queryANormalized = queryA.replace("fagi-gis:links", grConf.getAllLinksGraph() );
            queryBNormalized = queryA.replace("fagi-gis:links", grConf.getAllLinksGraph() );
            
            if ( !queryANormalized.contains( "fagi-gis:metadata" ) ) {
                queryAll = queryANormalized;
            } else {
                queryANormalized = queryANormalized.replace("fagi-gis:metadata", tGraph + "_" + dbConf.getDBName() + "A");
            }
            if ( !queryBNormalized.contains( "fagi-gis:metadata" ) ) {
                queryAll = queryBNormalized;
            } else {
                queryBNormalized = queryBNormalized.replace("fagi-gis:metadata", tGraph + "_" + dbConf.getDBName() + "A");
            }
            
            System.out.println(queryANormalized);
            System.out.println(queryBNormalized);
            
            try {
                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                System.out.println(connEx.getMessage());
                out.println("Connection to virtuoso failed");
                out.print("{\"error\":\"connection to virtuoso failed\"}");
                out.close();

                return;
            }
            
            virt_conn = vSet.getConnection();
            
            String errString = "";
            if ( queryAll.isEmpty() ) {
                try {
                    stmt = virt_conn.prepareStatement("SPARQL " + queryANormalized);

                    rs = stmt.executeQuery();

                    while (rs.next()) {
                        //ret.add(new JSONLink(rs.getString(1), rs.getString(2)));
                        System.out.println("Creating " + rs.getString(1) + " " + rs.getString(2));
                        ret.links.put(rs.getString(1), rs.getString(1));
                    }

                    rs.close();
                    stmt.close();
                } catch (SQLException ex) {
                    System.out.println("Localized Message : " + ex.getLocalizedMessage());
                    System.out.println("Message : " + ex.getMessage());
                    System.out.println("Message : " + ex.getSQLState());
                    
                    errString = ex.getLocalizedMessage();
                            
                    Logger.getLogger(SPARQLFilterServlet.class.getName()).log(Level.SEVERE, null, ex);
                }

                try {

                    stmt = virt_conn.prepareStatement("SPARQL " + queryBNormalized);
                    rs = stmt.executeQuery();

                    while (rs.next()) {
                        //ret.add(new JSONLink(rs.getString(1), rs.getString(2)));
                        System.out.println("Creating " + rs.getString(1) + " " + rs.getString(2));
                        ret.links.put(rs.getString(1), rs.getString(1));
                    }

                    rs.close();
                    stmt.close();

                } catch (SQLException ex) {
                    System.out.println("Localized Message : " + ex.getLocalizedMessage());
                    System.out.println("Message : " + ex.getMessage());
                    System.out.println("Message : " + ex.getSQLState());
                    
                    errString = ex.getLocalizedMessage();
                    
                    Logger.getLogger(SPARQLFilterServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                try {
                    stmt = virt_conn.prepareStatement("SPARQL " + queryAll);
                    rs = stmt.executeQuery();

                    while (rs.next()) {
                        //ret.add(new JSONLink(rs.getString(1), rs.getString(2)));
                        System.out.println("Creating " + rs.getString(1) + " " + rs.getString(2));
                        ret.links.put(rs.getString(1), rs.getString(1));
                    }

                    rs.close();
                    stmt.close();

                } catch (SQLException ex) {
                    System.out.println("Localized Message : " + ex.getLocalizedMessage());
                    System.out.println("Message : " + ex.getMessage());
                    System.out.println("Message : " + ex.getSQLState());
                    
                    errString = ex.getLocalizedMessage();
                    
                    Logger.getLogger(SPARQLFilterServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (Map.Entry<String, String> entry : ret.links.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                String check = "chk"+i;
                sb.append("<li><div class=\"checkboxes\">");
                sb.append("<label for=\""+check+"\"><input type=\"checkbox\" value=\"\"name=\""+check+"\" id=\""+check+"\" />"+key+"<-->"+val+"</label>");
                sb.append("</div>\n</li>");
                i++;
            }
            
            retHtml = new JSONHtmlLinks(sb.toString());
            
            System.out.println("Ret JSON "+mapper.writeValueAsString(ret));
            System.out.println("Ret JSON "+mapper.writeValueAsString(retHtml));
            
            out.print(mapper.writeValueAsString(retHtml));
            
            String test = "SELECT";
            System.out.println("test A "+test.matches(selectVerify));
            test = "   SELECT";
            System.out.println("test A "+test.matches(selectVerify));
            test = "SELECT ?subjectA ?subjectB WHERE {";
            System.out.println("test A "+test.matches(selectVerify));
            test = "SELECT d?subjectA ?subjectB WHERE {";
            System.out.println("test B "+test.matches(selectVerify));
            test = "SELECT     ?subjectA     ?subjectB WHERE {";
            System.out.println("test C "+test.matches(selectVerify));
            test = "   SELECT ?subjectA ?subjectB WHERE {";
            System.out.println("test D "+test.matches(selectVerify));
            test = "   SELECT ?subjectA ?subjectB WHERE {dgdfgdfg";
            System.out.println("test E "+test.matches(selectVerify));
            
            /*
            String errString = checkInput();
            
            if ( !errString.isEmpty() ) {
                out.print(errString);
                
                return;
            }            
            */
        }
    }

    String checkInput() {
        String errString = "";
        
        if (queryA == null || queryB == null) {
            errString = "{\"error\":\"null queries\"}";
        }

        if (!queryA.contains("<fagi-gis:links>")) {
            errString = "{\"error\":\"<fagi-gis:links> graph required\"}";
        }
        if (!queryB.contains("<fagi-gis:links>")) {
            errString = "{\"error\":\"<fagi-gis:links> graph required\"}";
        }
        if (!queryA.contains("?subjectA") || !queryA.contains("?subjectA")) {
            errString = "{\"error\":\"select requires ?subjectA and ?subjectB\"}";
        }
        if (!queryB.contains("?subjectB") || !queryB.contains("?subjectB")) {
            errString = "{\"error\":\"SELECT requires ?subjectA and ?subjectB\"}";
        }
        
        return errString;
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
            processRequest(request, response);
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
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
            processRequest(request, response);
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(CreateLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
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
