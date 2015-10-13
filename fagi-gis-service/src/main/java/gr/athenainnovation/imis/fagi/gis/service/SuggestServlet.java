/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gr.athenainnovation.imis.fagi.gis.service;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Nick Vitsas
 */
@WebServlet(name = "SuggestServlet", urlPatterns = {"/SuggestServlet"})
public class SuggestServlet extends HttpServlet {

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
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        //System.out.println(request.getParameter("query"));
        
        final String queryString = "select distinct ?g\n" +
                                    "where { GRAPH ?g { ?s ?p ?o } }";
        
        //System.out.println(request.getParameter("db_end"));
        String getFromA = "";
        
        //QueryExecution selectFromB = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", queryString );  
        QueryExecution selectFromB = QueryExecutionFactory.sparqlService("http://localhost:8890/sparql", queryString );  
        
        final com.hp.hpl.jena.query.ResultSet resultSetFromB = selectFromB.execSelect();
        StringBuilder geomColl = new StringBuilder(10000);
        
        //System.out.print("luda");
        
        while(resultSetFromB.hasNext()) {
            final QuerySolution querySolution = resultSetFromB.next();
            RDFNode s, p1, g, p2;
            g = querySolution.get("?g");
            
            if (g != null) {
                //System.out.println(g.asNode());
                geomColl.append(g.asNode());
                geomColl.append(";");
            }
        }
        
        int len = geomColl.length();
        geomColl.setLength(len-1);
        
        //System.out.print("luda2");
        try {
            /* TODO output your page here. You may use following sample code. */
            /*
            select distinct ?g where { GRAPH ?g { ?s ?p ?o } }
            */
            out.print(geomColl.toString());
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
            throws ServletException, IOException {
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
