/** @file Cave3DSurvey.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief 3D: survey
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDString;

import java.util.List;
import java.util.ArrayList;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class Cave3DSurvey
{
  private static int msCount = 0;
  static void resetCount() { msCount = 0; }

  public int number; // survey index
  int mId;    // id 
  int mPid;   // parent Id
  String name;
  boolean visible;
  private int color;

  ArrayList< Cave3DShot > mShots;
  ArrayList< Cave3DShot > mSplays;
  ArrayList< Cave3DStation > mStations;

  // int mNrShots;
  // int mNrSplays;
  double mLenShots;
  double mLenSplays;

  /** @return the survey ID
   */
  public int getId() { return mId; }

  /** serialize the 3D survey
   * @param dos   output stream
   */
  void serialize( DataOutputStream dos ) throws IOException
  {
    dos.writeInt( mId );
    dos.writeUTF( name );
    dos.writeInt( color );
  }

  /** @return survey color (int AARRGGBB) value
   */
  public int getColor() { return color; }

  /** set the survey color
   * @param col color
   * @note used by parser tdconfig
   */
  public void setColor( int col ) { color = col; }


  /** serialize a 3D survey
   * @param dis   input stream
   * @param version  stream version
   * @return deserialized 3D survey
   */
  static Cave3DSurvey deserialize( DataInputStream dis, int version )
  {
    int id    = 0;
    String nm = "survey";
    int col   = 0;
    try {
      id = dis.readInt();
      nm = dis.readUTF();
      col = dis.readInt();
    } catch ( IOException e ) {
      TDLog.Error( e.getMessage() );
    }
    // TDLog.v("Cave3D survey deserialized " + id + " " + nm + " color " + col );
    return new Cave3DSurvey( nm, id, -1, col );
  }

  // ------------------------------------------------------ 

  /** cstr
   * @param nm   name
   * @param col  color
   */
  public Cave3DSurvey( String nm, int col )
  {
    number = msCount; ++ msCount;
    init( nm, -1, -1, col );
  }

  /** cstr
   * @param n   name
   * @param id  id
   * @param pid parent id
   * @param col  color
   */
  public Cave3DSurvey( String n, int id, int pid, int col )
  {
    number = msCount; ++ msCount;
    init( n, id, pid, col );
  }

  /** @return true if the 3D station has a specified name
   * @param nm  name
   */
  public boolean hasName( String nm ) { return name != null && name.equals( nm ); }

  /** @return the station name
   */
  public String getName() { return name; }

  // void addShot( String from, String to, double len, double ber, double cln ) { addShot( new Cave3DShot( from, to, ber, len, cln, 0, 0 ) ); }
  
  // void addSplay( String from, double len, double ber, double cln ) { addSplay( new Cave3DShot( from, null, ber, len, cln, 0, 0 ) ); }

  /** add a shot to the station
   * @param sh   shot to add
   */
  public void addShot( Cave3DShot sh ) 
  { 
    mShots.add( sh );
    sh.setSurvey( this );
    addStation( sh.from_station ); // make sure shot stations are linked by the survey (included in the survey stations)
    addStation( sh.to_station );
    // mNrShots ++;
    mLenShots += sh.len;
  }

  /** add a splay to the station
   * @param sh   splay to add
   */
  public void addSplay( Cave3DShot sh )
  {
    mSplays.add( sh );
    sh.setSurvey( this );
    // mNrSplays ++;
    mLenSplays += sh.len;
  }

  /** add a station to the survey stations
   * @param st   station to add
   */
  public Cave3DStation addStation( Cave3DStation st )
  { 
    if ( ! mStations.contains( st ) ) {
      mStations.add( st );
      st.setSurvey( this );
      // TDLog.v("3D survey add station " + st.getShortName() + " size " + mStations.size() );
    }
    return st;
  }

  /** @return the station with a given name
   * @param name   name of the station
   */
  public Cave3DStation getStation( String name ) 
  {
    if ( TDString.isNullOrEmpty( name ) ) return null;
    if ( name.equals("-") || name.equals(".") ) return null;
    for ( Cave3DStation st : mStations ) if ( name.equals( st.getFullName() ) ) return st;
    return null;
  }

  /** @return list of shots
   */
  public List< Cave3DShot > getShots()       { return mShots; }

  /** @return list of splays
   */
  public List< Cave3DShot > getSplays()      { return mSplays; }

  /** @return list of stations
   */
  public List< Cave3DStation > getStations() { return mStations; }

  // --------------------------- DATA REDUCTION
  // this data reduction is never used
  //
  // /** compute the data reduction
  //  */
  // void reduce()
  // {
  //   TDLog.v("3D reduce survey " + name ); 
  //   mLenShots  = 0.0;
  //   mLenSplays = 0.0;
  //   addStation( new Cave3DStation( mShots.get(0).from, 0f, 0f, 0f ) );
  //   int used_shots = 0; // check connectedness
  //   int size = 0;
  //   while ( size < mStations.size() ) {
  //     size = mStations.size();
  //     for ( Cave3DShot sh : mShots ) {
  //       if ( sh.hasSurvey() ) continue;
  //       Cave3DStation fr = getStation( sh.from );
  //       if ( fr != null ) {
  //         sh.from_station = fr;
  //         markShotUsed( sh );
  //         ++used_shots;
  //         Cave3DStation to = getStation( sh.to, size );
  //         if ( to == null ) to = addStation( sh.getStationFromStation( fr ) );
  //         sh.to_station = to;
  //       } else {
  //         Cave3DStation to = getStation( sh.to, size );
  //         if ( to != null ) {
  //           sh.to_station = to;
  //           markShotUsed( sh );
  //           ++used_shots;
  //           sh.from_station = addStation( sh.getStationFromStation( to ) );
  //         }
  //       }
  //     }
  //   }
  //   // TDLog.v("shots " + mShots.size() + " used " + used_shots );
  //   int used_splays = 0; // check
  //   for ( Cave3DShot sp : mSplays ) {
  //     Cave3DStation st = getStation( sp.from );
  //     if ( st != null ) {
  //       sp.from_station = st;
  //       markSplayUsed( sp );
  //       ++ used_splays;
  //     } else {
  //       st = getStation( sp.to );
  //       if ( st != null ) {
  //         sp.from_station = st;
  //         markSplayUsed( sp );
  //         ++ used_splays;
  //       }
  //     }
  //   }
  //   // TDLog.v("splays " + mSplays.size() + " used " + used_splays );
  // }

  /** @return a station starting from index id
   * @param name   station name
   * @param idx    start index id
   */
  private Cave3DStation getStation( String name, int idx ) 
  {
    if ( TDString.isNullOrEmpty( name ) ) return null;
    if ( name.equals("-") || name.equals(".") ) return null;
    for ( ; idx < mStations.size(); ++idx ) {
      Cave3DStation st = mStations.get(idx);
      if ( name.equals( st.getFullName() ) ) return st;
    }
    return null;
  }
 
  /** mark a shot "used"
   * @param sh   shot to mark "used"
   */
  private void markShotUsed( Cave3DShot sh )
  {
    mLenShots += sh.len;
    sh.setSurvey( this );
  }
 
  /** mark a splay "used"
   * @param sh   splay to mark "used"
   */
  private void markSplayUsed( Cave3DShot sh )
  {
    mLenSplays += sh.len;
    sh.setSurvey( this );
  }
  //

  // --------------------------- STATS
  /** @return the number of shots
   */
  int getShotNr()    { return mShots.size(); }   // mNrShots

  /** @return the number of splays
   */
  int getSplayNr()   { return mSplays.size(); }  // mNrSplays

  /** @return the number of stations
   */
  int getStationNr() { return mStations.size(); }

  /** @return the total length of the shots
   */
  double getShotLength()  { return mLenShots; }

  /** @return the total length of the splays
   */
  double getSplayLength() { return mLenSplays; }

  // ---------------------------- INIT
  /** initialize
   * @param nm  name
   * @param id  id
   * @param pid parent id
   * @param col  color
   */
  private void init( String nm, int id, int pid, int col )
  {
    // TDLog.v("Cave3DSurvey init  " + nm + " id " + id + "/" + pid + " color " + (col & 0xffffff) );
    mId  = id;
    mPid = pid;
    name = nm;
    visible = true;
    color   = col;
    if ( color == 0 ) color = TglColor.getSurveyColor(); // random survey color
    // mNrShots  = 0;
    // mNrSplays = 0;
    mShots    = new ArrayList< Cave3DShot >();
    mSplays   = new ArrayList< Cave3DShot >();
    mStations = new ArrayList< Cave3DStation >();
  }
}


