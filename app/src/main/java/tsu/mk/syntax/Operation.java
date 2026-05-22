package tsu.mk.syntax;

public final class Operation extends NotationEntry {
	public final OperationType type;
	
	public Operation(OperationType type) {
		this.type = type;
	}
	
	@Override
	public String toString() {
		return "Operation %s".formatted(type);
	}
}
