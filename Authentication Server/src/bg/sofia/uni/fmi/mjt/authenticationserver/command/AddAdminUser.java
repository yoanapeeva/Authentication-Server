package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.EndEvent;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.StartEvent;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthorization;

import java.util.Optional;

public class AddAdminUser extends BasicCommand {
    private String sessionId;
    private String username;

    public AddAdminUser(String command) throws InvalidCommand {
        super(CommandBehavior.ADD_ADMIN_USER, CommandType.ADMIN_ACCESSIBLE);
        if (command == null) {
            throw new InvalidCommand("The command cannot be null.");
        }

        final int sessionIndex = 2;
        final int userNameIndex = 4;

        String blankSpace = " ";
        String[] commandWords = command.split(blankSpace);

        if (checkValidCommand(commandWords)) {
            this.sessionId = commandWords[sessionIndex];
            this.username = commandWords[userNameIndex];
        }
    }

    @Override
    protected boolean checkValidCommand(String[] command) throws InvalidCommand {
        final int wordsInCommand = 5;
        final int sessionIdIndex = 1;
        final int userNameIndex = 3;

        String sessionId = "--session-id";
        String userName = "--username";

        if (command == null
            || command.length != wordsInCommand
            || !command[sessionIdIndex].equals(sessionId)
            || !command[userNameIndex].equals(userName)) {
            throw new InvalidCommand("Invalid adding admin command.");
        }
        return true;
    }

    private void setUnsuccessfulExecutedCommandInvalidSessionId(SessionManager sessionManager) {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        if (sessionManager.checkLastSessionBySessionId(sessionId)) {
            setLoggedOut(Optional.of(true));
            setStatusMessage(
                "The adding of a new admin is unsuccessful. The user with the session Id: " + sessionId +
                    " is logged out.");
        } else {
            setStatusMessage(
                "The adding of a new admin is unsuccessful. An user with the session Id: " + sessionId +
                    " doesn't exist.");
        }
    }

    private void setUnsuccessfulExecutedCommandNonAdminUser() {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        setStatusMessage(
            "The adding of a new admin is unsuccessful. The user with the session Id: " + sessionId +
                " doesn't have administrative permissions.");
    }

    private void setUnsuccessfulExecutedCommandInvalidUsername() {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        setStatusMessage(
            "The adding of a new admin is unsuccessful. An user with the username " + username + " doesn't exist.");
    }

    private void setUnsuccessfulExecutedCommandAddingCurrentAdminUser() {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        setStatusMessage(
            "The adding of a new admin is unsuccessful. An user with the username " + username +
                " is already an admin.");
    }

    private void setSuccessfulExecutedCommand(Database database) {
        setCommandStatus(CommandStatus.SUCCESSFUL);

        setAuthorizationToUser(database, UserAuthorization.ADMIN, username);

        setStatusMessage(
            "The adding user with username " + username + " is successful.");
    }

    @Override
    public void execute(Database database, SessionManager sessionManager) {
        if (!sessionManager.isSessionValid(sessionId)) {
            setUnsuccessfulExecutedCommandInvalidSessionId(sessionManager);
        } else if (getUserBySessionId(database, sessionManager, sessionId).getAuthorization()
            .equals(UserAuthorization.USER)) {
            setUnsuccessfulExecutedCommandNonAdminUser();
        } else if (database.getUserByUsername(username) == null) {
            setUnsuccessfulExecutedCommandInvalidUsername();
        } else if (database.getUserByUsername(username).getAuthorization().equals(UserAuthorization.ADMIN)) {
            setUnsuccessfulExecutedCommandAddingCurrentAdminUser();
        } else {
            setSuccessfulExecutedCommand(database);
        }
    }

    @Override
    public Optional<Event> createStartEvent(String ipAddress, Database database, SessionManager sessionManager) {
        if (sessionManager.isSessionValid(sessionId)) {
            String usernameAdmin = getUserBySessionId(database, sessionManager, sessionId).getUsername();
            return Optional.of(StartEvent.of(CommandBehavior.ADD_ADMIN_USER, usernameAdmin, ipAddress,
                "Adding a new admin of the user an username " + username + " by the user " + usernameAdmin + "."));
        }
        return Optional.of(
            StartEvent.of(CommandBehavior.ADD_ADMIN_USER, "UNKNOWN", ipAddress,
                "Adding a new admin of the user an username " + username + " by the user with  session Id: " +
                    sessionId + "."));
    }

    @Override
    public Optional<Event> createEndEvent(String ipAddress, Database database,
                                          SessionManager sessionManager) {
        if (getCommandStatus().equals(CommandStatus.UNSUCCESSFUL)) {
            if (sessionManager.isSessionValid(sessionId)) {
                String username = getUserBySessionId(database, sessionManager, sessionId).getUsername();
                return Optional.of(
                    EndEvent.of(CommandBehavior.ADD_ADMIN_USER, username, ipAddress, getStatusMessage()));
            }
            return Optional.of(
                EndEvent.of(CommandBehavior.ADD_ADMIN_USER, "UNKNOWN", ipAddress, getStatusMessage()));
        }
        String username = getUserBySessionId(database, sessionManager, sessionId).getUsername();
        return Optional.of(
            EndEvent.of(CommandBehavior.ADD_ADMIN_USER, username, ipAddress, getStatusMessage()));
    }
}
