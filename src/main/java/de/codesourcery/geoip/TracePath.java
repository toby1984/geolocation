package de.codesourcery.geoip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.lang.StringUtils;

public class TracePath {

	public static final String TRACEPATH = "/usr/bin/tracepath";

	public static final String TRACEROUTE = "/usr/bin/traceroute.db";

	public static void main(String[] args) throws Exception {

		int hop = 1;
		for ( String ip : trace("www.slashdot.org" ) ) {
			System.out.println( ">>>> "+hop+": "+ip);
			hop++;
		}
	}

	public static boolean isPathTracingAvailable() {
		return isTraceRouteAvailable() || isTracePathAvailable();
	}

	private static boolean isTracePathAvailable() {
		final File executable = new File(TRACEPATH);
		return executable.exists() && executable.canExecute();
	}

	private static boolean isTraceRouteAvailable() {
		final File executable = new File(TRACEROUTE);
		return executable.exists() && executable.canExecute();
	}

	protected interface IPathTracer {
		public List<String> trace(String address) throws ExecuteException, IOException, InterruptedException;
	}
	
// IPathTracer
	
	protected static IPathTracer getPathTracer() 
	{
		// prefer traceroute since it's faster
		if ( isTraceRouteAvailable() ) 
		{
			return new TraceRouteTracer();
		} 
		if ( isTracePathAvailable() ) {
			return new TracePathTracer();
		}
		throw new UnsupportedOperationException("No path tracing available."); 
	}
	
	public static List<String> trace(String address) throws ExecuteException, IOException, InterruptedException {
		IPathTracer tracer = getPathTracer();
		System.out.print("Tracing path to "+address+" (using "+tracer+") ...");
		return tracer.trace( address );
	}
	
	protected static abstract class AbstractPathTracer implements IPathTracer 
	{
		protected abstract CommandLine getCommandLine(String address);
		
		public List<String> trace(String address) throws ExecuteException, IOException, InterruptedException 
		{
			final CommandLine cmdLine = getCommandLine( address  );

			final HashMap<String,Object> map = new HashMap<>();
			map.put("address", address );
			cmdLine.setSubstitutionMap(map);

			final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

			final ExecuteWatchdog watchdog = new ExecuteWatchdog(60*1000);
			Executor executor = new DefaultExecutor();

			final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
			final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

			final OutputStream wrapper = new OutputStream() {

				@Override
				public void write(int b) throws IOException {
					stdOut.write( b );
					System.out.write( b );
					System.out.flush();
				}

				@Override
				public void close() throws IOException 
				{
					try {
						super.close();
					} finally {
						stdOut.close();
					}
				}
			};

			final PumpStreamHandler streamHandler= new PumpStreamHandler(wrapper,stdErr);
			executor.setStreamHandler( streamHandler );
			executor.setExitValue(1);
			executor.setWatchdog(watchdog);

			executor.execute(cmdLine, resultHandler);

			// some time later the result handler callback was invoked so we
			// can safely request the exit value
			resultHandler.waitFor();
			System.out.println("Finished");
			if ( resultHandler.getExitValue() != 0 ) 
			{
				System.err.println( new String( stdErr.toByteArray() ) );
				throw new ExecuteException("Execution failed",resultHandler.getExitValue());
			}

			/*
	 1:  192.168.2.23                                          0.070ms pmtu 1500
 1:  192.168.2.1                                           0.433ms 
 1:  192.168.2.1                                           0.529ms 
 2:  192.168.2.1                                           0.380ms pmtu 1492
 2:  213.191.64.208                                       82.772ms 
 3:  62.53.10.232                                         47.866ms asymm  4 
 4:  62.53.8.36                                           50.041ms 
 5:  62.53.8.41                                           47.878ms asymm  6 
 6:  84.16.7.233                                          55.952ms asymm  7 
 7:  94.142.120.238                                       65.963ms 	 
			 */
			final List<String> lines = extractLines(stdOut);
			final List<String> result = new ArrayList<>();
			String previousHop = null;
			for ( String line : lines ) 
			{
				line = line.trim();
				String squashWhitespace = squashWhitespace( line );
				System.out.println("LINE  : "+line);
				System.out.println("SQUASH: "+squashWhitespace);
				System.out.println("-----");
				final String[] parts = squashWhitespace.split(" ");
				if ( parts.length < 2 || StringUtils.isBlank( parts[1] ) ) {
					continue;
				}
				String ip = parts[1].trim();
				if ( isValidIP( ip ) && ! ip.equals( previousHop ) ) { 
					result.add( ip );
					previousHop = ip;
				}
			}
			
			final String endpoint = getIPAddress( address );
			if ( endpoint != null ) 
			{
				System.out.println( "Destination "+address+" has IP address "+endpoint);
				if ( result.isEmpty() ) {
					result.add( endpoint );
				} 
				else if ( ! endpoint.equals( result.get( result.size() - 1 ) ) ) 
				{
					result.add( endpoint );
				}
			}
			return result;
		}

		protected List<String> extractLines(final ByteArrayOutputStream stdOut) 
		{
			final List<String> result = new ArrayList<>( Arrays.asList(  new String( stdOut.toByteArray() ).split("\n") ) );
			return filterLines( result );
		}
		
		protected List<String> filterLines(List<String> lines) 
		{
			for (Iterator<String> it = lines.iterator(); it.hasNext();) 
			{
				String line = it.next().trim();
				if ( StringUtils.isBlank( line ) || ! Character.isDigit( line.charAt(0) ) ) {
					System.out.println("DISCARDED: "+line);
					it.remove();
				}
			}
			return lines;
		}
	}

	protected static String getIPAddress(String name) {
		try {
			InetAddress inet = Inet4Address.getByName( name );
			if ( inet != null ) {
				return inet.getHostAddress();
			}
		} catch(Exception e) {
			
		}
		try {
			InetAddress inet = Inet6Address.getByName( name );
			if ( inet != null ) {
				return inet.getHostAddress();
			}
		} catch(Exception e) {
		}
		return null;
	}
	protected static class TracePathTracer extends AbstractPathTracer {

		@Override
		protected CommandLine getCommandLine(String address) {
			final CommandLine cmdLine = new CommandLine( TRACEPATH );
			cmdLine.addArgument("-n");
			cmdLine.addArgument("${address}");
			return cmdLine;
		}
		
		@Override
		public String toString() {
			return TRACEPATH;
		}
	}
	
	protected static class TraceRouteTracer extends AbstractPathTracer {

		@Override
		protected CommandLine getCommandLine(String address) {
			final CommandLine cmdLine = new CommandLine( TRACEROUTE );
			cmdLine.addArgument("-n");
			cmdLine.addArgument("-q");
			cmdLine.addArgument("1");
			cmdLine.addArgument("-w");
			cmdLine.addArgument("1");			
			cmdLine.addArgument("${address}");
			return cmdLine;
		}
		
		@Override
		public String toString() {
			return TRACEROUTE;
		}		
	}
	
	public static boolean isValidIP(String s) {
		if ( StringUtils.isBlank( s ) ) {
			return false;
		}
		try {
			InetAddress ip = Inet4Address.getByName( s );
			if ( ip != null ) {
				return true;
			}
		} catch(Exception e) {
		}
		try {
			InetAddress ip = Inet6Address.getByName( s );
			if ( ip != null ) {
				return true;
			}
		} catch(Exception e) {
		}
		return false;
	}

	private static String squashWhitespace(String input) {

		StringBuilder out = new StringBuilder();
		boolean previousCharWasWhitespace = false;
		for ( int i = 0 ; i < input.length() ; i++ ) 
		{
			char c = input.charAt( i );
			if ( ! Character.isWhitespace( c ) ) {
				out.append( c );
				previousCharWasWhitespace  = false;
			}
			else 
			{
				if ( ! previousCharWasWhitespace) {
					out.append( c );
					previousCharWasWhitespace = true;
				}
			}
		}
		return out.toString();
	}
}
