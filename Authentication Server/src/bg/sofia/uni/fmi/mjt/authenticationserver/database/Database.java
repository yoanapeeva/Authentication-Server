package bg.sofia.uni.fmi.mjt.authenticationserver.database;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;
import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthorization;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Database {
    private static final Database INSTANCE = new Database();
    private final Map<String, User> users;

    private Database() {
        this.users = new HashMap();
    }

    public static Database getInstance() {
        return INSTANCE;
    }

    public Map<String, User> getUsers() {
        return getInstance().users;
    }

    public Map<String, User> getAdmins() {
        return getUsers()
            .entrySet()
            .stream()
            .filter(element -> element.getValue().getAuthorization().equals(UserAuthorization.ADMIN))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public User getUserBySessionId(SessionManager sessionManager, String sessionId) {
        return users.get(sessionManager.getUsernameBySessionId(sessionId));
    }

    public User getUserByUsername(String username) {
        return users.get(username);
    }
}
