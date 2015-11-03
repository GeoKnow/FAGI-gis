/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONFilteredLinks;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
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
 * @author Nick Vitsas
 */
@WebServlet(name = "FilterServlet", urlPatterns = {"/FilterServlet"})
public class FilterServlet extends HttpServlet {

    private static final org.apache.log4j.Logger LOG = Log.getClassFAGILogger(FilterServlet.class);    
    
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
        HashMap<String, String>         filteredLinks;
        JSONFilteredLinks              ret;
        JSONRequestResult               res;
        PrintWriter                     out = null;
        HttpSession                     sess;
        GraphConfig                     grConf;
        DBConfig                        dbConf;
        ObjectMapper                    mapper = new ObjectMapper();
        VirtGraph                       vSet = null;        
        
        try {  
            out = response.getWriter();
            
            sess = request.getSession(false);
            
            if ( sess == null ) {
                out.print("{}");
                
                return;
            }
            
            ret = new JSONFilteredLinks();
            res = new JSONRequestResult();
            ret.setResult(res);
            
            String set = request.getParameter("dataset");
            String[] filters = request.getParameterValues("filter[]");
            
            // Unfilter button was pressed
            StringBuilder sb = new StringBuilder();
            if (set.isEmpty()) {
                HashMap<String, String> linksHashed = (HashMap<String, String>) sess.getAttribute("links");
                int i = 0;
                for (Map.Entry<String, String> entry : linksHashed.entrySet()) {
                    String key = entry.getKey();
                    String val = entry.getValue();
                    String check = "chk" + i;
                    sb.append("<li><div class=\"checkboxes\">");
                    sb.append("<label for=\"" + check + "\"><input type=\"checkbox\" value=\"\"name=\"" + check + "\" id=\"" + check + "\" />" + key + "<-->" + val + "</label>");
                    sb.append("</div>\n</li>");
                
                    //out.println("<li><div class=\"checkboxes\">");
                    //out.println("<label for=\"" + check + "\"><input type=\"checkbox\" value=\"\"name=\"" + check + "\" id=\"" + check + "\" />" + key + "<-->" + val + "</label>");
                    //out.println("</div>\n</li>");
                    
                    i++;
                }
                
                ret.setLinksHTML(sb.toString());
                res.setStatusCode(0);
                res.setMessage("All good");
                out.println(mapper.writeValueAsString(ret));
            
                return;
            }
            
            filteredLinks = new HashMap<>();
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            dbConf = (DBConfig)sess.getAttribute("db_conf");
        
            /* TODO output your page here. You may use following sample code. */
            System.out.println(request.getParameterMap());
            StringBuilder bu = new StringBuilder();
                
            try {    
                vSet = new VirtGraph ("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                                    dbConf.getUsername(), 
                                    dbConf.getPassword());
            } catch (JenaException connEx) {
                LOG.trace("JenaException thrown during filtering ");
                LOG.debug("JenaException thrown during filtering : " + connEx.getMessage());

                res.setStatusCode(-1);
                res.setMessage("Failed to filter links from " + grConf.getGraphA());

                out.printf(mapper.writeValueAsString(ret));

                return;
            }
            Connection virt_conn = vSet.getConnection();
            if (set.equals("A")) {
                for (String filter : filters) {
                    String filterSelectA = "";
                    if (grConf.isDominantA()) {
                        filterSelectA = "SPARQL SELECT distinct(?s) ?o WHERE { GRAPH <" + grConf.getAllLinksGraph() + "> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } . GRAPH <" + grConf.getMetadataGraphA() + "> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + filter + "> } }";
                    } else {
                        filterSelectA = "SPARQL SELECT distinct(?s) ?o WHERE { GRAPH <" + grConf.getAllLinksGraph() + "> { ?o <http://www.w3.org/2002/07/owl#sameAs> ?s } . GRAPH <" + grConf.getMetadataGraphA() + "> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + filter + "> } }";
                    }
                    System.out.println(filterSelectA);
                    try (PreparedStatement filtersStmt = virt_conn.prepareStatement(filterSelectA)) {
                        ResultSet rs = filtersStmt.executeQuery();

                        while (rs.next()) {
                            String prop = rs.getString(1);
                            String prop2 = rs.getString(2);
                                //out.println(prop+",");
                            //bu.append(prop+"<-->"+prop2+",");
                            if (!filteredLinks.containsKey(prop)) {
                                filteredLinks.put(prop, prop2);
                            }
                            //System.out.println(prop);
                        }
                        
                    } catch (SQLException ex) {
                        LOG.trace("SQLException thrown filtering A ");
                        LOG.debug("SQLException thrown filtering A  : " + ex.getMessage());
                        LOG.debug("SQLException thrown filtering A  : " + ex.getSQLState());
                        
                        res.setStatusCode(-1);
                        res.setMessage("Failed to filter links from " + grConf.getGraphA());
                        
                        out.printf(mapper.writeValueAsString(ret));
                        
                        return;
                    }
                }
            } else {
                for (String filter : filters) {
                    String filterSelectB = "";
                    if (grConf.isDominantA()) {
                        filterSelectB = "sparql select distinct(?s) ?o where { GRAPH <" + grConf.getAllLinksGraph() + "> { ?s <http://www.w3.org/2002/07/owl#sameAs> ?o } . GRAPH <" + grConf.getMetadataGraphB() + "> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + filter + "> } }";
                    } else {
                        filterSelectB = "sparql select distinct(?s) ?o where { GRAPH <" + grConf.getAllLinksGraph() + "> { ?o <http://www.w3.org/2002/07/owl#sameAs> ?s } . GRAPH <" + grConf.getMetadataGraphB() + "> { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + filter + "> } }";
                    }
                    
                    try (PreparedStatement filtersStmt = virt_conn.prepareStatement(filterSelectB)) {
                        ResultSet rs = filtersStmt.executeQuery();
                        System.out.println(filterSelectB);
                        while (rs.next()) {
                            String prop = rs.getString(1);
                            String prop2 = rs.getString(2);
                            
                            if (!filteredLinks.containsKey(prop)) {
                                filteredLinks.put(prop, prop2);
                            }
                            //System.out.println(prop);
                        }
                    } catch (SQLException ex) {
                        LOG.trace("SQLException thrown filtering B ");
                        LOG.debug("SQLException thrown filtering B  : " + ex.getMessage());
                        LOG.debug("SQLException thrown filtering B  : " + ex.getSQLState());
                        
                        res.setStatusCode(-1);
                        res.setMessage("Failed to filter links from " + grConf.getGraphB());
                        
                        out.printf(mapper.writeValueAsString(ret));
                        
                        return;
                    }
                }
            }
            
            System.out.println(filteredLinks);
            int i = 0;
            sb = new StringBuilder();
            for (Map.Entry<String, String> entry : filteredLinks.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                String check = "chk"+i;
                sb.append("<li><div class=\"checkboxes\">");
                sb.append("<label for=\""+check+"\"><input type=\"checkbox\" value=\"\"name=\""+check+"\" id=\""+check+"\" />"+key+"<-->"+val+"</label>");
                sb.append("</div>\n</li>");
                //out.println("<li><div class=\"checkboxes\">");
                //out.println("<label for=\""+check+"\"><input type=\"checkbox\" value=\"\"name=\""+check+"\" id=\""+check+"\" />"+key+"<-->"+val+"</label>");
                //out.println("</div>\n</li>");
                i++;
            }
            
            ret.setLinksHTML(sb.toString());
            res.setStatusCode(0);
            res.setMessage("All good");
            
            out.println(mapper.writeValueAsString(ret));
            System.out.println("Filter "+mapper.writeValueAsString(ret));
        } catch (java.lang.OutOfMemoryError oome) {
            LOG.trace("OutOfMemoryError thrown");
            LOG.debug("OutOfMemoryError thrown : " + oome.getMessage());
            
            if ( out != null ) 
                out.print("{\"error\":\"error\"}");
            
            throw new ServletException("OutOfMemoryError thrown by Tomcat");
        } catch (JsonProcessingException ex) {
            LOG.trace("JsonProcessingException thrown");
            LOG.debug("JsonProcessingException thrown : " + ex.getMessage());
            
            if ( out != null ) 
                out.print("{\"error\":\"error\"}");
            
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
