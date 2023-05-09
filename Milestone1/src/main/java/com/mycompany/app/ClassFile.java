package com.mycompany.app;

import java.util.ArrayList;

public class ClassFile {
    private String name;
    private String path;
    private Boolean isDeleted;
    private ArrayList<Integer> commitsNumbers;
    private ArrayList<Integer> releases;
    private ArrayList<String> releasesNames;
    private ArrayList<Integer> LOCs;
    private ArrayList<Integer> touchedLOCs;         /* between two release:  added + deleted */
    private ArrayList<Integer> churn;               /* between two release: |added - deleted| -> questo può essere fatto facendo la differenza tra i LOC delle release da verificare*/
    private int revisionFirstAppearance;
    private ArrayList<Integer> nAuth;
    private ArrayList<Integer> revisions;
    private ArrayList<Integer> nFilesChanged;
    private ArrayList<Integer> numberOfBugFix;

    public ClassFile(String name, String path) {
        this.name           = name;
        this.path           = path;
        this.isDeleted      = false;
        this.commitsNumbers = new ArrayList<>();    // array conn il numero di commit relativo ad ogni release
        this.releases       = new ArrayList<>();    // array con il numero di release in cui appare la classe
        this.releasesNames  = new ArrayList<>();    // array con i nomi delle release in cui appare la classe
        this.LOCs           = new ArrayList<>();    // array con il numero di LOC per release
        this.touchedLOCs    = new ArrayList<>();    // array con il numero di LOC Touched per release
        this.churn          = new ArrayList<>();    // array con il numero di churn per release
        this.nAuth          = new ArrayList<>();    // array con il numero di autori globale
//        this.revisions      = new ArrayList<>();
        this.nFilesChanged  = new ArrayList<>();    // array con il numero medio di file cambiati insieme alla classe, per release
        this.numberOfBugFix = new ArrayList<>();    // array con il numero di bug fix sulla classe, per release
    }

    public String getName() {
        return this.name;
    }

    public void insertRelease(Integer release, String releaseName) {
        if (!this.releases.contains(release)){
            this.releases.add(release);
            this.insertReleaseName(releaseName);
        }
    }

    public ArrayList<Integer> getReleases() {
        return this.releases;
    }

    public void insertReleaseName(String releaseName) {
        this.releasesNames.add(releaseName);
    }

    public ArrayList<String> getReleasesNames() {
        return this.releasesNames;
    }

    public String getPaths() {
        return this.path;
    }

    public void insertLOCs(Integer newLOCs,Integer releaseNumber) {
        if (releaseNumber > this.LOCs.size()){
            this.LOCs.add(newLOCs);
        } else {
            Integer oldLOCs = this.getLOCs().get(releaseNumber-1);
            this.LOCs.remove(this.LOCs.size()-1);
            this.LOCs.add(oldLOCs + newLOCs);
        }
    }

    public ArrayList<Integer> getLOCs() {
        return this.LOCs;
    }

    public void insertTouchedLOCs(Integer t) {
        this.touchedLOCs.add(t);
    }

    public ArrayList<Integer> getTouchedLOCs() {
        return this.touchedLOCs;
    }

    public void insertChurn(Integer newChurn, Integer releaseNumber) {
        if (releaseNumber > this.churn.size()){
            this.churn.add(newChurn);
        } else {
            Integer oldChurn = this.getChurn().get(releaseNumber-1);
            this.churn.remove(this.churn.size()-1);
            this.churn.add(oldChurn + newChurn);
        }
    }

    public ArrayList<Integer> getChurn() {
        return this.churn;
    }

    public void setRevisionFirstAppearance(int revisionFirstAppearance) {
        this.revisionFirstAppearance = revisionFirstAppearance;
    }

    public int getRevisionFirstAppearance() {
        return this.revisionFirstAppearance;
    }

    public void insertAuth(int nauth) {
        this.nAuth.add(nauth);
    }

    public ArrayList<Integer> getnAuth() {
        return this.nAuth;
    }

    public void insertRevisions(Integer revision) {
        this.revisions.add(revision);
    }

    public ArrayList<Integer> getRevisions() {
        return this.revisions;
    }

    public ArrayList<Integer> getCommitsNumbers() {
        return this.commitsNumbers;
    }

    public void incrementCommitsNumbers(Integer releaseNumber) {
        if (this.commitsNumbers.size() == 0) {
            this.commitsNumbers.add(1);
        } else if (this.commitsNumbers.size() < this.releases.size()){
            this.commitsNumbers.add(1);
        } else {
            Integer value = this.getCommitsNumbers().get(this.commitsNumbers.size()-1);
            this.commitsNumbers.remove(this.commitsNumbers.size()-1);
            this.commitsNumbers.add(value + 1);
        }
    }

//    public void decAppearances() {
//        this.appearances -= 1;
//    }

    public void insertChangedSetSize(Integer changeSetSize) {
        this.nFilesChanged.add(changeSetSize);
    }

    public ArrayList<Integer> getNFilesChanged() {
        return this.nFilesChanged;
    }

    public ArrayList<Integer> getNumberOfBugFix() {
        return this.numberOfBugFix;
    }

    public void setNumberOfBugFix(Integer value, Integer releaseIndex) {
        if (releaseIndex > this.numberOfBugFix.size()){
            this.numberOfBugFix.add(value);
        } else {
            this.numberOfBugFix.remove(this.numberOfBugFix.size()-1);
            this.numberOfBugFix.add(value);
        }
    }

    public Boolean getDeleted() {
        return this.isDeleted;
    }

    public void setDeleted() {
        this.isDeleted = true;
    }
}
