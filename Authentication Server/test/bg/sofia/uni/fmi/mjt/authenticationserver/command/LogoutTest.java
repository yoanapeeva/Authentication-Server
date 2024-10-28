package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LogoutTest {
    private static final String correctCommand = "logout --session-id <sessionId>";
    private static final String incorrectCommand = "logout --Session-id <sessionId>";
    private static Logout logout;
    @Mock
    private Database database;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private User user;
    @Mock
    private Map<String, User> users;

    @BeforeAll
    public static void setCorrectLogoutCommand() throws InvalidCommand {
        logout = new Logout(correctCommand);
    }

    @Test
    public void testCreateLogoutCommandNull() {
        assertThrows(InvalidCommand.class, () -> new Logout(null),
            "It is not thrown exception when the command is null.");
    }

    @Test
    public void testLogoutCheckValidCommandInvalidFormat() {
        assertThrows(InvalidCommand.class, () -> new Logout(incorrectCommand),
            "It is not thrown exception when the command is with incorrect format.");
    }

    @Test
    public void testExecuteLogoutLoggedOutUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        logout.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, logout.getCommandStatus());
        assertTrue(logout.getStatusMessage().endsWith("is already logged out."));
    }

    @Test
    public void testExecuteLogoutInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        logout.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, logout.getCommandStatus());
        assertTrue(logout.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteLogout() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("username");
        when(database.getUsers()).thenReturn(users);
        when(users.get(anyString())).thenReturn(user);

        logout.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, logout.getCommandStatus());
        assertEquals("The logout is successful.", logout.getStatusMessage());
    }
}
