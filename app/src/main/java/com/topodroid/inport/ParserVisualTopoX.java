/* @file ParserVisualTopoX.java
 *
 * @author marco corvi
 * @date mar 2015
 *
 * @brief TopoDroid VisualTopo trox parser
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
import com.topodroid.prefs.TDSetting;
import com.topodroid.common.ExtendType;
import com.topodroid.common.LegType;
import com.topodroid.TDX.TDAzimuth;
import com.topodroid.TDX.SurveyInfo;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

class ParserVisualTopoX extends ImportParser
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
  ParserVisualTopoX( InputStreamReader isr, String filename, boolean apply_declination, boolean lrud, boolean leg_first ) throws ParserException
  {
    super( apply_declination );
    mName = TDio.extractName( filename );
    mLrud = lrud;
    mLegFirst = leg_first;
    readFile( isr, filename );
    checkValid();
  }

  /** @return true if duplicate is set in the flag
   * @param flag   flag
   */
  private static boolean isDuplicate( String flag )
  {
    if ( flag == null ) return false;
    return ( flag.indexOf('L') >= 0 );
  }

  /** @return true if surface is set in the flag
   * @param flag   flag
   */
  private static boolean isSurface( String flag )
  {
    if ( flag == null ) return false;
    return ( flag.indexOf('X') >= 0 );
  }

  /** @return angle value
   * @param value   value
   * @param unit    angle units
   * @param dm      whether value is in degrees.minute
   */
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

  /** #return string value delimited by double quotes 
   * @param key      key
   * @param line     line haystack
   */
  static private String getValue( String key, String line ) { return getValue( key, "\"", line ); }

  /** #return string value 
   * @param key      key - start
   * @param end      termination
   * @param line     line haystack
   */
  static private String getValue( String key, String end, String line )
  {
    // TDLog.v("VTopo X: get string in <" + line + "> key " + key + " end " + end );
    int pos = line.indexOf( key );
    if ( pos < 0 ) return null;
    pos += key.length();
    int qos = line.indexOf( end, pos );
    if ( qos > pos ) return line.substring( pos, qos ); // empty string is ok
    return null;
  }

  /** #return float value delimited by double quotes 
   * @param key      key
   * @param line     line haystack
   * @param val      default value
   */
  static private float getFloatValue( String key, String line, float val)
  {
    int pos = line.indexOf( key );
    if ( pos < 0 ) return val;
    pos += key.length();
    int qos = line.indexOf( "\"", pos );
    if ( qos <= pos+1 ) return val;
    try {
      return Float.parseFloat( line.substring( pos, qos ) );
    } catch ( NumberFormatException e ) {
      TDLog.Error("VTopo X: Non-number value");
    }
    return val;
  }

  /** add a leg and optionally LRUD
   * @param mFrom      FROM station
   * @param mTo        TO station
   * @param mLength    distance
   * @param mBearing   azimuth
   * @param mClino     clino [degrees]
   * @param shot_extend extend (int)
   * @param duplicate   whether a duplicate leg
   * @param surface     whether a surface leg
   * @param backshot    whether a backshot leg
   * @param comment     comment
   * @param station     LRUD station
   * @param mLeft       left (if positive)
   * @param mRight      right (if positive)
   * @param mUp         up (if positive)
   * @param mDown       down (if positive)
   * @param dir_w        direction of view (for left/right azimuth)
   */
  private void addLeg( String mFrom, String mTo, float mLength, float mBearing, float mClino,
                      int shot_extend, boolean duplicate, boolean surface, boolean backshot, String comment,
                      String station, float mLeft, float mRight, float mUp, float mDown, float dir_w )
  {
    // TDLog.v("add leg " + mFrom + " " + mTo );
    if ( mLegFirst ) {
      // extend = ( mBearing < 90 || mBearing > 270 )? 1 : -1;
      shots.add( new ParserShot( mFrom, mTo, mLength, mBearing, mClino, 0.0f,
                                 shot_extend, LegType.NORMAL, duplicate, surface, backshot, comment ) );
    }
    if ( mLrud ) {
      if ( mLeft > 0 ) {
        float ber = TDMath.in360( mBearing + 180 + 90 * dir_w );
        int extend = ( TDSetting.mLRExtend )? (int)TDAzimuth.computeSplayExtend( ber ) : ExtendType.EXTEND_UNSET;
        shots.add( new ParserShot( station, TDString.EMPTY, mLeft, ber, 0.0f, 0.0f, extend, LegType.XSPLAY, false, false, false, "" ) );
      }
      if ( mRight > 0 ) {
        float ber = TDMath.in360( mBearing + 180 - 90 * dir_w );
        int extend = ( TDSetting.mLRExtend )? (int)TDAzimuth.computeSplayExtend( ber ) : ExtendType.EXTEND_UNSET;
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
      // extend = ( mBearing < 90 || mBearing > 270 )? 1 : -1;
      shots.add( new ParserShot( mFrom, mTo, mLength, mBearing, mClino, 0.0f,
                                 shot_extend, LegType.NORMAL, duplicate, surface, backshot, comment ) );
    }
  }
  private void addSplay( String mFrom, float mLength, float mBearing, float mClino )
  {
    shots.add( new ParserShot( mFrom, TDString.EMPTY, mLength, mBearing, mClino, 0.0f,
                               ExtendType.EXTEND_UNSET, LegType.NORMAL, false, false, false, "" ) );
  }

  /** read input file
   * @param isr        input reader on the input file
   * @param filename   filename, in case isr is null
   */
  private void readFile( InputStreamReader isr, String filename ) throws ParserException
  {
    // TDLog.v("VTopo X read " + filename + " ISR " + ((isr == null)? "null" : isr.toString() ) );
    float mLength=0, mBearing=0, mClino=0, mLeft=-1, mUp=-1, mDown=-1, mRight=-1;
    String mFrom=null, mTo=null;
    boolean isSplay = false;
    String last_to = "";

    int dir_w = 1;  // direction of left/right width
    int dir_b = 1;  // bearing direction
    int dir_c = 1;  // clino direction

    boolean splayAtFrom = true;
    String comment = "";
    String mStation = null; // LRUD station
    // int extend = ExtendType.EXTEND_RIGHT;
    int shot_extend = ExtendType.EXTEND_RIGHT;
    boolean duplicate = false;
    final boolean surface   = false; // TODO ...
    final boolean backshot  = false;

    BufferedReader br = TDio.getBufferedReader( isr, filename );
    String line = null;
    mLineCnt = 0;
    try {
      line = nextLine( br );
      if ( line == null || ! line.startsWith( "<?xml " ) ) { // NOT XML
        TDLog.Error("VTopo X: not an xml file");
        return;
      }
    } catch ( IOException e ) {
      TDLog.Error("VTopo X: i/o error " + mLineCnt + ": " + e.getMessage() );
      return;
    }

    boolean inCavite  = false;
    boolean inMesures = false;
    boolean inVisee   = false;
    boolean withProf  = false;
    float   prevProf  = 0;
    String value;

    try {
      while ( (line = nextLine(br)) != null ) { // line is already trimmed
        // TDLog.v( "LINE " + mLineCnt + ": " + line );

        if ( line.startsWith("<Cavite>") )          { inCavite = true;
        } else if ( line.startsWith("</Cavite>") )  { inCavite = false;
        } else if ( line.startsWith("<Mesures>") )  { inMesures = true;
        } else if ( line.startsWith("</Mesures>") ) { inMesures = false; 
        } else if ( line.startsWith("<Visee") && inMesures ) {
          if ( ! line.endsWith( "/>" ) ) inVisee = true;
          comment = "";
          mFrom = getValue( "Dep=\"", line );
          mTo   = getValue( "Arr=\"", line );
          isSplay = (mTo == null);
          if ( mFrom == null ) mFrom = last_to;
          if ( mTo != null ) last_to = mTo;
          mStation = ( (splayAtFrom || isSplay )? mFrom : mTo );
          mLength  = getFloatValue("Long=\"", line, 0) * ul; 
          mBearing = angle( getFloatValue("Az=\"", line, 0), ub, dmb); 
          if ( withProf ) {
            float prof = getFloatValue("Pte=\"", line, 0) * ul;
            mClino = ( mLength > 0.001 )? TDMath.asind( (prof - prevProf) / mLength ) : 0;
            prevProf = prof;
          } else {
            mClino   = angle( getFloatValue("Pte=\"", line, 0), uc, dmc);
          }
          mBearing = TDMath.in360( mBearing );
          if ( mLrud && ! isSplay ) {
            mLeft  = getFloatValue( "G=\"", line, -1 ) * ul;
            mRight = getFloatValue( "D=\"", line, -1 ) * ul;
            mUp    = getFloatValue( "H=\"", line, -1 ) * ul;
            mDown  = getFloatValue( "B=\"", line, -1 ) * ul;
          }
          shot_extend = ExtendType.EXTEND_RIGHT;
          if ( ( value = getValue( "Inv=\"", line ) ) != null ) {
            if ( value.equals("I") ) shot_extend = ExtendType.EXTEND_LEFT;
          }
          duplicate = false;
          if ( ( value = getValue( "Exc=\"", line ) ) != null ) {
            if ( value.equals("E") ) duplicate = true;
          }
          if ( ! inVisee ) {
            if ( isSplay ) {
              addSplay( mFrom, mLength, mBearing, mClino );
            } else {
              addLeg( mFrom, mTo, mLength, mBearing, mClino, shot_extend, duplicate, surface, backshot, comment,
                      mStation, mLeft, mRight, mUp, mDown, dir_w );
            }
          }
        } else if ( line.startsWith("</Visee>") && inMesures ) { 
          if ( inVisee ) {
            if ( isSplay ) {
              addSplay( mFrom, mLength, mBearing, mClino );
            } else {
              addLeg( mFrom, mTo, mLength, mBearing, mClino, shot_extend, duplicate, surface, backshot, comment,
                      mStation, mLeft, mRight, mUp, mDown, dir_w );
            }
          }
          inVisee = false;
        } else if ( line.startsWith("<Param") && inMesures ) {
          if ( "Deca".equals( getValue( "InstrDist=\"", line ) ) ) {
            ub = 1; dmb = false;
            uc = 1; dmc = false;
            if ( ( value = getValue( "UnitDir=\"", line ) ) != null ) {
              if ( "Deg".equals( value ) ) { 
                dmb = true;
              } else if ( "Gra".equals( value ) ) {
                ub = 0.9f; // 360/400
              } else if ( "Degd".equals( value ) ) {
                TDLog.v("VTopo X: Degd: nothing"); // ub = 1, dmb = false
              }
            }
          }
          if ( "Clino".equals( getValue( "InstrPte=\"", line ) ) ) {
            if ( ( value = getValue( "UnitPte=\"", line ) ) != null ) {
              if ( "Deg".equals( value ) ) { 
                dmc = true;
              } else if ( "Gra".equals( value ) ) {
                uc = 0.9f; // 360/400
              } else if ( "Degd".equals( value ) ) {
                TDLog.v("VTopo X: Degd: nothing"); // uc = 1, dmc = false
              }
            }
          } else if ( "Prof".equals( getValue( "InstrPte=\"", line ) ) ) {
            withProf = true;
            // units profondeur ?
          }
          mDeclination = getFloatValue( "Declin=\"", line, SurveyInfo.DECLINATION_UNSET );
          if ( ( value = getValue( "DeclinAuto=\"", line ) ) != null ) {
            mApplyDeclination = value.equals("M");
          }
          if ( "Inv".equals( getValue( "SensDir=\"", line ) ) ) dir_b = -1;
          if ( "Inv".equals( getValue( "SensPte=\"", line ) ) ) dir_c = -1;
          if ( "Inv".equals( getValue( "SensLar=\"", line ) ) ) dir_w = -1;
          if ( ( value = getValue( "DimPt=\"", line ) ) != null ) {
            if ( value.equals("Inc") ) {
              // FIXME isSplay at next station: Which ???
              splayAtFrom = false;
            } else if ( value.equals("Dep") ) {
              splayAtFrom = true;
            } else if ( value.equals("Arr") ) {
              splayAtFrom = false;
            }
          }
          if ( ( value = getValue( "Date\"", line ) ) != null ) mDate = TDUtil.fromVTopoDate( value );

        } else if ( line.startsWith("<Commentaire>") ) { 
          String comm = getValue("<Commentaire>", "</Commentaire>", line );
          if ( comm != null ) {
            if ( inCavite ) {
              mDescr = comm;
            } else if ( inVisee ) {
              comment = comm;
            }
          }
        } else if ( line.startsWith("<Nom>") && inCavite ) { 
          String name = getValue("<Nom>", "</Nom>", line );
          if ( name != null ) mName = name;
        } else if ( line.startsWith("<Coordonnees") && inCavite ) { // <Coordonnees X="598.39700" Y="4885.56600" Z="1460.00" Projection="UTM32"/>
          float x = getFloatValue( "X=\"", line, 0.0f );
          float y = getFloatValue( "Y=\"", line, 0.0f );
          float z = getFloatValue( "Z=\"", line, 0.0f );
          String proj = getValue( "Projection=\"", line );
          // TODO handle coordinates
        } else if ( line.startsWith("<Club>" ) && inCavite ) {
          String club = getValue("<Club>", "</Club>", line );
          if ( club != null ) mTeam = club;
        } else if ( line.startsWith("<Entree" ) && inCavite ) {
          TDLog.v("VTopo X: Entree: TODO");
        } else if ( line.startsWith("<Configuration>") ) {
          break;
        }
        // ignore if ( line.startsWith("<VisualTopo>") ) continue;
        // ignore if ( line.startsWith("<Version>") ) continue;
        // ignore if ( line.startsWith("<Lignes>") ) continue;
        // ignore if ( line.startsWith("<Toporobot>") ) continue;
        // ignore if ( line.startsWith("<Couleur>") ) continue;
      }
    } catch ( IOException e ) {
      TDLog.Error( "VTopo X: i/o error " + mLineCnt + ": " + line + " = " + e.getMessage() );
      throw new ParserException();
    }
    // TDLog.Log( TDLog.LOG_THERION, "Parser VisualTopo shots "+ shots.size() +" splays "+ splays.size()  );
    // TDLog.v( "VTopo X: shots "+ shots.size() + " splays "+ splays.size() );
  }

}

