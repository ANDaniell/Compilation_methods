package tsu.mk.interpretation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiFunction;

import tsu.mk.Main;
import tsu.mk.core.ArrayEntry;
import tsu.mk.syntax.NotationEntry;
import tsu.mk.syntax.Operand;
import tsu.mk.syntax.Operation;
import tsu.mk.syntax.OperationType;
import tsu.mk.syntax.operand.Address;
import tsu.mk.syntax.operand.Literal;
import tsu.mk.syntax.operand.OperandType;
import tsu.mk.syntax.operand.Variable;

public class Interpreter {
	private final static Map<String, Integer> variables;
	private final static Map<String, ArrayEntry> arrays;
	private static final List<Integer> memory;
	private static int address;
	private final Deque<NotationEntry> stack;
	private final List<NotationEntry> entries;
	private final Map<OperationType, Interpretation> interpretations;
	private final Scanner reader;
	private final BufferedWriter writer;
	private int current;
	
	static {
		variables = new LinkedHashMap<>();
		arrays = new LinkedHashMap<>();
		address = 0;
		memory = new ArrayList<>();
	}
	
	public Interpreter(final List<NotationEntry> entries) {
		this.stack = new ArrayDeque<>();
		this.entries = entries;
		this.interpretations = new LinkedHashMap<>();
		this.reader = new Scanner(System.in);
		this.writer = new BufferedWriter(new OutputStreamWriter(System.out));
		this.current = 0;
		
		initOperations();
	}
	
	private void initOperations() {
		interpretations.put(OperationType.ADDITION, () -> calculate(this::add));
		interpretations.put(OperationType.SUBTRACTION, () -> calculate(this::subtract));
		interpretations.put(OperationType.MULTIPLICATION, () -> calculate(this::multiply));
		interpretations.put(OperationType.DIVISION, () -> calculate(this::divide));
		interpretations.put(OperationType.INDEXING, this::index);
		interpretations.put(OperationType.INT_CREATION, this::createInt);
		interpretations.put(OperationType.ARRAY_CREATION, this::createArray);
		interpretations.put(OperationType.ASSIGNMENT, this::assign);
		interpretations.put(OperationType.INPUT, this::input);
		interpretations.put(OperationType.OUTPUT, this::output);
		interpretations.put(OperationType.UNCONDITIONAL_JUMP, this::unconditionalJump);
		interpretations.put(OperationType.CONDITIONAL_JUMP, this::conditionalJump);
		interpretations.put(OperationType.GREATER, () -> calculate(this::greater));
		interpretations.put(OperationType.LESS, () -> calculate(this::less));
	}
	
	private void error(final String message) {
		if (!Main.hadError) Main.report(message);
	}
	
	private void conditionalJump() {
		final int mark = popInt();
		final int predicate = popInt();
		
		if (predicate <= 0) jumpTo(mark);
	}
	
	private void unconditionalJump() {
		final int mark = popInt();
		jumpTo(mark);
	}
	
	private void jumpTo(final int mark) {
		current = mark;
	}
	
	private void output() {
		final int value = popInt();
		try {
			writer.write("%d\n".formatted(value));
		} catch (IOException e) {
			error("An unexpected error in the output.");
		}
	}
	
	private void input() {
		final int address = popAddress();
		if (reader.hasNextInt()) {
			final int value = reader.nextInt();
			modifyAt(address, value);
		} else error("An unexpected error in the input.");
	}
	
	private void assign() {
		final int value = popInt();
		final int address = popAddress();
		if (address >= 0 && value >= 0 && address < Interpreter.address)
			modifyAt(address, value);
		else error("An unexpected error in the assignment.");
	}
	
	private void createArray() {
		final int size = popInt();
		final String name = popVariable();
		if (!name.isBlank() && !arrays.containsKey(name)) malloc(name, size);
		else error("An unexpected error in the array creation.");
	}
	
	private void createInt() {
		final String name = popVariable();
		if (!name.isBlank() && !variables.containsKey(name)) malloc(name);
		else error("An unexpected error in the int creation.");
	}
	
	private void index() {
		final int index = popInt();
		final String name = popVariable();
		if (arrays.containsKey(name)) {
			final ArrayEntry entry = arrays.get(name);
			if (index >= 0 && index < entry.size())
				stack.addLast(new Operand(new Address(entry.address() + index)));
			else error("The index %d is out of bounds.".formatted(index));
		} else error("An unexpected error in the array indexing.");
	}
	
	/* Pops a variable from the stack (if there is any)
	@returns a variable name, otherwise an empty string */
	private String popVariable() {
		if (!stack.isEmpty()) {
			final NotationEntry last = stack.removeLast();
			return getVariableName(last);
		} else return "";
	}
	
	/* Pops an int value from the stack (if there is any)
	@returns an int value, otherwise -1 */
	private int popInt() {
		if (!stack.isEmpty()) {
			final NotationEntry last = stack.removeLast();
			return getInt(last);
		} else return -1;
	}
	
	/* Pops an address value from the stack (if there is any)
	@returns an address value, otherwise -1 */
	private int popAddress() {
		if (!stack.isEmpty()) {
			final NotationEntry last = stack.removeLast();
			return getAddress(last);
		} else return -1;
	}
	
	private String getVariableName(NotationEntry entry) {
		final Operand operand = (Operand) entry;
		if (operand.type.getClass().equals(Variable.class))
			return (String) operand.type.value;
		else {
			error("The operand is not a variable.");
			return "";
		}
	}
	
	private void calculate(final BiFunction<Integer, Integer, Integer> action) {
		final int b = popInt();
		final int a = popInt();
		if (a >= 0 && b >= 0) {
			final int result = action.apply(a, b);
			stack.addLast(new Operand(new Literal(result)));
		} else error("The operands are not positive values.");
	}
	
	private int add(final int a, final int b) {
		return a + b;
	}
	
	private int subtract(final int a, final int b) {
		return a - b;
	}
	
	private int multiply(final int a, final int b) {
		return a * b;
	}
	
	private int divide(final int a, final int b) {
		return a / b;
	}
	
	private int greater(final int a, final int b) {
		return a > b ? 1 : 0;
	}
	
	private int less(final int a, final int b) {
		return a < b ? 1 : 0;
	}
	
	private int getInt(NotationEntry entry) {
		final OperandType type = ((Operand) entry).type;
		if (type.getClass().equals(Variable.class)) {
			final int address = variables.get((String) type.value);
			return valueAt(address);
		} else if (type.getClass().equals(Literal.class)) {
			return (int) type.value;
		} else if (type.getClass().equals(Address.class)) {
			final int address = (int) type.value;
			return valueAt(address);
		}
		
		return -1;
	}
	
	/* Retrieves an int value from the memory (if there is any)
	@param address a memory address
	@return an int value, otherwise -1 */
	private int valueAt(final int address) {
		return address < memory.size() ? memory.get(address) : -1;
	}
	
	/* Modifies an int value in the memory
	@param address a memory address
	@param value an int value, otherwise -1 */
	private void modifyAt(final int address, final int value) {
		if (valueAt(address) != -1) memory.set(address, value);
	}
	
	private Integer getAddress(NotationEntry entry) {
		final OperandType type = ((Operand) entry).type;
		if (type.getClass().equals(Address.class)) {
			return (int) type.value;
		} else if (type.getClass().equals(Variable.class)) {
			final String name = (String) type.value;
			return addressAt(name);
		}
		
		return -1;
	}
	
	/* Retrieves a variable address from the memory
	@param name a variable name
	@return an address value, otherwise -1 */
	private int addressAt(final String name) {
		return variables.getOrDefault(name, -1);
	}
	
	/* Allocates new memory for a zero variable
	@param name a variable name */
	private void malloc(final String name) {
		addVariable(name);
		occupy();
	}
	
	/* Allocates new memory for an array with values of zero
	@param name an array name
	@param size an array size */
	private void malloc(final String name, final int size) {
		addArray(name, size);
		occupy(size);
	}
	
	private void addVariable(final String name) {
		variables.put(name, address++);
	}
	
	private void addArray(final String name, final int size) {
		arrays.put(name, new ArrayEntry(address, size));
		address += size;
	}
	
	private void occupy() {
		memory.add(0);
	}
	
	private void occupy(final int size) {
		memory.addAll(Collections.nCopies(size, 0));
	}
	
	public void run() {
		while (!isAtEnd()) {
			final NotationEntry entry = advance();
			if (entry.getClass().equals(Operand.class))
				stack.addLast(entry);
			else {
				final Interpretation interpretation = interpretations.get(((Operation) entry).type);
				interpretation.execute();
			}
		}
		
		try {
			reader.close();
			writer.close();
		} catch (IOException e) {
			error("Unexpected error in the I/O closing.");
		}
	}
	
	/* Advancing along the reverse Polish notation (RPN)
	@return a current notation entry */
	private NotationEntry advance() {
		return entries.get(current++);
	}
	
	/* Checks if it is the RPN end
	@return {@code true} if it is, {@code false} otherwise */
	private boolean isAtEnd() {
		return current >= entries.size();
	}
}
