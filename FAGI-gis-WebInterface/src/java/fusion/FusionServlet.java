
package fusion;

import com.hp.hpl.jena.shared.JenaException;
//import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.virtuoso.Schema;
import gr.athenainnovation.imis.fusion.gis.virtuoso.SchemaMatchState;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
//import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
//import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
//import java.util.List;
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

/**
 *
 * @author nick
 */

@WebServlet(name = "FusionServlet", urlPatterns = {"/FusionServlet"})
public class FusionServlet extends HttpServlet {

    private Connection conn;
    private String reg;
    private String tgraph;
    private FusionState ft = null;
    private static final String DB_URL = "jdbc:postgresql:";
    private PreparedStatement stmt = null;
    private Connection dbConn = null;
    private ResultSet rs = null;
    private DBConfig dbConf = null;
    private HttpSession sess;
            
    private void analyzeChain(List<String> a, List<String> b, String qa, String qb, SchemaMatchState sms) {
        for (Map.Entry pairs : sms.foundA.entrySet()) {
            String key = (String)pairs.getKey();
            if (key.contains(qa) && !key.equals(qa)) {
                a.add(key);
            }
        }
        for (Map.Entry pairs : sms.foundB.entrySet()) {
            String key = (String)pairs.getKey();
            if (key.contains(qb) && !key.equals(qb)) {
                b.add(key);
            }
        }
        
        if(a.isEmpty())
            a.add(qa);
        if(b.isEmpty())
            b.add(qb);
        
        System.out.print("First List: ");
        for ( String s : a ) 
            System.out.println(s);
        System.out.print("Second List: ");
        for ( String s : b ) 
            System.out.println(s);
    }
    
    private ResultSet constructQuery (String[] a, String queryGraph) throws SQLException {         
        StringBuilder query = new StringBuilder();
        query.append("sparql SELECT ");
        for(int i = 0; i < a.length; i++) {
            int idx = i;
            query.append("?oa"+idx+ " ");
        }                
        query.append("WHERE { \n");
        query.append("GRAPH <"+queryGraph+"> { <"+reg+"> ");
        String prev_s = "";
        for(int i = 0; i < a.length; i++) {
            int idx = i ;
            query.append(prev_s+"<"+a[i]+"> ?oa"+idx+ " . ");
            prev_s = "?oa"+idx+ " ";
        }
        query.append("\n} }");
        System.out.println("What to query "+query.toString());
        PreparedStatement stmt = conn.prepareStatement(query.toString());
        return stmt.executeQuery();
    }
    
    private String queryChains(List<String> a, List<String> b, String p) throws SQLException {
        ResultSet rsa;
        ResultSet rsb;
        StringBuilder sb = new StringBuilder();
        sb.append("{\""+"property"+"\":"+"\""+p+"\",");
        sb.append("\""+"left"+"\":");
        sb.append("\"");
        String graph = (String)sess.getAttribute("t_graph");
        for(int i = 0; i < a.size(); i++){
            ft.predsA.add(a.get(i));
            String[] pieces = a.get(i).split(",");
            System.out.println("Pieces size :"+pieces.length+" "+pieces);
            rsa = constructQuery(pieces, graph+"metadataA");
            while(rsa.next()) {
                String simplified = StringUtils.substringAfter(pieces[pieces.length-1], "#");
                if (simplified.equals("") ) {
                    simplified = StringUtils.substring(pieces[pieces.length-1], StringUtils.lastIndexOf(pieces[pieces.length-1], "/")+1);
                }
                sb.append(simplified+":"+rsa.getString(pieces.length)+" ");
                ft.objsA.add(rsa.getString(pieces.length));
                //sb.append("\""+"right"+"\":"+"\""+rightVal+"\"");
                //sb.append(" },");
            }
            
        }
        sb.setLength(sb.length()-1);
        sb.append("\",\""+"right"+"\":");
        sb.append("\"");
        System.out.println(b);
        for(int i = 0; i < b.size(); i++){
            ft.predsB.add(b.get(i));
            System.out.println("E MPIKA MWRE");
            String[] pieces = b.get(i).split(",");
            System.out.println("Pieces size :"+pieces.length+" "+pieces);
            rsb = constructQuery(pieces, graph+"metadataB");
            while(rsb.next()) {
                sb.append(rsb.getString(pieces.length)+" ");
                ft.objsB.add(rsb.getString(pieces.length));
                //sb.append("\""+"right"+"\":"+"\""+rightVal+"\"");
                //sb.append(" },");
            }
        }
        sb.setLength(sb.length()-1);
        sb.append("\"");
        sb.append(" },");
        System.out.println("Some hopes "+sb.toString());
        System.out.println("END FUSION");
        return sb.toString();
    }
    
    private String getGeom(String reg) throws SQLException {
        
        try{
                Class.forName("org.postgresql.Driver");     
        } catch (ClassNotFoundException ex) {
                System.out.println(ex.getMessage());      
                
                return "";
            }
             
            try {
                String url = DB_URL.concat(dbConf.getDBName());
                dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
                //dbConn.setAutoCommit(false);
            } catch(SQLException sqlex) {
                System.out.println(sqlex.getMessage());      
                //out.println("Connection to postgis failed");
                //out.close();
            
                return "";
            }
            
            String selectLinkedGeoms = "SELECT ST_asText(a_g) as ga, ST_asText(b_g) as gb\n" +
                                        "FROM links \n" +
                                        "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s,\n" +
                                        "		   dataset_b_geometries.subject AS b_s,\n" +
                                        "		   dataset_a_geometries.geom AS a_g,\n" +
                                        "		   dataset_b_geometries.geom AS b_g\n" +
                                        "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms \n" +
                                        "		ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s) WHERE links.nodea = '"+reg+"'";
            
            System.out.println(selectLinkedGeoms);
            stmt = dbConn.prepareStatement(selectLinkedGeoms);
            rs = stmt.executeQuery();
            StringBuilder sb = new StringBuilder();
            sb.append(" ],\"Geoms\":[{");
            while (rs.next()) {
                int parA = rs.getString(1).indexOf("(");
                int parB = rs.getString(2).indexOf("(");
                
                sb.append("\""+"left"+"\":"+"\""+rs.getString(1).substring(0, parA)+"\",");
                sb.append("\""+"right"+"\":"+"\""+rs.getString(2).substring(0, parB)+"\"");
            }
            sb.append("}]}");
            
            return sb.toString();
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
            throws ServletException, IOException, SQLException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {            
            
            System.out.println(request.getParameterMap());
            ft = new FusionState();
            String[] selectedPreds = request.getParameterValues("props[]");
            sess = request.getSession(true);            
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            HashMap<String, String> hashLinks = (HashMap<String, String>)sess.getAttribute("links");
            
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
        
        conn = vSet.getConnection();
        //System.out.println("Selected Preds : ");
            for ( String s : selectedPreds) {
                //System.out.println(s);
            }
            reg = selectedPreds[0];
            
            String geom_json = getGeom(reg);
            
            String getLink = "sparql select ?o where { GRAPH <http://localhost:8890/DAV/all_links> {<"+reg+"> ?p ?o} }";
            PreparedStatement stmt = conn.prepareStatement(getLink);
            ResultSet rs = stmt.executeQuery();
            
            String linked = "";
            while (rs.next()) {
                linked = rs.getString(1);
            }
            sess.setAttribute("nodeA", reg);
            sess.setAttribute("nodeB", linked);
            
            rs.close();
            stmt.close();
            
            StringBuilder sb = new StringBuilder();
            sb.append("{\"fusions\":[");
                
            SchemaMatchState sms = (SchemaMatchState)sess.getAttribute("predicates_matches");
            //System.out.println(sms.foundA);
            //System.out.println(sms.foundB);
            for ( int j = 1; j < selectedPreds.length; j+=2) {
                if (selectedPreds[j+1].equals("http://www.opengis.net/ont/geosparql#asWKT"))
                    continue;
                String newPropertyName = selectedPreds[j];
                String prevPropertyName = selectedPreds[j+1];
                       
                String[] queryStrings = prevPropertyName.split("=>");
                String queryA = queryStrings[0];
                String queryB = queryStrings[1];
                
                List<String> presA = new ArrayList<String>();
                List<String> presB = new ArrayList<String>();
            
                analyzeChain(presA, presB, queryA, queryB, sms);
                String json_obj = queryChains(presA, presB, newPropertyName);
                sb.append(json_obj);
            }
            
            sess.setAttribute("fstate", ft);
            
            int newLen = sb.length() - 1;
            sb.setLength(newLen);
            sb.append(geom_json);
            //System.out.println(sb);   
            out.println(sb.toString());
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
            Logger.getLogger(FusionServlet.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(FusionServlet.class.getName()).log(Level.SEVERE, null, ex);
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
