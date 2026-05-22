package tsu.mk.syntax;

import tsu.mk.syntax.operand.OperandType;

public final class Operand extends NotationEntry {
	public OperandType type;
	
	public Operand(OperandType type) {
		this.type = type;
	}
	
	@Override
	public String toString() {
		return "Operand %s".formatted(type);
	}
}
