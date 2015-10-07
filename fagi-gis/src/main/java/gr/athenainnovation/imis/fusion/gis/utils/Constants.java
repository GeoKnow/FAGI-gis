/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.utils;

/**
 *
 * @author nick
 */
public class Constants {
    
    //Postgres URL
    public static final String      DB_URL = "jdbc:postgresql:";
    
    // This is the FAGI-gis prefered hasGeometry predicate
    public static final String      HAS_GEOMETRY_REGEX = "http://www.opengis.net/ont/geosparql#hasGeometry";
        
    // The REQUIRED FAGI gis Well Known Text predicate
    public static final String      AS_WKT_REGEX = "http://www.opengis.net/ont/geosparql#asWKT";
    
    // Basic WGS predicates
    public static final String      LONG_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    public static final String      LAT_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    
    // This is the FAGI-gis REQUIRED link predicate
    public static final String      SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";

    // Batch Size refers to the MAX size of the links graph
    // That a SPARQL query can handle
    public static final int         BATCH_SIZE = 10000;
    
    // Number of links to use for property matching sampling
    public static final int         SAMPLE_SIZE = 20;  
}
