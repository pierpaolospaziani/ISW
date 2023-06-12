package com.mycompany.app.connector;

import com.mycompany.app.model.Issue;
import com.mycompany.app.model.Release;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.mycompany.app.utils.JsonUtils.readJsonFromUrl;

public class JiraIssues {

    private JiraIssues() {
        throw new IllegalStateException("Utility class");
    }

    /** JIRA: costruisce l'ArrayList che contiene tutti i ticket BUG chiusi e fixati */
    public static List<Issue> retrieveIssues(String projectName, List<Release> releaseList) throws IOException {
        ArrayList<Issue> issuesListJira = new ArrayList<>();
        int j;
        int i = 0;
        int total;
        do {
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + projectName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt="
                    + i + "&maxResults=" + j;
            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            for (; i < total && i < j; i++) {
                String key = issues.getJSONObject(i % 1000).get("key").toString();
                Issue issue = new Issue(key);
                issuesListJira.add(issue);
                JSONObject fields = (JSONObject) issues.getJSONObject(i % 1000).get("fields");
                LocalDateTime openingVersionDate = LocalDateTime.parse(fields.get("created").toString().split("\\.")[0]);
                JSONArray fixVersions = fields.getJSONArray("fixVersions");
                JSONArray injectedVersions = fields.getJSONArray("versions");
                setOpeningVersion(issue, openingVersionDate, releaseList);
                setFixVersion(issue, fixVersions, projectName, releaseList);
                setInjectedVersion(issue, injectedVersions, projectName, releaseList);
            }
        } while (i < total);
        return issuesListJira;
    }


    /** Setta l'opening version nel'issue specificata */
    public static void setOpeningVersion(Issue issue, LocalDateTime openingVersionDate, List<Release> releaseList){
        for (Release release : releaseList){
            if (openingVersionDate.isBefore(release.getDate())){
                issue.setOpeningVersion(releaseList.indexOf(release));
                break;
            }
        }
    }


    /** Setta la fix version nel'issue specificata */
    public static void setFixVersion(Issue issue, JSONArray fixVersions, String projectName, List<Release> releaseList){
        if (fixVersions.length() != 0){
            String version = fixVersions.getJSONObject(fixVersions.length()-1).get("name").toString();
            for (Release release : releaseList){
                if (projectName.equals("BOOKKEEPER") && release.getName().split("-")[1].equals(version)
                        || projectName.equals("OPENJPA") && release.getName().split("/")[release.getName().split("/").length-1].equals(version)){
                    issue.setFixedVersion(releaseList.indexOf(release));
                    break;
                }
            }
        }
    }


    /** Setta l'injected version nel'issue specificata */
    public static void setInjectedVersion(Issue issue, JSONArray injectedVersions, String projectName, List<Release> releaseList){
        if (injectedVersions.length() != 0){
            String version = injectedVersions.getJSONObject(0).get("name").toString();
            for (Release release : releaseList){
                if (projectName.equals("BOOKKEEPER") && release.getName().split("-")[1].equals(version)
                        || projectName.equals("OPENJPA") && release.getName().split("/")[release.getName().split("/").length-1].equals(version)){
                    issue.setInjectedVersion(releaseList.indexOf(release));
                    break;
                }
            }
        }
    }
}
