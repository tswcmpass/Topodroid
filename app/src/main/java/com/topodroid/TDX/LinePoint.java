/* @file LinePoint.java
 *
 * @author marco corvi
 * @date dec 2011
 *
 * @brief TopoDroid drawing: a point on a line
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 *
 * points along a line: 
 *                                       ,---- possible CP of next
 *    ...----prev-----C1----C2----P----...----next----...
 *                   \______________/
 *                      LinePoint         
 *
 * 2016-06-16
 * control points coords renamed from mX1 to x1, etc.
 * following similar renaming for coords of Point2D (ie. BezierPoint)
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDMath;
import com.topodroid.math.TDVector;
import com.topodroid.prefs.TDSetting;
import com.topodroid.common.PlotType;
import com.topodroid.num.TDNum;
import com.topodroid.math.Point2D;

import java.io.PrintWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

import android.graphics.Matrix;

public class LinePoint extends Point2D
{
  // public float x, y; // inherited from PointF
  public float x1, y1;    // first control point (to the right of the previous LinePoint)
  public float x2, y2;    // second control point (to the left of this LinePoint)
  public boolean has_cp;  // whether the line-point has CPs or not
  public LinePoint mPrev; // previous LinePoint on the line
  public LinePoint mNext; // next LinePoint on the line

  /** change presentation from landscape to portrait
   */
  void landscapeToPortrait()
  {
    float t;
    t = x;  x = -y;  y = t;
    t =x1;  x1=-y1;  y1= t;
    t =x2;  x2=-y2;  y2= t;
  }

  /** flip horizontally
   * @param z   unused
   * @note from ICanvasCommand
   */
  public void flipXAxis(float z)
  {
    float dx = 2 * DrawingUtil.CENTER_X;
    x1 = dx - x1;
    x2 = dx - x2;
    x  = dx - x;
  }

  /** shift first control-point
   * @param dx   X shift coord
   * @param dy   Y shift coord
   */
  void shiftCP1By( float dx, float dy )
  {
     x1 += dx;
     y1 += dy;
  }

  /** shift second control-point
   * @param dx   X shift coord
   * @param dy   Y shift coord
   */
  void shiftCP2By( float dx, float dy )
  {
     x2 += dx;
     y2 += dy;
  }

  /** shift the point, and the second control-point, if the next point has control-points shift also its first control-point
   * @param dx   X shift coord
   * @param dy   Y shift coord
   */
  void shiftBy( float dx, float dy )
  {
    x += dx;
    y += dy;
    if ( has_cp ) {
      // x1 += dx;
      // y1 += dy;
      x2 += dx;
      y2 += dy;
    }
    if ( mNext != null && mNext.has_cp ) {
      mNext.x1 += dx;
      mNext.y1 += dy;
    }
  }

  /** scale by Z
   * @param z   scale factor
   * @param m   unused
   */
  void scaleBy( float z, Matrix m )
  {
    x *= z;
    y *= z;
    if ( has_cp ) {
      // x1 *= z;
      // y1 *= z;
      x2 *= z;
      y2 *= z;
    }
    if ( mNext != null && mNext.has_cp ) {
      mNext.x1 *= z;
      mNext.y1 *= z;
    }
  }

  /** affine transform
   * @param mm  transform 3x3 array
   * @param m   transform matrix
   */
  void affineTransformBy( float[] mm, Matrix m )
  {
    float xx = mm[0] * x + mm[1] * y + mm[2];
           y = mm[3] * x + mm[4] * y + mm[5];
           x = xx;
    if ( has_cp ) {
      xx = mm[0] * x2 + mm[1] * y2 + mm[2];
      y2 = mm[3] * x2 + mm[4] * y2 + mm[5];
      x2 = xx;
    }
    if ( mNext != null && mNext.has_cp ) {
      xx = mm[0] * mNext.x1 + mm[1] * mNext.y1 + mm[2];
      mNext.y1 = mm[3] * mNext.x1 + mm[4] * mNext.y1 + mm[5];
      mNext.x1 = xx;
    }
  }

  /** make an isolated line-point copying coords from another
   * @param lp   line point to copy
   */
  LinePoint( LinePoint lp )
  { 
    x  = lp.x;
    y  = lp.y;
    x1 = lp.x1;
    y1 = lp.y1;
    x2 = lp.x2;
    y2 = lp.y2;
    has_cp = false;
    mNext = null;
    mPrev = null;
  }

  /** make a copy LinePoint and append to another point
   * @param lp   line-point to copy from
   * @param prev point to append to
   */
  LinePoint( LinePoint lp, LinePoint prev )
  { 
    x  = lp.x;
    y  = lp.y;
    x1 = lp.x1;
    y1 = lp.y1;
    x2 = lp.x2;
    y2 = lp.y2;
    has_cp = lp.has_cp;
    mNext = null;
    if ( prev != null ) prev.mNext = this;
    mPrev = prev;
  }
  
  /** make a linepoint and append to another one
   * @param x0   linepoint X coords (no control-points)
   * @param y0   linepoint Y coords (no control-points)
   * @param prev point to append to
   */
  LinePoint( float x0, float y0, LinePoint prev )
  {
    super( x0, y0 );
    // x = x0;
    // y = y0;
    has_cp = false;
    mNext = null;
    if ( prev != null ) prev.mNext = this;
    mPrev = prev;
  }

  /** make a linepoint and append to another one
   * @param x10  first control-points X coords
   * @param y10  first control-points Y coords
   * @param x20  second control-points X coords
   * @param y20  second control-points Y coords
   * @param x0   linepoint X coords 
   * @param y0   linepoint Y coords 
   * @param prev    point to append to
   */
  LinePoint( float x10, float y10, float x20, float y20, float x0, float y0, LinePoint prev )
  {
    super( x0, y0 );
    // x  = x0;
    // y  = y0;
    x1 = x10;
    y1 = y10;
    x2 = x20;
    y2 = y20;
    has_cp = true;
    mNext = null;
    if ( prev != null ) prev.mNext = this;
    mPrev = prev;
  }

  /** @return the distance of the first control-point from a given (X,Y) coord-pair
   * @param x0  X coordinate
   * @param y0  Y coordinate
   */
  float distanceCP1( float x0, float y0 )
  {
    return (float)Math.sqrt( (x0-x1)*(x0-x1) + (y0-y1)*(y0-y1) );
  }

  /** @return the distance of the second control-point from a given (X,Y) coord-pair
   * @param x0  X coordinate
   * @param y0  Y coordinate
   */
  float distanceCP2( float x0, float y0 )
  {
    return (float)Math.sqrt( (x0-x2)*(x0-x2) + (y0-y2)*(y0-y2) );
  }

  /** @return line-coord of the projection of this point on the segment P0-P1
   * @param p0   first endpoint of the segment
   * @param p1   second endpoint of the segment
   */
  float orthoProject( LinePoint p0, LinePoint p1 )
  {
    float x01 = p1.x - p0.x;
    float y01 = p1.y - p0.y;
    return ((x-p0.x)*x01 + (y-p0.y)*y01) / ( x01*x01 + y01*y01 );
  }
    
  /** @return orthogonal distance of this point on the segment P0-P1
   * @param p0   first endpoint of the segment
   * @param p1   second endpoint of the segment
   */
  float orthoDistance( LinePoint p0, LinePoint p1 )
  {
    float x01 = p1.x - p0.x;
    float y01 = p1.y - p0.y;
    return TDMath.abs( (x-p0.x)*y01 - (y-p0.y)*x01 ) / TDMath.sqrt( x01*x01 + y01*y01 );
  }

  /** write this line-point in therion format
   * @param pw   output writer
   */
  public void toTherion( PrintWriter pw )
  {
      if ( has_cp ) {
        pw.format(Locale.US, "  %.2f %.2f %.2f %.2f %.2f %.2f\n",
          x1*TDSetting.mToTherion, -y1*TDSetting.mToTherion,
          x2*TDSetting.mToTherion, -y2*TDSetting.mToTherion,
          x*TDSetting.mToTherion,  -y*TDSetting.mToTherion );
      } else {
        pw.format(Locale.US, "  %.2f %.2f\n", x*TDSetting.mToTherion, -y*TDSetting.mToTherion );
      }
  }

  /** write this line-point in tdr (binary) format
   * @param dos   output writer
   */
  void toDataStream( DataOutputStream dos )
  {
    try {
      dos.writeFloat( x  );
      dos.writeFloat( y  );
      if ( has_cp ) {
        dos.write( 1 );
        dos.writeFloat( x1 );
        dos.writeFloat( y1 );
        dos.writeFloat( x2 );
        dos.writeFloat( y2 );
        // TDLog.v( "Pt " + x + " " + y + " " + x1 + " " + y1 + " " + x2 + " " + y2 );
      } else {
        // TDLog.v( "Pt " + x + " " + y );
        dos.write( 0 );
      }
    } catch ( IOException e ) {
      TDLog.Error( e.getMessage() );
    }
  }

  /** write this line-point in Cave3D format
   * @param pw      output writer
   * @param type   plot type
   * @param cmd    drawing item manager
   * @param num    data reduction - used to get the 3D vector of this line-point
   */
  void toCave3D( PrintWriter pw, int type, DrawingCommandManager cmd, TDNum num )
  {
    // x: east, y:south, v:down
    // cx, cy are in pixels divide by 20 to write coords in meters
    float x0 = DrawingUtil.sceneToWorldX( x, y );
    float y0 = DrawingUtil.sceneToWorldY( x, y );
    float v0 = 0;
    TDVector vv = cmd.getCave3Dv( x, y, num );
    if ( type == PlotType.PLOT_PLAN ) {
      v0 = vv.z;
    } else {
      v0 = y0;
      x0 = vv.x;
      y0 = vv.y;
    }
    pw.format( Locale.US, "%f %f %f\n", x0, -y0, -v0 );
  }

  /** write this line-point in Cave3D format
   * @param pw      output writer
   * @param type   plot type
   * @param V1     X unit axis lifted in 3D
   * @param V2     Y unit axis lifted in 3D
   */
  void toCave3D( PrintWriter pw, int type, TDVector V1, TDVector V2 )
  {
    // cx,cy are in pixels divide by 20 to write coords in meters
    TDVector vv = DrawingPath.getCave3D( x, y, V1, V2 );
    // pw.format( Locale.US, "%f %f %f\n", vv.x, -vv.y, -vv.z );
    pw.format( Locale.US, "%f %f %f\n", vv.x, vv.y, -vv.z );
  }

  /** @return debug string (x,y)
   */
  String toDebugString() { return String.format(Locale.US, "<%.0f %.0f>", x, y ); }


  /** @return area of the CCW triangle
   * @paramm p1 first vertex
   * @paramm p2 second vertex
   * @paramm p3 third vertex
   */
  static float area( LinePoint p1, LinePoint p2, LinePoint p3 )
  {
    return p1.x*p2.y + p2.x*p3.y + p3.x*p1.y - p1.y*p2.x - p2.y*p3.x - p3.y*p1.x;
  }

}
