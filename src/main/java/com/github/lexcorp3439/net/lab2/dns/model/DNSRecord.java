package com.github.lexcorp3439.net.lab2.dns.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DNSRecord {
    private int timeToLive, rdLength, mxPreference;
    private String name, domain;
    private byte[] queryClass;
    private QueryType queryType;
    private boolean auth;
    private int byteLength;

    public DNSRecord(boolean auth) {
        this.auth = auth;
    }

    public void outputRecord() {
        switch (this.queryType) {
            case A:
                this.outputATypeRecords();
                break;
            case NS:
                this.outputNSTypeRecords();
                break;
            case MX:
                this.outputMXTypeRecords();
                break;
            case CNAME:
                this.outputCNameTypeRecords();
                break;
            default:
                break;
        }
    }

    private void outputATypeRecords() {
        String authString = this.auth ? "auth" : "nonauth";
        System.out.println("IP\t" + this.domain + "\t" + this.timeToLive + "\t" + authString);
    }

    private void outputNSTypeRecords() {
        String authString = this.auth ? "auth" : "nonauth";
        System.out.println("NS\t" + this.domain + "\t" + this.timeToLive + "\t" + authString);
    }

    private void outputMXTypeRecords() {
        String authString = this.auth ? "auth" : "nonauth";
        System.out.println("MX\t" + this.domain + "\t" + mxPreference + "\t" + this.timeToLive + "\t" + authString);
    }

    private void outputCNameTypeRecords() {
        String authString = this.auth ? "auth" : "nonauth";
        System.out.println("CNAME\t" + this.domain + "\t" + this.timeToLive + "\t" + authString);
    }

}