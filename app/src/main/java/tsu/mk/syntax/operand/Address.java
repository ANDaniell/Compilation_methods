package tsu.mk.syntax.operand;

public final class Address extends OperandType {
	public Address(final int value) {
		super(value);
	}
	
	@Override
	public String toString() {
		return "Address %s".formatted(value);
	}
}
