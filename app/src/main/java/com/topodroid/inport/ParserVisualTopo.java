/* @file ParserVisualTopo.java
 *
 * @author marco corvi
 * @date mar 2015
 *
 * @brief TopoDroid VisualTopo tro parser
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
// import com.topodroid.utils.TDUtil;
import com.topodroid.prefs.TDSetting;
import com.topodroid.common.ExtendType;
import com.topodroid.common.LegType;
import com.topodroid.TDX.TDAzimuth;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

class ParserVisualTopo extends ImportParser
{
  private boolean dmb = false; // whether bearing is DD.MM
  private boolean dmc = false;
  private float ul = 1;  // units factor [m]
  private float ub = 1;  // dec.deg
  private float uc = 1;  // dec.deg
  private boolean mLrud;
  private boolean mLegFirst;

  /** VisualTopo parser
   * @param filename name of the file to parse
   * @param apply_declination  whether to apply declination correction
   */
  ParserVisualTopo( InputStreamReader isr, String filename, boolean apply_declination, boolean lrud, boolean leg_first ) throws ParserException
  {
    super( apply_declination );
    mName = TDio.extractName( filename );
    mLrud = lrud;
    mLegFirst = leg_first;
    readFile( isr, filename );
    checkValid();

  }

  private static boolean isDuplicate( String flag )
  {
    if ( flag == null ) return false;
    return ( flag.indexOf('L') >= 0 );
  }

  private static boolean isSurface( String flag )
  {
    if ( flag == null ) return false;
    return ( flag.indexOf('X') >= 0 );
  }

  private static float angle( float value, float unit, boolean dm )
  {
    if ( dm ) { // angle value in degrees.minutes
      int sign = 1;
      if ( value < 0 ) { sign = -1; value = -value; }
      int iv = (int)value;
      return sign * ( iv + (value-iv)*0.6f ); // 0.6 = 60/100
    }
    return value * unit;
  }
      

  /** read input file
   * @param isr input reader on the input file
   * @param filename   filename, in case isr is null
   */
  private void readFile( InputStreamReader isr, String filename ) throws ParserException
  {
    float mLength, mBearing, mClino, mLeft, mUp, mDown, mRight;
    String mFlag=null, mFrom=null, mTo=null;
    String last_to = "";

    int dir_w = 1;  // width direction
    int dir_b = 1;  // bearing direction
    int dir_c = 1;  // clino direction

    boolean splayAtFrom = true;
    String comment = "";
    int extend = ExtendType.EXTEND_RIGHT;
    int shot_extend = ExtendType.EXTEND_RIGHT;
    boolean duplicate = false;
    final boolean surface   = false; // TODO ...
    final boolean backshot  = false;

    BufferedReader br = TDio.getBufferedReader( isr, filename );
    String line = null;
    try {
      line = nextLine( br );
      while ( line != null ) {
        line = line.trim();
        // TDLog.v( "LINE: " + line );
        if ( line.startsWith("[Configuration]") ) break;

        int pos = line.indexOf(";");
        if ( pos >= 0 ) {
          comment = (pos+1<line.length())? line.substring( pos+1 ) : "";
          line    = line.substring( 0, pos );
          comment = comment.trim();
        } else {
          comment = "";
        }
        if ( line.length() > 0 ) {    // length==0 comment
          String[] vals = splitLine(line); // line.split( "\\s+" );
          if ( line.startsWith("Version") ) {
            TDLog.v("Version: ignored");
          } else if ( line.startsWith("Trou") ) {
            String[] params = line.substring(5).split(",");
            if ( params.length > 0 ) {
              mName = params[0].replaceAll(" ","_");
              // TODO coordinates
            }
          } else if ( vals[0].equals("Param") ) {
            for ( int k = 1; k < vals.length; ++k ) {
              if ( vals[k].equals("Deca") ) {
                if ( ++k < vals.length ) {
                  ub = 1;
                  dmb = false;
                  if ( vals[k].equals("Deg") ) {
                    dmb = true;
                  } else if ( vals[k].equals("Gra" ) ) {
                    ub = 0.9f; // 360/400
                  // } else { // if ( vals[k].equals("Degd" )
                    /* nothing */
                  }
                }
              } else if ( vals[k].equals("Clino") ) {
                if ( ++k < vals.length ) {
                  uc = 1;
                  dmc = false;
                  if ( vals[k].equals("Deg") ) {
                    dmc = true;
                  } else if ( vals[k].equals("Gra" ) ) {
                    uc = 0.9f; // 360/400
                  // } else { // if ( vals[k].equals("Degd" )
                    /* nothing */
                  }
                }
              } else if ( vals[k].startsWith("Dir") || vals[k].startsWith("Inv") ) {
                String[] dirs = vals[k].split(",");
                if ( dirs.length == 3 ) {
                  dir_b = ( dirs[0].equals("Dir") )? 1 : -1;
                  dir_c = ( dirs[1].equals("Dir") )? 1 : -1;
                  dir_w = ( dirs[2].equals("Dir") )? 1 : -1;
                }
              } else if ( vals[k].equals("Inc") ) {
                // FIXME splay at next station: Which ???
                splayAtFrom = false;
              } else if ( vals[k].equals("Dep") ) {
                splayAtFrom = true;
              } else if ( vals[k].equals("Arr") ) {
                splayAtFrom = false;
              } else if ( vals[k].equals("Std") ) {
                TDLog.v("Std colors: ignored" );
              } else if ( k == 5 ) {
                try {
                  mDeclination = angle( Float.parseFloat( vals[k] ), 1, true ); // declination is in degrees.minutes
                } catch ( NumberFormatException e ) {
                  TDLog.Error("VTopo: Non-number declination");
                }
              // } else {
                // ignore colors
              }
            }
          } else if ( vals[0].equals("Entree") ) {
            TDLog.v("Entree: ignored" );
          } else if ( vals[0].equals("Club") ) {
            mTeam = line.substring(5);
          } else if ( vals[0].equals("Couleur") ) {
            TDLog.v("Couleur: ignored");
          } else if ( vals[0].equals("Surface") ) {
            TDLog.v("Surface: ignored");
          } else { // survey data
            if ( vals.length >= 5 && ! vals[0].equals( vals[1] ) ) {
              boolean splay = false;
              int k = 0;
              mFrom = vals[k]; ++k; // 0
              if ( mFrom.equals("*") ) mFrom = last_to;
              mTo   = vals[k]; ++k; // 1
              if ( mTo.equals( "*" ) ) { splay = true; } else { last_to = mTo; }
              try {
                String station = ( (splayAtFrom || splay )? mFrom : mTo );
                mLength  = Float.parseFloat(vals[k]) * ul; ++k; // 2
                mBearing = angle( Float.parseFloat(vals[k]), ub, dmb); ++k; // 3
                mClino   = angle( Float.parseFloat(vals[k]), uc, dmc); ++k; // 5
                mBearing = TDMath.in360( mBearing );
                if ( splay ) {
                  shots.add( new ParserShot( mFrom, TDString.EMPTY, mLength, mBearing, mClino, 0.0f,
                                             ExtendType.EXTEND_UNSET, LegType.NORMAL, false, false, false, "" ) );

                } else {
                  // TDLog.v( mFrom + " " + mTo + " " + mBearing + " DMB " + dmb + " UB " + ub );
                  mLeft = mRight = mUp = mDown = 0;
                  if ( mLrud ) {
                    if ( k < vals.length ) {
                      mLeft  = vals[k].equals("*")? -1 : Float.parseFloat(vals[k]) * ul; ++k; // 5
                    }
                    if ( k < vals.length ) {
                      mRight = vals[k].equals("*")? -1 : Float.parseFloat(vals[k]) * ul; ++k; // 6
                    }
                    if ( k < vals.length ) {
                      mUp    = vals[k].equals("*")? -1 : Float.parseFloat(vals[k]) * ul; ++k; // 7
                    }
		    if ( k < vals.length ) {
                      mDown  = vals[k].equals("*")? -1 : Float.parseFloat(vals[k]) * ul; ++k; // 8
                    }
                  }
                  shot_extend = ExtendType.EXTEND_RIGHT;
                  if ( k < vals.length ) {
                    shot_extend = vals[k].equals("N")? ExtendType.EXTEND_RIGHT : ExtendType.EXTEND_LEFT; ++k; // 'N' or 'I'
                  } 
                  duplicate = false;
                  if ( k < vals.length ) {
                    duplicate = vals[k].equals("E"); ++k;           // 'I' or 'E'
                  }

                  if ( mLegFirst ) {
                    extend = ( mBearing < 90 || mBearing > 270 )? 1 : -1;
                    shots.add( new ParserShot( mFrom, mTo, mLength, mBearing, mClino, 0.0f,
                                               shot_extend, LegType.NORMAL, duplicate, surface, backshot, comment ) );
                  }
                  if ( mLrud ) {
                    if ( mLeft > 0 ) {
	              float ber = TDMath.in360( mBearing + 180 + 90 * dir_w );
                      extend = ( TDSetting.mLRExtend )? (int)TDAzimuth.computeSplayExtend( ber ) : ExtendType.EXTEND_UNSET;
                      shots.add( new ParserShot( station, TDString.EMPTY, mLeft, ber, 0.0f, 0.0f, extend, LegType.XSPLAY, false, false, false, "" ) );
                    }
                    if ( mRight > 0 ) {
                      float ber = TDMath.in360( mBearing + 180 - 90 * dir_w );
                      extend = ( TDSetting.mLRExtend )? (int)TDAzimuth.computeSplayExtend( ber ) : ExtendType.EXTEND_UNSET;
                      shots.add( new ParserShot( station, TDString.EMPTY, mRight, ber, 0.0f, 0.0f, -extend, LegType.XSPLAY, false, false, false, "" ) );
                    } 
                    if ( mUp > 0 ) {
                      // FIXME splays
                      shots.add( new ParserShot( station, TDString.EMPTY, mUp, 0.0f, 90.0f, 0.0f, ExtendType.EXTEND_VERT, LegType.XSPLAY, false, false, false, "" ) );
                    }
                    if ( mDown > 0 ) {
                      // FIXME splays
                      shots.add( new ParserShot( station, TDString.EMPTY, mDown, 0.0f, -90.0f, 0.0f, ExtendType.EXTEND_VERT, LegType.XSPLAY, false, false, false, "" ) );
                    }
                  }
                  if ( ! mLegFirst ) { 
                    extend = ( mBearing < 90 || mBearing > 270 )? 1 : -1;
                    shots.add( new ParserShot( mFrom, mTo, mLength, mBearing, mClino, 0.0f,
                                               shot_extend, LegType.NORMAL, duplicate, surface, backshot, comment ) );
                  }
                }
              } catch ( NumberFormatException e ) {
                TDLog.Error( "VTopo: number error " + mLineCnt + ": " + line + " " + e.getMessage() );
              }
            }
          }
        }
        line = nextLine( br );
      }
    } catch ( IOException e ) {
      TDLog.Error( "VTopo: i/o error " + mLineCnt + ": " + line + " " + e.getMessage() );
      throw new ParserException();
    }
    // TDLog.Log( TDLog.LOG_THERION, "Parser VisualTopo shots "+ shots.size() +" splays "+ splays.size()  );
    // TDLog.v( "VTopo: shots "+ shots.size() + " splays "+ splays.size() );
  }

  // float parseAngleUnit( String unit )
  // {
  //   // not handled "percent"
  //   if ( unit.startsWith("Min") ) return 1/60.0f;
  //   if ( unit.startsWith("Grad") ) return (float)TDUtil.GRAD2DEG;
  //   if ( unit.startsWith("Mil") ) return (float)TDUtil.GRAD2DEG;
  //   // if ( unit.startsWith("Deg") ) return 1.0f;
  //   return 1.0f;
  // }

  // float parseLengthUnit( String unit )
  // {
  //   if ( unit.startsWith("c") ) return 0.01f; // cm centimeter
  //   if ( unit.startsWith("f") ) return (float)TDUtil.FT2M; // ft feet
  //   if ( unit.startsWith("i") ) return (float)TDUtil.IN2M; // in inch
  //   if ( unit.startsWith("milli") || unit.equals("mm") ) return 0.001f; // mm millimeter
  //   if ( unit.startsWith("y") ) return (float)TDUtil.YD2M; // yd yard
  //   // if ( unit.startsWith("m") ) return 1.0f;
  //   return 1.0f;
  // }

}
