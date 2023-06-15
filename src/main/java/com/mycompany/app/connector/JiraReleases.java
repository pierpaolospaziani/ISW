package com.mycompany.app.connector;

import com.mycompany.app.model.Release;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.mycompany.app.utils.JsonUtils.readJsonFromUrl;

public class JiraReleases {

    private JiraReleases() {
        throw new IllegalStateException("Utility class");
    }

    /** JIRA: recupera la prima metà della lista di release ordinata */
    public static List<Release> retrieveReleases(Repository repository, String projName) throws IOException, JSONException {

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

        createReleases(entryList, releaseList);

        return releaseList;
    }


    private static void createReleases(List<Map.Entry<String, LocalDateTime>> entryList, List<Release> releaseList) {
        for (Map.Entry<String, LocalDateTime> entry : entryList) {
            Release release = new Release(entry.getKey(), entry.getValue());
            releaseList.add(release);
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
}