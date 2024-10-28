package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.EndEvent;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.StartEvent;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;

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
public class ResetPasswordTest {
    private static final String correctCommand =
        "reset-password --session-id <session-id> --username <username> --old-password <password> --new-password <newPassword>";
    private static final String incorrectCommand =
        "reset-password --session-id <session-id> --username <username> --Old-password <oldPassword> --new-password <newPassword>";
    private static ResetPassword resetPassword;
    @Mock
    private User user;
    @Mock
    private Database database;
    @Mock
    private SessionManager sessionManager;

    @BeforeAll
    public static void setCorrectResetPasswordCommand() throws InvalidCommand {
        resetPassword = new ResetPassword(correctCommand);
    }

    @Test
    public void testCreateResetPasswordCommandNull() {
        assertThrows(InvalidCommand.class, () -> new ResetPassword(null),
            "It is not thrown exception when the command is null.");
    }

    @Test
    public void testResetPasswordCheckValidCommandInvalidFormat() {
        assertThrows(InvalidCommand.class, () -> new ResetPassword(incorrectCommand),
            "It is not thrown exception when the command is with incorrect format.");
    }

    @Test
    public void testExecuteResetPasswordLoggedOutUser() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        resetPassword.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, resetPassword.getCommandStatus());
        assertTrue(resetPassword.getStatusMessage().endsWith("is logged out."));
    }

    @Test
    public void testExecuteResetPasswordInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(false);

        resetPassword.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, resetPassword.getCommandStatus());
        assertTrue(resetPassword.getStatusMessage().endsWith("doesn't exist."));
    }

    @Test
    public void testExecuteResetPasswordIncorrectUsername() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("incorrectUsername");

        resetPassword.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, resetPassword.getCommandStatus());
        assertTrue(resetPassword.getStatusMessage()
            .startsWith("The password reset is unsuccessful. The username is not correct for session Id"));
    }

    @Test
    public void testExecuteResetPasswordIncorrectPassword() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("<username>");
        when(user.getPassword()).thenReturn("incorrectPassword");

        resetPassword.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, resetPassword.getCommandStatus());
        assertEquals("The password reset is unsuccessful. The password is not correct.",
            resetPassword.getStatusMessage());
    }

    @Test
    public void testExecuteResetPassword() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("<username>");
        when(user.getPassword()).thenReturn("<password>");

        resetPassword.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, resetPassword.getCommandStatus());
        assertEquals("The password reset is successful.",
            resetPassword.getStatusMessage());
    }

    @Test
    public void testResetPasswordCreateStartEventValidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("<username>");

        String ipAddress = "ipAddress";

        Optional<Event> result = resetPassword.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.RESET_PASSWORD, startEvent.getCommandBehavior());
        assertEquals(user.getUsername(), startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertEquals("Password reset of the user an username " + user.getUsername() + ".", startEvent.getDescription());
    }

    @Test
    public void testResetPasswordCreateStartEventInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);

        String ipAddress = "ipAddress";
        String unknown = "UNKNOWN";

        Optional<Event> result = resetPassword.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.RESET_PASSWORD, startEvent.getCommandBehavior());
        assertEquals(unknown, startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertTrue(startEvent.getDescription().startsWith("Password reset of the user with session Id"));
    }

    @Test
    public void testResetPasswordCreateEndEventUnsuccessfulValidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("incorrectUsername").thenReturn("<username>");

        resetPassword.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = resetPassword.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.RESET_PASSWORD, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(resetPassword.getStatusMessage(), endEvent.getDescription());
    }

    @Test
    public void testResetPasswordCreateEndEventUnsuccessfulInvalidSessionId() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(false);
        when(sessionManager.checkLastSessionBySessionId(anyString())).thenReturn(true);

        resetPassword.execute(database, sessionManager);

        String unknown = "UNKNOWN";
        String ipAddress = "ipAddress";

        Optional<Event> result = resetPassword.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.RESET_PASSWORD, endEvent.getCommandBehavior());
        assertEquals(unknown, endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(resetPassword.getStatusMessage(), endEvent.getDescription());
    }

    @Test
    public void testResetPasswordCreateEndEventSuccessful() {
        when(sessionManager.isSessionValid(anyString())).thenReturn(true);
        when(database.getUserBySessionId(eq(sessionManager), anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("<username>");
        when(user.getPassword()).thenReturn("<password>");

        resetPassword.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = resetPassword.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.RESET_PASSWORD, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertTrue(
            endEvent.getDescription()
                .startsWith("The update is successful for the user with username " + user.getUsername() + "."));
    }
}
