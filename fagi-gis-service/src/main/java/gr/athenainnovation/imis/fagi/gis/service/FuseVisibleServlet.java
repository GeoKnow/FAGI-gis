/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gr.athenainnovation.imis.fusion.gis.core.GeometryFuser;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author nick
 */
@WebServlet(name = "FuseVisibleServlet", urlPatterns = {"/FuseVisibleServlet"})
public class FuseVisibleServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:postgresql:";
    private PreparedStatement stmt = null;
    private Connection dbConn = null;
    private ResultSet rs = null;
    private JSONGeometries ret = null;
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    
    private class JSONLink {
        String geomA;
        String subA;
        String subB;
        String geomB;

        public JSONLink() {
        }

        public String getGeomA() {
            return geomA;
        }

        public void setGeomA(String geomA) {
            this.geomA = geomA;
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

        public String getGeomB() {
            return geomB;
        }

        public void setGeomB(String geomB) {
            this.geomB = geomB;
        }

    }
    
    private class JSONGeometries {
        List<JSONLink> linked_ents;

        public JSONGeometries() {
            linked_ents = new LinkedList<>();
        }

        public List<JSONLink> getLinked_ents() {
            return linked_ents;
        }

        public void setLinked_ents(List<JSONLink> linked_ents) {
            this.linked_ents = linked_ents;
        }
        
        
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        response.setContentType("text/html;charset=UTF-8");
        
        HttpSession sess = request.getSession(true);
        GraphConfig grConf = (GraphConfig)sess.getAttribute("gr_conf");
        DBConfig dbConf = (DBConfig)sess.getAttribute("db_conf");
        
        try (PrintWriter out = response.getWriter()) {
            //System.out.println(request.getParameter("top"));
            //System.out.println(request.getParameter("right"));
            //System.out.println(request.getParameter("bottom"));
            //System.out.println(request.getParameter("left"));
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            mapper.setDateFormat(outputFormat);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            
            ret = new JSONGeometries();
            double bLeft = Double.parseDouble(request.getParameter("left"));
            double bBottom = Double.parseDouble(request.getParameter("bottom"));
            double bRight = Double.parseDouble(request.getParameter("right"));
            double bTop = Double.parseDouble(request.getParameter("top"));
            
            String selectFromBBoxA = "SELECT dataset_a_geometries.subject AS s, ST_AsText(dataset_a_geometries.geom) AS g\n" +
                                    "FROM dataset_a_geometries\n" +
                                    "WHERE dataset_a_geometries.geom && ST_Transform(ST_MakeEnvelope(?, ?, ?, ?, 900913), 4326)";
            String selectFromBBoxB = "SELECT dataset_b_geometries.subject AS s, ST_AsText(dataset_b_geometries.geom) AS g\n" +
                                    "FROM dataset_b_geometries\n" +
                                    "WHERE dataset_b_geometries.geom && ST_Transform(ST_MakeEnvelope(?, ?, ?, ?, 900913), 4326)";
            
            String selectVisible = "SELECT links.nodea AS sa, links.nodeb AS sb , St_asText(a_g) AS ga, ST_asText(b_g) AS gb\n" +
                                    "FROM links \n" +
                                    "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s, dataset_b_geometries.subject AS b_s,\n" +
                                    "		dataset_a_geometries.geom AS a_g, dataset_b_geometries.geom AS b_g,\n" +
                                    "		ST_X(dataset_a_geometries.geom) AS a_x, ST_Y(dataset_a_geometries.geom) AS a_y,\n" +
                                    "		ST_X(ST_Centroid(dataset_b_geometries.geom)) AS b_x, ST_Y(ST_Centroid(dataset_b_geometries.geom)) AS b_y\n" +
                                    "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s)";
            System.out.println(selectVisible);
            try {
                String url = DB_URL.concat(dbConf.getDBName());
                dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
                //dbConn.setAutoCommit(false);
            } catch(SQLException sqlex) {
                System.out.println(sqlex.getMessage());      
                out.println("Connection to postgis failed");
                out.close();
            
                return;
            }
            
            stmt = dbConn.prepareStatement(selectFromBBoxA);
            stmt.setDouble(1, bLeft);
            stmt.setDouble(2, bBottom);
            stmt.setDouble(3, bRight);
            stmt.setDouble(4, bTop);
            rs = stmt.executeQuery();
            
            int count = 0;
            List<Link> lst= new ArrayList<>();
            HashMap<String, String> linksHashed = (HashMap<String, String>)sess.getAttribute("links");
            while (rs.next()) {
                String subject = rs.getString("s");
                Link lnk = new Link(subject, linksHashed.get(subject));
                lst.add(lnk);
            }
            System.out.println("Count: "+lst.size());

            rs.close();
            stmt.close();
            for ( Link s : lst ) {
                System.out.println("Link "+s.getNodeA()+" "+s.getNodeB());
            }
            
            final GeometryFuser geometryFuser = new GeometryFuser();
            try {
                geometryFuser.connect(dbConf);
                geometryFuser.loadLinks(lst);
            }
            catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            finally {
                geometryFuser.clean();
            } 
            try{
                Class.forName("org.postgresql.Driver");     
            } catch (ClassNotFoundException ex) {
                System.out.println(ex.getMessage());      
                out.println("Class of postgis failed");
                out.close();
            
                return;
            }
            
            stmt = dbConn.prepareStatement(selectVisible);
            rs = stmt.executeQuery();
            
            ret = new JSONGeometries();
            while ( rs.next() ) {
                JSONLink jlnk = new JSONLink();
                jlnk.geomA = rs.getString("ga");
                jlnk.geomB = rs.getString("gb");
                jlnk.subA = rs.getString("sa");
                jlnk.subB = rs.getString("sa");
                
                ret.linked_ents.add(jlnk);
            }
            
            System.out.println(mapper.writeValueAsString(ret));
            rs.close();
            stmt.close();
            /*
            stmt = dbConn.prepareStatement(selectFromBBoxB);
            stmt.setDouble(1, bLeft);
            stmt.setDouble(2, bBottom);
            stmt.setDouble(3, bRight);
            stmt.setDouble(4, bTop);
            rs = stmt.executeQuery();
            
            count = 0;
            while (rs.next()) {
                ret.subsB.add(rs.getString("s"));
                ret.geomsB.add(rs.getString("g"));
                count++;
            }
            System.out.println("Count: "+count);
            System.out.println(mapper.writeValueAsString(ret));
            */
            /* TODO output your page here. You may use following sample code. */
            
            out.println(mapper.writeValueAsString(ret));
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
            Logger.getLogger(FuseVisibleServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(PreviewServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(PreviewServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(PreviewServlet.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(FuseVisibleServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex) {
                    Logger.getLogger(PreviewServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    Logger.getLogger(PreviewServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (dbConn != null) {
                try {
                    dbConn.close();
                } catch (SQLException ex) {
                    Logger.getLogger(PreviewServlet.class.getName()).log(Level.SEVERE, null, ex);
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
