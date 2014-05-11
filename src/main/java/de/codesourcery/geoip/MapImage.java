package de.codesourcery.geoip;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.jhlabs.map.proj.MillerCylindrical1Projection;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.RobinsonProjection;

/**
 * A map image with it's associated {@link Projection}.
 *  
 * @author tobias.gierke@code-sourcery.de
 */
public class MapImage {

	public final BufferedImage image;
	public final Projection projection;
	
	// Scaling factors used to map cartesian coordinates returned by the 
	// map projection to actual image coordinates
	private double scaleX;
	private double scaleY;
	
    // These are where the prime meridian crosses the aequator - all lat/long coordinates are relative to this point
	// these values are percentage values relative to the map image size (e.g. 0.5/0.5 would be right in the center of the image)
    private double originXPercentage;
    private double originYPercentage;
    
    private boolean hasChanged = false;
    
    private static BufferedImage loadImage(String classpath) throws IOException 
    {
		try ( InputStream io = Main.class.getResourceAsStream( classpath ) ) 
		{
			if ( io == null ) {
				throw new IOException("failed to open world map image");
			}
			return ImageIO.read( io );
		}
    }
    
	public static MapImage getRobinsonWorldMap() throws IOException 
	{
		final MapImage result = new MapImage( loadImage("/world_robinson.png") , new RobinsonProjection() );
		result.setOriginPercentages(0.4744791666666667,0.5);
		result.setScale( 186 , 360 );
		return result;
	}
	
	public static MapImage getMillerWorldMap() throws IOException 
	{
		final MapImage result = new MapImage( loadImage( "/world_miller_cylindrical.jpg" ) , new MillerCylindrical1Projection() );
		result.setOriginPercentages( 0.5 , 0.5009541984732825);
		result.setScale( 158.5 , 213 );		
		return result;
	}	
	
	public MapImage(BufferedImage image, Projection projection) {
		this.image = image;
		this.projection = projection;
	}
	
	/**
	 * Sets some magic scaling factors used to map the cartesian
	 * coordinates returned by {@link Projection#project(double, double, java.awt.geom.Point2D.Double)}
	 * onto the map image.
	 * 
	 * @param scaleX
	 * @param scaleY
	 * 
	 * @see #getScaleX()
	 * @see #getScaleY()
	 */	
	public void setScale(double scaleX,double scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		hasChanged = true;
	}
	
	/**
	 * Specifies the origin of the world's coordinate system (0° latitude, 0° longitude) on the map image.
	 *   
	 * @param xPercentage percentage value (range 0...1) relative to the width of the map image. 0 = left-most pixel, 1 = right-most pixel
	 * @param yPercentage percentage value (range 0...1) relative to the height of the map image. 0 = top-most pixel, 1 = bottom-most pixel
	 */	
	public void setOriginPercentages(double xPercentage,double yPercentage) {
		this.originXPercentage = xPercentage;
		this.originYPercentage = yPercentage;
		hasChanged = true;
	}
	
	public boolean hasChanged() {
		return hasChanged;
	}
	
	public void resetChanged() {
		hasChanged = false;
	}
	
	public double getScaleX() {
		return scaleX;
	}
	
	public double getScaleY() {
		return scaleY;
	}
	
	public double getOriginXPercentage() {
		return originXPercentage;
	}
	
	public double getOriginYPercentage() {
		return originYPercentage;
	}	
}