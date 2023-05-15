package com.mycompany.app.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Initializer {
    private static List<String> PROJECT_NAMES    = null;
    private static List<String> REPO_PATH        = null;
    private static String JIRA_REST_API          = null;
    private static String SEARCH_URL_FIRST_HALF  = null;
    private static String SEARCH_URL_SECOND_HALF = null;
    private static Initializer instance          = null;
    private static String LOG_FILE_NAME          = null;

    private Initializer() {}

    public static void getInstance() throws IOException {
        if(instance==null) {
            instance = new Initializer();
            instance.init();
        }
    }

    public static List<String> getProjectNames(){
        return PROJECT_NAMES;
    }

    public static List<String> getRepoPath(){
        return REPO_PATH;
    }

    public static String getJiraRestApi() {
        return JIRA_REST_API;
    }

    public static String getSearchUrlFirstHalf() {
        return SEARCH_URL_FIRST_HALF;
    }

    public static String getSearchUrlSecondHalf() {
        return SEARCH_URL_SECOND_HALF;
    }

    public static String getLogFileName() {
        return LOG_FILE_NAME;
    }

    private void init() throws IOException {
        String path = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "config" + File.separator + "config.json";
        IO.clean();

        File file = new File(path);
        if (!file.exists()){
                throw new IOException("Configuration file not found!");
        }
        String myJson = new Scanner(file).useDelimiter("\\Z").next();
        JSONObject config = new JSONObject(myJson);
        JSONArray names = config.names();

        JIRA_REST_API          = config.getString(names.getString(0));
        SEARCH_URL_SECOND_HALF = config.getString(names.getString(1));
        LOG_FILE_NAME          = config.getString(names.getString(2));
        PROJECT_NAMES          = convertJSONArrayListString(config,names.getString(3));
        REPO_PATH              = convertJSONArrayListString(config,names.getString(4));
        SEARCH_URL_FIRST_HALF  = config.getString(names.getString(5));
    }

    private static List<String> convertJSONArrayListString(JSONObject obj, String field){
        JSONArray temp = obj.getJSONArray(field);
        List<String> list = new ArrayList<>();
        for(int i = 0; i < temp.length(); i++){
            list.add(temp.getString(i));
        }
        return list;
    }
}
