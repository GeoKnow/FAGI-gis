/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.utils;

import java.util.regex.Pattern;

/**
 * Regex patterns used in various parts of FAGI-gis
 * @author Nick Vitsas
 */
public class Patterns {
    
    //Integer
    public static final Pattern PATTERN_INT = Pattern.compile( "^(\\d+)$" );
    
    //Date
    public static final Pattern PATTERN_DATE = Pattern.compile( "^(\\d{2}(/\\d{2}/\\d{4}|-\\d{2}-\\d{4}))$" );
    
    //Word
    public static final Pattern PATTERN_WORD = Pattern.compile( "^(\\w)$" );
    
    //Text
    public static final Pattern PATTERN_TEXT = Pattern.compile( "\\w(\\s+\\w)+" );
    
    //Decimal
    public static final Pattern PATTERN_DECIMAL = Pattern.compile( "^(\\d+(.|,)\\d+)$" );
    
    // Word breaker
    public static final Pattern PATTERN_WORD_BREAKER = Pattern.compile( "(([a-z]|[A-Z])[a-z]+)|(([a-z]|[A-Z])[A-Z]+)" );
    
    // POLYGON Pattern
    public static final Pattern PATTERN_POLYGON = Pattern.compile(  "POLYGON\\(\\((.*?)\\)\\)" );
    
    // Triple Pattern
    public static final Pattern PATTERN_TRIPLE = Pattern.compile( "\\{([^F]*)" );
    
}
