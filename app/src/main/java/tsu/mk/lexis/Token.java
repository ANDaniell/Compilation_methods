package tsu.mk.lexis;

public record Token(TokenType type, String lexeme, Object literal, int line, int column) {
	@Override
	public String toString() {
		return "%s %s %s".formatted(type, lexeme, literal);
	}
}
