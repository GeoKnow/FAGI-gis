/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fusion;

import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

/**
 *
 * @author nick
 */
@WebServlet(name = "FuseLinkServlet", urlPatterns = {"/FuseLinkServlet"})
public class FuseLinkServlet extends HttpServlet {

    private static final String WKT = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String HAS_GEOMETRY = "http://www.opengis.net/ont/geosparql#hasGeometry";
    private DBConfig dbConf;
    private GraphConfig grConf;
    private VirtGraph vSet = null;
    private static final String DB_URL = "jdbc:postgresql:";
    private PreparedStatement stmt = null;
    private Connection dbConn = null;
    private ResultSet rs = null;
    private FusionState fs = null;
    private String tGraph = null;
    private String nodeA = null;
    private String nodeB = null;
    
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
        PrintWriter out = response.getWriter();
        
        HttpSession sess = request.getSession(true);
        
        try {
            
        
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            fs = (FusionState)sess.getAttribute("fstate");
            nodeA = (String)sess.getAttribute("nodeA");
            nodeB = (String)sess.getAttribute("nodeB");
            tGraph = (String)sess.getAttribute("t_graph");
            
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
        
            try{
                Class.forName("org.postgresql.Driver");     
            } catch (ClassNotFoundException ex) {
                System.out.println(ex.getMessage());      
                out.println("Class of postgis failed");
                out.close();
            
                return;
            }
             
            try {
                String url = DB_URL.concat(dbConf.getDBName());
                dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
                dbConn.setAutoCommit(false);
            } catch(SQLException sqlex) {
                System.out.println(sqlex.getMessage());      
                out.println("Connection to postgis failed");
                out.close();
            
                return;
            }
            String[] props = request.getParameterValues("props[]");
            
            System.out.println("Fusing : "+nodeA+" "+nodeB);
            AbstractFusionTransformation trans = null;
            for(int i = 0; i < 5; i+=5) {
                System.out.println(props[3]);
                trans = FuserPanel.transformations.get(props[3]);
                System.out.println(trans == null);
                trans.fuse(dbConn, nodeA, nodeB);
            }
            VirtuosoImporter virtImp = (VirtuosoImporter)sess.getAttribute("virt_imp");
            virtImp.setTransformationID(trans.getID());
            virtImp.importGeometriesToVirtuoso((String)sess.getAttribute("t_graph"));
            
            
            for(int i = 5; i < props.length; i+=5) {
                System.out.println("Pred : "+props[i]);
                System.out.println("Pred : "+props[i+1]);
                System.out.println("Pred : "+props[i+2]);
                System.out.println("Pred : "+props[i+3]);
                System.out.println("Pred : "+props[i+4]);
                handleMetadataFusion(props[i+3], i);
            }
            System.out.println("List : "+ fs.actions);
            System.out.println("List : "+ fs.objsA);
            System.out.println("List : "+ fs.objsB);
            System.out.println("List : "+ fs.preds);
            System.out.println("List : "+ fs.predsA);
            System.out.println("List : "+ fs.predsB);            
            
            virtImp.trh.finish();
            //System.out.println(FuserPanel.transformations);
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet FuseLinkServlet</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet FuseLinkServlet at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            out.close();
        }
    }
    
    private void handleMetadataFusion(String action, int idx) throws SQLException {
        if (action.equals("Keep Left")) {
            metadataKeepLeft(idx);
        }
        if (action.equals("Keep Right")) {
            metadataKeepRight(idx);
        }
        if (action.equals("Keep Both")) {
            metadataKeepBoth(idx);
        }
        if (action.equals("Keep Concatenated Right")) {
            
        }
        if (action.equals("Keep Concatenated Left")) {
            metadataKeepConcatLeft(idx);
        }
        if (action.equals("Keep Concatenated Both")) {
            
        }
        if (action.equals("Keep Flattened Right")) {
            metadataKeepFlatRight(idx);
        }
        if (action.equals("Keep Flattened Left")) {
            metadataKeepFlatLeft(idx);
        }
        if (action.equals("Keep Flattened Both")) {
            metadataKeepFlatLeft(idx);
            metadataKeepFlatRight(idx);  
        }
    }
    
    private void metadataKeepFlatLeft(int idx) throws SQLException {
        Connection virt_conn = vSet.getConnection();
        StringBuilder concat_str = new StringBuilder();
        for (String s : fs.predsA) {
            String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("sparql SELECT * ");
            String prev_s = "<"+nodeA+">";
            q.append(" WHERE {\n GRAPH <"+tGraph+"metadataA> {");
            if (pres.length == 1) {
                StringBuilder sq = new StringBuilder();
                sq.append("INSERT { GRAPH <"+tGraph+"> { ");
                sq.append(prev_s+" <"+pres[0]+"> ?o"+0+" . } ");
                sq.append(" } WHERE {\n GRAPH <"+tGraph+"metadataA> { "+prev_s+" <"+pres[0]+"> ?o"+0+" . } }");
                
                //System.out.println(sq.toString());
                
                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(sq.toString(), vSet);
                vur.exec();
                
                return;
            }
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            //System.out.println(q.toString());
            
            PreparedStatement stmt;
            stmt = virt_conn.prepareStatement(q.toString());
            ResultSet rs = stmt.executeQuery();
            
            while(rs.next()) {
                StringBuilder insq = new StringBuilder();
                insq.append("INSERT { GRAPH <"+tGraph+"> { ");
                prev_s = "<"+nodeA+">";
                for (int i = 0; i < pres.length-2; i++) {
                    insq.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                    prev_s = "?o"+i;
                }
                String o = rs.getString(pres.length);
                String simplified = StringUtils.substringAfter(pres[pres.length-1], "#");
                if (simplified.equals("") ) {
                    simplified = StringUtils.substring(pres[pres.length-1], StringUtils.lastIndexOf(pres[pres.length-1], "/")+1);
                }
                String o2 = rs.getString(pres.length-1);
                insq.append(prev_s+" <"+o2+"_"+simplified+"> \""+o+"\"");
                insq.append("} } WHERE {\n GRAPH <"+tGraph+"metadataA> {");
                prev_s = "<"+nodeA+">";
                for (int i = 0; i < pres.length; i++) {
                    insq.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                    prev_s = "?o"+i;
                }
                
                insq.append("} }");
                //System.out.println(insq.toString());
                
                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(insq.toString(), vSet);
                vur.exec();
            }
            
            //System.out.println(q.toString());
        }
    }
    
    private void metadataKeepFlatRight(int idx) throws SQLException {
        Connection virt_conn = vSet.getConnection();
        StringBuilder concat_str = new StringBuilder();
        for (String s : fs.predsB) {
            String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("sparql SELECT * ");
            String prev_s = "<"+nodeA+">";
            q.append(" WHERE {\n GRAPH <"+tGraph+"metadataB> {");
            if (pres.length == 1) {
                StringBuilder sq = new StringBuilder();
                sq.append("INSERT { GRAPH <"+tGraph+"> { ");
                sq.append(prev_s+" <"+pres[0]+"> ?o"+0+" . } ");
                sq.append(" } WHERE {\n GRAPH <"+tGraph+"metadataB> { "+prev_s+" <"+pres[0]+"> ?o"+0+" . } }");
                
                //System.out.println(sq.toString());
                
                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(sq.toString(), vSet);
                vur.exec();
                
                return;
            }
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            //System.out.println(q.toString());
            
            PreparedStatement stmt;
            stmt = virt_conn.prepareStatement(q.toString());
            ResultSet rs = stmt.executeQuery();
            
            while(rs.next()) {
                StringBuilder insq = new StringBuilder();
                insq.append("INSERT { GRAPH <"+tGraph+"> { ");
                prev_s = "<"+nodeA+">";
                for (int i = 0; i < pres.length-2; i++) {
                    insq.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                    prev_s = "?o"+i;
                }
                String o = rs.getString(pres.length);
                String simplified = StringUtils.substringAfter(pres[pres.length-1], "#");
                if (simplified.equals("") ) {
                    simplified = StringUtils.substring(pres[pres.length-1], StringUtils.lastIndexOf(pres[pres.length-1], "/")+1);
                }
                String o2 = rs.getString(pres.length-1);
                insq.append(prev_s+" <"+o2+"_"+simplified+"> \""+o+"\"");
                insq.append("} } WHERE {\n GRAPH <"+tGraph+"metadataB> {");
                prev_s = "<"+nodeA+">";
                for (int i = 0; i < pres.length; i++) {
                    insq.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                    prev_s = "?o"+i;
                }
                
                insq.append("} }");
                //System.out.println(insq.toString());
                
                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(insq.toString(), vSet);
                vur.exec();
            }
            
            //System.out.println(q.toString());
        }
    }
    
    private void metadataKeepConcatLeft(int idx) throws SQLException {
        Connection virt_conn = vSet.getConnection();
        StringBuilder concat_str = new StringBuilder();
        for (String s : fs.predsA) {
            String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("sparql SELECT * ");
            String prev_s = "<"+nodeA+">";
            q.append(" WHERE {\n GRAPH <"+tGraph+"metadataA> {");
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            //System.out.println(q.toString());
            
            PreparedStatement stmt;
            stmt = virt_conn.prepareStatement(q.toString());
            ResultSet rs = stmt.executeQuery();
            
            while(rs.next()) {
                //for (int i = 0; i < pres.length; i++) {
                    String o = rs.getString(pres.length);
                    String simplified = StringUtils.substringAfter(pres[pres.length-1], "#");
                    if (simplified.equals("") ) {
                        simplified = StringUtils.substring(pres[pres.length-1], StringUtils.lastIndexOf(pres[pres.length-1], "/")+1);
                    }
                    //concat_str.append(simplified+":"+o+" ");
                    concat_str.append(o+" ");
                //}
            }
            
            //System.out.println(q.toString());
        }
        concat_str.setLength(concat_str.length()-1);
        int lastIdx = StringUtils.lastIndexOf(fs.predsA.get(0), ",");
        String pred = fs.predsA.get(0).substring(0, lastIdx);
        String[] pres = StringUtils.split(pred, ",");
        StringBuilder q = new StringBuilder();
        q.append("INSERT { GRAPH <"+tGraph+"> { ");
        String prev_s = "<"+nodeA+">";
        for (int i = 0; i < pres.length-1; i++) {
            q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append(prev_s+" ?o"+(pres.length-1)+" \""+concat_str+"\"");
        q.append("} } WHERE {\n GRAPH <"+tGraph+"metadataA> {");
        prev_s = "<"+nodeA+">";
        for (int i = 0; i < pres.length; i++) {
            q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append("} }");
        //System.out.println(q.toString());
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
        vur.exec();
        
        //System.out.println(concat_str);
    }
    
    private void metadataKeepConcatBoth(int idx) {
        
    }
    
    private void metadataKeepConcatRight(int idx) {
        
    }
    
    private void metadataKeepLeft(int idx) {
        //<"+nodeA+"> ?p ?o } } WHERE {\n"
                    //"GRAPH <http://localhost:8890/fused_datasetmetadataA> { <"+nodeA+"> ?p ?o } }";*/
        for (String s : fs.predsA) {
            String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("INSERT { GRAPH <"+tGraph+"> { ");
            String prev_s = "<"+nodeA+">";
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            prev_s = "<"+nodeA+">";
            q.append("} } WHERE {\n GRAPH <"+tGraph+"metadataA> {");
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            //System.out.println(q.toString());
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        }
        
    }

    private void metadataKeepBoth(int idx) {
        for (String s : fs.predsA) {
            String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("INSERT { GRAPH <"+tGraph+"> { ");
            String prev_s = "<"+nodeA+">";
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            prev_s = "<"+nodeA+">";
            q.append("} } WHERE {\n GRAPH <"+tGraph+"metadataA> {");
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            //System.out.println(q.toString());
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        }
        
        for (String s : fs.predsB) {
            String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("INSERT { GRAPH <"+tGraph+"> { ");
            String prev_s = "<"+nodeA+">";
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            prev_s = "<"+nodeA+">";
            q.append("} } WHERE {\n GRAPH <"+tGraph+"metadataB> {");
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            //System.out.println(q.toString());
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        }
    }
    
    private void metadataKeepRight(int idx) {
        for (String s : fs.predsB) {
            String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("INSERT { GRAPH <"+tGraph+"> { ");
            String prev_s = "<"+nodeA+">";
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            prev_s = "<"+nodeA+">";
            q.append("} } WHERE {\n GRAPH <"+tGraph+"metadataB> {");
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            //System.out.println(q.toString());
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        }
    }
    private String formInsertQuery(String tGraph, String subject, String fusedGeometry) { 
        return "INSERT INTO <" + tGraph + "> { <" + subject + "> <" + HAS_GEOMETRY + "> _:a . _:a <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> }";
    }
    
    private String formInsertQuery(String tGraph, String subject, String predicate, String object){
        return "WITH <" + tGraph + "> INSERT { <" + subject +"> <" + predicate +"> " + object +" }";
    }
    
    private void executeVirtuosoUpdate(String updateQuery) {                
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(updateQuery, vSet);
        
        //VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create (query, set);
        //vqe.execSelect();
        //update_handler.addUpdate(updateQuery);
        vur.exec();
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
            Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
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
        try {        
            processRequest(request, response);
        } catch (SQLException ex) {
            Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(FuseLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
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
