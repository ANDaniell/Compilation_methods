package tsu.mk.syntax;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import tsu.mk.Main;
import tsu.mk.lexis.Token;
import tsu.mk.lexis.TokenType;
import tsu.mk.syntax.operand.Literal;
import tsu.mk.syntax.operand.OperandType;
import tsu.mk.syntax.operand.Variable;

public final class Parser {
	private static final int NUM_ROWS;
	private static final int NUM_COLUMNS;
	private final List<Token> tokens;
	private final Deque<String> magazine;
	private final Deque<GeneratorEntry> generator;
	private final TableEntry[][] table;
	private final Deque<Integer> marks;
	private final Map<TokenType, Integer> columns;
	private final Map<String, Integer> rows;
	private final Map<GeneratorEntry, OperationType> operations;
	private final Map<GeneratorEntry, Consumer<Token>> programs;
	private final Map<String, TokenType> terminals;
	private final List<NotationEntry> raw;
	/* Last cached index from the marks for the else clause */
	private int lastCachedIndex;
	private int current;
	/* The current index in the generating reverse Polish notation (RPN) */
	private int generating;

	static
	{
		NUM_ROWS = 16;
		NUM_COLUMNS = TokenType.values().length;
	}

	/* The parsing table entries to be used
	@param production a production rule
	@param semantic a semantic rule */
	private record TableEntry(String[] production, GeneratorEntry[] semantic) {
		private static TableEntry empty() {
			return new TableEntry(null, null);
		}

		private static TableEntry lambda() {
			return new TableEntry(new String[0], new GeneratorEntry[0]);
		}

		private boolean isNotEmpty() {
			return production != null && semantic != null;
		}

		private boolean isLambda() {
			return isNotEmpty() && production.length + semantic.length == 0;
		}
	}

	public Parser(final List<Token> tokens) {
		this.tokens = tokens;
		this.magazine = new ArrayDeque<>();
		this.generator = new ArrayDeque<>();
		this.table = new TableEntry[NUM_ROWS][NUM_COLUMNS];
		this.marks = new ArrayDeque<>();
		this.columns = new LinkedHashMap<>();
		this.rows = new LinkedHashMap<>();
		this.operations = new LinkedHashMap<>();
		this.programs = new LinkedHashMap<>();
		this.terminals = new LinkedHashMap<>();
		this.raw = new ArrayList<>();
		this.current = 0;
		this.generating = 0;
		this.lastCachedIndex = -1;

		initMagazine();
		initGenerator();
		inflate();
		initOperations();
		initPrograms();
		initTerminals();
	}

	private void initTerminals() {
		terminals.put("<", TokenType.LESS);
		terminals.put(">", TokenType.GREATER);
		terminals.put("(", TokenType.LEFT_PAREN);
		terminals.put(")", TokenType.RIGHT_PAREN);
		terminals.put("[", TokenType.LEFT_BRACKET);
		terminals.put("]", TokenType.RIGHT_BRACKET);
		terminals.put("{", TokenType.LEFT_BRACE);
		terminals.put("}", TokenType.RIGHT_BRACE);
		terminals.put(",", TokenType.COMMA);
		terminals.put("-", TokenType.MINUS);
		terminals.put("+", TokenType.PLUS);
		terminals.put(";", TokenType.SEMICOLON);
		terminals.put("/", TokenType.SLASH);
		terminals.put("*", TokenType.STAR);
		terminals.put("=", TokenType.EQUAL);
		terminals.put("a", TokenType.IDENTIFIER);
		terminals.put("k", TokenType.INT_LITERAL);
		terminals.put("while", TokenType.WHILE);
		terminals.put("if", TokenType.IF);
		terminals.put("else", TokenType.ELSE);
		terminals.put("input", TokenType.INPUT);
		terminals.put("output", TokenType.OUTPUT);
		terminals.put("array", TokenType.ARRAY);
		terminals.put("int", TokenType.INT_TYPE);
		terminals.put("$", TokenType.EOF);
	}

	private void initPrograms() {
		programs.put(GeneratorEntry.INT_NAME, token -> {
			name(token.lexeme());
			addOperation(OperationType.INT_CREATION);
		});
		programs.put(GeneratorEntry.ARRAY_NAME, token -> {
			name(token.lexeme());
		});
		programs.put(GeneratorEntry.ARRAY_SIZE, token -> createArray());
		programs.put(GeneratorEntry.START, token -> start());
		programs.put(GeneratorEntry.CONDITIONAL_JUMP, token -> conditionalJump());
		programs.put(GeneratorEntry.UNCONDITIONAL_JUMP, token -> unconditionalJump());
		programs.put(GeneratorEntry.END, token -> end(generating));
		programs.put(GeneratorEntry.EXTRA, token -> extra());
	}

	private void initOperations() {
		operations.put(GeneratorEntry.PLUS, OperationType.ADDITION);
		operations.put(GeneratorEntry.MINUS, OperationType.SUBTRACTION);
		operations.put(GeneratorEntry.STAR, OperationType.MULTIPLICATION);
		operations.put(GeneratorEntry.SLASH, OperationType.DIVISION);
		operations.put(GeneratorEntry.EQUAL, OperationType.ASSIGNMENT);
		operations.put(GeneratorEntry.INDEXING, OperationType.INDEXING);
		operations.put(GeneratorEntry.INPUT, OperationType.INPUT);
		operations.put(GeneratorEntry.OUTPUT, OperationType.OUTPUT);
		operations.put(GeneratorEntry.CONDITIONAL_JUMP, OperationType.CONDITIONAL_JUMP);
		operations.put(GeneratorEntry.UNCONDITIONAL_JUMP, OperationType.UNCONDITIONAL_JUMP);
		operations.put(GeneratorEntry.GREATER, OperationType.GREATER);
		operations.put(GeneratorEntry.LESS, OperationType.LESS);
	}

	private void initMagazine() {
		magazine.addLast("P");
		magazine.addLast("$");
	}

	private void initGenerator() {
		generator.addLast(GeneratorEntry.EMPTY);
		generator.addLast(GeneratorEntry.EMPTY);
	}

	private void inflate() {
		columns();
		rows();
		table();
	}

	private void columns() {
		columns.put(TokenType.LESS, 0);
		columns.put(TokenType.GREATER, 1);
		columns.put(TokenType.LEFT_PAREN, 2);
		columns.put(TokenType.RIGHT_PAREN, 3);
		columns.put(TokenType.LEFT_BRACKET, 4);
		columns.put(TokenType.RIGHT_BRACKET, 5);
		columns.put(TokenType.LEFT_BRACE, 6);
		columns.put(TokenType.RIGHT_BRACE, 7);
		columns.put(TokenType.COMMA, 8);
		columns.put(TokenType.MINUS, 9);
		columns.put(TokenType.PLUS, 10);
		columns.put(TokenType.SEMICOLON, 11);
		columns.put(TokenType.SLASH, 12);
		columns.put(TokenType.STAR, 13);
		columns.put(TokenType.EQUAL, 14);
		columns.put(TokenType.IDENTIFIER, 15);
		columns.put(TokenType.INT_LITERAL, 16);
		columns.put(TokenType.WHILE, 17);
		columns.put(TokenType.IF, 18);
		columns.put(TokenType.ELSE, 19);
		columns.put(TokenType.INPUT, 20);
		columns.put(TokenType.OUTPUT, 21);
		columns.put(TokenType.ARRAY, 22);
		columns.put(TokenType.INT_TYPE, 23);
		columns.put(TokenType.EOF, 24);
	}

	private void rows() {
		rows.put("P", 0);
		rows.put("Q", 1);
		rows.put("I", 2);
		rows.put("M", 3);
		rows.put("A", 4);
		rows.put("N", 5);
		rows.put("H", 6);
		rows.put("V", 7);
		rows.put("U", 8);
		rows.put("B", 9);
		rows.put("K", 10);
		rows.put("D", 11);
		rows.put("S", 12);
		rows.put("T", 13);
		rows.put("F", 14);
		rows.put("Z", 15);
	}

	private void init() {
		for (int row = 0; row < NUM_ROWS; row++)
			for (int column = 0; column < NUM_COLUMNS; column++)
				table[row][column] = TableEntry.empty();
	}

	/* Inflates the parsing table rules for different states (eg. Q, I) */
	private void table() {
		init();
		p();
		q();
		i();
		m();
		a();
		n();
		h();
		v();
		u();
		b();
		k();
		d();
		s();
		t();
		f();
		z();
	}

	private void p() {
		final int p = rows.get("P");
		for (int column = 0; column < NUM_COLUMNS; column++)
			table[p][column] = TableEntry.lambda();
		sharedRules(p);

		table[p][columns.get(TokenType.ARRAY)] = new TableEntry(
				new String[]{"array", "A", "P"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);

		table[p][columns.get(TokenType.INT_TYPE)] = new TableEntry(
				new String[]{"int", "I", "P"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);
	}

	private void q() {
		final int q = rows.get("Q");
		for (int column = 0; column < NUM_COLUMNS; column++)
			table[q][column] = TableEntry.lambda();
		sharedRules(q);

		table[q][columns.get(TokenType.ARRAY)] = new TableEntry(
				new String[]{"array", "A", "Q"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);

		table[q][columns.get(TokenType.INT_TYPE)] = new TableEntry(
				new String[]{"int", "I", "Q"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);
	}

	private void i() {
		final int i = rows.get("I");

		table[i][columns.get(TokenType.IDENTIFIER)] = new TableEntry(
				new String[]{"a", "M"},
				new GeneratorEntry[]{GeneratorEntry.INT_NAME, GeneratorEntry.EMPTY}
		);
	}

	private void m() {
		final int m = rows.get("M");

		table[m][columns.get(TokenType.COMMA)] = new TableEntry(
				new String[]{",", "a", "M"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.INT_NAME,
					GeneratorEntry.EMPTY
				}
		);

		table[m][columns.get(TokenType.SEMICOLON)] = new TableEntry(
				new String[]{";"},
				new GeneratorEntry[]{GeneratorEntry.EMPTY}
		);
	}

	private void a() {
		final int a = rows.get("A");

		table[a][columns.get(TokenType.IDENTIFIER)] = new TableEntry(
				new String[]{"a", "[", "S", "]", "N"},
				new GeneratorEntry[]{
					GeneratorEntry.ARRAY_NAME,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.ARRAY_SIZE,
					GeneratorEntry.EMPTY
				}
		);
	}

	private void n() {
		final int n = rows.get("N");

		table[n][columns.get(TokenType.COMMA)] = new TableEntry(
				new String[]{",", "a", "[", "S", "]", "N"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.ARRAY_NAME,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.ARRAY_SIZE,
					GeneratorEntry.EMPTY
				}
		);

		table[n][columns.get(TokenType.SEMICOLON)] = new TableEntry(
				new String[]{";"},
				new GeneratorEntry[]{GeneratorEntry.EMPTY}
		);
	}

	private void h() {
		final int h = rows.get("H");
		for (int column = 0; column < NUM_COLUMNS; column++)
			table[h][column] = TableEntry.lambda();

		table[h][columns.get(TokenType.LEFT_BRACKET)] = new TableEntry(
				new String[]{"[", "S", "]"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.INDEXING
				}
		);
	}

	private void v() {
		final int v = rows.get("V");
		for (int column = 0; column < NUM_COLUMNS; column++)
			table[v][column] = TableEntry.lambda();

		table[v][columns.get(TokenType.SLASH)] = new TableEntry(
				new String[]{"/", "F", "V"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.SLASH
				}
		);

		table[v][columns.get(TokenType.STAR)] = new TableEntry(
				new String[]{"*", "F", "V"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.STAR
				}
		);
	}

	private void u() {
		final int u = rows.get("U");
		for (int column = 0; column < NUM_COLUMNS; column++)
			table[u][column] = TableEntry.lambda();

		table[u][columns.get(TokenType.MINUS)] = new TableEntry(
				new String[]{"-", "T", "U"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.MINUS
				}
		);

		table[u][columns.get(TokenType.PLUS)] = new TableEntry(
				new String[]{"+", "T", "U"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.PLUS
				}
		);
	}

	private void b() {
		final int b = rows.get("B");

		table[b][columns.get(TokenType.LESS)] = new TableEntry(
				new String[]{"<", "S", "Z"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.LESS
				}
		);

		table[b][columns.get(TokenType.GREATER)] = new TableEntry(
				new String[]{">", "S", "Z"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.GREATER
				}
		);
	}

	private void k() {
		final int k = rows.get("K");
		for (int column = 0; column < NUM_COLUMNS; column++)
			table[k][column] = TableEntry.lambda();

		table[k][columns.get(TokenType.ELSE)] = new TableEntry(
				new String[]{"else", "{", "Q", "}"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EXTRA,
					GeneratorEntry.EMPTY,
					GeneratorEntry.END
				}
		);
	}

	private void d() {
		final int d = rows.get("D");

		table[d][columns.get(TokenType.LEFT_PAREN)] = new TableEntry(
				new String[]{"(", "S", ")", "V", "U", "B"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);

		table[d][columns.get(TokenType.IDENTIFIER)] = new TableEntry(
				new String[]{"a", "H", "V", "U", "B"},
				new GeneratorEntry[]{
					GeneratorEntry.IDENTIFIER,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);

		table[d][columns.get(TokenType.INT_LITERAL)] = new TableEntry(
				new String[]{"k", "V", "U", "B"},
				new GeneratorEntry[]{
					GeneratorEntry.CONSTANT,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);
	}

	private void s() {
		final int s = rows.get("S");

		table[s][columns.get(TokenType.LEFT_PAREN)] = new TableEntry(
				new String[]{"(", "S", ")", "V", "U"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);

		table[s][columns.get(TokenType.IDENTIFIER)] = new TableEntry(
				new String[]{"a", "H", "V", "U"},
				new GeneratorEntry[]{
					GeneratorEntry.IDENTIFIER,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);

		table[s][columns.get(TokenType.INT_LITERAL)] = new TableEntry(
				new String[]{"k", "V", "U"},
				new GeneratorEntry[]{
					GeneratorEntry.CONSTANT,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);
	}

	private void t() {
		final int t = rows.get("T");

		table[t][columns.get(TokenType.LEFT_PAREN)] = new TableEntry(
				new String[]{"(", "S", ")", "V"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);

		table[t][columns.get(TokenType.IDENTIFIER)] = new TableEntry(
				new String[]{"a", "H", "V"},
				new GeneratorEntry[]{
					GeneratorEntry.IDENTIFIER,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);

		table[t][columns.get(TokenType.INT_LITERAL)] = new TableEntry(
				new String[]{"k", "V"},
				new GeneratorEntry[]{
					GeneratorEntry.CONSTANT,
					GeneratorEntry.EMPTY
				}
		);
	}

	private void f() {
		final int f = rows.get("F");

		table[f][columns.get(TokenType.LEFT_PAREN)] = new TableEntry(
				new String[]{"(", "S", ")"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);

		table[f][columns.get(TokenType.IDENTIFIER)] = new TableEntry(
				new String[]{"a", "H"},
				new GeneratorEntry[]{
					GeneratorEntry.IDENTIFIER,
					GeneratorEntry.EMPTY
				}
		);

		table[f][columns.get(TokenType.INT_LITERAL)] = new TableEntry(
				new String[]{"k"},
				new GeneratorEntry[]{
					GeneratorEntry.CONSTANT
				}
		);
	}

	private void z() {
		final int z = rows.get("Z");
		for (int column = 0; column < NUM_COLUMNS; column++)
			table[z][column] = TableEntry.lambda();
	}

	/*Inflates the common rules between the P and the Q states
	@param row the index of the table’s row */
	private void sharedRules(int row) {
		table[row][columns.get(TokenType.IDENTIFIER)] = new TableEntry(
				new String[]{"a", "H", "=", "S", ";", "Q"},
				new GeneratorEntry[]{
					GeneratorEntry.IDENTIFIER,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EQUAL,
					GeneratorEntry.EMPTY
				}
		);

		table[row][columns.get(TokenType.WHILE)] = new TableEntry(
				new String[]{"while", "(", "D", ")", "{", "Q", "}", "Q"},
				new GeneratorEntry[]{
					GeneratorEntry.START,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.CONDITIONAL_JUMP,
					GeneratorEntry.EMPTY,
					GeneratorEntry.UNCONDITIONAL_JUMP,
					GeneratorEntry.EMPTY
				}
		);

		table[row][columns.get(TokenType.IF)] = new TableEntry(
				new String[]{"if", "(", "D", ")", "{", "Q", "}", "K", "Q"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.CONDITIONAL_JUMP,
					GeneratorEntry.EMPTY,
					GeneratorEntry.END,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY
				}
		);

		table[row][columns.get(TokenType.INPUT)] = new TableEntry(
				new String[]{"input", "(", "a", "H", ")", ";", "Q"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.IDENTIFIER,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.INPUT,
					GeneratorEntry.EMPTY
				}
		);

		table[row][columns.get(TokenType.OUTPUT)] = new TableEntry(
				new String[]{"output", "(", "a", "H", ")", ";", "Q"},
				new GeneratorEntry[]{
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.IDENTIFIER,
					GeneratorEntry.EMPTY,
					GeneratorEntry.EMPTY,
					GeneratorEntry.OUTPUT,
					GeneratorEntry.EMPTY
				}
		);
	}

	private boolean isTerminal(String lexeme) {
		return !rows.containsKey(lexeme);
	}

	/* The leftmost derivation process
	@param token a current token to be processed
	@returns a derivative */
	private String update(final Token token) {
		final String leftDerivative = magazine.pop();
		final GeneratorEntry action = generator.pop();
		if (action.isOperand()) {
			if (action == GeneratorEntry.IDENTIFIER)
				addOperand(new Variable(token.lexeme()));
			else addOperand(new Literal((int) token.literal()));
		}

		if (action.isOperation()) {
			final OperationType type = operations.get(action);
			addOperation(type);
		}

		if (action.isProgram()) execute(action, token);

		return leftDerivative;
	}

	/* This executes the pre-defined programs
	@param action a program entry from the magazine stack
	@param token a processing token */
	private void execute(final GeneratorEntry action, final Token token) {
		final Consumer<Token> program = programs.get(action);
		program.accept(token);
	}

	/* Represents the program #8 */
	private void extra() {
		if (lastCachedIndex >= 0) end(generating + 2, lastCachedIndex);
		else end(generating + 2);
		mark();
		addEmptyOperand();
		addOperation(OperationType.UNCONDITIONAL_JUMP);
	}

	/* Represents the program #4 */
	private void conditionalJump() {
		mark();
		addEmptyOperand();
		addOperation(OperationType.CONDITIONAL_JUMP);
	}

	public void addEmptyOperand() {
		addOperand(new Literal(0));
	}

	/* Represents the program #6 */
	private void unconditionalJump() {
		end(generating + 2);
		addOperand(new Literal(marks.pop()));
		addOperation(OperationType.UNCONDITIONAL_JUMP);
	}

	/* Represents the program #7
	@param value a new value */
	private void end(final int value) {
		lastCachedIndex = marks.pop();
		changeOperandAt(lastCachedIndex, value);
	}

	/* Represents the program #7
	@param value a new value
	@param index a concrete index of the generating element in the RPN */
	private void end(final int value, final int index) {
		changeOperandAt(index, value);
	}

	/* Represents the program #5 */
	private void start() {
		mark();
	}

	/* Creates a new mark for if clauses and while loops */
	private void mark() {
		marks.addFirst(generating);
	}

	/* Represents the programs #1–2
	@param lexeme a variable name to be checked */
	private void name(final String lexeme) {
		addOperand(new Variable(lexeme));
	}

	/* Represents the program #3 */
	private void createArray() {
		addOperation(OperationType.ARRAY_CREATION);
	}

	public Notation parse() {
		while (!magazine.isEmpty() && !isAtEnd()) {
			final Token token = peek();
			final String leftDerivative = update(token);
			if (isTerminal(leftDerivative)) {
				if (!matches(leftDerivative, token)) {
					error("Expected '%s', but found '%s'.".formatted(leftDerivative, token.lexeme()));
					break;
				}
				advance();
				continue;
			}

			final int column = columns.getOrDefault(token.type(), -1);
			final int row = rows.getOrDefault(leftDerivative, -1);
			if (!isValid(column, row)) break;

			final TableEntry entry = table[row][column];
			if (entry.isLambda()) continue;
			if (entry.isNotEmpty()) derive(entry);
			else {
				error(unexpectedTokenMessage(row, token));
				break;
			}
		}

		if (!isAtEnd() || !magazine.isEmpty()) error("Unexpected syntax.");

		return new Notation(raw);
	}

	private String unexpectedTokenMessage(final int row, final Token token) {
		final List<String> expected = new ArrayList<>();
		for (Map.Entry<String, TokenType> terminal : terminals.entrySet()) {
			final Integer column = columns.get(terminal.getValue());
			if (column == null) continue;

			if (table[row][column].isNotEmpty() && !table[row][column].isLambda())
				expected.add("'" + terminal.getKey() + "'");
		}

		if (expected.isEmpty()) return "Unexpected token '%s'.".formatted(token.lexeme());

		return "Unexpected token '%s'. Expected one of: %s."
				.formatted(token.lexeme(), String.join(", ", expected));
	}

	private void error(final String message) {
		if (Main.hadError) return;
		if (tokens.isEmpty()) Main.report(1, 1, message);
		else {
			final Token token = peek();
			Main.report(token.line(), token.column(), message);
		}
	}

	private boolean isValid(final int column, final int row) {
		if (column < 0 || row < 0) {
			String message = "Unexpected column-row values for the parsing table. " +
					"Column is %d, and row is %d.".formatted(column, row);
			error(message);
			return false;
		}

		return true;
	}

	/* Derives new rules based on the entry
	@param entry a parsing table entry */
	private void derive(final TableEntry entry) {
		final int size = entry.production.length;
		for (int idx = size - 1; idx >= 0; idx--) {
			magazine.addFirst(entry.production[idx]);
			generator.addFirst(entry.semantic[idx]);
		}
	}

	private boolean matches(final String terminal, final Token token) {
		return token.type() == terminals.get(terminal);
	}

	/* Advancing along the tokens */
	private void advance() {
		current++;
	}

	/* Sees the current processing token
	@return a processing token */
	private Token peek() {
		if (isAtEnd()) return tokens.getLast();
		return tokens.get(current);
	}

	/* Checks if it is the end of a tokens list
	@returns {@code true} if it is, or {@code false} otherwise */
	private boolean isAtEnd() {
		return current >= tokens.size();
	}

	private void addOperand(final OperandType type) {
		raw.add(new Operand(type));
		generating++;
	}

	private void changeOperandAt(final int index, final Object value) {
		((Operand) raw.get(index)).type.value = value;
	}

	private void addOperation(final OperationType type) {
		raw.add(new Operation(type));
		generating++;
	}
}
