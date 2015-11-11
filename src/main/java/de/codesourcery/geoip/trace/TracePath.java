/**
 * Copyright 2015 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.geoip.trace;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.exec.ExecuteException;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class TracePath 
{
    /**
     * Filesystem path to the Linux 'tracepath' utility on <b>Ubuntu</b> systems.
     */
    public static final String TRACEPATH = "/usr/bin/tracepath";

    /**
     * Filesystem path to the Linux 'traceroute' utility on <b>Ubuntu</b> systems.
     */	
    public static final String TRACEROUTE = "/usr/bin/traceroute.db";

    public static void main(String[] args) throws Exception {

        final String sIp = "192.168.2.1";
        System.out.println( sIp+" is a private IP: "+isUnroutableAddress( sIp ) );
        
        int hop = 1;
        for ( String ip : trace("www.slashdot.org", line -> System.out.println(line) ) ) {
            System.out.println( ">>>> "+hop+": "+ip+" (private: "+isUnroutableAddress( ip )+")" );
            hop++;
        }
    }

    /**
     * Check whether path-tracing is available on this system.
     * 
     * @return
     */
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
        public List<String> trace(String address, Consumer<String> stdOutConsumer) throws  IOException, InterruptedException;
    }

    // IPathTracer

    protected static IPathTracer getPathTracer() 
    {
        // prefer traceroute since it's faster
        if ( isTraceRouteAvailable() ) 
        {
            System.out.println("'traceroute' tool available");
            return new TraceRouteTracer();
        } 
        if ( isTracePathAvailable() ) {
            System.out.println("'tracepath' available.");
            return new TracePathTracer();
        }
        throw new UnsupportedOperationException("No path tracing available."); 
    }

    /**
     * Tries to trace all intermediate hops from the local machine to a specific IP address/host name.
     * 
     * <p>Intermediate hops whose IP address cannot be determined will be removed from the result (so the result
     * contains only valid IP addresses).</p>
     * 
     * @param address
     * @return List of IP addresses
     * @throws ExecuteException
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<String> trace(String address,Consumer<String> stdOutConsumer) throws  IOException, InterruptedException {
        IPathTracer tracer = getPathTracer();
        System.out.print("Tracing path to "+address+" (using "+tracer+") ...");
        return tracer.trace( address, stdOutConsumer );
    }

    protected static final class StdOutParser implements Consumer<String> {

        private final List<String> result = new ArrayList<>();
        private String previousHop = null;
        private final String address;
        private final Consumer<String> stdOutConsumer;

        public StdOutParser(String address,Consumer<String> stdOutConsumer) {
            this.address = address;
            this.stdOutConsumer = stdOutConsumer;
        }

        @Override
        public void accept(String line)
        {
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

            final String squashWhitespace = squashWhitespace( line.trim() );
            final String[] parts = squashWhitespace.split(" ");
            if ( parts.length >= 2 && StringUtils.isNotBlank( parts[1] ) ) 
            {
                String ip = parts[1].trim();
                if ( isValidAddress( ip ) && ! ip.equals( previousHop ) ) 
                { 
                    result.add( ip );
                    previousHop = ip;
                    stdOutConsumer.accept( ip );
                }
            }
        }

        public List<String> getResult() 
        {
            final String endpoint = getIPAddress( address );
            if ( endpoint != null ) 
            {
                System.out.println( "Destination "+address+" has IP address "+endpoint);
                if ( result.isEmpty() ||  ! endpoint.equals( result.get( result.size() - 1 ) ) ) 
                {
                    result.add( endpoint );
                } 
            }
            return result;            
        }
    }

    protected static final class ParsingOutputStream extends OutputStream 
    {
        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private final String charset;
        private final Consumer<String> lineConsumer;

        public ParsingOutputStream(Consumer<String> lineConsumer,String charset) {
            this.lineConsumer = lineConsumer;
            this.charset = charset;
        }

        @Override
        public void write(int b) throws IOException 
        {
            if ( b == '\n' ) {
                parseBuffer();
            } else {
                buffer.write( b );
            }
        }

        private void parseBuffer() throws UnsupportedEncodingException 
        {
            try {
                lineConsumer.accept( new String(buffer.toByteArray(),charset ) );
            } finally {
                buffer = new ByteArrayOutputStream();
            }
        }

        @Override
        public void close() throws IOException 
        {
            try {
                super.close();
            } finally {
                if ( buffer.size() > 0 ) {
                    parseBuffer();
                }
            }
        }
    }

    protected static abstract class AbstractPathTracer implements IPathTracer 
    {
        protected abstract String[] getCommandLine(String address);

        public List<String> trace(String address, Consumer<String> stdOutConsumer) throws  IOException, InterruptedException 
        {
            final Process process = Runtime.getRuntime().exec( getCommandLine( address ) );
            
            final StdOutParser parser = new StdOutParser( address , stdOutConsumer );
            final ParsingOutputStream stdOut = new ParsingOutputStream( parser , "UTF-8" );
            final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
            
            final IOThread t1 = new IOThread("std_err",process.getErrorStream() , stdErr );
            final IOThread t2 = new IOThread("std_out",process.getInputStream() , stdOut );
            t1.start();
            t2.start();
            final int exitCode = process.waitFor();
            if ( exitCode != 0 ) {
                System.err.println( new String( stdErr.toByteArray() ) );
                throw new RuntimeException("Tracing path failed with exit code "+exitCode);
            }
            if ( t1.error != null || t2.error != null ) {
                throw new RuntimeException("read() from sub-process failed");
            }
            return parser.getResult();
        }
        
        protected static final class IOThread extends Thread {
            
            private final InputStream in;
            private final OutputStream out;
            
            public IOException error;
            public boolean eofReached;
            public volatile boolean terminate = false;
            
            public IOThread(String name,InputStream in,OutputStream out) {
                setDaemon(true);
                setName( name );
                this.in= in;
                this.out = out;
            }
            
            @Override
            public void run() 
            {
                try 
                {                
                    while( ! terminate ) 
                    {
                            int value = in.read();
                            if ( value == -1 ) {
                                eofReached = true;
                                break;
                            }
                            out.write( value );
                    }
                } 
                catch (IOException e) 
                {
                    error = e;
                    terminate = true;
                } 
                finally 
                {
                    if ( eofReached ) {
                        System.out.println( "Thread "+this+" reached EOF");
                    } 
                    if ( error != null ) {
                        System.out.println( "Thread "+this+" caught "+error.getMessage());
                    }
                    closeQuietly(in);
                    closeQuietly(out);
                }
            }            
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
        protected String[] getCommandLine(String address) {
            return new String[] { TRACEPATH , "-n", address };
        }

        @Override
        public String toString() {
            return TRACEPATH;
        }
    }

    protected static class TraceRouteTracer extends AbstractPathTracer {

        @Override
        protected String[] getCommandLine(String address) {
            return new String[] {  TRACEROUTE,
             "-n",
             "-q",
             "1",
             "-w",
             "1",
             address };
        }

        @Override
        public String toString() {
            return TRACEROUTE;
        }		
    }

    /**
     * Returns whether a string contains a valid IP address or host name.
     * 
     * @param s
     * @return
     */
    public static boolean isValidAddress(String s) 
    {
        return parseIP(s).isPresent();
    }
    
    private static Optional<InetAddress> parseIP(String s) 
    {
        if ( s != null && ! s.contains("." ) ) {
            return Optional.empty();
        }
        try {
            InetAddress ip = Inet4Address.getByName( s );
            if ( ip != null ) {
                return Optional.of(ip);
            }
        } catch(Exception e) {
        }
        try {
            InetAddress ip = Inet6Address.getByName( s );
            if ( ip != null ) {
                return Optional.of(ip);
            }
        } catch(Exception e) {
        }
        return Optional.empty();
    }
    
    public static boolean isUnroutableAddress(String s) 
    {
        final Optional<InetAddress> ip = parseIP( s );
        if ( ip.isPresent() ) 
        {
            final String[] subnets = { "10.0.0.0/8","172.16.0.0/12","192.168.0.0/16" };
            for ( String subnet : subnets ) {
                if ( isInSubnet( ip.get() , subnet ) ) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static boolean isInSubnet(InetAddress ip, String subnet) 
    {
        final String[] parts = subnet.split("/");
        final byte[] netPart = parseIP( parts[0] ).get().getAddress();
        final int maskBits = Integer.parseInt( parts[1] );
        final byte[] mask = new byte[ netPart.length ];
        
        int index = 0;
        int bitsLeft = maskBits;
        int maskBit = 1<<7;
        while ( bitsLeft > 0 ) 
        {
            mask[index] |= maskBit;
            maskBit = maskBit >> 1;
            if ( maskBit == 0 ) {
                maskBit = 1<<7;
                index++;
            }
            bitsLeft--;
        }
        
        byte[] adr1 = ip.getAddress();
        byte[] adr2 = netPart;
        if ( adr1.length != adr2.length ) {
            return false;
        }
        for ( int i = 0 ; i < mask.length ; i++ ) {
            adr1[i] &= mask[i];
            adr2[i] &= mask[i];
            if ( adr1[i] != adr2[i] ) {
                return false;
            }
        }
        return true;
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
    
    protected static void closeQuietly(Closeable c) {
        try {
            if ( c != null ) {
                c.close();
            }
        } catch(Exception e) {
            // intentionally ignored
        }
    }
}
