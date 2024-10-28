package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.FailedLogin;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginByUsernameTest {
    private static final String correctCommand = "login --username <username> --password <password>";
    private static final String incorrectCommand = "login --username <username> --Password <password>";
    private static LoginByUsername login;
    @Mock
    private Database database;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private User user;
    @Mock
    private Map<String, User> users;

    @BeforeAll
    public static void setCorrectLoginByUsernameCommand() throws InvalidCommand {
        login = new LoginByUsername(correctCommand);
    }

    @Test
    public void testCreateLoginByUsernameCommandNull() {
        assertThrows(InvalidCommand.class, () -> new LoginByUsername(null),
            "It is not thrown exception when the command is null.");
    }

    @Test
    public void testLoginByUsernameCheckValidCommandInvalidFormat() {
        assertThrows(InvalidCommand.class, () -> new LoginByUsername(incorrectCommand),
            "It is not thrown exception when the command is with incorrect format.");
    }

    @Test
    public void testExecuteLoginByUsernameInvalidUsername() {
        when(database.getUserByUsername(anyString())).thenReturn(null);
        login.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, login.getCommandStatus());
        assertTrue(login.getStatusMessage().startsWith("The login is unsuccessful. An user with the username:"));
    }

    @Test
    public void testExecuteLoginByUsernameIncorrectPassword() {
        when(database.getUserByUsername(anyString())).thenReturn(user);
        when(database.getUsers()).thenReturn(users);
        when(users.get(anyString())).thenReturn(user);
        when(user.getPassword()).thenReturn("<Password>");

        login.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, login.getCommandStatus());
        assertTrue(login.getStatusMessage().startsWith("The login is unsuccessful. The password is incorrect."));
    }

    @Test
    public void testExecuteLoginByUsername() {
        when(database.getUserByUsername(anyString())).thenReturn(user);
        when(database.getUsers()).thenReturn(users);
        when(users.get(anyString())).thenReturn(user);
        when(user.getPassword()).thenReturn("<password>");

        login.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, login.getCommandStatus());
        assertTrue(login.getStatusMessage().startsWith("The login is successful."));
    }

    @Test
    public void testLoginByUsernameFailedLoginEvent() {
        when(database.getUserByUsername(anyString())).thenReturn(null);

        login.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = login.createFailedLoginEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof FailedLogin);
        FailedLogin failedLogin = (FailedLogin) event;
        assertEquals(CommandBehavior.FAILED_LOGIN, failedLogin.getCommandBehavior());
        assertEquals(login.getUsername(), failedLogin.getUsername());
        assertEquals(ipAddress, failedLogin.getIpAddress());
    }

    @Test
    public void testLoginByUsernameFailedLoginEventUnsuccessful() {
        when(database.getUserByUsername(anyString())).thenReturn(user);
        when(database.getUsers()).thenReturn(users);
        when(users.get(anyString())).thenReturn(user);
        when(user.getPassword()).thenReturn("<password>");

        login.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = login.createFailedLoginEvent(ipAddress, database, sessionManager);

        assertEquals(Optional.empty(), result);
    }
}
