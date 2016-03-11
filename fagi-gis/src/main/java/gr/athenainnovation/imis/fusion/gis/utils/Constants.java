/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.utils;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Nick Vitsas
 */
public class Constants {
    
    // Whether to use Late Fetching
    public static final boolean                 LATE_FETCH = true;
    public static final boolean              MAP_STREAM = false;
    
    //Postgres URL
    public static final String                  DB_URL = "jdbc:postgresql:";
    
    public static final String                  OWL_CLASS_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    
    // This is the FAGI-gis prefered hasGeometry predicate
    public static final String                  HAS_GEOMETRY_REGEX = "http://www.opengis.net/ont/geosparql#hasGeometry";
    public static final String                  HAS_GEOMETRY = HAS_GEOMETRY_REGEX;
    public static final String                  GEOMETRY_TYPE_REGEX = "http://www.openlinksw.com/schemas/virtrdf#Geometry";
    
    // The REQUIRED FAGI gis Well Known Text predicate
    public static final String                  AS_WKT_REGEX = "http://www.opengis.net/ont/geosparql#asWKT";
    public static final String                  WKT = AS_WKT_REGEX;
    
    // WKT Literal
    public static final String                  WKT_LITERAL_REGEX = "http://www.opengis.net/ont/geosparql#wktLiteral";
        
    // Basic WGS predicates
    public static final String                  LONG_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#long";
    public static final String                  LAT_REGEX = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    
    // Properties for inserting OSM Rec class recommendations
    public static final String                  OWL_CLASS = "http://www.w3.org/2002/07/owl#Class";
    public static final String                  LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
    public static final String                  TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    // This is the FAGI-gis REQUIRED link predicate
    public static final String                  SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";

    // Batch Size refers to the MAX size of the links graph
    // That a SPARQL query can handle
    public static final int                     BATCH_SIZE = 100;
    
    // In case of failures, how many times to repeat a query until giving up
    public static final int                     MAX_SPARQL_TRIES = 3;
    
    public static final float                   NANOS_PER_SECOND = 1000000000f;
    
    // Number of links to use for property matching sampling
    public static final int                     SAMPLE_SIZE = 5;  
    
    // FAGI special property Separator
    public static final String                  PROPERTY_SEPARATOR = "=>";
    
    // WordNet paths per OS
    public static final String                  PATH_TO_WORDNET_LINUX = "/usr/share/wordnet";
    public static final String                  PATH_TO_WORDNET_OS_X = "/usr/local/WordNet-3.0/dict";
    public static final String                  PATH_TO_WORDNET_WINDOWS = "C:\\Program Files (x86)\\WordNet\\2.1\\dict";
    
    // Useful integer mapping to the two Datasets
    public static final int                     DATASET_A = 0;
    public static final int                     DATASET_B = 1;
    
    // Max Metadata Depth to break recursions
    public static final int                     MAX_METADATA_DEPTH = 6;
    
    // Quick approx from Merc map Distance to meter distance
    public static final int                     MAGIC_MERC_TO_METERS_NUMBER = 111195;
    public static final float                   MAGIC_METERS_TO_MErc_NUMBER = 1.0F/111195;
    
    public static final float                   MERC_X_MAX = 180f;
    public static final float                   MERC_Y_MAX = 85.05f;
    
    public static final Map<String, Integer>    GEOM_TYPE_PRECEDENCE_TABLE;
    static {
        GEOM_TYPE_PRECEDENCE_TABLE = Maps.newHashMapWithExpectedSize(7);
        GEOM_TYPE_PRECEDENCE_TABLE.put("MULTIPOLYGON", 0);
        GEOM_TYPE_PRECEDENCE_TABLE.put("POLYGON", 1);
        GEOM_TYPE_PRECEDENCE_TABLE.put("MULTILINESTRING", 2);
        GEOM_TYPE_PRECEDENCE_TABLE.put("LINESTRING", 3);
        GEOM_TYPE_PRECEDENCE_TABLE.put("MULTIPOINT", 4);
        GEOM_TYPE_PRECEDENCE_TABLE.put("POINT", 5);
        GEOM_TYPE_PRECEDENCE_TABLE.put("NONE", 6);
    }
    
    public static final boolean                 FAAS = false;
}
