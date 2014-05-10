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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import com.jhlabs.map.proj.MillerCylindrical1Projection;

import de.codesourcery.geoip.locate.CachingGeoLocator;
import de.codesourcery.geoip.locate.FreeGeoIPLocator;
import de.codesourcery.geoip.locate.IGeoLocator;
import de.codesourcery.geoip.render.IImageProjection;
import de.codesourcery.geoip.render.IMapElement;
import de.codesourcery.geoip.render.IMapElement.Type;
import de.codesourcery.geoip.render.IMapElementRenderer;
import de.codesourcery.geoip.render.IMapElementRendererFactory;
import de.codesourcery.geoip.render.LineRenderer;
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

		final IGeoLocator<StringSubject> locator = new CachingGeoLocator<StringSubject>( new FreeGeoIPLocator() , StringSubject.class );

		final JFrame frame = new JFrame("dummy");		
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

		final MapCanvas canvas = new MapCanvas(image);

		canvas.setScale( SCALE_X , SCALE_Y );
		canvas.setPreferredSize( new Dimension(640,480 ) );

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

		final JTextField ipAddress = new JTextField( "www.slashdot.org" );
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
				for (Iterator<GeoLocation<StringSubject>> it = locations.iterator(); it.hasNext();) 
				{
					final GeoLocation<StringSubject> location = it.next();
					if ( ! location.hasValidCoordinates() ) 
					{
						it.remove();
						System.err.println("Ignoring invalid location for "+location);
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
					canvas.addCoordinate( PointRenderer.createPoint( locations.get(0) , Color.RED ) );
				} 
				else if ( locations.size() > 1 ) 
				{
					GeoLocation<StringSubject> previous = locations.get(0);
					final int len = locations.size();
					for ( int i = 1 ; i < len ; i++ ) 
					{
						final GeoLocation<StringSubject> current = locations.get(i);
						canvas.addCoordinate( LineRenderer.createLine( previous , current , Color.RED ) );
						previous = locations.get(i);
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

	protected static final class MapCanvas extends JPanel {

		private final SimpleMapRenderer mapRenderer;

		private int mouseX=-1;
		private int mouseY=-1;

		private final MouseAdapter mouseAdapter = new MouseAdapter() 
		{
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
					final GeoLocation<?> location = point.location;
					return location.subject().toString()+
							",city="+point.location.parameter(GeoLocation.KEY_CITY, "" )+								
							",country="+point.location.parameter(GeoLocation.KEY_COUNTRY, "" );
				}
			}
			return null;
		}

		public void removeAllCoordinates() {
			mapRenderer.removeAllCoordinates();
		}

		public void setScale(double scaleX,double scaleY) {
			mapRenderer.setScale( scaleX,  scaleY );
		}

		public double getScaleX() {
			return mapRenderer.getScaleX();
		}

		public double getScaleY() {
			return mapRenderer.getScaleY();
		}

		public MapCanvas(BufferedImage image) 
		{
			if ( image == null ) {
				throw new IllegalArgumentException("image must not be NULL");
			}
			this.mapRenderer = new SimpleMapRenderer();

			this.mapRenderer.setMapImage( image );
			this.mapRenderer.setProjection( new MillerCylindrical1Projection() );
			this.mapRenderer.setOrigin( 0.5 , 0.5009541984732825);
			this.mapRenderer.setScale(160, 225);
			this.mapRenderer.setMapElementRendererFactory( new IMapElementRendererFactory() {

				@Override
				public IMapElementRenderer createRenderer(Type type,IImageProjection projection, Graphics g) 
				{
					switch( type ) 
					{
					case LINE:
						return new LineRenderer(projection, g);
					case POINT:
						return new PointRenderer(projection, g);
					default:
						throw new RuntimeException("Unhandled type "+type);
					}
				}
			});

			addMouseMotionListener( mouseAdapter );
			setFocusable( true );
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

		@Override
		protected void paintComponent(Graphics g) 
		{
			super.paintComponent(g);

			mapRenderer.renderMap( g ,  getWidth() ,  getHeight() );

			final double width = getWidth();
			final double height = getHeight();

			if ( mouseX != -1 && mouseY != -1 ) 
			{
				double xPercentage = mouseX/width;
				double yPercentage = mouseY/height;

				g.setColor(Color.BLACK);
				g.drawString( "X = "+mouseX+" ("+xPercentage+") , Y = "+mouseY+" ("+yPercentage+")" , 15, 15);

				g.drawLine( 0 , mouseY , getWidth() , mouseY );
				g.drawLine( mouseX , 0 , mouseX , getHeight() );
			}
		}
	}
}