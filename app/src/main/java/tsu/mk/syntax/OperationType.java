package tsu.mk.syntax;

/* Operation type in the reverse Polish notation (RPN) */
public enum OperationType {
	ADDITION,
	SUBTRACTION,
	MULTIPLICATION,
	DIVISION,
	ASSIGNMENT,
	INT_CREATION,
	ARRAY_CREATION,
	INDEXING,
	INPUT,
	OUTPUT,
	CONDITIONAL_JUMP,
	UNCONDITIONAL_JUMP,
	GREATER,
	LESS;
}
