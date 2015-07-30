/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.google.common.collect.Maps;
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
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
 * @author nick
 */
@WebServlet(name = "FilterServlet", urlPatterns = {"/FilterServlet"})
public class FilterServlet extends HttpServlet {

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
        HashMap<String, String> filteredLinks = new HashMap<>();
        PrintWriter out = response.getWriter();
        HttpSession sess = request.getSession(true);
        GraphConfig grConf = (GraphConfig)sess.getAttribute("gr_conf");
        DBConfig dbConf = (DBConfig)sess.getAttribute("db_conf");
        
        try {
            /* TODO output your page here. You may use following sample code. */
            System.out.println(request.getParameterMap());
            String set = request.getParameter("dataset");
            String[] filters = request.getParameterValues("filter[]");
            StringBuilder bu = new StringBuilder();
                
            VirtGraph vSet = null;
            try {    
                vSet = new VirtGraph ("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                                    dbConf.getUsername(), 
                                    dbConf.getPassword());
            } catch (JenaException connEx) {
                System.out.println(connEx.getMessage());      
                out.println("Connection to virtuoso failed");
                out.close();
            
                return;
            }
            Connection virt_conn = vSet.getConnection();
            PreparedStatement filtersStmt;
            if (set.equals("A")) {
            for (String filter : filters ) {  
                String filterSelectA = "";
                if (grConf.isDominantA()) {
                    filterSelectA = "sparql select distinct(?s) ?o where { GRAPH <http://localhost:8890/DAV/all_links_"+dbConf.getDBName()+"> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } . GRAPH <"+(String)sess.getAttribute("t_graph")+"_"+dbConf.getDBName()+"A> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+filter+"> } }";
                } else {
                    filterSelectA = "sparql select distinct(?s) ?o where { GRAPH <http://localhost:8890/DAV/all_links_"+dbConf.getDBName()+"> { ?o <http://www.w3.org/2002/07/owl#sameAs> ?s } . GRAPH <"+(String)sess.getAttribute("t_graph")+"_"+dbConf.getDBName()+"A> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+filter+"> } }";
                }
                System.out.println(filterSelectA);
                filtersStmt = virt_conn.prepareStatement(filterSelectA);
                ResultSet rs = filtersStmt.executeQuery();
               
                while (rs.next()) {
                    String prop = rs.getString(1);
                    String prop2 = rs.getString(2);
                    //out.println(prop+",");
                    //bu.append(prop+"<-->"+prop2+",");
                    if ( !filteredLinks.containsKey(prop) )
                        filteredLinks.put(prop, prop2);
                    //System.out.println(prop);
                }
            }
            } else {
            for (String filter : filters ) {           
                String filterSelectB = "";
                if (grConf.isDominantA()) {
                    filterSelectB = "sparql select distinct(?s) ?o where { GRAPH <http://localhost:8890/DAV/all_links_"+dbConf.getDBName()+"> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } . GRAPH <"+(String)sess.getAttribute("t_graph")+"_"+dbConf.getDBName()+"B> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+filter+"> } }";
                } else {
                    filterSelectB = "sparql select distinct(?s) ?o where { GRAPH <http://localhost:8890/DAV/all_links_"+dbConf.getDBName()+"> { ?o <http://www.w3.org/2002/07/owl#sameAs> ?s } . GRAPH <"+(String)sess.getAttribute("t_graph")+"_"+dbConf.getDBName()+"B> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <"+filter+"> } }";
                }
                filtersStmt = virt_conn.prepareStatement(filterSelectB);
                ResultSet rs = filtersStmt.executeQuery();
                System.out.println(filterSelectB);
                while (rs.next()) {
                    String prop = rs.getString(1);
                    String prop2 = rs.getString(2);
                    //out.println(prop+",");
                    //bu.append(prop+"<-->"+prop2+",");
                    if ( !filteredLinks.containsKey(prop) )
                        filteredLinks.put(prop, prop2);
                    //System.out.println(prop);
                } 
            }
            }
            
            System.out.println(filteredLinks);
            int i = 0;
            for (Map.Entry<String, String> entry : filteredLinks.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                String check = "chk"+i;
                out.println("<li><div class=\"checkboxes\">");
                out.println("<label for=\""+check+"\"><input type=\"checkbox\" value=\"\"name=\""+check+"\" id=\""+check+"\" />"+key+"<-->"+val+"</label>");
                out.println("</div>\n</li>");
                i++;
            }
            
            if (bu.length() > 0) {
                int newLength = bu.length() - 1;
                //bu.setLength(newLength);
            }
            
            System.out.println(bu);
            //out.println(bu);
            //System.out.println("FILTERING "+ request.getParameter("filter"));
        } finally {
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
        } catch (SQLException ex) {
            Logger.getLogger(FilterServlet.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(FilterServlet.class.getName()).log(Level.SEVERE, null, ex);
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
