package bg.sofia.uni.fmi.mjt.authenticationserver.command;

public enum CommandBehavior {
    REGISTER("register"),
    LOGIN_BY_USERNAME("login --username"),
    LOGIN_BY_SESSION_ID("login --session-id"),
    FAILED_LOGIN("failed login"),
    UPDATE_USER("update-user"),
    RESET_PASSWORD("reset-password"),
    LOGOUT("logout"),
    ADD_ADMIN_USER("add-admin-user"),
    REMOVE_ADMIN_USER("remove-admin-user"),
    DELETE_USER("delete-user"),
    DOWNLOAD_DATABASE("download-database"),
    INVALID_COMMAND("invalid command");

    private final String commandDescription;

    CommandBehavior(String commandDescription) {
        this.commandDescription = commandDescription;
    }

    public String getCommandDescription() {
        return commandDescription;
    }

    public static CommandBehavior getCommandByDescription(String commandDescription) {
        for (CommandBehavior command : CommandBehavior.values()) {
            if (command.getCommandDescription().equalsIgnoreCase(commandDescription)) {
                return command;
            }
        }
        return null;
    }
}
