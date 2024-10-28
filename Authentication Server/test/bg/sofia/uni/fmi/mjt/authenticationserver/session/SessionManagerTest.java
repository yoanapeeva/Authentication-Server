package bg.sofia.uni.fmi.mjt.authenticationserver.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class SessionManagerTest {
    private SessionManager sessionManager;

    @BeforeEach
    public void setUp() {
        sessionManager = SessionManager.getInstance();
    }

    @AfterEach
    public void shutdown() {
        sessionManager.shutdown();
    }

    @Test
    public void testCreateSession() {
        String username = "username";
        String sessionId = sessionManager.createSession(username);
        assertNotNull(sessionId);
    }

    @Test
    public void testIsSessionValid() {
        String username = "username";
        String sessionId = sessionManager.createSession(username);
        assertTrue(sessionManager.isSessionValid(sessionId));
    }

    @Test
    public void testGetSessionBySessionId() {
        String username = "username";
        String sessionId = sessionManager.createSession(username);
        assertNotNull(sessionManager.getSessionBySessionId(sessionId));
    }

    @Test
    public void testGetUsernameBySessionId() {
        String username = "username";
        String sessionId = sessionManager.createSession(username);
        assertEquals(username, sessionManager.getUsernameBySessionId(sessionId));
    }

    @Test
    public void testRemoveAllUserSession() {
        String username = "username";
        String sessionId = sessionManager.createSession(username);
        sessionManager.removeAllUserSession(username);
        assertFalse(sessionManager.isSessionValid(sessionId));
    }

    @Test
    public void testRemoveUserSession() {
        String username = "username";
        String sessionId = sessionManager.createSession(username);
        sessionManager.removeUserSession(sessionId);
        assertFalse(sessionManager.isSessionValid(sessionId));
    }

    @Test
    public void testCheckLastSessionBySessionId() {
        String username = "username";
        String sessionId = sessionManager.createSession(username);
        assertTrue(sessionManager.checkLastSessionBySessionId(sessionId));
    }

    @Test
    public void testCheckLastSessionBySessionIdInvalidSessionId() {
        String sessionId = "12345678";
        assertFalse(sessionManager.checkLastSessionBySessionId(sessionId));
    }
}
