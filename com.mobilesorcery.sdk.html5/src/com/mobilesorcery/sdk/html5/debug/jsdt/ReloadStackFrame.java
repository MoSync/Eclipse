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

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.debug.LocalVariableScope;

public class ReloadStackFrame implements StackFrame {

	private final static JSONParser PARSER = new JSONParser();

	private final ReloadVirtualMachine vm;
	private Variable thisVar;
	private SimpleLocation location;
	private Number catchLine;

	private boolean inited;

	private ArrayList<ReloadVariable> localVars;

	private final int stackDepth;

	private boolean isTop;

	public ReloadStackFrame(ReloadVirtualMachine vm, JSONObject suspendCommand,
			int ix) {
		this.vm = vm;
		this.stackDepth = ix;

		init(suspendCommand, ix);
	}

	private void init(JSONObject suspended, int ix) {
		int line = ((Long) suspended.get("line")).intValue();
		JSONArray stack = (JSONArray) suspended.get("stack");
		JSONArray frame = ix >= 0 ? (JSONArray) stack.get(ix) : null;
		String functionName = frame == null ? "<unknown>" : (String) frame.get(0);
		String file = frame == null ? (String) suspended.get("file") : (String) frame.get(1);
		// For the non-top stack frame, the line has been stored elsewhere.
		isTop = ix == stack.size() - 1;
		if (!isTop) {
			JSONArray nextFrame = (JSONArray) stack.get(ix + 1);
			line = ((Long) nextFrame.get(3)).intValue();
		}
		IFile resource = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(file));
		location = new SimpleLocation(vm, resource, line);
		location.setFunctionName(functionName);
		// For exceptions; where was it caught!?
		this.catchLine = (Number) suspended.get("catchLine");
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
			localVars = new ArrayList<ReloadVariable>();
			int scopeLine = catchLine == null ? location.lineNumber() : catchLine.intValue();
			LocalVariableScope scope = vm.getLocalVariableScope(location.scriptReference(), scopeLine);
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
		result.addAll(localVars);
		return result;
	}

	@Override
	public Location location() {
		return location;
	}

	@Override
	public Value evaluate(String expression) {
		return parse(internalEvaluate(expression, stackDepth));
	}

	private String internalEvaluate(String expression, int stackDepth) {
		Integer stackDepthToSend = isTop ? null : stackDepth + 1;
		String valueStr;
		try {
			valueStr = "" + vm.evaluate(expression, stackDepthToSend);
		} catch (Exception e) {
			valueStr = null;
		}
		return valueStr;
	}

	private Value parse(String valueStr) {
		return vm.mirrorOf(valueStr);
	}
	
	public static String createDropToFrameExpression(int frameToDropTo, String expression) {
		if (Util.isEmpty(expression)) {
			// Noop.
			expression = "{}";
		}
		return MessageFormat.format("MoSyncDebugProtocol.doDropToFrame({0}, {1});", Integer.toString(frameToDropTo), expression);
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
		String metaEvaluation = internalEvaluate(metaExpr, stackDepth);
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
