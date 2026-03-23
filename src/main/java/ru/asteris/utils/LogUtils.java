package ru.asteris.utils;

import ru.asteris.Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtils {

    public static void log(String message) {
        if (!Main.getInstance().getConfig().getBoolean("logging.enabled", true)) return;

        File dataFolder = Main.getInstance().getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();

        File logFile = new File(dataFolder, "logs.txt");
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            String time = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
            pw.println("[" + time + "] " + message);
        } catch (IOException ignored) {}
    }
}