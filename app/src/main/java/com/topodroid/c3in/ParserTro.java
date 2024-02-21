/** @file ParserTro.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief VisualTopo file parser
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.c3in;

import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDString;
import com.topodroid.TDX.TopoGL;
import com.topodroid.TDX.TglParser;
import com.topodroid.TDX.Cave3DCS;
import com.topodroid.TDX.Cave3DSurvey;
import com.topodroid.TDX.Cave3DFix;
import com.topodroid.TDX.Cave3DShot;
import com.topodroid.TDX.Cave3DStation;

// import java.io.File;
import java.io.IOException;
// import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
// import java.io.StringWriter;
// import java.io.PrintWriter;
// import java.util.ArrayList;


public class ParserTro extends TglParser
{
  public static final int FLIP_NONE       = 0;
  public static final int FLIP_HORIZONTAL = 1;
  public static final int FLIP_VERTICAL   = 2;

  public static final int DATA_NONE      = 0;
  public static final int DATA_NORMAL    = 1;
  public static final int DATA_DIMENSION = 2;

  double declination = 0.0;
  boolean dmb = false; // whether bearing is DD.MM
  boolean dmc = false;
  double ul = 1;  // units factor [m]
  double ub = 1;  // dec.deg
  double uc = 1;  // dec.deg
  int dir_w = 1;  // width direction (read but not used)
  int dir_b = 1;  // bearing direction (read but not used)
  int dir_c = 1;  // clino direction (read but not used)

  public ParserTro( TopoGL app, InputStreamReader isr, String name ) throws ParserException
  {
    super( app, name );

    readFile( isr );
    processShots();
    setShotSurveys();
    setSplaySurveys();
    setStationDepths();
  }

  // private static boolean isDuplicate( String flag )
  // {
  //   if ( flag == null ) return false;
  //   return ( flag.indexOf('L') >= 0 );
  // }

  // private static boolean isSurface( String flag )
  // {
  //   if ( flag == null ) return false;
  //   return ( flag.indexOf('X') >= 0 );
  // }

  private static double angle( double value, double unit, boolean dm )
  {
    if ( dm ) {
      int sign = 1;
      if ( value < 0 ) { sign = -1; value = -value; }
      int iv = (int)value;
      return sign * ( iv + (value-iv)*0.6f ); // 0.6 = 60/100
    }
    return value * unit;
  }

  private boolean setTroFix( String entrance, String coords )
  {
    String[] params = coords.split(",");
    if ( params.length >= 5 ) {
      String name = params[0].replaceAll(" ","_"); // cave name
      // TDLog.v("TRO entrance " + entrance + " coords " + coords );
      try { // FIXME coordinates NOT SURE THERE IS z
        double x = Double.parseDouble( params[1] );
        double y = Double.parseDouble( params[2] );
        double z = Double.parseDouble( params[3] );
        // TDLog.v("TRO XYZ " + x + " " + y + " " + z );
        String vt_cs = params[4]; // coords system: must be present in VisualTopo registry
        fixes.add( new Cave3DFix( entrance, x, y, z, new Cave3DCS( vt_cs ), 1, 1 ) ); // no WGS84 - M_TO_UNITS = 1
        return true;
      } catch ( NumberFormatException e ) {
        TDLog.Error("Non-number param");
      }
    }
    return false;
  }

  /** read input TRO file
   */
  private boolean readFile( InputStreamReader isr ) throws ParserException
  {
    if ( isr == null ) {
      TDLog.Error("Parser Tro: null input stream reader");
      throw new ParserException( "null TRO input", 0 );
    }

    String entrance = null;
    String coords   = null;
    // Cave3DCS cs = null;
    // int in_data = 0; // 0 none, 1 normal, 2 dimension

    // String survey = null; // UNUSED

    String last_to = "";
    boolean splayAtFrom = true;
    String comment = "";
    long millis = 0;

    try {
      int cnt_shot = 0;
      int cnt_splay = 0;
      BufferedReader br = new BufferedReader( isr );
      linenr = 0;
      for ( String line = nextLine( br ); line != null; line = nextLine( br ) ) {
        if ( line.length() == 0 ) continue;
        line = line.replaceAll("\\s+", " ");
        if ( line.startsWith("[Configuration") ) break;
        // TDLog.v( "TRO LINE: " + line );

        int pos = line.indexOf(";");
        if ( pos >= 0 ) {
          comment = (pos+1<line.length())? line.substring( pos+1 ) : "";
          line    = line.substring( 0, pos );
          comment = comment.trim();
        } else {
          comment = "";
        }
        if ( line.length() == 0 ) continue;    // comment
        
        String[] vals = splitLine( line ); 
        int idx = TDString.nextIndex( vals, -1 );
        if ( line.startsWith("Version") ) {
          // IGNORE
        } else if ( line.startsWith("Trou") ) { 
          coords = line.substring(5); // save 
          if ( entrance != null ) setTroFix( entrance, coords );
        } else if ( vals[idx].equals("Param") ) {
          for ( int k = idx+1; k < vals.length; ++k ) {
            if ( vals[k].equals("Deca") ) {
              if ( ++k < vals.length ) {
                ub = 1;
                dmb = false;
                if ( vals[k].equals("Deg") ) {
                  dmb = true;
                } else if ( vals[k].equals("Gra" ) ) {
                  ub = 0.9f; // 360/400
                } else { // if ( vals[k].equals("Degd" )
                  /* nothing: dmb = false */
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
                } else { // if ( vals[k].equals("Degd" )
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
              // standard colors; ignore
            } else if ( k == 5 ) {
              try {
                declination = angle( Double.parseDouble( vals[k] ), 1, true );
              } catch ( NumberFormatException e ) {
                TDLog.Error("Non-number declination");
              }
            } else {
              // ignore colors
            }
          }
        } else if ( vals[idx].equals("Entree") ) { // entrance station
          if ( vals.length > 1 ) {
            entrance = vals[1];
            if ( coords != null ) setTroFix( entrance, coords );
          }
        } else if ( vals[idx].equals("Club") ) {  // team and caving club
          // IGNORE mTeam = line.substring(5);
        } else if ( vals[idx].equals("Couleur") ) { 
          // IGNORE
        } else if ( vals[idx].equals("Surface") ) {
          // IGNORE
        } else { // survey data
          if ( vals.length >= 5 ) {
            String from = vals[idx];
            if ( from.equals( entrance ) ) TDLog.v("TRO line: " + line );
            if ( from.equals("*") ) from = last_to;

            idx = TDString.nextIndex( vals, idx );
            String to   = vals[idx];
            if ( ! to.equals("*") ) last_to = to;
            if ( ! from.equals( to ) ) {
              boolean splay = ( to.equals( "*" ) );

              try {
                idx = TDString.nextIndex( vals, idx );
                double len = Double.parseDouble(vals[idx]) * ul;
                idx = TDString.nextIndex( vals, idx );
                double ber = angle( Double.parseDouble(vals[idx]), ub, dmb);
                idx = TDString.nextIndex( vals, idx );
                double cln = angle( Double.parseDouble(vals[idx]), uc, dmc); 
                if ( splay ) {
                  if ( mSplayUse > SPLAY_USE_SKIP ) {
                    splays.add( new Cave3DShot( from, from + cnt_splay, len, ber, cln, 0, millis, mColor ) );
                    ++ cnt_splay;
                  }
                } else {
                  String station = ( (splayAtFrom || splay )? from : to );
                  shots.add( new Cave3DShot( from, to, len, ber, cln, 0, millis, mColor ) );
                  ++ cnt_shot;

                  if ( mSplayUse > SPLAY_USE_SKIP ) {
                    idx = TDString.nextIndex( vals, idx );
	            len = vals[idx].equals("*")? -1 : Double.parseDouble(vals[idx]) * ul; 
	            if ( len > 0 ) splays.add( new Cave3DShot( station, station+"-L", len, ber-90, 0, 0, millis, mColor ) );
                    
                    idx = TDString.nextIndex( vals, idx );
	            len = vals[idx].equals("*")? -1 : Double.parseDouble(vals[idx]) * ul; 
	            if ( len > 0 ) splays.add( new Cave3DShot( station, station+"-R", len, ber+90, 0, 0, millis, mColor ) );

                    idx = TDString.nextIndex( vals, idx );
	            len = vals[idx].equals("*")? -1 : Double.parseDouble(vals[idx]) * ul; 
	            if ( len > 0 ) splays.add( new Cave3DShot( station, station+"-U", len, ber, 90, 0, millis, mColor ) );
                    
                    idx = TDString.nextIndex( vals, idx );
	            len = vals[idx].equals("*")? -1 : Double.parseDouble(vals[idx]) * ul; 
	            if ( len > 0 ) splays.add( new Cave3DShot( station, station+"-D", len, ber, -90, 0, millis, mColor ) );
                  }
                }
              } catch ( NumberFormatException e ) {
                TDLog.Error( "TRO Error " + linenr + ": " + line + " " + e.getMessage() );
              }
            }
          }
        }
      }
    } catch ( IOException e ) {
      TDLog.Error( "TRO IO error " + e.getMessage() );
      throw new ParserException( getName(), linenr );
    }
    // TDLog.v( "TRO shots " + shots.size() + " splays " + splays.size() );
    return ( shots.size() > 0 );
  }

  // ------------------------------------------------------------

  private void setShotSurveys()
  {
    for ( Cave3DShot sh : shots ) {
      Cave3DStation sf = sh.from_station;
      Cave3DStation st = sh.to_station;
      sh.mSurvey = null;
      if ( sf != null && st != null ) {
        String sv = sh.from;
        sv = sv.substring( 1 + sv.indexOf('@', 0) );
        for ( Cave3DSurvey srv : surveys ) {
          if ( srv.hasName( sv ) ) {
            // sh.mSurvey = srv;
            // sh.mSurveyNr = srv.number;
            // srv.addShotInfo( sh );
            srv.addShot( sh );
            break;
          }
        }
        if ( sh.mSurvey == null ) {
          Cave3DSurvey survey = new Cave3DSurvey(sv, 0);
          // sh.mSurvey = survey;
          // sh.mSurveyNr = survey.number;
          // survey.addShotInfo( sh );
          survey.addShot( sh );
          surveys.add( survey );
        } 
      }
    }
  }

  private void setSplaySurveys()
  {
    if ( mSplayUse == SPLAY_USE_SKIP ) return;
    for ( Cave3DShot sh : splays ) {
      String sv = null;
      Cave3DStation sf = sh.from_station;
      if ( sf == null ) {
        sf = sh.to_station;
        sv = sh.to;
      } else {
        sv = sh.from;
      }
      if ( sf != null ) {
        sv = sv.substring( 1 + sv.indexOf('@', 0) );
        for ( Cave3DSurvey srv : surveys ) {
          if ( srv.hasName( sv ) ) {
            // sh.mSurvey = srv;
            // sh.mSurveyNr = srv.number;
            // srv.addSplayInfo( sh );
            srv.addSplay( sh );
            break;
          }
        }
      }
    }
  }

  private void processShots()
  {
    if ( shots.size() == 0 ) return;
    if ( fixes.size() == 0 ) {
      Cave3DShot sh = shots.get( 0 );
      fixes.add( new Cave3DFix( sh.from, 0.0f, 0.0f, 0.0f, null, 1, 1 ) ); // no WGS84 - M_TO_UNITS = 1
      // TDLog.v( "TRO shots " + shots.size() + " no fixes. starts at " + sh.from );
    }
 
    int mLoopCnt = 0;
    Cave3DFix f0 = fixes.get( 0 );
    // TDLog.v( "TRO process shots. Fix " + f0.name + " " + f0.x + " " + f0.y + " " + f0.z );

    mCaveLength = 0.0f;
    mSurfaceLength = 0.0f;

    for ( Cave3DFix f : fixes ) {
      boolean found = false;
      // TDLog.v( "TRO checking fix " + f.name );
      for ( Cave3DStation s1 : stations ) {
        if ( s1.hasName( f.getFullName() ) ) { found = true; break; }
      }
      if ( found ) { // skip fixed stations that are already included in the model
        // TDLog.v( "TRO found fix " + f.name );
        continue;
      }
      // TDLog.v( "TRO start station " + f.name + " N " + f.y + " E " + f.x + " Z " + f.z );
      stations.add( new Cave3DStation( f.getFullName(), f.x, f.y, f.z ) );
      // sh.from_station = s0;
    
      boolean repeat = true;
      while ( repeat ) {
        // TDLog.v( "TRO scanning the shots");
        repeat = false;
        for ( Cave3DShot sh : shots ) {
          if ( sh.isUsed() ) continue;
          // TDLog.v( "TRO check shot " + sh.from + " " + sh.to );
          // Cave3DStation sf = sh.from_station;
          // Cave3DStation st = sh.to_station;
          Cave3DStation sf = null;
          Cave3DStation st = null;
          for ( Cave3DStation s : stations ) {
            if ( s.hasName( sh.from ) ) {
              sf = s;
              if (  sh.from_station == null ) sh.from_station = s;
              else if ( sh.from_station != s ) TDLog.Error( "TRO shot " + sh.from + " " + sh.to + " from-station mismatch ");
            } 
            if ( s.hasName( sh.to ) )   {
              st = s;
              if (  sh.to_station == null ) sh.to_station = s;
              else if ( sh.to_station != s ) TDLog.Error( "TRO shot " + sh.from + " " + sh.to + " to-station mismatch ");
            }
            if ( sf != null && st != null ) break;
          }
          if ( sf != null && st != null ) {
            // TDLog.v( "TRO unused shot " + sh.from + " " + sh.to + " : " + sf.name + " " + st.name );
            sh.setUsed( ); // LOOP
            if ( sh.isSurvey() ) {
              mCaveLength += sh.length();
            } else if ( sh.isSurface() ) {
              mSurfaceLength += sh.length();
            }
            // make a fake station
            Cave3DStation s = sh.getStationFromStation( sf );
            stations.add( s );
            s.addToName( mLoopCnt ); // s.name = s.name + "-" + mLoopCnt;
            ++ mLoopCnt;
            sh.to_station = s;
          } else if ( sf != null && st == null ) {
            // TDLog.v( "TRO unused shot " + sh.from + " " + sh.to + " : " + sf.name + " null" );
            Cave3DStation s = sh.getStationFromStation( sf );
            stations.add( s );
            sh.to_station = s;
            // TDLog.v("TRO add station " + sh.to_station.name + " N " + sh.to_station.n + " E " + sh.to_station.e + " Z " + sh.to_station.z );
            sh.setUsed( );
            if ( sh.isSurvey() ) {
              mCaveLength += sh.length();
            } else if ( sh.isSurface() ) {
              mSurfaceLength += sh.length();
            }
            repeat = true;
          } else if ( sf == null && st != null ) {
            // TDLog.v( "TRO unused shot " + sh.from + " " + sh.to + " : null " + st.name );
            Cave3DStation s = sh.getStationFromStation( st );
            stations.add( s );
            sh.from_station = s;
            // TDLog.v("TRO add station " + sh.from_station.name + " N " + sh.from_station.n + " E " + sh.from_station.e + " Z " + sh.from_station.z );
            sh.setUsed( );
            if ( sh.isSurvey() ) {
              mCaveLength += sh.length();
            } else if ( sh.isSurface() ) {
              mSurfaceLength += sh.length();
            }
            repeat = true;
          } else {
            // TDLog.v( "TRO unused shot " + sh.from + " " + sh.to + " : null null" );
          }
        }
      }
    } // for ( Cave3DFix f : fixes )

    // 3D splay shots
    if ( mSplayUse > SPLAY_USE_SKIP ) {
      for ( Cave3DShot sh : splays ) {
        if ( sh.isUsed() ) continue;
        if (  sh.from_station != null ) continue;
        // TDLog.v("TRO check shot " + sh.from + " " + sh.to );
        for ( Cave3DStation s : stations ) {
          if ( s.hasName( sh.from ) ) {
            sh.from_station = s;
            sh.setUsed( );
            sh.to_station = sh.getStationFromStation( s );
            break;
          }
        }
      }
    }

    computeBoundingBox();
    // // bounding box
    // emin = emax = stations.get(0).e;
    // nmin = nmax = stations.get(0).n;
    // zmin = zmax = stations.get(0).z;
    // for ( Cave3DStation s : stations ) {
    //   if ( nmin > s.n )      nmin = s.n;
    //   else if ( nmax < s.n ) nmax = s.n;
    //   if ( emin > s.e )      emin = s.e;
    //   else if ( emax < s.e ) emax = s.e;
    //   if ( zmin > s.z )      zmin = s.z;
    //   else if ( zmax < s.z ) zmax = s.z;
    // }
  }

}
