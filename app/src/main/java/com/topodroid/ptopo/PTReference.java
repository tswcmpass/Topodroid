/* @file PTReference.java
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


class PTReference
{
    private PTId _station;
    private long _east;     //!< east coordinate [mm]
    private long _north;    //!< north coordinate [mm]
    private int  _h_geo;    //!< geoid altitude [mm]
    private PTString _comment;

    PTReference()
    {
      _station = new PTId();
      _east = 0L;
      _north = 0L;
      _h_geo = 0;
      _comment = new PTString();
    }

    PTReference( String id, long east, long north, int h_geo, String comment )
    {
      _station = new PTId();
      _station.set( id );
      _east = east;
      _north = north;
      _h_geo = h_geo;
      _comment = new PTString( comment );
    }

    // ---------------------------------------------------

    PTId station() { return _station; }

    long east() { return _east; }
    long north() { return _north; }
    int altitude() { return _h_geo; }

    void setEast( long east ) { _east = east; }
    void setNorth( long north ) { _north = north; }
    void setAltitude( int h_geo ) { _h_geo = h_geo; }

    void set( int e, int n, int a ) 
    {
      _east = e;
      _north = n;
      _h_geo = a;
    }

    String comment() { return _comment.value(); }
    void setComment( String str ) { _comment.set( str ); }

    // ---------------------------------------------------


    void read( InputStream fs )
    {
      _station.read( fs );
      _east  = PTFile.readLong( fs );
      _north = PTFile.readLong( fs );
      _h_geo = PTFile.readInt( fs );
      _comment.read( fs );
    }

    void write( OutputStream fs )
    {
      _station.write( fs );
      PTFile.writeLong( fs, _east );
      PTFile.writeLong( fs, _north );
      PTFile.writeInt( fs, _h_geo );
      _comment.write( fs );
    }

    // void print( )
    // { 
    //   TDLog.v( "reference: east " + _east + " north " + _north + " h_geo " + _h_geo );
    //   _station.print();
    //   _comment.print();
    // }

/*
void 
PTreference::printTherion( FILE * fp )
{
  std::string station = _station.toString();
  fprintf(fp, "    fix %s %8.2f %8.2f %8.2f\n",
    station.c_str(), _east/1000.0, _north/1000.0, _h_geo/1000.0 );
  if ( _comment.size() > 0 ) {
    fprintf(fp, "    # %s\n", _comment.value() );
  }
}
*/

}
