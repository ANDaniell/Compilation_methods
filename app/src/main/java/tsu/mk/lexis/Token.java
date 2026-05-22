package tsu.mk.lexis;

public record Token(TokenType type, String lexeme, Object literal) {
    @Override
    public String toString() {
        return "%s %s %s".formatted(type, lexeme, literal);
    }
}
