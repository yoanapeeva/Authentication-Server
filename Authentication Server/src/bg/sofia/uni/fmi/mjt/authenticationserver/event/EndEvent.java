package bg.sofia.uni.fmi.mjt.authenticationserver.event;

import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandBehavior;

import java.sql.Timestamp;

public class EndEvent extends BasicEvent {
    private EndEvent(Timestamp timestamp, CommandBehavior commandBehavior, String username, String ipAddress,
                     String description) {
        super(timestamp, commandBehavior, username, ipAddress, description);
    }

    public static EndEvent of(CommandBehavior commandBehavior, String username, String ipAddress, String description) {
        return new EndEvent(new Timestamp(System.currentTimeMillis()), commandBehavior, username,
            ipAddress, description);
    }
}
