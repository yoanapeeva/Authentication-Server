package bg.sofia.uni.fmi.mjt.authenticationserver.server;

import bg.sofia.uni.fmi.mjt.authenticationserver.auditlog.AuditLog;

import bg.sofia.uni.fmi.mjt.authenticationserver.communication.input.Input;

import bg.sofia.uni.fmi.mjt.authenticationserver.communication.output.Output;

import bg.sofia.uni.fmi.mjt.authenticationserver.session.SessionManager;
import bg.sofia.uni.fmi.mjt.authenticationserver.troubleshootlog.TroubleshootLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;

import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Arrays;
import java.util.Iterator;

public class Server {
    private static final Integer BUFFER_SIZE = 512;
    private static final ByteBuffer BUFFER = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private static final String HOST = "localhost";
    private static ServerCommandExecutor serverCommandExecutor;
    private final Integer port;

    public Server(int port) {
        this.port = port;
        serverCommandExecutor = new ServerCommandExecutor();
    }

    private void serverWorking(Selector selector, boolean isServerWorking) {
        while (isServerWorking) {
            try {
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    continue;
                }
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isReadable()) {
                        try {
                            handleReadable(key);
                        } catch (IOException e) {
                            TroubleshootLog.getInstance()
                                .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                                    Arrays.toString(e.getStackTrace()) + ".");
                        }
                    } else if (key.isAcceptable()) {
                        accept(selector, key);
                    }
                    keyIterator.remove();
                }
            } catch (IOException e) {
                System.out.println("Error occurred while processing client request. " +
                    "Try again later or contact administrator by providing the logs in " +
                    TroubleshootLog.getLogFilePath());
                TroubleshootLog.getInstance()
                    .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                        Arrays.toString(e.getStackTrace()) + ".");
            } catch (ClassNotFoundException e) {
                System.out.println(
                    "Error occurred while processing client request - the request is not in the right format." +
                        "Try again later or contact administrator by providing the logs in " +
                        TroubleshootLog.getLogFilePath());
                TroubleshootLog.getInstance()
                    .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                        Arrays.toString(e.getStackTrace()) + ".");
            }
        }
    }

    public void startServer() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            Selector selector = Selector.open();
            configureServerSocketChannel(serverSocketChannel, selector);
            boolean isServerWorking = true;
            serverWorking(selector, isServerWorking);
        } catch (IOException e) {
            TroubleshootLog.getInstance()
                .log(TroubleshootLog.getId() + ".Error message: " + e.getMessage() + ", StackTrace: " +
                    Arrays.toString(e.getStackTrace()) + ".");
            throw new UncheckedIOException("Failed to start the server.", e);
        } finally {
            AuditLog.getInstance().close();
            TroubleshootLog.getInstance().close();
            shutdown();
        }
    }

    private void handleReadable(SelectionKey key) throws IOException, ClassNotFoundException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        InetSocketAddress clientAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
        String clientIpAddress = clientAddress.getAddress().getHostAddress();

        Input clientInput = getClientInput(clientChannel);
        if (clientInput != null) {
            Output output = serverCommandExecutor.executeCommand(clientInput, clientIpAddress);
            sendObject(clientChannel, output);
        }
    }

    private void configureServerSocketChannel(ServerSocketChannel channel, Selector selector) throws IOException {
        channel.bind(new InetSocketAddress(HOST, port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private Input deserializeInput(byte[] serializedData) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream byteArray = new ByteArrayInputStream(serializedData);
             ObjectInputStream objectStream = new ObjectInputStream(byteArray)) {

            return (Input) objectStream.readObject();
        }
    }

    private Input getClientInput(SocketChannel clientChannel) throws IOException, ClassNotFoundException {
        BUFFER.clear();

        int readBytes = clientChannel.read(BUFFER);
        if (readBytes < 0) {
            clientChannel.close();
            return null;
        }

        BUFFER.flip();

        int length = BUFFER.getInt();
        byte[] serializedInput = new byte[length];
        BUFFER.get(serializedInput);

        return deserializeInput(serializedInput);
    }

    private void sendObject(SocketChannel clientChannel, Output output) throws IOException {
        try (ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
             ObjectOutputStream objectStream = new ObjectOutputStream(byteArray)) {

            objectStream.writeObject(output);
            objectStream.flush();

            byte[] serializedOutput = byteArray.toByteArray();

            ByteBuffer buffer = ByteBuffer.allocate(serializedOutput.length + Integer.BYTES);
            buffer.putInt(serializedOutput.length);
            buffer.put(serializedOutput);
            buffer.flip();

            clientChannel.write(buffer);
        }
    }

    private void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel sockChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = sockChannel.accept();

        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    public void shutdown() {
        serverCommandExecutor.shutdown();
    }

    public static void main(String[] args) {
        Server server = new Server(9999);
        server.startServer();
    }
}