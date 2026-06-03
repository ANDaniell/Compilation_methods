package tsu.mk.lexis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import tsu.mk.Main;

public final class Scanner {
	private final String source;
	private final List<Token> tokens;
	private int start;
	private int current;
	private int line;
	private int column;
	private int startLine;
	private int startColumn;
	private static final Map<String, TokenType> keywords;
	private static final Map<Character, Optional<TokenType>> dictionary;

	static {
		keywords = new LinkedHashMap<>();
		keywords.put("while", TokenType.WHILE);
		keywords.put("if", TokenType.IF);
		keywords.put("else", TokenType.ELSE);
		keywords.put("input", TokenType.INPUT);
		keywords.put("output", TokenType.OUTPUT);
		keywords.put("array", TokenType.ARRAY);
		keywords.put("int", TokenType.INT_TYPE);
		
		dictionary = new LinkedHashMap<>();
		dictionary.put('$', Optional.of(TokenType.EOF));
		dictionary.put('(', Optional.of(TokenType.LEFT_PAREN));
		dictionary.put(')', Optional.of(TokenType.RIGHT_PAREN));
		dictionary.put('[', Optional.of(TokenType.LEFT_BRACKET));
		dictionary.put(']', Optional.of(TokenType.RIGHT_BRACKET));
		dictionary.put('{', Optional.of(TokenType.LEFT_BRACE));
		dictionary.put('}', Optional.of(TokenType.RIGHT_BRACE));
		dictionary.put('+', Optional.of(TokenType.PLUS));
		dictionary.put('-', Optional.of(TokenType.MINUS));
		dictionary.put('*', Optional.of(TokenType.STAR));
		dictionary.put(';', Optional.of(TokenType.SEMICOLON));
		dictionary.put(',', Optional.of(TokenType.COMMA));
		dictionary.put('/', Optional.of(TokenType.SLASH));
		dictionary.put('=', Optional.of(TokenType.EQUAL));
		dictionary.put('>', Optional.of(TokenType.GREATER));
		dictionary.put('<', Optional.of(TokenType.LESS));
	}
	
	public Scanner(final String source) {
		this.source = source;
		this.start = 0;
		this.current = 0;
		this.line = 1;
		this.column = 1;
		this.startLine = 1;
		this.startColumn = 1;
		this.tokens = new ArrayList<>();
	}
	
	public List<Token> scanTokens() {
		while (!isAtEnd()) {
			if (Main.hadError) break;
			start = current;
			startLine = line;
			startColumn = column;
			scanToken();
		}
		
		return tokens;
	}
	
	/* Scans the current token */
	private void scanToken() {
		char c = advance();
		if (c == ' ' || c == '\r' || c == '\t' || c == '\n') return;
		Optional<TokenType> type = dictionary.getOrDefault(c, Optional.empty());
		if (type.isPresent()) {
			addToken(type.get());
			return;
		}
		
		if (isDigit(c)) number();
		else if (isAlpha(c)) identifier();
			else Main.report(startLine, startColumn, "An unexpected character: %s.".formatted(c));
	}
	
	/* Reads the identifier token */
	private void identifier() {
		while (isAlphaNumeric(peek())) advance();
		String text = source.substring(start, current);
		TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);
		addToken(type);
	}
	
	private boolean isAlpha(final char c) {
		return (c >= 'a' && c <= 'z')
			|| (c >= 'A' && c <= 'Z')
				|| c == '_';
	}
	
	private boolean isAlphaNumeric(final char c) {
		return isAlpha(c) || isDigit(c);
	}
	
	/* Reads the number token */
	private void number() {
		while (isDigit(peek())) advance();
		addToken(TokenType.INT_LITERAL, Integer.parseInt(source.substring(start, current)));
	}
	
	private boolean isDigit(final char c) {
		return c >= '0' && c <= '9';
	}
	
	/* Helps to see the processing char in the sequence
	@returns the processing char */
	private char peek() {
		return source.charAt(current);
	}
	
	/* Helps to see the char after the current one to be processed
	@returns the next processing char */
	private char peekNext() {
		if (current + 1 >= source.length()) return '$';
		
		return source.charAt(current + 1);
	}
	
	/* Advancing along the sequence
	@returns the current char */
	private char advance() {
		final char c = source.charAt(current++);
		if (c == '\n') {
			line++;
			column = 1;
		} else column++;

		return c;
	}
	
	private void addToken(final TokenType type) {
		addToken(type, null);
	}
	
	private void addToken(final TokenType type, final Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, startLine, startColumn));
	}
	
	/* Checks if it is the end of a sequence
	@returns {@code true} if it is, {@code false} otherwise */
	private boolean isAtEnd() {
		return current >= source.length();
	}
}
