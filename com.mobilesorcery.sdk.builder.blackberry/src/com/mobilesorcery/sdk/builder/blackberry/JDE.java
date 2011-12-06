package com.mobilesorcery.sdk.builder.blackberry;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.mobilesorcery.sdk.builder.java.KeystoreCertificateInfo;
import com.mobilesorcery.sdk.core.CollectingLineHandler;
import com.mobilesorcery.sdk.core.CommandLineBuilder;
import com.mobilesorcery.sdk.core.CommandLineExecutor;
import com.mobilesorcery.sdk.core.MoSyncBuilder;
import com.mobilesorcery.sdk.core.MoSyncTool;
import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.core.Version;

public class JDE {

	private final static Pattern JDE_RAPC_VERSION_PATTERN = Pattern.compile(".*Build:\\s*(.*)");
	private static final Version VER_430 = new Version("4.3.0");

	public final static int TYPE_DEV_TOOLS = 0;
	public final static int TYPE_SIMULATOR = 1;

	private IPath root;
	private Version version;

	protected JDE(IPath root, Version version) {
		this.root = root;
		this.version = version;
	}

	public IPath getLocation() {
		return root;
	}

	public Version getVersion() {
		return version;
	}

	public void apply(JDE workingCopy) {
		this.root = workingCopy.root;
		this.version = workingCopy.version;
	}

	public int getType() {
		return TYPE_DEV_TOOLS;
	}

	/**
	 * Uses heuristics to guess the version of a JDE at a certain location
	 * @return <code>null</code> if for some reason the version could not
	 * be guessed
	 */
	public static Version getVersion(int toolType, IPath root) {
		JDE throwaway = create(toolType, root, null);
		try {
			return throwaway.guessVersion();
		} catch (IOException e) {
			return null;
		}
	}

	public static JDE create(int toolType, IPath root, Version version) {
		return toolType == TYPE_DEV_TOOLS ? new JDE(root, version) :
			new Simulator(root, version);
	}

	public Version guessVersion() throws IOException {
		CollectingLineHandler lines = new CollectingLineHandler();
		CommandLineExecutor executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
		executor.setLineHandlers(lines, lines);
		runRapc(executor, new String[] { "-version" });

		for (String line : lines.getLines()) {
			Version version = guessVersion(line);
			if (version != null) {
				return version;
			}
		}

		return null;
	}

	private void runRapc(CommandLineExecutor exe, String[] args) throws IOException {
		String[] baseCommand = new String[] { "java", "-classpath",
				getRapc().toFile().getAbsolutePath(),
				"net.rim.tools.compiler.Compiler" };
		ArrayList<String> fullCommand = new ArrayList<String>();
		fullCommand.addAll(Arrays.asList(baseCommand));
		fullCommand.addAll(Arrays.asList(args));
		exe.runCommandLine(fullCommand.toArray(new String[0]));
	}

	private Version guessVersion(String version) {
		Matcher versionMatcher = JDE_RAPC_VERSION_PATTERN.matcher(version);
		if (versionMatcher.matches()) {
			return new Version(versionMatcher.group(1)).truncate(Version.MICRO);
		}
		return null;
	}

	private IPath getRapc() {
		return root.append("bin/rapc.jar");
	}

	private IPath getPreverifier() {
		return root.append("bin/preverify" + MoSyncTool.getBinExtension());
	}

	private IPath getSignTool() {
		// Win only
		return root.append("bin/SignatureTool.jar");
	}

	/**
	 * Preverifies a jar and repackages it into a new jar -- will create a number of temporary files
	 * @param input
	 * @param output
	 * @throws IOException
	 */
	public void preverifyJAR(File jar, File output) throws IOException {
		File tmpJar = new File(jar.getParentFile(), "tmp-jar");
		Util.deleteFiles(tmpJar, null, Util.INFINITE_DEPTH, new NullProgressMonitor());
		File tmpPrev = new File(jar.getParentFile(), "tmp-prev");

		try {
			Util.unjar(jar, tmpJar);
			// Copy all non-class files!
			Util.copy(new NullProgressMonitor(), tmpJar, tmpPrev, new FileFilter() {
				@Override
				public boolean accept(File file) {
					return !"class".equalsIgnoreCase(Util.getExtension(file));
				}
			});
			String[] preverifyCommandLine = new String[] {
					getPreverifier().toFile().getAbsolutePath(),
					"-classpath",
					getBootClasspath().toFile().getAbsolutePath(),
					"-d",
					tmpPrev.getAbsolutePath(),
					tmpJar.getAbsolutePath()
			};
			CommandLineExecutor executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
			executor.runCommandLine(preverifyCommandLine);
			Util.jar(tmpPrev, output);
		} finally {
			Util.deleteFiles(tmpJar, null, Util.INFINITE_DEPTH, new NullProgressMonitor());
			Util.deleteFiles(tmpPrev, null, Util.INFINITE_DEPTH, new NullProgressMonitor());
		}
	}

	public void convertJARToCOD(File jar, File jad, File cod) throws IOException {
		String codNameParam = getVersion().isOlder(VER_430) ? "codename" : "codname";
		String[] rapcCommandLine = new String[] {
				"import=" + getBootClasspath().toOSString(),
				codNameParam + "=" + Util.getNameWithoutExtension(cod),
				"-midlet",
				"-quiet",
				"jad=" + jad.getAbsolutePath(),
				jar.getAbsolutePath()
		};
		CommandLineExecutor executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
		executor.setExecutionDirectory(jar.getParent());
		runRapc(executor, rapcCommandLine);
	}

	public void sign(File finalOutput, KeystoreCertificateInfo certInfo) throws IOException {
		if (VER_430.isNewer(getVersion())) {
			throw new IOException("Signing is only supported for BlackBerry JDE versions above 4.3.0 (due to limitations in the JDE -- please sign manually)");
		}
		String password = certInfo.getKeyPassword();
		if (Util.isEmpty(password)) {
			throw new IOException("Missing password for signing");
		}

		// BLURGH!
		CommandLineBuilder commandLine = new CommandLineBuilder("java", true).flag("-jar").with(getSignTool().toFile().getAbsolutePath()).
			flag("-p", true).with(password).flag("-C").flag("-a").with(finalOutput);

		CommandLineExecutor executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
		executor.setExecutionDirectory(finalOutput.getParent());
		executor.runCommandLine(commandLine.asArray(), commandLine.toHiddenString());
	}

	private IPath getBootClasspath() {
		return root.append("lib/net_rim_api.jar");
	}


	@Override
	public String toString() {
		String versionStr = (version == null ? "?" : version
				.asCanonicalString());
		return getClass().getSimpleName() + " version " + versionStr;
	}


}
