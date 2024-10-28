package bg.sofia.uni.fmi.mjt.authenticationserver.session;

public record Session(String username, String sessionId, long ttl) {
}
