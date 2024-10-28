package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.EndEvent;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.StartEvent;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;
import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthorization;

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
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteUserTest {
    private static final String correctCommand = "delete-user --session-id <sessionId> --username <username>";
    private static final String incorrectCommand = "delete-user --session-id <sessionId> --Username <username>";
    private static DeleteUser deleteUser;
    @Mock
    private Database database;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private User user;
    @Mock
    private Map<String, User> admins;

    @BeforeAll
    public static void setCorrectDeleteUserCommand() throws InvalidCommand {
        deleteUser = new DeleteUser(correctCommand);
    }

    @Test
    public void testCreateDeleteUserCommandNull() {
        assertThrows(InvalidCommand.class, () -> new DeleteUser(null),
            "It is not thrown exception when the command is null.");
    }

    @Test
    public void testDeleteUserCheckValidCommandInvalidFormat() {
        assertThrows(InvalidCommand.class, () -> new DeleteUser(incorrectCommand),
            "It is not thrown exception when the command is with incorrect format.");
    }

    @Test
    public void testExecuteDeleteUserLoggedOutUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        deleteUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, deleteUser.getCommandStatus());
        assertTrue(deleteUser.getStatusMessage().endsWith("is logged out."));
    }

    @Test
    public void testExecuteDeleteUserInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        deleteUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, deleteUser.getCommandStatus());
        assertTrue(deleteUser.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteDeleteUserNonAdmin() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.USER);

        deleteUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, deleteUser.getCommandStatus());
        assertTrue(deleteUser.getStatusMessage().endsWith("doesn't have administrative permissions."));
    }

    @Test
    public void testExecuteDeleteUserInvalidUsername() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN);

        deleteUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, deleteUser.getCommandStatus());
        assertTrue(deleteUser.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteDeleteUserLastAdmin() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN);
        when(database.getUserByUsername(anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("<username>");
        when(database.getAdmins()).thenReturn(admins);
        when(admins.size()).thenReturn(1);

        deleteUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, deleteUser.getCommandStatus());
        assertTrue(deleteUser.getStatusMessage().endsWith("is the only left admin."));
    }

    @Test
    public void testExecuteDeleteUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN);
        when(database.getUserByUsername(anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("usernameAdmin");

        deleteUser.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, deleteUser.getCommandStatus());
        assertTrue(deleteUser.getStatusMessage().endsWith("is successful."));
    }

    @Test
    public void testDeleteUserCreateStartEventValidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("usernameAdmin");

        String ipAddress = "ipAddress";

        Optional<Event> result = deleteUser.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.DELETE_USER, startEvent.getCommandBehavior());
        assertEquals(user.getUsername(), startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertTrue(startEvent.getDescription().endsWith(" by the user " + user.getUsername() + "."));
    }

    @Test
    public void testDeleteUserCreateStartEventInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);

        String ipAddress = "ipAddress";
        String unknown = "UNKNOWN";

        Optional<Event> result = deleteUser.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.DELETE_USER, startEvent.getCommandBehavior());
        assertEquals(unknown, startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertTrue(startEvent.getDescription().contains(" by the user with  session Id:"));
    }

    @Test
    public void testDeleteUserCreateEndEventUnsuccessfulValidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.USER);
        when(user.getUsername()).thenReturn("usernameAdmin");

        deleteUser.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = deleteUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.DELETE_USER, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(deleteUser.getStatusMessage(), endEvent.getDescription());
    }

    @Test
    public void testDeleteUserCreateEndEventUnsuccessfulInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        deleteUser.execute(database, sessionManager);

        String unknown = "UNKNOWN";
        String ipAddress = "ipAddress";

        Optional<Event> result = deleteUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.DELETE_USER, endEvent.getCommandBehavior());
        assertEquals(unknown, endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(deleteUser.getStatusMessage(), endEvent.getDescription());
    }

    @Test
    public void testDeleteUserCreateEndEventSuccessful() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN);
        when(database.getUserByUsername(anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("usernameAdmin");

        deleteUser.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = deleteUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.DELETE_USER, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(deleteUser.getStatusMessage(), endEvent.getDescription());
    }
}
