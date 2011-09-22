package com.mobilesorcery.sdk.builder.blackberry;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

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
	private IPath root;
	private Version version;

	public static void main(String[] args) {
		JDE jde = new JDE();
		System.out.println(jde.guessVersion("RAPC Version: 2.2 Build: 6.0.0.141"));
	}

	public JDE(IPath root, Version version) {
		this.root = root;
		this.version = version;
	}

	private JDE() {
		// TODO Auto-generated constructor stub
	}

	public IPath getLocation() {
		return root;
	}


	public Version getVersion() {
		return version;
	}

	/**
	 * Uses heuristics to guess the version of a JDE at a certain location
	 * @return <code>null</code> if for some reason the version could not
	 * be guessed
	 */
	public static Version getVersion(IPath root) {
		JDE throwaway = new JDE(root, null);
		try {
			return throwaway.guessVersion();
		} catch (IOException e) {
			return null;
		}
	}

	public Version guessVersion() throws IOException {
		File rapc = getRapc().toFile();
		CollectingLineHandler lines = new CollectingLineHandler();
		CommandLineExecutor executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
		executor.setLineHandlers(lines, lines);
		executor.runCommandLine(new String[] {
			rapc.getAbsolutePath(),
			"-version"
		});

		for (String line : lines.getLines()) {
			Version version = guessVersion(line);
			if (version != null) {
				return version;
			}
		}

		return null;
	}

	private Version guessVersion(String version) {
		Matcher versionMatcher = JDE_RAPC_VERSION_PATTERN.matcher(version);
		if (versionMatcher.matches()) {
			return new Version(versionMatcher.group(1)).truncate(Version.MICRO);
		}
		return null;
	}

	private IPath getRapc() {
		return root.append("bin/rapc" + MoSyncTool.getBinExtension());
	}

	private IPath getPreverifier() {
		return root.append("bin/preverify" + MoSyncTool.getBinExtension());
	}

	private IPath getSignTool() {
		// Win only
		return root.append("bin/SignatureTool.jar");
	}

	public void apply(JDE workingCopy) {
		this.root = workingCopy.root;
		this.version = workingCopy.version;
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
				getRapc().toFile().getAbsolutePath(),
				"import=" + getBootClasspath().toOSString(),
				codNameParam + "=" + Util.getNameWithoutExtension(cod),
				"-midlet",
				"-quiet",
				"jad=" + jad.getAbsolutePath(),
				jar.getAbsolutePath()
		};
		CommandLineExecutor executor = new CommandLineExecutor(MoSyncBuilder.CONSOLE_ID);
		executor.setExecutionDirectory(jar.getParent());
		executor.runCommandLine(rapcCommandLine);
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
		CommandLineBuilder commandLine = new CommandLineBuilder("javaw", true).flag("-jar").with(getSignTool().toFile().getAbsolutePath()).
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
		return "JDE version " + version + " (" + root + ")";
	}


}
