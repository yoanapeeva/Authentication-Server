package bg.sofia.uni.fmi.mjt.authenticationserver.session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.stream.Collectors;

public class SessionManager {
    private static final long TTL = 5 * 60 * 1000;
    private static final SessionManager SESSION_MANAGER = new SessionManager();
    private final Map<String, Session> sessions;
    private final Map<String, String> lastSessionOfUser;
    private final ScheduledExecutorService executor;

    private SessionManager() {
        this.sessions = new HashMap<>();
        this.lastSessionOfUser = new HashMap<>();
        this.executor = Executors.newScheduledThreadPool(10);
        this.executor.scheduleAtFixedRate(this::removeExpiredSessions, 0, 1, TimeUnit.MINUTES);
    }

    public String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        String lastSession = lastSessionOfUser.get(username);
        if (isSessionValid(lastSession)) {
            sessions.remove(lastSession);
        }
        sessions.put(sessionId, new Session(username, sessionId, System.currentTimeMillis() + TTL));
        lastSessionOfUser.put(username, sessionId);
        return sessionId;
    }

    public boolean isSessionValid(String sessionId) {
        return sessions.get(sessionId) != null && sessions.get(sessionId).ttl() >= System.currentTimeMillis();
    }

    public Session getSessionBySessionId(String sessionId) {
        return sessions.get(sessionId);
    }

    public String getUsernameBySessionId(String sessionId) {
        return sessions.get(sessionId).username();
    }

    public void removeAllUserSession(String username) {
        String lastSessionId = lastSessionOfUser.get(username);
        removeUserSession(lastSessionId);
    }

    public void removeUserSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public void replaceSession(Session session) {
        sessions.put(session.sessionId(), session);
    }

    public synchronized void removeExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> entry.getValue().ttl() <= currentTime);
    }

    public boolean checkLastSessionBySessionId(String sessionId) {
        return lastSessionOfUser.containsValue(sessionId);
    }

    public static SessionManager getInstance() {
        return SESSION_MANAGER;
    }

    public List<String> getUsersWithExpiredSessions() {
        return lastSessionOfUser.entrySet()
            .stream()
            .filter(element -> !isSessionValid(element.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public void shutdown() {
        executor.shutdown();
    }
}
