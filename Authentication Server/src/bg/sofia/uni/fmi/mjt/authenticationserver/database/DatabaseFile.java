package bg.sofia.uni.fmi.mjt.authenticationserver.database;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.cipher.CipherPassword;
import bg.sofia.uni.fmi.mjt.authenticationserver.troubleshootlog.TroubleshootLog;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.List;

public class DatabaseFile {
    private static final String LOG_FILE_PATH = "database.txt";
    private static final Gson GSON = new Gson();
    private static PrintWriter writer;

    private static void createNewWriter() {
        try {
            File logFile = new File(LOG_FILE_PATH);
            if (writer != null) {
                writer.close();
            }
            if (logFile.exists()) {
                logFile.delete();
            }
            logFile.createNewFile();
            writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
        } catch (IOException e) {
            System.out.println("Failed to create DatabaseFile." +
                "Try again later or contact administrator by providing the logs in " +
                TroubleshootLog.getLogFilePath());
            TroubleshootLog.getInstance()
                .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                    Arrays.toString(e.getStackTrace()) + ".");
        }
    }

    public static void getDatabase() {
        createNewWriter();
        List<User> users = Database.getInstance().getUsers().values()
            .stream()
            .toList();
        for (User user : users) {
            String password = user.getPassword();
            String encryptedPassword = CipherPassword.encryptPassword(password);
            if (encryptedPassword != null) {
                user.setPassword(encryptedPassword);
                String json = GSON.toJson(user);
                writer.write(json + System.lineSeparator());
                writer.flush();
            } else {
                close();
                return;
            }
        }
        close();
    }

    public static String getLogFilePath() {
        return LOG_FILE_PATH;
    }

    private static void close() {
        writer.close();
    }
}
