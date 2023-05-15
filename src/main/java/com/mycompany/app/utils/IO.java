package com.mycompany.app.utils;

import java.io.*;

public class IO {

    private IO() {
        throw new IllegalStateException("Utility class");
    }

    public static void appendOnLog(String whatToWrite) throws IOException {
        try (FileWriter fileWriter = new FileWriter(Initializer.getLogFileName())) {
            fileWriter.append(whatToWrite).append("\n");
            fileWriter.flush();
        }
    }

    public static void clean() throws IOException {
        try (FileWriter fileWriter = new FileWriter("log.txt")) {
            fileWriter.append("");
            fileWriter.flush();
        }
    }
}
