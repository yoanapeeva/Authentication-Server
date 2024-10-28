package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;

import bg.sofia.uni.fmi.mjt.authenticationserver.event.EndEvent;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.Event;
import bg.sofia.uni.fmi.mjt.authenticationserver.event.StartEvent;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.Session;
import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class UpdateUser extends BasicCommand {
    private final String sessionId;
    private final String newUsername;
    private final String newFirstName;
    private final String newLastName;
    private final String newEmail;

    private UpdateUser(UpdateUserBuilder builder) {
        super(CommandBehavior.UPDATE_USER, CommandType.SECURE);
        this.sessionId = builder.sessionId;
        this.newUsername = builder.newUsername;
        this.newFirstName = builder.newFirstName;
        this.newLastName = builder.newLastName;
        this.newEmail = builder.newEmail;
    }

    private static UpdateUserBuilder updateUserAttributes(String[] commandWords,
                                                          List<String> commandWordsAsList) {
        UpdateUserBuilder userBuilder = null;
        final int sessionIdIndex = 2;
        String newUsername = "--new-username";
        String newFirstName = "--new-first-name";
        String newLastName = "--new-last-name";
        String newEmail = "--new-email";

        userBuilder = new UpdateUserBuilder(commandWords[sessionIdIndex]);
        if (commandWordsAsList.contains(newUsername)) {
            userBuilder =
                userBuilder.setNewUsername(commandWords[commandWordsAsList.indexOf(newUsername) + 1]);
        }
        if (commandWordsAsList.contains(newFirstName)) {
            userBuilder =
                userBuilder.setNewFirstName(commandWords[commandWordsAsList.indexOf(newFirstName) + 1]);
        }
        if (commandWordsAsList.contains(newLastName)) {
            userBuilder =
                userBuilder.setNewLastName(commandWords[commandWordsAsList.indexOf(newLastName) + 1]);
        }
        if (commandWordsAsList.contains(newEmail)) {
            userBuilder =
                userBuilder.setNewEmail(commandWords[commandWordsAsList.indexOf(newEmail) + 1]);
        }
        return userBuilder;
    }

    public static UpdateUserBuilder builder(String command) throws InvalidCommand {
        if (command == null) {
            throw new InvalidCommand("The command cannot be null.");
        }

        String blankSpace = " ";
        String[] commandWords = command.split(blankSpace);

        List<String> commandWordsAsList = Arrays.asList(commandWords);

        UpdateUserBuilder userBuilder = null;
        if (checkValidUpdateCommand(commandWords)) {
            userBuilder = updateUserAttributes(commandWords, commandWordsAsList);
        }
        return userBuilder;
    }

    private static boolean checkValidUpdateCommand(String[] command) throws InvalidCommand {
        final int minCountWords = 3;
        final int sessionIdIndex = 1;

        String sessionId = "--session-id";
        String newUsername = "--new-username";
        String newFirstName = "--new-first-name";
        String newLastName = "--new-last-name";
        String newEmail = "--new-email";

        List<String> optionalParameters = List.of(newUsername, newFirstName, newLastName, newEmail);

        if (command == null
            || command.length < minCountWords
            || !command[sessionIdIndex].equals(sessionId)) {
            throw new InvalidCommand("Invalid update command.");
        }

        List<String> actualParameters = new ArrayList<>();
        for (int i = minCountWords; i < command.length; i += 2) {
            if (!optionalParameters.contains(command[i]) || i + 1 >= command.length ||
                actualParameters.contains(command[i])) {
                throw new InvalidCommand("Invalid update command.");
            }
            actualParameters.add(command[i]);
        }
        return true;
    }

    @Override
    protected boolean checkValidCommand(String[] command) throws InvalidCommand {
        return checkValidUpdateCommand(command);
    }

    private void setUnsuccessfulExecutedCommandInvalidSessionId(SessionManager sessionManager) {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        if (sessionManager.checkLastSessionBySessionId(sessionId)) {
            setLoggedOut(Optional.of(true));
            setStatusMessage(
                "The user update is unsuccessful. The user with the session Id: " + sessionId + " is logged out.");
        } else {
            setStatusMessage(
                "The user update is unsuccessful. An user with the session Id: " + sessionId + " doesn't exist.");
        }
    }

    private void setSuccessfulExecutedCommand(Database database, SessionManager sessionManager) {
        setCommandStatus(CommandStatus.SUCCESSFUL);

        User currentUser = getUserBySessionId(database, sessionManager, sessionId);
        String currentUsername = newUsername != null ? newUsername : currentUser.getUsername();
        String currentFirstName = newFirstName != null ? newFirstName : currentUser.getFirstName();
        String currentLastName = newLastName != null ? newLastName : currentUser.getLastName();
        String currentEmail = newEmail != null ? newEmail : currentUser.getEmail();

        User newUser =
            new User(currentUsername, currentUser.getPassword(), currentFirstName, currentLastName, currentEmail);
        database.getUsers().remove(currentUser.getUsername());
        database.getUsers().put(currentUsername, newUser);

        Session currentSession = sessionManager.getSessionBySessionId(sessionId);
        sessionManager.replaceSession(new Session(currentUsername, sessionId, currentSession.ttl()));

        setStatusMessage("The update is successful.");
    }

    @Override
    public void execute(Database database, SessionManager sessionManager) {
        if (!sessionManager.isSessionValid(sessionId)) {
            setUnsuccessfulExecutedCommandInvalidSessionId(sessionManager);
        } else {
            setSuccessfulExecutedCommand(database, sessionManager);
        }
    }

    @Override
    public Optional<Event> createStartEvent(String ipAddress, Database database, SessionManager sessionManager) {
        if (sessionManager.isSessionValid(sessionId)) {
            String username = getUserBySessionId(database, sessionManager, sessionId).getUsername();
            return Optional.of(StartEvent.of(CommandBehavior.UPDATE_USER, username, ipAddress,
                "Update of user with an username " + username + "."));
        }
        return Optional.of(
            StartEvent.of(CommandBehavior.UPDATE_USER, "UNKNOWN", ipAddress,
                "Update of the user with session Id: " + sessionId + "."));
    }

    @Override
    public Optional<Event> createEndEvent(String ipAddress, Database database,
                                          SessionManager sessionManager) {
        if (getCommandStatus().equals(CommandStatus.UNSUCCESSFUL)) {
            return Optional.of(
                EndEvent.of(CommandBehavior.UPDATE_USER, "UNKNOWN", ipAddress, getStatusMessage()));
        }
        User user = getUserBySessionId(database, sessionManager, sessionId);
        return Optional.of(
            EndEvent.of(CommandBehavior.UPDATE_USER, user.getUsername(), ipAddress,
                "The update is successful. The new information about the user is: username - " + user.getUsername() +
                    ", first name - " + user.getFirstName() + ", last name - " + user.getLastName() + ", email - " +
                    user.getEmail() + "."));
    }

    public static class UpdateUserBuilder {
        private final String sessionId;
        private String newUsername;
        private String newFirstName;
        private String newLastName;
        private String newEmail;

        private UpdateUserBuilder(String sessionId) {
            this.sessionId = sessionId;
        }

        public UpdateUserBuilder setNewUsername(String newUsername) {
            this.newUsername = newUsername;
            return this;
        }

        public UpdateUserBuilder setNewFirstName(String newFirstName) {
            this.newFirstName = newFirstName;
            return this;
        }

        public UpdateUserBuilder setNewLastName(String newLastName) {
            this.newLastName = newLastName;
            return this;
        }

        public UpdateUserBuilder setNewEmail(String newEmail) {
            this.newEmail = newEmail;
            return this;
        }

        public UpdateUser build() {
            return new UpdateUser(this);
        }
    }
}
