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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AddAdminUserTest {
    private static final String correctCommand = "add-admin-user --session-id <sessionId> --username <username>";
    private static final String incorrectCommand = "add-admin-user --session-id <sessionId> --Username <username>";
    private static AddAdminUser addAdminUser;
    @Mock
    private Database database;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private User user;

    @BeforeAll
    public static void setCorrectAddAdminUserCommand() throws InvalidCommand {
        addAdminUser = new AddAdminUser(correctCommand);
    }

    @Test
    public void testCreateAddAdminUserCommandNull() {
        assertThrows(InvalidCommand.class, () -> new AddAdminUser(null),
            "It is not thrown exception when the command is null.");
    }

    @Test
    public void testAddAdminUserCheckValidCommandInvalidFormat() {
        assertThrows(InvalidCommand.class, () -> new AddAdminUser(incorrectCommand),
            "It is not thrown exception when the command is with incorrect format.");
    }

    @Test
    public void testExecuteAddAdminUserLoggedOutUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        addAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, addAdminUser.getCommandStatus());
        assertTrue(addAdminUser.getStatusMessage().endsWith("is logged out."));
    }

    @Test
    public void testExecuteAddAdminUserInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        addAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, addAdminUser.getCommandStatus());
        assertTrue(addAdminUser.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteAddAdminUserNonAdmin() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.USER);

        addAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, addAdminUser.getCommandStatus());
        assertTrue(addAdminUser.getStatusMessage().endsWith("doesn't have administrative permissions."));
    }

    @Test
    public void testExecuteAddAdminUserInvalidUsername() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN);

        addAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, addAdminUser.getCommandStatus());
        assertTrue(addAdminUser.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteAddAdminUserAlreadyAdmin() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN);
        when(database.getUserByUsername(anyString())).thenReturn(user);

        addAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, addAdminUser.getCommandStatus());
        assertTrue(addAdminUser.getStatusMessage().endsWith("is already an admin."));
    }

    @Test
    public void testExecuteAddAdminUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN).thenReturn(UserAuthorization.USER);
        when(database.getUserByUsername(anyString())).thenReturn(user);

        addAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, addAdminUser.getCommandStatus());
        assertTrue(addAdminUser.getStatusMessage().endsWith("is successful."));
    }

    @Test
    public void testAddAdminUserCreateStartEventValidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("usernameAdmin");

        String ipAddress = "ipAddress";

        Optional<Event> result = addAdminUser.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.ADD_ADMIN_USER, startEvent.getCommandBehavior());
        assertEquals(user.getUsername(), startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertTrue(startEvent.getDescription().endsWith(" by the user " + user.getUsername() + "."));
    }

    @Test
    public void testAddAdminUserCreateStartEventInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);

        String ipAddress = "ipAddress";
        String unknown = "UNKNOWN";

        Optional<Event> result = addAdminUser.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.ADD_ADMIN_USER, startEvent.getCommandBehavior());
        assertEquals(unknown, startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertTrue(startEvent.getDescription().contains(" by the user with  session Id:"));
    }

    @Test
    public void testAddAdminUserCreateEndEventUnsuccessfulValidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.USER);
        when(user.getUsername()).thenReturn("usernameAdmin");

        addAdminUser.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = addAdminUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.ADD_ADMIN_USER, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(addAdminUser.getStatusMessage(), endEvent.getDescription());
    }

    @Test
    public void testAddAdminUserCreateEndEventUnsuccessfulInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        addAdminUser.execute(database, sessionManager);

        String unknown = "UNKNOWN";
        String ipAddress = "ipAddress";

        Optional<Event> result = addAdminUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.ADD_ADMIN_USER, endEvent.getCommandBehavior());
        assertEquals(unknown, endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(addAdminUser.getStatusMessage(), endEvent.getDescription());
    }

    @Test
    public void testAddAdminUserCreateEndEventSuccessful() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN).thenReturn(UserAuthorization.USER);
        when(database.getUserByUsername(anyString())).thenReturn(user);

        addAdminUser.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = addAdminUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.ADD_ADMIN_USER, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(addAdminUser.getStatusMessage(), endEvent.getDescription());
    }
}
