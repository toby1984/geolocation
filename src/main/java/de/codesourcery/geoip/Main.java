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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.geoip.locate.CachingGeoLocator;
import de.codesourcery.geoip.locate.FreeGeoIPLocator;
import de.codesourcery.geoip.locate.IGeoLocator;
import de.codesourcery.geoip.locate.MaxMindGeoLocator;
import de.codesourcery.geoip.render.CurvedLineRenderer;
import de.codesourcery.geoip.render.DefaultMapElementRendererFactory;
import de.codesourcery.geoip.render.IMapElement;
import de.codesourcery.geoip.render.PointRenderer;
import de.codesourcery.geoip.render.PointRenderer.MapPoint;
import de.codesourcery.geoip.render.SimpleMapRenderer;
import de.codesourcery.geoip.trace.TracePath;

public class Main {

	protected static final GeoLocation<StringSubject> ZERO       = new GeoLocation<StringSubject>( new StringSubject("ZERO") , 0 , 0 );	
	protected static final GeoLocation<StringSubject> PARIS      = new GeoLocation<StringSubject>( new StringSubject("Paris") , 48.85341 , 2.3488 );
	protected static final GeoLocation<StringSubject> NEW_YORK   = new GeoLocation<StringSubject>( new StringSubject("New York")  , 40.7142700 , -74.0059700 );
	protected static final GeoLocation<StringSubject> MELBOURNE  = new GeoLocation<StringSubject>( new StringSubject("Melbourne")  , -37.814251 , 144.963169 );	
	protected static final GeoLocation<StringSubject> WELLINGTON = new GeoLocation<StringSubject>( new StringSubject("Wellington/NZ") , -41.2864800 , 174.7762170 );	
	protected static final GeoLocation<StringSubject> DUBLIN     = new GeoLocation<StringSubject>( new StringSubject("Dublin") , 53.3441040 , -6.2674937 );
	protected static final GeoLocation<StringSubject> HAMBURG = new GeoLocation<StringSubject>( new StringSubject("HAMBURG") , 53.553272  , 9.992092 );		

	private static IGeoLocator<StringSubject> createGeoLocator() {
		
		if ( MaxMindGeoLocator.isAvailable() ) {
			System.out.println("Using MaxMind locator");
			return new MaxMindGeoLocator();
		}
		System.out.println("Using freegeoIP locator");
		return new CachingGeoLocator<StringSubject>( new FreeGeoIPLocator() , StringSubject.class ); 
	}
	
	private static List<StringSubject> getSpammers() {
		final String[] spammers = {           
				   "188.51.199.220 ",
		           "78.93.239.190  ",
		           "94.183.242.35  ",
		           "117.199.137.252",
		           "89.137.17.19   ",
		           "178.163.110.31 ",
		           "216.81.74.110  ",
		           "2.180.185.154  ",
		           "2.186.145.1    ",
		           "5.235.200.25   ",
		           "5.236.246.8    ",
		           "24.157.16.27   ",
		           "31.180.238.254 ",
		           "41.140.90.133  ",
		           "61.82.125.252  ",
		           "62.151.202.185 ",
		           "78.5.33.22     ",
		           "78.81.248.241  ",
		           "78.83.199.5    ",
		           "78.97.60.53    ",
		           "80.110.56.219  ",
		           "87.117.229.124 ",
		           "94.20.114.43   ",
		           "95.61.65.171   ",
		           "97.65.70.66    ",
		           "97.100.99.166  ",
		           "109.75.128.14  ",
		           "117.216.117.34 ",
		           "120.50.93.15   ",
		           "122.162.136.8  ",
		           "146.200.150.7  ",
		           "151.74.153.40  ",
		           "181.29.78.118  ",
		           "181.166.91.212 ",
		           "185.47.49.234  ",
		           "186.13.6.32    ",
		           "186.86.77.155  ",
		           "186.118.37.173 ",
		           "186.249.200.187",
		           "187.240.75.133 ",
		           "188.242.212.166",
		           "189.193.199.88 ",
		           "189.200.91.206 ",
		           "190.103.204.41 ",
		           "190.109.159.76 ",
		           "190.124.105.153",
		           "190.193.76.194 ",
		           "190.194.48.66  ",
		           "200.123.55.75  ",
		           "200.127.61.109 ",
		           "201.216.221.193",
		           "206.74.253.82  ",
		           "212.50.235.138 ",
		           "212.51.45.194  ",
		           "212.92.216.240 ",
		           "212.224.105.138",
		           "213.153.47.146 "};
		
		final List<StringSubject> list = new ArrayList<>();
		for ( String s : spammers ) {
			list.add( new StringSubject( s.trim() ) );
		}
		return list;
	}
	
	public static void main(String[] args) throws Exception 
	{
		final IGeoLocator<StringSubject> locator = createGeoLocator();

		final JFrame frame = new JFrame("GeoIP");		
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

		frame.addWindowListener( new WindowAdapter() 
		{
			public void windowClosing(java.awt.event.WindowEvent e) 
			{
				try {
					locator.dispose();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			};
		});

		final MapImage image = MapImage.getRobinsonWorldMap(); // MapImage.getMillerWorldMap();		
		final MapCanvas canvas = new MapCanvas(image);
		
		for ( GeoLocation<StringSubject> loc : locator.locate( getSpammers() ) ) 
		{
			if ( loc.hasValidCoordinates() ) {
				canvas.addCoordinate( PointRenderer.createPoint( loc , Color.YELLOW ) );
			}
		}

//		canvas.addCoordinate( PointRenderer.createPoint( ZERO , Color.YELLOW ) );
//		canvas.addCoordinate( PointRenderer.createPoint( WELLINGTON , Color.RED ) );
//		canvas.addCoordinate( PointRenderer.createPoint( MELBOURNE , Color.RED ) );
//		canvas.addCoordinate( PointRenderer.createPoint( HAMBURG , Color.RED ) );
		
		final double heightToWidth = image.height() / (double) image.width(); // preserve aspect ratio of map
		canvas.setPreferredSize( new Dimension(640,(int) Math.round( 640 * heightToWidth )) );

		JPanel panel = new JPanel();
		panel.setLayout( new FlowLayout() );

		panel.add( new JLabel("Scale-X"));
		final JTextField scaleX = new JTextField( Double.toString( image.getScaleX() ) );
		scaleX.setColumns( 5 );

		final JTextField scaleY = new JTextField( Double.toString( image.getScaleY() ) );
		scaleY.setColumns( 5 );

		final ActionListener listener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				double x = Double.parseDouble( scaleX.getText() );
				double y = Double.parseDouble( scaleY.getText() );
				image.setScale(x,y);
				canvas.repaint();				
			}
		};
		scaleX.addActionListener( listener );
		scaleY.addActionListener( listener );

		panel.add( new JLabel("Scale-X"));
		panel.add( scaleX );

		panel.add( new JLabel("Scale-Y"));
		panel.add( scaleY );

		final JTextField ipAddress = new JTextField( "www.kickstarter.com" );
		ipAddress.setColumns( 20 );

		final ActionListener ipListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				final String destinationIP = ipAddress.getText(); 
				if ( StringUtils.isBlank( destinationIP ) ) 
				{
					return;
				}

				/*
				 * Perform traceroute.
				 */
				final List<String> hops;
				try {
					if ( TracePath.isPathTracingAvailable() ) 
					{
						hops = TracePath.trace( destinationIP );
					} 
					else 
					{
						System.err.println("tracepath not available.");
						if ( TracePath.isValidAddress( destinationIP ) ) {
							hops = new ArrayList<>();
							hops.add( destinationIP );
						} else {
							System.err.println( destinationIP+" is no valid IP");
							return;
						}
					}
				} 
				catch(Exception ex) {
					System.err.println("Failed to trace "+destinationIP);
					ex.printStackTrace();
					return;
				}

				System.out.println("Trace contains "+hops.size()+" IPs");

				/*
				 * Gather locations.
				 */
				final List<StringSubject> subjects = new ArrayList<>();
				for ( String ip : hops ) {
					subjects.add( new StringSubject(ip) );
				}

				final List<GeoLocation<StringSubject>> locations;
				try 
				{
					long time = -System.currentTimeMillis();
					locations = locator.locate( subjects );
					time += System.currentTimeMillis();

					System.out.println("Locating hops for "+destinationIP+" returned "+locations.size()+" valid locations ( time: "+time+" ms)");
					System.out.flush();					

				} 
				catch (Exception e2) 
				{
					e2.printStackTrace();
					return;
				}

				/*
				 * Weed-out invalid/unknown locations.
				 */
				{
					GeoLocation<StringSubject> previous = null;
					for (Iterator<GeoLocation<StringSubject>> it = locations.iterator(); it.hasNext();) 
					{
						final GeoLocation<StringSubject> location = it.next();
						if ( ! location.hasValidCoordinates() || ( previous != null && previous.coordinate().equals( location.coordinate() ) ) ) 
						{
							it.remove();
							System.err.println("Ignoring invalid/duplicate location for "+location);
						} else {
							previous = location;
						}
					}
				}
				
				/*
				 * Populate chart.
				 */

				System.out.println("Adding "+locations.size()+" hops to chart");
				System.out.flush();					

				canvas.removeAllCoordinates();

				if ( locations.size() == 1 ) 
				{
					canvas.addCoordinate( PointRenderer.createPoint( locations.get(0) , getLabel( locations.get(0) ) , Color.BLACK ) );
				} 
				else if ( locations.size() > 1 ) 
				{
					GeoLocation<StringSubject> previous = locations.get(0);
					MapPoint previousPoint = PointRenderer.createPoint( previous , getLabel( previous ) , Color.BLACK );
					final int len = locations.size();
					for ( int i = 1 ; i < len ; i++ ) 
					{
						final GeoLocation<StringSubject> current = locations.get(i);
//						final MapPoint currentPoint = PointRenderer.createPoint( current , getLabel( current ) , Color.BLACK );
						final MapPoint currentPoint = PointRenderer.createPoint( current , Color.BLACK );						
						
//						canvas.addCoordinate( LineRenderer.createLine( previousPoint , currentPoint , Color.RED ) );
						canvas.addCoordinate( CurvedLineRenderer.createLine( previousPoint , currentPoint , Color.RED ) );
						
						previous = locations.get(i);
						previousPoint = currentPoint;
					}
				}
				System.out.println("Finished adding");
				System.out.flush();
				canvas.repaint();			
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

	protected static String getLabel(GeoLocation<?> location) 
	{
		String city = location.parameter(GeoLocation.KEY_CITY, "" ).toString();
		String country = location.parameter(GeoLocation.KEY_COUNTRY, "" ).toString();
		
		String result = location.subject().toString();
		if ( StringUtils.isNotBlank( city ) ) {
			result += ","+city;
			if ( StringUtils.isNotBlank( country ) ) {
				result += ","+country;
			}
		} else if ( StringUtils.isNotBlank( country ) ) {
			result += ","+country;
		}
		return result;
	}	

	protected static final class MapCanvas extends JPanel {

		protected final SimpleMapRenderer mapRenderer;

		protected int mouseX=-1;
		protected int mouseY=-1;
		
		protected Point selectionStart= null;
		protected boolean dragging = false;		
		
		protected ImageRegion currentRegion;
		
		private final MouseAdapter mouseAdapter = new MouseAdapter() 
		{
			public void mousePressed(java.awt.event.MouseEvent e) 
			{
				if ( e.getButton() == MouseEvent.BUTTON1 ) 
				{
					selectionStart=new Point(e.getX() , e.getY() );
					dragging = true;
				}
			}
					
			public void mouseReleased(MouseEvent e) 
			{
				if ( e.getButton() == MouseEvent.BUTTON3 ) {
					zoomOut();
					return;
				}
				
				if ( e.getButton() == MouseEvent.BUTTON1 ) 
				{
					mouseX = e.getX();
					mouseY = e.getY();
					
					// ZOOM-IN
					
					final int minX = Math.min( selectionStart.x , mouseX );
					final int maxX = Math.max( selectionStart.x , mouseX );
					
					final int minY = Math.min( selectionStart.y , mouseY );
					final int maxY = Math.max( selectionStart.y , mouseY );
					
					Point p0 = new Point(minX,minY);
					Point p1 = new Point(maxX,maxY);
					
					currentRegion.unproject( getWidth() , getHeight() , p0 );
					currentRegion.unproject( getWidth() , getHeight() , p1 );					
					
					final double heightToWidth = getHeight() / (double) getWidth();  
					final int newWidth = p1.x - p0.x;
					final int newHeight = (int) Math.round( newWidth*heightToWidth ); // preserve aspect ratio

					currentRegion = new ImageRegion( p0.x , p0.y , newWidth , newHeight );
					
					// update display
					selectionStart = null;
					dragging = false;
					
					MapCanvas.this.repaint();
				}
			}
			
			public void mouseDragged(MouseEvent e) 
			{
				if ( dragging ) 
				{
					mouseX = e.getX();
					mouseY = e.getY();
					MapCanvas.this.repaint();
				}
			}
			
			public void mouseMoved(java.awt.event.MouseEvent e) 
			{
				mouseX = e.getX();
				mouseY = e.getY();

				IMapElement element = mapRenderer.getClosestMapElement( mouseX , mouseY , 15 );

				if ( element != null ) 
				{
					element = element.getClosestMapElement( mouseX , mouseY , 15 );
				}
				setToolTipText( toTooltip( element ) );
				MapCanvas.this.repaint();
			};
		};

		protected String toTooltip(IMapElement element) 
		{
			if ( element != null ) 
			{
				switch( element.getType() ) 
				{
				case POINT:
					final MapPoint point = (MapPoint) element;
					return getLabel( point.location );
				default:
						return null;
				}
			}
			return null;
		}
		
		private void zoomOut() 
		{
			Point p0 = new Point();
			Point p1 = new Point();
			
			currentRegion.getTopLeftCorner( p0 );
			currentRegion.getBottomRightCorner( p1 );
			
			int centerX = p0.x + currentRegion.width/2;
			int centerY = p0.y + currentRegion.height/2;
			
			final int newWidth = currentRegion.width*2;
			
			final double heightToWidth = getHeight() / (double) getWidth(); 			
			final int newHeight = (int) Math.round( newWidth * heightToWidth );	// preserve aspect ratio		
			
			p0.x = centerX - newWidth/2;
			p0.y = centerY - newHeight/2;
			
			currentRegion = mapRenderer.getMapImage().limit( new ImageRegion( p0.x, p0.y , newWidth , newHeight ) );
			
			System.out.println("Zooming out to "+currentRegion);
			MapCanvas.this.repaint();
		}
		
		public void removeAllCoordinates() {
			mapRenderer.removeAllCoordinates();
		}

		public MapCanvas(MapImage image) 
		{
			if ( image == null ) {
				throw new IllegalArgumentException("image must not be NULL");
			}
			this.mapRenderer = new SimpleMapRenderer();
			this.mapRenderer.setMapImage( image );
			this.currentRegion = image.fullRegion();
			this.mapRenderer.setMapElementRendererFactory( new DefaultMapElementRendererFactory() );

			addMouseMotionListener( mouseAdapter );
			addMouseListener( mouseAdapter );
			requestFocusInWindow();
		}

		public void addCoordinates(Collection<IMapElement> elements ) {
			this.mapRenderer.addCoordinates( elements );
		}

		public void addCoordinate(IMapElement c) {
			this.mapRenderer.addCoordinate( c );
		}

		public boolean removeCoordinate(IMapElement c) {
			return this.mapRenderer.removeCoordinate( c );
		}

		private Font font;
		
		@Override
		protected void paintComponent(Graphics g) 
		{
			super.paintComponent(g);

			if ( font == null ) {
				font = new Font("serif", Font.BOLD , 12 );
				
			}
			g.setFont( font );
			
			mapRenderer.renderMap( g ,  currentRegion , getWidth() ,  getHeight() );

			if ( dragging && selectionStart != null ) 
			{
				final int minX = Math.min( selectionStart.x , mouseX );
				final int maxX = Math.max( selectionStart.x , mouseX );
				
				final int minY = Math.min( selectionStart.y , mouseY );
				final int maxY = Math.max( selectionStart.y , mouseY );

				g.setColor(Color.BLUE);				
				g.drawRect( minX ,minY , maxX - minX , maxY - minY );
			}
			
			if ( ! dragging && mouseX != -1 && mouseY != -1 ) 
			{
				g.setColor(Color.BLACK);
				g.drawLine( 0 , mouseY , getWidth() , mouseY );
				g.drawLine( mouseX , 0 , mouseX , getHeight() );
			}
		}
	}
}