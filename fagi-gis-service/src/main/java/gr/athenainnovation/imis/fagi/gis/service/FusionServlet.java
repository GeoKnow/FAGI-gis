
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
//import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
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
 * @author nick
 */

@WebServlet(name = "FusionServlet", urlPatterns = {"/FusionServlet"})
public class FusionServlet extends HttpServlet {
    private static final String OWL_CLASS_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String DB_URL = "jdbc:postgresql:";

    private class JSONFusion {
        List<String> geomsA;
        List<String> geomsB;
        List<String> classesA;
        List<String> classesB;
        List<JSONPropertyMatch> properties;
        List<String> geomTransforms;
        List<String> metaTransforms;
        
        public JSONFusion() {
            this.geomsA = new ArrayList<>();
            this.geomsB = new ArrayList<>();
            this.classesA = new ArrayList<>();
            this.classesB = new ArrayList<>();
            this.properties = new ArrayList<>();
            this.geomTransforms = new ArrayList<>();
            this.metaTransforms = new ArrayList<>();
        }

        public List<JSONPropertyMatch> getProperties() {
            return properties;
        }

        public void setProperties(List<JSONPropertyMatch> properties) {
            this.properties = properties;
        }

        public List<String> getGeomsA() {
            return geomsA;
        }

        public void setGeomsA(List<String> geomsA) {
            this.geomsA = geomsA;
        }

        public List<String> getGeomsB() {
            return geomsB;
        }

        public void setGeomsB(List<String> geomsB) {
            this.geomsB = geomsB;
        }

        public List<String> getGeomTransforms() {
            return geomTransforms;
        }

        public void setGeomTransforms(List<String> geomTransforms) {
            this.geomTransforms = geomTransforms;
        }

        public List<String> getMetaTransforms() {
            return metaTransforms;
        }

        public void setMetaTransforms(List<String> metaTransforms) {
            this.metaTransforms = metaTransforms;
        }

        public List<String> getClassesA() {
            return classesA;
        }

        public void setClassesA(List<String> classesA) {
            this.classesA = classesA;
        }

        public List<String> getClassesB() {
            return classesB;
        }

        public void setClassesB(List<String> classesB) {
            this.classesB = classesB;
        }
                
    }
    
    private class JSONPropertyMatch {
        String valueA;
        String valueB;
        String property;
        String propertyLong;
        String result;

        public JSONPropertyMatch() {
            valueA = "";
            valueB = "";
            property = "";
            propertyLong = "";
            result = "";
        }

        public String getPropertyLong() {
            return propertyLong;
        }

        public void setPropertyLong(String propertyLong) {
            this.propertyLong = propertyLong;
        }

        public String getValueA() {
            return valueA;
        }

        public void setValueA(String valueA) {
            this.valueA = valueA;
        }

        public String getValueB() {
            return valueB;
        }

        public void setValueB(String valueB) {
            this.valueB = valueB;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
        
        
    }
    
    private void analyzeChain(List<String> a, List<String> b, String qa, String qb, SchemaMatchState sms) {
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
    
    private PreparedStatement constructQuery (String[] a, String queryGraph, String reg, Connection conn) throws SQLException {         
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
        //PreparedStatement stmt1 = conn.prepareStatement(query.toString());
        return conn.prepareStatement(query.toString());
    }
    
    private String queryChains(HttpSession sess, JSONFusion ret, List<FusionState> ft, List<String> a, List<String> b, String p, String pl, String reg, Connection co, DBConfig dbConf) throws SQLException {
        ResultSet rsa;
        ResultSet rsb;
        StringBuilder sb = new StringBuilder();
        JSONPropertyMatch pm = new JSONPropertyMatch();
        pm.property = p;
        pm.propertyLong = pl;
        sb.append("{\""+"property"+"\":"+"\""+p+"\",");
        sb.append("\""+"left"+"\":");
        sb.append("\"");
        ft.get(ft.size()-1).preds.add(p);
        String graph = (String)sess.getAttribute("t_graph");
        String result;
        for(int i = 0; i < a.size(); i++){
            ft.get(ft.size()-1).predsA.add(a.get(i));
            String[] toks = a.get(i).split("\\|");
            for (String s : toks) {
                String[] pieces = s.split(",");
                System.out.println("Pieces size :" + pieces.length + " " + pieces);
                PreparedStatement stmt = constructQuery(pieces, graph + "_" + dbConf.getDBName() + "A", reg, co);
                rsa = stmt.executeQuery();
                StringBuilder param = new StringBuilder();
                boolean found = false;
                while (rsa.next()) {
                    found = true;
                    sb.append(rsa.getString(pieces.length) + " ");
                    param.append(rsa.getString(pieces.length) + " ");
                    ft.get(ft.size() - 1).objsA.add(rsa.getString(pieces.length));
                //sb.append("\""+"right"+"\":"+"\""+rightVal+"\"");
                    //sb.append(" },");
                }
                
                rsa.close();
                stmt.close();
                
                if (!found) {
                    continue;
                }

                int prevLen = param.length();
                param.setLength(prevLen - 1);
                pm.valueA += param.toString() + " ";
            }
        }
        sb.setLength(sb.length()-1);
        sb.append("\",\""+"right"+"\":");
        sb.append("\"");
        System.out.println(b);
        for(int i = 0; i < b.size(); i++){
            ft.get(ft.size()-1).predsB.add(b.get(i));
            System.out.println("E MPIKA MWRE");
            String[] toks = b.get(i).split("\\|");
            for (String s : toks) {
                String[] pieces = s.split(",");
                System.out.println("Pieces size :" + pieces.length + " " + pieces);
                StringBuilder param = new StringBuilder();
                PreparedStatement stmt = constructQuery(pieces, graph + "_" + dbConf.getDBName() + "B", reg, co);
                rsb = stmt.executeQuery();
                boolean found = false;
                while (rsb.next()) {
                    found = true;
                    sb.append(rsb.getString(pieces.length) + " ");
                    param.append(rsb.getString(pieces.length) + " ");
                    ft.get(ft.size() - 1).objsB.add(rsb.getString(pieces.length));
                //sb.append("\""+"right"+"\":"+"\""+rightVal+"\"");
                    //sb.append(" },");
                }

                rsb.close();
                stmt.close();
                
                if (!found) {
                    continue;
                }

                int prevLen = param.length();
                param.setLength(prevLen - 1);
                pm.valueB += param.toString() + " ";
            }
        }
        System.out.println(pm.valueA+" "+pm.valueB);
        ret.properties.add(pm);
        sb.setLength(sb.length()-1);
        sb.append("\"");
        sb.append(" },");
        System.out.println("Some hopes "+sb.toString());
        System.out.println("END FUSION");
        return sb.toString();
    }
    
    private String getGeom(String reg, DBConfig dbConf, JSONFusion ret) throws SQLException {
        
        try{
                Class.forName("org.postgresql.Driver");     
        } catch (ClassNotFoundException ex) {
                System.out.println(ex.getMessage());      
                
                return "";
            }
             
            Connection dbConn = null;
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
            PreparedStatement stmt = null;
            stmt = dbConn.prepareStatement(selectLinkedGeoms);
            ResultSet rs = stmt.executeQuery();
            StringBuilder sb = new StringBuilder();
            sb.append(" ],\"Geoms\":[{");
            while (rs.next()) {
                int parA = rs.getString(1).indexOf("(");
                int parB = rs.getString(2).indexOf("(");
                ret.geomsA.add(rs.getString(1));
                ret.geomsB.add(rs.getString(2));
                sb.append("\""+"left"+"\":"+"\""+rs.getString(1).substring(0, parA)+"\",");
                sb.append("\""+"right"+"\":"+"\""+rs.getString(2).substring(0, parB)+"\"");
            }
            sb.append("}]}");
            
            rs.close();
            stmt.close();
            
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
        Connection conn;
        String reg;
        String tgraph;
        List<FusionState> ft = null;
        PreparedStatement stmt = null;
        Connection dbConn = null;
        ResultSet rs = null;
        DBConfig dbConf = null;
        HttpSession sess;
        JSONFusion ret;      
    
        try {            
            ret = new JSONFusion();
            ObjectMapper mapper = new ObjectMapper();
            System.out.println(request.getParameterMap());
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            System.out.println(request.getParameterMap());
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            mapper.setDateFormat(outputFormat);
            System.out.println(request.getParameterMap());

            System.out.println(request.getParameterMap());

            mapper.setSerializationInclusion(Include.NON_EMPTY);
            
            System.out.println(request.getParameterMap());
            ft = new ArrayList<>();
            String[] selectedPreds = request.getParameterValues("props[]");
            sess = request.getSession(true);    
            GraphConfig grConf = (GraphConfig)sess.getAttribute("gr_conf");
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
                System.out.println(s);
            }
            reg = selectedPreds[0];
            
            String geom_json = getGeom(reg, dbConf, ret);
            System.out.println("Geom JSON "+mapper.writeValueAsString(ret));
            String getLink = "";
            if (grConf.isDominantA()) {
                getLink = "sparql select ?o where { GRAPH <http://localhost:8890/DAV/all_links_"+dbConf.getDBName()+"> {<"+reg+"> ?p ?o} }";
            } else {
                getLink = "sparql select ?o where { GRAPH <http://localhost:8890/DAV/all_links_"+dbConf.getDBName()+"> {?o ?p <"+reg+">} }";
            }
            
            stmt = null;
            stmt = conn.prepareStatement(getLink);
            rs = stmt.executeQuery();
            
            String linked = "";
            while (rs.next()) {
                linked = rs.getString(1);
            }
            sess.setAttribute("nodeA", reg);
            sess.setAttribute("nodeB", linked);
            
            rs.close();
            stmt.close();
            
            String getClassA = "sparql SELECT ?owlClass"
                    + "  WHERE {GRAPH <http://localhost:8890/DAV/all_links_"+dbConf.getDBName()+">"
                    + " { <"+reg+"> <http://www.w3.org/2002/07/owl#sameAs> ?o } . \n" +
                      " GRAPH <"+(String)sess.getAttribute("t_graph")+"_"+dbConf.getDBName()+"A> {<"+reg+"> <"+OWL_CLASS_PROPERTY+"> ?owlClass } }";
            
            String getClassB = "sparql SELECT ?owlClass"
                    + "  WHERE {GRAPH <http://localhost:8890/DAV/all_links_"+dbConf.getDBName()+">"
                    + " { <"+reg+"> <http://www.w3.org/2002/07/owl#sameAs> ?o } . \n" +
                      " GRAPH <"+(String)sess.getAttribute("t_graph")+"_"+dbConf.getDBName()+"B> {<"+reg+"> <"+OWL_CLASS_PROPERTY+"> ?owlClass } }";
            
            
            stmt = null;
            stmt = conn.prepareStatement(getClassA);
            rs = stmt.executeQuery();
            
            System.out.println("Class from A "+getClassA);
            System.out.println("Class from B "+getClassB);
            while (rs.next()) {
                ret.classesA.add(rs.getString(1));
            }
            
            stmt.close();
            rs.close();
            
            stmt = null;
            stmt = conn.prepareStatement(getClassB);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                ret.classesB.add(rs.getString(1));
            }
            rs.close();
            stmt.close();
            
            sess.setAttribute("nodeA", reg);
            sess.setAttribute("nodeB", linked);
            
            StringBuilder sb = new StringBuilder();
            sb.append("{\"fusions\":[");
                
            SchemaMatchState sms = (SchemaMatchState)sess.getAttribute("predicates_matches");
            //System.out.println(sms.foundA);
            //System.out.println(sms.foundB);
            if ( selectedPreds.length == 1 ) {
                JSONPropertyMatch pm = new JSONPropertyMatch();
                pm.property ="dummy";
                ret.properties.add(pm);
            }
            for ( int j = 1; j < selectedPreds.length; j+=2) {
                if (selectedPreds[j+1].equals("http://www.opengis.net/ont/geosparql#asWKT"))
                    continue;
                String newPropertyName = selectedPreds[j];
                String prevPropertyName = selectedPreds[j+1];
                System.out.println("Breaking on "+prevPropertyName);      
                String[] queryStrings = prevPropertyName.split("=>");
                String queryA = queryStrings[0];
                String queryB = queryStrings[1];
                
                List<String> presA = new ArrayList<String>();
                List<String> presB = new ArrayList<String>();
                ft.add(new FusionState());
                analyzeChain(presA, presB, queryA, queryB, sms);
                String json_obj = queryChains(sess, ret, ft, presA, presB, newPropertyName, prevPropertyName, reg, conn, dbConf);
                sb.append(json_obj);
            }
            System.out.println("Geom JSON "+mapper.writeValueAsString(ret));

            
            for (Map.Entry<String, AbstractFusionTransformation> entry : FuserPanel.transformations .entrySet())
            {
                //System.out.println("Transformation "+entry.getKey());
                ret.geomTransforms.add(entry.getKey());
            }
            
            for (Map.Entry<String, String> entry : FuserPanel.meta_transformations .entrySet())
            {
                //System.out.println("Transformation "+entry.getKey());
                ret.metaTransforms.add(entry.getKey());
            }
            
            sess.setAttribute("fstate", ft);
            
            int newLen = sb.length() - 1;
            sb.setLength(newLen);
            sb.append(geom_json);
            
            System.out.println(mapper.writeValueAsString(ret));   
            out.println(mapper.writeValueAsString(ret));
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
            /*
            (rs != null) {
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
            */
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
            /*
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
            */
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
