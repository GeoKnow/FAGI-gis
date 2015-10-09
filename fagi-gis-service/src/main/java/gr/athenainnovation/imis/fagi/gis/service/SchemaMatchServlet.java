/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fagi.gis.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import gr.athenainnovation.imis.fusion.gis.core.Link;
import gr.athenainnovation.imis.fusion.gis.gui.workers.DBConfig;
import gr.athenainnovation.imis.fusion.gis.gui.workers.GraphConfig;
import gr.athenainnovation.imis.fusion.gis.json.JSONMatches;
import gr.athenainnovation.imis.fusion.gis.json.JSONRequestResult;
import gr.athenainnovation.imis.fusion.gis.utils.Log;
import gr.athenainnovation.imis.fusion.gis.virtuoso.SchemaMatchState;
import gr.athenainnovation.imis.fusion.gis.virtuoso.ScoredMatch;
import gr.athenainnovation.imis.fusion.gis.virtuoso.VirtuosoImporter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.apache.jena.atlas.web.auth.HttpAuthenticator;
import org.apache.jena.atlas.web.auth.SimpleAuthenticator;
import org.apache.lucene.queryParser.ParseException;
import virtuoso.jena.driver.VirtGraph;

/**
 *
 * @author nick
 */
@WebServlet(name = "SchemaMatchServlet", urlPatterns = {"/SchemaMatchServlet"})
public class SchemaMatchServlet extends HttpServlet {
        
    private static final org.apache.log4j.Logger LOG = Log.getClassFAGILogger(SchemaMatchServlet.class);
    
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
        
        response.setContentType("application/json");
        
        // Per request state
        PrintWriter             out = null;
        JSONMatches             matches;
        JSONRequestResult       res;
        DBConfig                dbConf;
        GraphConfig             grConf;
        Connection              virt_conn;
        VirtGraph               vSet = null;
        HttpSession             sess;
        
        try {
            
            try {
                out = response.getWriter();
            } catch (IOException ex) {
                LOG.trace("IOException thrown in servlet Writer");
                LOG.debug("IOException thrown in servlet Writer : \n" + ex.getMessage() );
                
                return;
            }
            
            sess = request.getSession(false);

            if ( sess == null ) {
                out.print("{}");
                
                return;
            }
            
            matches = new JSONMatches();
            res = new JSONRequestResult();
            matches.setResult(res);
            
            ObjectMapper mapper = new ObjectMapper();
            //mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            //mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            //SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy");
            //mapper.setDateFormat(outputFormat);
            //mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            
            dbConf = (DBConfig)sess.getAttribute("db_conf");
            grConf = (GraphConfig)sess.getAttribute("gr_conf");
            
            String[] selectedLinks = request.getParameterValues("links[]");
            List<Link> lst = new ArrayList<>();
            for(String s : selectedLinks) {
                String subs[] = s.split("<-->");
                Link l = new Link(subs[0], subs[1]);
                lst.add(l);
                System.out.println("Link "+s);
            }
            
            if (vSet == null) {
                try {
                    vSet = new VirtGraph("jdbc:virtuoso://" + dbConf.getDBURL() + "/CHARSET=UTF-8",
                            dbConf.getUsername(),
                            dbConf.getPassword());
                } catch (JenaException connEx) {
                    System.out.println(connEx.getMessage());

                    return;
                }
            }
            
            virt_conn = vSet.getConnection();
            createLinksGraph(lst, virt_conn, grConf, "");
            
            System.out.println(request.getParameterMap());
            StringBuilder sb = new StringBuilder();
            
            VirtuosoImporter virtImp = (VirtuosoImporter)sess.getAttribute("virt_imp");
            SchemaMatchState sms = virtImp.scanProperties(3, null);
            
            System.out.println("Dom A "+sms.domOntoA+" Dom B "+sms.domOntoB);
            sess.setAttribute("domA", sms.domOntoA);
            sess.setAttribute("domB", sms.domOntoB);
            
            //sms.foundA.put("lalalala"+sess.getCreationTime(), new HashSet<String>());
            matches.setFoundA(sms.foundA);
            matches.setFoundB(sms.foundB);
            matches.setOtherPropertiesA(sms.otherPropertiesA);
            matches.setOtherPropertiesB(sms.otherPropertiesB);
            matches.setGeomTransforms(sms.geomTransforms);
            matches.setMetaTransforms(sms.metaTransforms);
            
            sess.setAttribute("property_patternsA", sms.getPropertyList("A"));
            sess.setAttribute("property_patternsB", sms.getPropertyList("B"));
            sess.setAttribute("predicates_matches", sms);
            System.out.println("Problem");
            sb.append("{");
            
            //System.out.println("Matches : "+mapper.writeValueAsString(matches));
            //System.out.println(sb);
            out.println(mapper.writeValueAsString(matches));
        } catch (JsonProcessingException ex) {
        } catch ( java.lang.OutOfMemoryError oome) {
            LOG.trace("OutOfMemoryError thrown");
            LOG.debug("OutOfMemoryError thrown : " + oome.getMessage());
            if (out != null) {
                out.println("{}");

                out.close();
            }
        } finally {
            if ( vSet != null ) {
                vSet.close();
            }
            
            if ( out != null )
                out.close();
        }
    }

    public void createLinksGraph(List<Link> lst, Connection virt_conn, GraphConfig grConf, String bulkInsertDir) throws SQLException, IOException {
        final String dropGraph = "sparql DROP SILENT GRAPH <"+ grConf.getLinksGraph()+  ">";
        final String createGraph = "sparql CREATE GRAPH <"+ grConf.getLinksGraph()+ ">";
        //final String endDesc = "sparql LOAD SERVICE <"+endpointA+"> DATA";

        PreparedStatement dropStmt;
        long starttime, endtime;
        dropStmt = virt_conn.prepareStatement(dropGraph);
        dropStmt.execute();

        dropStmt.close();
        
        PreparedStatement createStmt;
        createStmt = virt_conn.prepareStatement(createGraph);
        createStmt.execute();
        
        createStmt.close();
        
        //bulkInsertLinks(lst, virt_conn, bulkInsertDir);
        SPARQLInsertLink(lst, grConf);
    }

    private void SPARQLInsertLink(List<Link> l, GraphConfig grConf) {
        boolean updating = true;
        int addIdx = 0;
        int cSize = 1;
        int sizeUp = 1;
        while (updating) {
            try {
                ParameterizedSparqlString queryStr = new ParameterizedSparqlString();
                //queryStr.append("WITH <"+fusedGraph+"> ");
                queryStr.append("INSERT DATA { ");
                queryStr.append("GRAPH <"+ grConf.getLinksGraph()+ "> { ");
                int top = 0;
                if (cSize >= l.size()) {
                    top = l.size();
                } else {
                    top = cSize;
                }
                for (int i = addIdx; i < top; i++) {
                    final String subject = l.get(i).getNodeA();
                    final String subjectB = l.get(i).getNodeB();
                    queryStr.appendIri(subject);
                    queryStr.append(" ");
                    queryStr.appendIri(SAME_AS);
                    queryStr.append(" ");
                    queryStr.appendIri(subjectB);
                    queryStr.append(" ");
                    queryStr.append(".");
                    queryStr.append(" ");
                }
                queryStr.append("} }");
                //System.out.println("Print "+queryStr.toString());

                UpdateRequest q = queryStr.asUpdate();
                HttpAuthenticator authenticator = new SimpleAuthenticator("dba", "dba".toCharArray());
                UpdateProcessor insertRemoteB = UpdateExecutionFactory.createRemoteForm(q, grConf.getEndpointT(), authenticator);
                insertRemoteB.execute();
                //System.out.println("Add at "+addIdx+" Size "+cSize);
                addIdx += (cSize - addIdx);
                sizeUp *= 2;
                cSize += sizeUp;
                if (cSize >= l.size()) {
                    cSize = l.size();
                }
                if (cSize == addIdx) {
                    updating = false;
                }
            } catch (org.apache.jena.atlas.web.HttpException ex) {
                System.out.println("Failed at " + addIdx + " Size " + cSize);
                System.out.println("Crazy Stuff");
                System.out.println(ex.getLocalizedMessage());
                ex.printStackTrace();
                ex.printStackTrace(System.out);
                sizeUp = 1;
                cSize = addIdx;
                cSize += sizeUp;
                if (cSize >= l.size()) {
                    cSize = l.size();
                }
                //System.out.println("Going back at "+addIdx+" Size "+cSize);

                break;
                //System.out.println("Going back at "+addIdx+" Size "+cSize);
            } catch (Exception ex) {
                System.out.println(ex.getLocalizedMessage());
                break;
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
