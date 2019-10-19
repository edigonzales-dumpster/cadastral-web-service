package ch.so.agi.cis;

import org.locationtech.jts.geom.Geometry;

public class Grundstueck {
    private String nummer;
    private String nbident;
    private String egrid;
    private String art;
    private double flaechenmass;
    private Geometry geometrie;
    private String gbSubKreis;
    private int bfsNr;
    
    public String getNummer() {
        return nummer;
    }
    public void setNummer(String nummer) {
        this.nummer = nummer;
    }
    public String getNbident() {
        return nbident;
    }
    public void setNbident(String nbident) {
        this.nbident = nbident;
    }
    public String getEgrid() {
        return egrid;
    }
    public void setEgrid(String egrid) {
        this.egrid = egrid;
    }
    public String getArt() {
        return art;
    }
    public void setArt(String art) {
        this.art = art;
    }
    public double getFlaechenmass() {
        return flaechenmass;
    }
    public void setFlaechenmass(double flaechenmass) {
        this.flaechenmass = flaechenmass;
    }
    public Geometry getGeometrie() {
        return geometrie;
    }
    public void setGeometrie(Geometry geometrie) {
        this.geometrie = geometrie;
    }
    public String getGbSubKreis() {
        return gbSubKreis;
    }
    public void setGbSubKreis(String gbSubKreis) {
        this.gbSubKreis = gbSubKreis;
    }
    public int getBfsNr() {
        return bfsNr;
    }
    public void setBfsNr(int bfsNr) {
        this.bfsNr = bfsNr;
    }

}
