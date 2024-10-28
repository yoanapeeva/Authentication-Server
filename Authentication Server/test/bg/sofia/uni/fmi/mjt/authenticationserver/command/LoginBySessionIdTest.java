package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.FailedLogin;
import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginBySessionIdTest {
    private static final String correctCommand = "login --session-id <sessionId>";
    private static final String incorrectCommand = "login --Session-id <sessionId>";
    private static LoginBySessionId login;
    @Mock
    private Database database;
    @Mock
    private SessionManager sessionManager;

    @BeforeAll
    public static void setCorrectLoginBySessionIdCommand() throws InvalidCommand {
        login = new LoginBySessionId(correctCommand);
    }

    @Test
    public void testCreateLoginBySessionIdCommandNull() {
        assertThrows(InvalidCommand.class, () -> new LoginBySessionId(null),
            "It is not thrown exception when the command is null.");
    }

    @Test
    public void testLoginBySessionIdCheckValidCommandInvalidFormat() {
        assertThrows(InvalidCommand.class, () -> new LoginBySessionId(incorrectCommand),
            "It is not thrown exception when the command is with incorrect format.");
    }

    @Test
    public void testExecuteLoginBySessionIdLoggedOutUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        login.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, login.getCommandStatus());
        assertTrue(login.getStatusMessage().endsWith("is logged out."));
    }

    @Test
    public void testExecuteLoginBySessionIdInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        login.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, login.getCommandStatus());
        assertTrue(login.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteLoginBySessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);

        login.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, login.getCommandStatus());
        assertEquals("The login is successful.", login.getStatusMessage());
    }

    @Test
    public void testLoginBySessionIdFailedLoginEvent() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        login.execute(database, sessionManager);

        String ipAddress = "ipAddress";
        String unknown = "UNKNOWN";
        Optional<Event> result = login.createFailedLoginEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof FailedLogin);
        FailedLogin failedLogin = (FailedLogin) event;
        assertEquals(CommandBehavior.FAILED_LOGIN, failedLogin.getCommandBehavior());
        assertEquals(unknown, failedLogin.getUsername());
        assertEquals(ipAddress, failedLogin.getIpAddress());
    }

    @Test
    public void testLoginBySessionIdFailedLoginEventUnsuccessful() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);

        login.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = login.createFailedLoginEvent(ipAddress, database, sessionManager);

        assertEquals(Optional.empty(), result);
    }
}
