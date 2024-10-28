package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.EndEvent;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.StartEvent;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;

import java.util.Optional;

public class ResetPassword extends BasicCommand {
    private String sessionId;
    private String username;
    private String oldPassword;
    private String newPassword;

    public ResetPassword(String command) throws InvalidCommand {
        super(CommandBehavior.RESET_PASSWORD, CommandType.SECURE);
        if (command == null) {
            throw new InvalidCommand("The command cannot be null.");
        }

        final int sessionIndex = 2;
        final int userNameIndex = 4;
        final int oldPasswordIndex = 6;
        final int newPasswordIndex = 8;

        String blankSpace = " ";
        String[] commandWords = command.split(blankSpace);

        if (checkValidCommand(commandWords)) {
            this.sessionId = commandWords[sessionIndex];
            this.username = commandWords[userNameIndex];
            this.oldPassword = commandWords[oldPasswordIndex];
            this.newPassword = commandWords[newPasswordIndex];
        }
    }

    @Override
    protected boolean checkValidCommand(String[] command) throws InvalidCommand {
        final int wordsInCommand = 9;
        final int sessionIndex = 1;
        final int userNameIndex = 3;
        final int oldPasswordIndex = 5;
        final int newPasswordIndex = 7;

        String sessionId = "--session-id";
        String userName = "--username";
        String oldPassword = "--old-password";
        String newPassword = "--new-password";


        if (command == null
            || command.length != wordsInCommand
            || !command[sessionIndex].equals(sessionId)
            || !command[userNameIndex].equals(userName)
            || !command[oldPasswordIndex].equals(oldPassword)
            || !command[newPasswordIndex].equals(newPassword)) {
            throw new InvalidCommand("Invalid reset password command.");
        }
        return true;
    }

    private boolean checkUsername(Database database, SessionManager sessionManager, String sessionId) {
        return getUserBySessionId(database, sessionManager, sessionId).getUsername().equals(username);
    }

    private boolean checkPassword(Database database, SessionManager sessionManager, String sessionId) {
        return getUserBySessionId(database, sessionManager, sessionId).getPassword().equals(oldPassword);
    }

    private void setUnsuccessfulExecutedCommandInvalidSessionId(SessionManager sessionManager) {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        if (sessionManager.checkLastSessionBySessionId(sessionId)) {
            setLoggedOut(Optional.of(true));
            setStatusMessage(
                "The password reset is unsuccessful. The user with the session Id: " + sessionId +
                    " is logged out.");
        } else {
            setStatusMessage(
                "The password reset is unsuccessful. An user with the session Id: " + sessionId +
                    " doesn't exist.");
        }
    }

    private void setUnsuccessfulExecutedCommandIncorrectUsername() {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        setStatusMessage(
            "The password reset is unsuccessful. The username is not correct for session Id: " + sessionId + ".");
    }

    private void setUnsuccessfulExecutedCommandIncorrectPassword() {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        setStatusMessage(
            "The password reset is unsuccessful. The password is not correct.");
    }

    private void setSuccessfulExecutedCommand(Database database, SessionManager sessionManager) {
        setCommandStatus(CommandStatus.SUCCESSFUL);

        User currentUser = getUserBySessionId(database, sessionManager, sessionId);
        User newUser =
            new User(username, newPassword, currentUser.getFirstName(),
                currentUser.getLastName(), currentUser.getEmail());
        database.getUsers().remove(username);
        database.getUsers().put(username, newUser);

        setStatusMessage("The password reset is successful.");
    }

    @Override
    public void execute(Database database, SessionManager sessionManager) {
        if (!sessionManager.isSessionValid(sessionId)) {
            setUnsuccessfulExecutedCommandInvalidSessionId(sessionManager);
        } else if (!checkUsername(database, sessionManager, sessionId)) {
            setUnsuccessfulExecutedCommandIncorrectUsername();
        } else if (!checkPassword(database, sessionManager, sessionId)) {
            setUnsuccessfulExecutedCommandIncorrectPassword();
        } else {
            setSuccessfulExecutedCommand(database, sessionManager);
        }
    }

    @Override
    public Optional<Event> createStartEvent(String ipAddress, Database database, SessionManager sessionManager) {
        if (sessionManager.isSessionValid(sessionId)) {
            String username = getUserBySessionId(database, sessionManager, sessionId).getUsername();
            return Optional.of(StartEvent.of(CommandBehavior.RESET_PASSWORD, username, ipAddress,
                "Password reset of the user an username " + username + "."));
        }
        return Optional.of(
            StartEvent.of(CommandBehavior.RESET_PASSWORD, "UNKNOWN", ipAddress,
                "Password reset of the user with session Id: " + sessionId + "."));
    }

    @Override
    public Optional<Event> createEndEvent(String ipAddress, Database database,
                                          SessionManager sessionManager) {
        if (getCommandStatus().equals(CommandStatus.UNSUCCESSFUL)) {
            if (sessionManager.isSessionValid(sessionId)) {
                String username = getUserBySessionId(database, sessionManager, sessionId).getUsername();
                return Optional.of(
                    EndEvent.of(CommandBehavior.RESET_PASSWORD, username, ipAddress, getStatusMessage()));
            }
            return Optional.of(
                EndEvent.of(CommandBehavior.RESET_PASSWORD, "UNKNOWN", ipAddress, getStatusMessage()));
        }
        String username = getUserBySessionId(database, sessionManager, sessionId).getUsername();
        return Optional.of(
            EndEvent.of(CommandBehavior.RESET_PASSWORD, username, ipAddress,
                "The update is successful for the user with username " + username + "."));
    }
}
