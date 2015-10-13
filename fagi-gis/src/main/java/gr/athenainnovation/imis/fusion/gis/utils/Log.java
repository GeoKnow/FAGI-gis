/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.utils;

import java.util.Enumeration;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author Nick Vitsas
 */
public class Log {
    
    private static final Logger FAGI_ROOT_LOG = Logger.getRootLogger();    
    
    public static final Logger getFAGILogger() {
        return FAGI_ROOT_LOG;
    }
    
    public static final Logger getClassFAGILogger(Class cls) {
        return Logger.getLogger(cls);
    }
    
    public static final void setFAGILoggerLevel(Level lvl) {
        if ( lvl == Level.DEBUG ) {
            
        } else if ( lvl == Level.INFO ) {
        
        } else if ( lvl == Level.TRACE ) {
            
        }
    }
    
    private static FileAppender getFAGILoggerFileAppender() {
        Enumeration e = FAGI_ROOT_LOG.getAllAppenders();
        while ( e.hasMoreElements() ) {
            Appender app = (Appender) e.nextElement();
            
            if ( app instanceof FileAppender ) {
                return (FileAppender) app;
            }
            
        }
        
        return null;
    }
    
    private static ConsoleAppender getFAGILoggerConsoleAppender() {
        Enumeration e = FAGI_ROOT_LOG.getAllAppenders();
        while ( e.hasMoreElements() ) {
            Appender app = (Appender) e.nextElement();
            
            if ( app instanceof ConsoleAppender ) {
                return (ConsoleAppender) app;
            }
            
        }
        
        return null;
    }
}
