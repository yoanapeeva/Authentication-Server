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
public class RemoveAdminUserTest {
    private static final String correctCommand = "remove-admin-user --session-id <sessionId> --username <username>";
    private static final String incorrectCommand = "remove-admin-user --session-id <sessionId> --Username <username>";
    private static RemoveAdminUser removeAdminUser;
    @Mock
    private Database database;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private User user;
    @Mock
    private Map<String, User> admins;

    @BeforeAll
    public static void setCorrectRemoveAdminUserCommand() throws InvalidCommand {
        removeAdminUser = new RemoveAdminUser(correctCommand);
    }

    @Test
    public void testCreateRemoveAdminUserCommandNull() {
        assertThrows(InvalidCommand.class, () -> new RemoveAdminUser(null),
            "It is not thrown exception when the command is null.");
    }

    @Test
    public void testRemoveAdminUserCheckValidCommandInvalidFormat() {
        assertThrows(InvalidCommand.class, () -> new RemoveAdminUser(incorrectCommand),
            "It is not thrown exception when the command is with incorrect format.");
    }

    @Test
    public void testExecuteRemoveAdminUserLoggedOutUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        removeAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, removeAdminUser.getCommandStatus());
        assertTrue(removeAdminUser.getStatusMessage().endsWith("is logged out."));
    }

    @Test
    public void testExecuteRemoveAdminUserInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        removeAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, removeAdminUser.getCommandStatus());
        assertTrue(removeAdminUser.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteRemoveAdminUserNonAdmin() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.USER);

        removeAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, removeAdminUser.getCommandStatus());
        assertTrue(removeAdminUser.getStatusMessage().endsWith("doesn't have administrative permissions."));
    }

    @Test
    public void testExecuteRemoveAdminUserInvalidUsername() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN);

        removeAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, removeAdminUser.getCommandStatus());
        assertTrue(removeAdminUser.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteRemoveAdminUserNotCurrentlyAdmin() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN).thenReturn(UserAuthorization.USER);
        when(database.getUserByUsername(anyString())).thenReturn(user);

        removeAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, removeAdminUser.getCommandStatus());
        assertTrue(removeAdminUser.getStatusMessage().endsWith("is currently not an admin."));
    }

    @Test
    public void testExecuteRemoveAdminUserLastAdmin() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN);
        when(database.getUserByUsername(anyString())).thenReturn(user);
        when(database.getAdmins()).thenReturn(admins);
        when(admins.size()).thenReturn(1);

        removeAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, removeAdminUser.getCommandStatus());
        assertEquals("The removing of the admin is unsuccessful. There is only one admin left.",
            removeAdminUser.getStatusMessage());
    }

    @Test
    public void testExecuteRemoveAdminUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN);
        when(database.getUserByUsername(anyString())).thenReturn(user);
        when(database.getAdmins()).thenReturn(admins);

        removeAdminUser.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, removeAdminUser.getCommandStatus());
        assertTrue(removeAdminUser.getStatusMessage().endsWith("is successful."));
    }

    @Test
    public void testRemoveAdminUserCreateStartEventValidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("usernameAdmin");

        String ipAddress = "ipAddress";

        Optional<Event> result = removeAdminUser.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.REMOVE_ADMIN_USER, startEvent.getCommandBehavior());
        assertEquals(user.getUsername(), startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertTrue(startEvent.getDescription().endsWith(" by the user " + user.getUsername() + "."));
    }

    @Test
    public void testRemoveAdminUserCreateStartEventInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);

        String ipAddress = "ipAddress";
        String unknown = "UNKNOWN";

        Optional<Event> result = removeAdminUser.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.REMOVE_ADMIN_USER, startEvent.getCommandBehavior());
        assertEquals(unknown, startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertTrue(startEvent.getDescription().contains(" by the user with  session Id:"));
    }

    @Test
    public void testRemoveAdminUserCreateEndEventUnsuccessfulValidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.USER);
        when(user.getUsername()).thenReturn("usernameAdmin");

        removeAdminUser.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = removeAdminUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.REMOVE_ADMIN_USER, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(removeAdminUser.getStatusMessage(), endEvent.getDescription());
    }

    @Test
    public void testRemoveAdminUserCreateEndEventUnsuccessfulInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        removeAdminUser.execute(database, sessionManager);

        String unknown = "UNKNOWN";
        String ipAddress = "ipAddress";

        Optional<Event> result = removeAdminUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.REMOVE_ADMIN_USER, endEvent.getCommandBehavior());
        assertEquals(unknown, endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(removeAdminUser.getStatusMessage(), endEvent.getDescription());
    }

    @Test
    public void testRemoveAdminUserCreateEndEventSuccessful() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getAuthorization()).thenReturn(UserAuthorization.ADMIN);
        when(database.getUserByUsername(anyString())).thenReturn(user);
        when(database.getAdmins()).thenReturn(admins);

        removeAdminUser.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = removeAdminUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.REMOVE_ADMIN_USER, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(removeAdminUser.getStatusMessage(), endEvent.getDescription());
    }
}
