package com.mycompany.app;

import com.mycompany.app.model.ClassFile;
import com.mycompany.app.model.Issue;
import com.mycompany.app.model.Release;
import com.mycompany.app.utils.IO;
import com.mycompany.app.utils.Initializer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.*;
import java.util.*;

import static com.mycompany.app.connector.JiraReleases.*;
import static com.mycompany.app.connector.JiraIssues.retrieveIssues;

public class RetrieveDataset {
    private static List<Issue> issueList = null;
    private static List<Release> releaseList = null;
    private static Repository repository = null;
    private static Git gitRepository = null;


    /** Ritorna l'age del file */
    public static int countAge(Map<String, Integer> hashMap, ClassFile file){
        int age = 0;
        int index = 0;
        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            if (entry.getKey().equals(file.getPath())){
                age = entry.getValue();
                entry.setValue(age+1);
                break;
            } else if (index == hashMap.size() - 1){
                hashMap.put(file.getPath(), age);
            }
            index++;
        }
        return age;
    }


    /** Conta le LOC del file specificato nella release */
    public static int countLOCs(RevCommit releaseCommit, ClassFile classFile, Repository repository) throws IOException {
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
    public static int countAuthorsInFile(String filepath, ObjectId releaseCommitId, Repository repository) throws GitAPIException {
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


    /** Ritorna il numero di Churn sul file nel commit |added - deleted| */
    public static int countChurn(RevTree newTree, RevTree oldTree, String filepath) throws IOException {
        TreeWalk tw = new TreeWalk(repository);
        tw.addTree(newTree);
        if (oldTree != null) tw.addTree(oldTree);
        tw.setRecursive(true);
        tw.setFilter(PathFilter.create(filepath));
        tw.next();
        int currLines = countLines(repository.open(tw.getObjectId(0)).openStream());
        int prevLines = 0;
        if (oldTree != null && tw.getFileMode(1)!= FileMode.MISSING) prevLines = countLines(repository.open(tw.getObjectId(1)).openStream());
        return Math.abs(currLines - prevLines);
    }


    /** Ritorna il numero di LOC Touched sul file nel commit (added + deleted) */
    public static int countLOCTouched(DiffEntry diff) throws IOException {
        DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        formatter.setRepository(repository);
        formatter.setContext(0);
        FileHeader fileHeader = formatter.toFileHeader(diff);
        List<? extends HunkHeader> hunks = fileHeader.getHunks();
        int addedLines = 0;
        int removedLines = 0;
        for (HunkHeader hunk : hunks) {
            for (Edit edit : hunk.toEditList()) {
                addedLines   += edit.getLengthB();
                removedLines += edit.getLengthA();
            }
        }
        return addedLines + removedLines;
    }


    /** Ritorna il numero di LOC aggiunte sul file nel commit */
    public static int countAddedLOCs(DiffEntry diff) throws IOException {
        DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        formatter.setRepository(repository);
        formatter.setContext(0);
        FileHeader fileHeader = formatter.toFileHeader(diff);
        List<? extends HunkHeader> hunks = fileHeader.getHunks();
        int addedLines = 0;
        for (HunkHeader hunk : hunks) {
            for (Edit edit : hunk.toEditList()) {
                addedLines += edit.getLengthB();
            }
        }
        return addedLines;
    }


    /** Conta le linee di codice presenti nel file (righe vuote escluse) */
    private static int countLines(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
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


    /** Se il commit riguarda un bugfix, ritorna la relativa issue, altrimenti null */
    public static Issue bugFixIssue(RevCommit commit) {
        String message = commit.getShortMessage().replace("  ", "");
        if (message.startsWith("BOOKKEEPER-") || message.startsWith("OPENJPA-")){
            for (Issue issue : issueList){
                if (issue.getKey().contains(message.split(":")[0])){
                    return issue;
                }
            }
        }
        return null;
    }


    /** Setta il file come buggy per tutte le affected versions */
    public static void setBugginess(Issue issue, String filepath){
        if (issue.getInjectedVersion() != null && issue.getFixedVersion() != null && issue.getFixedVersion() <= releaseList.size()){
            for (int i = issue.getInjectedVersion(); i < issue.getFixedVersion(); i++){
                List<ClassFile> files = releaseList.get(i).getFiles();
                for (ClassFile file : files){
                    if (file.getPath().equals(filepath)){
                        file.setBuggy();
                        break;
                    }
                }
            }
        }
    }


    /** GIT: costruisce un ArrayList<ArrayList<RevCommit>> 'releaseCommits' che contiene l'array di commit divisi per release */
    public static List<ArrayList<RevCommit>> getCommitsPerRelease(){
        ArrayList<ArrayList<RevCommit>> releaseCommits = new ArrayList<>();
        ArrayList<RevCommit> commits = new ArrayList<>();
        for (Release release : releaseList) {
            ArrayList<RevCommit> newCommits = new ArrayList<>();
            for (RevCommit c : release.getCommits()){
                if (!commits.contains(c)){
                    commits.add(c);
                    newCommits.add(c);
                }
            }
            releaseCommits.add(newCommits);
        }
        return releaseCommits;
    }


    /** Controlla se il file è un .java, non test e ritorna il suo indice nella release */
    public static int isGoodFile(String filepath,  int releaseNumber){
        if (filepath.contains(".java") && !filepath.contains("/test")){
            for (ClassFile classFile : releaseList.get(releaseNumber - 1).getFiles()){
                if (classFile.getPath().equals(filepath)) return releaseList.get(releaseNumber - 1).getFiles().indexOf(classFile);
            }
        }
        return -1;
    }


    /** Azioni da eseguire se il commit analizzato è il primo e quindi non ha parent */
    public static void firstCommitCase(RevTree tree, RevCommit commit, int releaseNumber) throws IOException {
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        ClassFile classFile = null;
        int fileNumber = 0;
        while (treeWalk.next()) {
            String filePath = treeWalk.getPathString();
            int index = isGoodFile(filePath,releaseNumber);
            if (index != -1){
                fileNumber += 1;
                classFile = releaseList.get(releaseNumber-1).getFiles().get(index);
                classFile.incrementCommitsNumbers();
                classFile.incrementTouchedLOCs(countLines(repository.open(treeWalk.getObjectId(0)).openStream()));
                classFile.increaseChurn(countChurn(tree, null, filePath));
                Issue issue = bugFixIssue(commit);
                if (issue != null){
                    classFile.incrementNumberOfBugFix();
                    setBugginess(issue, filePath);
                }
                classFile.incrementAddedLOCs(countLines(repository.open(treeWalk.getObjectId(0)).openStream()));
            }
        }
        if (fileNumber != 0) classFile.incrementAverageChangeSet(fileNumber);
    }


    /** Azioni da eseguire per tutti i commit tranne il primo */
    public static void diffCommitCase(RevWalk revWalk, RevCommit commit, RevTree tree, int releaseNumber) throws IOException, GitAPIException {
        RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0).getId());
        RevTree oldTree = parentCommit.getTree();
        CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
        newTreeParser.reset(repository.newObjectReader(), tree.getId());
        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
        oldTreeParser.reset(repository.newObjectReader(), oldTree.getId());
        List<DiffEntry> diffs = gitRepository.diff()
                .setNewTree(newTreeParser)
                .setOldTree(oldTreeParser)
                .call();
        for (DiffEntry diff : diffs) {
            String filePath = diff.getNewPath();
            int index = isGoodFile(filePath,releaseNumber);
            if (index != -1){
                ClassFile classFile = releaseList.get(releaseNumber-1).getFiles().get(index);
                classFile.incrementCommitsNumbers();
                classFile.incrementTouchedLOCs(countLOCTouched(diff));
                classFile.increaseChurn(countChurn(tree, oldTree, filePath));
                Issue issue = bugFixIssue(commit);
                if (issue != null){
                    classFile.incrementNumberOfBugFix();
                    setBugginess(issue, filePath);
                }
                classFile.incrementAverageChangeSet(diffs.size());
                classFile.incrementAddedLOCs(countAddedLOCs(diff));
            }
        }
    }


    /** Calcola le metriche per ogni commit di ogni release */
    public static void computeMetrics(List<ArrayList<RevCommit>> releaseCommits) throws IOException, GitAPIException {
        for (ArrayList<RevCommit> commitsPerRelease : releaseCommits){
            int releaseNumber = releaseCommits.indexOf(commitsPerRelease) + 1;
            for (RevCommit commit : commitsPerRelease){
                RevTree tree = commit.getTree();
                RevWalk revWalk = new RevWalk(repository);
                // caso speciale per il primo commit che non ha parent
                if (commit.getParentCount() == 0) {
                    firstCommitCase(tree, commit, releaseNumber);
                } else {
                    diffCommitCase(revWalk, commit, tree, releaseNumber);
                }
            }
        }
    }


    /** Calcola e setta il coefficiente di proportion per ogni release */
    public static void computeProportion(int i){
        double p = 0;
        int howMany = 0;
        for (Issue issue : issueList){
            if (issue.getInjectedVersion() != null
                    && issue.getOpeningVersion() != null
                    && issue.getFixedVersion() != null
                    && issue.getFixedVersion() == i
                    && issue.getInjectedVersion() <= issue.getOpeningVersion()
                    && issue.getOpeningVersion() <= issue.getFixedVersion()){
                howMany += 1;
                if (Objects.equals(issue.getFixedVersion(), issue.getOpeningVersion())) {
                    p += issue.getFixedVersion() - issue.getInjectedVersion();
                } else {
                    p += (double) (issue.getFixedVersion() - issue.getInjectedVersion()) / (issue.getFixedVersion() - issue.getOpeningVersion());
                }
            }
        }
        if (howMany != 0 && p != 0) {
            releaseList.get(i).setProportion(p/howMany);
        } else {
            releaseList.get(i).setProportion(1);
        }
    }


    /** Usa il coefficiente di proportion per calcolare l'injected version per i ticket che ne sono privi */
    public static void incrementalProportion(){
        for (Issue issue : issueList){
            if (issue.getInjectedVersion() == null
                    && issue.getOpeningVersion() != null
                    && issue.getFixedVersion() != null
                    && issue.getOpeningVersion() <= issue.getFixedVersion()
                    && issue.getFixedVersion() <= releaseList.size()){
                double predictedIV = 0;
                int i;
                for (i = 0; i < issue.getFixedVersion(); i++) {
                    predictedIV += issue.getFixedVersion() - (issue.getFixedVersion() - issue.getOpeningVersion()) * releaseList.get(i).getProportion();
                }
                predictedIV = predictedIV / (i + 1);
                issue.setInjectedVersion((int) Math.round(predictedIV));
            }
        }
    }


    public static void main(String[] args) throws Exception {
        try {
            Initializer.getInstance();

            List<String> projects = Initializer.getProjectNames();

            for (String projectName : projects) {

                IO.appendOnLog(projectName+"\n");

                String repoPath = Initializer.getRepoPath().get(projects.indexOf(projectName));

                FileRepositoryBuilder builder = new FileRepositoryBuilder();
                repository = builder
                        .setGitDir(new File(repoPath)).readEnvironment()
                        .findGitDir().build();

                gitRepository = new Git(repository);

                // JIRA: prendo la lista di release (ordinata)
                releaseList = retrieveReleases(repository, projectName);
                // JIRA: prendo la lista di issue bug fix
                issueList = retrieveIssues(projectName, releaseList);

                if (releaseList.size()%2 == 0)
                    releaseList = releaseList.subList(0, releaseList.size()/2);
                else
                    releaseList = releaseList.subList(0, (releaseList.size()+1)/2);

                for (Release release : releaseList) {
                    release.retrieveCommits(repository);
                    release.retrieveFiles(repository);
                }

                // Calcola e setta il coefficiente di proportion per ogni release
                for (int i = 0; i < releaseList.size(); i++) {
                    computeProportion(i);
                }
                // Calcola l'injected version per i ticket che ne sono privi
                incrementalProportion();

                // GIT: costruisce un ArrayList<ArrayList<RevCommit>> 'releaseCommits' che contiene l'array di commit divisi per release
                List<ArrayList<RevCommit>> releaseCommits = getCommitsPerRelease();

                computeMetrics(releaseCommits);

                IO.writeOnFile(projectName, releaseList);

                repository.close();
            }
        } catch (Exception e){
            IO.appendOnLog(e+"\n");
        }
    }
}
