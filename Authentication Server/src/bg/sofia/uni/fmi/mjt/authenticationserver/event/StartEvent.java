package bg.sofia.uni.fmi.mjt.authenticationserver.event;

import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandBehavior;

import java.sql.Timestamp;

public class StartEvent extends BasicEvent {
    private StartEvent(Timestamp timestamp, CommandBehavior commandBehavior, String username, String ipAddress,
                       String description) {
        super(timestamp, commandBehavior, username, ipAddress, description);
    }

    public static StartEvent of(CommandBehavior commandBehavior, String username, String ipAddress,
                                String description) {
        increment();
        return new StartEvent(new Timestamp(System.currentTimeMillis()), commandBehavior, username,
            ipAddress, description);
    }
}
