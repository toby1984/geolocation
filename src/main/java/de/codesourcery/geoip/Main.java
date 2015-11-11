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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.geoip.locate.CachingGeoLocator;
import de.codesourcery.geoip.locate.DelegatingGeoLocator;
import de.codesourcery.geoip.locate.FreeGeoIPLocator;
import de.codesourcery.geoip.locate.IGeoLocator;
import de.codesourcery.geoip.locate.IPInfoDbLocator;
import de.codesourcery.geoip.locate.MaxMindGeoLocator;
import de.codesourcery.geoip.render.CurvedLineRenderer;
import de.codesourcery.geoip.render.DefaultMapElementRendererFactory;
import de.codesourcery.geoip.render.IMapElement;
import de.codesourcery.geoip.render.LineRenderer.MapLine;
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

    private IGeoLocator<StringSubject> locator;
    private MapCanvas canvas;
    private volatile ProgressMonitor progressMonitor; 
    
    private JCheckBox showAllLabels = new JCheckBox("Show all labels",false);
    
    protected interface ThrowingSupplier<T> 
    {
        public T get() throws Exception;
    }
    
    protected interface ThrowingConsumer<T> 
    {
        public void accept(T obj) throws Exception;
    }   
    
    protected interface ThrowingRunnable
    {
        public void run() throws Exception;
    }         
    
	private IGeoLocator<StringSubject> createGeoLocator() 
	{
	    final DelegatingGeoLocator<StringSubject> delegate = new DelegatingGeoLocator<>( new MaxMindGeoLocator() , new IPInfoDbLocator() , new FreeGeoIPLocator() );
		return new CachingGeoLocator<StringSubject>( delegate , StringSubject.class ); 
	}
	
	private List<StringSubject> getSpammers() 
	{
		final String[] spammers = { "www.heise.de" };           
				   
		
		final List<StringSubject> list = new ArrayList<>();
		for ( String s : spammers ) {
			list.add( new StringSubject( s.trim() ) );
		}
		return list;
	}
	
	public static void main(String[] args) throws Exception 
	{
	    new Main().run();
	}
	
	private void runOnEDT(Runnable r)
	{
	    if ( SwingUtilities.isEventDispatchThread() ) 
	    {
	        r.run();
	    } else {
	        try {
                SwingUtilities.invokeAndWait( r );
            } catch (InvocationTargetException | InterruptedException e) {
                error( e,  e.getMessage() );
            } 
	    }
	}
	
	private void error(Throwable t , String message) 
	{
	    runOnEDT( () -> 
	    {
	        if ( t != null ) {
	            t.printStackTrace();
	        }
	        JOptionPane.showMessageDialog(null, message);
	    });
	}
	
	protected boolean onProgress(int currentItem,int totalItemCount) 
	{
	    final AtomicBoolean cancelled = new AtomicBoolean();
	    runOnEDT( () -> 
	    {
	        if ( progressMonitor == null ) 
	        {
	            progressMonitor = new ProgressMonitor( canvas ,"Looking up "+totalItemCount+" geo locations...\n", "" , 0 , totalItemCount );
	        }	        
	        if ( currentItem == 0 ) {
	            progressMonitor.setMaximum( totalItemCount );
	        }
	        final int progress = (int) (totalItemCount == 0 ? 100.0f : 100.0f*(currentItem/(float) totalItemCount));
	        String message = String.format("Completed %d%% (%d of %d).\n", progress, currentItem , totalItemCount );
	        System.out.println( message );
	        progressMonitor.setNote(message);	        
	        progressMonitor.setProgress( currentItem );

	        if ( currentItem == totalItemCount ) {
	            progressMonitor = null;
	        } 
	        else if ( progressMonitor.isCanceled() ) 
	        {
	            cancelled.set(true);
	            progressMonitor.close();
	            progressMonitor = null;
	        }
	    });
	    return ! cancelled.get();
	}
	
	public void run() throws Exception {

        locator = createGeoLocator();

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

        final MapImage image = MapImage.getRobinsonWorldMap();
        canvas = new MapCanvas(image);
        
        for ( GeoLocation<StringSubject> loc : locator.locate( getSpammers(), this::onProgress ) ) 
        {
            if ( loc.hasValidCoordinates() ) {
                canvas.addCoordinate( PointRenderer.createPoint( loc , Color.YELLOW ) );
            }
        }

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
                performTrace( destinationIP , loc -> populateChart(loc) , throwable ->  error( throwable , "Something went wrong: "+throwable.getMessage() ) );
            }
        };  
        
        ipAddress.addActionListener( ipListener );

        panel.add( showAllLabels );
        
        showAllLabels.addActionListener( e -> showLabelsToggled() );
        
        panel.add( new JLabel("IP"));       
        panel.add( ipAddress );

        frame.getContentPane().setLayout( new BorderLayout() );
        frame.getContentPane().add( panel , BorderLayout.NORTH);
        frame.getContentPane().add( canvas , BorderLayout.CENTER );
        frame.pack();
        frame.setVisible(true);
	}
	
	private void populateChart(List<GeoLocation<StringSubject>> locations) 
	{
        System.out.println("Adding "+locations.size()+" hops to chart");
        System.out.flush();                 

        canvas.removeAllCoordinates();

        if ( locations.size() == 1 ) 
        {
            final MapPoint point = PointRenderer.createPoint( locations.get(0) , getLabel( locations.get(0) ) , Color.BLACK );
            markAsEndpoint( point );
            canvas.addCoordinate( point );
        } 
        else if ( locations.size() > 1 ) 
        {
            GeoLocation<StringSubject> previous = locations.get(0);
            MapPoint previousPoint = PointRenderer.createPoint( previous , getLabel( previous ) , Color.BLACK );
            markAsEndpoint( previousPoint );
            final int len = locations.size();
            for ( int i = 1 ; i < len ; i++ ) 
            {
                final GeoLocation<StringSubject> current = locations.get(i);
                final MapPoint currentPoint = PointRenderer.createPoint( current , getLabel(current) , Color.BLACK ); 
                
                final boolean isLast = i == len-1;
                if ( isLast ) {
                    markAsEndpoint( currentPoint );
                }
                canvas.addCoordinate( CurvedLineRenderer.createLine( previousPoint , currentPoint , Color.RED ) );
                
                previous = locations.get(i);
                previousPoint = currentPoint;
            }
        }
        System.out.println("Finished adding");
        System.out.flush();
        
        showLabelsToggled();
	}
	
    private boolean isPoint(IMapElement e) {
        return e.hasType( IMapElement.Type.POINT );
    }	
    
    private void markAsEndpoint(MapPoint point) 
    {
        point.getAttributes().put( "endpoint", Boolean.TRUE );
        point.getAttributes().put( IMapElement.ATTRIBUTE_RENDER_LABEL , Boolean.TRUE );
    }

	private boolean isEndpoint(IMapElement e) 
	{
	    return isPoint(e) && e.getAttributes().containsKey( "endpoint" ); 
	}
	
	private Collection<IMapElement> getUniquePoints(Collection<IMapElement> elements) {
	    
	    final Map<GeoLocation<?>,IMapElement> points = new HashMap<>();
	    for ( IMapElement e : elements ) 
	    {
	        if ( e instanceof MapPoint ) {
	            points.put( ((MapPoint) e).location , e );
	        } 
	        else if ( e instanceof MapLine ) 
	        {
	            final MapPoint start = ((MapLine) e).start;
	            final MapPoint end = ((MapLine) e).end;
	            points.put( start.location , start );
	            points.put( end.location , end );
	        } else {
	            throw new RuntimeException("Don't know how to unwrap "+e);
	        }
	    }
	    return points.values();
	}
	
	private void showLabelsToggled() 
	{
	    final boolean showLabels = showAllLabels.isSelected();
	    
	    getUniquePoints( canvas.mapRenderer.getMapElements() ).stream()
	    .filter( e -> ! isEndpoint(e) ) // endpoint labels are always drawn
	    .forEach( point -> 
	    {
	        if ( showLabels ) {
	            point.getAttributes().put( IMapElement.ATTRIBUTE_RENDER_LABEL , Boolean.TRUE );
	        } else {
	            point.getAttributes().remove( IMapElement.ATTRIBUTE_RENDER_LABEL );
	        }
	    });
	    canvas.repaint();
	}
    protected void performTrace(String destinationIP,Consumer<List<GeoLocation<StringSubject>>> onSuccess,Consumer<Throwable> onFailure) 
    {
        final ThrowingSupplier<List<String>> hopsSupplier = () -> 
        {
            List<String> hops = null;
            if ( TracePath.isPathTracingAvailable() ) 
            {
                hops = TracePath.trace( destinationIP, line -> System.out.println("TRACE: "+line ) );
                hops = hops.stream().filter( ip -> ! TracePath.isUnroutableAddress( ip ) ).collect( Collectors.toList() );
            } 
            else 
            {
                System.err.println("path tracing not available.");
                if ( ! TracePath.isValidAddress( destinationIP ) ) {
                    throw new RuntimeException( destinationIP+" is no valid IP");
                }
                hops = Arrays.asList( destinationIP );
            }
            System.out.println("Trace contains "+hops.size()+" IPs");
            return hops;
        };
        
        final ThrowingConsumer<List<String>> hopsConsumer = hops -> 
        {
            final List<StringSubject> subjects = hops.stream().map( StringSubject::new ).collect(Collectors.toList());
            long time = -System.currentTimeMillis();
            final List<GeoLocation<StringSubject>> locations = locator.locate( subjects, Main.this::onProgress );
            time += System.currentTimeMillis();

            System.out.println("Locating hops for "+destinationIP+" returned "+locations.size()+" valid locations ( time: "+time+" ms)");
            System.out.flush();    
            
            /*
             * Weed-out invalid/unknown locations.
             */
            GeoLocation<StringSubject> previous = null;
            for (Iterator<GeoLocation<StringSubject>> it = locations.iterator(); it.hasNext();) 
            {
                final GeoLocation<StringSubject> location = it.next();
                if ( ! location.hasValidCoordinates() ) 
                {
                    it.remove();
                    System.err.println("Ignoring invalid location for "+location);
                } 
                else if ( previous != null && previous.coordinate().equals( location.coordinate() ) ) 
                {
                    it.remove();
                    System.err.println("Ignoring duplicate location for "+location+" <-> "+previous);
                } else {
                    previous = location;
                }
            }
            runOnEDT( () -> onSuccess.accept( locations ) );
        };
        
        doWith( hopsSupplier , hopsConsumer , onFailure );
    }
    
    protected <A> void doWith(ThrowingSupplier<A> supp, ThrowingConsumer<A> consumer, Consumer<Throwable> onFailure) {
        
        async( () -> 
        {
            consumer.accept( supp.get() );
        } , onFailure );
    }
    
    private void async(ThrowingRunnable block,Consumer<Throwable> onFailure) 
    {
        final Thread t = new Thread() 
        {
            @Override
            public void run() 
            {
                try 
                {
                    block.run();
                } catch(Exception e) {
                    onFailure.accept(e);
                }
            }
        };
        t.setDaemon( true );
        t.start();        
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

					final ImageRegion newRegion = new ImageRegion( p0.x , p0.y , newWidth , newHeight );
					
					if ( newRegion.width > 10 && newRegion.height > 10 ) { 
					    currentRegion = newRegion;
					}
					
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