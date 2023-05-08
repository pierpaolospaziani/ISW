package com.mycompany.app;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
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

import static com.mycompany.app.getReleaseInfo.relNames;
import static com.mycompany.app.getReleaseInfo.retrieveReleases;
import static com.mycompany.app.getReleaseInfo.readJsonFromUrl;

public class FilesRet {

    public static ArrayList<ClassFile> files = new ArrayList<>();
    public static String repo_path = "/Users/pierpaolospaziani/Downloads/bookkeeper/.git";
    public static String projName = "BOOKKEEPER";
    public static List<Ref> branches = new ArrayList<>();
    public static List<Ref> tags = new ArrayList<>();
    public static Repository repository;
    public static ArrayList<Release> releases = new ArrayList<>();

    public static void writeOnFile(){
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(projName + "FilesInfo.csv");
            fileWriter.append("Version, Version_Name, Name, LOCs, Churn, Age, Number_of_Authors, Number of Revisions, Average Change Set\n");

            for (int i = 0; i < relNames.size(); i++) {
                for (ClassFile file : files) {
                    if ((i >= file.getRevisionFirstAppearance() - 1) && (file.getAppearances() > 0)) {

                        fileWriter.append(Integer.toString(i+1));

                        fileWriter.append(",");
                        fileWriter.append(relNames.get(i));

                        fileWriter.append(",");
                        fileWriter.append(file.getPaths().get(0));

                        fileWriter.append(",");
                        fileWriter.append(file.getLOCs().get(0).toString());

                        fileWriter.append(",");
                        fileWriter.append(file.getChurn().get(0).toString());

                        fileWriter.append(",");
                        fileWriter.append((Integer.toString(i - file.getRevisionFirstAppearance() + 1)));

                        fileWriter.append(",");
                        fileWriter.append((file.getnAuth().get(0).toString()));

                        fileWriter.append(",");
                        fileWriter.append(file.getRevisions().get(i - file.getRevisionFirstAppearance() + 1).toString());

                        fileWriter.append(",");
                        fileWriter.append(file.getNFilesChanged().get(i - file.getRevisionFirstAppearance() + 1).toString());

                        fileWriter.append("\n");

                        file.getPaths().remove(0);
                        file.getLOCs().remove(0);
                        file.getChurn().remove(0);
                        file.getnAuth().remove(0);

                        file.decAppearances();
                    }
                }
            }
            System.out.println("File correctly written.");
        } catch (Exception e) {
            System.out.println("Error in csv writer.");
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter.");
                e.printStackTrace();
            }
        }
    }

    /** Ritorna la posizione del file nell'array 'files', se non è presente -1 */
    public static int getFileIndex(String name, String path){
        for(ClassFile f : files){
            if(f.getName().equals(name)){
                if(f.getPaths().size() > 0) {
                    if (path.equals(f.getPaths().get(f.getPaths().size()-1))) {
                        return files.indexOf(f);
                    }
                }
            }
        }
        return -1;
    }

    /** Analizza ogni file per ogni commit */
    public static void listRepositoryContents(String releaseName, int releaseNumber) throws IOException, GitAPIException {
        ObjectId objId = repository.resolve(releaseName);
        if (objId==null) return;

        // il RevWalk consente di novigare sui commit
        RevWalk walk = new RevWalk(repository);
        // prende un determinato commit
        RevCommit commit = walk.parseCommit(objId.toObjectId());
        // prende l'albero di directory e file che costituiscono lo stato del repository al momento del commit
        RevTree tree = commit.getTree();

        // prende un TreeWalk per iterare su tutti i file nell'albero ricorsivamente
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);

        int fileIndex;
        String[] tkns;

        while (treeWalk.next()) {
            if(treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test")) {

                System.out.println(releaseName + " :: " + treeWalk.getPathString());

                tkns = treeWalk.getPathString().split("/");
                fileIndex = getFileIndex(tkns[tkns.length - 1], treeWalk.getPathString());

                if (fileIndex >= 0) {
                    // se il file era già presente ...
                    files.get(fileIndex).incAppearances();
                    files.get(fileIndex).insertRelease(releaseName);
                    files.get(fileIndex).insertPath(treeWalk.getPathString());
                    files.get(fileIndex).insertLOCs(countLOCs(treeWalk.getPathString(), releaseName));
                    files.get(fileIndex).insertChurn(files.get(fileIndex).getReleases().size() - 1);
                    files.get(fileIndex).insertAuth(countAuthorsInFile(treeWalk.getPathString(), relNames.get(releaseNumber-1)));
                    files.get(fileIndex).insertRevisions(countCommits(repository, treeWalk.getPathString(), releaseName));
                    files.get(fileIndex).insertChangedSetSize(nFileCommittedTogether(repository, treeWalk.getPathString(), releaseName));
                } else {
                    // ... se il file è nuovo
                    ClassFile classFile = new ClassFile(tkns[tkns.length - 1]);
                    classFile.setName(tkns[tkns.length - 1]);
                    classFile.insertRelease(releaseName);
                    classFile.insertPath(treeWalk.getPathString());
                    classFile.insertLOCs(countLOCs(treeWalk.getPathString(), releaseName));
                    classFile.insertChurn(0);
                    classFile.setRevisionFirstAppearance(releaseNumber);
                    classFile.insertAuth(countAuthorsInFile(treeWalk.getPathString(), relNames.get(releaseNumber-1)));
                    classFile.insertRevisions(countCommits(repository, treeWalk.getPathString(), releaseName));
                    classFile.insertChangedSetSize(nFileCommittedTogether(repository, treeWalk.getPathString(), releaseName));
                    files.add(classFile);
                }
            }
        }
    }


    public static int countLOCs(String filePath, String release) throws IOException {
        RevWalk walk = new RevWalk(repository);
        ObjectId headId = repository.resolve(release);
        RevCommit commit = walk.parseCommit(headId);
        RevTree tree = commit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathFilter.create(filePath));
        int lines = 0;
        while (treeWalk.next()) {
            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repository.open(objectId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(loader.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines++;
                }
            }
        }
        return lines;
    }


    public static int countAuthorsInFile(String filePath, String toCommit) throws IOException, GitAPIException {
        int authorsCount = 0;

        ObjectId to = repository.resolve(toCommit); //resolve the commit id from the tag name

        BlameResult blameResult = new Git(repository).blame()
                .setFilePath(filePath)
                .setStartCommit(to)
                .call();

        Set<String> authors = new HashSet<>();
        for (int i = 0; i < blameResult.getResultContents().size(); i++) {
            authors.add(blameResult.getSourceAuthor(i).getName());
            //System.out.println(blameResult.getSourceAuthor(i).getName());
        }
        authorsCount = authors.size();
        return authorsCount;
    }


    /** Ritorna, per la release, quanti file mediamente vengono toccati nei commit insieme al file specificato */
    public static int nFileCommittedTogether(Repository repository, String file, String currentRelease) throws IOException, GitAPIException{

        RevWalk walk = new RevWalk(repository);
        Git git = new Git(repository);
        int count = 0; // number of files changed into a commit to return

        ObjectReader reader = repository.newObjectReader();
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();

        // prendi gli object id delle release
        ObjectId objId = repository.resolve(currentRelease);
        RevCommit curRelCommit = walk.parseCommit(objId);

        // fai il log dell'ultima release e prendi tutti i commit
        LogCommand log = git.log().add(curRelCommit);

        Iterable<RevCommit> commits = log.call();

        ArrayList<RevCommit> relCommits = new ArrayList<>();
        for(RevCommit com : commits){
            relCommits.add(com);
        }

        int commitsNumber = 0;  // numero di commit in cui è stata modificata una classe
        for(RevCommit commit : relCommits) {
            if(relCommits.indexOf(commit) == relCommits.size()-1)
                break;

            RevTree tree1 = commit.getTree();
            newTreeIter.reset(reader, tree1);

            RevTree tree2 = relCommits.get(relCommits.indexOf(commit) + 1).getTree();
            oldTreeIter.reset(reader, tree2);

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(repository);
            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);     // classi cambiate tra due commit

            for(DiffEntry entry : entries) {
                if(entry.getNewPath().equals(file)) {
                    count += entries.size();
                    commitsNumber++;
                    break;
                }
            }
        }
//        System.out.println("Media di file toccati in ogni commit: " + count/commitsNumber);
        return count/commitsNumber;
    }


    public static int countCommits(Repository repository, String file, String currentRelease) throws IOException, GitAPIException {
        RevWalk walk = new RevWalk(repository);
        Git git = new Git(repository);
        int c = 0;

        ObjectReader reader = repository.newObjectReader();
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();

        // prendi gli object id delle release
        ObjectId objNewId = repository.resolve(currentRelease);

        RevCommit curRelCommit = walk.parseCommit(objNewId);

        // fai il log dell'ultima release e prendi tutti i commit
        LogCommand log = git.log().add(curRelCommit);

        Iterable<RevCommit> commits = log.call();

        ArrayList<RevCommit> relCommits = new ArrayList<>();
        for(RevCommit com : commits){
            relCommits.add(com);
        }

        for(RevCommit commit : relCommits) {
            if(relCommits.indexOf(commit) == relCommits.size()-1)
                break;

            RevTree tree1 = commit.getTree();
            newTreeIter.reset(reader, tree1);

            RevTree tree2 = relCommits.get(relCommits.indexOf(commit) + 1).getTree();
            oldTreeIter.reset(reader, tree2);

            DiffFormatter diffFormatter = new DiffFormatter( DisabledOutputStream.INSTANCE );
            diffFormatter.setRepository(repository);
            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);

            //System.out.println(commit + "     c: " + c);
            for(DiffEntry entry : entries) {
                if(entry.getNewPath().equals(file)){
                    //System.out.println(entry.getNewPath());
                    c++;
                }

            }
        }
//        System.out.println("Numero di commit di un file in una release: " + c);
        return c;
    }


    public static void countFixCommits(String releaseName, int releaseIndex) throws IOException, GitAPIException {

        RevWalk walk = new RevWalk(repository);
        Git git = new Git(repository);

        ObjectReader reader = repository.newObjectReader();
        CanonicalTreeParser treeIter = new CanonicalTreeParser();

        ArrayList<String> issuesListJira = new ArrayList<>();
        int j, i = 0, total;
        do {
            // Get JSON API for closed bugs w/ AV in the project
            // Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
                    + i + "&maxResults=" + j;
            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            for (; i < total && i < j; i++) {
                //Iterate through each bug
                String key = issues.getJSONObject(i % 1000).get("key").toString();
                issuesListJira.add(key);
            }
        } while (i < total);

        // GIT: costruisce un'ArrayList 'commits' che contiene tutti i commit della release
        ObjectId objId = repository.resolve(releaseName);
        RevCommit curRelCommit = walk.parseCommit(objId);
        LogCommand log = git.log().add(curRelCommit);
        Iterable<RevCommit> iterableCommits = log.call();
        ArrayList<RevCommit> commits = new ArrayList<>();
        iterableCommits.forEach(commits::add);

        // GIT: costruisce l'array 'issueMessages' che contiene i nomi dei commit relativi a bugfix (es. BOOKKEEPER-123)
        //      e l'array 'commitsList' che univocamente associa i relativi commit effettivi
        ArrayList<String> commitMessages  = new ArrayList<>();
        ArrayList<String> issueMessages   = new ArrayList<>();
        ArrayList<RevCommit> commitsList  = new ArrayList<>();
        int issuesIndex = 0;
        for(RevCommit commit : commits) {
            String message = commit.getShortMessage().replace("  ", "");
            if (message.startsWith("BOOKKEEPER-")){
                commitMessages.add(message);
                String[] listOfMessages = commitMessages.get(issuesIndex).split(":");
                issueMessages.add(listOfMessages[0]);
                issuesIndex++;
                commitsList.add(commit);
            }
        }

        // conteggio dei commit bugfix presenti nella release
        for (String message : issueMessages){
            if (issuesListJira.contains(message)){
                RevCommit commit = commitsList.get(issueMessages.indexOf(message));
                RevTree tree = commit.getTree();
                TreeWalk treeWalk = new TreeWalk(repository);
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);

                try (RevWalk revWalk = new RevWalk(repository)) {
                    RevCommit parentCommit = revWalk.parseCommit(commit.getParent(0).getId());
                    CanonicalTreeParser newTree = new CanonicalTreeParser();
                    newTree.reset(repository.newObjectReader(), commit.getTree().getId());

                    CanonicalTreeParser oldTree = new CanonicalTreeParser();
                    oldTree.reset(repository.newObjectReader(), parentCommit.getTree().getId());

                    List<DiffEntry> diffs = git.diff()
                            .setNewTree(newTree)
                            .setOldTree(oldTree)
                            .call();

                    List<String> filePaths = new ArrayList<>();

                    for (DiffEntry diff : diffs) {
                        if(diff.getNewPath().contains(".java") && !diff.getNewPath().contains("/test")) {
                            filePaths.add(diff.getNewPath());

//                            System.out.println(message + " :: " + filePaths);

                            String[] tkn = filePaths.get(0).split("/");
                            String filename = tkn[tkn.length-1];

//                            System.out.println(message + " :: " + filename + "\n");

                            for (ClassFile classe : files){
                                if (classe.getName().equals(filename)){
                                    if (classe.getNumberOfBugFix().size() == 0){
                                        classe.setNumberOfBugFix(0, releaseIndex);
                                    }
                                    classe.setNumberOfBugFix(classe.getNumberOfBugFix().get(releaseIndex) + 1, releaseIndex);
                                }
                            }
                        }
//                        break;
                    }
                }
            }
        }
    }


    public static void main(String[] args) throws IOException, GitAPIException {

        long startTime = System.nanoTime();

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder
                .setGitDir(new File(repo_path)).readEnvironment()
                .findGitDir().build();

        retrieveReleases();

        // per ogni release (tag) lista i file
        int releaseNumber = 0;
        for (String releaseName : relNames) {
            // per ogni branch cerca tutti i file - escludi HEAD e master
//            countFixCommits(releaseName,releaseNumber);
            releaseNumber++;
            listRepositoryContents(releaseName, releaseNumber);
            if (releaseNumber == 1){
                break;
            }
        }
        countFixCommits(relNames.get(0),0);

        for (ClassFile classe : files){
            System.out.println(classe.getName() + " :: " + classe.getNumberOfBugFix() + "\n");
        }

        writeOnFile();
        repository.close();

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);  // in nanosecondi

        System.out.println("Tempo di esecuzione: " + duration/1000000 + " millisecondi");
    }
}
