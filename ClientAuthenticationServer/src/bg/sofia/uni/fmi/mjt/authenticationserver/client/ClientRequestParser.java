package bg.sofia.uni.fmi.mjt.authenticationserver.client;

import bg.sofia.uni.fmi.mjt.authenticationserver.communication.input.Input;

import bg.sofia.uni.fmi.mjt.authenticationserver.command.CommandType;

import java.util.Scanner;

public class ClientRequestParser {
    private final Scanner scanner;

    public ClientRequestParser(Scanner scanner) {
        this.scanner = scanner;
    }

    private String enterStartingCommand() {
        System.out.println(startingMenu());
        return scanner.nextLine();
    }

    private String enterActionCommand() {
        System.out.println(actionMenu());
        return scanner.nextLine();
    }

    private String startingMenu() {
        return """
            Please enter your command in one of these formats:
                        
            1.register --username <username> --password <password> --first-name <firstName> --last-name <lastName> --email <email>
            2.login --username <username> --password <password>
            3.login --session-id <sessionId>
            4.disconnect
            """;
    }

    private String actionMenu() {
        return """
            Please enter your command in one of these formats:
                        
            1.update-user --session-id <session-id> --new-username <newUsername> --new-first-name <newFirstName> --new-last-name <newLastName> --new-email <newEmail>                                                                                                                      
            2.reset-password --session-id <session-id> --username <username> --old-password <oldPassword> --new-password <newPassword>
            3.logout --session-id <sessionId>
            4.add-admin-user --session-id <sessionId> --username <username>
            5.remove-admin-user --session-id <sessionId> --username <username>
            6.delete-user --session-id <sessionId> --username <username>
            7.download-database --session-id <sessionId>
            8.disconnect""";
    }

    public Input setStartingMessage() {
        String prompt = enterStartingCommand();
        return new Input(prompt, CommandType.UNSECURE);
    }

    public Input setActionMessage() {
        String prompt = enterActionCommand();
        return new Input(prompt, CommandType.SECURE);
    }
}

