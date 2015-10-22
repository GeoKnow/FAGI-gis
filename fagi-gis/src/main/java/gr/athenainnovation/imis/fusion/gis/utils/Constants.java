/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.utils;

/**
 *
 * @author Nick Vitsas
 */
public class Constants {
    
    // Whether to use Late Fetching
    public static final boolean     LATE_FETCH = true;
    
    //Postgres URL
    public static final String      DB_URL = "jdbc:postgresql:";
    
    public static final String      OWL_CLASS_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    
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
    
    // In case of failures, how many times to repeat a query until giving up
    public static final int         MAX_SPARQL_TRIES = 3;
    
    public static final float       NANOS_PER_SECOND = 1000000000f;
    
    // Number of links to use for property matching sampling
    public static final int         SAMPLE_SIZE = 20;  
    
    // FAGI special property Separator
    public static final String      PROPERTY_SEPARATOR = "=>";
    
    // WordNet paths per OS
    public static final String      PATH_TO_WORDNET_LINUX = "/usr/share/wordnet";
    public static final String      PATH_TO_WORDNET_OS_X = "/usr/local/WordNet-3.0/dict";
    public static final String      PATH_TO_WORDNET_WINDOWS = "C:\\Program Files (x86)\\WordNet\\2.1\\dict";
    
}
