package com.mycompany.app.utils;

import java.io.*;

public class IO {

    private IO() {
        throw new IllegalStateException("Utility class");
    }

    public static void appendOnLog(String whatToWrite) throws IOException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(Initializer.getLogFileName());
            fileWriter.append(whatToWrite).append("\n");
            fileWriter.flush();
        } catch (IOException i) {
            i.printStackTrace();
        } finally {
            assert fileWriter != null;
            fileWriter.close();
        }
    }

    public static void clean() throws IOException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("log.txt");
            fileWriter.append("");
            fileWriter.flush();
        } catch (IOException i) {
            i.printStackTrace();
        } finally {
            assert fileWriter != null;
            fileWriter.close();
        }
    }
}
