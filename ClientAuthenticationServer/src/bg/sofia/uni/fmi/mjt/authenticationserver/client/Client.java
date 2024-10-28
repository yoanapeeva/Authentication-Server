package bg.sofia.uni.fmi.mjt.authenticationserver.client;

import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandBehavior;
import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandStatus;

import bg.sofia.uni.fmi.mjt.authenticationserver.communication.input.Input;
import bg.sofia.uni.fmi.mjt.authenticationserver.communication.output.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import java.util.Scanner;

public class Client {
    private static final Integer BUFFER_SIZE = 1024;
    private static final ByteBuffer BUFFER = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private static final Integer MAX_FAILED_LOGIN_ATTEMPTS = 3;
    private static final Integer SLEEP_TIME = 60000;
    private final String host;
    private final Integer serverPort;
    private final ClientRequestParser clientRequest;
    private Integer failedLoginAttempts;

    public Client(String host, int serverPort) {
        this.host = host;
        this.serverPort = serverPort;
        Scanner scanner = new Scanner(System.in);
        this.clientRequest = new ClientRequestParser(scanner);
        this.failedLoginAttempts = 0;
    }

    private boolean shouldDisconnect(String message) {
        String disconnect = "disconnect";
        return message.equals(disconnect);
    }

    private boolean isStatusSuccessful(CommandStatus status) {
        return status.equals(CommandStatus.SUCCESSFUL);
    }

    private void countFailedLoginAttempts(CommandBehavior commandBehaviour, CommandStatus status) {
        if (commandBehaviour.equals(CommandBehavior.LOGIN_BY_SESSION_ID) ||
            commandBehaviour.equals(CommandBehavior.LOGIN_BY_USERNAME)) {
            if (isStatusSuccessful(status)) {
                failedLoginAttempts = 0;
            } else {
                failedLoginAttempts++;
            }
        }
    }

    private boolean isCommandBehaviorLogout(CommandBehavior commandBehavior) {
        return commandBehavior.equals(CommandBehavior.LOGOUT);
    }

    private void sleepClient() {
        try {
            System.out.println(
                "You have exceeded the login attempts. Try to log in after 1 min." + System.lineSeparator());
            Thread.sleep(SLEEP_TIME);
            failedLoginAttempts = 0;
        } catch (InterruptedException e) {
            System.out.println("The state of the terminal has been interrupted during sleep." +
                "Try again later or contact your administrator.");
        }
    }

    private String authenticateClient(SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        String emptySpace = "";
        String message = emptySpace;

        boolean authenticated = false;

        while (!authenticated && !shouldDisconnect(message)) {
            Input input = clientRequest.setStartingMessage();
            message = input.message();
            if (shouldDisconnect(message)) {
                break;
            }
            sendObject(socketChannel, input);

            Output output = getServerOutput(socketChannel);
            System.out.println(output.message() + System.lineSeparator());
            countFailedLoginAttempts(output.commandBehaviour(), output.status());

            if (failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                sleepClient();
            }
            if (isStatusSuccessful(output.status())) {
                authenticated = true;
            }
        }
        return message;
    }

    private String performClientActions(SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        String emptySpace = "";
        String message = emptySpace;
        while (!shouldDisconnect(message)) {
            Input input = clientRequest.setActionMessage();
            message = input.message();
            if (shouldDisconnect(message)) {
                break;
            }
            sendObject(socketChannel, input);
            Output output = getServerOutput(socketChannel);
            countFailedLoginAttempts(output.commandBehaviour(), output.status());

            if (isStatusSuccessful(output.status()) && isCommandBehaviorLogout(output.commandBehaviour())) {
                System.out.println(output.message() + System.lineSeparator());
                break;
            }
            if (output.loggedOut()) {
                System.out.println(output.message() + System.lineSeparator());
                break;
            }
            System.out.println(output.message() + System.lineSeparator());
        }
        return message;
    }

    public void startClient() {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(host, serverPort));

            String emptySpace = "";
            String message = emptySpace;
            while (!shouldDisconnect(message)) {
                message = authenticateClient(socketChannel);
                if (shouldDisconnect(message)) {
                    break;
                }
                message = performClientActions(socketChannel);
            }
        } catch (IOException e) {
            System.out.println("There is a problem with the network communication" +
                "Try again later or contact your administrator.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Output deserializeOutput(byte[] serializedData) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArray = new ByteArrayInputStream(serializedData);
             ObjectInputStream objectStream = new ObjectInputStream(byteArray)) {

            return (Output) objectStream.readObject();
        }
    }

    private Output getServerOutput(SocketChannel socketChannel) throws IOException, ClassNotFoundException {
        BUFFER.clear();
        socketChannel.read(BUFFER);
        BUFFER.flip();

        int length = BUFFER.getInt();
        byte[] serializedOutput = new byte[length];
        BUFFER.get(serializedOutput);

        return deserializeOutput(serializedOutput);
    }

    private void sendObject(SocketChannel clientChannel, Input input) throws IOException {
        try (ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
             ObjectOutputStream objectStream = new ObjectOutputStream(byteArray)) {

            objectStream.writeObject(input);
            objectStream.flush();

            byte[] serializedInput = byteArray.toByteArray();

            ByteBuffer buffer = ByteBuffer.allocate(serializedInput.length + Integer.BYTES);
            buffer.putInt(serializedInput.length);
            buffer.put(serializedInput);
            buffer.flip();

            clientChannel.write(buffer);
        }
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 9999);
        client.startClient();
    }
}