package bg.sofia.uni.fmi.mjt.authenticationserver.auditlog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;

import java.util.Arrays;
import java.util.Optional;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;

import bg.sofia.uni.fmi.mjt.authenticationserver.troubleshootlog.TroubleshootLog;

import com.google.gson.Gson;

public class AuditLog {
    private static final String LOG_FILE_PATH = "audit.log.txt";
    private static final AuditLog AUDIT_LOG = new AuditLog();
    private Gson gson;
    private PrintWriter writer;

    private AuditLog() {
        File logFile = new File(LOG_FILE_PATH);
        try {
            if (logFile.exists()) {
                logFile.delete();
            }
            logFile.createNewFile();
            this.writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
            this.gson = new Gson();
        } catch (IOException e) {
            System.out.println("Failed to create AuditLog." +
                "Try again later or contact administrator by providing the logs in " +
                TroubleshootLog.getLogFilePath());
            TroubleshootLog.getInstance()
                .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                    Arrays.toString(e.getStackTrace()) + ".");
        }
    }

    public void log(Optional<Event> event) {
        if (event.isPresent()) {
            Event actualEvent = event.get();
            String eventJson = gson.toJson(actualEvent);
            writer.println(eventJson);
            writer.flush();
        }
    }

    public static AuditLog getInstance() {
        return AUDIT_LOG;
    }

    public void close() {
        writer.close();
    }
}
