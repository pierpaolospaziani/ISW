package com.mycompany.app.utils;

import com.mycompany.app.model.ClassFile;
import com.mycompany.app.model.Release;

import java.io.*;
import java.util.HashMap;
import java.util.List;

import static com.mycompany.app.RetrieveDataset.countAge;

public class IO {

    private IO() {
        throw new IllegalStateException("Utility class");
    }

    /** Esegue la scrittura delle metriche sul csv */
    public static void writeOnFile(String projectName, List<Release> releaseList) throws IOException {
        try (FileWriter fileWriter = new FileWriter(projectName + ".csv")) {
            fileWriter.append("Version, Version Name, Name, Age, Revisions, Bugfix, LOCs, LOCs Touched, LOCs Added, Churn, Avg. Churn, Authors Number, Average Change Set, Buggy\n");

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

                    fileWriter.append(",");
                    fileWriter.append(String.valueOf(countAge(hashMap, file)));

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
                        fileWriter.append(String.valueOf((double) file.getChurn()/file.getCommitsNumbers()));
                    } else {
                        fileWriter.append(file.getChurn().toString());
                    }

                    fileWriter.append(",");
                    fileWriter.append((file.getnAuth().toString()));

                    fileWriter.append(",");
                    if (file.getCommitsNumbers() != 0){
                        fileWriter.append(String.valueOf((double) file.getAverageChangeSet()/file.getCommitsNumbers()));
                    } else {
                        fileWriter.append((file.getAverageChangeSet().toString()));
                    }

                    fileWriter.append(",");
                    fileWriter.append((file.getBuggy().toString()));

                    fileWriter.append("\n");
                }
            }
            fileWriter.flush();
        } catch (Exception e) {
            IO.appendOnLog(e +"\n");
        }
    }

    public static void appendOnLog(String whatToWrite) throws IOException {
        try (FileWriter fileWriter = new FileWriter(Initializer.getLogFileName())) {
            fileWriter.append(whatToWrite).append("\n");
            fileWriter.flush();
        }
    }

    public static void clean() throws IOException {
        try (FileWriter fileWriter = new FileWriter("log.txt")) {
            fileWriter.append("");
            fileWriter.flush();
        }
    }
}
