package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.FailedLogin;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthentication;

import java.util.Optional;

public class LoginByUsername extends BasicCommand {
    private String username;
    private String password;

    public LoginByUsername(String command) throws InvalidCommand {
        super(CommandBehavior.LOGIN_BY_USERNAME, CommandType.UNSECURE);
        if (command == null) {
            throw new InvalidCommand("The command cannot be null.");
        }

        final int userNameIndex = 2;
        final int passwordIndex = 4;

        String blankSpace = " ";
        String[] commandWords = command.split(blankSpace);

        if (checkValidCommand(commandWords)) {
            this.username = commandWords[userNameIndex];
            this.password = commandWords[passwordIndex];
        }
    }

    @Override
    protected boolean checkValidCommand(String[] command) throws InvalidCommand {
        final int wordsInCommand = 5;
        final int userNameIndex = 1;
        final int passwordIndex = 3;
        final String userName = "--username";
        final String password = "--password";

        if (command == null
            || command.length != wordsInCommand
            || !command[userNameIndex].equals(userName)
            || !command[passwordIndex].equals(password)) {
            throw new InvalidCommand("Invalid login-by-username command.");
        }
        return true;
    }

    public String getUsername() {
        return username;
    }

    private boolean checkPassword(Database database) {
        return database.getUsers().get(username).getPassword().equals(password);
    }

    private void setUnsuccessfulExecutedCommandInvalidUsername() {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        setStatusMessage(
            "The login is unsuccessful. An user with the username: " + username + " doesn't exist.");
    }

    private void setUnsuccessfulExecutedCommandIncorrectPassword() {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        setStatusMessage(
            "The login is unsuccessful. The password is incorrect.");
    }

    private void setSuccessfulExecutedCommand(Database database, SessionManager sessionManager) {
        setCommandStatus(CommandStatus.SUCCESSFUL);

        setAuthenticationToUser(database, UserAuthentication.AUTHENTICATED, username);

        String sessionId = sessionManager.createSession(username);
        setStatusMessage(
            "The login is successful. Your current session Id is: " + sessionId + ".");
    }

    @Override
    public void execute(Database database, SessionManager sessionManager) {
        if (database.getUserByUsername(username) == null) {
            setUnsuccessfulExecutedCommandInvalidUsername();
        } else if (!checkPassword(database)) {
            setUnsuccessfulExecutedCommandIncorrectPassword();
        } else {
            setSuccessfulExecutedCommand(database, sessionManager);
        }
    }

    @Override
    public Optional<Event> createFailedLoginEvent(String ipAddress, Database database,
                                                  SessionManager sessionManager) {
        if (getCommandStatus().equals(CommandStatus.UNSUCCESSFUL)) {
            return Optional.of(FailedLogin.of(username, ipAddress));
        }
        return Optional.empty();
    }
}
