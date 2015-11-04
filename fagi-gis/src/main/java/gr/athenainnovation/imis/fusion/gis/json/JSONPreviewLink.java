/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

import java.io.IOException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 *
 * @author Nick Vitsas
 */
public class JSONPreviewLink {
    
    
    private class StringBuilderToStringSerializer extends JsonSerializer<StringBuilder> {

        @Override
        public void serialize(StringBuilder t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            t.setLength(t.length()- 1);
            jg.writeObject(t.toString());
        }

    }
    
    @JsonSerialize(using = StringBuilderToStringSerializer.class, as = String.class)
    StringBuilder           geomA;
    String                  subA;
    
    @JsonSerialize(using = StringBuilderToStringSerializer.class, as = String.class)
    StringBuilder           geomB;
    String                  subB;

    public JSONPreviewLink(String geomA, String subA, String geomB, String subB) {
        this.geomA = new StringBuilder(geomA);
        this.subA = subA;
        this.geomB = new StringBuilder(geomB);
        this.subB = subB;
    }
    
}
