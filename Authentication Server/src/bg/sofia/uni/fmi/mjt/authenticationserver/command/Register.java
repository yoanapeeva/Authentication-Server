package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.EndEvent;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.StartEvent;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;
import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthentication;
import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthorization;

import java.util.Optional;

public class Register extends BasicCommand {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email;

    public Register(String command) throws InvalidCommand {
        super(CommandBehavior.REGISTER, CommandType.UNSECURE);
        if (command == null) {
            throw new InvalidCommand("The command cannot be null.");
        }

        final int userNameIndex = 2;
        final int passwordIndex = 4;
        final int firstNameIndex = 6;
        final int lastNameIndex = 8;
        final int emailIndex = 10;

        String blankSpace = " ";
        String[] commandWords = command.split(blankSpace);

        if (checkValidCommand(commandWords)) {
            this.username = commandWords[userNameIndex];
            this.password = commandWords[passwordIndex];
            this.firstName = commandWords[firstNameIndex];
            this.lastName = commandWords[lastNameIndex];
            this.email = commandWords[emailIndex];
        }
    }

    @Override
    protected boolean checkValidCommand(String[] command) throws InvalidCommand {
        final int wordsInCommand = 11;
        final int userNameIndex = 1;
        final int passwordIndex = 3;
        final int firstNameIndex = 5;
        final int lastNameIndex = 7;
        final int emailIndex = 9;
        String userName = "--username";
        String password = "--password";
        String firstName = "--first-name";
        String lastName = "--last-name";
        String email = "--email";

        if (command == null
            || command.length != wordsInCommand
            || !command[userNameIndex].equals(userName)
            || !command[passwordIndex].equals(password)
            || !command[firstNameIndex].equals(firstName)
            || !command[lastNameIndex].equals(lastName)
            || !command[emailIndex].equals(email)) {
            throw new InvalidCommand("Invalid register command.");
        }
        return true;
    }

    private void setUnsuccessfulExecutedCommand() {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        setStatusMessage(
            "The registry is unsuccessful. An user with the username: " + username + " already exists.");
    }

    private void addUserToDatabase(Database database) {
        database.getUsers().put(username, new User(username, password, firstName, lastName, email));
    }

    private void setSuccessfulExecutedCommandFirstUser(Database database, String sessionId) {
        setAuthorizationToUser(database, UserAuthorization.ADMIN, username);

        setStatusMessage(
            "The registry is successful. Your current session Id is: " + sessionId + "." +
                System.lineSeparator() + "You have been granted with administrative permissions.");
    }

    private void setSuccessfulExecutedCommandNonFirstUser(String sessionId) {
        setStatusMessage(
            "The registry is successful. Your current session Id is: " + sessionId + ".");
    }

    private void setSuccessfulExecutedCommand(Database database, SessionManager sessionManager) {
        setCommandStatus(CommandStatus.SUCCESSFUL);

        addUserToDatabase(database);
        setAuthenticationToUser(database, UserAuthentication.AUTHENTICATED, username);

        String sessionId = sessionManager.createSession(username);
        if (database.getUsers().size() == 1) {
            setSuccessfulExecutedCommandFirstUser(database, sessionId);
        } else {
            setSuccessfulExecutedCommandNonFirstUser(sessionId);
        }
    }

    @Override
    public void execute(Database database, SessionManager sessionManager) {
        if (database.getUserByUsername(username) != null) {
            setUnsuccessfulExecutedCommand();
        } else {
            setSuccessfulExecutedCommand(database, sessionManager);
        }
    }

    @Override
    public Optional<Event> createStartEvent(String ipAddress, Database database, SessionManager sessionManager) {
        return Optional.of(StartEvent.of(CommandBehavior.REGISTER, username, ipAddress,
            "Registration of the user " + username + "."));
    }

    @Override
    public Optional<Event> createEndEvent(String ipAddress, Database database, SessionManager sessionManager) {
        if (getCommandStatus().equals(CommandStatus.UNSUCCESSFUL)) {
            return Optional.of(
                EndEvent.of(CommandBehavior.REGISTER, username, ipAddress, getStatusMessage()));
        } else {
            return Optional.of(EndEvent.of(CommandBehavior.REGISTER, username, ipAddress,
                "The registry of the user " + username + " is successful."));
        }
    }
}
