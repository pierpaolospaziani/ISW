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

import static com.mycompany.app.getReleaseInfo.*;

public class FilesRet {

    public static ArrayList<ClassFile> files = new ArrayList<>();
    public static String repo_path = "/Users/pierpaolospaziani/Downloads/bookkeeper/.git";
    public static String projName = "BOOKKEEPER";
    public static Repository repository;
    public static ArrayList<String> issueList;

    public static void writeOnFile(){
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(projName + "FilesInfo.csv");
//            fileWriter.append("Version, Version_Name, Name, LOCs, Churn, Age, Number_of_Authors, Number of Revisions, Average Change Set\n");
            fileWriter.append("Version, Version Name, Path, LOCs, LOCs Touched, Churn, Age, Number of Revisions, Authors Number, Bugfix\n");

            for (int i = 0; i < relNames.size(); i++) {
                for (ClassFile file : files) {
                    if (file.getReleases().contains(i+1)){
                        int index = file.getReleases().indexOf(i+1);

                        fileWriter.append(Integer.toString(i+1));

                        fileWriter.append(",");
                        fileWriter.append(relNames.get(i));

                        fileWriter.append(",");
                        fileWriter.append(file.getPath());

                        fileWriter.append(",");
                        fileWriter.append(file.getLOCs().get(index).toString());

                        fileWriter.append(",");
                        fileWriter.append(file.getTouchedLOCs().get(index).toString());

                        fileWriter.append(",");
                        fileWriter.append(file.getChurn().get(index).toString());

//                        fileWriter.append(",");
//                        fileWriter.append((Integer.toString(i - file.getRevisionFirstAppearance() + 1)));

                        fileWriter.append(",");
                        fileWriter.append(String.valueOf(index + 1));

                        fileWriter.append(",");
                        fileWriter.append(file.getCommitsNumbers().get(index).toString());

                        fileWriter.append(",");
                        fileWriter.append((file.getnAuth().get(index).toString()));

                        fileWriter.append(",");
                        fileWriter.append((file.getNumberOfBugFix().get(index).toString()));
//                        int somma = 0;
//                        for (int j = 0; j < index; j++) {
//                            somma += file.getNumberOfBugFix().get(j);
//                        }
//                        fileWriter.append(String.valueOf(somma));

                        fileWriter.append("\n");
                    }
                }
            }
            System.out.println("File correctly written.");
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            System.out.println("Error in csv writer.");
            e.printStackTrace();
        }
    }

    /** Ritorna la posizione del file nell'array 'files', se non è presente ritorna -1 */
    public static int getFileIndex(String path){
        for(ClassFile f : files){
            if (f.getPath().contains(path)){
                return files.indexOf(f);
            }
        }
        return -1;
    }


    /** Analizza ogni file per ogni commit */
//    public static void listRepositoryContents(String releaseName, int releaseNumber) throws IOException, GitAPIException {
//        ObjectId objId = repository.resolve(releaseName);
//        if (objId==null) return;
//
//        // il RevWalk consente di novigare sui commit
//        RevWalk walk = new RevWalk(repository);
//        // prende un determinato commit
//        RevCommit commit = walk.parseCommit(objId.toObjectId());
//        // prende l'albero di directory e file che costituiscono lo stato del repository al momento del commit
//        RevTree tree = commit.getTree();
//
//        // prende un TreeWalk per iterare su tutti i file nell'albero ricorsivamente
//        TreeWalk treeWalk = new TreeWalk(repository);
//        treeWalk.addTree(tree);
//        treeWalk.setRecursive(true);
//
//        int fileIndex;
//        String[] tkns;
//
//        while (treeWalk.next()) {
//            if(treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test")) {
//
//                System.out.println(releaseName + " :: " + treeWalk.getPathString());
//
//                tkns = treeWalk.getPathString().split("/");
//                fileIndex = getFileIndex(tkns[tkns.length - 1], treeWalk.getPathString());
//
//                if (fileIndex >= 0) {
//                    // se il file era già presente ...
//                    files.get(fileIndex).incAppearances();
//                    files.get(fileIndex).insertRelease(releaseName);
//                    files.get(fileIndex).insertPath(treeWalk.getPathString());
//                    files.get(fileIndex).insertLOCs(countLOCs(treeWalk.getPathString(), releaseName));
//                    files.get(fileIndex).insertChurn(files.get(fileIndex).getReleases().size() - 1);
//                    files.get(fileIndex).insertAuth(countAuthorsInFile(treeWalk.getPathString(), relNames.get(releaseNumber-1)));
//                    files.get(fileIndex).insertRevisions(countCommits(repository, treeWalk.getPathString(), releaseName));
//                    files.get(fileIndex).insertChangedSetSize(nFileCommittedTogether(repository, treeWalk.getPathString(), releaseName));
//                } else {
//                    // ... se il file è nuovo
//                    ClassFile classFile = new ClassFile(tkns[tkns.length - 1]);
//                    classFile.insertRelease(releaseName);
//                    classFile.insertPath(treeWalk.getPathString());
//                    classFile.insertLOCs(countLOCs(treeWalk.getPathString(), releaseName));
//                    classFile.insertChurn(0);
//                    classFile.setRevisionFirstAppearance(releaseNumber);
//                    classFile.insertAuth(countAuthorsInFile(treeWalk.getPathString(), relNames.get(releaseNumber-1)));
//                    classFile.insertRevisions(countCommits(repository, treeWalk.getPathString(), releaseName));
//                    classFile.insertChangedSetSize(nFileCommittedTogether(repository, treeWalk.getPathString(), releaseName));
//                    files.add(classFile);
//                }
//            }
//        }
//    }


    public static int countLOCs(TreeWalk treeWalk, ClassFile classe) throws IOException {
        treeWalk.setFilter(PathFilter.create(classe.getPath()));
        treeWalk.next();
        ObjectId objectId = treeWalk.getObjectId(0);
        ObjectLoader loader = repository.open(objectId);
//        System.out.println("Linee: " + countLines(loader.openStream()) + " :: " + filePath);
        return countLines(loader.openStream());
    }


    public static int countChurn(RevTree newTree, RevTree oldTree, ClassFile classe) throws IOException {

        int prevLines = 0;
        int currLines = 0;

        try (TreeWalk tw = new TreeWalk(repository)) {
            tw.addTree(oldTree);
            tw.addTree(newTree);
            tw.setRecursive(true);
            tw.setFilter(PathFilter.create(classe.getPath()));
            tw.next();
            // se il file è stato cancellato
            try {
                currLines = countLines(repository.open(tw.getObjectId(1)).openStream());
            } catch (Exception e){
                classe.setDeleted();
                classe.removeRelease();
            }
            try {
                prevLines = countLines(repository.open(tw.getObjectId(0)).openStream());
            } catch (Exception ignored){}
        }
        return Math.abs(currLines - prevLines);
    }


    public static int countLOCTouched(RevTree newTree, RevTree oldTree, ClassFile classe) throws IOException {

        int prevLines = 0;
        int currLines = 0;

        try (TreeWalk tw = new TreeWalk(repository)) {
            tw.addTree(oldTree);
            tw.addTree(newTree);
            tw.setRecursive(true);
            tw.setFilter(PathFilter.create(classe.getPath()));
            tw.next();
            // se il file è stato cancellato
            try {
                currLines = countLines(repository.open(tw.getObjectId(1)).openStream());
            } catch (Exception e){
                classe.setDeleted();
                classe.removeRelease();
            }
            try {
                prevLines = countLines(repository.open(tw.getObjectId(0)).openStream());
            } catch (Exception ignored){}
        }
        return currLines + prevLines;
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


    public static int countAuthorsInFile(ClassFile classe, String relName) throws GitAPIException, IOException {
        ObjectId to = repository.resolve(relName);
        BlameResult blameResult = new Git(repository).blame()
                .setFilePath(classe.getPath())
                .setStartCommit(to)
                .call();
        Set<String> authors = new HashSet<>();
        try {
            for (int i = 0; i < blameResult.getResultContents().size(); i++) {
                authors.add(blameResult.getSourceAuthor(i).getName());
            }
            return authors.size();
        } catch (Exception e) {
            int authNumber = 1;
            if (classe.getnAuth().size() >= (relNames.indexOf(relName)+1)){
                authNumber += classe.getnAuth().get(relNames.indexOf(relName));
            }
            return authNumber;
        }
    }


    /** Ritorna in una release, quanti file mediamente vengono toccati nei commit insieme al file specificato */
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


    /** Ritorna in una release, quanti bugfix sono stati effettuati sul file */
    public static Boolean isBufFix(RevCommit commit) {
        String message = commit.getShortMessage().replace("  ", "");
        return message.startsWith("BOOKKEEPER-") && issueList.contains(message.split(":")[0]);
    }

    /** JIRA: costruisce l'ArrayList che contiene tutti i ticket BUG chiusi e fixati*/
    public static ArrayList<String> retrieveIssues() throws IOException {
        ArrayList<String> issuesListJira = new ArrayList<>();
        int j, i = 0, total;
        do {
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
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

    public static void main(String[] args) throws Exception {

        long startTime = System.nanoTime();

        // GIT: prendo il repository
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder
                .setGitDir(new File(repo_path)).readEnvironment()
                .findGitDir().build();
        Git git = new Git(repository);
        TreeWalk lastTreeWalk = new TreeWalk(repository);
        RevTree tree;

//      // JIRA: prendo la lista di release (ordinata)
        retrieveReleases();
        issueList = retrieveIssues();

        // tolgo i branch e il 50%
        for (int i = 1; i <= relNames.size(); i++) {
            ObjectId obj = repository.resolve(relNames.get(i - 1));
            if (obj == null){
                relNames.remove(relNames.get(i - 1));
            }
        }
        int len = relNames.size();
        if (len > len / 2 + 1) {
            relNames.subList(len / 2 + 1, len).clear();
        }

        // GIT: costruisce un ArrayList<ArrayList<RevCommit>> 'releaseCommits' che contiene l'array di commit divisi per release
        ArrayList<ArrayList<RevCommit>> releaseCommits = new ArrayList<>();
        ArrayList<RevCommit> commits = new ArrayList<>();
        Iterable<RevCommit> iterableCommits;
        for (String releaseName : relNames) {
            ObjectId objId = repository.resolve(releaseName);
            RevWalk walk = new RevWalk(repository);
            RevCommit curRelCommit = walk.parseCommit(objId);
            iterableCommits = git.log().add(curRelCommit).call();
            ArrayList<RevCommit> diffCommits = new ArrayList<>();
            iterableCommits.forEach(diffCommits::add);
            Collections.reverse(diffCommits);
            ArrayList<RevCommit> newCommits = new ArrayList<>();
            for (RevCommit c : diffCommits){
                if (!commits.contains(c)){
                    commits.add(c);
                    newCommits.add(c);
                }
            }
            releaseCommits.add(newCommits);
        }

        for (ArrayList<RevCommit> commitsPerRelease : releaseCommits){
            int releaseNumber = releaseCommits.indexOf(commitsPerRelease) + 1;
            for (RevCommit commit : commitsPerRelease){
                // costruisce un'ArrayList 'filePaths' che contiene tutti i path dei file toccati nel commit (.java e non test)
                tree = commit.getTree();
                TreeWalk treeWalk = new TreeWalk(repository);
                lastTreeWalk = treeWalk;
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                try (RevWalk revWalk = new RevWalk(repository)) {
                    RevCommit parentCommit = null;
                    // caso speciale per il primo commit che non ha parent
                    if (commit.getParentCount() == 0) {
                        while (treeWalk.next()) {
                            if (treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test")) {
                                String filePath = treeWalk.getPathString();
                                String[] tkn = filePath.split("/");
                                String filename = tkn[tkn.length - 1];
                                ClassFile classFile = new ClassFile(filename, filePath);
                                files.add(classFile);
                                classFile.getCommitsNumbers().add(0);
                                classFile.getReleases().add(releaseNumber);
                                classFile.getReleasesNames().add(relNames.get(releaseNumber-1));
                                classFile.insertLOCs(countLOCs(treeWalk, classFile), releaseNumber);
                                classFile.insertTouchedLOCs(0, releaseNumber);
                                classFile.insertChurn(0, releaseNumber);
                                classFile.insertAuth(countAuthorsInFile(classFile, relNames.get(releaseNumber-1)));
                                classFile.increaseNumberOfBugFix(releaseNumber, isBufFix(commit));
                            }
                        }
                    } else {
                        parentCommit = revWalk.parseCommit(commit.getParent(0).getId());
                        RevTree oldTree = parentCommit.getTree();
                        CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                        newTreeParser.reset(repository.newObjectReader(), commit.getTree().getId());
                        CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
                        oldTreeParser.reset(repository.newObjectReader(), parentCommit.getTree().getId());
                        List<DiffEntry> diffs = git.diff()
                                .setNewTree(newTreeParser)
                                .setOldTree(oldTreeParser)
                                .call();
                        for (DiffEntry diff : diffs) {
                            if (diff.getNewPath().contains(".java") && !diff.getNewPath().contains("/test")) {
                                String filePath = diff.getNewPath();
                                String[] tkn = filePath.split("/");
                                String filename = tkn[tkn.length - 1];
                                int fileIndex = getFileIndex(filePath);
                                if (fileIndex == -1) {
                                    // se il file non era presente nella lista 'files'
                                    ClassFile classFile = new ClassFile(filename, filePath);
                                    files.add(classFile);
                                    fileIndex = files.size() - 1;
                                }
                                ClassFile classe = files.get(fileIndex);
                                // bisogna chiamare tutte le metriche per 'classe'
                                classe.insertRelease(releaseNumber, relNames.get(releaseCommits.indexOf(commitsPerRelease)));
                                classe.incrementCommitsNumbers(releaseNumber);
                                classe.insertLOCs(countLOCs(treeWalk, classe), releaseNumber);
                                classe.insertTouchedLOCs(countLOCTouched(tree, oldTree, classe), releaseNumber);
                                classe.insertChurn(countChurn(tree, oldTree, classe), releaseNumber);
                                classe.insertAuth(countAuthorsInFile(classe, relNames.get(releaseNumber-1)));
                                classe.increaseNumberOfBugFix(releaseNumber, isBufFix(commit));
                            }
                        }
                    }
                }
            }

            for (ClassFile classFile : files){
                // se il file non è stato eliminato && non è stato toccato mai nella release
                if (classFile.getDeleted() && !classFile.getReleases().contains(releaseNumber)){
                    classFile.getCommitsNumbers().add(0);
                    classFile.getReleases().add(releaseNumber);
                    classFile.getReleasesNames().add(relNames.get(releaseNumber-1));
                    classFile.insertLOCs(countLOCs(lastTreeWalk, classFile), releaseNumber);
                    classFile.insertTouchedLOCs(0, releaseNumber);
                    classFile.insertChurn(0, releaseNumber);
                    classFile.insertAuth(classFile.getnAuth().get(releaseNumber-1));
                    classFile.setNoBugFix();
                }
            }
        }

//        countFixCommits(relNames.get(0),0);

//        for (ClassFile classe : files){
//            System.out.println(classe.getPaths() + " :: " + classe.getReleases());
//            System.out.println(classe.getPaths() + " :: " + classe.getCommitsNumbers() + "\n");
//        }

        writeOnFile();

        repository.close();

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);

        System.out.println("Tempo di esecuzione: " + (float) duration/1000000000 + " secondi");
    }
}
