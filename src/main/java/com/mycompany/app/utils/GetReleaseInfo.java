package com.mycompany.app.utils;

import com.mycompany.app.model.Release;
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


    public static List<Release> retrieveReleases(Repository repository, String projName) throws IOException, JSONException {

        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        List<Release> releaseList = new ArrayList<>();

        for (int i = 0; i < versions.length(); i++) {
            String name = "";
            if (versions.getJSONObject(i).has("releaseDate") && versions.getJSONObject(i).has("name")) {
                name = versions.getJSONObject(i).get("name").toString();
                addRelease(releaseList, name, repository);
            }
        }

        // ordina le release
        releaseList.sort((s1, s2) -> {
            String[] s1Parts = s1.getName().split("-");
            String[] s2Parts = s2.getName().split("-");
            String[] s1VersionParts = s1Parts[s1Parts.length - 1].split("\\.");
            String[] s2VersionParts = s2Parts[s2Parts.length - 1].split("\\.");
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

        // tolgo i branch e il 50%
        for (int i = 1; i <= releaseList.size(); i++) {
            ObjectId obj = repository.resolve(releaseList.get(i - 1).getName());
            if (obj == null){
                releaseList.remove(releaseList.get(i - 1));
            }
        }
        int len = releaseList.size();
        if (len > len / 2 + 1) {
            releaseList.subList(len / 2 + 1, len).clear();
        }

        return releaseList;
    }


    private static void addRelease(List<Release> releaseList, String name, Repository repository) {
        Release release = new Release("refs/tags/release-" + name, repository);
        releaseList.add(release);
    }


    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
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