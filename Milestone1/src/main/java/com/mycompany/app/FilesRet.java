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

import java.io.*;
import java.util.*;

import static com.mycompany.app.getReleaseInfo.relNames;
import static com.mycompany.app.getReleaseInfo.retrieveReleases;

public class FilesRet {

    public static ArrayList<RepoFile> files = new ArrayList<>();
    public static String repo_path = "/Users/pierpaolospaziani/Downloads/bookkeeper/.git";
    public static String projName = "Bookkeeper";
    public static List<Ref> branches = new ArrayList<>();
    public static List<Ref> tags = new ArrayList<>();
    public static Repository repository;
    public static ArrayList<Release> releases = new ArrayList<>();

    public static void writeOnFile(){
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(projName + "FilesInfo.csv");
            fileWriter.append("Version, Version_Name, Name, LOCs, Churn, Age, Number_of_Authors, Number of Revisions, Change Set Size\n");

            for (int i = 0; i < relNames.size(); i++) {
                for (RepoFile file : files) {
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
        for(RepoFile f : files){
            if(f.equals(name)){
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
        ObjectId head = repository.resolve(releaseName);
        if (head==null) return;

        // il RevWalk consente di novigare sui commit
        RevWalk walk = new RevWalk(repository);
        // prende un determinato commit
        RevCommit commit = walk.parseCommit(head.toObjectId());
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
                    RepoFile repoFile = new RepoFile(tkns[tkns.length - 1]);
                    repoFile.insertRelease(releaseName);
                    repoFile.insertPath(treeWalk.getPathString());
                    repoFile.insertLOCs(countLOCs(treeWalk.getPathString(), releaseName));
                    repoFile.insertChurn(0);
                    repoFile.setRevisionFirstAppearance(releaseNumber);
                    repoFile.insertAuth(countAuthorsInFile(treeWalk.getPathString(), relNames.get(releaseNumber-1)));
                    repoFile.insertRevisions(countCommits(repository, treeWalk.getPathString(), releaseName));
                    repoFile.insertChangedSetSize(nFileCommittedTogether(repository, treeWalk.getPathString(), releaseName));
                    files.add(repoFile);
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
        ObjectId objNewId = repository.resolve(currentRelease);

        RevCommit curRelCommit = walk.parseCommit(objNewId);

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


    public static void main(String[] args) throws IOException, GitAPIException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        repository = builder
                .setGitDir(new File(repo_path)).readEnvironment()
                .findGitDir().build();

        retrieveReleases();

        // per ogni release (tag) lista i file
        int releaseNumber = 0;
        for (String releaseName : relNames) {
            // per ogni branch cerca tutti i file - escludi HEAD e master
            releaseNumber++;
            listRepositoryContents(releaseName, releaseNumber);
            if (releaseNumber == 2){
                break;
            }
        }

        writeOnFile();
        repository.close();
    }
}
