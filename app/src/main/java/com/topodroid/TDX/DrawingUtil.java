/* @file DrawingUtil.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid drawing utilities
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 *  @note this is actually DrawingUtilPortrait.java as the Landscape is never used
 *        and it is made all static (state-less)
 */
package com.topodroid.TDX;

import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDMath;
import com.topodroid.prefs.TDSetting;
import com.topodroid.math.Point2D;

import android.graphics.Paint;
import android.graphics.Path;

public class DrawingUtil
{
  public static final float SCALE_FIX = 20.0f; 
  public static final float CENTER_X = 100f;
  public static final float CENTER_Y = 120f;

  // private static final PointF mCenter = new PointF( CENTER_X, CENTER_Y );

  /** @return scene X-coord
   * @param p    point (x,y) [world]
   */
  public static float toSceneX( Point2D p ) { return CENTER_X + p.x * SCALE_FIX; }

  /** @return scene Y-coord
   * @param p    point (x,y) [world]
   */
  public static float toSceneY( Point2D p ) { return CENTER_Y + p.y * SCALE_FIX; }

  /** @return world X-coord
   * @param p    point (x,y) [scene]
   */
  public static float sceneToWorldX( Point2D p ) { return (p.x - CENTER_X) / SCALE_FIX; }

  /** @return world Y-coord
   * @param p    point (x,y) [scene]
   */
  public static float sceneToWorldY( Point2D p ) { return (p.y - CENTER_Y) / SCALE_FIX; }

  // float toSceneX( float x, float y ) { return x; } 
  // float toSceneY( float x, float y ) { return y; } 

  /** @return scene X-coord
   * @param x    X coord [world]
   * @param y    Y coord [world] (unused)
   */
  public static float toSceneX( double x, double y ) { return (float)(CENTER_X + x * SCALE_FIX); }

  /** @return scene Y-coord
   * @param x    X coord [world] (unused)
   * @param y    Y coord [world]
   */
  public static float toSceneY( double x, double y ) { return (float)(CENTER_Y + y * SCALE_FIX); }

  // public static float toSceneX( float x, float y ) { return (CENTER_X + x * SCALE_FIX); }
  // public static float toSceneY( float x, float y ) { return (CENTER_Y + y * SCALE_FIX); }

  // float sceneToWorldX( float x, float y ) { return x; } 
  // float sceneToWorldY( float x, float y ) { return y; }

  /** @return world X-coord
   * @param x    X coord [scene]
   * @param y    Y coord [scene] (unused)
   */
  public static float sceneToWorldX( double x, double y ) { return (float)((x - CENTER_X)/SCALE_FIX); }

  /** @return world Y-coord
   * @param x    X coord [scene] (unused)
   * @param y    Y coord [scene]
   */
  public static float sceneToWorldY( double x, double y ) { return (float)((y - CENTER_Y)/SCALE_FIX); }

  // public static float sceneToWorldX( float x, float y ) { return ((x - CENTER_X)/SCALE_FIX); }
  // public static float sceneToWorldY( float x, float y ) { return ((y - CENTER_Y)/SCALE_FIX); }
    
  // int toBoundX( float x, float y ) { return Math.round(x); } 
  // int toBoundY( float x, float y ) { return Math.round(y); }
  // private static int toBoundX( double x, double y ) { return Math.round(x); }
  // private static int toBoundY( double x, double y ) { return Math.round(y); }
  private static int toBoundX( float x, float y ) { return Math.round(x); }
  private static int toBoundY( float x, float y ) { return Math.round(y); }

  /** make a splay path
   * @param dpath   splay path
   * @param xx1     first point X coord [world]
   * @param yy1     first point Y coord [world]
   * @param xx2     second point X coord [world]
   * @param yy2     second point Y coord [world]
   */
  static
  void makeDrawingSplayPath( DrawingSplayPath dpath, float xx1, float yy1, float xx2, float yy2 )
  {
    float x1 = toSceneX( xx1, yy1 );
    float y1 = toSceneY( xx1, yy1 );
    float x2 = toSceneX( xx2, yy2 );
    float y2 = toSceneY( xx2, yy2 );
    dpath.setEndPoints( x1, y1, x2, y2 ); // this sets the midpoint only
    dpath.makePath( x1, y1, x2, y2 );
  }

  /** make a straight path
   * @param dpath   path
   * @param xx1     first point X coord [world]
   * @param yy1     first point Y coord [world]
   * @param xx2     second point X coord [world]
   * @param yy2     second point Y coord [world]
   */
  static
  void makeDrawingPath( DrawingPath dpath, float xx1, float yy1, float xx2, float yy2 )
  {
    float x1 = toSceneX( xx1, yy1 );
    float y1 = toSceneY( xx1, yy1 );
    float x2 = toSceneX( xx2, yy2 );
    float y2 = toSceneY( xx2, yy2 );
    dpath.setEndPoints( x1, y1, x2, y2 ); // this sets the midpoint only
    dpath.makePath( x1, y1, x2, y2 ); // this sets the midpoint only
  }

  /** make a straight path with an angle (for the North arrow)
   * ie the path makes a counterclockwise angle if the declination is positive, and clockwise if it is negative
   * @param dpath   path
   * @param xx1     first point X coord [world]
   * @param yy1     first point Y coord [world]
   * @param xx2     second point X coord [world]
   * @param yy2     second point Y coord [world]
   * @param decl    angle (declination) [degrees]
   * @note used only for H-xsection North line
   */
  static
  void makeDrawingPathWithAngle( DrawingPath dpath, float xx1, float yy1, float xx2, float yy2, float decl )
  {
    float x1 = toSceneX( xx1, yy1 );
    float y1 = toSceneY( xx1, yy1 );
    float x2 = toSceneX( xx2, yy2 );
    float y2 = toSceneY( xx2, yy2 );
    float c = TDMath.cosd( decl );
    float s = TDMath.sind( decl );
    float xx3 = (xx1 - xx2)*2;
    float yy3 = (yy1 - yy2)*2;
    float xx4 = xx2 + xx3 * c + yy3 * s;
    float yy4 = yy2 - xx3 * s + yy3 * c;
    float x4 = toSceneX( xx4, yy4 );
    float y4 = toSceneY( xx4, yy4 );
    TDLog.v("North P1 " + x1 + " " + y1 + " P2 " + x2 + " " + y2 + " P3 " + x4 + " " + y4 );
    
    dpath.setEndPoints( x1, y1, x2, y2 ); // this sets the midpoint only
    dpath.updateBounds( x4, y4 );
    dpath.makePath( x2, y2, x1, y1, x4, y4 ); // this sets the midpoint only
  }

  /** make a straight path
   * @param dpath   path
   * @param xx1     first point X coord [world]
   * @param yy1     first point Y coord [world]
   * @param xx2     second point X coord [world]
   * @param yy2     second point Y coord [world]
   * @param xoff    X offset [scene] 
   * @param yoff    Y offset [scene] 
   */
  static
  void makeDrawingPath( DrawingPath dpath, float xx1, float yy1, float xx2, float yy2, float xoff, float yoff )
  {
    float x1 = toSceneX( xx1, yy1 );
    float y1 = toSceneY( xx1, yy1 );
    float x2 = toSceneX( xx2, yy2 );
    float y2 = toSceneY( xx2, yy2 );
    dpath.setEndPoints( x1, y1, x2, y2 ); // this sets the midpoint only
    dpath.makePath( x1 - xoff, y1 - yoff, x2 - xoff, y2 - yoff );
  }

  /** make a straight path
   * @param z       grid line index - gray-color goes with the index 
   * @param x1      first point X coord [scene]
   * @param y1      first point Y coord [scene]
   * @param x2      second point X coord [scene]
   * @param y2      second point Y coord [scene]
   * @param surface drawing surface
   */
  private static void addGridLine( int z, float x1, float x2, float y1, float y2, DrawingSurface surface )
  { 
    // TDLog.v("add grid-line Z " + z + " X " + x1 + " " + x2 + " Y " + y1 + " " + y2 );
    DrawingPath dpath = new DrawingPath( DrawingPath.DRAWING_PATH_GRID, null, -1 );
    int k = 1;
    Paint paint = BrushManager.fixedGridPaint;
    if ( Math.abs( z % 100 ) == 0 ) {
      k = 100;
      paint = BrushManager.fixedGrid100Paint;
    } else if ( Math.abs( z % 10 ) == 0 ) {
      k = 10;
      paint = BrushManager.fixedGrid10Paint;
    }
    dpath.setPathPaint( paint );
    dpath.mPath  = new Path();
    dpath.mPath.moveTo( x1, y1 );
    dpath.mPath.lineTo( x2, y2 );
    dpath.setBBox( x1, x2, y1, y2 );
    dpath.x1 = x1; // endpoints
    dpath.y1 = y1; // endpoints
    dpath.x2 = x2; // endpoints
    dpath.y2 = y2; // endpoints
    surface.addGridPath( dpath, k );
  }

  /** make a straight path - used for plan/profile
   * @param xmin    first point X coord [world]
   * @param xmax    second point X coord [world]
   * @param ymin    first point Y coord [world]
   * @param ymax    second point Y coord [world]
   * @param surface drawing surface
   */
  static
  void addGrid( float xmin, float xmax, float ymin, float ymax, DrawingSurface surface )
  {
    // TDLog.v("add grid min/max X " + xmin + " " + xmax + " Y " + ymin + " " + ymax );
    if ( xmin > xmax ) { float x = xmin; xmin = xmax; xmax = x; }
    if ( ymin > ymax ) { float y = ymin; ymin = ymax; ymax = y; }
    xmin = (xmin - 100.0f) / TDSetting.mUnitGrid;
    xmax = (xmax + 100.0f) / TDSetting.mUnitGrid;
    ymin = (ymin - 100.0f) / TDSetting.mUnitGrid;
    ymax = (ymax + 100.0f) / TDSetting.mUnitGrid;
    float x1 = toSceneX( xmin, ymin );
    float y1 = toSceneY( xmin, ymin );
    float x2 = toSceneX( xmax, ymax );
    float y2 = toSceneY( xmax, ymax );
    if ( x1 > x2 ) { float x = x1; x1 = x2; x2 = x; } // important for the bbox culling
    if ( y1 > y2 ) { float y = y1; y1 = y2; y2 = y; }
    // mDrawingSurface.setBounds( toSceneX( xmin ), toSceneX( xmax ), toSceneY( ymin ), toSceneY( ymax ) );

    int xx1 = toBoundX( xmin, ymin );
    int yy1 = toBoundY( xmin, ymin );
    int xx2 = toBoundX( xmax, ymax );
    int yy2 = toBoundY( xmax, ymax );
    if ( xx1 > xx2 ) { int x = xx1; xx1 = xx2; xx2 = x; }
    if ( yy1 > yy2 ) { int y = yy1; yy1 = yy2; yy2 = y; }

    DrawingPath dpath = null;
    for ( int x = xx1; x <= xx2; x += 1 ) {
      float x0 = x * TDSetting.mUnitGrid;
      x0 = toSceneX( x0, x0 );
      addGridLine( x, x0, x0, y1, y2, surface );
    }
    for ( int y = yy1; y <= yy2; y += 1 ) {
      float y0 = y * TDSetting.mUnitGrid;
      y0 = toSceneY( y0, y0 );
      addGridLine( y, x1, x2, y0, y0, surface );
    }
  }

  /** make a straight path - used for x-sections
   * @param xmin    first point X coord [world]
   * @param xmax    second point X coord [world]
   * @param ymin    first point Y coord [world]
   * @param ymax    second point Y coord [world]
   * @param xoff    X offset [scene]
   * @param yoff    Y offset [scene]
   * @param surface drawing surface
   */
  static void addGrid( float xmin, float xmax, float ymin, float ymax, float xoff, float yoff, DrawingSurface surface )
  {
    // TDLog.v("add grid min/max X " + xmin + " " + xmax + " Y " + ymin + " " + ymax + " off " + xoff + " " + yoff );
    if ( xmin > xmax ) { float x = xmin; xmin = xmax; xmax = x; }
    if ( ymin > ymax ) { float y = ymin; ymin = ymax; ymax = y; }
    xmin = (xmin - 100.0f) / TDSetting.mUnitGrid;
    xmax = (xmax + 100.0f) / TDSetting.mUnitGrid;
    ymin = (ymin - 100.0f) / TDSetting.mUnitGrid;
    ymax = (ymax + 100.0f) / TDSetting.mUnitGrid;
    float x1 = toSceneX( xmin, ymin ) - xoff;
    float y1 = toSceneY( xmin, ymin ) - yoff;
    float x2 = toSceneX( xmax, ymax ) - xoff;
    float y2 = toSceneY( xmax, ymax ) - yoff;
    if ( x1 > x2 ) { float x = x1; x1 = x2; x2 = x; } // important for the bbox culling
    if ( y1 > y2 ) { float y = y1; y1 = y2; y2 = y; }
    // mDrawingSurface.setBounds( toSceneX( xmin ), toSceneX( xmax ), toSceneY( ymin ), toSceneY( ymax ) );
    // TDLog.v("add grid X " + x1 + " " + x2 + " Y " + y1 + " " + y2 );
    
    int xx1 = toBoundX( xmin, ymin );
    int yy1 = toBoundY( xmin, ymin );
    int xx2 = toBoundX( xmax, ymax );
    int yy2 = toBoundY( xmax, ymax );
    if ( xx1 > xx2 ) { int x = xx1; xx1 = xx2; xx2 = x; }
    if ( yy1 > yy2 ) { int y = yy1; yy1 = yy2; yy2 = y; }

    // TDLog.v("add grid Y-lines XX " + xx1 + " " + xx2 + " Y " + y1 + " " + y2 );
    DrawingPath dpath = null;
    for ( int x = xx1; x <= xx2; x += 1 ) {
      float x0 = x * TDSetting.mUnitGrid;
      x0 = toSceneX( x0, x0) - xoff;
      addGridLine( x, x0, x0, y1, y2, surface );
    }
    // TDLog.v("add grid X-lines X " + x1 + " " + x2 + " YY " + yy1 + " " + yy2 );
    for ( int y = yy1; y <= yy2; y += 1 ) { // grid-line index y
      float y0 = y * TDSetting.mUnitGrid;
      y0 = toSceneY( y0, y0 ) - yoff;
      addGridLine( y, x1, x2, y0, y0, surface );
    }
    // TDLog.v("grid sizes " + surface.getGrid1Size() + " " + surface.getGrid10Size() );
  }

  /** make a straight path - used for x-sections
   * @param xmin    first point X coord [world]
   * @param xmax    second point X coord [world]
   * @param ymin    first point Y coord [world]
   * @param ymax    second point Y coord [world]
   * @param xc      center X
   * @param yc      center Y
   * @param xoff    X offset [scene]
   * @param yoff    Y offset [scene]
   * @param surface drawing surface
   */
  static void addGrid( float xmin, float xmax, float ymin, float ymax, float xc, float yc, float xoff, float yoff, DrawingSurface surface )
  {
    // TDLog.v("add grid min/max X " + xmin + " " + xmax + " Y " + ymin + " " + ymax + " off " + xoff + " " + yoff );
    if ( xmin > xmax ) { float x = xmin; xmin = xmax; xmax = x; }
    if ( ymin > ymax ) { float y = ymin; ymin = ymax; ymax = y; }
    xmin = (xmin - 100.0f) / TDSetting.mUnitGrid;
    xmax = (xmax + 100.0f) / TDSetting.mUnitGrid;
    ymin = (ymin - 100.0f) / TDSetting.mUnitGrid;
    ymax = (ymax + 100.0f) / TDSetting.mUnitGrid;
    float x1c = toSceneX( xc+xmin, yc+ymin ) - xoff;
    float y1c = toSceneY( xc+xmin, yc+ymin ) - yoff;
    float x2c = toSceneX( xc+xmax, yc+ymax ) - xoff;
    float y2c = toSceneY( xc+xmax, yc+ymax ) - yoff;
    if ( x1c > x2c ) { float x = x1c; x1c = x2c; x2c = x; } // important for the bbox culling
    if ( y1c > y2c ) { float y = y1c; y1c = y2c; y2c = y; }
    // mDrawingSurface.setBounds( toSceneX( xmin ), toSceneX( xmax ), toSceneY( ymin ), toSceneY( ymax ) );
    // TDLog.v("add grid X " + x1c + " " + x2c + " Y " + y1c + " " + y2c );
    
    int xx1 = toBoundX( xmin, ymin );
    int yy1 = toBoundY( xmin, ymin );
    int xx2 = toBoundX( xmax, ymax );
    int yy2 = toBoundY( xmax, ymax );
    if ( xx1 > xx2 ) { int x = xx1; xx1 = xx2; xx2 = x; }
    if ( yy1 > yy2 ) { int y = yy1; yy1 = yy2; yy2 = y; }

    // TDLog.v("add grid Y-lines XX " + xx1 + " " + xx2 + " Y " + y1c + " " + y2c );
    DrawingPath dpath = null;
    for ( int x = xx1; x <= xx2; x += 1 ) {
      float x0  = x * TDSetting.mUnitGrid;
      float x0c = toSceneX( xc+x0, xc+x0) - xoff;
      addGridLine( x, x0c, x0c, y1c, y2c, surface );
    }
    // TDLog.v("add grid X-lines X " + x1c + " " + x2c + " YY " + yy1 + " " + yy2 );
    for ( int y = yy1; y <= yy2; y += 1 ) { // grid-line index y
      float y0 = y * TDSetting.mUnitGrid;
      float y0c = toSceneY( yc+y0, yc+y0 ) - yoff;
      addGridLine( y, x1c, x2c, y0c, y0c, surface );
    }
    // TDLog.v("grid sizes " + surface.getGrid1Size() + " " + surface.getGrid10Size() );
  }

  /** @return X coord corrected with the declination: x' = cos(d) * x - sin(d) * y
   * @param x   X coord [canvas]
   * @param y   Y coord [canvas]
   * @param cd  cos( declination )
   * @param sd  sin( declination )
   */
  public static double declinatedX( double x, double y, double cd, double sd ) 
  {
    return cd * ( x - DrawingUtil.CENTER_X ) - sd * ( y - DrawingUtil.CENTER_Y );
  }

  /** @return Y coord corrected with the declination: y' = sin(d) * x + cos(d) * y
   * @param x   X coord [canvas]
   * @param y   Y coord [canvas]
   * @param cd  cos( declination )
   * @param sd  sin( declination )
   */
  public static double declinatedY( double x, double y, double cd, double sd ) 
  {
    return cd * ( y - DrawingUtil.CENTER_Y ) + sd * ( x - DrawingUtil.CENTER_X );
  }

}
