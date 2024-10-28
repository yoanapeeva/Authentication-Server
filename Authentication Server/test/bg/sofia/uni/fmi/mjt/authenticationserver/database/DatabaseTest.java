package bg.sofia.uni.fmi.mjt.authenticationserver.database;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;

import bg.sofia.uni.fmi.mjt.authenticationserver.user.User;
import bg.sofia.uni.fmi.mjt.authenticationserver.user.UserAuthorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class DatabaseTest {
    private static Database database;
    @Mock
    private SessionManager sessionManager;

    @BeforeEach
    public void setUp() {
        database = Database.getInstance();
    }

    @Test
    public void testGetAdmins() {
        Map<String, User> users = new HashMap<>();
        User user = new User("user", "password", "John", "Doe", "john@example.com");
        User admin = new User("admin", "password", "Admin", "User", "admin@example.com");
        admin.setAuthorization(UserAuthorization.ADMIN);
        users.put("user", user);
        users.put("admin", admin);

        database.getUsers().putAll(users);
        Map<String, User> admins = database.getAdmins();

        assertTrue(admins.containsKey("admin"));
        assertFalse(admins.containsKey("user"));
    }

    @Test
    public void testGetUserBySessionId() {
        String sessionId = "12345678";
        when(sessionManager.getUsernameBySessionId(sessionId)).thenReturn("user");

        Map<String, User> users = new HashMap<>();
        User user = new User("user", "password", "John", "Doe", "john@example.com");
        users.put("user", user);
        database.getUsers().putAll(users);

        User userBySessionId = database.getUserBySessionId(sessionManager, sessionId);
        assertEquals(user, userBySessionId);
    }

    @Test
    public void testGetUserByUsername() {
        Map<String, User> users = new HashMap<>();
        User user = new User("user", "password", "John", "Doe", "john@example.com");
        users.put("user", user);
        database.getUsers().putAll(users);

        User userByUsername = database.getUserByUsername("user");
        assertEquals(user, userByUsername);
    }
}
