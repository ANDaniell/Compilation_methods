package tsu.mk;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tsu.mk.interpretation.Interpreter;
import tsu.mk.lexis.Scanner;
import tsu.mk.lexis.Token;
import tsu.mk.syntax.Notation;
import tsu.mk.syntax.NotationEntry;
import tsu.mk.syntax.Parser;

public final class Main {
	public static boolean hadError = false;
	
	private static void runFile(final Path path) throws IOException {
		final byte[] bytes = Files.readAllBytes(path);
		run(new String(bytes, Charset.defaultCharset()));
		
		if (hadError) System.exit(1);
	}
	
	private static void run(final String source) {
		final Scanner scanner = new Scanner(source);
		final List<Token> tokens = scanner.scanTokens();
		final Parser parser = new Parser(tokens);
		final Notation notation = parser.parse();
		
		final Interpreter interpreter = new Interpreter(notation.raw());
		interpreter.run();
	}
	
	private static void displayTokens(final List<Token> tokens) {
		tokens.forEach(System.out::println);
	}
	
	private static void displayNotation(final Notation notation) {
		List<NotationEntry> raw = notation.raw();
		for (int index = 0; index < raw.size(); index++) {
			System.out.printf("%d: %s\n", index, raw.get(index));
		}
	}
	
	public static void report(final String message) {
		System.err.printf("Error: %s%n", message);
		hadError = true;
	}
	
	public static void report(final int line, final int column, final String message) {
		System.err.printf("Error at line %d, column %d: %s%n", line, column, message);
		hadError = true;
	}
	
	public static void main(String[] args) throws IOException {
		final Path path = Path.of("tests", "test_1.txt");
		runFile(path);
	}
}
