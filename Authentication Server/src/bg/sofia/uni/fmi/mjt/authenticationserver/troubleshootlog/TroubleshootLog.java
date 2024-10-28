package bg.sofia.uni.fmi.mjt.authenticationserver.troubleshootlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TroubleshootLog {
    private static final String LOG_FILE_PATH = "troubleshoot.log.txt";
    private static final TroubleshootLog TROUBLESHOOT_LOG = new TroubleshootLog();
    private static Integer id;
    private final PrintWriter writer;

    private TroubleshootLog() {
        File logFile = new File(LOG_FILE_PATH);
        try {
            if (logFile.exists()) {
                logFile.delete();
            }
            logFile.createNewFile();
            this.writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
            id = 1;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Troubleshoot Log.", e);
        }
    }

    public void log(String message) {
        writer.println(message);
        writer.flush();
        id++;
    }

    public static TroubleshootLog getInstance() {
        return TROUBLESHOOT_LOG;
    }

    public static int getId() {
        return id;
    }

    public static String getLogFilePath() {
        return LOG_FILE_PATH;
    }

    public void close() {
        writer.close();
    }
}
