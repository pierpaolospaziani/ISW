package com.mycompany.app.model;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

public class Release {
    private final String name;
    private final LocalDateTime date;
    private final List<RevCommit> commits;
    private final List<ClassFile> files;
    private double proportion;

    public Release(String name, LocalDateTime date, Repository repository) throws IOException, GitAPIException {
        this.name       = name;
        this.date       = date;
        this.commits    = retrieveCommits(repository);
        this.files      = retrieveFiles(repository);
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

    /** Recupera la lista di commit relativa alla release */
    public List<RevCommit> retrieveCommits(Repository repository) throws IOException {
        ObjectId releaseCommitId = repository.resolve(this.getName());
        List<RevCommit> commitsList = new ArrayList<>();
        if (releaseCommitId != null) {
            RevWalk revWalk = new RevWalk(repository);
            RevCommit releaseCommit = revWalk.parseCommit(releaseCommitId);
            revWalk.markStart(releaseCommit);
            revWalk.setRevFilter(RevFilter.NO_MERGES);
            for (RevCommit commit : revWalk) {
                commitsList.add(commit);
            }
            revWalk.dispose();
        }
        Collections.reverse(commitsList);
        return commitsList;
    }

    /** Recupera la lista di file presenti nella release */
    private List<ClassFile> retrieveFiles(Repository repository) throws IOException, GitAPIException {
        ArrayList<ClassFile> fileList = new ArrayList<>();
        ObjectId releaseCommitId = repository.resolve(this.name);
        if (releaseCommitId != null) {
            RevWalk revWalk = new RevWalk(repository);
            RevCommit releaseCommit = revWalk.parseCommit(releaseCommitId);
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(releaseCommit.getTree());
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                if (!treeWalk.isSubtree() && treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test")) {
                    ClassFile classFile = new ClassFile(treeWalk.getPathString());
                    fileList.add(classFile);
                    classFile.setLOCs(countLOCs(releaseCommit, classFile, repository));
                    classFile.setnAuth(countAuthorsInFile(classFile.getPath(), releaseCommitId, repository));
                }
            }
        }
        return fileList;
    }

    /** Conta le LOC del file specificato nella release */
    private int countLOCs(RevCommit releaseCommit, ClassFile classFile, Repository repository) throws IOException {
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(releaseCommit.getTree());
        treeWalk.setFilter(PathFilter.create(classFile.getPath()));
        treeWalk.setRecursive(true);
        treeWalk.next();
        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(objectId);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(loader.openStream()))) {
            int lines = 0;
            String line;
            while ((line = reader.readLine()) != null){
                if (!line.trim().isEmpty()) {
                    lines++;
                }
            }
            return lines;
        }
    }

    /** Conta il numero di autori del file specificato nella release */
    private int countAuthorsInFile(String filepath, ObjectId releaseCommitId, Repository repository) throws GitAPIException {
        BlameResult blameResult = new Git(repository).blame()
            .setFilePath(filepath)
            .setStartCommit(releaseCommitId)
            .call();
        Set<String> authors = new HashSet<>();
        for (int i = 0; i < blameResult.getResultContents().size(); i++) {
            authors.add(blameResult.getSourceAuthor(i).getName());
        }
        return authors.size();
    }
}
