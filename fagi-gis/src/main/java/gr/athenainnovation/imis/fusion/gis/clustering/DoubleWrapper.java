/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.clustering;

import java.util.Comparator;

/**
 *
 * @author Nick Vitsas
 */
public class DoubleWrapper implements Comparator<DoubleWrapper>, Comparable<DoubleWrapper> {

    Double val;

    public DoubleWrapper() {
        this.val = 0.0;
    }

    public DoubleWrapper(Double val) {
        this.val = val;
    }

    public Double getVal() {
        return val;
    }

    public void setVal(Double val) {
        this.val = val;
    }

    @Override
    public int compare(DoubleWrapper o1, DoubleWrapper o2) {
        return o1.getVal().compareTo(o2.getVal());
    }

    @Override
    public int compareTo(DoubleWrapper o) {
        return (this.val).compareTo(o.val);
    }

}
