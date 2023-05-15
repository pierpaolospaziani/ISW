package com.mycompany.app.utils;

import com.mycompany.app.model.Release;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GetReleaseInfo {

    private GetReleaseInfo() {
        throw new IllegalStateException("Utility class");
    }


    public static List<Release> retrieveReleases(Repository repository, String projName) throws IOException, JSONException, GitAPIException {

        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        List<Release> releaseList = new ArrayList<>();
        List<String> releasesNames = new ArrayList<>();

        for (int i = 0; i < versions.length(); i++) {
            if (versions.getJSONObject(i).has("releaseDate") && versions.getJSONObject(i).has("name")) {
                String name = versions.getJSONObject(i).get("name").toString();
                addRelease(releasesNames, name, projName);
            }
        }

        // ordina le release
        if (Objects.equals(projName, "BOOKKEEPER")){
            bookkeeperSort(releasesNames);
        } else {
            openjpaSort(releasesNames);
        }

        // tolgo i branch e il 50%
        for (int i = 1; i <= releasesNames.size(); i++) {
            ObjectId obj = repository.resolve(releasesNames.get(i - 1));
            if (obj == null){
                releasesNames.remove(releasesNames.get(i - 1));
            }
        }
        int len = releasesNames.size();
        if (len > len / 2 + 1) {
            releasesNames.subList(len / 2 + 1, len).clear();
        }

        createReleases(releasesNames, releaseList, repository);

        return releaseList;
    }

    private static void createReleases(List<String> releasesNames, List<Release> releaseList, Repository repository) throws IOException, GitAPIException {
        for (String name : releasesNames) {
            printProgressBar(name, releasesNames.indexOf(name), releasesNames.size());
            Release release = new Release(name, repository);
            releaseList.add(release);
        }
    }


    public static void printProgressBar(String name, int progress, int total) {
        int percent = (int) ((float) progress / (float) total * 100);
        System.out.print("\r" + name + " :: [");
        for (int i = 0; i < 50; i++) {
            if (i < (percent / 2)) {
                System.out.print("=");
            } else if (i == (percent / 2)) {
                System.out.print(">");
            } else {
                System.out.print(" ");
            }
        }
        System.out.print("] " + percent + "%  ");
        if (progress == total) {
            System.out.print("\n");
        }
    }


    private static void openjpaSort(List<String> releasesNames) {
        releasesNames.sort((s1, s2) -> {
            String[] s1Parts = s1.split("/");
            String[] s2Parts = s2.split("/");
            String[] s1VersionParts = s1Parts[s1Parts.length-1].split("\\.");
            String[] s2VersionParts = s2Parts[s1Parts.length-1].split("\\.");
            int length = Math.min(s1VersionParts.length, s2VersionParts.length);
            for (int i = 0; i < length; i++) {
                int s1Part = Integer.parseInt(s1VersionParts[i]);
                int s2Part = Integer.parseInt(s2VersionParts[i]);
                if (s1Part != s2Part) {
                    return s1Part - s2Part;
                }
            }
            return s1VersionParts.length - s2VersionParts.length;
        });
    }


    private static void bookkeeperSort(List<String> releasesNames) {
        releasesNames.sort((s1, s2) -> {
            String[] s1Parts = s1.split("-");
            String[] s2Parts = s2.split("-");
            String[] s1VersionParts = s1Parts[1].split("\\.");
            String[] s2VersionParts = s2Parts[1].split("\\.");
            int length = Math.min(s1VersionParts.length, s2VersionParts.length);
            for (int i = 0; i < length; i++) {
                int s1Part = Integer.parseInt(s1VersionParts[i]);
                int s2Part = Integer.parseInt(s2VersionParts[i]);
                if (s1Part != s2Part) {
                    return s1Part - s2Part;
                }
            }
            return s1VersionParts.length - s2VersionParts.length;
        });
    }


    private static void addRelease(List<String> releaseList, String name, String projName) {
        String[] tkn = name.split("-");
        if (!name.contains("-") || tkn.length == 0){
            if (Objects.equals(projName, "BOOKKEEPER")){
                releaseList.add("refs/tags/release-" + name);
            } else {
                releaseList.add("refs/tags/" + name);
            }
        }
    }


    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String jsonText = readAll(rd);
        return new JSONObject(jsonText);
    }


    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}