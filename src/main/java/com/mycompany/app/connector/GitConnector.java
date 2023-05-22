package com.mycompany.app.connector;

import com.mycompany.app.RetrieveDataset;
import com.mycompany.app.model.ClassFile;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GitConnector {

    private GitConnector() {
        throw new IllegalStateException("Utility class");
    }

    /** Recupera la lista di commit relativa alla release */
    public static List<RevCommit> retrieveCommits(Repository repository, String releaseName) throws IOException {
        ObjectId releaseCommitId = repository.resolve(releaseName);
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
    public static List<ClassFile> retrieveFiles(Repository repository, String releaseName) throws IOException, GitAPIException {
        ArrayList<ClassFile> fileList = new ArrayList<>();
        ObjectId releaseCommitId = repository.resolve(releaseName);
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
                    classFile.setLOCs(RetrieveDataset.countLOCs(releaseCommit, classFile, repository));
                    classFile.setnAuth(RetrieveDataset.countAuthorsInFile(classFile.getPath(), releaseCommitId, repository));
                }
            }
        }
        return fileList;
    }
}
