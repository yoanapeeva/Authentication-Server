package bg.sofia.uni.fmi.mjt.authenticationserver.communication.input;

import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandType;

import java.io.Serializable;

public record Input(String message, CommandType commandType) implements Serializable {
}
