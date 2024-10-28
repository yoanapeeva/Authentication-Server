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

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RegisterTest {
    private static final String correctCommand =
        "register --username <username> --password <password> --first-name <firstName> --last-name <lastName> --email <email>";
    private static final String incorrectCommand =
        "register --username <username> --password <password> --First-name <firstName> --last-name <lastName> --email <email>";
    private static Register register;
    @Mock
    private Database database;
    @Mock
    private SessionManager sessionManager;
    @Mock
    private User user;
    @Mock
    private Map<String, User> users;

    @BeforeAll
    public static void setCorrectRegisterCommand() throws InvalidCommand {
        register = new Register(correctCommand);
    }

    @Test
    public void testCreateRegisterCommandNull() {
        assertThrows(InvalidCommand.class, () -> new Register(null),
            "It is not thrown exception when the command is null.");
    }

    @Test
    public void testRegisterCheckValidCommandInvalidFormat() {
        assertThrows(InvalidCommand.class, () -> new Register(incorrectCommand),
            "It is not thrown exception when the command is with incorrect format.");
    }

    @Test
    public void testExecuteRegisterExistingUser() {
        when(database.getUserByUsername(anyString())).thenReturn(user);

        register.execute(database, sessionManager);

        assertEquals(CommandStatus.UNSUCCESSFUL, register.getCommandStatus());
        assertTrue(register.getStatusMessage().startsWith("The registry is unsuccessful."));
    }

    @Test
    public void testExecuteRegister() {
        when(database.getUserByUsername(anyString())).thenReturn(null);
        when(database.getUsers()).thenReturn(users);
        when(users.get(anyString())).thenReturn(user);

        register.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, register.getCommandStatus());
        assertTrue(register.getStatusMessage().startsWith("The registry is successful. Your current session Id is: "));
    }

    @Test
    public void testExecuteRegisterFirstUser() {
        when(database.getUserByUsername(anyString())).thenReturn(null).thenReturn(user);
        when(database.getUsers()).thenReturn(users);
        when(database.getUsers().size()).thenReturn(1);
        when(users.get(anyString())).thenReturn(user);

        register.execute(database, sessionManager);

        assertEquals(CommandStatus.SUCCESSFUL, register.getCommandStatus());
        assertTrue(register.getStatusMessage().endsWith("You have been granted with administrative permissions."));
    }

    @Test
    public void testRegisterCreateStartEvent() {
        when(user.getUsername()).thenReturn("<username>");
        String ipAddress = "ipAddress";

        Optional<Event> result = register.createStartEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof StartEvent);
        StartEvent startEvent = (StartEvent) event;

        assertEquals(CommandBehavior.REGISTER, startEvent.getCommandBehavior());
        assertEquals(user.getUsername(), startEvent.getUsername());
        assertEquals(ipAddress, startEvent.getIpAddress());
        assertEquals("Registration of the user " + user.getUsername() + ".", startEvent.getDescription());
    }

    @Test
    public void testRegisterCreateEndEventUnsuccessful() {
        when(database.getUserByUsername(anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("<username>");

        register.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = register.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.REGISTER, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals(register.getStatusMessage(), endEvent.getDescription());
    }

    @Test
    public void testRegisterCreateEndEventSuccessful() {
        when(database.getUserByUsername(anyString())).thenReturn(null);
        when(database.getUsers()).thenReturn(users);
        when(users.get(anyString())).thenReturn(user);
        when(user.getUsername()).thenReturn("<username>");

        register.execute(database, sessionManager);

        String ipAddress = "ipAddress";

        Optional<Event> result = register.createEndEvent(ipAddress, database, sessionManager);

        assertTrue(result.isPresent());
        Event event = result.get();

        assertTrue(event instanceof EndEvent);
        EndEvent endEvent = (EndEvent) event;

        assertEquals(CommandBehavior.REGISTER, endEvent.getCommandBehavior());
        assertEquals(user.getUsername(), endEvent.getUsername());
        assertEquals(ipAddress, endEvent.getIpAddress());
        assertEquals("The registry of the user " + user.getUsername() + " is successful.",
            endEvent.getDescription());
    }
}
