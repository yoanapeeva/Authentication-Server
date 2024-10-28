package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;
import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthentication;

import java.util.Optional;

public class Logout extends BasicCommand {
    private String sessionId;

    public Logout(String command) throws InvalidCommand {
        super(CommandBehavior.LOGOUT, CommandType.SECURE);
        if (command == null) {
            throw new InvalidCommand("The command cannot be null.");
        }

        final int sessionIndex = 2;

        String blankSpace = " ";
        String[] commandWords = command.split(blankSpace);

        if (checkValidCommand(commandWords)) {
            this.sessionId = commandWords[sessionIndex];
        }
    }

    @Override
    protected boolean checkValidCommand(String[] command) throws InvalidCommand {
        final int wordsInCommand = 3;
        final int sessionIndex = 1;

        String sessionId = "--session-id";

        if (command == null
            || command.length != wordsInCommand
            || !command[sessionIndex].equals(sessionId)) {
            throw new InvalidCommand("Invalid logout command.");
        }
        return true;
    }

    private void setUnsuccessfulExecutedCommandInvalidSessionId(SessionManager sessionManager) {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        if (sessionManager.checkLastSessionBySessionId(sessionId)) {
            setLoggedOut(Optional.of(true));
            setStatusMessage(
                "The logout is unsuccessful. The user with the session Id: " + sessionId +
                    " is already logged out.");
        } else {
            setStatusMessage(
                "The logout is unsuccessful. An user with the session Id: " + sessionId + " doesn't exist.");
        }
    }

    private void setSuccessfulExecutedCommand(Database database, SessionManager sessionManager) {
        setCommandStatus(CommandStatus.SUCCESSFUL);

        User currentUser = getUserBySessionId(database, sessionManager, sessionId);
        setAuthenticationToUser(database, UserAuthentication.UNAUTHENTICATED, currentUser.getUsername());

        sessionManager.removeUserSession(sessionId);

        setStatusMessage("The logout is successful.");
    }

    @Override
    public void execute(Database database, SessionManager sessionManager) {
        if (!sessionManager.isSessionValid(sessionId)) {
            setUnsuccessfulExecutedCommandInvalidSessionId(sessionManager);
        } else {
            setSuccessfulExecutedCommand(database, sessionManager);
        }
    }
}
