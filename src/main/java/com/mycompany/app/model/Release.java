package com.mycompany.app.model;

import com.mycompany.app.connector.GitConnector;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class Release {
    private final String name;
    private final LocalDateTime date;
    private List<RevCommit> commits;
    private List<ClassFile> files;
    private double proportion;

    public Release(String name, LocalDateTime date) {
        this.name       = name;
        this.date       = date;
        this.proportion = 0.0;
    }

    public String getName() {
        return this.name;
    }

    public LocalDateTime getDate() {
        return this.date;
    }

    public List<RevCommit> getCommits() {
        return this.commits;
    }

    public List<ClassFile> getFiles() {
        return this.files;
    }

    public double getProportion() {
        return proportion;
    }

    public void setProportion(double proportion) {
        this.proportion = proportion;
    }

    public void retrieveCommits(Repository repository) throws IOException {
        this.commits = GitConnector.retrieveCommits(repository, this.name);
    }

    public void retrieveFiles(Repository repository) throws GitAPIException, IOException {
        this.files = GitConnector.retrieveFiles(repository, this.name);
    }
}
