package com.mycompany.app;

public class ClassFile {
    private final String path;
    private Integer commitsNumbers;
    private Integer locs;
    private Integer touchedLOCs;         /* between two release:  added + deleted */
    private Integer churn;               /* between two release: |added - deleted| -> questo può essere fatto facendo la differenza tra i LOC delle release da verificare*/
    private Integer nAuth;
    private Integer averageChangeSet;
    private Integer numberOfBugFix;

    public ClassFile(String path) {
        this.path             = path;
        this.commitsNumbers   = 0;    // numero di commit relativo ad ogni release
        this.locs             = 0;    // numero di LOC per release
        this.touchedLOCs      = 0;    // numero di LOC Touched per release
        this.churn            = 0;    // numero di churn per release
        this.nAuth            = 0;    // numero di autori globale
        this.averageChangeSet = 0;    // numero medio di file cambiati insieme alla classe, per release
        this.numberOfBugFix   = 0;    // numero di bug fix sulla classe, per release
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
        return locs;
    }

    public void setLOCs(Integer locs) {
        this.locs = locs;
    }

    public Integer getTouchedLOCs() {
        return touchedLOCs;
    }

    public void incrementTouchedLOCs(Integer touchedLOCs) {
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

    public Integer getAverageChangeSet() {
        return averageChangeSet;
    }

    public void incrementAverageChangeSet(Integer nFilesChanged) {
        this.averageChangeSet += nFilesChanged;
    }

    public Integer getNumberOfBugFix() {
        return numberOfBugFix;
    }

    public void incrementNumberOfBugFix() {
        this.numberOfBugFix += 1;
    }
}
