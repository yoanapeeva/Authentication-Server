package bg.sofia.uni.fmi.mjt.authenticationserver.server;

import bg.sofia.uni.fmi.mjt.authenticationserver.auditlog.AuditLog;

import bg.sofia.uni.fmi.mjt.authenticationserver.communication.input.Input;
import bg.sofia.uni.fmi.mjt.authenticationserver.communication.output.Output;

import bg.sofia.uni.fmi.mjt.authenticationserver.command.AddAdminUser;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.BasicCommand;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandBehavior;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandStatus;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandType;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.DeleteUser;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.DownloadDatabase;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.LoginBySessionId;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.LoginByUsername;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.Logout;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.Register;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.RemoveAdminUser;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.ResetPassword;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.UpdateUser;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.troubleshootlog.TroubleshootLog;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthentication;

import java.util.Arrays;
import java.util.Optional;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerCommandExecutor {
    private final Database database = Database.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AuditLog auditLog = AuditLog.getInstance();
    private final ScheduledExecutorService executor;

    public ServerCommandExecutor() {
        this.executor = Executors.newScheduledThreadPool(10);
        this.executor.scheduleAtFixedRate(this::unauthenticateUsers, 0, 1, TimeUnit.MINUTES);
    }

    private BasicCommand createCommand(String message, CommandBehavior behavior) throws InvalidCommand {
        switch (behavior) {
            case REGISTER -> {
                return new Register(message);
            }
            case LOGIN_BY_USERNAME -> {
                return new LoginByUsername(message);
            }
            case LOGIN_BY_SESSION_ID -> {
                return new LoginBySessionId(message);
            }
            case UPDATE_USER -> {
                return UpdateUser.builder(message).build();
            }
            case RESET_PASSWORD -> {
                return new ResetPassword(message);
            }
            case LOGOUT -> {
                return new Logout(message);
            }
            case ADD_ADMIN_USER -> {
                return new AddAdminUser(message);
            }
            case REMOVE_ADMIN_USER -> {
                return new RemoveAdminUser(message);
            }
            case DELETE_USER -> {
                return new DeleteUser(message);
            }
            case DOWNLOAD_DATABASE -> {
                return new DownloadDatabase(message);
            }
            default -> throw new InvalidCommand("Invalid login command.");
        }
    }

    private BasicCommand parserCommand(String message) throws InvalidCommand {
        String blankSpace = " ";
        String login = "login";
        final String sessionId = "--session-id";
        final String userName = "--username";
        String[] words = message.split(blankSpace);
        final int commandIndicatorPosition = 0;
        final int minCountWords = 3;
        String command = words[commandIndicatorPosition];
        if (words.length < minCountWords) {
            throw new InvalidCommand("Invalid command.");
        }
        if (command.equals(login)) {
            final int loginCommandIndicatorPosition = 1;
            switch (words[loginCommandIndicatorPosition]) {
                case userName -> command += blankSpace + userName;
                case sessionId -> command += blankSpace + sessionId;
                default -> throw new InvalidCommand("Invalid login command.");
            }
        }

        CommandBehavior behavior = CommandBehavior.getCommandByDescription(command);
        if (behavior == null) {
            throw new InvalidCommand("Invalid command format.");
        }
        return createCommand(message, behavior);
    }

    private void handleCommand(BasicCommand command, String ipAddress) {
        if (isLoginCommand(command)) {
            setLoginCommandLogs(command, ipAddress);
        } else if (isEventlessCommand(command)) {
            command.execute(database, sessionManager);
        } else {
            setActionCommandsLogs(command, ipAddress);
        }
    }

    private Output createOutput(BasicCommand command) {
        if (command.getLoggedOut().isPresent()) {
            return new Output(command.getStatusMessage(), command.getCommandStatus(),
                command.getCommandBehavior(), true);
        } else {
            return new Output(command.getStatusMessage(), command.getCommandStatus(),
                command.getCommandBehavior(), false);
        }
    }

    public Output executeCommand(Input input, String ipAddress) {
        try {
            CommandType commandType = input.commandType();
            BasicCommand command = parserCommand(input.message());
            if (!isUnSecureCommand(command, commandType) && !isSecureCommand(command, commandType)) {
                TroubleshootLog.getInstance()
                    .log(TroubleshootLog.getId() +
                        ".Error message: The user tried to execute unsupported command. Ip address: " + ipAddress +
                        ".");
                return new Output("Invalid command. Please enter new command.", CommandStatus.UNSUCCESSFUL,
                    CommandBehavior.INVALID_COMMAND, false);
            }
            handleCommand(command, ipAddress);
            return createOutput(command);
        } catch (InvalidCommand invalidCommand) {
            TroubleshootLog.getInstance()
                .log(TroubleshootLog.getId() + ".Error message: " + invalidCommand.getMessage() + "Ip address: " +
                    ipAddress + "." + System.lineSeparator() + "StackTrace: " +
                    Arrays.toString(invalidCommand.getStackTrace()) + ".");
            return new Output("Invalid command. Please enter new command.", CommandStatus.UNSUCCESSFUL,
                CommandBehavior.INVALID_COMMAND, false);
        }
    }

    private void setActionCommandsLogs(BasicCommand command, String ipAddress) {
        Optional<Event> startEvent = command.createStartEvent(ipAddress, database, sessionManager);
        if (startEvent.isPresent()) {
            auditLog.log(startEvent);
            command.execute(database, sessionManager);
            Optional<Event> endEvent = command.createEndEvent(ipAddress, database, sessionManager);
            auditLog.log(endEvent);
        }
    }

    private void setLoginCommandLogs(BasicCommand command, String ipAddress) {
        command.execute(database, sessionManager);
        Optional<Event> failedLogin = command.createFailedLoginEvent(ipAddress, database, sessionManager);
        if (failedLogin.isPresent()) {
            auditLog.log(failedLogin);
        }
    }

    private boolean isEventlessCommand(BasicCommand command) {
        return command.getCommandBehavior().equals(CommandBehavior.DOWNLOAD_DATABASE) ||
            command.getCommandBehavior().equals(CommandBehavior.LOGOUT);
    }

    private boolean isLoginCommand(BasicCommand command) {
        return command.getCommandBehavior().equals(CommandBehavior.LOGIN_BY_SESSION_ID) ||
            command.getCommandBehavior().equals(CommandBehavior.LOGIN_BY_USERNAME);
    }

    private boolean isSecureCommand(BasicCommand command, CommandType commandType) {
        return (command.getCommandType().equals(CommandType.SECURE) ||
            command.getCommandType().equals(CommandType.ADMIN_ACCESSIBLE)) && commandType.equals(CommandType.SECURE);
    }

    private boolean isUnSecureCommand(BasicCommand command, CommandType commandType) {
        return command.getCommandType().equals(CommandType.UNSECURE) && commandType.equals(CommandType.UNSECURE);
    }

    private synchronized void unauthenticateUsers() {
        sessionManager.getUsersWithExpiredSessions()
            .forEach(
                element -> database.getUsers().get(element).setAuthentication(UserAuthentication.UNAUTHENTICATED));
    }

    public void shutdown() {
        executor.shutdown();
        sessionManager.shutdown();
    }
}
