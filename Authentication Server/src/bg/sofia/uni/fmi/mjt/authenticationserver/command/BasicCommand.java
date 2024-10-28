package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;
import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthentication;
import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthorization;

import java.util.Optional;

public abstract class BasicCommand implements Command {
    private final CommandBehavior commandBehavior;
    private final CommandType commandType;
    private CommandStatus commandStatus;
    private String statusMessage;
    private Optional<Boolean> loggedOut;

    public BasicCommand(CommandBehavior commandBehavior, CommandType commandType) {
        this.commandBehavior = commandBehavior;
        this.commandType = commandType;
        this.commandStatus = CommandStatus.UNDEFINED;
        this.loggedOut = Optional.empty();
    }

    public CommandBehavior getCommandBehavior() {
        return commandBehavior;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public CommandStatus getCommandStatus() {
        return commandStatus;
    }

    protected void setCommandStatus(CommandStatus commandStatus) {
        this.commandStatus = commandStatus;
    }

    protected void setStatusMessage(String message) {
        this.statusMessage = message;
    }

    public String getStatusMessage() {
        return this.statusMessage;
    }

    public Optional<Boolean> getLoggedOut() {
        return this.loggedOut;
    }

    protected void setLoggedOut(Optional<Boolean> loggedOut) {
        this.loggedOut = loggedOut;
    }

    protected void setAuthenticationToUser(Database database, UserAuthentication authentication, String username) {
        database.getUsers().get(username).setAuthentication(authentication);
    }

    protected void setAuthorizationToUser(Database database, UserAuthorization authorization, String username) {
        database.getUserByUsername(username).setAuthorization(authorization);
    }

    protected User getUserBySessionId(Database database, SessionManager sessionManager, String sessionId) {
        return database.getUserBySessionId(sessionManager, sessionId);
    }

    protected abstract boolean checkValidCommand(String[] command) throws InvalidCommand;

    public Optional<Event> createStartEvent(String ipAddress, Database database, SessionManager sessionManager) {
        return Optional.empty();
    }

    public Optional<Event> createEndEvent(String ipAddress, Database database, SessionManager sessionManager) {
        return Optional.empty();
    }

    public Optional<Event> createFailedLoginEvent(String ipAddress, Database database,
                                                  SessionManager sessionManager) {
        return Optional.empty();
    }
}
