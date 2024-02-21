/* @file Weeder.java
 *
 * @author marco corvi
 * @date june 2012
 *
 * @brief TopoDroid point strings (lines) simplifier (point weeding algorithm)
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDMath;
import com.topodroid.math.Point2D;
import com.topodroid.prefs.TDSetting;

import java.util.List;
import java.util.ArrayList;

class Weeder
{
  /** point 2D with the line-abscissa
   */
  static class WeedPoint extends Point2D
  {
    float s;  // line abscissa
    WeedPoint( float xx, float yy ) { super(xx,yy); }
  }
  
  /** check whether two segments crosses - allow a buffer to both, buffers are in units of segments lengths
   * @param p1   first point of first segment
   * @param p2   second point of first segment
   * @param q1   first point of second segment
   * @param q2   second point of second segment
   * @param bp   buffer of first segment
   * @param bq   buffer of second segment
   *
   * p1 + s * (p2 - p1) = q1 + t * (q2 - q1)
   */
  static boolean cross( WeedPoint p1, WeedPoint p2, WeedPoint q1, WeedPoint q2, float bp, float bq )
  {
    float px = p2.x - p1.x;
    float py = p2.y - p1.y;
    float qx = q2.x - q1.x;
    float qy = q2.y - q1.y;
  
    float den = qx*py - qy*px;
    if ( den == 0 ) return false;
    float t = ((p1.x-q1.x)*py - (p1.y-q1.y)*px)/den;
    if ( t < -bq || t > 1+bq ) return false;
    float s = ((p1.x-q1.x)*qy - (p1.y-q1.y)*qx)/den;
    if ( s < -bp || s > 1+bp ) return false;
    return true;
  }
  
  /** 2D line
   * basically used only to compute the farthest point
   */
  class WeedSegment
  { 
    float a, b, c;       // line-segment coefficients: a * X + b * Y + c = 0, normalized: a^2 + b^2 = 1
    WeedPoint pt1, pt2;  // endpoints
    float dx, dy;
    float prj;
  
    // LineWeed( float aa, float bb, float cc ) : a(aa), b(bb), c(cc) { reduce(); }
  
    /** cstr
     * @param p1   first point
     * @param p2   second point
     */
    WeedSegment( WeedPoint p1, WeedPoint p2 )
    {
      pt1 = p1;
      pt2 = p2;
      dx = p2.x - p1.x;
      dy = p2.y - p1.y;
      float d = dx*dx + dy*dy;
      dx /= d;
      dy /= d;

      a =   p2.y - p1.y;
      b = - p2.x + p1.x;
      c = - p1.y * b - p1.x * a; // = p1.x * p2.y - p1.y * p2.x
      d = TDMath.sqrt( a*a + b*b );
      a /= d;
      b /= d;
      c /= d;
    }
  
    /** @return the distance of a point from the segment:
     *   | P - P1 | if the projection of P on the line P1-P2 is on the side of P1
     *   | P - P2 | if it is on the side of P2
     *   d( P, P1-P2) if it is inside
     */
    float distance( WeedPoint p ) 
    {
      prj = (p.x-pt1.x)*dx + (p.y-pt1.y)*dy; // project P on the segment ("inside if in [0,1])
      if ( prj < 0.0f ) return pt1.distance( p );
      if ( prj > 1.0f ) return pt2.distance( p );
      return TDMath.abs( a*p.x + b*p.y + c );
    }
  }
  
  /** forward linked list of points
   */
  class WeedIndex
  {
    int k;
    WeedPoint p;    // point
    WeedIndex next; // next index
  
    /** cstr
     * @param pt    point
     * @param kk    ...
     * @param pp    previous index, or null
     * @param nn    next index
     */
    WeedIndex( WeedPoint pt, int kk, WeedIndex pp, WeedIndex nn ) 
    {
      p = pt;
      k = kk;
      next = nn;
      if ( pp != null ) pp.next = this;
    }
  }
  // ------------------------------------------------------------------

  ArrayList< WeedPoint > mPoints;

  /** cstr
   */
  Weeder()
  {
    mPoints = new ArrayList< WeedPoint >();
  }

  /** add a point
   * @param x   point X coord
   * @param y   point Y coord
   */
  void addPoint( float x, float y ) { mPoints.add( new WeedPoint(x,y) ); }

  /** simplify the set of points
   * @param max_dist maximum distance from segment for point suppression
   * @param max_len  maximum section length
   * @return simplified list of points
   */
  ArrayList< Point2D > simplify( float max_dist, float max_len )
  {
    ArrayList< Point2D > ret = new ArrayList<>();
    if ( mPoints.size() < 4 ) {
      for ( WeedPoint wp : mPoints ) ret.add( new Point2D( wp.x, wp.y ) );
      return ret;
    }
    initLineAbscissa( mPoints );
    WeedIndex idx = initSections( mPoints, max_len );
  
    // simplify within each section
    WeedIndex i1 = idx;
    while ( i1.next != null ) {
      WeedIndex i2 = i1.next;
      simplifySection( mPoints, i1, i2, max_dist ); 
      i1 = i2;
    }
    while ( idx != null ) {
      ret.add( new Point2D( idx.p.x, idx.p.y ) );
      idx = idx.next;
    }
    return ret;
  }

  // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  /** initialize the line-abscissa of the points
   * @param pts   list of points
   */
  private void initLineAbscissa( List< WeedPoint > pts )
  {
    WeedPoint p = pts.get( 0 );
    p.s = 0;
    int k2 = pts.size();
    for ( int k = 1; k < k2; ++k ) {
      WeedPoint pn = pts.get(k);
      pn.s = p.s + p.distance( pn );
      p = pn;
    }
  }
  
  /** @return index of farthest point between two points
   * @param pts   list of points
   * @param i1    first point
   * @param i2    second point
   * @param thr   minimum acceptable distance - return null if distance is too small
   */
  private WeedIndex farthestPoint( List< WeedPoint > pts, WeedIndex i1, WeedIndex i2, float thr )
  {
    int k1 = i1.k;
    int k2 = i2.k;
    if ( k1+1 >= k2 ) return null;
    WeedSegment ll = new WeedSegment( pts.get(k1), pts.get(k2) );
    float d0 = 0;
    int k0 = k1;
    for ( int k=k1+1; k<k2; ++k ) {
      float d = ll.distance( pts.get(k) );
      if ( d > d0 ) { d0 = d; k0 = k; }
    }
    if ( d0 < thr ) return null;
    return new WeedIndex( pts.get(k0), k0, i1, i2 );
  }
  
  /** initialize the sections of points
   * @param pts   list of points
   * @param max_len ...
   * @return index of ...
   */
  private WeedIndex initSections( List< WeedPoint > pts, float max_len )
  {
    initLineAbscissa( pts );
  
    int k1 = 0;
    int k2 = pts.size() - 1;
    WeedIndex idx2 = new WeedIndex( pts.get(k2), k2, null, null );
    WeedIndex idx1 = new WeedIndex( pts.get(k1), k1, null, idx2 );
    WeedIndex idx0 = idx1;
  
    // intersections
    WeedPoint p1 = pts.get(k1); // first point of the segment
    // WeedPoint p2 = pts[k2];
    float len = 0.001f;
    WeedPoint q0 = p1; // prev point - used for the length of the curve portion
    for ( int k=1; k < k2; ++ k ) {
      WeedPoint pp = pts.get(k); // running point to find the second point of the segment
      len += pp.s - q0.s;
      if ( len > max_len ) {
        idx1 = new WeedIndex( pp, k, idx1, idx2 );
        p1   = pp;
	len = 0;
	q0   = p1;
      } else {
        q0 = pp;
        WeedPoint q1 = pts.get(k+1);
        boolean crosses = false;
        for ( int kk = k+2; kk<k2; ++kk ) {
          WeedPoint q2 = pts.get(kk);
          if ( cross( p1, pp, q1, q2, TDSetting.mWeedBuffer/len, TDSetting.mWeedBuffer/(0.001f + q2.s-q1.s)  ) ) { crosses = true; break; }
	  q1 = q2;
        }
        if  ( crosses ) {
          idx1 = new WeedIndex( pp, k, idx1, idx2 );
          p1   = pp;
        }
      }
    }
    return idx0;
  }
  
  /** simplify a section of points
   * @param pts   list of points
   * @param i1    first point of the section
   * @param i2    second point of the section
   * @param thr   minimum acceptable distance - return null if distance is too small
   */
  private void simplifySection( List< WeedPoint > pts, WeedIndex i1, WeedIndex i2, float thr )
  {
    if ( i1.k + 1 >= i2.k ) return;
    while ( i1 != i2 ) {
      if ( farthestPoint( pts, i1, i1.next, thr ) == null ) {
        i1 = i1.next;
      }
    }
  }
}


