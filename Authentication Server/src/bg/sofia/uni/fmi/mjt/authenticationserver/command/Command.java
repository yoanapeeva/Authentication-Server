package bg.sofia.uni.fmi.mjt.authenticationserver.command;

import bg.sofia.uni.fmi.mjt.authenticationserver.database.Database;
import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

public interface Command {
    /**
     * Execute command
     *
     * @param database       - database of the users
     * @param sessionManager - manager of the users sessions
     */
    void execute(Database database, SessionManager sessionManager);
}
