package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;
import com.mobilesorcery.sdk.html5.debug.IRedefiner;
import com.mobilesorcery.sdk.html5.debug.RedefineException;
import com.mobilesorcery.sdk.html5.debug.RedefinitionResult;
import com.mobilesorcery.sdk.html5.debug.hotreplace.FileRedefinable;
import com.mobilesorcery.sdk.html5.debug.hotreplace.FunctionRedefinable;

public class ReloadRedefiner implements IRedefiner {

	private ReloadVirtualMachine vm;
	
	private ArrayList<IFile> fileUpdates = new ArrayList<IFile>();
	private ArrayList<FunctionRedefinable> functionUpdates = new ArrayList<FunctionRedefinable>();
	private HashMap<IRedefinable, ReloadStackFrame> matchedFrames;
	private int dropToFrame = -1;

	private RedefinitionResult redefineResult;

	public ReloadRedefiner(ReloadVirtualMachine vm) {
		this.vm = vm;
		this.redefineResult = RedefinitionResult.ok();
	}
	
	@Override
	public void collect(IRedefinable redefinable, IRedefinable replacement) {
		if (redefinable instanceof FileRedefinable) {
			redefineResult = redefineResult.merge(collectFile((FileRedefinable) redefinable, (FileRedefinable) replacement));
		} else if (redefinable instanceof FunctionRedefinable) {
			redefineResult = redefineResult.merge(collectFunction((FunctionRedefinable) redefinable, (FunctionRedefinable) replacement));
		}
	}

	private RedefinitionResult collectFunction(FunctionRedefinable redefinable,
			FunctionRedefinable replacement) {
		RedefinitionResult result = redefinable.canRedefine(replacement);
		if (!RedefinitionResult.isOk(result)) {
			return result;
		}
		FunctionRedefinable replacementFunction = (FunctionRedefinable) replacement;
		if (Util.equals(replacementFunction.getFunctionSource(), redefinable.getFunctionSource())) {
			return RedefinitionResult.ok();
		}
		functionUpdates.add(replacement);
		ReloadThreadReference mainThread = vm.mainThread();
		if (mainThread.isSuspended()) {
			if (matchedFrames == null) {
				matchFrames(mainThread);
			}
			ReloadStackFrame frame = matchedFrames.get(redefinable);
			if (frame != null) {
				int stackDepth = frame.getStackDepth();
				// As usual: reverse!
				dropToFrame = Math.max(dropToFrame, mainThread.frameCount() - stackDepth - 1);
			}
		}
		return RedefinitionResult.ok();
	}

	private void matchFrames(ReloadThreadReference mainThread) {
		matchedFrames = new HashMap<IRedefinable, ReloadStackFrame>();
		for (Object frameObj : mainThread.frames()) {
			ReloadStackFrame frame = (ReloadStackFrame) frameObj;
			// NOT YET IMPL.
		}
	}

	private RedefinitionResult collectFile(FileRedefinable redefinable,
			FileRedefinable replacement) {
		fileUpdates.add(redefinable.getFile());
		return RedefinitionResult.ok();
	}

	@Override
	public void commit(boolean reloadHint) throws RedefineException {
		boolean isOk = RedefinitionResult.isOk(redefineResult) || (reloadHint && !redefineResult.isFlagSet(RedefinitionResult.CANNOT_RESTART));
		if (!isOk) {
			throw new RedefineException(redefineResult);
		}
		
		for (IFile fileUpdate : fileUpdates) {
			vm.update(fileUpdate);
		}
		
		if (reloadHint) {
			vm.reload();
		} else {
			for (FunctionRedefinable functionUpdate : functionUpdates) {
				vm.updateFunction(functionUpdate.key(), functionUpdate.getFunctionSource());
			}
			
			if (dropToFrame >= 0) {
				vm.dropToFrame(dropToFrame);
			}
		}
	}

	

}
