package com.mycompany.app.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Initializer {
    private static List<String> projectNames = null;
    private static List<String> repoPath = null;
    private static Initializer instance = null;
    private static String logFileName = null;

    private Initializer() {}

    public static void getInstance() throws IOException {
        if(instance==null) {
            instance = new Initializer();
            init();
        }
    }

    public static List<String> getProjectNames(){
        return projectNames;
    }

    public static List<String> getRepoPath(){
        return repoPath;
    }

    public static String getLogFileName() {
        return logFileName;
    }

    private static void init() throws IOException {
        String path = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "config" + File.separator + "config.json";
        IO.clean();

        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("Configuration file not found!");
        }
        try (Scanner scanner = new Scanner(file)) {
            String myJson = scanner.useDelimiter("\\Z").next();
            JSONObject config = new JSONObject(myJson);
            JSONArray names = config.names();

            logFileName = config.getString(names.getString(0));
            projectNames = convertJSONArrayListString(config, names.getString(1));
            repoPath = convertJSONArrayListString(config, names.getString(2));
        }
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
