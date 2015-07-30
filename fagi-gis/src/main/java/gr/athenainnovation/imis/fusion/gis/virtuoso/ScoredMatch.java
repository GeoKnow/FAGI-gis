/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.virtuoso;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author nick
 */
public class ScoredMatch {
    String rep;
    //String shortRep;
    float score;

    public ScoredMatch(String rep, float score) {
        this.rep = rep;
        /*StringBuilder sb = new StringBuilder();
        String[] tokens = StringUtils.split(rep, ",");
        for ( String token : tokens ) {
            String main = StringUtils.substringAfter(token, "#");
            if (main.equals("") ) {
                main = StringUtils.substring(token, StringUtils.lastIndexOf(token, "/")+1);
            }
            sb.append(main+",");
        }
        int newLength = sb.length() - 1;
        sb.setLength(newLength);
        this.shortRep = sb.toString();*/
        this.score = score;
    }

    public ScoredMatch() {
        rep = "";
        score = 0.0f;
    }

    public String getRep() {
        return rep;
    }

    public void setRep(String rep) {
        this.rep = rep;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.rep);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ScoredMatch other = (ScoredMatch) obj;
        if (!Objects.equals(this.rep, other.rep)) {
            return false;
        }
        return true;
    }
    
    
}
