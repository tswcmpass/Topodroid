/* @file ParserTherion.java
 *
 * @author marco corvi
 * @date may 2012
 *
 * @brief TopoDroid Therion parser
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

import com.topodroid.common.LegType;
import com.topodroid.common.StationFlag;

import java.io.IOException;
// import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

// TODO this class can be made extend ImportParser
//
// units, calibrate, sd supported quantities:
//   length tape bearing compass gradient clino counter depth x y z position easting dx northing dy altitude dz

class ParserTherion extends ImportParser
{
  /** fix station:
   * fix stations are supposed to be referred to the same coord system
   */
  static class ThFix
  {
    // private CS cs;
    String name;
    float e, n, z; // north east, vertical (upwards)

    ThFix( String nm, float e0, float n0, float z0 )
    {
      name = nm;
      e = e0;
      n = n0;
      z = z0;
    }
  }

  static class Station
  {
    String name;
    String comment;
    long flag;
 
    Station( String n, String c, long f )
    {
      name = n;
      comment = c;
      flag = f;
    }
  }

  private ArrayList< ThFix > fixes;
  private ArrayList< Station > stations;
  // private ArrayList< ParserShot > shots;   // centerline shots // FROM ImportParser
  // private ArrayList< ParserShot > splays;  // splay shots
  // public int getShotNumber()    { return shots.size(); } // FROM ImportParser
  // public int getSplayNumber()   { return splays.size(); }
  // ArrayList< ParserShot > getShots()    { return shots; }
  // ArrayList< ParserShot > getSplays()   { return splays; }
  ArrayList< Station >    getStations() { return stations; }
  ArrayList< ThFix >        getFixes()    { return fixes; }

  // same as in ImportParser.java
  // String initStation() // FROM ImportParser
  // {
  //   for ( ParserShot sh : shots ) {
  //     if ( sh.from != null && sh.from.length() > 0 ) return sh.from;
  //   }
  //   return TDString.ZERO;
  // }

  /** @return the declination value [deg ?]
   */
  float surveyDeclination( ) { return mDeclination; }

  // ---------------------------------------------------------

  /** cstr
   * @param isr    input reader
   * @param name   filename or surveyname ?
   * @param apply_declination whether to apply the declination correction
   * @param therionPath       whether to add survey-path to station names
   */
  ParserTherion( InputStreamReader isr, String name, boolean apply_declination, boolean therionPath ) throws ParserException
  {
    super( apply_declination );
    fixes    = new ArrayList<>();
    stations = new ArrayList<>();
    // shots    = new ArrayList<>(); // FROM ImportParser
    // splays   = new ArrayList<>();
    // // mStates  = new Stack< ParserTherionState >();
    // mApplyDeclination = apply_declination;
    ParserTherionState state = new ParserTherionState(); // root of the linked list of states
    readFile( isr, name, "", state, therionPath );
    checkValid();
  }

  // private String nextLine( BufferedReader br ) throws IOException // FROM ImportParser
  // {
  //   StringBuffer ret = new StringBuffer();
  //   {
  //     String line = br.readLine();
  //     if ( line == null ) return null; // EOF
  //     while ( line != null && line.endsWith( "\\" ) ) {
  //       ret.append( line.replace( '\\', ' ' ) ); // FIXME
  //       line = br.readLine();
  //     }
  //     if ( line != null ) ret.append( line );
  //   }
  //   return ret.toString();
  // }

  /** @return the name of the station from a fullname 
   * @param fullname   fullname
   */
  private String extractStationName( String fullname, String path ) 
  {
    int idx = fullname.indexOf('@');
    if ( idx > 0 ) {
       if ( path != null ) {
         return fullname.substring( 0, idx ) + "@" + path + "." + fullname.substring(idx+1);
       } else {
         return fullname.substring( 0, idx );
       }
    }
    if ( path != null ) {
      return fullname + "@" + path;
    }
    return fullname;
  }

  /** read input file
   * @param filename name of the file to parse
   * @param basepath survey pathname base
   * @param state    state of the parser
   * @param therionPath       whether to add survey-path to station names
   */
  private void readFile( InputStreamReader isr, String filename, String basepath, ParserTherionState state, boolean therionPath ) throws ParserException
  {
    // TDLog.v("Parser TH file " + filename + " base " + basepath );
    String path = basepath;   // survey pathname(s)
    int ks = 0;               // survey index
    int ks_max = 20;
    int[] survey_pos = new int[ks_max]; // current survey pos in the pathname

    int jFrom    = 0;
    int jTo      = 1;
    int jLength  = 2;
    int jCompass = 3;
    int jClino   = 4;
    int jLeft  = -1;
    int jRight = -1;
    int jUp    = -1;
    int jDown  = -1;

    Pattern pattern = Pattern.compile( "\\s+" );
    StringBuffer team = new StringBuffer();
    String surveyPath = therionPath? path : null;

    try {
      String dirname = "./";
      int i = filename.lastIndexOf('/');
      if ( i > 0 ) dirname = filename.substring(0, i+1);
      // System.out.println("readFile dir " + dirname + " filename " + filename );
      // TDLog.v( "import read Therion file <" + filename + ">" );

      BufferedReader br = TDio.getBufferedReader( isr, filename );
      String line = nextLine( br );
      while ( line != null ) {
        // TDLog.v( "Parser TH " + state.in_survey + " " + state.in_centerline + " " + state.in_data + " : " + line );
        line = line.trim();
        int pos = line.indexOf( '#' );
        if ( pos >= 0 ) {
          line = line.substring( 0, pos );
        }
        if ( line.length() > 0 ) {
          String[] vals = pattern.split(line); // line.split( "\\s+" );
          // int vals_len = 0;
          // for ( int k=0; k<vals.length; ++k ) {
          //   vals[vals_len] = vals[k];
          //   if ( vals[vals_len].length() > 0 ) {
          //     ++ vals_len;
          //   }
          // }
          // TDLog.v( "Parser TH vals " + vals.length + " line " + line );
          int vals_len = vals.length;
          if ( vals_len > 0 ) {
            String cmd = vals[0];
            
            if ( cmd.equals("encoding" ) ) { 
              // TDLog.v("Warning: therion encoding ignored");
            } else if ( cmd.equals("import") ) {
              // TDLog.v("Warning: therion import ignored");
            } else if ( ! state.in_centerline && cmd.equals("grade") ) {
              // TDLog.v("Warning: therion grade ignored");
            } else if ( cmd.equals("revise") ) {
              // TDLog.v("Warning: therion revise ignored");
            } else if ( cmd.equals("join") ) {
              // TDLog.v("Warning: therion join ignored");
            } else if ( cmd.equals("grade") ) {
              // TDLog.v("Warning: therion grade ignored");
              state.in_grade = true;
            } else if ( cmd.equals("input") ) { // ignore
              // TDLog.v("Warning: therion input ignored");
              // int j = 1;
              // while ( vals[j] != null ) {
              //   if ( vals[j].length() > 0 ) {
              //     filename = vals[j];
              //     if ( filename.endsWith( ".th" ) ) {
              //       readFile( dirname + '/' + filename, 
              //           path,
              //           use_survey_declination, survey_declination,
              //           units_len, units_ber, units_cln );
              //     }
              //     break;
              //   }
              // }
            } else if ( cmd.equals("surface") ) {
              // TODO check not already in_surface
              state.in_surface = true;
              // TDLog.v("Theron in surface");
            } else if ( cmd.equals("map") ) {
              // TODO check not already in_map
              state.in_map = true;
              // TDLog.v("Theron in map");
            } else if ( cmd.equals("scrap") ) {
              // TODO check not already in_scrap
              state.in_scrap = true;
              // TDLog.v("Theron in scrap");
            } else if ( state.in_scrap && cmd.equals("line") ) {
              // TODO check not already in_line
              state.in_line = true;
              // TDLog.v("Theron in line");
            } else if ( state.in_scrap && cmd.equals("area") ) {
              // TODO check not already in_area
              state.in_area = true;
              // TDLog.v("Theron in area");

            } else if ( state.in_line && cmd.equals("endline") ) { 
              state.in_line = false;
            } else if ( state.in_area && cmd.equals("endarea" ) ) {
              state.in_area = false;
            } else if ( state.in_scrap && cmd.equals("endscrap" ) ) {
              state.in_scrap = false;
            } else if ( state.in_map && cmd.equals("endmap" ) ) {
              state.in_map = false;
            } else if ( state.in_surface && cmd.equals("endsurface" ) ) {
              state.in_surface = false;
            } else if ( state.in_grade && cmd.equals("endgrade") ) {
              state.in_grade = false;
            } else if ( state.in_map || state.in_surface || state.in_scrap || state.in_line || state.in_area || state.in_grade ) {
              // ignore
              // TDLog.v("Warning: therion ignored " + line);

            } else if ( cmd.equals("survey") ) {
              survey_pos[ks] = path.length(); // set current survey pos in pathname
              path = path + "." + vals[1];    // add survey name to path
              ++ks;
	      if ( ks >= ks_max ) {
		    ks_max += 10;
		    int[] tmp = new int[ks_max];
		    // for ( int k=0; k<ks; ++k ) tmp[k] = survey_pos[k];
		    System.arraycopy( survey_pos, 0, tmp, 0, ks );
		    survey_pos = tmp;
	      }
              // pushState( state );
              state = new ParserTherionState( state );
              state.mSurveyLevel ++;
              state.in_survey= true;

              // parse survey id
              if ( mName == null ) {
                mName = vals[1];
              }
              // TDLog.v("Theron in survey " + mName );

              // parse survey options
              for ( int j=2; j<vals_len; ++j ) {
                if ( vals[j].equals("-declination") && j+1 < vals_len ) {
		  if ( vals[j+1].equals("-") ) { // declination reset
                    state.mDeclination = ( state.mParent == null )? 0 : state.mParent.mDeclination;
		  } else {
                    try {
                      state.mDeclination = Float.parseFloat( vals[j+1] );
                      ++j;
                      if ( j+1 < vals_len ) { // FIXME check for units
                        state.mDeclination *= ParserUtil.parseAngleUnit( vals[j+1] );
                        ++j;
                      }
                      if ( ! mApplyDeclination ) mDeclination = state.mDeclination;
                    } catch ( NumberFormatException e ) {
                      TDLog.Error( "Error therion: -declination " + line );
                    }
		  }
                } else if ( vals[j].equals("-title") && j+1 < vals_len ) {
                  for ( ++j; j<vals_len; ++j ) {
                    if ( vals[j].length() == 0 ) continue;
                    if ( vals[j].startsWith("\"") ) {
                      if ( vals[j].endsWith( "\"" ) ) {
                        mTitle = vals[j].substring(1,vals[j].length()-1);
                      } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append( vals[j].substring(1) );
                        for ( ++j; j<vals_len; ++j ) {
                          if ( vals[j].length() == 0 ) continue;
                          if ( vals[j].endsWith( "\"" ) ) {
                            sb.append(" ").append(vals[j].substring(0, vals[j].length()-1));
                            break;
                          } else {
                            sb.append(" ").append(vals[j] );
                          }
                        }
                        mTitle = sb.toString();
                      }
                    } else {
                      mTitle = vals[j];
                    }
                    break;
                  }
                }
              }

            } else if ( state.in_centerline ) {
              if ( cmd.equals("endcenterline") || cmd.equals("endcentreline") ) {
                // state.in_data = false;
                // state.in_centerline = false;
                // state = popState();
                // TDLog.v("Theron end centerline");
                if ( state.mParent != null ) state = state.mParent;

              } else if ( cmd.equals("date") ) {
                String date = vals[1];
                if ( mDate == null ) mDate = date; // save centerline date
                // TDLog.v("Theron date " + mDate );
              } else if ( cmd.equals("team") ) {
                TDUtil.concatMissings( team, vals, 1 );
              // } else if ( cmd.equals("explo-date") ) {
              // } else if ( cmd.equals("explo-team") ) {
              // } else if ( cmd.equals("instrument") ) {
                // TDLog.v("Theron team " + team );
              } else if ( cmd.equals("calibrate") ) {
                // TDLog.v("Theron calibrate" );
                boolean c_len = false;
                boolean c_ber = false;
                boolean c_cln = false;
		int k = 1;
                for ( ; k<vals_len - 1; ++k ) {
                  if ( vals[k].equals("length") || vals[k].equals("tape") )     c_len = true;
                  if ( vals[k].equals("compass") || vals[k].equals("bearing") ) c_ber = true;
                  if ( vals[k].equals("clino") || vals[k].equals("gradient") )  c_cln = true;
                }
                float zero = 0.0f;
                float scale = 1.0f;
		int kk = 1;
                while ( kk<vals_len-1 ) {
		  try { // try to read the "zero" float (next val)
		    ++kk;
                    zero = Float.parseFloat( vals[kk] );
		    break;
                  } catch ( NumberFormatException e ) {
                    TDLog.Error("Non-number zero");
                  }
		}
                while ( kk<vals_len-1 ) {
		  try { // try to read the "scale" float (next val)
		    ++kk;
                    scale  = Float.parseFloat( vals[kk] );
		    break;
                  } catch ( NumberFormatException e ) {
                    TDLog.Error("Non-number scale");
                  }
                }

                if ( c_len ) {
                  state.mZeroLen  = zero;
                  state.mScaleLen = scale;
                }
                if ( c_ber ) {
                  state.mZeroBer  = zero;
                  state.mScaleBer = scale;
                }
                if ( c_cln ) {
                  state.mZeroCln  = zero;
                  state.mScaleCln = scale;
                }
              } else if ( cmd.equals("units") ) { // units quantity_list [factor] unit
                // TDLog.v("Therion units" );
                boolean u_len = false;
                boolean u_ber = false;
                boolean u_cln = false;
                boolean u_left  = false;
                boolean u_right = false;
                boolean u_up    = false;
                boolean u_down  = false;
                for ( int k=1; k<vals_len - 1; ++k ) {
                  if ( vals[k].equals("length")  || vals[k].equals("tape") )     u_len = true;
                  if ( vals[k].equals("compass") || vals[k].equals("bearing") )  u_ber = true;
                  if ( vals[k].equals("clino")   || vals[k].equals("gradient") ) u_cln = true;
                }
                float factor = 1.0f;
                try {
                  factor = Float.parseFloat( vals[vals_len-2] );
                } catch ( NumberFormatException e ) {
                  // TDLog.v( "Warning: therion units without factor (assuming 1) " + line ); // this is OK
                }
                if ( u_len || u_left || u_right || u_up || u_down ) {
                  float len = factor * ParserUtil.parseLengthUnit( vals[vals_len-1] );
                  if ( u_len )   state.mUnitLen = len;
                } 
                if ( u_ber || u_cln ) {
                  float angle = factor * ParserUtil.parseAngleUnit( vals[vals_len-1] );
                  if ( u_ber ) state.mUnitBer = angle;
                  if ( u_cln ) state.mUnitCln = angle;
                }
              } else if ( cmd.equals("sd") ) {
                // TDLog.v("Warning: therion sd ignored" );
              } else if ( cmd.equals("grade") ) {
                // TDLog.v("Warning: therion grade ignored" );
              } else if ( cmd.equals("declination") ) { 
                // TDLog.v("Therion declination " + line );
                if ( 1 < vals_len ) {
		  if ( vals[1].equals("-") ) { // declination reset
                    state.mDeclination = ( state.mParent == null )? 0 : state.mParent.mDeclination;
		  } else {
                    try {
                      float declination = Float.parseFloat( vals[1] );
                      if ( 2 < vals_len ) {
                      declination *= ParserUtil.parseAngleUnit( vals[2] );
                      }
                      state.mDeclination = declination;
                      if ( ! mApplyDeclination ) mDeclination = state.mDeclination;
                    } catch ( NumberFormatException e ) {
                      TDLog.Error( "Error therion declination " + line );
                    }
		  }
                }      
              } else if ( cmd.equals("instrument") ) {
                // TDLog.v("Warning: therion instrument ignored" );
              } else if ( cmd.equals("flags") ) {
                // TDLog.v("Therion flags");
                if ( vals_len >= 2 ) {
                  if ( vals[1].startsWith("dup") || vals[1].startsWith("splay") ) {
                    state.mDuplicate = true;
                  } else if ( vals[1].startsWith("surf") ) {
                    state.mSurface = true;
                  } else if ( vals[1].equals("not") && vals_len >= 3 ) {
                    if ( vals[2].startsWith("dup") || vals[2].startsWith("splay") ) {
                      state.mDuplicate = false;
                    } else if ( vals[2].startsWith("surf") ) {
                      state.mSurface = false;
                    }
                  }
                }
              } else if ( cmd.equals("cs") ) { 
                // TDLog.v("Warning: therion cs ignored" );
                // TODO cs
              } else if ( cmd.equals("mark") ) { // ***** fix station east north Z (ignored std-dev's)
                // TDLog.v("Therion mark");
                String flag_str = vals[ vals_len - 1 ];
                int flag = 0;
                if ( "painted".equals( vals[ vals_len-1 ] ) ) {
                  flag = StationFlag.STATION_PAINTED;
                } else if ( "fixed".equals( vals[ vals_len-1 ] ) ) {
                  flag = StationFlag.STATION_FIXED;
                }
                // TDLog.v( "Therion parser: mark flag " + flag + " " + flag_str );
                if ( flag != 0 ) {
                  for ( int k=1; k<vals_len-1; ++k ) {
                    String name = extractStationName( vals[k], surveyPath );
                    // TDLog.v( "mark station " + name );
                    boolean must_add = true;
                    for ( Station st : stations ) if ( st.name.equals( name ) ) {
                      must_add = false;
                      st.flag = flag;
                      break;
                    }
                    if ( must_add ) stations.add( new Station( name, "", flag ) );
                  }
                }   
                
              } else if ( cmd.equals("station") ) { // ***** station name "comment"
                // TDLog.v("Therion station");
                if ( vals_len > 2 ) {
                  String name = extractStationName( vals[1], surveyPath );
                  String comment = vals[2];
                  if ( comment.startsWith( "\"" ) ) {
                    if ( comment.endsWith( "\"" ) ) {
                      comment = comment.substring(1,comment.length()-1);
                    } else {
                      StringBuilder sb = new StringBuilder();
                      sb.append( comment.substring( 1 ) );
                      for ( int kk=3; kk<vals_len; ++kk ) {
                        if ( vals[kk].endsWith("\"") ) {
                          sb.append(" ");
                          sb.append( vals[kk].substring(0, vals[kk].length()-1) );
                          break;
                        } else {
                          sb.append(" ");
                          sb.append( vals[kk] );
                        }
                      }
                      comment = sb.toString();
                    }
                  }
                  // TDLog.v( "Therion parser station " + name + " comment <" + comment + ">" );
                  if ( comment.length() > 0 ) {
                    boolean must_add = true;
                    for ( Station st : stations ) if ( st.name.equals( name ) ) { 
                      must_add = false;
                      st.comment = comment;
                      break;
                    }
                    if ( must_add ) stations.add( new Station( name, comment, 0 ) );
                  }
                }
              } else if ( cmd.equals("fix") ) { // ***** fix station east north Z (ignored std-dev's)
                // TDLog.v("Therion fix");
                if ( vals_len > 4 ) {
                  String name = extractStationName( vals[1], surveyPath );
                  try {
	            fixes.add( new ThFix( name,
                                        Float.parseFloat( vals[2] ),
                                        Float.parseFloat( vals[3] ),
                                        Float.parseFloat( vals[4] ) ) );
                  } catch ( NumberFormatException e ) {
                    TDLog.Error( "therion parser error: fix " + line );
                  }
                }
              } else if ( cmd.equals("equate") ) {
                // TDLog.v("Therion equate");
                if ( vals_len > 2 ) {
                  String from, to;
                  int idx = vals[1].indexOf('@');
                  if ( idx > 0 ) {
                    if ( therionPath ) {
                      from = vals[1].substring( 0, idx ) + "@" + path + "." + vals[1].substring(idx+1);
                    } else {
                      from = vals[1].substring( 0, idx );
                    }
                  } else {
                    if ( therionPath ) {
                      from = vals[1] + "@" + path;
                    } else {
                      from = vals[1];
                    }
                  }
                  for ( int j=2; j<vals_len; ++j ) {
                    idx = vals[j].indexOf('@');
                    if ( idx > 0 ) {
                      if ( therionPath ) {
                        to = vals[j].substring( 0, idx ) + "@" + path + "." + vals[j].substring(idx+1);
                      } else {
                        to = vals[j].substring( 0, idx );
                      }
                    } else {
                      if ( therionPath ) {
                        to = vals[j] + "@" + path;
                      } else {
                        to = vals[j];
                      }
                    }
                    shots.add( new ParserShot( state.mPrefix + from + state.mSuffix, state.mPrefix + to + state.mSuffix,
                                         0.0f, 0.0f, 0.0f, 0.0f, 0, LegType.NORMAL, true, false, false, "" ) );
                  }
                }
              } else if ( cmd.startsWith("explo") ) { // explo-date explo-team
                // TDLog.v("Warning therion explo ignored");
              } else if ( cmd.equals("break") ) {
                // TDLog.v("Warning therion break ignored");
              } else if ( cmd.equals("infer") ) {
                // TDLog.v("Warning therion infer ignored");

              } else if ( cmd.equals("group") ) {
                // TDLog.v("Therion group");
                // pushState( state );
                state = new ParserTherionState( state );
              } else if ( cmd.equals("endgroup") ) {
                // TDLog.v("Therion end group");
                // state = popState();
                if ( state.mParent != null ) state = state.mParent;

              } else if ( cmd.equals("walls") ) {
                // TDLog.v("Warning therion walls ignored");
              } else if ( cmd.equals("vthreshold") ) {
                // TDLog.v("Warning therion vthreshold ignored");
              } else if ( cmd.equals("extend") ) { 
                if ( vals_len == 2 ) {
                  state.mExtend = ParserUtil.parseExtend( vals[1], state.mExtend );
                } else { // not implemented "extend value station [station]
                }
                // TDLog.v("Therion extend " + state.mExtend );
              } else if ( cmd.equals("station_names") ) {
                // TDLog.v("Therion station_names");
                state.mPrefix = "";
                state.mSuffix = "";
                if ( vals_len > 1 ) {
                  int off = vals[1].indexOf( '"' );
                  if ( off >= 0 ) {
                    int end = vals[1].lastIndexOf( '"' );
                    state.mPrefix = vals[1].substring(off+1, end );
                  }
                  if ( vals_len > 2 ) {
                    off = vals[2].indexOf( '"' );
                    if ( off >= 0 ) {
                      int end = vals[2].lastIndexOf( '"' );
                      state.mSuffix = vals[2].substring(off+1, end );
                    }
                  }
                }
              } else if ( cmd.equals("data") ) {
                // TDLog.v("Therion data format");
                // data normal from to length compass clino ...
                if ( vals[1].equals("normal") ) {
                  state.data_type = ParserUtil.DATA_NORMAL;
                  jFrom = jTo = jLength = jCompass = jClino = -1;
                  jLeft = jUp = jRight  = jDown = -1;
                  int j0 = 0;
                  for ( int j=2; j < vals_len; ++j ) {
                    if ( vals[j].equals("from") ) {
                      jFrom = j0; ++j0;
                    } else if ( vals[j].equals("to") ) {
                      jTo = j0; ++j0;
                    } else if ( vals[j].equals("length") || vals[j].equals("tape") ) {
                      jLength = j0; ++j0;
                    } else if ( vals[j].equals("compass") || vals[j].equals("bearing") ) {
                      jCompass = j0; ++j0;
                    } else if ( vals[j].equals("clino") || vals[j].equals("gradient") ) {
                      jClino = j0; ++j0;
                    } else if ( vals[j].equals("left") ) {
                      jLeft  = j0; ++j0;
                    } else if ( vals[j].equals("right") ) {
                      jRight = j0; ++j0;
                    } else if ( vals[j].equals("up") ) {
                      jUp    = j0; ++j0;
                    } else if ( vals[j].equals("down") ) {
                      jDown  = j0; ++j0;
                    } else {
                      ++j0;
                    }
                  }
                  state.in_data = (jFrom >= 0) && (jTo >= 0) && (jLength >= 0) && (jCompass >= 0) && (jClino >= 0);
                // TODO other style syntax
                } else if ( vals[1].equals("topofil") ) {
                  state.data_type = ParserUtil.DATA_TOPOFIL;
                } else if ( vals[1].equals("diving") ) {
                  state.data_type = ParserUtil.DATA_DIVING;
                } else if ( vals[1].equals("cartesian") ) {
                  state.data_type = ParserUtil.DATA_CARTESIAN;
                } else if ( vals[1].equals("cylpolar") ) {
                  state.data_type = ParserUtil.DATA_CYLPOLAR;
                } else if ( vals[1].equals("dimensions") ) {
                  state.data_type = ParserUtil.DATA_DIMENSION;
                } else if ( vals[1].equals("nosurvey") ) {
                  state.data_type = ParserUtil.DATA_NOSURVEY;
                } else {
                  state.data_type = ParserUtil.DATA_NONE;
                }
                // TDLog.v("Therion data format - type " + state.data_type );
              } else if ( state.in_data && vals_len >= 5 ) {
                // TDLog.v("Therion data " + line );
                if ( state.data_type == ParserUtil.DATA_NORMAL ) {
                  try {
                    int sz = vals.length;
                    String from = vals[jFrom];
                    String to   = vals[jTo];
                    float len  = Float.parseFloat( vals[jLength] );
                    float ber  = Float.parseFloat( vals[jCompass] );
                    float cln  = Float.parseFloat( vals[jClino] );

		    // measure = (read - zero)*scale
		    float zLen = state.mZeroLen;
		    float sLen = state.mScaleLen * state.mUnitLen;

                    len = (len - zLen) * sLen;
                    ber = (ber - state.mZeroBer) * state.mScaleBer * state.mUnitBer;
                    if ( mApplyDeclination ) ber += state.mDeclination;
		    // if ( ber < 0 ) { ber += 360; } else if ( ber >= 360 ) { ber -= 360; }
                    ber = TDMath.in360( ber );
                    cln = (cln - state.mZeroCln) * state.mScaleCln * state.mUnitCln;

                    float dist, b;
                    if ( jLeft >= 0 && jLeft < sz ) {
                      dist = (Float.parseFloat( vals[jLeft] ) - zLen) * sLen;
                      // b = ber - 90; if ( b < 0 ) b += 360;
                      b = TDMath.sub90( ber );
                      shots.add( new ParserShot( state.mPrefix + from + state.mSuffix, TDString.EMPTY,
                                 dist, b, 0, 0.0f, state.mExtend, LegType.XSPLAY, state.mDuplicate, state.mSurface, false, "" ) );
                    }
                    if ( jRight >= 0 && jRight < sz ) {
                      dist = (Float.parseFloat( vals[jRight] ) - zLen) * sLen;
                      // b = ber + 90; if ( b >= 360 ) b -= 360;
                      b = TDMath.add90( ber );
                      shots.add( new ParserShot( state.mPrefix + from + state.mSuffix, TDString.EMPTY,
                                 dist, b, 0, 0.0f, state.mExtend, LegType.XSPLAY, state.mDuplicate, state.mSurface, false, "" ) );
                    }
                    if ( jUp >= 0 && jUp < sz ) {
                      dist = (Float.parseFloat( vals[jUp] ) - zLen) * sLen;
                      shots.add( new ParserShot( state.mPrefix + from + state.mSuffix, TDString.EMPTY,
                                 dist, 0, 90, 0.0f, state.mExtend, LegType.XSPLAY, state.mDuplicate, state.mSurface, false, "" ) );
                    }
                    if ( jDown >= 0 && jDown < sz ) {
                      dist = (Float.parseFloat( vals[jDown] ) - zLen) * sLen;
                      shots.add( new ParserShot( state.mPrefix + from + state.mSuffix, TDString.EMPTY,
                                 dist, 0, -90, 0.0f, state.mExtend, LegType.XSPLAY, state.mDuplicate, state.mSurface, false, "" ) );
                    }

                    // TODO add shot
                    if ( to.equals("-") || to.equals(".") ) { // splay shot
                      if ( therionPath ) {
                        from = from + "@" + path;
                      }
                      // FIXME splays
                      shots.add( new ParserShot( state.mPrefix + from + state.mSuffix, TDString.EMPTY,
                                            len, ber, cln, 0.0f,
                                            state.mExtend, LegType.NORMAL, state.mDuplicate, state.mSurface, false, "" ) );
                    } else {
                      if ( therionPath ) {
                        from = from + "@" + path;
                        to   = to + "@" + path;
                      }
                      // TDLog.v( "Parser TH add shot " + from + " -- " + to);
                      shots.add( new ParserShot( state.mPrefix + from + state.mSuffix, state.mPrefix + to + state.mSuffix,
                                           len, ber, cln, 0.0f,
                                           state.mExtend, LegType.NORMAL, state.mDuplicate, state.mSurface, false, "" ) );
                    }
                  } catch ( NumberFormatException e ) {
                    TDLog.Error( "therion parser error: data " + line );
                  }
                }
                // FIXME other data types
              }            
            } else if ( cmd.equals("centerline") || cmd.equals("centreline") ) {
              // TDLog.v("Therion in centerline");
              // pushState( state );
              state = new ParserTherionState( state );
              state.in_centerline = true;
              state.in_data = false;
            } else if ( cmd.equals("endsurvey") ) {
              // TDLog.v("Therion end survey");
              // state = popState();
              if ( state.mParent != null ) state = state.mParent;
	      if ( ks > 0 ) {
                --ks;
              } else {
                TDLog.Error("Parser Therion: endsurvey out of survey");
	      }
              int k_pos = survey_pos[ks];
              path = ( k_pos > 0 )? path.substring(k_pos) : ""; // return to previous survey_pos in path
              state.in_survey = ( ks > 0 );
            }
          }
        }
        line = nextLine( br );
      }
      mTeam = team.toString();

    } catch ( IOException e ) {
      // TODO
      TDLog.Error("IO error " + e.getMessage() );
      throw new ParserException();
    }
    if ( mDate == null ) {
      mDate = TDUtil.currentDate();
    }
    // TDLog.v( "Parser TH shots "+ shots.size() + " splays "+ splays.size() +" fixes "+  fixes.size() );
  }


}
