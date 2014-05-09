/**
 * Copyright 2012 Tobias Gierke <tobias.gierke@code-sourcery.de>
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
package de.codesourcery.geoip;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.json.JSONObject;
import org.json.JSONTokener;

import com.jhlabs.map.proj.MillerCylindrical1Projection;

public class Main {

//	private static final Coordinate HAMBURG = Coordinate.coordinateInDegrees( 53.5538148  , 9.9915752 , "Hamburg" , Color.RED );
	
	private static final Coordinate ZERO = Coordinate.coordinateInDegrees( 0 , 0 , "ZERO" , Color.GREEN );	
	
	private static final Coordinate PARIS = Coordinate.coordinateInDegrees( 48.85341 , 2.3488 , "Paris" , Color.YELLOW );
	
	private static final Coordinate NEW_YORK = Coordinate.coordinateInDegrees( 40.7142700 , -74.0059700 , "Paris" , Color.YELLOW );
	
	private static final Coordinate MELBOURNE = Coordinate.coordinateInDegrees( -37.814251 , 144.963169 , "Melbourne" , Color.YELLOW );	
	
	private static final Coordinate WELLINGTON = Coordinate.coordinateInDegrees( -41.2864800 , 174.7762170 , "Wellington/NZ" , Color.YELLOW );	
	
	private static final Coordinate DUBLIN = Coordinate.coordinateInDegrees( 53.3441040 , -6.2674937 , "Dublin" , Color.YELLOW );	
	
	public static final double SCALE_X = 158.5;
	public static final double SCALE_Y = 213.0;
	
	public static void main(String[] args) throws Exception 
	{
		final BufferedImage image; 		
		try ( InputStream io = Main.class.getResourceAsStream("/world_miller_cylindrical.jpg" ) ) 
		{
			if ( io == null ) {
				throw new IOException("failed to open world map image");
			}
			image = ImageIO.read( io );
		}
		
		final JFrame frame = new JFrame("dummy");		
		final MapCanvas canvas = new MapCanvas(image);

		canvas.setScale( SCALE_X , SCALE_Y );
		canvas.setPreferredSize( new Dimension(640,480 ) );
		canvas.addCoordinates( PARIS , ZERO , NEW_YORK , DUBLIN , MELBOURNE , WELLINGTON );
		
		JPanel panel = new JPanel();
		panel.setLayout( new FlowLayout() );
		
		panel.add( new JLabel("Scale-X"));
		final JTextField scaleX = new JTextField( Double.toString(SCALE_X) );
		scaleX.setColumns( 5 );
		
		final JTextField scaleY = new JTextField( Double.toString(SCALE_Y) );
		scaleY.setColumns( 5 );
		
		final ActionListener listener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateScale(scaleX, scaleY, canvas);
			}
		};
		scaleX.addActionListener( listener );
		scaleY.addActionListener( listener );
		
		panel.add( new JLabel("Scale-X"));
		panel.add( scaleX );

		panel.add( new JLabel("Scale-Y"));
		panel.add( scaleY );
		
		final JTextField ipAddress = new JTextField( "213.191.64.208" );
		ipAddress.setColumns( 20 );
		
		final ActionListener ipListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				String ip = ipAddress.getText(); 
				if ( ip != null && ip.trim().length() > 0 ) 
				{
					ip = ip.trim();
					boolean isValid = false;
					try {
						isValid = Inet4Address.getByName( ip ) != null;
					} catch(Exception ex) {
					}
					if ( ! isValid ) {
						try {
							isValid = Inet6Address.getByName( ip ) != null;
						} catch(Exception ex) {
						}						
					}
					if ( isValid ) 
					{
						try 
						{
							Coordinate result = resolve( ip );
							if ( result != null ) {
								canvas.removeAllCoordinates();
								canvas.addCoordinate( result );
								canvas.repaint();
							}
						} catch (IOException e1) {
							System.err.println("Failed to resolve IP "+ip);
							e1.printStackTrace();
						}
					}
				}
			}
		};	
		ipAddress.addActionListener( ipListener );
		
		panel.add( new JLabel("IP"));		
		panel.add( ipAddress );
		
		frame.getContentPane().setLayout( new BorderLayout() );
		frame.getContentPane().add( panel , BorderLayout.NORTH);
		frame.getContentPane().add( canvas , BorderLayout.CENTER );
		frame.pack();
		frame.setVisible(true);
	}
	
	private static void updateScale(JTextField scaleX, JTextField scaleY , MapCanvas canvas) 
	{
		try {
			double x = Double.parseDouble( scaleX.getText() );
			double y = Double.parseDouble( scaleY.getText() );
			canvas.setScale(x,y);
			canvas.repaint();
		} 
		catch(Exception e) {
		}
	}
	
	private static Coordinate resolve(String ipAddress) throws IOException {
		
		// freegeoip.net/{format}/{ip_or_hostname}
		
        final Map<String, String> response = sendRequest(ipAddress);
        System.out.println("GOT RESPONSE: \n"+response);
        if ( response.containsKey( "longitude" ) && response.containsKey("latitude" ) ) {
        	double latitude = Double.parseDouble( response.get("latitude" ) );
        	double longitude  = Double.parseDouble( response.get("longitude" ) );
        	
        	String text = ipAddress;
        	if ( response.containsKey("city" ) ) {
        		text += ", "+response.get("city");
        	}
        	if ( response.containsKey( "country_name" ) ) {
        		text += ", "+response.get("country_name");
        	}
        	return Coordinate.coordinateInDegrees( latitude , longitude , text, Color.RED );
        }
        return null;
	}

	private static Map<String,String> sendRequest(String ipAddress) throws MalformedURLException, IOException 
	{
    	final StringBuilder builder = new StringBuilder();
    	final URL api = new URL("http://freegeoip.net/json/"+ipAddress);
    	final URLConnection yc = api.openConnection(); 
        try ( BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream())) ) 
        {
        	String inputLine;
        	while ((inputLine = in.readLine()) != null) 
        	{
        		builder.append( inputLine );
        	}
        } 
        // {"ip":"213.191.64.208","country_code":"DE","country_name":"Germany","region_code":"","region_name":"","city":"","zipcode":"","latitude":51,"longitude":9,"metro_code":"","area_code":""}
        return parseJSON( builder.toString() );
	}
	
	private static Map<String,String> parseJSON(String input) 
	{
		Map<String,String> result = new HashMap<>();
		JSONObject obj = new JSONObject( new JSONTokener( input ) );
		for ( String key :  JSONObject.getNames( obj ) ) {
			Object value = obj.get(key);
			if ( value != null ) {
				result.put( key.toLowerCase() , value.toString() );
			}
		}
		return result;
	}
	
	protected static final class Coordinate 
	{
		public final Color color;
		public final String text;
		public final double longitudeInDeg;
		public final double latitudeInDeg;
		
		public final Point currentPoint=new Point();
		
		private Coordinate(double latitudeInDeg, double longitudeInDeg,String text,Color color) {
			this.longitudeInDeg = longitudeInDeg;
			this.latitudeInDeg = latitudeInDeg;
			this.text = text;
			this.color = color;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(latitudeInDeg);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(longitudeInDeg);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Coordinate other = (Coordinate) obj;
			if (Double.doubleToLongBits(latitudeInDeg) != Double
					.doubleToLongBits(other.latitudeInDeg))
				return false;
			if (Double.doubleToLongBits(longitudeInDeg) != Double
					.doubleToLongBits(other.longitudeInDeg))
				return false;
			return true;
		}



		public double getDistanceToPixel(int x,int y) 
		{
			double dx = x - currentPoint.x;
			double dy = y - currentPoint.y; 
			return Math.sqrt( dx*dx + dy*dy );
		}
		
		public static Coordinate coordinateInDegrees(double latitudeInDeg, double longitudeInDeg,String text,Color color) {
			return new Coordinate( latitudeInDeg , longitudeInDeg,text,color); 
		}
		
		public double longitudeInRad() {
			return Math.toRadians( longitudeInDeg );
		}
		
		public double latitudeInRad() {
			return Math.toRadians( latitudeInDeg );
		}		
	}
	
	protected static final class MapCanvas extends JPanel {
		
		private final BufferedImage image;
		private final List<Coordinate> coordinates = new ArrayList<>();
		
		private int mouseX=-1;
		private int mouseY=-1;
		
		private double scaleX=160;
		private double scaleY=225;
		
		private final MouseAdapter mouseAdapter = new MouseAdapter() 
		{
			public void mouseMoved(java.awt.event.MouseEvent e) 
			{
				mouseX = e.getX();
				mouseY = e.getY();
				
				double distance = Integer.MAX_VALUE;
				Coordinate bestMatch = null;
				for ( Coordinate c : coordinates ) 
				{
					double d = c.getDistanceToPixel( mouseX ,  mouseY );
					if ( d <= 5f ) 
					{
						if ( bestMatch == null || d < distance ) {
							bestMatch = c;
							distance = d;
						}
					}
				}

				setToolTipText( bestMatch == null ? null : bestMatch.text );
				MapCanvas.this.repaint();
			};
		};
		
		public void removeAllCoordinates() {
			this.coordinates.clear();
		}
		
		public void setScale(double scaleX,double scaleY) {
			this.scaleX = scaleX;
			this.scaleY = scaleY;
		}
		
		public double getScaleX() {
			return scaleX;
		}
		
		public double getScaleY() {
			return scaleY;
		}
		
		public MapCanvas(BufferedImage image) {
			if ( image == null ) {
				throw new IllegalArgumentException("image must not be NULL");
			}
			this.image = image;
			addMouseMotionListener( mouseAdapter );
			setFocusable( true );
		}
		
		public void addCoordinates(Coordinate... c) {
			if ( c != null ) {
				this.coordinates.addAll( Arrays.asList(c) );
			}
		}
		
		public void addCoordinate(Coordinate c) {
			this.coordinates.add( c );
		}
		
		public boolean removeCoordinate(Coordinate c) {
			return this.coordinates.remove(c);
		}
		
		@Override
		protected void paintComponent(Graphics g) 
		{
			super.paintComponent(g);
			
			g.drawImage( image , 0 , 0 , getWidth() , getHeight() , null );
			
			final double width = getWidth();
			final double height = getHeight();
					
			if ( mouseX != -1 && mouseY != -1 ) {
				double xPercentage = mouseX/width;
				double yPercentage = mouseY/height;
				g.setColor(Color.BLACK);
				g.drawString( "X = "+mouseX+" ("+xPercentage+") , Y = "+mouseY+" ("+yPercentage+")" , 15, 15);
				
				g.drawLine( 0 , mouseY , getWidth() , mouseY );
				g.drawLine( mouseX , 0 , mouseX , getHeight() );
			}
			
			for ( Coordinate coordinate : this.coordinates ) 
			{
				final Point point = unproject(coordinate);
				
				coordinate.currentPoint.setLocation( point );
				
				final int radius = 3;
				
				g.setColor(coordinate.color);
				g.fillArc( point.x - radius , point.y-radius , radius*2 , radius*2 , 0 , 360 );
			}
		}
		
		protected Point unproject(Coordinate c) 
		{
			Point2D.Double out = new Point2D.Double(0,0);
			final MillerCylindrical1Projection projection = new MillerCylindrical1Projection();		
			projection.project( c.longitudeInRad() , c.latitudeInRad() , out );
			
			return toImageCoordinates( out );
		}
		
		protected Point toImageCoordinates(Point2D.Double projection) 
		{
			  // This is the size of the globe in the image, typically the width
			  // (in most projections, the height can be derived from the width).
			
			  final double imageWidth = getWidth(); // image gets scaled to canvas size
			  final double imageHeight = getHeight(); // image gets scaled to canvas size
					  
			  // These are where the prime meridian crosses 0° latitude ( 0° longitude, 0° latitude) - all
			  // lat/long coordinates are relative to this point
			  
			  final double xPercentage=0.5,yPercentage=0.5009541984732825; // x=677 , y=525

			  final double IMAGE_GLOBE_CENTER_X = xPercentage * imageWidth; // 473.0
			  final double IMAGE_GLOBE_CENTER_Y = yPercentage * imageHeight; // 366.0			  
			  
			  double scaleX = this.scaleX*(imageWidth/1000.0);
			  double scaleY = this.scaleY*(imageHeight/1000.0);			  
			  double x = IMAGE_GLOBE_CENTER_X + projection.x*scaleX;
			  double y = IMAGE_GLOBE_CENTER_Y - projection.y*scaleY;
			  return new Point((int) Math.round(x),(int) Math.round(y));
		}
	}
}