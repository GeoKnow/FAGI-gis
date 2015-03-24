/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gr.athenainnovation.imis.fusion.gis.virtuoso.SchemaMatchState;
import gr.athenainnovation.imis.fusion.gis.virtuoso.ScoredMatch;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import org.apache.lucene.queryParser.ParseException;

/**
 *
 * @author nick
 */
@WebServlet(name = "SchemaMatchServlet", urlPatterns = {"/SchemaMatchServlet"})
public class SchemaMatchServlet extends HttpServlet {
    JSONMatches matches = null;
    
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
        response.setContentType("application/json");
        try (PrintWriter out = response.getWriter()) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            mapper.setDateFormat(outputFormat);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            HttpSession sess = request.getSession(true);
            
            matches = new JSONMatches();
            System.out.println(sess.getAttribute("links"));
            //System.out.println(virtImp.scanProperties(2));
            System.out.println(request.getParameter("links"));
            String[] selectedLinks = request.getParameterValues("links[]");
            System.out.println(request.getParameterMap());
            for(String s : selectedLinks) {
                System.out.println("Link "+s);
            }
            System.out.println(request.getParameterMap());
            StringBuilder sb = new StringBuilder();
            
            VirtuosoImporter virtImp = (VirtuosoImporter)sess.getAttribute("virt_imp");
            SchemaMatchState sms = virtImp.scanProperties(1, null);
            System.out.println("Dom A "+sms.domOntoA+" Dom B "+sms.domOntoB);
            sess.setAttribute("domA", sms.domOntoA);
            sess.setAttribute("domB", sms.domOntoB);
            //sms.foundA.put("lalalala"+sess.getCreationTime(), new HashSet<String>());
            matches.foundA = sms.foundA;
            matches.foundB = sms.foundB;
            sess.setAttribute("predicates_matches", sms);
            System.out.println("Problem");
            sb.append("{");
            /*Iterator it = sms.foundA.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry)it.next();
                sb.append("\""+pairs.getKey()+"\" : [");
                HashSet<String> set = (HashSet<String>)pairs.getValue();
                for(String s : set) {
                    sb.append("\""+s+"\",");
                }
                int newLength = sb.length();
                sb.setLength(newLength - 1);
                sb.append(" ],");
            }
            int newLength = sb.length();
            sb.setLength(newLength - 1);
            sb.append("}");*/
            //JSONArray ja;
            System.out.println("Matches : "+mapper.writeValueAsString(matches));
            //System.out.println(sb);
            out.println(mapper.writeValueAsString(matches));
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
            Logger.getLogger(SchemaMatchServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JWNLException ex) {
            Logger.getLogger(SchemaMatchServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SchemaMatchServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(SchemaMatchServlet.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(SchemaMatchServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JWNLException ex) {
            Logger.getLogger(SchemaMatchServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SchemaMatchServlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(SchemaMatchServlet.class.getName()).log(Level.SEVERE, null, ex);
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
