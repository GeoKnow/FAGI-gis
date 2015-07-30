/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import java.io.IOException;
import java.io.PrintWriter;
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

/**
 *
 * @author nick
 */
@WebServlet(name = "FindLinkServlet", urlPatterns = {"/FindLinkServlet"})
public class FindLinkServlet extends HttpServlet {
    
    private JSONEntity ent = null;
    
    private GraphConfig grConf;
    private DBConfig dbConf;
    private HttpSession sess;
    
    private WKTReader wkt = new WKTReader();
    
    private static class JSONEntity {
        String sub;
        String ds;
        String geom;

        public JSONEntity() {
        }

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }

        public String getDs() {
            return ds;
        }

        public void setDs(String ds) {
            this.ds = ds;
        }

        public String getGeom() {
            return geom;
        }

        public void setGeom(String geom) {
            this.geom = geom;
        }

    }
    
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

    }
    
    //Text
    private final String strPatternText = "[a-zA-Z]+(\\b[a-zA-Z]+\\b)*([a-zA-Z])";
    final Pattern patternText = Pattern.compile( strPatternText );
    final Pattern patternInt = Pattern.compile( "^(\\d+)$" );
    
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
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            sess = request.getSession();
            if ( sess == null )
                out.print("Invalid session");
            
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            dbConf = (DBConfig)sess.getAttribute("db_conf");
        
            String jsonEntity = request.getParameter("entity");
            int radius = Integer.parseInt(request.getParameter("radius"));
            if ( jsonEntity == null )
                out.print("Invalid entity");
            
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
            JsonParser jp = factory.createJsonParser(jsonEntity);
            ent = mapper.readValue(jp, JSONEntity.class );

            grConf.scanGeoProperties();
            
            List<String> geoPropsA = grConf.getGeoPropertiesA();
            List<String> geoPropsB = grConf.getGeoPropertiesB();
            List<String> geoTypesA = grConf.getGeoTypesA();
            List<String> geoTypesB = grConf.getGeoTypesB();
                        
            StringBuilder geoQuery = new StringBuilder();
            geoQuery.append("SELECT ?s ?p ?o ?geo\nWHERE { ?s ?p ?o {\n");
            geoQuery.append("SELECT ?s ?geo\nWHERE {\n");
            if (ent.getDs().equals("B")) {
                geoQuery.append("?s <" + geoPropsA.get(0) + "> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n");
            } else {
                geoQuery.append("?s <" + geoPropsB.get(0) + "> ?o . ?o <http://www.opengis.net/ont/geosparql#asWKT> ?geo .\n");
            }
            geoQuery.append("FILTER (bif:st_contains (?geo, bif:st_geomfromtext(\"" + ent.getGeom() + "\"), "+((float)radius / 111195) + "))\n"
                    + "} } }");

            System.out.println(geoQuery.toString());
            
            String service = grConf.getEndpointB();
            String graph = grConf.getGraphB();
            if ( ent.getDs().equals("B") ) {
                service = grConf.getEndpointA();
                graph = grConf.getGraphA();
            }
            
            
            HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
            QueryEngineHTTP qeh = new QueryEngineHTTP(service, geoQuery.toString(), authenticator);
            qeh.addDefaultGraph(graph);
            QueryExecution queryExecution = qeh;
            final ResultSet resultSet = queryExecution.execSelect();

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

            System.out.println("Fetched from A : " + fetchedGeomsA.size());
            System.out.println("Fetched from B : " + fetchedGeomsB.size());

            // Fetch neighboring entities Predicate.Object pairs
            Map<String, List<SubObjPair>> mappings = new HashMap<>();
            Map<String, IntWrapper> freqs = new HashMap<>();
            Map<String, String> geoms = new HashMap<>();
            Set<String> uniqueSubs = new HashSet<>();
            
            Geometry centerGeom = wkt.read(ent.getGeom()).getCentroid();
            double maxDist = -1;
            int newGeom = 0;
            while (resultSet.hasNext()) {
                final QuerySolution querySolution = resultSet.next();

                final String geo = querySolution.getLiteral("?geo").getString();
                final String pre = querySolution.getResource("?p").getURI();
                String obj = "";
                RDFNode n = querySolution.get("?o");
                if ( n.isResource() )
                    continue;
                else
                    obj = n.asLiteral().getString();
                
                System.out.println(obj + " : " + patternText.matcher(obj).find());
                System.out.println(obj + " : " + patternInt.matcher(obj).find());
                System.out.println(obj + " : " + obj.matches(strPatternText));
                
                if ( patternInt.matcher(obj).find() )
                    continue;
                
                if ( !patternText.matcher(obj).find() )
                    continue;
                
                if ( obj.contains("http") )
                    continue;
                
                final String sub = querySolution.getResource("?s").getURI();
                geoms.put(sub, geo);
                
                uniqueSubs.add(sub);
                
                IntWrapper freq = freqs.get(obj);
                if ( freq == null ) {
                    IntWrapper newFreq = new IntWrapper(0);
                    freq = newFreq;
                    freqs.put(obj, newFreq);
                }
                freq.inc();
                
                if ( !fetchedGeomsA.contains(sub) ) {
                    fetchedGeomsA.add(sub);
                    newGeom++;
                }
                
                SubObjPair pair = new SubObjPair(sub, obj);
                List<SubObjPair> pairList = mappings.get(pre);
                if ( pairList == null ) {
                    pairList = new ArrayList<>();
                    mappings.put(pre, pairList);
                }
                pairList.add(pair);
                
                //System.out.println("Fetched "+geo);
            }
            
            System.out.println("Hash Set Size : " + uniqueSubs.size());
            for ( Map.Entry<String, List<SubObjPair>> entry : mappings.entrySet() ) {
                String sub = entry.getKey();
                List<SubObjPair> pairs = entry.getValue();
                System.out.println(sub);
                
                for ( SubObjPair p : pairs) {
                    System.out.println("Freq : "+((float)freqs.get(p.getObj()).i / uniqueSubs.size()));
                    System.out.println(p.sub+ " : " +p.getObj());
                }
                System.out.println();
            }
            
            for ( Map.Entry<String, IntWrapper> entry : freqs.entrySet() ) {
                String obj = entry.getKey();
                IntWrapper i = entry.getValue();
                System.out.println(obj+" : "+i.i);
            }
            
            // Fetch selected entity Predicate.Object pairs
            String query = "SELECT ?p ?o\n" +
                           "WHERE { <"+ent.getSub()+"> ?p ?o }";
            
            service = grConf.getEndpointA();
            graph = grConf.getGraphA();
            if ( ent.getDs().equals("B") ) {
                service = grConf.getEndpointB();
                graph = grConf.getGraphB();
            }
            
            authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
            //QueryExecution queryExecution = QueryExecutionFactory.sparqlService(service, query, graph, authenticator);
            qeh = new QueryEngineHTTP(service, query, authenticator);
            qeh.addDefaultGraph(graph);
            queryExecution = qeh;
            final ResultSet resultSetEnt = queryExecution.execSelect();
            
            List<JSONGeomLink> newLinks = new ArrayList<>();
            while (resultSetEnt.hasNext()) {
                final QuerySolution querySolution = resultSetEnt.next();

                final String pre = querySolution.getResource("?p").getURI();
                String obj = "";
                RDFNode n = querySolution.get("?o");
                if ( n.isResource() ) {
                    continue;
                } else {
                    obj = n.asLiteral().getString();
                } 
                
                if ( patternInt.matcher(obj).find() )
                    continue;
                
                if ( !patternText.matcher(obj).find() )
                    continue;
                
                if ( obj.contains("http") )
                    continue;
                

                System.out.println(ent.getSub()+ " " + pre + " " + obj);
                
                boolean foundLink = false;
                String subA = "";
                String subB = "";
                for (Map.Entry<String, List<SubObjPair>> entry : mappings.entrySet()) {
                    String sub = entry.getKey();
                    List<SubObjPair> pairs = entry.getValue();

                    for (SubObjPair p : pairs) {
                        float tf = (float) freqs.get(p.getObj()).i / uniqueSubs.size();
                        if ( freqs.get(p.getObj()).i > 1 ) {
                            continue;
                        } else {
                            System.out.println("Comparing "+obj + " with "+ p.getObj() );
                            Geometry tmpGeom = wkt.read(geoms.get(p.getSub()));
                            System.out.println("Distance " + tmpGeom.getCentroid().distance(centerGeom) * 111195);
                            double dist = tmpGeom.getCentroid().distance(centerGeom) * 111195;
                            if (dist > maxDist) {
                                maxDist = dist;
                            }
                            float JaccardIndex = getJaccardIndex(obj, p.getObj());
                            if ( JaccardIndex > 0.8 ) {
                                JSONGeomLink l = new JSONGeomLink(
                                    ent.getSub(), "",
                                        p.getSub(), geoms.get(p.getSub()),
                                        dist,
                                        JaccardIndex
                                );
                                
                                newLinks.add(l);
                            }
                        }
                    }
                }
                
                
            }
            for (JSONGeomLink l : newLinks) {
                System.out.println("Sub A"+l.subA);
                System.out.println("Geo A"+l.geomA);
                System.out.println("Sub B"+l.subB);
                System.out.println("Geo B"+l.geomB);
            }
            System.out.println(mapper.writeValueAsString(newLinks));
            out.print(mapper.writeValueAsString(newLinks));
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
        
        System.out.println(intersection+"/"+union.size());
        
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
            Logger.getLogger(FindLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(FindLinkServlet.class.getName()).log(Level.SEVERE, null, ex);
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
