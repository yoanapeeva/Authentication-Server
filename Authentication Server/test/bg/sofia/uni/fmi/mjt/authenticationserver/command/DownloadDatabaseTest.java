package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;
import bg.sofia.uni.fmi.mjt.authenticationserver.database.DatabaseFile;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DownloadDatabaseTest {
    private static final String correctCommand = "download-database --session-id <sessionId>";
    private static final String incorrectCommand = "download-database --Session-id <sessionId>";
    private static DownloadDatabase downloadDatabase;
    @Mock
    private Database database;
    @Mock
    private SessionManager sessionManager;

    @BeforeAll
    public static void setCorrectDownloadDatabaseCommand() throws InvalidCommand {
        downloadDatabase = new DownloadDatabase(correctCommand);
    }

    @Test
    public void testCreateDownloadDatabaseCommandNull() {
        assertThrows(InvalidCommand.class, () -> new DownloadDatabase(null),
            "It is not thrown exception when the command is null.");
    }

    @Test
    public void testDownloadDatabaseCheckValidCommandInvalidFormat() {
        assertThrows(InvalidCommand.class, () -> new DownloadDatabase(incorrectCommand),
            "It is not thrown exception when the command is with incorrect format.");
    }

    @Test
    public void testExecuteDownloadDatabaseLoggedOutUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        downloadDatabase.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, downloadDatabase.getCommandStatus());
        assertTrue(downloadDatabase.getStatusMessage().endsWith("is logged out."));
    }

    @Test
    public void testExecuteDownloadDatabaseInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        downloadDatabase.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, downloadDatabase.getCommandStatus());
        assertTrue(downloadDatabase.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteDownloadDatabase() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);

        downloadDatabase.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, downloadDatabase.getCommandStatus());
        assertEquals("The download of the database is successful in the file: " + DatabaseFile.getLogFilePath() + ".",
            downloadDatabase.getStatusMessage());
    }
}
