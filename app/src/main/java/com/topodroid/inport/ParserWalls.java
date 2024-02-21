/* @file ParserWalls.java
 *
 * @author marco corvi
 * @date dec 2021
 *
 * @brief TopoDroid Walls parser
 *
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * ----------------------------------------------------------
 */
package com.topodroid.inport;

import com.topodroid.utils.TDMath;
import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDio;
import com.topodroid.utils.TDString;
import com.topodroid.utils.TDUtil;
// import com.topodroid.prefs.TDSetting;

// import com.topodroid.TDX.FixedInfo;
// import com.topodroid.TDX.CurrentStation;

import com.topodroid.common.LegType;

import java.io.IOException;
// import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.Locale;

// TODO this class can be made extend ImportParser
//
// units, calibrate, sd supported quantities:
//   length tape bearing compass gradient clino counter depth x y z position easting dx northing dy altitude dz

class ParserWalls extends ImportParser
{
  static class WallsFix
  {
    String station;  // fix station
    double lng, lat; // WGS84 longitude and latitude
    double h_geo;    // geoid altitude
  
    /** cstr
     * @param st    station name
     * @param _lng  longitude [dec. degrees]
     * @param _lat  latitude [dec. degrees]
     * @param _geo  geoid altitude [m]
     */
    WallsFix( String st, double _lng, double _lat, double _geo )
    {
      station = st;
      lng = _lng;
      lat = _lat;
      h_geo = _geo;
    }
  }

  ArrayList< WallsFix > mFixes;
  int mIsWGS;

  float surveyDeclination( ) { return mDeclination; }
  // ---------------------------------------------------------

  /** cstr
   * @param isr         input stream 
   * @param name        survey name
   * @param apply_declination whether to apply the declination to the azimuths
   */
  ParserWalls( InputStreamReader isr, String name, boolean apply_declination ) throws ParserException
  {
    super( apply_declination );
    mFixes = new ArrayList< WallsFix >();
    readFile( isr, name, "" );
    checkValid();
  }

  /** @return true if the given value represents a "non-exist" (starts with two '-')
   * @param val  specified value
   */
  private boolean isNonExistent( String val )
  {
    return val != null && val.startsWith("--");
  }

  /** read input file
   * @param isr      input stream
   * @param filename name of the file to parse
   * @param basepath survey pathname base
   * 
   * LRUD styles: F FROM station perpendicular [default]
   *              T To station perpendicular
   *              FB FROM station bisector - replaced by F
   *              TB TO station bisector - replaced by T
   */
  private void readFile( InputStreamReader isr, String filename, String basepath ) throws ParserException
  {
    // String path = basepath;   // survey pathname(s)

    // int jFrom    = 0; // index of FROM station
    // int jTo      = 1;
    int jLength  = 2; // index of length (DAV)
    int jCompass = 3;
    int jClino   = 4;
    int jEast    = 2; // index of east (ENU)
    int jNorth   = 3;
    int jUpward  = 4;
    int jLeft  = 0;
    int jRight = 1;
    int jUp    = 2;
    int jDown  = 3;
    boolean lrudAtTo = false;
    String from = null;
    String to   = null;
    float len, ber=0, cln;
    float ul = 1.0f; // units of length

    Pattern pattern = Pattern.compile( "\\s+" );
    StringBuffer team = new StringBuffer();
    String prefix = "";
    boolean in_comment = false;
    int extend = 1;

    Locale locale = Locale.getDefault();

    // data order DAV / ENU

    try {
      String dirname = "./";
      int i = filename.lastIndexOf('/');
      if ( i >= 0 ) {
        dirname = filename.substring(0, i+1);
        mName = filename.substring( i+1 ).toLowerCase( locale );
      } else {
        mName = filename.toLowerCase( locale );
      } 
      int j = mName.lastIndexOf(".");
      if ( j >= 0 ) mName = mName.substring( 0, j );

      // System.out.println("readFile dir " + dirname + " filename " + filename );
      // TDLog.Log( TDLog.LOG_IO, "import read Walls file <" + filename + ">" );

      BufferedReader br = TDio.getBufferedReader( isr, filename );
      String line = nextLine( br );
      while ( line != null ) {
        // TDLog.v( "Parser Walls " + state.in_survey + " " + state.in_centerline + " " + state.in_data + " : " + line );
        line = line.trim();
        int pos = line.indexOf( ';' ); // remove comment (anything after ';')
        if ( pos >= 0 ) {
          line = line.substring( 0, pos ).trim();
        }
        if ( in_comment ) {
          if ( line.startsWith("#]") ) in_comment = false;
        } else if ( line.length() > 0 ) {
          String[] vals = pattern.split(line); // line.split( "\\s+" );
          int sz = vals.length;
          if ( sz > 0 ) {
            String cmd = vals[0].toLowerCase( locale );
            if ( cmd.equals("#units" ) ) { 
              for ( int k=1; k<sz; ++k ) { 
                if ( vals[k].toLowerCase( locale ).equals("feet") ) {
                  ul = TDUtil.FT2M;
                } else if ( vals[k].toLowerCase( locale ).equals("meter") ) {
                  ul = 1;
                } else if ( vals[k].toLowerCase( locale ).startsWith("order=") ) {
                  int c = 2;
                  int kc_max = vals[k].length(); if ( kc_max > 9 ) kc_max = 9;
                  for ( int kc = 6; kc<kc_max; ++kc, ++c ) {
                    switch ( vals[k].charAt(kc) ) {
                      case 'D': jLength  = c; break;
                      case 'A': jCompass = c; break;
                      case 'V': jClino   = c; break;
                      case 'E': jEast    = c; break;
                      case 'N': jNorth   = c; break;
                      case 'U': jUpward  = c; break;
                    }
                  }
                } else if ( vals[k].toLowerCase( locale ).startsWith("decl=") ) {
                  mDeclination = Float.parseFloat( vals[k].substring(5) );
                } 
                // TODO continue
              }
            } else if ( cmd.equals("#prefix") ) {
              if ( sz > 1 ) prefix = vals[1];
            } else if ( cmd.equals("#fix") ) {
              if ( sz >= 5 ) {
                mIsWGS = 0;
                String station = prefix + vals[1];
                String lng_str = vals[2].replace("W", "-").replace("E", "+");
                String lat_str = vals[3].replace("S", "-").replace("N", "+");
                double lng = strToCoordinate( lng_str, ul );
                double lat = strToCoordinate( lat_str, ul );
                double h_geo = strToValue( vals[4], ul );
                if ( mIsWGS == 2 ) {
                  mFixes.add( new WallsFix( station, lng, lat, h_geo ) );
                }
              }
            } else if ( cmd.equals("#date") ) { // yyyy-mm-dd
              if ( sz > 1 ) mDate = vals[1];
            } else if ( cmd.startsWith("#[") ) { 
              in_comment = true;
            } else if ( cmd.startsWith("#") ) { // ignored
              // "#segment"
              // "#note"
              // "#flag"
              // "#symbol"
            } else if ( sz >= 4 ) {
              try {
                from = prefix + vals[0];
                to   = (vals[1].equals("-")) ? "" : prefix + vals[1];
                len  = Float.parseFloat( vals[jLength] ) * ul;
                ber  = Float.parseFloat( vals[jCompass] );
                cln  = (sz == 4 )? 0 : Float.parseFloat( vals[jClino] );
                if ( sz > 5 ) {
                  String[] lrud = vals[5].substring(1, vals[5].length()-1).split(",");
                  if ( lrud.length == 4 ) {
                    String station = lrudAtTo ? to : from;
                    handleLRUD( station, lrudAtTo, ber, lrud, jLeft, jRight, jUp, jDown, ul );
                  }
                }
                shots.add( new ParserShot( from, to, len, ber, cln, 0.0f, extend, LegType.NORMAL ) );
              } catch ( NumberFormatException e ) {
                TDLog.Error( "walls parser error: data " + line );
              }
            } else if ( sz >= 2 ) { // station and LRUD 
              String[] lrud = vals[1].substring(1, vals[5].length()-1).split(",");
              if ( lrud.length == 4 ) {
                String station = prefix + vals[0];
                if ( from != null && from.equals(station) ) {
                  handleLRUD( station, false, ber, lrud, jLeft, jRight, jUp, jDown, ul );
                } else if ( to != null && to.equals(station) ) {
                  handleLRUD( station, true, ber, lrud, jLeft, jRight, jUp, jDown, ul );
                }
              }
            }
          }
        }
        line = nextLine( br );
      }
      mTeam = team.toString();
    } catch ( IOException e ) {
      // TODO
      throw new ParserException();
    }
    if ( mDate == null ) {
      mDate = TDUtil.currentDate();
    }
    // TDLog.v( "Parser Walls shots "+ shots.size() + " splays "+ splays.size() +" fixes "+  mFixes.size() );
  }

  static final int extend_unset = 5;

  /** handle LRUD
   * @param station   station
   * @param atTo      whether the LRUD is at a TO station
   * @param ber       azimuth of the leg shot 
   * @param lrud      lrud as L,R,U,D
   * @param jL        index of left
   * @param jR        index of right
   * @param jU        index of up
   * @param jD        index of down
   * @param ul        current unit of length
   */
  private void handleLRUD( String station, boolean atTo, float ber, String[] lrud, int jL, int jR, int jU, int jD, float ul ) throws NumberFormatException
  {
    float b;
    float dist;
    if ( jL >= 0 && ! isNonExistent( lrud[jL] ) ) { // left
      dist = Float.parseFloat( lrud[jL] ) * ul;
      b = atTo ? TDMath.add90( ber ) : TDMath.sub90( ber );
      shots.add( new ParserShot( station, TDString.EMPTY, dist, b, 0, 0.0f, extend_unset, LegType.XSPLAY ) );
    }
    if ( jR >= 0 && ! isNonExistent( lrud[jR] ) ) {  // right
      dist = Float.parseFloat( lrud[jR] ) * ul;
      b = atTo ? TDMath.sub90( ber ) : TDMath.add90( ber );
      shots.add( new ParserShot( station, TDString.EMPTY, dist, b, 0, 0.0f, extend_unset, LegType.XSPLAY ) );
    }
    if ( jU >= 0 && ! isNonExistent( lrud[jU] ) ) {  // up
      dist = Float.parseFloat( lrud[jU] ) * ul;
      shots.add( new ParserShot( station, TDString.EMPTY, dist, 0, 90, 0.0f, extend_unset, LegType.XSPLAY ) );
    }
    if ( jD >= 0 && ! isNonExistent( lrud[jD] ) ) {  // down
      dist = Float.parseFloat( lrud[jD] ) * ul;
      shots.add( new ParserShot( station, TDString.EMPTY, dist, 0, -90, 0.0f, extend_unset, LegType.XSPLAY ) );
    }
  }

  /** @return a value from its string presentation
   * @param str  string presentation
   * @param _ul  current units of length
   * @note the string can have a units suffix to override the current unit
   */
  private double strToValue( String str, float _ul ) throws NumberFormatException
  {
    float ul = _ul;
    if ( str.endsWith("f") ) {
      ul = TDUtil.FT2M;
      str = str.substring( 0, str.length() - 1 );
    } else if ( str.endsWith("m") ) {
      ul = 1.0f;
      str = str.substring( 0, str.length() - 1 );
    }
    return Double.parseDouble( str ) * ul;
  }

  /** @return the coordinate value from its string presentation
   * @param str  string presentation
   * @param _ul  current units of length
   * supported formats:
   *    W97:43:52.5  N31:16.75   N31.2791667  3461050.67m 620775.38
   * note W/E/N/S are replaced by the sign in the caller
   */
  private double strToCoordinate( String str, float _ul )
  {
    double d = 0;
    if ( str.charAt(3) == ':' ) { // degrees:...
      d = Integer.parseInt( str.substring(0,3) );
      if ( str.charAt(6) == ':' ) { // degrees:minutes:...
        d += Integer.parseInt( str.substring(4,6) ) / 60.0 + Double.parseDouble( str.substring(7) ) / 3600.0;
      } else {
        d += Double.parseDouble( str.substring(4) ) / 60.0;
      }
      mIsWGS ++;
    } else if ( str.charAt(3) == '.' ) {
      d = Double.parseDouble( str );
      mIsWGS ++;
    } else { // UTM..
      d = strToValue( str, _ul ); 
    }
    return d;
  }
   
        

}
