package com.mobilesorcery.sdk.html5.debug;

// This class is a bit confusing. Cleanup please.
public class RedefinitionResult {

	public static final int UNDETERMINED = 0;
	public static final int TERMINATE = 1;
	public static final int RELOAD = 2;
	public static final int CONTINUE = 3;
	
	
	public static final int REDEFINE_OK = 1 << 1;
	public static final int CANNOT_REDEFINE = 1 << 2;
	public static final int CANNOT_RELOAD = 1 << 3;
	public static final int SHOULD_RELOAD = 1 << 4;
	
	private static final RedefinitionResult OK_INSTANCE = new RedefinitionResult(REDEFINE_OK, "OK");

	private int flags;
	private String msg;

	private RedefinitionResult() {
		
	}
	
	public RedefinitionResult(int flags, String msg) {
		this.flags = flags;
		this.msg = msg;
	}
	
	public boolean isFlagSet(int flag) {
		return (this.flags & flag) != 0;
	}
	
	public static RedefinitionResult ok() {
		return OK_INSTANCE;
	}
	
	public String getMessage() {
		return msg;
	}
	
	public static boolean isOk(RedefinitionResult result) {
		return result.isFlagSet(REDEFINE_OK);
	}

	public static RedefinitionResult fail(String msg) {
		return new RedefinitionResult(CANNOT_REDEFINE, msg);
	}
	
	public static RedefinitionResult unrecoverable(String msg) {
		return new RedefinitionResult(CANNOT_REDEFINE | CANNOT_RELOAD, msg);
	}
	
	public RedefinitionResult merge(RedefinitionResult other) {
		RedefinitionResult result = new RedefinitionResult();
		boolean ok = isOk(this) && isOk(other);
		boolean cannotReload = isFlagSet(CANNOT_RELOAD) || other.isFlagSet(CANNOT_RELOAD);
		boolean cannotRedefine = isFlagSet(CANNOT_REDEFINE) || other.isFlagSet(CANNOT_REDEFINE);
		boolean shouldReload = isFlagSet(SHOULD_RELOAD) || other.isFlagSet(SHOULD_RELOAD);
		
		result.flags = (ok ? REDEFINE_OK : 0) |
				(cannotReload ? CANNOT_RELOAD : 0) | 
				(cannotRedefine ? CANNOT_REDEFINE : 0) |
				(shouldReload ? SHOULD_RELOAD : 0);
		result.msg = this.flags > other.flags ? this.msg : other.msg;
		return result;
	}

}
