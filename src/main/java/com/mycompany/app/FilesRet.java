package com.mycompany.app;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

import static com.mycompany.app.getReleaseInfo.*;

public class FilesRet {
    public static final String REPO_PATH = "/Users/pierpaolospaziani/Downloads/bookkeeper/.git";
    public static final String PROJ_NAME = "BOOKKEEPER";
    private static List<String> ISSUE_LIST = null;
    private static List<Release> RELEASE_LIST = null;
    private static Repository REPOSITORY = null;
    private static Git GIT_REPOSITORY = null;

    public static void writeOnFile() throws IOException {
        try (FileWriter fileWriter = new FileWriter(PROJ_NAME + "FilesInfo.csv")) {
            //            fileWriter.append("Version, Version_Name, Name, LOCs, Churn, Age, Number_of_Authors, Number of Revisions, Average Change Set\n");
            fileWriter.append("Version, Version Name, Path, LOCs, LOCs Touched, Churn, Number of Revisions, Authors Number, Bugfix, Average Change Set\n");

            for (Release release : RELEASE_LIST) {
                for (ClassFile file : release.getFiles()) {

                    int releaseNumber = RELEASE_LIST.indexOf(release);
                    fileWriter.append(Integer.toString(releaseNumber+1));

                    fileWriter.append(",");
                    fileWriter.append(release.getName());

                    fileWriter.append(",");
                    fileWriter.append(file.getPath());

                    fileWriter.append(",");
                    fileWriter.append(file.getLOCs().toString());

                    fileWriter.append(",");
                    fileWriter.append(file.getTouchedLOCs().toString());

                    fileWriter.append(",");
                    fileWriter.append(file.getChurn().toString());

//                        fileWriter.append(",");
//                        fileWriter.append((Integer.toString(i - file.getRevisionFirstAppearance() + 1)));

//                    fileWriter.append(",");
//                    fileWriter.append(String.valueOf(index + 1));

                    fileWriter.append(",");
                    fileWriter.append(file.getCommitsNumbers().toString());

                    fileWriter.append(",");
                    fileWriter.append((file.getnAuth().toString()));

                    fileWriter.append(",");
                    fileWriter.append((file.getNumberOfBugFix().toString()));

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


    public static int countChurn(RevTree newTree, RevTree oldTree, String filepath) {
        try{
            TreeWalk tw = new TreeWalk(REPOSITORY);
            tw.addTree(newTree);
            if (oldTree != null) tw.addTree(oldTree);
            tw.setRecursive(true);
            tw.setFilter(PathFilter.create(filepath));
            tw.next();
            int currLines = countLines(REPOSITORY.open(tw.getObjectId(0)).openStream());
            int prevLines = 0;
            if (oldTree != null && tw.getFileMode(1)!= FileMode.MISSING) prevLines = countLines(REPOSITORY.open(tw.getObjectId(1)).openStream());
            return Math.abs(currLines - prevLines);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }


    public static int countLOCTouched(RevTree newTree, RevTree oldTree, String filepath) {
        try{
            TreeWalk tw = new TreeWalk(REPOSITORY);
            tw.addTree(newTree);
            if (oldTree != null) tw.addTree(oldTree);
            tw.setRecursive(true);
            tw.setFilter(PathFilter.create(filepath));
            tw.next();
            int currLines = countLines(REPOSITORY.open(tw.getObjectId(0)).openStream());
            int prevLines = 0;
            if (oldTree != null && tw.getFileMode(1)!= FileMode.MISSING) prevLines = countLines(REPOSITORY.open(tw.getObjectId(1)).openStream());
            return currLines + prevLines;
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }


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


    /** Ritorna in una release, quanti bugfix sono stati effettuati sul file */
    public static Boolean isBufFix(RevCommit commit) {
        String message = commit.getShortMessage().replace("  ", "");
        return message.startsWith("BOOKKEEPER-") && ISSUE_LIST.contains(message.split(":")[0]);
    }


    /** JIRA: costruisce l'ArrayList che contiene tutti i ticket BUG chiusi e fixati*/
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


    /** GIT: costruisce un ArrayList<ArrayList<RevCommit>> 'releaseCommits' che contiene l'array di commit divisi per release*/
    public static List<ArrayList<RevCommit>> getCommitsPerRelease(){
        ArrayList<ArrayList<RevCommit>> releaseCommits = new ArrayList<>();
        ArrayList<RevCommit> commits = new ArrayList<>();
        for (Release release : RELEASE_LIST) {
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


    public static int isGoodFile(String filepath,  int releaseNumber){
        if (filepath.contains(".java") && !filepath.contains("/test")){
            for (ClassFile classFile : RELEASE_LIST.get(releaseNumber - 1).getFiles()){
                if (classFile.getPath().equals(filepath)) return RELEASE_LIST.get(releaseNumber - 1).getFiles().indexOf(classFile);
            }
        }
        return -1;
    }


    public static void computeMetrics(List<ArrayList<RevCommit>> releaseCommits){

        for (ArrayList<RevCommit> commitsPerRelease : releaseCommits){
            int releaseNumber = releaseCommits.indexOf(commitsPerRelease) + 1;
            for (RevCommit commit : commitsPerRelease){
                // costruisce un'ArrayList 'filePaths' che contiene tutti i path dei file toccati nel commit (.java e non test)
                RevTree tree = commit.getTree();
                try (RevWalk revWalk = new RevWalk(REPOSITORY)) {
                    // caso speciale per il primo commit che non ha parent
                    if (commit.getParentCount() == 0) {
                        TreeWalk treeWalk = new TreeWalk(REPOSITORY);
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        ClassFile classFile = null;
                        int fileNumber = 0;
                        while (treeWalk.next()) {
                            String filePath = treeWalk.getPathString();
                            int index = isGoodFile(filePath,releaseNumber);
                            if (index != -1){
                                fileNumber += 1;
                                classFile = RELEASE_LIST.get(releaseNumber-1).getFiles().get(index);
                                classFile.incrementCommitsNumbers();
                                classFile.incrementTouchedLOCs(countLOCTouched(tree, null, filePath));
                                classFile.increaseChurn(countChurn(tree, null, filePath));
                                if (isBufFix(commit)) classFile.incrementNumberOfBugFix();
                            }
                        }
                        if (fileNumber != 0) classFile.incrementAverageChangeSet(fileNumber);
                    } else {
                        RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0).getId());
                        RevTree oldTree = parentCommit.getTree();
                        CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                        newTreeParser.reset(REPOSITORY.newObjectReader(), tree.getId());
                        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
                        oldTreeParser.reset(REPOSITORY.newObjectReader(), oldTree.getId());
                        List<DiffEntry> diffs = GIT_REPOSITORY.diff()
                                .setNewTree(newTreeParser)
                                .setOldTree(oldTreeParser)
                                .call();
                        for (DiffEntry diff : diffs) {
                            String filePath = diff.getNewPath();
                            int index = isGoodFile(filePath,releaseNumber);
                            if (index != -1){
                                ClassFile classFile = RELEASE_LIST.get(releaseNumber-1).getFiles().get(index);
                                classFile.incrementCommitsNumbers();
                                classFile.incrementTouchedLOCs(countLOCTouched(tree, oldTree, filePath));
                                classFile.increaseChurn(countChurn(tree, oldTree, filePath));
                                if (isBufFix(commit)) classFile.incrementNumberOfBugFix();
                                classFile.incrementAverageChangeSet(diffs.size());
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    public static void main(String[] args) throws Exception {

        long startTime = System.nanoTime();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        REPOSITORY = builder
                .setGitDir(new File(REPO_PATH)).readEnvironment()
                .findGitDir().build();

        GIT_REPOSITORY = new Git(REPOSITORY);

        // JIRA: prendo la lista di release (ordinata)
        RELEASE_LIST = RetrieveReleases(REPOSITORY);
        // JIRA: prendo la lista di issue bug fix
        ISSUE_LIST = retrieveIssues();

        // GIT: costruisce un ArrayList<ArrayList<RevCommit>> 'releaseCommits' che contiene l'array di commit divisi per release
        List<ArrayList<RevCommit>> releaseCommits = getCommitsPerRelease();

        computeMetrics(releaseCommits);

        writeOnFile();

        REPOSITORY.close();

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);

        System.out.println("Tempo di esecuzione: " + (float) duration/1000000000 + " secondi");
    }
}
