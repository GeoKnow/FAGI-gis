/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.utils;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Scanner;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author nick
 */
public class Utilities {

    public static String getPredicateOntology(String pred )
    {
        String onto = StringUtils.substringBefore(pred, "#");
        onto = onto.concat("#");
        if (onto.equals(pred)) {
            onto = StringUtils.substring(pred, 0, StringUtils.lastIndexOf(pred, "/"));
            onto = onto.concat("/");
        }
        
        return onto;
    }
        
    public static String getPredicateName(String pred )
    {
        return null;
    }
    
    public static String convertStreamToString(java.io.InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    
    public static boolean isLocalInstance(InetAddress addr) {
        
        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
        
    }
    
    public static boolean isURLToLocalInstance(String url) {
        boolean isLocal;
        
        // Check if the address is defined on any interface
        try {
            URL endURL = new URL(url);
            isLocal = isLocalInstance(InetAddress.getByName(endURL.getHost())); //"localhost" for localhost
        }catch(UnknownHostException unknownHost) {
            isLocal = false;
        } catch (MalformedURLException ex) {
            isLocal = false;
        }
        
        return isLocal;
    }
}
