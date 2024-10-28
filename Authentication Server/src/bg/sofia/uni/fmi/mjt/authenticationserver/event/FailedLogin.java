package bg.sofia.uni.fmi.mjt.authenticationserver.event;

import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandBehavior;

import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;

public class FailedLogin implements Event {
    @SerializedName("Timestamp")
    private final Timestamp timestamp;
    @SerializedName("Type of command")
    private final CommandBehavior commandBehavior;
    @SerializedName("Username")
    private final String username;
    @SerializedName("IP Address")
    private final String ipAddress;

    private FailedLogin(Timestamp timestamp, CommandBehavior commandBehavior,
                        String username, String ipAddress) {
        this.timestamp = timestamp;
        this.commandBehavior = commandBehavior;
        this.username = username;
        this.ipAddress = ipAddress;
    }

    public CommandBehavior getCommandBehavior() {
        return this.commandBehavior;
    }

    public String getUsername() {
        return this.username;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public static FailedLogin of(String username, String ipAddress) {
        return new FailedLogin(new Timestamp(System.currentTimeMillis()), CommandBehavior.FAILED_LOGIN, username,
            ipAddress);
    }
}
