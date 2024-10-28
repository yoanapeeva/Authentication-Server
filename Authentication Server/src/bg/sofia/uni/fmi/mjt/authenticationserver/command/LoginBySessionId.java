package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.FailedLogin;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import java.util.Optional;

public class LoginBySessionId extends BasicCommand {
    private String sessionId;

    public LoginBySessionId(String command) throws InvalidCommand {
        super(CommandBehavior.LOGIN_BY_SESSION_ID, CommandType.UNSECURE);
        if (command == null) {
            throw new InvalidCommand("The command cannot be null.");
        }

        String blankSpace = " ";
        String[] commandWords = command.split(blankSpace);

        final int sessionIdIndex = 2;

        if (checkValidCommand(commandWords)) {
            this.sessionId = commandWords[sessionIdIndex];
        }
    }

    @Override
    protected boolean checkValidCommand(String[] command) throws InvalidCommand {
        final int wordsInCommand = 3;
        final int sessionIdIndex = 1;

        String sessionId = "--session-id";

        if (command == null
            || command.length != wordsInCommand
            || !command[sessionIdIndex].equals(sessionId)) {
            throw new InvalidCommand("Invalid login-by-session-id command.");
        }
        return true;
    }

    private void setUnsuccessfulExecutedCommandInvalidSessionId(SessionManager sessionManager) {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        if (sessionManager.checkLastSessionBySessionId(sessionId)) {
            setLoggedOut(Optional.of(true));
            setStatusMessage(
                "The login is unsuccessful. The user with the session Id: " + sessionId + " is logged out.");
        } else {
            setStatusMessage(
                "The login is unsuccessful. An user with the session Id: " + sessionId + " doesn't exist.");
        }
    }

    private void setSuccessfulExecutedCommand() {
        setCommandStatus(CommandStatus.SUCCESSFUL);
        setStatusMessage("The login is successful.");
    }

    @Override
    public void execute(Database database, SessionManager sessionManager) {
        if (!sessionManager.isSessionValid(sessionId)) {
            setUnsuccessfulExecutedCommandInvalidSessionId(sessionManager);
        } else {
            setSuccessfulExecutedCommand();
        }
    }

    @Override
    public Optional<Event> createFailedLoginEvent(String ipAddress, Database database,
                                                  SessionManager sessionManager) {
        if (getCommandStatus().equals(CommandStatus.UNSUCCESSFUL)) {
            return Optional.of(FailedLogin.of("UNKNOWN", ipAddress));
        }
        return Optional.ofNullable(null);
    }
}
