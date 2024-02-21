/* @file DLNPole.java
 *
 * @author marco corvi
 * @date june 2017
 *
 * @brief TopoDroid Delaunay site pole
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.dln;

import com.topodroid.math.Point2D;

class DLNPole 
{
  Point2D mP;
  DLNTriangle mT;
  float mDist;

  DLNPole( Point2D p, DLNTriangle t, float d )
  {
    mP = p;
    mT = t;
    mDist = d;
  }

  void set ( Point2D p, DLNTriangle t, float d )
  {
    mP = p;
    mT = t;
    mDist = d;
  }
}
