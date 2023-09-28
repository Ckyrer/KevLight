package ru.kvdl.kevlight;

import java.io.File;
import java.io.FileInputStream;

public class DHOperator {
    public static String readFile(File file) {
        String data = "";
        try {
            FileInputStream in = new FileInputStream(file);
            data = new String(in.readAllBytes());
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public static byte[] readFileBytes(File file) {
        byte[] data = null;
        try {
            FileInputStream in = new FileInputStream(file);
            data = in.readAllBytes();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
    public static String readFile(String path) {
        String data = "";
        try {
            FileInputStream in = new FileInputStream(path);
            data = new String(in.readAllBytes());
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public static boolean isFileExist(String path) {
        File res = new File(path);
        if (res.exists() && !res.isDirectory()) {
            return true;
        }
        return false;
    }

    public static String buildPage(String path) {
        File t = new File(path);
        if (t.exists() && t.isDirectory()) {
            String result = readFile(path+"/index.html");
            if (isFileExist(path+"/main.js")) {
                int edge = result.lastIndexOf("</body>");
                result = result.substring(0, edge)+"<script>"+readFile(path+"/main.js")+"</script>"+result.substring(edge);
            }

            if (isFileExist(path+"/style.css")) {
                int edge = result.lastIndexOf("</head>");
                result = result.substring(0, edge)+"<style>"+readFile(path+"/style.css")+"</style>"+result.substring(edge);
            }
            
            return result;
        } else {
            return "You don't supposed to see it. Please report about this. (build failed)   ";
        }
    }
}
