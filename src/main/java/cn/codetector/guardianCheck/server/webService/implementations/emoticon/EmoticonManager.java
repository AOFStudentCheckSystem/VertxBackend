package cn.codetector.guardianCheck.server.webService.implementations.emoticon;

import java.io.*;
import java.util.ArrayList;

public class EmoticonManager {

    private static ArrayList<String> ems = new ArrayList<>();

    static {
        //general
        try (BufferedReader br = new BufferedReader(new FileReader(new File(ExportResource("/emoticon.list"))))) {
            String line;
            while ((line = br.readLine()) != null) {
                ems.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String ExportResource(String resourceName) throws Exception {
        InputStream stream = null;
        OutputStream resStreamOut = null;
        String jarFolder;
        try {
            stream = EmoticonManager.class.getResourceAsStream(resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            jarFolder = new File(EmoticonManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath().replace('\\', '/');
            if (!new File(jarFolder + resourceName).exists()) {
                resStreamOut = new FileOutputStream(jarFolder + resourceName);
                while ((readBytes = stream.read(buffer)) > 0) {
                    resStreamOut.write(buffer, 0, readBytes);
                }
            }
        } finally {
            assert stream != null;
            stream.close();
            if (resStreamOut != null)
                resStreamOut.close();
        }
        return jarFolder + resourceName;
    }

    public static String get() {
        if (ems.isEmpty()) {
            return "404 QAQ, Sam Xie failed to find the page you requested";
        }
        return ems.get((int) (Math.random() * ems.size()));
    }
}