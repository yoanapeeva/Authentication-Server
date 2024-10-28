package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;
import bg.sofia.uni.fmi.mjt.authenticationserver.database.DatabaseFile;

import bg.sofia.uni.fmi.mjt.authenticationserver.exception.InvalidCommand;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

public class DownloadDatabase extends BasicCommand {
    private String sessionId;

    public DownloadDatabase(String command) throws InvalidCommand {
        super(CommandBehavior.DOWNLOAD_DATABASE, CommandType.SECURE);
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
            throw new InvalidCommand("Invalid download command.");
        }
        return true;
    }

    private void setUnsuccessfulExecutedCommandInvalidSessionId(SessionManager sessionManager) {
        setCommandStatus(CommandStatus.UNSUCCESSFUL);
        if (sessionManager.checkLastSessionBySessionId(sessionId)) {
            setStatusMessage(
                "The download is unsuccessful. The user with the session Id: " + sessionId +
                    " is logged out.");
        } else {
            setStatusMessage(
                "The download reset is unsuccessful. An user with the session Id: " + sessionId +
                    " doesn't exist.");
        }
    }

    private void setSuccessfulExecutedCommand(Database database) {
        setCommandStatus(CommandStatus.SUCCESSFUL);
        DatabaseFile.getDatabase();
        setStatusMessage(
            "The download of the database is successful in the file: " + DatabaseFile.getLogFilePath() + ".");
    }

    @Override
    public void execute(Database database, SessionManager sessionManager) {
        if (!sessionManager.isSessionValid(sessionId)) {
            setUnsuccessfulExecutedCommandInvalidSessionId(sessionManager);
        } else {
            setSuccessfulExecutedCommand(database);
        }
    }
}
