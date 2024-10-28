package bg.sofia.uni.fmi.mjt.authenticationserver.server;

import bg.sofia.uni.fmi.mjt.authenticationserver.communication.input.Input;

import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandBehavior;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandStatus;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandType;

import bg.sofia.uni.fmi.mjt.authenticationserver.communication.output.Output;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServerCommandExecutorTest {
    private static final String correctRegisterCommand =
        "register --username <username> --password <password> --first-name <firstName> --last-name <lastName> --email <email>";
    private static final String incorrectRegisterCommand =
        "register --username <username> --password <password> --First-name <firstName> --last-name <lastName> --email <email>";
    private static final String correctLoginByUsernameCommand = "login --username <username> --password <password>";
    private static final String correctLoginBySessionIdCommand = "login --session-id <sessionId>";
    private static final String correctUpdateUserCommand =
        "update-user --session-id <session-id> --new-username <newUsername> --new-first-name <newFirstName> --new-last-name <newLastName> --new-email <newEmail>";
    private static final String correctResetPasswordCommand =
        "reset-password --session-id <session-id> --username <username> --old-password <oldPassword> --new-password <newPassword>";
    private static final String correctLogoutCommand = "logout --session-id <sessionId>";
    private static final String correctAddAdminUserCommand =
        "add-admin-user --session-id <sessionId> --username <username>";
    private static final String correctRemoveAdminUserCommand =
        "remove-admin-user --session-id <sessionId> --username <username>";
    private static final String correctDeleteUserCommand = "delete-user --session-id <sessionId> --username <username>";
    private static final String correctDownloadDatabaseCommand = "download-database --session-id <sessionId>";
    private static final String invalidCommand = "invalid command";
    private static final String ipAddress = "ipAddress";
    private static ServerCommandExecutor serverCommandExecutor;
    @Mock
    private Input input;

    @BeforeAll
    public static void setUp() {
        serverCommandExecutor = new ServerCommandExecutor();
    }

    @Test
    public void testExecuteRegisterCommandValidUnsecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.UNSECURE);
        when(input.message()).thenReturn(correctRegisterCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.REGISTER, output.commandBehaviour());
        assertEquals(CommandStatus.SUCCESSFUL, output.status());
        assertEquals(false, output.loggedOut());
    }

    @Test
    public void testExecuteRegisterCommandValidSecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.SECURE);
        when(input.message()).thenReturn(correctRegisterCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.INVALID_COMMAND, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(false, output.loggedOut());
    }

    @Test
    public void testExecuteInvalidRegisterCommandValidUnsecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.UNSECURE);
        when(input.message()).thenReturn(incorrectRegisterCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.INVALID_COMMAND, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(false, output.loggedOut());
    }

    @Test
    public void testExecuteLoginByUsernameCommandValidUnsecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.UNSECURE);
        when(input.message()).thenReturn(correctLoginByUsernameCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.LOGIN_BY_USERNAME, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(false, output.loggedOut());
    }

    @Test
    public void testExecuteLoginBySessionIdCommandValidUnsecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.UNSECURE);
        when(input.message()).thenReturn(correctLoginBySessionIdCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.LOGIN_BY_SESSION_ID, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(false, output.loggedOut());
    }

    @Test
    public void testExecuteUpdateUserCommandValidSecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.SECURE);
        when(input.message()).thenReturn(correctUpdateUserCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.UPDATE_USER, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(output.loggedOut(), false);
    }

    @Test
    public void testExecuteUpdateUserCommandValidUnsecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.UNSECURE);
        when(input.message()).thenReturn(correctUpdateUserCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.INVALID_COMMAND, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(false, output.loggedOut());
    }

    @Test
    public void testExecuteResetPasswordCommandValidSecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.SECURE);
        when(input.message()).thenReturn(correctResetPasswordCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.RESET_PASSWORD, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(output.loggedOut(), false);
    }

    @Test
    public void testExecuteLogoutCommandValidSecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.SECURE);
        when(input.message()).thenReturn(correctLogoutCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.LOGOUT, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(output.loggedOut(), false);
    }

    @Test
    public void testExecuteAddAdminUserCommandValidSecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.SECURE);
        when(input.message()).thenReturn(correctAddAdminUserCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.ADD_ADMIN_USER, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(output.loggedOut(), false);
    }

    @Test
    public void testExecuteRemoveAdminUserCommandValidSecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.SECURE);
        when(input.message()).thenReturn(correctRemoveAdminUserCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.REMOVE_ADMIN_USER, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(output.loggedOut(), false);
    }

    @Test
    public void testExecuteDeleteUserCommandValidSecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.SECURE);
        when(input.message()).thenReturn(correctDeleteUserCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.DELETE_USER, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(output.loggedOut(), false);
    }

    @Test
    public void testExecuteDownloadDatabaseCommandValidSecureCommandInput() {
        when(input.commandType()).thenReturn(CommandType.SECURE);
        when(input.message()).thenReturn(correctDownloadDatabaseCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.DOWNLOAD_DATABASE, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(output.loggedOut(), false);
    }

    @Test
    public void testExecuteInvalidCommand() {
        when(input.commandType()).thenReturn(CommandType.UNSECURE);
        when(input.message()).thenReturn(invalidCommand);

        Output output = serverCommandExecutor.executeCommand(input, ipAddress);

        assertEquals(CommandBehavior.INVALID_COMMAND, output.commandBehaviour());
        assertEquals(CommandStatus.UNSUCCESSFUL, output.status());
        assertEquals(output.loggedOut(), false);
    }
}
