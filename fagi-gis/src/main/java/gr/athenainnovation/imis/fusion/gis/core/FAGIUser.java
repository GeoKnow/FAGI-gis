/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.core;

/**
 *
 * @author nick
 */
public class FAGIUser {
    String name;
    String pass;
    String mail;

    public FAGIUser() {
        this.name = null;
        this.pass = null;
        this.mail = null;
    }
    
    public FAGIUser(String name, String pass, String mail) {
        this.name = name;
        this.pass = pass;
        this.mail = mail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }
    
}
