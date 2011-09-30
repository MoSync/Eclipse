package com.mobilesorcery.sdk.core.build;

import org.eclipse.ui.IMemento;

public class BundleBuildStep extends CommandLineBuildStep {

	public final static String ID = "bundle";

	public static class Factory extends CommandLineBuildStep.Factory {

		private String inFile;
		private String outFile;

		@Override
		public String getId() {
			return ID;
		}

		public void setInFile(String inFile) {
			this.inFile = inFile;
		}

		public String getInFile() {
			return inFile;
		}

		public void setOutFile(String outFile) {
			this.outFile = outFile;
		}

		public String getOutFile() {
			return outFile;
		}

		@Override
		public Script getScript() {
			Script script = new Script(new String[][] {{
				"%mosync-bin%/Bundle", "-in", inFile, "-out", outFile
			}});
			return script;
		}

		@Override
		public boolean shouldRunPerFile() {
			return false;
		}

		@Override
		public void load(IMemento memento) {
			IMemento command = memento.getChild("bundle");
			if (command != null) {
				this.inFile = command.getString("inFile");
				this.outFile = command.getString("outFile");
				this.name = command.getString("name");
				Boolean failOnError = command.getBoolean("foe");
				this.failOnError = failOnError == null ? false : failOnError;
			}
		}

		@Override
		public void store(IMemento memento) {
			IMemento command = memento.createChild("bundle");
			command.putString("inFile", inFile);
			command.putString("outFile", outFile);
			command.putBoolean("foe", failOnError);
			command.putString("name", name);
		}

		@Override
		public boolean requiresPrivilegedAccess() {
			return false;
		}

	}

	public BundleBuildStep(Factory prototype) {
		super(prototype);
	}

}
