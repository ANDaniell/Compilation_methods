package tsu.mk.syntax.operand;

public final class Literal extends OperandType {
	public Literal(final int value) {
		super(value);
	}
	
	@Override
	public String toString() {
		return "Literal %s".formatted(value);
	}
}
