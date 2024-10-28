package bg.sofia.uni.fmi.mjt.authenticationserver.communication.output;

import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandBehavior;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandStatus;

import java.io.Serializable;

public record Output(String message, CommandStatus status, CommandBehavior commandBehaviour, Boolean loggedOut)
    implements Serializable {
}
