package com.mycompany.app;

import com.mycompany.app.model.ClassFile;
import com.mycompany.app.model.Release;
import com.mycompany.app.utils.IO;
import com.mycompany.app.utils.Initializer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

import static com.mycompany.app.utils.GetReleaseInfo.*;

public class FilesRet {
    public static String REPO_PATH           = null;
    public static String PROJ_NAME           = null;
    private static List<String> issueList    = null;
    private static List<Release> releaseList = null;
    private static Repository repository     = null;
    private static Git gitRepository         = null;


    /** Esegue la scrittura delle metriche sul csv */
    public static void writeOnFile() {
        try (FileWriter fileWriter = new FileWriter(PROJ_NAME + ".csv")) {
            fileWriter.append("Version, Version Name, Name, Age, Revisions, Bugfix, LOCs, LOCs Touched, LOCs Added, Churn, Avg. Churn, Authors Number, Average Change Set\n");

            // hashMap per il conteggio dell'Age
            HashMap<String, Integer> hashMap = new HashMap<>();
            for (ClassFile file : releaseList.get(0).getFiles()) {
                hashMap.put(file.getPath(), 0);
            }

            for (Release release : releaseList) {
                for (ClassFile file : release.getFiles()) {

                    int releaseNumber = releaseList.indexOf(release);
                    fileWriter.append(Integer.toString(releaseNumber+1));

                    fileWriter.append(",");
                    fileWriter.append(release.getName());

                    fileWriter.append(",");
                    fileWriter.append(file.getPath());

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
                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(age));

                    fileWriter.append(",");
                    fileWriter.append(file.getCommitsNumbers().toString());

                    fileWriter.append(",");
                    fileWriter.append((file.getNumberOfBugFix().toString()));

                    fileWriter.append(",");
                    fileWriter.append(file.getLOCs().toString());

                    fileWriter.append(",");
                    fileWriter.append(file.getTouchedLOCs().toString());

                    fileWriter.append(",");
                    fileWriter.append(file.getAddedLOCs().toString());

                    fileWriter.append(",");
                    fileWriter.append(file.getChurn().toString());

                    fileWriter.append(",");
                    if (file.getCommitsNumbers() != 0){
                        fileWriter.append(String.valueOf(file.getChurn()/file.getCommitsNumbers()));
                    } else {
                        fileWriter.append(file.getChurn().toString());
                    }

                    fileWriter.append(",");
                    fileWriter.append((file.getnAuth().toString()));

                    fileWriter.append(",");
                    if (file.getCommitsNumbers() != 0){
                        fileWriter.append(String.valueOf(file.getAverageChangeSet()/file.getCommitsNumbers()));
                    } else {
                        fileWriter.append((file.getAverageChangeSet().toString()));
                    }

                    fileWriter.append("\n");
                }
            }
            fileWriter.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /** Ritorna il numero di Churn sul file nel commit |added - deleted| */
    public static int countChurn(RevTree newTree, RevTree oldTree, String filepath) {
        try{
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
        } catch (Exception e){
            throw new RuntimeException(e);
        }
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


    /** Ritorna un booleano che indica se il commit riguarda un bugfix */
    public static Boolean isBufFix(RevCommit commit) {
        String message = commit.getShortMessage().replace("  ", "");
        return message.startsWith("BOOKKEEPER-") && issueList.contains(message.split(":")[0]);
    }


    /** JIRA: costruisce l'ArrayList che contiene tutti i ticket BUG chiusi e fixati */
    public static List<String> retrieveIssues() throws IOException {
        ArrayList<String> issuesListJira = new ArrayList<>();
        int j;
        int i = 0;
        int total;
        do {
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + PROJ_NAME + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
                    + i + "&maxResults=" + j;
            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            for (; i < total && i < j; i++) {
                String key = issues.getJSONObject(i % 1000).get("key").toString();
                issuesListJira.add(key);
            }
        } while (i < total);
        return issuesListJira;
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
                if (isBufFix(commit)) classFile.incrementNumberOfBugFix();
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
                if (isBufFix(commit)) classFile.incrementNumberOfBugFix();
                classFile.incrementAverageChangeSet(diffs.size());
                classFile.incrementAddedLOCs(countAddedLOCs(diff));
            }
        }
    }


    /** Calcola le metriche per ogni commit di ogni release */
    public static void computeMetrics(List<ArrayList<RevCommit>> releaseCommits){
        for (ArrayList<RevCommit> commitsPerRelease : releaseCommits){
            int releaseNumber = releaseCommits.indexOf(commitsPerRelease) + 1;
            for (RevCommit commit : commitsPerRelease){
                RevTree tree = commit.getTree();
                try (RevWalk revWalk = new RevWalk(repository)) {
                    // caso speciale per il primo commit che non ha parent
                    if (commit.getParentCount() == 0) {
                        firstCommitCase(tree, commit, releaseNumber);
                    } else {
                        diffCommitCase(revWalk, commit, tree, releaseNumber);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    public static void main(String[] args) throws Exception {

        long startTime = System.nanoTime();

        Initializer.getInstance();
        IO.appendOnLog("STARTING SOFTWARE METRICS ANALYZER\n");

        List<String> projects = Initializer.getProjectNames();

        for (String project : projects) {

            REPO_PATH = Initializer.getRepoPath();
            PROJ_NAME = Initializer.getProjectNames().get(project.indexOf(project));

            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            repository = builder
                    .setGitDir(new File(REPO_PATH)).readEnvironment()
                    .findGitDir().build();

            gitRepository = new Git(repository);

            // JIRA: prendo la lista di release (ordinata)
            releaseList = retrieveReleases(repository, PROJ_NAME);
            // JIRA: prendo la lista di issue bug fix
            issueList = retrieveIssues();

            // GIT: costruisce un ArrayList<ArrayList<RevCommit>> 'releaseCommits' che contiene l'array di commit divisi per release
            List<ArrayList<RevCommit>> releaseCommits = getCommitsPerRelease();

            computeMetrics(releaseCommits);

            writeOnFile();

            repository.close();
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);

        System.out.println("Tempo di esecuzione: " + (float) duration/1000000000 + " secondi");
    }
}
