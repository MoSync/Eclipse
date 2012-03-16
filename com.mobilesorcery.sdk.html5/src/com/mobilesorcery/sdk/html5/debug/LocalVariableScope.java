package com.mobilesorcery.sdk.html5.debug;

import java.util.ArrayList;
import java.util.List;

import com.mobilesorcery.sdk.core.Util;

public class LocalVariableScope {

	public final static LocalVariableScope EMPTY = new LocalVariableScope();

	private final LocalVariableScope parent;
	private final String localVar;
	private final LocalVariableScope predecessor;

	public LocalVariableScope() {
		this(null, null, null);
	}

	private LocalVariableScope(LocalVariableScope parent, LocalVariableScope predecessor, String localVar) {
		this.parent = parent;
		this.predecessor = predecessor;
		this.localVar = localVar;
	}

	public LocalVariableScope getParent() {
		return parent;
	}

	public LocalVariableScope getPredecessor() {
		return predecessor;
	}

	public LocalVariableScope addLocalVariableDeclaration(String localVar) {
		if (localVar == null) {
			return this;
		}
		return new LocalVariableScope(parent, this, localVar);
	}

	public LocalVariableScope nestScope() {
		return new LocalVariableScope(this, null, null);
	}

	public LocalVariableScope unnestScope() {
		if (parent == null) {
			throw new IllegalStateException();
		}
		LocalVariableScope newParent = parent.getParent();
		LocalVariableScope newPredecessor = parent;
		return new LocalVariableScope(newParent, newPredecessor, null);
	}

	public List<String> getLocalVariables() {
		ArrayList<String> result = new ArrayList<String>();
		if (localVar != null) {
			result.add(localVar);
		}
		if (predecessor != null) {
			result.addAll(predecessor.getLocalVariables());
		} else if (parent != null) {
			result.addAll(parent.getLocalVariables());
		}
		return result;
	}

	@Override
	public String toString() {
		return Util.join(getLocalVariables().toArray(), ", ");
	}

	public static void main(String[] args) {
		LocalVariableScope scope = new LocalVariableScope();
		System.err.println(":" + scope);
		scope = scope.addLocalVariableDeclaration("a");
		System.err.println(":" + scope);
		scope = scope.nestScope();
		System.err.println(":" + scope);
		scope = scope.addLocalVariableDeclaration("b");
		System.err.println(":" + scope);
		scope = scope.unnestScope();
		System.err.println(":" + scope);
		scope = scope.addLocalVariableDeclaration("c");
		System.err.println(":" + scope);
	}
}
