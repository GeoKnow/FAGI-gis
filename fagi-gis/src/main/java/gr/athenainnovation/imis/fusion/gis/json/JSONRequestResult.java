/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.athenainnovation.imis.fusion.gis.json;

/**
 * Used as return status report for Requests
 * @author nick
 */
class JSONRequestResult {

    // -1 error 0 success
    int             statusCode;
    String          message;

    public JSONRequestResult() {
        this.statusCode = -1;
        this.message = "general error";
    }

    public JSONRequestResult(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
