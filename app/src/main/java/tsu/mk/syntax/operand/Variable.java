package tsu.mk.syntax.operand;

public final class Variable extends OperandType {
	public Variable(final String value) {
		super(value);
	}
	
	@Override
	public String toString() {
		return "Variable %s".formatted(value);
	}
}
