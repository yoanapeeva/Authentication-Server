package bg.sofia.uni.fmi.mjt.authenticationserver.user;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class User {
    @SerializedName("Username")
    private String username;
    @SerializedName("Password")
    private String password;
    @SerializedName("First name")
    private String firstName;
    @SerializedName("Last name")
    private String lastName;
    @SerializedName("Email")
    private String email;
    @SerializedName("Authentication")
    private UserAuthentication authentication;
    @SerializedName("Authorization")
    private UserAuthorization authorization;

    public User(String username, String password, String firstName, String lastName, String email) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.authentication = UserAuthentication.UNAUTHENTICATED;
        this.authorization = UserAuthorization.USER;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getEmail() {
        return this.email;
    }

    public UserAuthorization getAuthorization() {
        return this.authorization;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAuthentication(UserAuthentication authentication) {
        this.authentication = authentication;
    }

    public void setAuthorization(UserAuthorization authorization) {
        this.authorization = authorization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
