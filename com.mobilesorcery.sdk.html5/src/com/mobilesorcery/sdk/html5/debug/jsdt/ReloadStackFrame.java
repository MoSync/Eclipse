package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.jsdt.debug.core.jsdi.Location;
import org.eclipse.wst.jsdt.debug.core.jsdi.StackFrame;
import org.eclipse.wst.jsdt.debug.core.jsdi.Value;
import org.eclipse.wst.jsdt.debug.core.jsdi.Variable;
import org.eclipse.wst.jsdt.debug.core.jsdi.VirtualMachine;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mobilesorcery.sdk.html5.debug.LocalVariableScope;

public class ReloadStackFrame implements StackFrame {

	private final static JSONParser PARSER = new JSONParser();

	private final ReloadVirtualMachine vm;
	private Variable thisVar;
	private Location location;

	private boolean inited;

	private ReloadVariable argsVar;

	private ArrayList<ReloadVariable> localVars;

	public ReloadStackFrame(ReloadVirtualMachine vm, JSONObject suspendCommand,
			int ix) {
		this.vm = vm;
		init(suspendCommand, ix);
	}

	private void init(JSONObject suspended, int ix) {
		String file = (String) suspended.get("file");
		int line = ((Long) suspended.get("line")).intValue();
		IFile resource = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(file));
		location = new SimpleLocation(vm, resource, line);
	}

	@Override
	public VirtualMachine virtualMachine() {
		return vm;
	}

	@Override
	public Variable thisObject() {
		initVars();
		return thisVar;
	}

	private void initVars() {
		if (!inited) {
			inited = true;
			thisVar = new ReloadVariable(vm, this, "this", "this");
			argsVar = new ReloadVariable(vm, this, "arguments", "arguments");
			localVars = new ArrayList<ReloadVariable>();
			LocalVariableScope scope = vm.getLocalVariableScope(location);
			if (scope != null) {
				for (String localVar : scope.getLocalVariables()) {
					localVars.add(new ReloadVariable(vm, this, localVar, localVar));
				}
			}
		}
	}

	@Override
	public List variables() {
		initVars();
		ArrayList result = new ArrayList();
		result.add(argsVar);
		result.addAll(localVars);
		return result;
	}

	@Override
	public Location location() {
		return location;
	}

	@Override
	public Value evaluate(String expression) {
		return parse(internalEvaluate(expression));
	}

	private String internalEvaluate(String expression) {
		String valueStr;
		try {
			valueStr = vm.evaluate(expression);
		} catch (Exception e) {
			valueStr = null;
		}
		return valueStr;
	}

	private Value parse(String valueStr) {
		return vm.mirrorOf(valueStr);
	}

	public Value getValue(String name) {
		String metaExpr = String.format(
				"var ____info = {};" +
				"var ____keys = [];" +
				"var ____typeOf = typeof(%s);" +
				"if (____typeOf == \"object\" && %s != null) {" +
				"  if (!%s.____oid) {" +
				"    MoSyncDebugProtocol.assignOID(%s);" +
				"  }" +
				"  ____info.oid = %s.____oid;" +
				"  for (var ____key in %s) {" +
				"    ____keys.push(____key);" +
				"  }" +
				"  ____info.properties = ____keys;" +
				"  ____info.class = ____info.constructor ? ____info.constructor.toString() : null;" +
				"  ____info.repr = %s.toString();" +
				"} else if (____typeOf == \"function\") {" +
				"  ____info.repr = ____typeOf;" +
				"} else {" +
				"  ____info.repr = %s;" +
				"}" +
				"____info.type = ____typeOf; ____info;"
				, name, name, name, name, name, name, name, name, name);
		String metaEvaluation = internalEvaluate(metaExpr);
		try {
			if (metaEvaluation != null) {
				JSONObject metaObject = (JSONObject) PARSER.parse(metaEvaluation);
				String type = (String) metaObject.get("type");
				String repr = "" + metaObject.get("repr");
				if ("object".equals(type) || "function".equals(type)) {
					Number oid = (Number) metaObject.get("oid");
					String className = (String) metaObject.get("class");
					JSONArray properties = (JSONArray) metaObject.get("properties");
					ArrayList<ReloadProperty> generatedProperties = new ArrayList<ReloadProperty>();
					boolean hasArrayProperty = false;
					boolean hasLengthProperty = false;

					for (int i = 0; properties != null && i < properties.size(); i++) {
						String propertyName = (String) properties.get(i);
						hasLengthProperty |= "length".equals(propertyName);
						boolean isArrayProperty = Character.isDigit(propertyName.charAt(0));
						hasArrayProperty |= isArrayProperty;
						String fullName = isArrayProperty ? name + "[" + propertyName + "]" : name + "." + propertyName;
						boolean isSpecialProperty = "____oid".equals(propertyName);
						if (!isSpecialProperty) {
							generatedProperties.add(new ReloadProperty(vm, this, fullName, propertyName));
						}
					}
					boolean isArray = hasArrayProperty; // && hasLengthProperty;
					ReloadObjectReference ref = isArray ? new ReloadArrayReference(vm, repr, oid) : new ReloadObjectReference(vm, repr, className, oid);
					for (ReloadProperty generatedProperty : generatedProperties) {
						ref.addProperty(generatedProperty);
					}
					return ref;
				} else if ("null".equals(type)) {
					return vm.mirrorOfNull();
				} else if ("string".equals(type)) {
					return vm.mirrorOf(repr);
				} else if ("number".equals(type)) {
					Number reprNum = Double.parseDouble(repr);
					return vm.mirrorOf(reprNum);
				} else if ("boolean".equals(type)) {
					Boolean reprBool = Boolean.parseBoolean(repr);
					return vm.mirrorOf(reprBool);
				} else {
					return vm.mirrorOfUndefined();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			// IGNORE.
		}
		return vm.mirrorOfUndefined();
	}
}
