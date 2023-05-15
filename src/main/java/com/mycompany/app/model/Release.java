package com.mycompany.app.model;

import com.mycompany.app.model.ClassFile;
import org.eclipse.jgit.api.Git;
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
import java.util.*;

public class Release {
    private final String name;
    private final List<RevCommit> commits;
    private final List<ClassFile> files;

    public Release(String name, Repository repository){
        this.name    = name;
        this.commits = retrieveCommits(repository);
        this.files   = retrieveFiles(repository);
    }

    public String getName() {
        return this.name;
    }

    public List<RevCommit> getCommits() {
        return this.commits;
    }

    public List<ClassFile> getFiles() {
        return this.files;
    }

    /** Recupera la lista di commit relativa alla release */
    public List<RevCommit> retrieveCommits(Repository repository){
        try {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Recupera la lista di file presenti nella release */
    private List<ClassFile> retrieveFiles(Repository repository){
        try {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
    private int countAuthorsInFile(String filepath, ObjectId releaseCommitId, Repository repository){
        try {
            BlameResult blameResult = new Git(repository).blame()
                .setFilePath(filepath)
                .setStartCommit(releaseCommitId)
                .call();
            Set<String> authors = new HashSet<>();
            for (int i = 0; i < blameResult.getResultContents().size(); i++) {
                authors.add(blameResult.getSourceAuthor(i).getName());
            }
            return authors.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}