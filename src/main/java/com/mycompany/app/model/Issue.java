package com.mycompany.app.model;

public class Issue {
    private final String key;
    private Integer injectedVersion;
    private Integer fixedVersion;
    private Integer openingVersion;

    public Issue(String key){
        this.key             = key;
        this.injectedVersion = null;
        this.fixedVersion    = null;
        this.openingVersion  = null;
    }

    public String getKey() {
        return key;
    }

    public Integer getInjectedVersion() {
        return injectedVersion;
    }

    public void setInjectedVersion(Integer injectedVersion) {
        this.injectedVersion = injectedVersion;
    }

    public Integer getFixedVersion() {
        return fixedVersion;
    }

    public void setFixedVersion(Integer fixedVersion) {
        this.fixedVersion = fixedVersion;
    }

    public Integer getOpeningVersion() {
        return openingVersion;
    }

    public void setOpeningVersion(Integer openingVersion) {
        this.openingVersion = openingVersion;
    }
}
