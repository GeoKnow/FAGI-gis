
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
//import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONFusion;
import gr.athenainnovation.imis.fusion.gis.json.JSONPropertyMatch;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.utils.Constants;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
//import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * @author Nick Vitsas
 */

@WebServlet(name = "FusionServlet", urlPatterns = {"/FusionServlet"})
public class FusionServlet extends HttpServlet {

    private static final org.apache.log4j.Logger LOG = Log.getClassFAGILogger(FusionServlet.class);
    
    private void analyzeChain(List<String> a, List<String> b, String qa, String qb, SchemaMatchState sms) {
        
        // Look for property entries with the same prefix
        for (Map.Entry pairs : sms.foundA.entrySet()) {
            String key = (String)pairs.getKey();
            int firstApp = key.indexOf(qa);
            if (firstApp == 0 && !key.equals(qa)) {
                a.add(key);
            }
        }
        
        for (Map.Entry pairs : sms.foundB.entrySet()) {
            String key = (String)pairs.getKey();
            int firstApp = key.indexOf(qb);
            if (firstApp == 0 && !key.equals(qb)) {
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
    
    private PreparedStatement constructQuery (String[] a, String queryGraph, String reg, Connection conn) {      
        PreparedStatement stmt = null;
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
        
        try {
            stmt = conn.prepareStatement(query.toString());
        } catch (SQLException ex) {
            stmt = null;
        }
                
        return stmt;
    }
    
    private String queryChains(HttpSession sess, JSONFusion ret, List<FusionState> ft, List<String> a, List<String> b, String p, String pl, String reg, Connection co, DBConfig dbConf, GraphConfig grConf) {
        ResultSet rsa = null;
        ResultSet rsb = null;
        PreparedStatement stmt =null;
        StringBuilder sb = new StringBuilder();
        JSONPropertyMatch pm = new JSONPropertyMatch();
        pm.setProperty(p);
        pm.setPropertyLong(pl);
        ft.get(ft.size()-1).preds.add(p);
        String graph = (String)sess.getAttribute("t_graph");
        String result;
        
        for(int i = 0; i < a.size(); i++){
            ft.get(ft.size()-1).predsA.add(a.get(i));
            String[] toks = a.get(i).split("\\|");
            for (String s : toks) {
                String[] pieces = s.split(",");
                System.out.println("Pieces size :" + pieces.length + " " + pieces);
                
                try {
                    stmt = constructQuery(pieces, grConf.getMetadataGraphA(), reg, co);
                    rsa = stmt.executeQuery();
                    
                    StringBuilder param = new StringBuilder();
                    boolean found = false;
                    while (rsa.next()) {
                        found = true;
                        sb.append(rsa.getString(pieces.length) + " ");
                        param.append(rsa.getString(pieces.length) + " ");
                        ft.get(ft.size() - 1).objsA.add(rsa.getString(pieces.length));

                    }

                    rsa.close();

                    stmt.close();

                    if (!found) {
                        continue;
                    }

                    int prevLen = param.length();
                    param.setLength(prevLen - 1);
                    pm.valueA += param.toString() + " ";
                } catch (SQLException ex) {
                    Logger.getLogger(FusionServlet.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    if ( rsa != null ) {
                        try {
                            rsa.close();
                        } catch (SQLException ex) {
                        }
                    }
                    if ( stmt != null ) {
                        try {
                            stmt.close();
                        } catch (SQLException ex) {
                        }
                    }
                }
            }
        }
        
        System.out.println(b);
        for(int i = 0; i < b.size(); i++){
            ft.get(ft.size()-1).predsB.add(b.get(i));
            System.out.println("E MPIKA MWRE");
            String[] toks = b.get(i).split("\\|");
            for (String s : toks) {
                String[] pieces = s.split(",");
                System.out.println("Pieces size :" + pieces.length + " " + pieces);
                StringBuilder param = new StringBuilder();
                try {
                    stmt = constructQuery(pieces, grConf.getMetadataGraphB(), reg, co);
                    rsb = stmt.executeQuery();
                    boolean found = false;
                    while (rsb.next()) {
                        found = true;
                        sb.append(rsb.getString(pieces.length) + " ");
                        param.append(rsb.getString(pieces.length) + " ");
                        ft.get(ft.size() - 1).objsB.add(rsb.getString(pieces.length));
                    }

                    rsb.close();
                    stmt.close();

                    if (!found) {
                        continue;
                    }

                    int prevLen = param.length();
                    param.setLength(prevLen - 1);
                    pm.valueB += param.toString() + " ";
                } catch (SQLException ex) {
                } finally {
                    if ( rsa != null ) {
                        try {
                            rsa.close();
                        } catch (SQLException ex) {
                        }
                    }
                    if ( stmt != null ) {
                        try {
                            stmt.close();
                        } catch (SQLException ex) {
                        }
                    }
                }
            }
        }
        
        System.out.println(pm.valueA+" "+pm.valueB);
        ret.getProperties().add(pm);
        
        return sb.toString();
    }
    
    private boolean getGeom(String reg, DBConfig dbConf, JSONFusion ret) {
        boolean success = true;
        
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            LOG.trace("ClassNotFoundException thrown");
            LOG.debug("ClassNotFoundException thrown : " + ex.getMessage());
            
            success = false;
            
            return success;
        }

        Connection dbConn = null;
        try {
            String url = Constants.DB_URL.concat(dbConf.getDBName());
            dbConn = DriverManager.getConnection(url, dbConf.getDBUsername(), dbConf.getDBPassword());
        } catch (SQLException sqlex) {
            LOG.trace("SQLException thrown");
            LOG.debug("SQLException thrown : " + sqlex.getMessage());
            LOG.debug("SQLException thrown : " + sqlex.getSQLState());

            success = false;

            return success;
        } finally {
            try {
                if (dbConn != null) {
                    dbConn.close();
                }
            } catch (SQLException sqlex) {
                LOG.trace("SQLException thrown when closeing connection");
                LOG.debug("SQLException thrown when closeing connection : " + sqlex.getMessage());
                LOG.debug("SQLException thrown when closeing connection : " + sqlex.getSQLState());
            }
        }

        // Get geometry of bot entities from PostGIS
        String selectLinkedGeoms = "SELECT ST_asText(a_g) as ga, ST_asText(b_g) as gb\n"
                + "FROM links \n"
                + "INNER JOIN (SELECT dataset_a_geometries.subject AS a_s,\n"
                + "		   dataset_b_geometries.subject AS b_s,\n"
                + "		   dataset_a_geometries.geom AS a_g,\n"
                + "		   dataset_b_geometries.geom AS b_g\n"
                + "		FROM dataset_a_geometries, dataset_b_geometries) AS geoms \n"
                + "		ON(links.nodea = geoms.a_s AND links.nodeb = geoms.b_s) WHERE links.nodea = '" + reg + "'";

        System.out.println(selectLinkedGeoms);
       
        try (PreparedStatement stmt = dbConn.prepareStatement(selectLinkedGeoms);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ret.getGeomsA().add(rs.getString("ga"));
                ret.getGeomsB().add(rs.getString("gb"));
            }
        } catch (SQLException sqlex) {
            LOG.trace("SQLException thrown");
            LOG.debug("SQLException thrown : " + sqlex.getMessage());
            LOG.debug("SQLException thrown : " + sqlex.getSQLState());

            success = false;

            return success;
        } finally {
            try {
                if (dbConn != null) {
                    dbConn.close();
                }
            } catch (SQLException sqlex) {
                LOG.trace("SQLException thrown when closeing connection");
                LOG.debug("SQLException thrown when closeing connection : " + sqlex.getMessage());
                LOG.debug("SQLException thrown when closeing connection : " + sqlex.getSQLState());
            }
        }
        
        try {
            if ( dbConn != null )
                dbConn.close();
        } catch (SQLException sqlex) {
            LOG.trace("SQLException thrown when closeing connection");
            LOG.debug("SQLException thrown when closeing connection : " + sqlex.getMessage());
            LOG.debug("SQLException thrown when closeing connection : " + sqlex.getSQLState());
        }
        
        return success;
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
            throws ServletException {
        response.setContentType("text/html;charset=UTF-8");
        
        //  Per session state
        PrintWriter             out = null;
        Connection              conn;
        String                  subject;
        String                  tgraph;
        List<FusionState>       ft = null;
        VirtGraph               vSet = null;
        Connection              dbConn = null;
        DBConfig                dbConf = null;
        HttpSession             sess;
        JSONFusion              ret;      
        JSONRequestResult       res;      
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
            
            ret = new JSONFusion();
            res = new JSONRequestResult();
            ret.setResult(res);
            
            sess = request.getSession(false);

            if (sess == null) {
                LOG.trace("Not a valid session");
                LOG.debug("Not a valid session" );
                
                ret.getResult().setMessage("Failed to create session!");
                ret.getResult().setStatusCode(-1);
                
                out.println(mapper.writeValueAsString(ret));

                out.close();

                return;

            }

            ft = new ArrayList<>();
            String[] selectedPreds = request.getParameterValues("props[]");

            GraphConfig grConf = (GraphConfig) sess.getAttribute("gr_conf");
            dbConf = (DBConfig) sess.getAttribute("db_conf");
            HashMap<String, String> hashLinks = (HashMap<String, String>) sess.getAttribute("links");

            try {
                vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                        dbConf.getUsername(),
                        dbConf.getPassword());
            } catch (JenaException connEx) {
                LOG.trace("Failed to create Jena VirtGraph");
                LOG.trace("Failed to create Jena VirtGraph : " + connEx.getMessage());

                ret.getResult().setMessage("Failed to perform property matching!");
                ret.getResult().setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));

                out.close();

                return;
            }

            conn = vSet.getConnection();
            System.out.println("Selected Preds : ");
            for (String s : selectedPreds) {
                System.out.println(s);
            }
            
            // The first element in the incoming array is
            // always the subject of the entity
            subject = selectedPreds[0];

            // Get the geometry of the entity
            success = getGeom(subject, dbConf, ret);
            
            // Where to look for the name of the other entity
            String getLink = "";
            if (grConf.isDominantA()) {
                getLink = "SPARQL SELECT ?o WHERE { GRAPH <" + grConf.getAllLinksGraph() + "> {<" + subject + "> ?p ?o} }";
            } else {
                getLink = "SPARQL SELECT ?o WHERE { GRAPH <" + grConf.getAllLinksGraph() + "> {?o ?p <" + subject + ">} }";
            }
            
            String linked = "";
            try (PreparedStatement stmt = conn.prepareStatement(getLink);
                    ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    linked = rs.getString("?o");
                }
                
            } catch (SQLException ex) {
                LOG.trace("Failed to get other Link");
                LOG.trace("Failed to get other Link : " + ex.getMessage());
                LOG.trace("Failed to get other Link : " + ex.getSQLState());

                ret.getResult().setMessage("Failed to perform property matching!");
                ret.getResult().setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));

                out.close();

                return;
            }
            
            sess.setAttribute("nodeA", subject);
            sess.setAttribute("nodeB", linked);
               
            String getClassA = "SPARQL SELECT ?owlClass"
                    + "  WHERE {GRAPH <" + grConf.getAllLinksGraph() + ">"
                    + " { <" + subject + "> <http://www.w3.org/2002/07/owl#sameAs> ?o } . \n"
                    + " GRAPH <" + grConf.getMetadataGraphA() + "> {<" + subject + "> <" + Constants.OWL_CLASS_PROPERTY + "> ?owlClass } }";

            String getClassB = "sparql SELECT ?owlClass"
                    + "  WHERE {GRAPH <" + grConf.getAllLinksGraph() + ">"
                    + " { <" + subject + "> <http://www.w3.org/2002/07/owl#sameAs> ?o } . \n"
                    + " GRAPH <" + grConf.getMetadataGraphB() + "> {<" + subject + "> <" + Constants.OWL_CLASS_PROPERTY + "> ?owlClass } }";

           
            try (PreparedStatement stmt = conn.prepareStatement(getClassA);
                    ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    ret.getClassesA().add(rs.getString("?owlClass"));
                }
                
            } catch (SQLException ex) {
                LOG.trace("Failed to get other Link");
                LOG.trace("Failed to get other Link : " + ex.getMessage());
                LOG.trace("Failed to get other Link : " + ex.getSQLState());

                ret.getResult().setMessage("Failed to perform property matching!");
                ret.getResult().setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));

                out.close();

                return;
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(getClassB);
                    ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    ret.getClassesB().add(rs.getString("?owlClass"));
                }
                
            } catch (SQLException ex) {
                LOG.trace("Failed to get other Link");
                LOG.trace("Failed to get other Link : " + ex.getMessage());
                LOG.trace("Failed to get other Link : " + ex.getSQLState());

                ret.getResult().setMessage("Failed to perform property matching!");
                ret.getResult().setStatusCode(-1);

                out.println(mapper.writeValueAsString(ret));

                out.close();

                return;
            }

            SchemaMatchState sms = (SchemaMatchState) sess.getAttribute("predicates_matches");
            
            // Add a dummy property for convenience 
            if (selectedPreds.length == 1) {
                JSONPropertyMatch pm = new JSONPropertyMatch();
                pm.setProperty("dummy");
                ret.getProperties().add(pm);
            }
            
            // Iterate over all selected properties
            // Skip fiest element
            // Step = 2 as
            // Index j contains the new property name
            // Index j + 1 contains the old property
            for (int j = 1; j < selectedPreds.length; j += 2) {
                // Special treatment for WKT
                if (selectedPreds[j + 1].equals("http://www.opengis.net/ont/geosparql#asWKT")) {
                    continue;
                }
                
                String newPropertyName = selectedPreds[j];
                String prevPropertyName = selectedPreds[j + 1];
                //System.out.println("Breaking on " + prevPropertyName);
                // 
                String[] queryStrings = prevPropertyName.split(Constants.PROPERTY_SEPARATOR);
                String queryA = queryStrings[0];
                String queryB = queryStrings[1];

                List<String> presA = new ArrayList<String>();
                List<String> presB = new ArrayList<String>();
                ft.add(new FusionState());
                analyzeChain(presA, presB, queryA, queryB, sms);
                String json_obj = queryChains(sess, ret, ft, presA, presB, newPropertyName, prevPropertyName, subject, conn, dbConf, grConf);
            }
            System.out.println("Geom JSON " + mapper.writeValueAsString(ret));

            for (Map.Entry<String, AbstractFusionTransformation> entry : FuserPanel.transformations.entrySet()) {
                //System.out.println("Transformation "+entry.getKey());
                ret.getGeomTransforms().add(entry.getKey());
            }

            for (Map.Entry<String, String> entry : FuserPanel.meta_transformations.entrySet()) {
                //System.out.println("Transformation "+entry.getKey());
                ret.getMetaTransforms().add(entry.getKey());
            }

            // Set the fusion state so that 
            // If fusion takes place later,
            // the state will be in the session
            sess.setAttribute("fstate", ft);

            //System.out.println(mapper.writeValueAsString(ret));
            out.println(mapper.writeValueAsString(ret));
        } catch (JsonProcessingException ex) {
            LOG.trace("JsonProcessingException thrown");
            LOG.debug("JsonProcessingException thrown : " + ex.getMessage());
            
        } catch ( java.lang.OutOfMemoryError oome) {
            LOG.trace("OutOfMemoryError thrown");
            LOG.debug("OutOfMemoryError thrown : " + oome.getMessage());
            
        } finally {
            if ( vSet != null ) {
                vSet.close();
            }
            if (out != null) {
                out.close();
            }
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
