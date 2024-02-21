/* @file ProjectionCommandManager.java
 *
 * @author marco corvi
 * @date mar 2016
 *
 * @brief TopoDroid projected profile azimuth: commands manager
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.utils.TDLog;
// import com.topodroid.math.Point2D;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

class ProjectionCommandManager
{
  private static final int BORDER = 20;

  private RectF mBBox = null;

  // private List< DrawingPath >    mGridStack1;
  // private List< DrawingPath >    mGridStack10;
  // private List< DrawingPath >    mGridStack100;

  private final List< DrawingPath >        mLegsStack;
  private final List< DrawingPath >        mSplaysStack;
  private final List< DrawingStationName > mStations;  // survey stations

  private boolean mDisplayPoints;

  private Matrix mMatrix;
  private float  mScale; // current zoom: value of 1 pl in scene space

  List< DrawingPath >        getLegs()         { return mLegsStack;    }
  List< DrawingPath >        getSplays()       { return mSplaysStack;  }
  List< DrawingStationName > getStations()     { return mStations;     } 


  // private void flipXAxes( List< DrawingPath > paths )
  // {
  //   final float z = 1/mScale;
  //   final Iterator i1 = paths.iterator();
  //   while ( i1.hasNext() ){
  //     final DrawingPath drawingPath = (DrawingPath) i1.next();
  //     drawingPath.flipXAxis( z );
  //   }
  // }

  // from ICanvasCommand
  // public void flipXAxis(float z)
  // {
  //   synchronized( mGridStack1 ) {
  //     flipXAxes( mGridStack1 );
  //     flipXAxes( mGridStack10 );
  //     flipXAxes( mGridStack100 );
  //   }
  //   synchronized( TDPath.mShotsLock ) {
  //     flipXAxes( mLegsStack );
  //     flipXAxes( mSplaysStack );
  //   }
  //   synchronized( TDPath.mStationsLock ) {
  //     for ( DrawingStationName st : mStations ) {
  //       st.flipXAxis(z);
  //     }
  //   }
  // }

  /** cstr
   */
  ProjectionCommandManager()
  {
    // mGridStack1   = Collections.synchronizedList(new ArrayList< DrawingPath >());
    // mGridStack10  = Collections.synchronizedList(new ArrayList< DrawingPath >());
    // mGridStack100 = Collections.synchronizedList(new ArrayList< DrawingPath >());
    mLegsStack    = Collections.synchronizedList(new ArrayList< DrawingPath >());
    mSplaysStack  = Collections.synchronizedList(new ArrayList< DrawingPath >());
    mStations     = Collections.synchronizedList(new ArrayList< DrawingStationName >());
    mMatrix = new Matrix(); // identity
  }

  /** @return the set of shot paths that intersect a segment
   * @param p1  segment first endpoint
   * @param p2  segment second endpoint
   */
  List< DrawingPath > getIntersectionShot( LinePoint p1, LinePoint p2 )
  {
    List< DrawingPath > ret = new ArrayList<>();
    for ( DrawingPath p : mLegsStack ) {
      if ( p.mType == DrawingPath.DRAWING_PATH_FIXED ) {
        if ( p.intersectSegment( p1.x, p1.y, p2.x, p2.y ) >= 0 ) {
          ret.add( p );
        }
      }
    }
    return ret;
  }

  // DrawingStationName getStationAt( float x, float y, float size ) // x,y canvas coords
  // {
  //   // TDLog.v( "get station at " + x + " " + y );
  //   for ( DrawingStationName st : mStations ) {
  //     // TDLog.v( "station at " + st.cx + " " + st.cy );
  //     if ( Math.abs( x - st.cx ) < size && Math.abs( y - st.cy ) < size ) return st;
  //   }
  //   return null;
  // }

  // void setDisplayPoints( boolean display ) { mDisplayPoints = display; }

  // boolean isSelectable() { return mSelection != null; }

  void clearReferences()
  {
    // synchronized( mGridStack1 ) {
    //   mGridStack1.clear();
    //   mGridStack10.clear();
    //   mGridStack100.clear();
    // }
    synchronized( TDPath.mShotsLock ) {
      mLegsStack.clear();
      mSplaysStack.clear();
    }
    synchronized( TDPath.mStationsLock ) {
      mStations.clear();
    }
  }

  void clearDrawing()
  {
    // mGridStack1.clear();
    // mGridStack10.clear();
    // mGridStack100.clear();
    mLegsStack.clear();
    mSplaysStack.clear();
    mStations.clear();
  }

  /** set the transformation matrix
   * @param dx   X translation
   * @param dy   Y translation
   * @param s    scaling
   * 
   * x' = (x + dx) * s
   * y' = (y + dy) * s
   */
  void setTransform( float dx, float dy, float s )
  {
    mMatrix = new Matrix();
    mMatrix.postTranslate( dx, dy );
    mMatrix.postScale( s, s );
    mScale  = 1 / s;
    // mBBox.left   = - dx;      // scene coords
    // mBBox.right  = mScale * TopoDroidApp.mDisplayWidth - dx; 
    // mBBox.top    = - dy;
    // mBBox.bottom = mScale * TopoDroidApp.mDisplayHeight - dy;
  }

  // ------------------------------------------------
  void addLegPath( DrawingPath path )
  { 
    if ( mLegsStack == null ) return;
    synchronized( TDPath.mShotsLock ) {
      mLegsStack.add( path );
    }
  }  

  void addSplayPath( DrawingPath path )
  {
    if ( mSplaysStack == null ) return;
    synchronized( TDPath.mShotsLock ) {
      mSplaysStack.add( path );
    }
  }  
  
  // this is empty but it is called by ProjectionSurface
  void addGrid( DrawingPath path, int k )
  { 
    // if ( mGridStack1 == null ) return;
    // synchronized( mGridStack1 ) {
    //   switch (k) {
    //     case 1:   mGridStack1.add( path );   break;
    //     case 10:  mGridStack10.add( path );  break;
    //     case 100: mGridStack100.add( path ); break;
    //   }
    // }
  }

  void executeAll( Canvas canvas )
  {
    if ( canvas == null ) {
      TDLog.Error( "drawing executeAll null canvas");
      return;
    }

    // if ( mGridStack1 != null ) {
    //   synchronized( mGridStack1 ) {
    //     if ( mScale < 1 ) {
    //       final Iterator i1 = mGridStack1.iterator();
    //       while ( i1.hasNext() ){
    //         final DrawingPath drawingPath = (DrawingPath) i1.next();
    //         drawingPath.draw( canvas, mMatrix, mScale, mBBox );
    //       }
    //     }
    //     if ( mScale < 10 ) {
    //       final Iterator i10 = mGridStack10.iterator();
    //       while ( i10.hasNext() ){
    //         final DrawingPath drawingPath = (DrawingPath) i10.next();
    //         drawingPath.draw( canvas, mMatrix, mScale, mBBox );
    //       }
    //     }
    //     final Iterator i100 = mGridStack100.iterator();
    //     while ( i100.hasNext() ){
    //       final DrawingPath drawingPath = (DrawingPath) i100.next();
    //       drawingPath.draw( canvas, mMatrix, mScale, mBBox );
    //     }
    //   }
    // }

    synchronized( TDPath.mShotsLock ) {
      if ( mLegsStack != null ) {
        for ( DrawingPath path : mLegsStack ) {
          // path.draw( canvas, mMatrix, mScale, mBBox );
          path.draw( canvas, mMatrix, mBBox );
        }
      }
      if ( mSplaysStack != null ) {
        for ( DrawingPath path : mSplaysStack ) {
          // path.draw( canvas, mMatrix, mScale, mBBox );
          path.draw( canvas, mMatrix, mBBox );
        }
      }
    }
 
    if ( mStations != null ) {  
      synchronized( TDPath.mStationsLock ) {
        for ( DrawingStationName st : mStations ) {
          // st.draw( canvas, mMatrix, mScale, mBBox );
          st.draw( canvas, mMatrix, mBBox );
        }
      }
    }
  }

  // called by DrawingSurface::addDrawingStationName
  void addStation( DrawingStationName st )
  {
    synchronized( TDPath.mStationsLock ) {
      mStations.add( st );
    }
  }

}
