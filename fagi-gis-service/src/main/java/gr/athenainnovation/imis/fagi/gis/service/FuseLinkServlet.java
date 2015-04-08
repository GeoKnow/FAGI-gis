/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hp.hpl.jena.shared.JenaException;
import gr.athenainnovation.imis.fusion.gis.geotransformations.AbstractFusionTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.ShiftAToB;
import gr.athenainnovation.imis.fusion.gis.geotransformations.ShiftBToA;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepLeftTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepBothTransformation;
import gr.athenainnovation.imis.fusion.gis.geotransformations.KeepRightTransformation;
import gr.athenainnovation.imis.fusion.gis.gui.FuserPanel;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
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
    private List<FusionState> fs = null;
    private String tGraph = null;
    private String nodeA = null;
    private String nodeB = null;
    private String dom = null;
    private String domSub = null;
    private HttpSession sess = null;
    private JSONPropertyFusion[] selectedFusions;
    
    private class JSONFusions {
        List<JSONPropertyFusion> fusions;

        public JSONFusions(List<JSONPropertyFusion> fusions) {
            this.fusions = fusions;
        }

                
        public JSONFusions() {
            fusions = new ArrayList<>();
        }

        public List<JSONPropertyFusion> getFusions() {
            return fusions;
        }

        public void setFusions(List<JSONPropertyFusion> fusions) {
            this.fusions = fusions;
        }
        
    }
    
    private static class JSONShiftFactors {
        Float shift;
        Float scaleFact;
        Float rotateFact;

        public JSONShiftFactors() {
        }

        public Float getShift() {
            return shift;
        }

        public void setShift(Float shift) {
            this.shift = shift;
        }

        public Float getScaleFact() {
            return scaleFact;
        }

        public void setScaleFact(Float scaleFact) {
            this.scaleFact = scaleFact;
        }

        public Float getRotateFact() {
            return rotateFact;
        }

        public void setRotateFact(Float rotateFact) {
            this.rotateFact = rotateFact;
        }
        
    }
    
    private static class JSONPropertyFusion {
        String valA;
        String pre;
        String preL;
        String valB;
        String action;

        public JSONPropertyFusion() {
        }

        public String getValA() {
            return valA;
        }

        public void setValA(String valA) {
            this.valA = valA;
        }

        public String getPre() {
            return pre;
        }

        public void setPre(String pre) {
            this.pre = pre;
        }

        public String getPreL() {
            return preL;
        }

        public void setPreL(String preL) {
            this.preL = preL;
        }

        public String getValB() {
            return valB;
        }

        public void setValB(String valB) {
            this.valB = valB;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
        
    }
    
    private class JSONFusionResult {
        String geom;
        String na;
        String nb;

        public JSONFusionResult() {
        }

        public String getGeom() {
            return geom;
        }

        public void setGeom(String geom) {
            this.geom = geom;
        }

        public String getNa() {
            return na;
        }

        public void setNa(String na) {
            this.na = na;
        }

        public String getNb() {
            return nb;
        }

        public void setNb(String nb) {
            this.nb = nb;
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
            throws ServletException, IOException, SQLException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        sess = request.getSession(true);
        JSONFusionResult ret = new JSONFusionResult();
        //List<JSONPropertyFusion> selectedFusions = new ArrayList<>();
        //JSONFusions selectedFusions = new JSONFusions();
        try {
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            mapper.setDateFormat(outputFormat);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            fs = (List<FusionState>)sess.getAttribute("fstate");
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
            String propsJSON = request.getParameter("propsJSON");
            String shiftJSON = request.getParameter("factJSON");
            
            String domA = (String)sess.getAttribute("domA");
            String domB = (String)sess.getAttribute("domB");
            dom = domB;
            domSub = nodeB;
            if ( grConf.isDominantA() ) {
                domSub = nodeA;
                dom = domA;
            }
            System.out.println("Dom A "+domA+" Dom B "+domB);
            
            JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
            JsonParser jp = factory.createJsonParser(propsJSON);
            selectedFusions = mapper.readValue(jp, JSONPropertyFusion[].class );

            JsonParser jp2 = factory.createJsonParser(shiftJSON);
            JSONShiftFactors sFactors = mapper.readValue(jp2, JSONShiftFactors.class );
            
            if ( sFactors != null ) {
                System.out.println(sFactors.shift);
                System.out.println(sFactors.scaleFact);
                System.out.println(sFactors.rotateFact);
            }
            
            System.out.println(propsJSON);
            System.out.println("Shift JSON "+shiftJSON);
            for (JSONPropertyFusion pf : selectedFusions ) {
                System.out.println(pf.valA);
                System.out.println(pf.valB);
            }
            
            System.out.println("Fusing : "+nodeA+" "+nodeB);
            AbstractFusionTransformation trans = null;
            for(;;) {
                trans = FuserPanel.transformations.get(selectedFusions[0].action);
                if ( trans instanceof ShiftAToB) {
                    ((ShiftAToB)trans).setShift(sFactors.shift);
                    ((ShiftAToB)trans).setRotate(sFactors.rotateFact);
                    ((ShiftAToB)trans).setScale(sFactors.scaleFact);
                }
            
                if ( trans instanceof ShiftBToA) {
                    ((ShiftBToA)trans).setShift(sFactors.shift);
                    ((ShiftBToA)trans).setRotate(sFactors.rotateFact);
                    ((ShiftBToA)trans).setScale(sFactors.scaleFact);
                }
                
                if ( trans instanceof KeepRightTransformation) {
                    String q = formInsertGeomQuery(tGraph, domSub, selectedFusions[0].valB);
                    System.out.println("Query Right "+q);
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q, vSet);
                    vur.exec();
                    
                    ret.geom = selectedFusions[0].valB;
                    break;
                }
                
                if ( trans instanceof KeepLeftTransformation) {
                    String q = formInsertGeomQuery(tGraph, domSub, selectedFusions[0].valA);
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q, vSet);
                    vur.exec();
                    
                    ret.geom = selectedFusions[0].valA;
                    break;
                }
                
                if ( trans instanceof KeepBothTransformation) {
                    String qA = formInsertGeomQuery(tGraph, domSub, selectedFusions[0].valA);
                    String qB = formInsertGeomQuery(tGraph, domSub, selectedFusions[0].valB);
                    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(qA, vSet);
                    vur.exec();
                    vur = VirtuosoUpdateFactory.create(qB, vSet);
                    vur.exec();
                    
                    ret.geom = selectedFusions[0].valA;
                    ret.geom = selectedFusions[0].valB;
                    break;
                }
                
                System.out.println(trans == null);
                
                trans.fuse(dbConn, nodeA, nodeB);
                
                VirtuosoImporter virtImp = (VirtuosoImporter)sess.getAttribute("virt_imp");
                virtImp.setTransformationID(trans.getID());
            
                virtImp.importGeometriesToVirtuoso((String)sess.getAttribute("t_graph"));
            
                virtImp.trh.finish();
            
                String queryGeoms = "SELECT links.nodea as la, links.nodeb as lb, ST_asText(b.geom) as g\n" +
                                 "FROM links INNER JOIN fused_geometries AS b\n" +
                                 "ON (b.subject_a = ?)";
            
                stmt = dbConn.prepareStatement(queryGeoms);
                stmt.setString(1, nodeA);
                rs = stmt.executeQuery();
            
                if(rs.next()) {
                    ret.geom = rs.getString(3);
                    System.out.println("Returning geom : "+ret.geom);
                }
                System.out.println(queryGeoms);
            
                break;
            }
            
            
            for(int i = 1; i < selectedFusions.length; i++) {
                handleMetadataFusion(selectedFusions[i].action, i);
            }
            
            for(int i = 4; i < props.length; i+=4) {
                System.out.println("Pred : "+props[i]);
                System.out.println("Pred : "+props[i+1]);
                System.out.println("Pred : "+props[i+2]);
                System.out.println("Pred : "+props[i+3]);
                //System.out.println("Pred : "+props[i+4]);
                //handleMetadataFusion(props[i+3], i-4);
                /*System.out.println("List : "+ fs.get((i%4)-1).actions);
                System.out.println("List : "+ fs.get((i%4)-1).objsA);
                System.out.println("List : "+ fs.get((i%4)-1).objsB);
                System.out.println("List : "+ fs.get((i%4)-1).preds);
                System.out.println("List : "+ fs.get((i%4)-1).predsA);
                System.out.println("List : "+ fs.get((i%4)-1).predsB); */     
            }

            System.out.println("JSON Geometry "+mapper.writeValueAsString(ret));
            /* TODO output your page here. You may use following sample code. */
            out.println(mapper.writeValueAsString(ret));
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
    
    private void handleMetadataFusion(String action, int idx) throws SQLException, UnsupportedEncodingException {
        if (action.equals("Keep Left")) {
            metadataKeepLeft(idx);
        }
        if (action.equals("Keep Right")) {
            metadataKeepRight(idx);
        }
        if (action.equals("Keep Both")) {
            //metadataKeepBoth(idx);
            metadataKeepRight(idx);
            metadataKeepLeft(idx);
        }
        if (action.equals("Keep Concatenated Right")) {
            metadataKeepConcatRight(idx);
        }
        if (action.equals("Keep Concatenated Left")) {
            metadataKeepConcatLeft(idx);
        }
        if (action.equals("Keep Concatenated Both")) {
            metadataKeepConcatLeft(idx);
            metadataKeepConcatRight(idx);
        }
        if (action.equals("Concatenation")) {
            metadataConcatenation(idx);
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
    
    private void metadataConcatenation(int idx) throws SQLException, UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        StringBuilder concat_str = new StringBuilder();
        String domOnto = "";
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        String[] predicates = longName.split("=>");
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        String[] leftPreTokens = leftPre.split(",");
        String[] rightPreTokens = rightPre.split(",");
        
        //for (String s : fst.predsA) {
            String prev_s = "<"+nodeA+">";
            
            StringBuilder q = new StringBuilder();
            q.append("sparql SELECT * ");
            prev_s = "<"+nodeA+">";
            q.append(" WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"A> {");
            for (int i = 0; i < leftPreTokens.length; i++) {
                q.append(prev_s+" <"+leftPreTokens[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            System.out.println(q.toString());
            
            
            PreparedStatement stmt;
            stmt = virt_conn.prepareStatement(q.toString());
            ResultSet rs = stmt.executeQuery();
            
            
            while(rs.next()) {
                //for (int i = 0; i < pres.length; i++) {
                    String o = rs.getString(leftPreTokens.length);
                    String simplified = StringUtils.substringAfter(leftPreTokens[leftPreTokens.length-1], "#");
                    if (simplified.equals("") ) {
                        simplified = StringUtils.substring(leftPreTokens[leftPreTokens.length-1], StringUtils.lastIndexOf(leftPreTokens[leftPreTokens.length-1], "/")+1);
                    }
                    //concat_str.append(simplified+":"+o+" ");
                    concat_str.append(o+" ");
                //}
            }
            
            //System.out.println(q.toString());
        //}
        
        //for (String s : fst.predsB) {
            prev_s = "<"+nodeA+">";
            
            q = new StringBuilder();
            q.append("sparql SELECT * ");
            prev_s = "<"+nodeA+">";
            q.append(" WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
            for (int i = 0; i < rightPreTokens.length; i++) {
                q.append(prev_s+" <"+rightPreTokens[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            System.out.println(q.toString());
            
            rs.close();
            stmt.close();
            
            stmt = virt_conn.prepareStatement(q.toString());
            rs = stmt.executeQuery();
            
            while(rs.next()) {
                //for (int i = 0; i < pres.length; i++) {
                    String o = rs.getString(rightPreTokens.length);
                    String simplified = StringUtils.substringAfter(rightPreTokens[rightPreTokens.length-1], "#");
                    if (simplified.equals("") ) {
                        simplified = StringUtils.substring(rightPreTokens[rightPreTokens.length-1], StringUtils.lastIndexOf(rightPreTokens[rightPreTokens.length-1], "/")+1);
                    }
                    concat_str.append(o+" ");
                //}
            }
            
            System.out.println("Concat query B "+q.toString());
        //}
            
        concat_str.setLength(concat_str.length()-1);
        
        q = new StringBuilder();
        q.append("INSERT { GRAPH <"+tGraph+"> { ");
        prev_s = "<"+nodeA+">";
        q.append(prev_s+" <"+domOnto+newPred+"> \""+concat_str+"\"");
        /*q.append("} } WHERE {\n GRAPH <"+tGraph+"metadataA> {");
        prev_s = "<"+nodeA+">";
        for (int i = 0; i < rightPreTokens.length; i++) {
            q.append(prev_s+" <"+rightPreTokens[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }*/
        q.append("} }");
        System.out.println("Insert query "+q.toString());
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
        vur.exec();
        
        /*
        System.out.println("Object "+concat_str.toString());
        System.out.println("Pred "+fst.preds.get(0));
        String newPred = fst.preds.get(0);
        String lastPred = fst.predsA.get(fst.predsA.size() - 1);
        String vals[] = newPred.split("=>");
        System.out.println("Last Pred "+lastPred);
        String concatePred = "";
        if ( vals.length == 1 ) {
            concatePred = dom + vals[0];
        } else {
            concatePred = dom + vals[0] + "_" + vals[1];
        }
        
        concat_str.setLength(concat_str.length()-1);
        int lastIdx = StringUtils.lastIndexOf(fs.get((idx)).predsA.get(0), ",");
        String pred = fs.get((idx)).predsA.get(0).substring(0, lastIdx);
        String[] pres = StringUtils.split(pred, ",");*/
        /*StringBuilder q = new StringBuilder();
        q.append("INSERT { GRAPH <"+tGraph+"> { ");
        String prev_s = "<"+nodeA+">";
        for (int i = 0; i < pres.length-1; i++) {
            q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }*/
        /*q.append(prev_s+" <"+concatePred+"> \""+concat_str+"\"");
        /*q.append("} } WHERE {\n GRAPH <"+tGraph+"metadataA> {");
        prev_s = "<"+nodeA+">";
        for (int i = 0; i < pres.length; i++) {
            q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }*/
        /*q.append("} }");
        System.out.println(q.toString());
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
        vur.exec();
        */
        //System.out.println(q.toString());
    }

    private void metadataKeepFlatLeft(int idx) throws SQLException, UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        StringBuilder concat_str = new StringBuilder();
        String domOnto = "";
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        String[] predicates = longName.split("=>");
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        String[] leftPreTokens = leftPre.split(",");
        String[] rightPreTokens = rightPre.split(",");
        
        if ( leftPreTokens.length == 1) {
            metadataKeepLeft(idx);
            return;
        }
        
        StringBuilder q = new StringBuilder();
        q.append("sparql SELECT ?o"+(leftPreTokens.length-1));
        String prev_s = "<"+nodeA+">";
        q.append(" WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
        for (int i = 0; i < leftPreTokens.length; i++) {
            q.append(prev_s+" <"+leftPreTokens[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append("} }");
        System.out.println(q.toString());
        PreparedStatement stmt;
        stmt = virt_conn.prepareStatement(q.toString());
        ResultSet rs = stmt.executeQuery();
        
        while(rs.next()) {
            StringBuilder insq = new StringBuilder();
            insq.append("INSERT { GRAPH <"+tGraph+"> { ");
                      
            prev_s = "<"+nodeA+">";
            for (int i = 0; i < leftPreTokens.length-2; i++) {
                insq.append(prev_s+" <"+leftPreTokens[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            String o = rs.getString(1);
            String simplified = StringUtils.substringAfter(leftPreTokens[leftPreTokens.length-1], "#");
            if (simplified.equals("") ) {
                simplified = StringUtils.substring(leftPreTokens[leftPreTokens.length-1], StringUtils.lastIndexOf(leftPreTokens[leftPreTokens.length-1], "/")+1);
            }
            String simplifiedPred = StringUtils.substringAfter(leftPreTokens[leftPreTokens.length-2], "#");
            if (simplifiedPred.equals("") ) {
                simplifiedPred = StringUtils.substring(leftPreTokens[leftPreTokens.length-2], StringUtils.lastIndexOf(leftPreTokens[leftPreTokens.length-1], "/")+1);
            }
                insq.append(prev_s+" <"+domOnto+"_"+simplifiedPred+"_"+simplified+"> \""+o+"\"");
                insq.append("} } WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
                prev_s = "<"+nodeA+">";
                for (int i = 0; i < leftPreTokens.length; i++) {
                    insq.append(prev_s+" <"+leftPreTokens[i]+"> ?o"+i+" . ");
                    prev_s = "?o"+i;
                }
                
                insq.append("} }");
                System.out.println("Insert Query "+insq.toString());
                
                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(insq.toString(), vSet);
                vur.exec();
        }
    }
    
    private void metadataKeepFlatRight(int idx) throws SQLException, UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        StringBuilder concat_str = new StringBuilder();
        String domOnto = "";
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        String[] predicates = longName.split("=>");
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        String[] leftPreTokens = leftPre.split(",");
        String[] rightPreTokens = rightPre.split(",");
        
        if ( rightPreTokens.length == 1) {
            metadataKeepRight(idx);
            return;
        }
        
        StringBuilder q = new StringBuilder();
        q.append("sparql SELECT ?o"+(rightPreTokens.length-1));
        String prev_s = "<"+nodeA+">";
        q.append(" WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
        for (int i = 0; i < rightPreTokens.length; i++) {
            q.append(prev_s+" <"+rightPreTokens[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append("} }");
        System.out.println(q.toString());
        PreparedStatement stmt;
        stmt = virt_conn.prepareStatement(q.toString());
        ResultSet rs = stmt.executeQuery();
        
        while(rs.next()) {
            StringBuilder insq = new StringBuilder();
            insq.append("INSERT { GRAPH <"+tGraph+"> { ");
                      
            prev_s = "<"+nodeA+">";
            for (int i = 0; i < rightPreTokens.length-2; i++) {
                insq.append(prev_s+" <"+rightPreTokens[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            String o = rs.getString(1);
            String simplified = StringUtils.substringAfter(rightPreTokens[rightPreTokens.length-1], "#");
            if (simplified.equals("") ) {
                simplified = StringUtils.substring(rightPreTokens[rightPreTokens.length-1], StringUtils.lastIndexOf(rightPreTokens[rightPreTokens.length-1], "/")+1);
            }
            String simplifiedPred = StringUtils.substringAfter(rightPreTokens[rightPreTokens.length-2], "#");
            if (simplifiedPred.equals("") ) {
                simplifiedPred = StringUtils.substring(rightPreTokens[rightPreTokens.length-2], StringUtils.lastIndexOf(rightPreTokens[rightPreTokens.length-1], "/")+1);
            }
                insq.append(prev_s+" <"+domOnto+"_"+simplifiedPred+"_"+simplified+"> \""+o+"\"");
                insq.append("} } WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
                prev_s = "<"+nodeA+">";
                for (int i = 0; i < rightPreTokens.length; i++) {
                    insq.append(prev_s+" <"+rightPreTokens[i]+"> ?o"+i+" . ");
                    prev_s = "?o"+i;
                }
                
                insq.append("} }");
                System.out.println("Insert Query "+insq.toString());
                
                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(insq.toString(), vSet);
                vur.exec();
        }
    }
    
    private void metadataKeepConcatLeft(int idx) throws SQLException, UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        StringBuilder concat_str = new StringBuilder();
        String domOnto = "";
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        String[] predicates = longName.split("=>");
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        String[] leftPreTokens = leftPre.split(",");
        String[] rightPreTokens = rightPre.split(",");
        
        if ( leftPreTokens.length == 1) {
            metadataKeepLeft(idx);
            return;
        }
        
        StringBuilder q = new StringBuilder();
        q.append("sparql SELECT ?o"+(leftPreTokens.length-1));
        String prev_s = "<"+nodeA+">";
        q.append(" WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
        for (int i = 0; i < leftPreTokens.length; i++) {
            q.append(prev_s+" <"+leftPreTokens[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append("} }");
        System.out.println(q.toString());
        PreparedStatement stmt;
        stmt = virt_conn.prepareStatement(q.toString());
        ResultSet rs = stmt.executeQuery();
        
        while(rs.next()) {
            //for (int i = 0; i < pres.length; i++) {
            String o = rs.getString(1);
            String simplified = StringUtils.substringAfter(leftPreTokens[leftPreTokens.length-1], "#");
            if (simplified.equals("") ) {
                simplified = StringUtils.substring(leftPreTokens[leftPreTokens.length-1], StringUtils.lastIndexOf(leftPreTokens[leftPreTokens.length-1], "/")+1);
            }
            concat_str.append(simplified+":"+o+" ");
        }
        
        concat_str.setLength(concat_str.length()-1);
        q = new StringBuilder();
        q.append("INSERT { GRAPH <"+tGraph+"> { ");
        prev_s = "<"+nodeA+">";
        for (int i = 0; i < leftPreTokens.length-1; i++) {
            q.append(prev_s+" <"+leftPreTokens[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append(prev_s+" <"+domOnto+newPred+"> \""+concat_str+"\"");
        q.append("} } WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
        prev_s = "<"+nodeA+">";
        for (int i = 0; i < leftPreTokens.length; i++) {
            q.append(prev_s+" <"+leftPreTokens[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append("} }");
        System.out.println("Last query "+q.toString());
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
        vur.exec();
    }
    
    private void metadataKeepConcatBoth(int idx) {
        
    }
    
    private void metadataKeepConcatRight(int idx) throws SQLException, UnsupportedEncodingException {
        Connection virt_conn = vSet.getConnection();
        StringBuilder concat_str = new StringBuilder();
        String domOnto = "";
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        String[] predicates = longName.split("=>");
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        String[] leftPreTokens = leftPre.split(",");
        String[] rightPreTokens = rightPre.split(",");
        
        if ( rightPreTokens.length == 1) {
            metadataKeepRight(idx);
            return;
        }
        
        StringBuilder q = new StringBuilder();
        q.append("sparql SELECT ?o"+(rightPreTokens.length-1));
        String prev_s = "<"+nodeA+">";
        q.append(" WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
        for (int i = 0; i < rightPreTokens.length; i++) {
            q.append(prev_s+" <"+rightPreTokens[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append("} }");
        System.out.println(q.toString());
        PreparedStatement stmt;
        stmt = virt_conn.prepareStatement(q.toString());
        ResultSet rs = stmt.executeQuery();
        
        while(rs.next()) {
            //for (int i = 0; i < pres.length; i++) {
            String o = rs.getString(1);
            String simplified = StringUtils.substringAfter(rightPreTokens[rightPreTokens.length-1], "#");
            if (simplified.equals("") ) {
                simplified = StringUtils.substring(rightPreTokens[rightPreTokens.length-1], StringUtils.lastIndexOf(rightPreTokens[rightPreTokens.length-1], "/")+1);
            }
            concat_str.append(simplified+":"+o+" ");
        }
        
        concat_str.setLength(concat_str.length()-1);
        q = new StringBuilder();
        q.append("INSERT { GRAPH <"+tGraph+"> { ");
        prev_s = "<"+nodeA+">";
        for (int i = 0; i < rightPreTokens.length-1; i++) {
            q.append(prev_s+" <"+rightPreTokens[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append(prev_s+" <"+domOnto+newPred+"> \""+concat_str+"\"");
        q.append("} } WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
        prev_s = "<"+nodeA+">";
        for (int i = 0; i < rightPreTokens.length; i++) {
            q.append(prev_s+" <"+rightPreTokens[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append("} }");
        System.out.println("Last query "+q.toString());
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
        vur.exec();
        
        /*
        for (String s : fs.get((idx)).predsB) {
            String[] pres = StringUtils.split(s, ",");
            String prev_s = "<"+nodeA+">";
            if (pres.length == 1) {
                StringBuilder sq = new StringBuilder();
                sq.append("INSERT { GRAPH <"+tGraph+"> { ");
                sq.append(prev_s+" <"+pres[0]+"> ?o"+0+" . } ");
                sq.append(" } WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> { "+prev_s+" <"+pres[0]+"> ?o"+0+" . } }");
                
                //System.out.println(sq.toString());
                
                VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(sq.toString(), vSet);
                vur.exec();
                
                return;
            }
            StringBuilder q = new StringBuilder();
            q.append("sparql SELECT * ");
            prev_s = "<"+nodeA+">";
            q.append(" WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            System.out.println(q.toString());
            
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
                    concat_str.append(simplified+":"+o+" ");
                //}
            }
            
            //System.out.println(q.toString());
        }
        concat_str.setLength(concat_str.length()-1);
        int lastIdx = StringUtils.lastIndexOf(fs.get((idx)).predsB.get(0), ",");
        String pred = fs.get((idx)).predsB.get(0).substring(0, lastIdx);
        String[] pres = StringUtils.split(pred, ",");
        StringBuilder q = new StringBuilder();
        q.append("INSERT { GRAPH <"+tGraph+"> { ");
        String prev_s = "<"+nodeA+">";
        for (int i = 0; i < pres.length-1; i++) {
            q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append(prev_s+" ?o"+(pres.length-1)+" \""+concat_str+"\"");
        q.append("} } WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
        prev_s = "<"+nodeA+">";
        for (int i = 0; i < pres.length; i++) {
            q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
            prev_s = "?o"+i;
        }
        q.append("} }");
        //System.out.println(q.toString());
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
        vur.exec();
        
        //System.out.println(concat_str); */
    }
        
    private void metadataKeepLeft(int idx) throws UnsupportedEncodingException {
        String domOnto = "";
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        String[] predicates = longName.split("=>");
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        String[] leftPreTokens = leftPre.split(",");
        String[] rightPreTokens = rightPre.split(",");
        System.out.println("In the far depths "+leftPreTokens.length + " " + rightPreTokens.length);
        System.out.println(fs.get((idx-1)).predsB.size());
        //for (String s : fs.get((idx-1)).predsB) {
        //    System.out.println(s);
        //    String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("INSERT { GRAPH <"+tGraph+"> { ");
            String prev_s = "<"+nodeA+">";
            for (int i = 0; i < leftPreTokens.length-1; i++) {
                q.append(prev_s+" <"+leftPreTokens[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append(prev_s+" <"+domOnto+newPred+"> ?o"+(leftPreTokens.length-1)+" . ");
            prev_s = "<"+nodeA+">";
            q.append("} } WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"A> {");
            for (int i = 0; i < leftPreTokens.length; i++) {
                q.append(prev_s+" <"+leftPreTokens[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            System.out.println(q.toString());
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        //}
    }

    private void metadataKeepBoth(int idx) {
        for (String s : fs.get((idx)).predsA) {
            String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("INSERT { GRAPH <"+tGraph+"> { ");
            String prev_s = "<"+nodeA+">";
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            prev_s = "<"+nodeA+">";
            q.append("} } WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"A> {");
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            System.out.println(q.toString());
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        }
        
        for (String s : fs.get((idx)).predsB) {
            String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("INSERT { GRAPH <"+tGraph+"> { ");
            String prev_s = "<"+nodeA+">";
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            prev_s = "<"+nodeA+">";
            q.append("} } WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
            for (int i = 0; i < pres.length; i++) {
                q.append(prev_s+" <"+pres[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            System.out.println(q.toString());
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        }
    }
    
    private void metadataKeepRight(int idx) throws UnsupportedEncodingException {
        System.out.println("In the far depths "+sess.getAttribute("domA"));
        System.out.println("In the far depths "+sess.getAttribute("domB"));
        String domOnto = "";
        if ( grConf.isDominantA() ) 
            domOnto = (String)sess.getAttribute("domA");
        else 
            domOnto = (String)sess.getAttribute("domB");
        String name = URLDecoder.decode(selectedFusions[idx].pre, "UTF-8");
        String longName = URLDecoder.decode(selectedFusions[idx].preL, "UTF-8");
        name = StringUtils.replace(name, "&gt;", ">");
        longName = StringUtils.replace(longName, "&gt;", ">");
        String[] newPredTokes = name.split("=>");
        String newPred = "";
        if ( newPredTokes.length == 2 ) {
            newPred = newPredTokes[1];
        } else {
            newPred = newPredTokes[0];
        }
        String[] predicates = longName.split("=>");
        String leftPre = predicates[0];
        String rightPre = predicates[1];
        String[] leftPreTokens = leftPre.split(",");
        String[] rightPreTokens = rightPre.split(",");
        System.out.println("In the far depths "+leftPreTokens.length + " " + rightPreTokens.length);
        System.out.println(fs.get((idx-1)).predsB.size());
        //for (String s : fs.get((idx-1)).predsB) {
        //    System.out.println(s);
        //    String[] pres = StringUtils.split(s, ",");
            StringBuilder q = new StringBuilder();
            q.append("INSERT { GRAPH <"+tGraph+"> { ");
            String prev_s = "<"+nodeA+">";
            for (int i = 0; i < rightPreTokens.length-1; i++) {
                q.append(prev_s+" <"+rightPreTokens[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append(prev_s+" <"+domOnto+newPred+"> ?o"+(rightPreTokens.length-1)+" . ");
            prev_s = "<"+nodeA+">";
            q.append("} } WHERE {\n GRAPH <"+tGraph+"_"+dbConf.getDBName()+"B> {");
            for (int i = 0; i < rightPreTokens.length; i++) {
                q.append(prev_s+" <"+rightPreTokens[i]+"> ?o"+i+" . ");
                prev_s = "?o"+i;
            }
            q.append("} }");
            System.out.println(q.toString());
            VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(q.toString(), vSet);
            vur.exec();
        //}
    }
    private String formInsertQuery(String tGraph, String subject, String fusedGeometry) { 
        return "INSERT INTO <" + tGraph + "> { <" + subject + "> <" + HAS_GEOMETRY + "> _:a . _:a <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> }";
    }
    
    private String formInsertGeomQuery(String tGraph, String subject, String fusedGeometry) { 
        return "INSERT INTO <" + tGraph + "> { <" + subject + "> <" + HAS_GEOMETRY + "> <" + subject + "_geom> . <" + subject +"_geom> <" + WKT + "> \"" + fusedGeometry + "\"^^<http://www.openlinksw.com/schemas/virtrdf#Geometry> }";
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
