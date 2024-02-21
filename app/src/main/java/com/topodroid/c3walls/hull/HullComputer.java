/** @file HullComputer.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D simple Hull computer
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.c3walls.hull;

import com.topodroid.TDX.Cave3DShot;
import com.topodroid.TDX.Cave3DStation;
// import com.topodroid.TDX.Vector3D;
import com.topodroid.TDX.Triangle3D;
import com.topodroid.TDX.TglParser;
import com.topodroid.TDX.WallComputer;

import java.util.ArrayList;

public class HullComputer implements WallComputer
{
  private TglParser mParser;
  private ArrayList< Cave3DShot > shots;
  private ArrayList< Triangle3D > triangles = null;

  /** cstr
   * @param parser    model parser
   * @param s         model shots
   */
  public HullComputer( TglParser parser, ArrayList< Cave3DShot > s )
  {
    mParser = parser;
    shots = s;
  }

  /** @return the list of triangles
   */
  public ArrayList< Triangle3D > getTriangles() { return triangles; }

  final int[] colors = { 0xffff0000, 0xffffff00, 0xff00ff00, 0xff00ffff, 0xff0000ff, 0xffff00ff };

  /** compute the walls model
   */
  public boolean computeWalls()
  {
    ArrayList< Cave3DHull > hulls = new ArrayList<>();
    int k_col = 0;
    for ( Cave3DShot sh : shots ) {
      Cave3DStation sf = sh.from_station;
      Cave3DStation st = sh.to_station;
      if ( sf != null && st != null ) {
        ArrayList< Cave3DShot > legs1   = mParser.getLegsAtExcept( sf, st );
        ArrayList< Cave3DShot > legs2   = mParser.getLegsAtExcept( st, sf );
        HullAngle af = computeAngles( sf, legs1, sh, +1 );
        HullAngle at = computeAngles( st, legs2, sh, -1 );
        ArrayList< Cave3DShot > splays1 = mParser.getSplayAt( sf, false );
        ArrayList< Cave3DShot > splays2 = mParser.getSplayAt( st, false );
        Cave3DHull hull = new Cave3DHull( sh, splays1, splays2, sf, st, af, at );
        hull.color = colors[ k_col ];
        k_col = ( k_col + 1) % 6;
        if ( hull.size()> 0 ) hulls.add( hull );
      }
    }

    triangles = new ArrayList< Triangle3D >();
    for ( Cave3DHull h : hulls ) {
      triangles.addAll( h.triangles ); // for ( Triangle3D t : h.triangles ) triangles.add( t );
    }
    return ( triangles.size() > 0 );
  }

  // Angle = (a1, a2) are the angles (off shot.bearing) of the projection planes
  // For a splay if splay.ber is [ ber1, ber2 ] project on plane at shot.ber+a1
  //             otherwise project on plane at shot.ber-a2
  private HullAngle computeAngles( Cave3DStation st, ArrayList< Cave3DShot > legs, Cave3DShot sh, int dir )
  {
    double ber1 = ( dir == 1 )? sh.ber : Math.PI + sh.ber;
    if ( ber1 >= 2*Math.PI ) ber1 -= 2*Math.PI;
    if ( legs.size() == 0 ) {
      return new HullAngle( st, ber1, ber1+Math.PI/2, ber1-Math.PI/2 );
    }
    HullAngle ret = new HullAngle( st, ber1 );
    for ( Cave3DShot shot : legs ) {
      if ( Math.abs(shot.ber - sh.ber) < 0.01 ) continue;
      ret.update( shot.ber );
    }
    ret.makeNormals();
    return ret;
  }
      

}
