package com.mycompany.app.utils;

import java.io.*;

public class IO {

    private IO() {
        throw new IllegalStateException("Utility class");
    }

    public static void appendOnLog(String whatToWrite) throws IOException {
        FileWriter fileWriter = new FileWriter(Initializer.getLogFileName());
        fileWriter.append(whatToWrite).append("\n");
        fileWriter.flush();
        fileWriter.close();
    }

    public static void clean() throws IOException {
        FileWriter fileWriter = new FileWriter("log.txt");
        fileWriter.append("");
        fileWriter.flush();
        fileWriter.close();
    }
}
