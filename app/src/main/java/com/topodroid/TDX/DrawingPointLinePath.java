/* @file DrawingPointLinePath.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid drawing: line of points
 *
 * The area border (line) path id DrawingPath.mPath
 *
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.utils.TDMath;
import com.topodroid.utils.TDLog;
import com.topodroid.math.Point2D;
import com.topodroid.math.BezierCurve;
import com.topodroid.prefs.TDSetting;

import android.graphics.Path;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.util.Locale;

public class DrawingPointLinePath extends DrawingPath
                                  implements IDrawingLink
{
  private boolean mVisible; // visible line
  private boolean mClosed;  // closed path
  // ArrayList< LinePoint > mPoints;      // points (scene coordinates)
  private LinePoint mPrevPoint = null; // previous point while constructing the line
  protected LinePoint mFirst;  // first point of the path
  protected LinePoint mLast;   // last point of the path
  private int mSize;           // number of points of the path

  float mDx, mDy; // unit vector in the direction of this line


  /** cstr
   * @param path_type  type of the path (DRAWING_PATH_LINE or DRAWING_PATH_AREA)
   * @param visible    whether the path is visible
   * @param closed     whether the path is closed
   * @param scrap      scrap the pat belongs to
   */
  public DrawingPointLinePath( int path_type, boolean visible, boolean closed, int scrap ) // TH2EDIT package
  {
    super( path_type, null, scrap ); // DrawingPath.DRAWING_PATH_AREA );
    // mPoints  = new ArrayList<>();
    clear();
    mVisible = visible;
    mClosed  = closed;
    mFirst   = null;
    mLast    = null;
    mSize    = 0;
    mDx = mDy = 0;
  }

  /** path size
   * @return the number of points of the path
   */
  public int size() { return mSize; }

  /** first point of the path
   * @return the first point of the path
   * @note access to mFirst mLast is used only by Selection
   */

  public LinePoint first() { return mFirst; }
  /** last point of the path
   * @return the last point of the path
   */
  public LinePoint last()  { return mLast; }

  /** check if a point is not the first nor the last point of the path
   * @param lp   line point
   * @return true if the line point is not an endpoint of the path
   */
  boolean isNotEndpoint( LinePoint lp ) { return lp != mFirst && lp != mLast; }

  // FIXME-COPYPATH
  // @Override
  // DrawingPath copyPath()
  // {
  //   DrawingPointLinePath ret = new DrawingPointLinePath( mType, mVisible, mClosed, mScrap );
  //   copyTo( ret );
  //   return ret;
  // }
  //
  // @Override
  // protected void copyTo( DrawingPath p )
  // {
  //   DrawingPointLinePath path = (DrawingPointLinePath)p;
  //   super.copyTo( path );
  //   path.append( this );
  // }

  /** DEBUG: counts how many points this line overlaps with another line
   * @param other   another line path
   * @return the number of points that are "close" to a point of the other path
   */
  int overlap( DrawingPointLinePath other ) 
  {
    int ret = 0;
    for (LinePoint l1 = mFirst; l1 != null; l1=l1.mNext ) {
      for ( LinePoint l2 = other.mFirst; l2 != null; l2 = l2.mNext ) {
        if ( Math.abs( l1.x - l2.x ) < 0.001f && Math.abs( l1.y - l2.y ) < 0.001f ) {
          ++ ret;
          break;
        }
      }
    }
    return ret;
  }

  /** X-coord of the last point
   * @return X-coord of the last point
   * @note implements IDrawingLink
   */
  public float getLinkX( ) { return mLast.x; }

  /** Y-coord of the last point
   * @return Y-coord of the last point
   * @note implements IDrawingLink
   */
  public float getLinkY( ) { return mLast.y; }
  // public Point2D getLink() { return mLast; }

  /** X coordinate of the "center"
   * @return mid-X of the bounding box
   */
  @Override
  float getX() { return (left+right)/2; }

  /** Y coordinate of the "center"
   * @return mid-Y of the bounding box
   */
  @Override
  float getY() { return (top+bottom)/2; }

  void dump( String msg ) // DEBUG
  {
    int k=0;
    for (LinePoint l1 = mFirst; l1 != null; l1=l1.mNext ) {
      TDLog.v( msg + " " + k + ": " + l1.x + " " + l1.y );
      ++k;
    }
  }

  /** shift the path point
   * @param dx   shift of X-coord
   * @param dy   shift of Y-coord
   */
  @Override
  void shiftBy( float dx, float dy )
  {
    left   += dx;
    right  += dx;
    top    += dy;
    bottom += dy;
    x1 += dx;
    y1 += dy;
    x2 += dx;
    y2 += dy;
    cx += dx;
    cy += dy;
    for ( LinePoint lp = mFirst; lp != null; lp = lp.mNext ) lp.shiftBy( dx, dy );
    retracePath();
  }

  /** move the first point to the specified position
   * @param x   X-coord
   * @param y   Y-coord
   */
  void moveFirstTo( float x, float y )
  {
    mFirst.x = x;
    mFirst.y = y;
    retracePath();
  }
    
  /** move the last point to the specified position
   * @param x   X-coord
   * @param y   Y-coord
   */
  void moveLastTo( float x, float y )
  {
    mLast.x = x;
    mLast.y = y;
    retracePath();
  }

  /** flip the path horizontally
   * @param z   actually unused
   */
  // from ICanvasCommand
  @Override
  public void flipXAxis( float z )
  {
    super.flipXAxis(z);
    for ( LinePoint lp = mFirst; lp != null; lp = lp.mNext ) lp.flipXAxis(z);
    retracePath();
  }

  /** count the number of points of the path
   */
  void recount() // throws Exception
  {
    if ( mFirst == null ) {
      mSize = 0;
      mLast = null;
      return;
    }
    mSize = 1;
    if ( mFirst.mPrev != null ) { // make sure mFirst has no prev
      mFirst.mPrev.mNext = null;
      mFirst.mPrev = null;
    }
    // THIS SHOULD NOT BE NECESSARY
    if ( mLast != null && mLast.mNext != null ) { // make sure mLast has no next
      if ( mLast.mNext.mPrev == mLast ) mLast.mNext.mPrev = null;
      mLast.mNext = null;
    }

    // THIS SHOULD NOT BE NECESSARY
    // LinePoint prev = mFirst;
    for ( LinePoint p = mFirst.mNext; p != null; p = p.mNext ) {
      // THIS SHOULD NOT BE NECESSARY
      // p.mPrev = prev;
      // prev = p;
      ++ mSize;
      if ( p.mNext == mFirst ) p.mNext = null; // make sure we don't come back to mFirst
    }
    // THIS SHOULD NOT BE NECESSARY
    // mLast = prev;

    // CHECK;
    // int size = 1;
    // for ( LinePoint p = mLast.mPrev; p != null && p != mLast; p = p.mPrev ) {
    //   // TDLog.v( "[<] " + p.x + " " + p.y );
    //   ++ size;
    // }
    // if ( size != mSize ) {
    //   TDLog.Error( "recount size mismatch " + mSize + " " + size );
    //   // throw new Exception("size mismatch");
    // }
  }

  /** "rotate" the path from landscape-presentation to portrait presentation
   */
  void landscapeToPortrait()
  {
    for ( LinePoint lp = mFirst; lp != null; lp = lp.mNext ) lp.landscapeToPortrait();
    retracePath();
  }

  /** set the closed attribute
   * @param closed   new value of the closed attribute
   */
  public void setClosed( boolean closed )  // TH2EDIT package
  { 
    if ( closed != mClosed ) {
      mClosed = closed; 
      retracePath();
    }
  }

  /** check if the path is closed
   * @return true if the path is closed
   */
  public boolean isClosed() { return mClosed; }

  /** set the visible attribute
   * @param visible   new value of the visible attribute
   */
  void setVisible( boolean visible ) { mVisible = visible; }

  /** check if the path is visible
   * @return true if the path is visible
   */
  public boolean isVisible() { return mVisible; }

  /** compute the unit normal to the path - by default there is no normal
   */
  void computeUnitNormal() { mDx = mDy = 0; }

  /** unlink a line_point
   * @param lp line point to unlink
   */
  void remove( LinePoint lp )
  {
    if ( lp == mFirst ) {
      mFirst = lp.mNext;
    } else if ( lp.mPrev != null ) {
      lp.mPrev.mNext = lp.mNext;
    }
    if ( lp == mLast ) {
      mLast = lp.mPrev;
    } else if ( lp.mNext != null ) {
      lp.mNext.mPrev = lp.mPrev;
    }
    lp.mNext = null;
    lp.mPrev = null;
    -- mSize;
    retracePath();
    computeUnitNormal();
  }

  /** get the line point after a given line point
   * @param lp  given line point
   * @return the line point after the given line point, or null
   * @note not static because DrawingAreaPath
   */
  LinePoint next( LinePoint lp )
  {
    if ( lp == null ) return null;
    return lp.mNext;
  }

  /** get the line point before a given line point
   * @param lp  given line point
   * @return the line point before the given line point, or null
   * @note not static because DrawingAreaPath
   */
  LinePoint prev( LinePoint lp )
  {
    if ( lp == null ) return null;
    return lp.mPrev;
  }

  /** clear the path
   */
  private void clear()
  {
    mFirst = null;
    mLast  = null;
    mPath = new Path();
    mSize = 0;
    left   = 0;
    right  = 0;
    top    = 0;
    bottom = 0;
    mDx = 0;
    mDy = 0;
  }

  /** make the path "sharp" (no control points)
   */
  void makeSharp( )
  {
    // FIXME this was here: retracePath();
    for ( LinePoint lp = mFirst; lp != null; lp = lp.mNext ) {
      lp.has_cp = false;
    }
    retracePath();
  }

  /** decimate the path points
   * @param decimation   log-decimation (must be at least 1)
   *                     1: keep one point every 2
   *                     2: keep one point every 4
   * @param min_size     2: line, 3: area
   */
  void makeReduce( int decimation, int min_size )
  {
    while ( decimation > 0 && mSize > min_size ) {
      int size = 1;  // keep first point 
      LinePoint prev = mFirst;
      LinePoint pt = prev.mNext;
      while ( pt != mLast && pt != null ) {
        LinePoint next = pt.mNext; // pt.mNext != null because pt < mLast
        prev.mNext = next;
        prev = next;
        if ( next == null ) { 
          TDLog.Error("Line reduce. Something went wrong: null next at size " + size + " Interrupt");
          pt = mLast;
          decimation = 0; // no more decimation
        } else {
          next.mPrev = prev; 
          pt = prev.mNext;
          ++ size;
        } 
      }
      if ( pt == mLast ) ++ size; // for the mLast point
      mSize = size;     
      -- decimation;
    }
    if ( mSize < min_size ) {
      throw new RuntimeException("PointLine makeReduce: small final size " + mSize );
    } 
    retracePath();
  }

  /** make the path "rock"-like
   */
  void makeRock()
  {
    if ( mSize > 2 ) {
      int size = 1;
      LinePoint prev = mFirst;
      LinePoint pt = mFirst.mNext;
      while ( pt != mLast ) {
        LinePoint next = pt.mNext; // pt.mNext != null because pt < mLast
        float x1 = pt.x - prev.x;
        float y1 = pt.y - prev.y;
        float x2 = next.x - pt.x;
        float y2 = next.y - pt.y;
        float cos = (x1*x2 + y1*y2)/(float)Math.sqrt((x1*x1+y1*y1)*(x2*x2+y2*y2));
        if ( cos >= TDSetting.mReduceCosine ) {
          prev.mNext = next;
          next.mPrev = prev; 
        } else {
          ++ size;
          prev = pt;
        }
        pt = next;
      }
      ++ size; // for the mLast point
      mSize = size;     
    }    
    retracePath();
  }

  /** make the path close
   */
  void makeClose( )
  {
    if ( mSize > 2 ) {
      float dx = (mFirst.x - mLast.x)/3;
      float dy = (mFirst.y - mLast.y)/3;
      if ( dx*dx + dy*dy > 1.0e-7 ) {
        if ( mLast.has_cp ) {
          mLast.x1 += dx;
          mLast.y1 += dy;
          mLast.x2 += dx*2;
          mLast.y2 += dy*2;
        }
        mLast.x = mFirst.x;
        mLast.y = mFirst.y;
        retracePath();
      }
    }    
  }

  /** make the path a straight-segment joining the first and the last points
   */
  public void makeStraight( ) // TH2EDIT package
  {
    // if ( mPoints.size() < 2 ) return;
    // LinePoint first = mPoints.get( 0 );
    // LinePoint last  = mPoints.get( mPoints.size() - 1 );
    if ( mSize < 2 ) return;
    LinePoint first = mFirst;
    LinePoint last  = mLast;

    clear();
    addStartPoint( first.x, first.y );
    addPoint( last.x, last.y );
    computeUnitNormal();
    // TDLog.v( "make straight final size " + mPoints.size() );
  }
  
  /** add the starting point
   * @param x   X-coord
   * @param y   Y-coord
   */
  public void addStartPoint( float x, float y ) 
  {
    // mPrevPoint = new LinePoint(x,y, null);
    // mPoints.add( mPrevPoint );
    mLast = new LinePoint(x,y, null);
    ++ mSize;
    mFirst = mLast;
    mPath.moveTo( x, y );
    left = right  = x;
    top  = bottom = y;
  }

  /** add a point (without control-points)
   * @param x   X-coord
   * @param y   Y-coord
   */
  public void addPoint( float x, float y ) 
  {
    if ( Float.isNaN(x) || Float.isNaN(y) ) return;
    if ( mFirst == null ) {
      addStartPoint( x, y );
    } else {
      // mPrevPoint = new LinePoint(x,y,mPrevPoint);
      // mPoints.add( mPrevPoint );
      mLast = new LinePoint(x, y, mLast);
      ++ mSize;
      mPath.lineTo( x, y );
      if ( x < left ) { left = x; } else if ( x > right  ) { right  = x; }
      if ( y < top  ) { top  = y; } else if ( y > bottom ) { bottom = y; }
    }
  }

  /** add a point with control points, ie, prev-point .. CP1 .. CP2 .. point
   * @param x1  X-coord of the first control-point
   * @param y1  Y-coord of the first control-point
   * @param x2  X-coord of the second control-point
   * @param y2  Y-coord of the second control-point
   * @param x   X-coord
   * @param y   Y-coord
   */
  public void addPoint3( float x1, float y1, float x2, float y2, float x, float y )  // TH2EDIT package
  {
    if ( Float.isNaN(x) || Float.isNaN(y) ) return;
    if ( mFirst == null ) {
      addStartPoint( x, y );
    } else {
      if ( Float.isNaN( x1 ) || Float.isNaN( y1 ) || Float.isNaN( x2 ) || Float.isNaN( y2 ) ) {
        mLast = new LinePoint(x, y, mLast);
        ++ mSize;
        mPath.lineTo( x, y );
      } else {
        // mPrevPoint = new LinePoint( x1,y1, x2,y2, x,y, mPrevPoint );
        // mPoints.add( mPrevPoint );
        mLast = new LinePoint( x1,y1, x2,y2, x,y, mLast );
        ++mSize;
        mPath.cubicTo( x1,y1, x2,y2, x,y );
      }
      if ( x < left ) { left = x; } else if ( x > right  ) { right  = x; }
      if ( y < top  ) { top  = y; } else if ( y > bottom ) { bottom = y; }
    }
  }

  // ----------------------------------------------
  /** add the starting point without tracing the path
   * @param x   X-coord
   * @param y   Y-coord
   */
  void addStartPointNoPath( float x, float y ) 
  {
    mFirst = mLast = new LinePoint(x,y, null);
    ++ mSize;
    left = right  = x;
    top  = bottom = y;
  }

  /** add a point without tracing the path
   * @param x   X-coord
   * @param y   Y-coord
   */
  void addPointNoPath( float x, float y ) 
  {
    mLast = new LinePoint(x, y, mLast);
    ++ mSize;
    if ( x < left ) { left = x; } else if ( x > right  ) { right  = x; }
    if ( y < top  ) { top  = y; } else if ( y > bottom ) { bottom = y; }
  }

  /** add a point with control-points without tracing the path
   * @param x1  X-coord of the first control-point
   * @param y1  Y-coord of the first control-point
   * @param x2  X-coord of the second control-point
   * @param y2  Y-coord of the second control-point
   * @param x   X-coord
   * @param y   Y-coord
   */
  void addPoint3NoPath( float x1, float y1, float x2, float y2, float x, float y ) 
  {
    mLast = new LinePoint( x1,y1, x2,y2, x,y, mLast );
    ++mSize;
    if ( x < left ) { left = x; } else if ( x > right  ) { right  = x; }
    if ( y < top  ) { top  = y; } else if ( y > bottom ) { bottom = y; }
  }
  // ----------------------------------------------

  // /** append another path to this path - UNUSED
  //  * @param line   path to append
  //  */
  // void append( DrawingPointLinePath line )
  // {
  //   if ( line.mSize ==  0 ) return;
  //   LinePoint lp = line.mFirst;
  //   addPoint( lp.x, lp.y );
  //   for ( lp = lp.mNext; lp != null; lp = lp.mNext ) {
  //     if ( lp.has_cp ) {
  //       addPoint3( lp.x1, lp.y1, lp.x2, lp.y2, lp.x, lp.y );
  //     } else {
  //       addPoint( lp.x, lp.y );
  //     }
  //   }
  //   // computeUnitNormal();
  // }

  /** reset the path from the given list of points
   * @param pts   list of points
   */
  void resetPath( ArrayList< LinePoint > pts )
  {
    clear();
    // if ( pts == null ) return;
    int size = pts.size();
    if ( size <= 1 ) return;
    LinePoint lp = pts.get(0);
    addStartPoint( lp.x, lp.y );
    for ( int k=1; k<size; ++k ) {
      lp = pts.get(k);
      if ( lp.has_cp ) {
        addPoint3( lp.x1, lp.y1, lp.x2, lp.y2, lp.x, lp.y );
      } else {
        addPoint( lp.x, lp.y );
      }
    }
    computeUnitNormal();
  }
     
  /** insert a point after another
   * @param x   X-coord of the new point
   * @param y   Y-coord of the new point
   * @param lp  point after which to insert the new point
   * @return the newly inserted point
   */
  LinePoint insertPointAfter( float x, float y, LinePoint lp )
  {
    if ( Float.isNaN(x) || Float.isNaN(y) ) return null;
    // int index = mPoints.indexOf(lp);
    // if ( index < mPoints.size() ) ++index; // insert before next point
    LinePoint next = lp.mNext;
    LinePoint pp = new LinePoint(x, y, lp );
    ++mSize;
    pp.mNext = next;
    if ( next != null ) next.mPrev = pp;
    // mPoints.add(index, pp);
    retracePath();
    return pp;
  }

  /** compute te size of the path
   */
  void recomputeSize()
  {
    // assert( mFirst.mPrev == null );
    // assert( mLast.mNext == null );
    mSize = 0;
    for ( LinePoint lp = mFirst; lp != null; lp = lp.mNext ) ++ mSize;
  }

  /** retrace the path
   */
  void retracePath()
  {
    // int size = mPoints.size();
    // if ( size == 0 ) return;
    // mPath = new Path();
    // LinePoint lp = mPoints.get(0);
    // mPath.moveTo( lp.x, lp.y );
    // for ( int k=1; k<size; ++k ) {
    //   lp = mPoints.get(k);
    //   if ( lp.has_cp ) {
    //     mPath.cubicTo( lp.x1, lp.y1, lp.x2, lp.y2, lp.x, lp.y );
    //   } else {
    //     mPath.lineTo( lp.x, lp.y );
    //   }
    // }
    if ( mSize == 0 ) return;
    mPath = new Path();
    LinePoint lp  = mFirst;
    if ( lp == null ) { // should not happen but it did 20201227
      mSize = 0;
      return;
    }
    left = right  = lp.x;
    top  = bottom = lp.y;
    mPath.moveTo( lp.x, lp.y );
    for ( lp = lp.mNext; lp != null && lp != mFirst; lp = lp.mNext ) {
      if ( lp.has_cp ) {
        mPath.cubicTo( lp.x1, lp.y1, lp.x2, lp.y2, lp.x, lp.y );
      } else {
        mPath.lineTo( lp.x, lp.y );
      }
      if ( lp.x < left ) { left = lp.x; } else if ( lp.x > right  ) { right  = lp.x; }
      if ( lp.y < top  ) { top  = lp.y; } else if ( lp.y > bottom ) { bottom = lp.y; }
      // if ( lp == mLast ) break; // FIXME-AREA-SNAP mLast should have mNext ====  null
    }
    if ( mClosed ) mPath.close();
    computeUnitNormal();
  }

  /** reverse the pointline path 
   * @note the drawing path is not retraced
   */
  void reverse()
  {
    if ( mSize == 0 ) return;
    LinePoint lf = mFirst;
    LinePoint ll = mLast;
    clear();
    // mPath = new Path();
    // mFirst = null;
    // mLast  = null;
    LinePoint lp = ll;
    addStartPoint( lp.x, lp.y );
    LinePoint prev = lp.mPrev;
    while ( prev != null ) {
      if ( lp.has_cp ) {
        addPoint3( lp.x2, lp.y2, lp.x1, lp.y1, prev.x, prev.y );
      } else {
        addPoint( prev.x, prev.y );
      }
      lp = prev;
      prev = prev.mPrev;
    }
    // if ( mClosed ) mPath.close();
    // computeUnitNormal(); // FIXME 
  }

  /** compute the distance from the path to a point
   * @param x   X-coord of the point
   * @param y   Y-coord of the point
   * @return the distance from the path to the point
   */
  @Override
  float distanceToPoint( float x, float y )
  {
    if ( Float.isNaN(x) || Float.isNaN(y) ) return Float.NaN;
    float dist = 10000000f; // FIXME
    // for ( LinePoint pt : mPoints ) 
    for ( LinePoint pt=mFirst; pt != null; pt = pt.mNext ) 
    {
      float dx = x - pt.x;
      float dy = y - pt.y;
      float d2 = dx*dx + dy*dy;
      if ( d2 < dist ) dist = d2;
    }
    return (dist > 0 )? TDMath.sqrt(dist) : 0;
  }

  /** close the path
   */
  void closePath() 
  {
    mPath.close();
    // TDLog.v( "area close path" );
  }

  // ArrayList< LinePoint > getPoints() { return mPoints; }

  // int size() { return mPoints.size(); }

  // @Override
  // void draw( Canvas canvas )
  // {
  //   super.draw( canvas );
  //   // Path path = new Path();
  //   // path.addCircle( 0, 0, 1, Path.Direction.CCW );
  //   // for ( LinePoint lp : mPoints ) {
  //   //   Path path1 = new Path( path );
  //   //   path1.offset( lp.x, lp.y );
  //   //   canvas.drawPath( path1, BrushManager.highlightPaint );
  //   // }
  // }

  // @Override
  // void draw( Canvas canvas, Matrix matrix )
  // {
  //   super.draw( canvas, matrix );
  //   // Path path = new Path();
  //   // path.addCircle( 0, 0, 1, Path.Direction.CCW );
  //   // for ( LinePoint lp : mPoints ) {
  //   //   Path path1 = new Path( path );
  //   //   path1.offset( lp.x, lp.y );
  //   //   path1.transform( matrix );
  //   //   canvas.drawPath( path1, BrushManager.highlightPaint );
  //   // }
  // }

  /** export the path in therion format
   * @param pw    export writer
   * @param close whether to close the exported line
   */
  protected void toTherionPoints( PrintWriter pw, boolean close )
  {
    // TDLog.v( "Th2: rectF X " + left + " " + right + " Y " + bottom + " " + top );
    // for ( LinePoint pt : mPoints ) 
    LinePoint pt = mFirst; 
    pt.toTherion( pw );
    float x0 = DrawingUtil.sceneToWorldX( pt );
    float y0 = DrawingUtil.sceneToWorldY( pt );
    float delta = 0;
    // TDLog.v( "X " + x0 + " Y " + y0 );
    // if ( mLineType == BrushManager.mLineLib.mLineSectionIndex && size() > 2 ) pt = pt.mNext; // skip first point (tick)
    for ( pt = pt.mNext; pt != null; pt = pt.mNext ) {
      float x3 = DrawingUtil.sceneToWorldX( pt );
      float y3 = DrawingUtil.sceneToWorldY( pt );
      if ( pt.has_cp ) { 
        pt.toTherion( pw );
	delta = 0;
        // TDLog.v( "X " + x3 + " Y " + y3 + " with CP " );
      } else {
	delta += TDMath.sqrt( (x3-x0)*(x3-x0) + (y3-y0)*(y3-y0) );
        // TDLog.v( "X " + x3 + " Y " + y3 + " Delta " + delta );
	if ( delta > TDSetting.mBezierStep ) {
          pt.toTherion( pw );
	  delta = 0;
	}
      }
      x0 = x3;
      y0 = y3;
    }
    if ( close ) { // insert start point again if closed
      float dx = mLast.x - mFirst.x;
      float dy = mLast.y - mFirst.y;
      if ( dx*dx + dy*dy > 1.0e-7 ) {
        mFirst.toTherion( pw );
      }
    }
  }

  /** export the path in cSurvey format
   * @param pw    export writer
   * @param close whether to close the exported line
   * @param reversed whether to export the point from the last to the first
   */
  protected void toCsurveyPoints( PrintWriter pw, boolean close, boolean reversed )
  {
    float bezier_step = TDSetting.getBezierStep();
    pw.format("            <points data=\"");
    // boolean b = true;
    float x3, y3;
    float x0, y0, x1, y1, x2, y2;
    // for ( LinePoint pt : mPoints ) 
    if ( ! reversed ) {
      // NOTE do not skip tick-point if want to save section with tick
      // if ( mLineType == BrushManager.mLineLib.mLineSectionIndex && size() > 2 ) pt = pt.mNext; // skip first point (tick)
      LinePoint pt = mFirst; 
      x0 = DrawingUtil.sceneToWorldX( pt.x, pt.y );
      y0 = DrawingUtil.sceneToWorldY( pt.x, pt.y );
      pw.format(Locale.US, "%.2f %.2f ", x0, y0 );
      // TDLog.v( "X " + x0 + " Y " + y0 );
      // if ( b ) {
        pw.format("B ");
        //  b = false;
      // }
      for ( pt = pt.mNext; pt != null; pt = pt.mNext ) 
      {
        x3 = DrawingUtil.sceneToWorldX( pt.x, pt.y );
        y3 = DrawingUtil.sceneToWorldY( pt.x, pt.y );
	if ( pt.has_cp ) {
          x1 = DrawingUtil.sceneToWorldX( pt.x1, pt.y1 );
          y1 = DrawingUtil.sceneToWorldY( pt.x1, pt.y1 );
          x2 = DrawingUtil.sceneToWorldX( pt.x2, pt.y2 );
          y2 = DrawingUtil.sceneToWorldY( pt.x2, pt.y2 );
	  float len = (x1-x0)*(x1-x0) + (x2-x1)*(x2-x1) + (x3-x2)*(x3-x2) + (x3-x0)*(x3-x0)
	            + (y1-y0)*(y1-y0) + (y2-y1)*(y2-y1) + (y3-y2)*(y3-y2) + (y3-y0)*(y3-y0);
	  int np = (int)( TDMath.sqrt( len ) * bezier_step + 0.5f );
	  if ( np > 1 ) {
	    BezierCurve bc = new BezierCurve( x0, y0, x1, y1, x2, y2, x3, y3 );
	    for ( int n=1; n < np; ++n ) {
	      Point2D p = bc.evaluate( (float)n / (float)np );
              pw.format(Locale.US, "%.2f %.2f ", p.x, p.y );
              // TDLog.v( "N " + n + " X " + p.x + " Y " + p.y );
            }
	  }
	} 
        pw.format(Locale.US, "%.2f %.2f ", x3, y3 );
        // TDLog.v( "X " + x3 + " Y " + y3 );
        // if ( b ) { pw.format("B "); b = false; }
	x0 = x3;
	y0 = y3;
      }
    } else {
      LinePoint pt = mLast;
      x0 = DrawingUtil.sceneToWorldX( pt.x, pt.y );
      y0 = DrawingUtil.sceneToWorldY( pt.x, pt.y );
      pw.format(Locale.US, "%.2f %.2f ", x0, y0 );
      // TDLog.v( "X " + x0 + " Y " + y0 );
      // if ( b ) {
        pw.format("B ");
        // b = false;
      // }
      for ( pt = pt.mPrev; pt != null; pt = pt.mPrev ) 
      {
        x3 = DrawingUtil.sceneToWorldX( pt.x, pt.y );
        y3 = DrawingUtil.sceneToWorldY( pt.x, pt.y );
	if ( pt.has_cp ) {
          x1 = DrawingUtil.sceneToWorldX( pt.x2, pt.y2 );
          y1 = DrawingUtil.sceneToWorldY( pt.x2, pt.y2 );
          x2 = DrawingUtil.sceneToWorldX( pt.x1, pt.y1 );
          y2 = DrawingUtil.sceneToWorldY( pt.x1, pt.y1 );
	  float len = (x1-x0)*(x1-x0) + (x2-x1)*(x2-x1) + (x3-x2)*(x3-x2) + (x3-x0)*(x3-x0)
	            + (y1-y0)*(y1-y0) + (y2-y1)*(y2-y1) + (y3-y2)*(y3-y2) + (y3-y0)*(y3-y0);
	  int np = (int)( TDMath.sqrt( len ) * bezier_step + 0.5f );
	  if ( np > 1 ) {
	    BezierCurve bc = new BezierCurve( x0, y0, x1, y1, x2, y2, x3, y3 );
	    for ( int n=1; n < np; ++n ) {
	      Point2D p = bc.evaluate( (float)n / (float)np );
              pw.format(Locale.US, "%.2f %.2f ", p.x, p.y );
              // TDLog.v( "N " + n + " X " + p.x + " Y " + p.y );
	    }
	  }
	}
        pw.format(Locale.US, "%.2f %.2f ", x3, y3 );
        // TDLog.v( "X " + x3 + " Y " + y3 );
        // if ( b ) { pw.format("B "); b = false; }
	x0 = x3;
	y0 = y3;
      }
    }
    if ( close ) { // insert start point again if closed
      LinePoint pt = mFirst; 
      x0 = DrawingUtil.sceneToWorldX( pt.x, pt.y );
      y0 = DrawingUtil.sceneToWorldY( pt.x, pt.y );
      pw.format(Locale.US, "%.2f %.2f ", x0, y0 );
    }

    pw.format("\" />\n");
  }

  String toDebugString()
  {
    return String.format(Locale.US, "<%.0f %.0f>-<%.0f %.0f>", mFirst.x, mFirst.y, mLast.x, mLast.y );
  }

  /** rotate the line segment to have an orientation multiple of the given bin
   * @param bin   angle range bin - must be even and divisor of 90
   * @note used to clamp the section lines in profile views to a set of fixed angles
   */
  void clampOrientation( float bin )
  {
    if ( mSize != 2 ) {
      // TDLog.v("clamp orientation nr. points " + mSize );
      return;
    }
    float cx = (mFirst.x + mLast.x)/2;
    float cy = (mFirst.y + mLast.y)/2;
    float dx = (mLast.x - mFirst.x)/2;
    float dy = (mLast.y - mFirst.y)/2;
    
    float a = TDMath.atan2d( dy, dx );
    if ( a > 90 ) {
      a = a - 180;
    } else if ( a < -90 ) {
      a = a + 180;
    }
    float ia = bin * (int)((bin/2 + TDMath.abs(a))/bin);
    float da = ( (a < 0 )? -ia - a : ia - a ) * TDMath.DEG2RAD;
    // rotate by da = ia-a
    float ca = TDMath.cos( da );
    float sa = TDMath.sin( da );
    // TDLog.v("clamp a " + a + " ia " + ia + " da " + (da*TDMath.RAD2DEG) );
    mLast.x  = cx + dx * ca - dy * sa;
    mLast.y  = cy + dx * sa + dy * ca;
    mFirst.x = cx - dx * ca + dy * sa;
    mFirst.y = cy - dx * sa - dy * ca;
    retracePath();
  }

  // /** @return true if the line path is counterclockwise - UNUSED
  //  */
  // boolean isCCW()
  // {
  //   if ( mSize < 3 ) return false;
  //   double angle = 0;
  //   LinePoint lp1 = mLast;
  //   LinePoint lp2 = mFirst;
  //   LinePoint lp3 = lp2.mNext;
  //   Point2D q1 = lp2.sub( lp1 ); 
  //   while ( lp3 != null ) {
  //     Point2D q2 = lp3.sub( lp2 );
  //     double s = q1.cross( q2 );
  //     double c = q1.dot( q2 );
  //     angle += Math.atan2( s, c );
  //     lp2 = lp3;
  //     q1  = q2;
  //     lp3 = lp3.mNext;
  //   }
  //   TDLog.v("CCW angle " + angle );
  //   return angle > 0;
  // }

  /** link firtt and last point, thus closing the chain of line points
   * @note used to try and continue areas
   */
  void chainFirstLast()
  {
    mFirst.mPrev = mLast;
    mLast.mNext  = mFirst;
  }

  /** reset the first/last line-points - unlink first from its prev and last from its next
   * @param lp1   new first point
   * @param lp2   new last point
   * @note used to try and continue areas
   *
   *     ... <--> lp1p <--> lp1 <--> ... ... <--> lp2 <--> lp2n <--> ...
   * (1)     <--> lp1p     first <-> ...
   * (2)                                     <--> last     lp2p <--> ...
   */
  void resetFirstLast( LinePoint lp1, LinePoint lp2 )
  {
    setFirst( lp1 );
    setLast( lp2 );
  }

  void setFirst( LinePoint lp1 ) 
  {
    if ( lp1 != mFirst ) { // 20230518 added test
      mFirst = lp1; // (1)
      if ( mFirst.mPrev != null ) mFirst.mPrev.mNext = null;
      mFirst.mPrev = null;
    }
  }

  void setLast( LinePoint lp2 )
  {
    if ( lp2 != mLast ) { // 20230518 added test
      mLast = lp2; // (2)
      if ( mLast.mNext != null ) mLast.mNext.mPrev = null;
      mLast.mNext = null;
    }
  }

  /** @return the index of a point in the line - DEBUG
   * @param lp0   line point
   */
  int indexOf( LinePoint lp0 )
  {
    int n = 0;
    for ( LinePoint lp = mFirst; lp != mLast; lp=lp.mNext ) {
      if ( lp == lp0 ) return n;
      ++ n;
    }
    return n;
  }

}

