package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.EndEvent;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.StartEvent;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.Session;
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
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateUserTest {
    private static final String correctCommand =
        "update-user --session-id <session-id> --new-username <newUsername> --new-first-name <newFirstName> --new-last-name <newLastName> --new-email <newEmail>";
    private static final String incorrectCommand =
        "update-user --session-id <session-id> --new-username <newUsername> --new-first-name <newFirstName> --new-last-name <newLastName> --New-email <newEmail>";
    private static UpdateUser updateUser;
    @Mock
    private User user;
    @Mock
    private Database database;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private Map<String, User> users;
    @Mock
    private Session session;

    @BeforeAll
    public static void setCorrectUpdateUserCommand() throws InvalidCommand {
        updateUser = UpdateUser.builder(correctCommand).build();
    }

    @Test
    public void testCreateUpdateUserCommandNull() {
        assertThrows(InvalidCommand.class, () -> UpdateUser.builder(null).build(),
            "It is not thrown exception when the command is null.");
    }

    @Test
    public void testUpdateUserCheckValidCommandInvalidFormat() {
        assertThrows(InvalidCommand.class, () -> UpdateUser.builder(incorrectCommand).build(),
            "It is not thrown exception when the command is with incorrect format.");
    }

    @Test
    public void testExecuteUpdateUserLoggedOutUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        updateUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, updateUser.getCommandStatus());
        assertTrue(updateUser.getStatusMessage().endsWith("is logged out."));
    }

    @Test
    public void testExecuteUpdateUserInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        updateUser.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, updateUser.getCommandStatus());
        assertTrue(updateUser.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteUpdateUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(database.getUsers()).thenReturn(users);
        when(sessionManager.getSessionBySessionId(anyString())).thenReturn(session);

        updateUser.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, updateUser.getCommandStatus());
        assertEquals("The update is successful.", updateUser.getStatusMessage());
    }

    @Test
    public void testUpdateUserCreateStartEventValidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);

        String ipAddress = "ipAddress";

        Optional<Event> result = updateUser.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.UPDATE_USER, startEvent.getCommandBehavior());
        assertEquals(user.getUsername(), startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertEquals("Update of user with an username " + user.getUsername() + ".", startEvent.getDescription());
    }

    @Test
    public void testUpdateUserCreateStartEventInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        String ipAddress = "ipAddress";
        String unknown = "UNKNOWN";

        Optional<Event> result = updateUser.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.UPDATE_USER, startEvent.getCommandBehavior());
        assertEquals(unknown, startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertTrue(startEvent.getDescription().startsWith("Update of the user with session Id"));
    }

    @Test
    public void testUpdateUserCreateEndEventUnsuccessful() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        updateUser.execute(database, sessionManager);

        String ipAddress = "ipAddress";
        String unknown = "UNKNOWN";

        Optional<Event> result = updateUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.UPDATE_USER, endEvent.getCommandBehavior());
        assertEquals(unknown, endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(updateUser.getStatusMessage(), endEvent.getDescription());
    }

    @Test
    public void testUpdateUserCreateEndEventSuccessful() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(database.getUsers()).thenReturn(users);
        when(sessionManager.getSessionBySessionId(anyString())).thenReturn(session);

        updateUser.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = updateUser.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.UPDATE_USER, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertTrue(
            endEvent.getDescription().startsWith("The update is successful. The new information about the user is"));
    }
}
