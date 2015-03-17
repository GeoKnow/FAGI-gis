
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import java.util.concurrent.Callable;

/**
 *
 * @author imis-nkarag
 */
public class Reader implements Callable  <  Void  > {
  
private String query = null;
private final String endpointA;

    public Reader(String query, String endpointA) {
        this.query = query; 
        this.endpointA = endpointA;
    }

    @Override
    public Void call() {
        try{ 
            UpdateRequest insertFromA = UpdateFactory.create(query);
            UpdateProcessor insertRemoteA = UpdateExecutionFactory.createRemoteForm(insertFromA, endpointA);
            insertRemoteA.execute();
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
        return null;
    }
} 
