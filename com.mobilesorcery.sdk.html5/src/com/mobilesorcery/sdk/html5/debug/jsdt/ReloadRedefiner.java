package com.mobilesorcery.sdk.html5.debug.jsdt;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.DebugException;

import com.mobilesorcery.sdk.core.CoreMoSyncPlugin;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.html5.debug.IRedefinable;
import com.mobilesorcery.sdk.html5.debug.IRedefiner;
import com.mobilesorcery.sdk.html5.debug.RedefineException;
import com.mobilesorcery.sdk.html5.debug.RedefinitionResult;
import com.mobilesorcery.sdk.html5.debug.ReloadVirtualMachine;
import com.mobilesorcery.sdk.html5.debug.hotreplace.FileRedefinable;
import com.mobilesorcery.sdk.html5.debug.hotreplace.FunctionRedefinable;
import com.mobilesorcery.sdk.html5.debug.hotreplace.HTMLRedefinable;

public class ReloadRedefiner implements IRedefiner {

	private ReloadVirtualMachine vm;

	private ArrayList<IFile> fileUpdates = new ArrayList<IFile>();
	private ArrayList<FunctionRedefinable> functionUpdates = new ArrayList<FunctionRedefinable>();
	private int dropToFrame = -1;

	private RedefinitionResult redefineResult;

	private boolean forceReload;

	public ReloadRedefiner(ReloadVirtualMachine vm, boolean forceReload) {
		this.vm = vm;
		this.forceReload = forceReload;
		this.redefineResult = RedefinitionResult.ok();
	}

	@Override
	public void changed(IRedefinable redefinable, IRedefinable replacement) {
		if (redefinable instanceof FileRedefinable) {
			redefineResult = redefineResult.merge(collectFile(
					(FileRedefinable) redefinable,
					(FileRedefinable) replacement));
		} else if (!forceReload && redefinable instanceof FunctionRedefinable) {
			redefineResult = redefineResult.merge(collectFunction(
					(FunctionRedefinable) redefinable,
					(FunctionRedefinable) replacement));
		}
	}

	@Override
	public void added(IRedefinable added) {
		cannotAddOrRemove(added);
	}

	@Override
	public void deleted(IRedefinable deleted) {
		cannotAddOrRemove(deleted);
	}

	private void cannotAddOrRemove(IRedefinable redefinable) {
		if (redefinable instanceof FileRedefinable
				|| (!forceReload && redefinable instanceof FunctionRedefinable)) {
			redefineResult = redefineResult.merge(RedefinitionResult
					.fail("Cannot add or delete files and functions"));
		}
	}

	private RedefinitionResult collectFunction(FunctionRedefinable redefinable,
			FunctionRedefinable replacement) {
		if (Util.equals(replacement.getFunctionSource(),
				redefinable.getFunctionSource())) {
			return RedefinitionResult.ok();
		}
		RedefinitionResult result = redefinable.canRedefine(replacement);
		if (!RedefinitionResult.isOk(result)) {
			return result;
		}

		functionUpdates.add(replacement);
		ReloadThreadReference mainThread = vm.mainThread();
		if (mainThread.isSuspended()) {
			for (Object frameObj : mainThread.frames()) {
				ReloadStackFrame frame = (ReloadStackFrame) frameObj;
				if (frame != null) {
					if (matchesFrame(redefinable, frame)) {
						int stackDepth = frame.getStackDepth();
						// As usual: reverse!
						dropToFrame = Math.max(dropToFrame,
								mainThread.frameCount() - stackDepth - 1);
					}
				}
			}

		}
		return RedefinitionResult.ok();
	}

	private boolean matchesFrame(FunctionRedefinable redefinable,
			ReloadStackFrame frame) {
		SimpleLocation location = (SimpleLocation) frame.location();
		SimpleScriptReference script = (SimpleScriptReference) location
				.scriptReference();
		IFile file = script.getFile();
		int line = location.lineNumber();
		FileRedefinable fileRedefinable = redefinable
				.getParent(FileRedefinable.class);
		if (fileRedefinable != null) {
			return Util.equals(redefinable.getFunctionName(),
					location.functionName())
					&& redefinable.isLineInSourceRange(line)
					&& fileRedefinable.getFile().equals(file);
		}
		return false;
	}

	private RedefinitionResult collectFile(FileRedefinable redefinable,
			FileRedefinable replacement) {
		RedefinitionResult result = RedefinitionResult.ok();
		boolean needsRedefine = false;
		// Reference equality, since we made a shallow copy of the project
		// redefinable!
		if (redefinable != replacement) {
			fileUpdates.add(redefinable.getFile());
			needsRedefine = true;
		}
		if (redefinable instanceof HTMLRedefinable
				&& replacement instanceof HTMLRedefinable) {
			if (!((HTMLRedefinable) redefinable)
					.areHtmlRangesEqual((HTMLRedefinable) replacement)) {
				needsRedefine = true;
				result = new RedefinitionResult(
						RedefinitionResult.SHOULD_RELOAD
								| RedefinitionResult.REDEFINE_OK,
						"HTML has changed, must reload");
			}
		}
		if (needsRedefine && !vm.canRedefine(redefinable)) {
			return RedefinitionResult
					.unrecoverable("Hot code replace and file reload not enabled");
		}
		return result;
	}

	@Override
	public void commit(boolean reloadHint) throws RedefineException {
		boolean isOk = RedefinitionResult.isOk(redefineResult)
				|| (reloadHint && !redefineResult
						.isFlagSet(RedefinitionResult.CANNOT_RELOAD));
		if (!isOk) {
			throw new RedefineException(redefineResult);
		}

		boolean doReload = forceReload || reloadHint
				|| redefineResult.isFlagSet(RedefinitionResult.SHOULD_RELOAD);

		if (doReload) {
			vm.reload();
		} else {
			for (IFile fileUpdate : fileUpdates) {
				vm.update(fileUpdate);
			}

			if (!fileUpdates.isEmpty()) {
				vm.refreshBreakpoints();
			}
			for (FunctionRedefinable functionUpdate : functionUpdates) {
				vm.updateFunction(functionUpdate.key(),
						functionUpdate.getFunctionSource());
			}

			if (vm.mainThread().isSuspended() && dropToFrame >= 0) {
				try {
					vm.dropToFrame(dropToFrame);
				} catch (DebugException e) {
					throw RedefineException.wrap(e);
				}
			}
		}
		if (CoreMoSyncPlugin.getDefault().isDebugging()) {
			CoreMoSyncPlugin.trace(
					"Committed redefinables. Files: {0}. Functions: {1}",
					fileUpdates, functionUpdates);
		}
	}

}
