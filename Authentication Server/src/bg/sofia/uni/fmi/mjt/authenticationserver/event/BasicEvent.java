package bg.sofia.uni.fmi.mjt.authenticationserver.event;

import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandBehavior;

import com.google.gson.annotations.SerializedName;

import java.sql.Timestamp;

public class BasicEvent implements Event {
    private static Integer eventId = 0;
    @SerializedName("Timestamp")
    private final Timestamp timestamp;
    @SerializedName("ID")
    private final int id;
    @SerializedName("Type of command")
    private final CommandBehavior commandBehavior;
    @SerializedName("Username")
    private final String username;
    @SerializedName("IP address")
    private final String ipAddress;
    @SerializedName("Description")
    private final String description;

    protected BasicEvent(Timestamp timestamp, CommandBehavior commandBehavior, String username,
                         String ipAddress,
                         String description) {
        this.timestamp = timestamp;
        this.id = getEventId();
        this.commandBehavior = commandBehavior;
        this.username = username;
        this.ipAddress = ipAddress;
        this.description = description;
    }

    protected static void increment() {
        eventId++;
    }

    protected static int getEventId() {
        return eventId;
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

    public String getDescription() {
        return this.description;
    }
}
