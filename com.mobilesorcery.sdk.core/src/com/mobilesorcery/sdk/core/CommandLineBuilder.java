package com.mobilesorcery.sdk.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A fluent class for building command lines.
 *
 * @author fmattias
 */
public class CommandLineBuilder
{
	/**
	 * Path to the executable file starting the command line.
	 */
	private final String m_executable;

	/**
	 * The set of flags, if a flag does not have a parameter its
	 * key is null.
	 */
	private final HashMap<String, String> m_params = new LinkedHashMap<String, String>();

	/**
	 * The trailing parameters of the command line, e.g. files to the cp command.
	 */
	private final ArrayList<String> m_endParams = new ArrayList<String>();

	/**
	 * The last flag that was added.
	 */
	private String m_lastFlag = null;

	private final HashSet<String> hiddenFlags = new HashSet<String>();

	/**
	 * Creates a class that builds a command line.
	 *
	 * @param executable Path to the executable.
	 */
	public CommandLineBuilder(String executable)
	{
		this(executable, false);
	}

	public CommandLineBuilder(String executable, boolean lenient)
	{
		if(!lenient && ! (new File( executable )).exists( ) )
		{
			throw new IllegalArgumentException( "Executable does not exist." );
		}

		m_executable = executable;
	}

	/**
	 * Adds a flag to the command line.
	 *
	 * @param flag A flag to the command line.
	 * @return itself.
	 */
	public CommandLineBuilder flag(String flag)
	{
		if( flag == null || flag.length( ) == 0 )
		{
			throw new IllegalArgumentException( "Flag is null or empty." );
		}

		m_lastFlag = flag;
		m_params.put( flag, null );

		return this;
	}

	public CommandLineBuilder flag(String flag, boolean hidden) {
		hiddenFlags.add(flag);
		return flag(flag);
	}

	/**
	 * Adds a parameter to the command line, either a flag or one of the
	 * trailing parameters.
	 *
	 * @param parameter either a flag parameter or an end parameter.
	 * @return itself.
	 */
	public CommandLineBuilder with(String parameter)
	{
		if( parameter == null )
		{
			return this;
		}

		if( m_lastFlag != null )
		{
			m_params.put( m_lastFlag, parameter );
		}
		else
		{
			throw new UnsupportedOperationException( "Trying to add parameter without flag." );
		}

		m_lastFlag = null;
		return this;
	}

	/**
	 * Takes the absolute path of the given file or
	 * directory and adds it to the command line.
	 *
	 * @param file A representation of a path in the file system.
	 * @return itself.
	 */
	public CommandLineBuilder with(File file)
	{
		with( file.getAbsolutePath( ) );
		return this;
	}

	/**
	 * Adds a trailing parameter.
	 *
	 * @return itself.
	 */
	public CommandLineBuilder param(String parameter)
	{
		if( parameter == null )
		{
			throw new IllegalArgumentException( "Cannot add trailing parameter." );
		}

		m_endParams.add( parameter );

		return this;
	}

	/**
	 * Adds a trailing parameter.
	 *
	 * @return itself.
	 */
	public CommandLineBuilder param(File param)
	{
		return param( param.getAbsolutePath( ) );
	}

	/**
	 * Returns the command line as a string array, where each flag and
	 * parameter is a separate array element. The array begins with
	 * the path to the executable. No quotation of strings is performed.
	 *
	 * @return The command line in the form of an array.
	 */
	public String[] asArray()
	{
		ArrayList<String> cmdLine = new ArrayList<String>( );
		cmdLine.add( m_executable );

		/* Add flags and their parameters */
		for( String flag : m_params.keySet( ) )
		{
			cmdLine.add( flag );

			String value = m_params.get( flag );
			if( value != null )
			{
				cmdLine.add( value );
			}
		}

		/* Add trailing arguments */
		cmdLine.addAll( m_endParams );

		return cmdLine.toArray( new String[ cmdLine.size( ) ] );
	}

	/**
	 * Converts the command line into a single string. Arguments
	 * containing spaces are automatically quoted.
	 *
	 * @return the command line as a string.
	 */
	@Override
	public String toString()
	{
		StringBuffer cmdLine = new StringBuffer( quote( m_executable ) );

		addFlags( m_params, cmdLine );
		addTrailingArgs( m_endParams, cmdLine );

		return cmdLine.toString( );
	}

	/**
	 * Converts the command line into a single string. Arguments containing
	 * spaces are quoted. Flags in the given list of hidden flags are
	 * hidden with "*HIDDEN*".
	 *
	 * @param hiddenFlags A list of flags which should be hidden from the
	 *                    command line.
	 * @return the command line as a string.
	 */
	public String toHiddenString()
	{
		/* Copy internal map and hide specified parameters. */
		Map<String, String> paramsHidden = new LinkedHashMap<String, String>(m_params);
		for(String hiddenFlag : hiddenFlags) {
			if( paramsHidden.containsKey( hiddenFlag ) ) {
				paramsHidden.put(hiddenFlag, "***HIDDEN***");
			}
		}

		// Throwaway cmdline
		StringBuffer cmdLine = new StringBuffer(quote( m_executable ));

		addFlags(paramsHidden, cmdLine);
		addTrailingArgs(m_endParams, cmdLine );

		return cmdLine.toString( );
	}

	/**
	 * Flattens a map of flag pairs elements into to a string, and quotes
	 * the parameter if it contains spaces or tabs.
	 *
	 * @param flags A map of flag pairs that will be appended in the
	 *              command line.
	 * @param cmdLine The command flag pairs will be appended here.
	 */
	private void addFlags(Map<String, String> flags, StringBuffer cmdLine)
	{
		for( String flag : flags.keySet( ) )
		{
			cmdLine.append( " " + flag );

			String value = flags.get( flag );
			if( value != null )
			{
				cmdLine.append( " " + quote( value ) );
			}
		}
	}

	/**
	 * Flattens a list of parameters into a string, and quotes a parameter
	 * if it contains spaces or tabs.
	 *
	 * @param args A list of arguments that will be appended to the command
	 *             line.
	 * @param cmdLine The command parameters will be appended here.
	 */
	private void addTrailingArgs(List<String> args, StringBuffer cmdLine)
	{
		for( String parameter : args )
		{
			cmdLine.append( " " + quote( parameter ) );
		}
	}


	/**
	 * Surrounds the input string with quotation marks
	 * if the input string contains whitespace.
	 *
	 * @param input Input string to be quoted.
	 * @return A quoted input string.
	 */
	private String quote(String input)
	{
		if( input == null )
		{
			return null;
		}

		if( input.contains( " " ) || input.contains( "\t" ) )
		{
			return "\"" + input + "\"";
		}
		else
		{
			return input;
		}
	}

}
