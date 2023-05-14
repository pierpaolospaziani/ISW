package com.mycompany.app.utils;

import java.io.*;

public class IO {

    private IO() {
        throw new IllegalStateException("Utility class");
    }

    public static void appendOnLog(String whatToWrite){
        try{
            String dir = "src" +  File.separator + "main" + File.separator + Initializer.getLogFileName();
            FileWriter fileWriter = new FileWriter(dir,true);
            fileWriter.append(whatToWrite).append("\n");
            fileWriter.flush();
            fileWriter.close();
        } catch(IOException i){
            i.printStackTrace();
        }
    }

    public static void clean() {
        try{
            String dir = "src" +  File.separator + "main" + File.separator + "log.txt";
            FileWriter fileWriter = new FileWriter(dir);
            fileWriter.append("");
            fileWriter.flush();
            fileWriter.close();
        } catch(IOException i){
            i.printStackTrace();
        }
    }
}
