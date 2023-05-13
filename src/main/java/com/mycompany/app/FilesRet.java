package com.mycompany.app;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
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

import static com.mycompany.app.getReleaseInfo.*;

public class FilesRet {
    public static final String RepoPath = "/Users/pierpaolospaziani/Downloads/bookkeeper/.git";
    public static final String ProjName = "BOOKKEEPER";
    public static List<String> IssueList = null;
    public static List<Release> ReleaseList = null;
    public static Repository Repository = null;
    public static Git GitRepository = null;

    public static void writeOnFile() throws IOException {
        try (FileWriter fileWriter = new FileWriter(ProjName + "FilesInfo.csv")) {
            //            fileWriter.append("Version, Version_Name, Name, LOCs, Churn, Age, Number_of_Authors, Number of Revisions, Average Change Set\n");
            fileWriter.append("Version, Version Name, Path, LOCs, LOCs Touched, Churn, Number of Revisions, Authors Number, Bugfix\n");

            for (Release release : ReleaseList) {
                for (ClassFile file : release.getFiles()) {

                    int releaseNumber = ReleaseList.indexOf(release);
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
//                        int somma = 0;
//                        for (int j = 0; j < index; j++) {
//                            somma += file.getNumberOfBugFix().get(j);
//                        }
//                        fileWriter.append(String.valueOf(somma));

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
            TreeWalk tw = new TreeWalk(Repository);
            tw.addTree(newTree);
            if (oldTree != null) tw.addTree(oldTree);
            tw.setRecursive(true);
            tw.setFilter(PathFilter.create(filepath));
            tw.next();
            int currLines = countLines(Repository.open(tw.getObjectId(0)).openStream());
            int prevLines = 0;
            if (oldTree != null && tw.getFileMode(1)!= FileMode.MISSING) prevLines = countLines(Repository.open(tw.getObjectId(1)).openStream());
            return Math.abs(currLines - prevLines);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }


    public static int countLOCTouched(RevTree newTree, RevTree oldTree, String filepath) {
        try{
            TreeWalk tw = new TreeWalk(Repository);
            tw.addTree(newTree);
            if (oldTree != null) tw.addTree(oldTree);
            tw.setRecursive(true);
            tw.setFilter(PathFilter.create(filepath));
            tw.next();
            int currLines = countLines(Repository.open(tw.getObjectId(0)).openStream());
            int prevLines = 0;
            if (oldTree != null && tw.getFileMode(1)!= FileMode.MISSING) prevLines = countLines(Repository.open(tw.getObjectId(1)).openStream());
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


    /** Ritorna in una release, quanti file mediamente vengono toccati nei commit insieme al file specificato */
    public static int nFileCommittedTogether(Repository repository, String file, String currentRelease) throws IOException, GitAPIException{

        RevWalk walk = new RevWalk(repository);
        Git git = new Git(repository);
        int count = 0; // number of files changed into a commit to return

        try (ObjectReader reader = repository.newObjectReader()){
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();

            // prendi gli object id delle release
            ObjectId objId = repository.resolve(currentRelease);
            RevCommit curRelCommit = walk.parseCommit(objId);

            // fai il log dell'ultima release e prendi tutti i commit
            LogCommand log = git.log().add(curRelCommit);

            Iterable<RevCommit> commits = log.call();

            ArrayList<RevCommit> relCommits = new ArrayList<>();
            for (RevCommit com : commits) {
                relCommits.add(com);
            }

            int commitsNumber = 0;  // numero di commit in cui è stata modificata una classe
            for (RevCommit commit : relCommits) {
                if (relCommits.indexOf(commit) == relCommits.size() - 1)
                    break;

                RevTree tree1 = commit.getTree();
                newTreeIter.reset(reader, tree1);

                RevTree tree2 = relCommits.get(relCommits.indexOf(commit) + 1).getTree();
                oldTreeIter.reset(reader, tree2);

                DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
                diffFormatter.setRepository(repository);
                List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);     // classi cambiate tra due commit

                for (DiffEntry entry : entries) {
                    if (entry.getNewPath().equals(file)) {
                        count += entries.size();
                        commitsNumber++;
                        break;
                    }
                }
            }
            //        System.out.println("Media di file toccati in ogni commit: " + count/commitsNumber);
            return count / commitsNumber;
        }
    }


    /** Ritorna in una release, quanti bugfix sono stati effettuati sul file */
    public static Boolean isBufFix(RevCommit commit) {
        String message = commit.getShortMessage().replace("  ", "");
        return message.startsWith("BOOKKEEPER-") && IssueList.contains(message.split(":")[0]);
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
                    + ProjName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
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
        for (Release release : ReleaseList) {
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
            for (ClassFile classFile : ReleaseList.get(releaseNumber - 1).getFiles()){
                if (classFile.getPath().equals(filepath)) return ReleaseList.get(releaseNumber - 1).getFiles().indexOf(classFile);
            }
        }
        return -1;
    }


    public static void doMetrics(List<ArrayList<RevCommit>> releaseCommits){

        for (ArrayList<RevCommit> commitsPerRelease : releaseCommits){
            int releaseNumber = releaseCommits.indexOf(commitsPerRelease) + 1;
            for (RevCommit commit : commitsPerRelease){
                // costruisce un'ArrayList 'filePaths' che contiene tutti i path dei file toccati nel commit (.java e non test)
                RevTree tree = commit.getTree();
                try (RevWalk revWalk = new RevWalk(Repository)) {
                    // caso speciale per il primo commit che non ha parent
                    if (commit.getParentCount() == 0) {
                        TreeWalk treeWalk = new TreeWalk(Repository);
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        while (treeWalk.next()) {
                            String filePath = treeWalk.getPathString();
                            int index = isGoodFile(filePath,releaseNumber);
                            if (index != -1){
                                ClassFile classFile = ReleaseList.get(releaseNumber-1).getFiles().get(index);
                                classFile.incrementCommitsNumbers();
                                classFile.increaseTouchedLOCs(countLOCTouched(tree, null, filePath));
                                classFile.increaseChurn(countChurn(tree, null, filePath));
                                if (isBufFix(commit)) classFile.increaseNumberOfBugFix();
                            }
                        }
                    } else {
                        RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0).getId());
                        RevTree oldTree = parentCommit.getTree();
                        CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                        newTreeParser.reset(Repository.newObjectReader(), tree.getId());
                        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
                        oldTreeParser.reset(Repository.newObjectReader(), oldTree.getId());
                        List<DiffEntry> diffs = GitRepository.diff()
                                .setNewTree(newTreeParser)
                                .setOldTree(oldTreeParser)
                                .call();
                        for (DiffEntry diff : diffs) {
                            String filePath = diff.getNewPath();
                            int index = isGoodFile(filePath,releaseNumber);
                            if (index != -1){
                                ClassFile classFile = ReleaseList.get(releaseNumber-1).getFiles().get(index);
                                // bisogna chiamare tutte le metriche per 'classe'
                                classFile.incrementCommitsNumbers();
                                classFile.increaseTouchedLOCs(countLOCTouched(tree, oldTree, filePath));
                                classFile.increaseChurn(countChurn(tree, oldTree, filePath));
                                if (isBufFix(commit)) classFile.increaseNumberOfBugFix();
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
        Repository = builder
                .setGitDir(new File(RepoPath)).readEnvironment()
                .findGitDir().build();

        GitRepository = new Git(Repository);

        // JIRA: prendo la lista di release (ordinata)
        ReleaseList = RetrieveReleases(Repository);
        // JIRA: prendo la lista di issue bug fix
        IssueList = retrieveIssues();

        // GIT: costruisce un ArrayList<ArrayList<RevCommit>> 'releaseCommits' che contiene l'array di commit divisi per release
        List<ArrayList<RevCommit>> releaseCommits = getCommitsPerRelease();

        doMetrics(releaseCommits);

        writeOnFile();

        Repository.close();

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);

        System.out.println("Tempo di esecuzione: " + (float) duration/1000000000 + " secondi");
    }
}
