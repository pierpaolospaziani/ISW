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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        HashMap<String, LocalDateTime> releasesMap = new HashMap<>();

        for (int i = 0; i < versions.length(); i++) {
            if (versions.getJSONObject(i).has("releaseDate") && versions.getJSONObject(i).has("name")) {
                LocalDateTime date = LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString()).atStartOfDay();
                String name = versions.getJSONObject(i).get("name").toString();
                addRelease(releasesMap, name, date, projName);
            }
        }

        // ordina le release in base al LocalDateTime
        List<Map.Entry<String, LocalDateTime>> entryList = new ArrayList<>(releasesMap.entrySet());
        entryList.sort(Map.Entry.comparingByValue());

        // tolgo i branch e il 50%
        for (int i = 1; i <= entryList.size(); i++) {
            ObjectId obj = repository.resolve(entryList.get(i - 1).getKey());
            if (obj == null){
                entryList.remove(entryList.get(i - 1));
            }
        }
        int len = entryList.size();
        if (len > len / 2 + 1) {
            entryList.subList(len / 2 + 1, len).clear();
        }

        createReleases(entryList, releaseList, repository);

//        for (Release r : releaseList) System.out.println(r.getName());

        return releaseList;
    }


    private static void createReleases(List<Map.Entry<String, LocalDateTime>> entryList, List<Release> releaseList, Repository repository) throws IOException, GitAPIException {
        for (Map.Entry<String, LocalDateTime> entry : entryList) {
            printProgressBar(entry.getKey(), entryList.indexOf(entry), entryList.size());
            Release release = new Release(entry.getKey(), entry.getValue(), repository);
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


    private static void addRelease(HashMap<String,LocalDateTime> releasesMap, String name, LocalDateTime date, String projName) {
        String[] tkn = name.split("-");
        if (!name.contains("-") || tkn.length == 0){
            if (Objects.equals(projName, "BOOKKEEPER")){
                releasesMap.put("refs/tags/release-" + name, date);
            } else if (!name.split("\\.")[0].equals(String.valueOf(0))){
                releasesMap.put("refs/tags/" + name, date);
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