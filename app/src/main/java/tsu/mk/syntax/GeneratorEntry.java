package tsu.mk.syntax;

public enum GeneratorEntry {
	EMPTY,
	IDENTIFIER,
	CONSTANT,
	PLUS,
	MINUS,
	STAR,
	SLASH,
	EQUAL,
	INDEXING,
	INPUT,
	OUTPUT,
	INT_NAME,
	ARRAY_NAME,
	ARRAY_SIZE,
	START,
	CONDITIONAL_JUMP,
	UNCONDITIONAL_JUMP,
	END,
	GREATER,
	LESS,
	EXTRA;
	
	public boolean isEmpty() {
		return this == EMPTY;
	}
	
	public boolean isProgram() {
		return this == INT_NAME
			|| this == ARRAY_NAME
			|| this == ARRAY_SIZE
			|| this == START
			|| this == CONDITIONAL_JUMP
			|| this == UNCONDITIONAL_JUMP
			|| this == END
			|| this == EXTRA;
	}
	
	public boolean isOperand() {
		return !isEmpty()
			&& !isProgram()
			&& (this == IDENTIFIER || this == CONSTANT);
	}
	
	public boolean isOperation() {
		return this == PLUS
			|| this == MINUS
			|| this == STAR
			|| this == SLASH
			|| this == EQUAL
			|| this == INDEXING
			|| this == INPUT
			|| this == OUTPUT
			|| this == GREATER
			|| this == LESS;
	}
}
