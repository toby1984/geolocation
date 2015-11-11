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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * Describes a rectangular region with relative to a source image. 
 *
 * <p>All coordinates and sizes are percentage values relative to a source image.</p>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class ImageRegion {

	// left-upper corner
	public final int xOrigin;
	public final int yOrigin;

	public final int width;
	public final int height;

	public ImageRegion(ImageRegion other) 
	{
		this.xOrigin = other.xOrigin;
		this.yOrigin = other.yOrigin;
		this.width = other.width;
		this.height = other.height;
	}
	
	public void getTopLeftCorner(Point point) 
	{
		point.setLocation( xOrigin ,  yOrigin );
	}
	
	public void getBottomRightCorner(Point point) 
	{
		point.setLocation( xOrigin+width,yOrigin+height);
	}	
	
	/**
	 * Convert from cartesian coordinates in the source image to cartesian coordinates
	 * in the (possibly scaled) output image.
	 *   
	 * @param srcImage
	 * @param dstWidth width in which this region was rendered
	 * @param dstHeight height in which this region was rendered
	 * @param point point in src image coordinates
	 */
	public void project(int dstWidth,int dstHeight,Point2D.Double point) 
	{
		int srcTopLeftX = xOrigin;
		int srcTopLeftY = yOrigin;
		
		// convert to dst image coordinates
		double x = (point.x - srcTopLeftX) / (double) width;
		double y = (point.y - srcTopLeftY) / (double) height;
		
		point.x = x * (double) dstWidth; 
		point.y = y * (double) dstHeight;
	}
	
	/**
	 * Convert from cartesian coordinates in the destination image to cartesian
	 * coordinates in the source image.
	 * 
	 * @param srcImage
	 * @param dstWidth
	 * @param dstHeight
	 * @param point
	 */
	public void unproject(int dstWidth,int dstHeight,Point point) 
	{
		// width,height in src image pixels
		double myWidth = width;
		double myHeight = height;
		
		double xPerc = point.x / (double) dstWidth;
		double yPerc = point.y / (double) dstHeight;
		
		double localX = xPerc * myWidth;
		double localY = yPerc * myHeight;
		
		double srcTopLeftX = xOrigin;
		double srcTopLeftY = yOrigin;
		
		// convert to src image coordinates
		point.x = (int) Math.round( srcTopLeftX + localX );
		point.y = (int) Math.round( srcTopLeftY + localY );
	}	
	
	public void getSize(Dimension dim) {
		dim.width = width;
		dim.height = height;
	}
	
	public ImageRegion(int x0,int y0,int width,int height) 
	{
		this.xOrigin = x0;
		this.yOrigin = y0;
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return "MapImageRegion [xOrigin=" + xOrigin + ", yOrigin=" + yOrigin
				+ ", width=" + width + ", height=" + height + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + height;
		result = prime * result + width;
		result = prime * result + xOrigin;
		result = prime * result + yOrigin;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ImageRegion)) {
			return false;
		}
		ImageRegion other = (ImageRegion) obj;
		if (height != other.height) {
			return false;
		}
		if (width != other.width) {
			return false;
		}
		if (xOrigin != other.xOrigin) {
			return false;
		}
		if (yOrigin != other.yOrigin) {
			return false;
		}
		return true;
	}
}