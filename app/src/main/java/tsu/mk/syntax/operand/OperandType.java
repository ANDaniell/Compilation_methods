package tsu.mk.syntax.operand;

public sealed class OperandType permits Literal, Variable, Address {
	public Object value;
	
	public OperandType(final Object value) {
		this.value = value;
	}
}
