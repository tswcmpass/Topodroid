/* @file PTMapping.java
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

// import com.topodroid.utils.TDLog;

import java.io.InputStream;
import java.io.OutputStream;

public class PTMapping
{
  static final private int XTHERION_FACTOR = 25;

    private PTPoint _origin;
    private int _scale;   //!< scale 50 .. 20000

    PTMapping()
    {
      _scale = XTHERION_FACTOR;
      _origin = new PTPoint();
    }

    // -----------------------------------------------------

    public PTPoint origin() { return _origin; }

    private void setOrigin( int x, int y ) { _origin.set( x, y ); }

    public int scale() { return _scale; }
    void setScale( int s )
    {
      if ( s >= 50 && s <= 20000 ) _scale = s;
    }

    void read( InputStream fs )
    {
      _origin.read( fs );
      _scale = PTFile.readInt( fs );
      // TDLog.Log( TDLog.LOG_PTOPO, "PT Mapping origin " + _origin._x + " " + _origin._y + " scale " + _scale );
    }

    void write( OutputStream fs )
    {
      _origin.write( fs );
      PTFile.writeInt( fs, _scale );
    }

    // void print( ) { TDLog.v( "PT mapping: scale " + _scale ); _origin.print(); }

    void clear()
    {
      setOrigin( 0, 0 );
      _scale = XTHERION_FACTOR;
    }
}

