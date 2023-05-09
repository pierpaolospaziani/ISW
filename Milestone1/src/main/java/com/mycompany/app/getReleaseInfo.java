package com.mycompany.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class getReleaseInfo {
    public static HashMap<LocalDateTime, String> releaseNames;
    public static HashMap<LocalDateTime, String> releaseID;
    public static ArrayList<LocalDateTime> releases;
    public static List<String> relNames = new ArrayList<>();    // lista dei nomi delle release ordinate

    /**
     * Popola le lista 'releases' e la ordina, ignorando quelle senza data
     * Popola 'relNames' e scarta l'ultimo 50% */
    public static void retrieveReleases() throws IOException, JSONException {

        String url = "https://issues.apache.org/jira/rest/api/2/project/" + "BOOKKEEPER";
        JSONObject json = readJsonFromUrl(url);
        JSONArray versions = json.getJSONArray("versions");

        releases = new ArrayList<>();
        releaseNames = new HashMap<>();
        releaseID = new HashMap<>();

        for (int i = 0; i < versions.length(); i++) {
            String name = "";
            String id = "";
            if (versions.getJSONObject(i).has("releaseDate")) {
                if (versions.getJSONObject(i).has("name"))
                    name = versions.getJSONObject(i).get("name").toString();
                if (versions.getJSONObject(i).has("id"))
                    id = versions.getJSONObject(i).get("id").toString();
                addRelease(versions.getJSONObject(i).get("releaseDate").toString(), name, id);
            }
        }

        // compone il nome completo delle release
        for(LocalDateTime ldt : releases){
            for(LocalDateTime l : releaseNames.keySet()) {
                if(l.equals(ldt))
                    relNames.add("refs/tags/release-" + releaseNames.get(l));
            }
        }

        // ordina le release e scarta l'ultimo 50%
        relNames.sort((s1, s2) -> {
            String[] s1Parts = s1.split("-");
            String[] s2Parts = s2.split("-");
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
        int len = relNames.size();
        if (len > len / 2 + 1) {
            relNames.subList(len / 2 + 1, len).clear();
        }
    }

    public static void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        if (!releases.contains(dateTime))
            releases.add(dateTime);
        releaseNames.put(dateTime, name);
        releaseID.put(dateTime, id);
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