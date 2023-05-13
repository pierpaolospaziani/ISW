package com.mycompany.app;

import java.util.ArrayList;

public class ClassFile {
    private String path;
    private Integer commitsNumbers;
    private Integer LOCs;
    private Integer touchedLOCs;         /* between two release:  added + deleted */
    private Integer churn;               /* between two release: |added - deleted| -> questo può essere fatto facendo la differenza tra i LOC delle release da verificare*/
    private Integer nAuth;
    private Integer nFilesChanged;
    private Integer numberOfBugFix;

    public ClassFile(String path) {
        this.path           = path;
        this.commitsNumbers = 0;    // numero di commit relativo ad ogni release
        this.LOCs           = 0;    // numero di LOC per release
        this.touchedLOCs    = 0;    // numero di LOC Touched per release
        this.churn          = 0;    // numero di churn per release
        this.nAuth          = 0;    // numero di autori globale
        this.nFilesChanged  = 0;    // numero medio di file cambiati insieme alla classe, per release
        this.numberOfBugFix = 0;    // numero di bug fix sulla classe, per release
    }

    public String getPath() {
        return path;
    }

    public Integer getCommitsNumbers() {
        return commitsNumbers;
    }

    public void incrementCommitsNumbers() {
        this.commitsNumbers += 1;
    }

    public Integer getLOCs() {
        return LOCs;
    }

    public void setLOCs(Integer LOCs) {
        this.LOCs = LOCs;
    }

    public Integer getTouchedLOCs() {
        return touchedLOCs;
    }

    public void increaseTouchedLOCs(Integer touchedLOCs) {
        this.touchedLOCs += touchedLOCs;
    }

    public Integer getChurn() {
        return churn;
    }

    public void increaseChurn(Integer churn) {
        this.churn += churn;
    }

    public Integer getnAuth() {
        return nAuth;
    }

    public void setnAuth(Integer nAuth) {
        this.nAuth = nAuth;
    }

    public Integer getnFilesChanged() {
        return nFilesChanged;
    }

    public void setnFilesChanged(Integer nFilesChanged) {
        this.nFilesChanged = nFilesChanged;
    }

    public Integer getNumberOfBugFix() {
        return numberOfBugFix;
    }

    public void increaseNumberOfBugFix() {
        this.numberOfBugFix += 1;
    }
}
