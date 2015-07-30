package gr.athenainnovation.imis.fagi.gis.service;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author nick
 */
@WebServlet(urlPatterns = {"/BatchFindLinkServlet"})
public class BatchFindLinkServlet extends HttpServlet {
    private static final String SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";
    private static final String WKT = "http://www.opengis.net/ont/geosparql#asWKT";
    private static final String HAS_GEOMETRY = "http://www.opengis.net/ont/geosparql#hasGeometry";
    
    private GraphConfig grConf;
    private VirtGraph vSet = null;
    private Connection dbConn = null;
    private HttpSession sess = null;
    private JSONBArea BBox;
    private JSONGeomLinkList ret = null;
    
    private WKTReader wkt = new WKTReader();
    
    private class IntWrapper {
        public int i;
        
        IntWrapper(int i) {
            this.i = i;
        }
        
        void inc() {
            i++;
        }
        
        void dec() {
            i--;
        }
    }
    
    private class SubObjPair {
        String sub;
        String obj;

        public SubObjPair(String sub, String obj) {
            this.sub = sub;
            this.obj = obj;
        }

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }

        public String getObj() {
            return obj;
        }

        public void setObj(String obj) {
            this.obj = obj;
        }
        
    }
    
    private class JSONGeomLink {
        String subA, geomA;
        String subB, geomB;
        double dist, jIndex;
        
        public JSONGeomLink() {
        }

        public JSONGeomLink(String subA, String geomA, String subB, String geomB) {
            this.subA = subA;
            this.geomA = geomA;
            this.subB = subB;
            this.geomB = geomB;
        }

        public JSONGeomLink(String subA, String geomA, String subB, String geomB, double dist, double jIndex) {
            this.subA = subA;
            this.geomA = geomA;
            this.subB = subB;
            this.geomB = geomB;
            this.dist = dist;
            this.jIndex = jIndex;
        }

        public double getDist() {
            return dist;
        }

        public void setDist(double dist) {
            this.dist = dist;
        }

        public double getjIndex() {
            return jIndex;
        }

        public void setjIndex(double jIndex) {
            this.jIndex = jIndex;
        }

        public String getSubA() {
            return subA;
        }

        public void setSubA(String subA) {
            this.subA = subA;
        }

        public String getGeomA() {
            return geomA;
        }

        public void setGeomA(String geomA) {
            this.geomA = geomA;
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
    
    //Text
    private final String strPatternText = "[a-zA-Z]+(\\b[a-zA-Z]+\\b)*([a-zA-Z])";
    final Pattern patternText = Pattern.compile( strPatternText );
    final Pattern patternInt = Pattern.compile( "^(\\d+)$" );
    
    private static class JSONBArea {
        String barea;
        double left, right, bottom, top;
        
        public String getBarea() {
            return barea;
        }

        public void setBarea(String barea) {
            this.barea = barea;
        }

        public double getLeft() {
            return left;
        }

        public void setLeft(double left) {
            this.left = left;
        }

        public double getRight() {
            return right;
        }

        public void setRight(double right) {
            this.right = right;
        }

        public double getBottom() {
            return bottom;
        }

        public void setBottom(double bottom) {
            this.bottom = bottom;
        }

        public double getTop() {
            return top;
        }

        public void setTop(double top) {
            this.top = top;
        }
        
    }
    
    private class JSONGeomLinkList {
        List<JSONGeomLink> links;
        HashMap<String, String> entitiesA;
        HashMap<String, String> entitiesB;
        String dataset;
        
        public JSONGeomLinkList() {
            links = new ArrayList<>();
            entitiesA = new HashMap<>();
            entitiesB = new HashMap<>();
            dataset = "";
        }

        public String getDataset() {
            return dataset;
        }

        public void setDataset(String dataset) {
            this.dataset = dataset;
        }

        public List<JSONGeomLink> getLinks() {
            return links;
        }

        public void setLinks(List<JSONGeomLink> links) {
            this.links = links;
        }

        public HashMap<String, String> getEntitiesA() {
            return entitiesA;
        }

        public void setEntitiesA(HashMap<String, String> entitiesA) {
            this.entitiesA = entitiesA;
        }

        public HashMap<String, String> getEntitiesB() {
            return entitiesB;
        }

        public void setEntitiesB(HashMap<String, String> entitiesB) {
            this.entitiesB = entitiesB;
        }
        
        void AddLink(JSONGeomLink l) {
            links.add(l);
        }
        
    }
    
    private class JSONUnlinkedEntity {
        String geom;
        String sub;

        public JSONUnlinkedEntity(String geom, String subs) {
            this.geom = geom;
            this.sub = subs;
        }

        public String getGeom() {
            return geom;
        }

        public void setGeom(String geom) {
            this.geom = geom;
        }

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
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
            throws ServletException, IOException, ParseException {
        response.setContentType("text/html;charset=UTF-8");
        
        sess = request.getSession(true);
        grConf = (GraphConfig)sess.getAttribute("gr_conf");
        
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            
            ObjectMapper mapper = new ObjectMapper();
            String bboxJSON = request.getParameter("bboxJSON");
            
            ret = new JSONGeomLinkList();
            
            System.out.println("Properties JSON "+bboxJSON);
            
            JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
            JsonParser jsParser = factory.createJsonParser(bboxJSON);
            BBox = mapper.readValue(jsParser, JSONBArea.class );
            
            StringBuilder geoQuery = new StringBuilder();
            final float X_MAX = 180f;
            final float Y_MAX = 85.05f;
            System.out.println(BBox.getRight());
            System.out.println(BBox.getLeft());
            System.out.println(BBox.getTop());
            System.out.println(BBox.getBottom());
            
            grConf.scanGeoProperties();
            
            // Allready previed geoms
            HashSet<String> fetchedGeomsA = (HashSet<String>) sess.getAttribute("fetchedGeomsA");
            HashSet<String> fetchedGeomsB = (HashSet<String>) sess.getAttribute("fetchedGeomsB");

            if (fetchedGeomsA == null) {
                fetchedGeomsA = new HashSet<>();
                sess.setAttribute("fetchedGeomsA", fetchedGeomsA);
            }

            if (fetchedGeomsB == null) {
                fetchedGeomsB = new HashSet<>();
                sess.setAttribute("fetchedGeomsB", fetchedGeomsB);
            }
            
            List<String> geoPropsA = grConf.getGeoPropertiesA();
            List<String> geoPropsB = grConf.getGeoPropertiesB();
            List<String> geoTypesA = grConf.getGeoTypesA();
            List<String> geoTypesB = grConf.getGeoTypesB();
            
            // Fetch neighboring entities Predicate.Object pairs
            HashMap<String, Geometry> geomsA = new HashMap<>();
            HashMap<String, Geometry> geomsB = new HashMap<>();
            int newGeom = 0;
            
            int countA = 0;
            int countB = 0;
            
            String p = geoPropsA.get(0);
            for (String t : geoTypesA) {
                if (t.equalsIgnoreCase("POLYGON")) {
                    geoQuery.append("SELECT ?s ?p ?o ?geo\n WHERE {\n"
                            + "?s <" + p + "> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n");
                    if (BBox.getLeft() < 0) {
                        geoQuery.append("FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " )  < " + (BBox.getRight() + X_MAX) + ")\n"
                                + "FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " ) > " + (BBox.getLeft() + X_MAX) + ")\n");
                    } else {
                        geoQuery.append("FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " )  < " + (BBox.getRight() + X_MAX) + ")\n"
                                + "FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " ) > " + (BBox.getLeft() + X_MAX) + ")\n");
                    }
                    if (BBox.getBottom() < 0) {
                        geoQuery.append("FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) > " + (BBox.getTop() + Y_MAX) + ")\n"
                                + "FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) < " + (BBox.getBottom() + Y_MAX) + ")\n");
                    } else {
                        geoQuery.append("FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) < " + (BBox.getTop() + Y_MAX) + ")\n"
                                + "FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) > " + (BBox.getBottom() + Y_MAX) + ")\n");
                    }
                    geoQuery.append("} ");
                } else {
                    geoQuery.append("SELECT ?s ?p ?o ?geo \n"
                            + "WHERE {\n"
                            + "?s <" + p + "> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n"
                            + "FILTER (bif:st_intersects (?geo, bif:st_geomfromtext(\"" + BBox.getBarea() + "\"), 0))\n"
                            + "} ");
                }
            }
            
            System.out.println("Count A query " + geoQuery);
            
            String service = grConf.getEndpointA();
            String graph = grConf.getGraphA();
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
            QueryEngineHTTP qeh = new QueryEngineHTTP(service, geoQuery.toString(), authenticator);
            qeh.addDefaultGraph(graph);
            QueryExecution queryExecution = qeh;
            final com.hp.hpl.jena.query.ResultSet resultSetGeomsA = queryExecution.execSelect();
            
            System.out.println("Fetched already: " + fetchedGeomsA.size());

            while (resultSetGeomsA.hasNext()) {
                final QuerySolution querySolution = resultSetGeomsA.next();

                final String geo = querySolution.getLiteral("?geo").getString();
                final String sub = querySolution.getResource("?s").getURI();
                
                //System.out.println(geo);
                Point centroid = wkt.read(geo).getCentroid();
                if ( !centroid.isValid() ) {
                    //System.out.println(geo);
                    continue;
                }
                String wktrep = wkt.read(geo).getCentroid().toText();
                //System.out.println(wktrep);
                //System.out.println(centroid.getX() + "  " +  centroid.getY());
                
                geomsA.put(sub, wkt.read(geo) );
            }
            queryExecution.close();
            
            System.out.println("Count from A : " + countA);
            
            geoQuery.setLength(0);
            p = geoPropsB.get(0);
            for (String t : geoTypesB) {
                if (t.equalsIgnoreCase("POLYGONs")) {
                    geoQuery.append("SELECT ?s ?p ?o ?geo\n WHERE {\n"
                            + "?s <" + p + "> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n");
                    if (BBox.getLeft() < 0) {
                        geoQuery.append("FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " )  < " + (BBox.getRight() + X_MAX) + ")\n"
                                + "FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " ) > " + (BBox.getLeft() + X_MAX) + ")\n");
                    } else {
                        geoQuery.append("FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " )  < " + (BBox.getRight() + X_MAX) + ")\n"
                                + "FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " ) > " + (BBox.getLeft() + X_MAX) + ")\n");
                    }
                    if (BBox.getBottom() < 0) {
                        geoQuery.append("FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) > " + (BBox.getTop() + Y_MAX) + ")\n"
                                + "FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) < " + (BBox.getBottom() + Y_MAX) + ")\n");
                    } else {
                        geoQuery.append("FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) < " + (BBox.getTop() + Y_MAX) + ")\n"
                                + "FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) > " + (BBox.getBottom() + Y_MAX) + ")\n");
                    }
                    geoQuery.append("} ");
                } else {
                    geoQuery.append("SELECT ?s ?p ?o ?geo \n"
                            + "WHERE {\n"
                            + "?s <" + p + "> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n"
                            + "FILTER (bif:st_intersects (?geo, bif:st_geomfromtext(\"" + BBox.getBarea() + "\"), 0))\n"
                            + "} ");
                }
            }
            
            countA = geomsA.size();
            System.out.println("Count A query " + geoQuery);
            
            service = grConf.getEndpointB();
            graph = grConf.getGraphB();
            authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
            qeh = new QueryEngineHTTP(service, geoQuery.toString(), authenticator);
            qeh.addDefaultGraph(graph);
            queryExecution = qeh;
            final com.hp.hpl.jena.query.ResultSet resultSetGeomsB = queryExecution.execSelect();
            
            System.out.println("Fetched already: " + fetchedGeomsA.size());

            while (resultSetGeomsB.hasNext()) {
                final QuerySolution querySolution = resultSetGeomsB.next();

                final String geo = querySolution.getLiteral("?geo").getString();
                final String sub = querySolution.getResource("?s").getURI();
                
                //System.out.println(geo);
                Point centroid = wkt.read(geo).getCentroid();
                if ( !centroid.isValid() ) {
                    //System.out.println(geo);
                    continue;
                }
                String wktrep = wkt.read(geo).getCentroid().toText();
                //System.out.println(wktrep);
                //System.out.println(centroid.getX() + "  " +  centroid.getY());
                
                geomsB.put(sub, wkt.read(geo));
            }
            queryExecution.close();
            
            countB = geomsB.size();
            System.out.println("Count from B : " + countB);
            
            if ( countB < countA && countB > 0) {
                System.out.println("Fetching from B");
            } else {
                System.out.println("Fetching from A");
            }
            
            HashMap<String, Geometry> geomsRef = geomsA;
            if (countB < countA && countB > 0) {
                geomsRef = geomsB;
            }
            
            System.out.println("Feometries fro least filled BBox " + geomsRef.size());
            List<JSONGeomLink> newLinks = new ArrayList<>();
            HashMap<String, String> geoms = new HashMap<>();
            HashMap<String, Double> maxDistIndex = new HashMap<>();
            for (Map.Entry<String, Geometry> entryA : geomsRef.entrySet()) {
                String subA = entryA.getKey();
                double maxDist = -1;
                Geometry geoA = entryA.getValue();
                
                geoQuery.setLength(0);
                geoQuery.append("SELECT ?s ?p ?o ?geo\nWHERE { ?s ?p ?o {\n");
                geoQuery.append("SELECT ?s ?geo\nWHERE {\n");
                if ( countB < countA && countB > 0 ) {
                    geoQuery.append("?s <" + geoPropsA.get(0) + "> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n");
                } else {
                    geoQuery.append("?s <" + geoPropsB.get(0) + "> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n");
                }
                geoQuery.append("FILTER (bif:st_contains (?geo, bif:st_geomfromtext(\"" + geoA.getCentroid().toText() + "\"), " + ((float) 100 / 111195) + "))\n"
                        + "} } }");

                //System.out.println("For loop query "+geoQuery.toString());

                service = grConf.getEndpointB();
                graph = grConf.getGraphB();
                if ( countB < countA && countB > 0) {
                    service = grConf.getEndpointA();
                    graph = grConf.getGraphA();
                }

                authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
                qeh = new QueryEngineHTTP(service, geoQuery.toString(), authenticator);
                qeh.addDefaultGraph(graph);
                queryExecution = qeh;
                final com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();

                // Fetch neighboring entities Predicate.Object pairs
                Map<String, List<SubObjPair>> mappings = new HashMap<>();
                Map<String, IntWrapper> freqs = new HashMap<>();
                
                Set<String> uniqueSubs = new HashSet<>();

                while (resultSet.hasNext()) {
                    final QuerySolution querySolution = resultSet.next();

                    final String geo = querySolution.getLiteral("?geo").getString();
                    final String pre = querySolution.getResource("?p").getURI();
                    String obj = "";
                    RDFNode n = querySolution.get("?o");
                    if (n.isResource()) {
                        continue;
                    } else {
                        obj = n.asLiteral().getString();
                    }

                    //System.out.println(obj + " : " + patternText.matcher(obj).find());
                    //System.out.println(obj + " : " + patternInt.matcher(obj).find());
                    //System.out.println(obj + " : " + obj.matches(strPatternText));

                    if (patternInt.matcher(obj).find()) {
                        continue;
                    }

                    if (!patternText.matcher(obj).find()) {
                        continue;
                    }

                    if (obj.contains("http")) {
                        continue;
                    }

                    final String sub = querySolution.getResource("?s").getURI();

                    uniqueSubs.add(sub);

                    IntWrapper freq = freqs.get(obj);
                    if (freq == null) {
                        IntWrapper newFreq = new IntWrapper(0);
                        freq = newFreq;
                        freqs.put(obj, newFreq);
                    }
                    freq.inc();

                    geoms.put(sub, geo);
                        
                    SubObjPair pair = new SubObjPair(sub, obj);
                    List<SubObjPair> pairList = mappings.get(pre);
                    if (pairList == null) {
                        pairList = new ArrayList<>();
                        mappings.put(pre, pairList);
                    }
                    pairList.add(pair);

                    //System.out.println("Fetched "+geo);
                }
                queryExecution.close();
                
                //System.out.println("Hash Set Size : " + uniqueSubs.size());
                for (Map.Entry<String, List<SubObjPair>> entry : mappings.entrySet()) {
                    String sub = entry.getKey();
                    List<SubObjPair> pairs = entry.getValue();
                    //System.out.println(sub);

                    for (SubObjPair pa : pairs) {
                        //System.out.println("Freq : " + ((float) freqs.get(pa.getObj()).i / uniqueSubs.size()));
                        //System.out.println(pa.sub + " : " + pa.getObj());
                    }
                    //System.out.println();
                }

                for (Map.Entry<String, IntWrapper> entry : freqs.entrySet()) {
                    String obj = entry.getKey();
                    IntWrapper i = entry.getValue();
                    //System.out.println(obj + " : " + i.i);
                }

                // Fetch selected entity Predicate.Object pairs
                String query = "SELECT ?p ?o\n"
                        + "WHERE { <" + subA + "> ?p ?o }";
                
                System.out.println(query);
                service = grConf.getEndpointA();
                graph = grConf.getGraphA();
                if ( countB < countA && countB > 0 ) {
                    service = grConf.getEndpointB();
                    graph = grConf.getGraphB();
                }

                authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
                qeh = new QueryEngineHTTP(service, query, authenticator);
                qeh.addDefaultGraph(graph);
                queryExecution = qeh;
                final com.hp.hpl.jena.query.ResultSet resultSetEnt = queryExecution.execSelect();
                
                while (resultSetEnt.hasNext()) {
                    final QuerySolution querySolution = resultSetEnt.next();

                    final String pre = querySolution.getResource("?p").getURI();
                    String obj = "";
                    RDFNode n = querySolution.get("?o");
                    if (n.isResource()) {
                        continue;
                    } else {
                        obj = n.asLiteral().getString();
                    }

                    if (patternInt.matcher(obj).find()) {
                        continue;
                    }

                    if (!patternText.matcher(obj).find()) {
                        continue;
                    }

                    if (obj.contains("http")) {
                        continue;
                    }

                    //System.out.println(subA + " " + pre + " " + obj);

                    boolean foundLink = false;
                    for (Map.Entry<String, List<SubObjPair>> entry : mappings.entrySet()) {
                        String sub = entry.getKey();
                        List<SubObjPair> pairs = entry.getValue();

                        for (SubObjPair pa : pairs) {
                            float tf = (float) freqs.get(pa.getObj()).i / uniqueSubs.size();
                            if (freqs.get(pa.getObj()).i > 1) {
                                continue;
                            } else {
                                //System.out.println("Comparing " + obj + " with " + pa.getObj());
                                float JaccardIndex = getJaccardIndex(obj, pa.getObj());
                                if (JaccardIndex > 0.8) {
                                    System.out.println("Matched " + obj + " with " + pa.getObj());
                                    System.out.println("Matched " + subA + " with " + pa.getSub());
                                    String geomText = geoms.get(pa.getSub());
                                    Geometry tmpGeom = wkt.read(geomText);
                                    System.out.println("Distance " + tmpGeom.getCentroid().distance(geoA.getCentroid()) * 111195 );
                                    double dist = tmpGeom.getCentroid().distance(geoA.getCentroid()) * 111195;
                                    if ( dist > maxDist ) {
                                        maxDist = dist;
                                    }
                                    JSONGeomLink l = new JSONGeomLink(
                                            subA, geoA.toText(),
                                            pa.getSub(), geoms.get(pa.getSub()),
                                            dist,
                                            JaccardIndex
                                    );

                                    ret.AddLink(l);
                                }
                            }
                        }
                    }

                }
                queryExecution.close();
                
                maxDistIndex.put(subA, maxDist);
            }
            
            for (JSONGeomLink l : newLinks) {
                //System.out.println("Sub A" + l.subA);
                //System.out.println("Geo A" + l.geomA);
                //System.out.println("Sub B" + l.subB);
                //System.out.println("Geo B" + l.geomB);
            }
            //System.out.println(mapper.writeValueAsString(newLinks));
            System.out.println("Size of geomsA "+geomsA.size());
            System.out.println("Size of geoms "+geoms.size());
            
            List<String> toBeRemovedA = new ArrayList<>();
            List<String> toBeRemovedB = new ArrayList<>();
            List<String> toBeRemovedLinked = new ArrayList<>();
            HashMap<String, String> geomsTemp = new HashMap<>();
            
            System.out.println("Prev fetched " + fetchedGeomsA );
            for ( JSONGeomLink l : ret.getLinks() ) {
                System.out.printf("Distance before %.9f", l.getDist());
                l.setDist(l.getDist() / maxDistIndex.get(l.getSubA() ));
                System.out.printf("Distance after %.9f", l.getDist());
                geomsRef.remove(l.getSubA());
                geoms.remove(l.getSubB());
                if (fetchedGeomsA.contains(l.getSubA())) {
                    l.setGeomA("");
                } else {
                    fetchedGeomsA.add(l.getSubA());
                }
                if (fetchedGeomsA.contains(l.getSubB())) {
                    l.setGeomB("");
                } else {
                    fetchedGeomsA.add(l.getSubB());
                }
                if (geomsRef.containsKey(l.getSubA())) {
                    l.setGeomA("");
                }
                if (geoms.containsKey(l.getSubB())) {
                    l.setGeomB("");
                }
            }
            
            for (Map.Entry<String, Geometry> entry : geomsA.entrySet()) {
                String key = entry.getKey();
                Geometry val = entry.getValue();
                if (!fetchedGeomsA.contains(key)) {
                    fetchedGeomsA.add(key);
                } else {
                    //geomsA.remove(key);
                    toBeRemovedA.add(key);
                    
                    continue;
                }
                //geomsTemp.put(
                //        key,
                //        val.toText()
                //);
            }
            for ( String k : toBeRemovedA ) {
                geomsA.remove(k);
            }
            for (Map.Entry<String, Geometry> entry : geomsB.entrySet()) {
                String key = entry.getKey();
                Geometry val = entry.getValue();
                if (!fetchedGeomsA.contains(key)) {
                    fetchedGeomsA.add(key);
                } else {
                    //geomsB.remove(key);
                    toBeRemovedB.add(key);

                    continue;
                }
                //geoms.put(
                //        key,
                //        val.toText()
                //);
            }
            for ( String k : toBeRemovedB ) {
                geomsB.remove(k);
            }
            for (Map.Entry<String, String> entry : geoms.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (!fetchedGeomsA.contains(key)) {
                    fetchedGeomsA.add(key);
                } else {
                    //geomsB.remove(key);
                    toBeRemovedLinked.add(key);

                    continue;
                }
                //geoms.put(
                //        key,
                //        val.toText()
                //);
            }
            for ( String k : toBeRemovedLinked ) {
                geoms.remove(k);
            }
            
            for (Map.Entry<String, Geometry> entry : geomsB.entrySet()) {
                String key = entry.getKey();
                Geometry val = entry.getValue();
                geoms.put(
                        key,
                        val.toText()
                );
            } 
            for (Map.Entry<String, Geometry> entry : geomsRef.entrySet()) {
                String key = entry.getKey();
                Geometry val = entry.getValue();
                geomsTemp.put(
                        key,
                        val.toText()
                );
            }
            
            System.out.println("Size of geomsA "+geomsA.size());
            System.out.println("Size of geomsB "+geomsB.size());
            System.out.println("Size of geoms "+geoms.size());
            System.out.println("Size of geomsTemp "+geomsTemp.size());
            
            ret.dataset = "A";
            if ( countB < countA && countB > 0) {
                ret.dataset = "B";
            }
            ret.setEntitiesA(geomsTemp);
            ret.setEntitiesB(geoms);
            
            System.out.println(mapper.writeValueAsString(ret));
            
            /*
            System.out.println("Geom query " + geoQuery);
            p = geoPropsB.get(0);
            geoQuery.setLength(0);
            for (String t : geoTypesB) {
                if (t.equalsIgnoreCase("POLYGON")) {
                    geoQuery.append("SELECT ?s ?p ?o ?geo\nWHERE { ?s ?p ?o {\n");
                    geoQuery.append("SELECT ?s ?geo WHERE {\n"
                            + "?s <" + p + "> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n");
                    if (BBox.getLeft() < 0) {
                        geoQuery.append("FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " )  < " + (BBox.getRight() + X_MAX) + ")\n"
                                + "FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " ) > " + (BBox.getLeft() + X_MAX) + ")\n");
                    } else {
                        geoQuery.append("FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " )  < " + (BBox.getRight() + X_MAX) + ")\n"
                                + "FILTER ( ( bif:st_xmax(?geo) + " + X_MAX + " ) > " + (BBox.getLeft() + X_MAX) + ")\n");
                    }
                    if (BBox.getBottom() < 0) {
                        geoQuery.append("FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) > " + (BBox.getTop() + Y_MAX) + ")\n"
                                + "FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) < " + (BBox.getBottom() + Y_MAX) + ")\n");
                    } else {
                        geoQuery.append("FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) < " + (BBox.getTop() + Y_MAX) + ")\n"
                                + "FILTER ( ( bif:st_ymax(?geo) + " + Y_MAX + " ) > " + (BBox.getBottom() + Y_MAX) + ")\n");
                    }
                    geoQuery.append("} } }");
                } else {
                    geoQuery.append("SELECT ?s ?p ?o ?geo\nWHERE { ?s ?p ?o {\n");
                    geoQuery.append("SELECT ?s ?geo\n"
                            + "WHERE {\n"
                            + "?s <" + p + "> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n"
                            + "FILTER (bif:st_intersects (?geo, bif:st_geomfromtext(\"" + BBox.getBarea() + "\"), 0))\n"
                            + "} } }");
                }
            }
            
            System.out.println("Geom query " + geoQuery);
            
            service = grConf.getEndpointB();
            graph = grConf.getGraphB();
            
            authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
            qeh = new QueryEngineHTTP(service, geoQuery.toString(), authenticator);
            qeh.addDefaultGraph(graph);
            queryExecution = qeh;
            final com.hp.hpl.jena.query.ResultSet resultSetB = queryExecution.execSelect();
            
            // Fetch neighboring entities Predicate.Object pairs
            HashMap<String, List<SubObjPair>> mappingsB = new HashMap<>();
            HashMap<String, IntWrapper> freqsB = new HashMap<>();
            HashMap<String, String> geomsB = new HashMap<>();
            Set<String> uniqueSubsB = new HashSet<>();
            
            while (resultSetB.hasNext()) {
                final QuerySolution querySolution = resultSetB.next();

                final String geo = querySolution.getLiteral("?geo").getString();
                final String pre = querySolution.getResource("?p").getURI();
                String obj = "";
                RDFNode n = querySolution.get("?o");
                if ( n.isResource() )
                    continue;
                else
                    obj = n.asLiteral().getString();
                
                //System.out.println(obj + " : " + patternText.matcher(obj).find());
                //System.out.println(obj + " : " + patternInt.matcher(obj).find());
                //System.out.println(obj + " : " + obj.matches(strPatternText));
                
                if ( patternInt.matcher(obj).find() )
                    continue;
                
                if ( !patternText.matcher(obj).find() )
                    continue;
                
                if ( obj.contains("http") )
                    continue;
                
                final String sub = querySolution.getResource("?s").getURI();
                if ( !fetchedGeomsA.contains(sub) ) {
                    newGeom++;
                    geomsB.put(sub, geo);
                } else {
                    //ret.AddEntityB(new JSONUnlinkedEntity(geo, sub));
                    geomsB.put(sub, "");
                }
                
                uniqueSubsB.add(sub);
                
                IntWrapper freq = freqsB.get(obj);
                if ( freq == null ) {
                    IntWrapper newFreq = new IntWrapper(0);
                    freq = newFreq;
                    freqsB.put(obj, newFreq);
                }
                freq.inc();
                
                SubObjPair pair = new SubObjPair(sub, obj);
                List<SubObjPair> pairList = mappingsB.get(pre);
                if ( pairList == null ) {
                    pairList = new ArrayList<>();
                    mappingsB.put(pre, pairList);
                }
                pairList.add(pair);
            }
            
            String subA = "";
            String subB = "";
            System.out.println("reached");
            for (Map.Entry<String, List<SubObjPair>> entryA : mappingsA.entrySet()) {
                subA = entryA.getKey();
                List<SubObjPair> pairsA = entryA.getValue();
                for (SubObjPair pA : pairsA) {
                    float tfA = (float) freqsA.get(pA.getObj()).i / uniqueSubsA.size();
                    if (freqsA.get(pA.getObj()).i > 1) {
                        continue;
                    }
                    for (Map.Entry<String, List<SubObjPair>> entryB : mappingsB.entrySet()) {
                        subB = entryB.getKey();
                        List<SubObjPair> pairsB = entryB.getValue();
                        //System.out.println("Subject " + subA + " + " + subB);
                        for (SubObjPair pB : pairsB) {
                            float tfB = (float) freqsB.get(pB.getObj()).i / uniqueSubsB.size();
                            if (freqsB.get(pB.getObj()).i > 1) {
                                continue;
                            } else {
                                float JaccardIndex = getJaccardIndex(pA.getObj(), pB.getObj());
                                if (JaccardIndex >= 0.8) {
                                    System.out.println("Matched " + pA.getObj() + " with " + pB.getObj());
                                    JSONGeomLink l = new JSONGeomLink (
                                        pA.getSub(), geomsA.get(pA.getSub()),
                                        pB.getSub(), geomsB.get(pB.getSub())
                                     );

                                     ret.AddLink(l);
                                }
                            }
                        }
                    }
                }
            }
            
            System.out.println("Size A "+geomsA.size());
            System.out.println("Size B "+geomsB.size());
            
            for ( String sub : geomsA.keySet() ) {
                fetchedGeomsA.add(sub);
            }
            
            for ( String sub : geomsB.keySet() ) {
                fetchedGeomsA.add(sub);
            }
            
            for ( JSONGeomLink l : ret.getLinks() ) {
                geomsA.remove(l.getSubA());
                geomsB.remove(l.getSubB());
                
                System.out.println("Subject A : " + l.getSubA());
                System.out.println("Subject B : " + l.getSubB());
                System.out.println("Geom A : " + l.getGeomA());
                System.out.println("Geom B : " + l.getGeomB());
            }
            ret.setEntitiesA(geomsA);
            ret.setEntitiesB(geomsB);
            System.out.println("Size A "+geomsA.size());
            System.out.println("Size B "+geomsB.size());
            System.out.println(mapper.writeValueAsString(ret));
            out.print(mapper.writeValueAsString(ret));
            */
            //System.out.println(mapper.writeValueAsString(ret));
            out.print(mapper.writeValueAsString(ret));
        }
    }
    
    final Pattern patternWordbreaker = Pattern.compile( "(([a-z]|[A-Z])[a-z]+)|(([a-z]|[A-Z])[A-Z]+)" );
    private float getJaccardIndex(String a, String b) {
        List<String> arrA = new ArrayList<>();
        List<String> arrB = new ArrayList<>();
        //System.out.println(chain.link);
        Matcher matA = patternWordbreaker.matcher(a);
        while (matA.find()) {
            arrA.add(matA.group());
        }
        Matcher matB = patternWordbreaker.matcher(b);
        while (matB.find()) {
            arrB.add(matB.group());
        }
        
        int intersection = 0;
        Set<String> union = new HashSet<>();
        for ( String tA : arrA ) {
            union.add(tA.toLowerCase());
            for ( String tB : arrB ) {
                double dist = StringUtils.getJaroWinklerDistance(tA, tB);
                //double dist = StringUtils.getLevenshteinDistance(tA, tB);
                //System.out.println("Distance "+dist);
                //System.out.println(tA+ "      " + tB);
                union.add(tB.toLowerCase());
                if ( dist > 0.8 )
                    intersection++;
            }
        }
        
        //System.out.println(intersection+"/"+union.size());
        
        return (float)intersection/union.size();
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
        } catch (ParseException ex) {
            Logger.getLogger(BatchFindLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
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
        } catch (ParseException ex) {
            Logger.getLogger(BatchFindLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
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
