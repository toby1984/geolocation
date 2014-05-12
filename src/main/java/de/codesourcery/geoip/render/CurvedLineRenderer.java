package de.codesourcery.geoip.render;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.NevilleInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunctionLagrangeForm;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import de.codesourcery.geoip.GeoLocation;
import de.codesourcery.geoip.render.LineRenderer.MapLine;
import de.codesourcery.geoip.render.PointRenderer.MapPoint;

/**
 * Connects two locations with a curved/interpolated line.
 * 
 * <p>If points are too close together (with 'too close' currently being hard-coded) or
 * interpolating a curve failed, a straight line will be drawn instead.</p>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class CurvedLineRenderer implements IMapElementRenderer {

	private final Graphics g;
	private final IImageProjection imageProject;
	private final PointRenderer pointRenderer;
	
	public static MapCurvedLine createLine(GeoLocation<?> start,GeoLocation<?> end , Color color) 
	{
		return new MapCurvedLine( PointRenderer.createPoint( start , color) ,
							 PointRenderer.createPoint( end , color) ,
							 color);
	}
	
	public static MapCurvedLine createLine(MapPoint start,MapPoint end , Color color) 
	{
		return new MapCurvedLine( start ,
							 end ,
							 color);
	}		
	
	public static final class MapCurvedLine extends MapLine
	{
		public MapCurvedLine(MapPoint start,MapPoint end,Color color) {
			super(start,end,color);
		}
		
		@Override
		public Type getType() {
			return IMapElement.Type.CURVED_LINE;
		}

		@Override
		public boolean hasType(Type t) {
			return t.equals( IMapElement.Type.LINE );
		}
	}
	
	public CurvedLineRenderer(IImageProjection imageProject, Graphics g) {
		this.imageProject = imageProject;
		this.g = g;
		this.pointRenderer = new PointRenderer( imageProject, g);
	}	
	
	@Override
	public void render(IMapElement element) 
	{
		final MapCurvedLine line = (MapCurvedLine) element;
		if ( ! line.isValid() ) {
			line.calculateCoordinates( imageProject );
		}
		
		pointRenderer.render( line.start );
		pointRenderer.render( line.end );
		
		Vector2D p0 = new Vector2D( line.start.point.x , line.start.point.y );
		Vector2D p1 = new Vector2D( line.end.point.x , line.end.point.y );
		
		Vector2D v = p1.subtract( p0 );

		// do not interpolate distances that are very short, this yields
		// ridiculously curved lines
		// TODO: These values should really be configurable or smartly calculated depending on the image size
		if ( p0.distance(p1) <= 40 || Math.abs( v.getX() ) <= 20 || Math.abs( v.getY() ) <= 20 ) {
			drawLine( p0 , p1 );
			return;
		}		
		
		Vector2D center = p0.add( p1 ).scalarMultiply(0.5);
		Vector2D normal = new Vector2D(v.getY(),-v.getX() ).normalize();
		Vector2D pTop = center.add( normal.scalarMultiply(-10) );
		
		final List<Vector2D> points = Arrays.asList( p0,pTop,p1);
		
		// interpolation requires X values to be strictly increasing
		Collections.sort( points ,new Comparator<Vector2D>() {
			@Override
			public int compare(Vector2D o1, Vector2D o2) 
			{
				return Double.compare( o1.getX() ,  o2.getX() );
			}
		} );
		
		final int len = points.size();
		double[] x = new double[len];
		double[] y = new double[len];
		for ( int i = 0 ; i < len ; i++ ) 
		{
			Vector2D p = points.get(i);
			x[i] = p.getX();
			y[i] = p.getY();
			
			// interpolate() requires X values to be strictly increasing and 
			// throws an exception otherwise, just render a straight line in this case
			if ( i > 0 && x[i-1] == x[i] ) {
				drawLine( p0 , p1 );
				return;
			}
		}
		
		final PolynomialFunctionLagrangeForm splineFunction = new NevilleInterpolator().interpolate(x ,  y );
		render( splineFunction , x[0] , x[x.length-1] );
	}
	
	private void render(UnivariateFunction func,double xMin,double xMax) 
	{
		final int stepCount=10;
		double step = (xMax-xMin)/ (double) stepCount;

		double previousX = 0;
		double previousY = 0;
		for ( int i = 0 ; i <= stepCount ; i++) 
		{
			double currentX = xMin + i*step;
			double currentY = func.value( currentX );
			if ( currentX != xMin ) {
				g.drawLine( round( previousX) , round( previousY) , round( currentX ), round( currentY ) );
			}
			previousX = currentX;
			previousY = currentY;
		}
	}
	
	private void drawLine(Vector2D p0,Vector2D p1) {
		g.drawLine( round( p0.getX() ) , round( p0.getY() ) , round( p1.getX() ), round( p1.getY() ) );
	}
	
	private static int round(double v) {
		return (int) Math.round(v);
	}
}