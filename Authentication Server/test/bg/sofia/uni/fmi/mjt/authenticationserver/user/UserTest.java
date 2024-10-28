package bg.sofia.uni.fmi.mjt.authenticationserver.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserTest {
    @Test
    public void testEquals() {
        User user = new User("username", "password", "firstname", "lastname", "email");
        User user2 = new User("username", "password2", "firstname2", "lastname2", "email2");
        assertEquals(user, user2);
    }
}
