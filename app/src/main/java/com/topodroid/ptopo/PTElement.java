/* @file PTElement.java
 *
 * @author marco corvi
 * @date march 2010
 *
 * @brief PocketTopo file IO
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.ptopo;

import java.io.InputStream;
import java.io.OutputStream;

public class PTElement
{
  static final byte ID_NO_ELEMENT       = (byte)0; // pocket topo elements id
  static final byte ID_POLYGON_ELEMENT  = (byte)1;
  static final byte ID_XSECTION_ELEMENT = (byte)3;

  byte _id;    //!< id of the element

  PTElement( byte id ) 
  {
    _id = id;
  }

  byte id() { return _id; }

  void read( InputStream fs ) 
  {
  }

  void write( OutputStream fs ) 
  { 
  }
  
  // void print( ) { }

  void printTherion( OutputStream fp, int x0, int y0, int scale, 
                     String[] points,
                     String[] lines )
  {
  }


  void xtherionBounds( int x0, int y0, int scale,
                       float xmin, float ymin,
                       float xmax, float ymax ) 
  { 
  }

}
