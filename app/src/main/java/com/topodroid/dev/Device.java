/* @file Device.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid bluetooth device object
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.dev;

// import androidx.annotation.RecentlyNonNull;

// import com.topodroid.utils.TDLog;

// SIWEI_TIAN changed on Jun 2022
public class Device
{
  // DistoX2 / SAP6 commands
  public static final int LASER_ON         =  1; // 0x36
  public static final int LASER_OFF        =  0; // 0x37
  public static final int MEASURE          =  2; // 0x38
  public static final int MEASURE_DOWNLOAD =  3; // 0x38
  public static final int CALIB_START      = 11; // 0x31 // FIXME_SAP6
  public static final int CALIB_STOP       = 10; // 0x30
  public static final int DEVICE_OFF       = 20; // 0x34

  // FIXME VirtualDistoX
  // static final String ZERO_ADDRESS = "00-00-00-00-00-00";

  private final String mAddress; // device mac address
  public String mModel;    // device model (type string)
  public String mName;     // device name (X310 only)
  public String mNickname; // device nickname
  public  int mType;       // device type
  private int mHead;
  private int mTail;

  public final static int DISTO_NONE   =  0; // device types - used as index in the arrays below
  public final static int DISTO_A3     =  1;
  public final static int DISTO_X310   =  2;
  // public final static int DISTO_X000   =  3; // FIXME VirtualDistoX
  // public final static int DISTO_BLEX   =  4;
  public final static int DISTO_SAP5   =  5; 
  public final static int DISTO_BRIC4  =  6;
  public final static int DISTO_XBLE   =  7; // SIWEI_TIAN
  public final static int DISTO_SAP6   =  8; 
  public final static int DISTO_BRIC5  =  9;
  public final static int DISTO_CAVWAY = 10;

  // SIWEI_TIAN                                       0          1         2          3          4       5       6        7            8       9        10
  final static String[] typeString               = { "Unknown", "A3",     "X310",    "X000",    "BLEX", "SAP5", "BRIC4", "XBLE",      "SAP6", "BRIC5", "CVWY1" };
  private final static String[] typeSimpleString = { "Unknown", "DistoX", "DistoX2", "DistoX0", "BleX", "Sap5", "Bric4", "DistoXBLE", "Sap6", "Bric5", "Cavway" };
  
  public static String typeToString( int type )
  {
    if ( type < 0 || type >= typeString.length ) return null;
    return typeString[ type ];
  }

  public String getAddress() { return mAddress; }

  // check if this device has given address or nickname
  public boolean hasAddressOrNickname( String addr )
  {
    return mAddress.equals( addr ) || ( mNickname != null && mNickname.equals( addr ) );
  }

  public boolean isBT( )  { return mType == DISTO_X310  || mType == DISTO_A3; }
  public boolean isBLE( ) { return mType == DISTO_XBLE || mType == DISTO_BRIC4 || mType == DISTO_BRIC5 
                                || mType == DISTO_SAP5 || mType == DISTO_SAP6  || mType == DISTO_CAVWAY; } // SIWEI_TIAN
  public static boolean isBle( int type ) { return type == DISTO_XBLE || type == DISTO_BRIC4 || type == DISTO_BRIC5
                                               ||  type == DISTO_SAP5 || type == DISTO_SAP6  || type == DISTO_CAVWAY ; } // SIWEI_TIAN

  public boolean isDistoX( )    { return mType == DISTO_X310  || mType == DISTO_A3; }
  public boolean isA3( )        { return mType == DISTO_A3; }
  public boolean isX310( )      { return mType == DISTO_X310; }
  public boolean isBric( )      { return mType == DISTO_BRIC4 || mType == DISTO_BRIC5; }
  public boolean isBric4( )     { return mType == DISTO_BRIC4; }
  public boolean isBric5( )     { return mType == DISTO_BRIC5; }
  public boolean isDistoXBLE( ) { return mType == DISTO_XBLE; } // SIWEI_TIAN
  public boolean isSap( )       { return mType == DISTO_SAP5 || mType == DISTO_SAP6; }
  public boolean isSap5( )      { return mType == DISTO_SAP5; }
  public boolean isSap6( )      { return mType == DISTO_SAP6; }
  public boolean isCavway()     { return mType == DISTO_CAVWAY; }

  public static boolean isDistoX( int type )    { return type == DISTO_X310 || type == DISTO_A3; }
  public static boolean isA3( int type )        { return type == DISTO_A3; }
  public static boolean isX310( int type )      { return type == DISTO_X310; }
  public static boolean isBric( int type )      { return type == DISTO_BRIC4 || type == DISTO_BRIC5; }
  public static boolean isBric4( int type )     { return type == DISTO_BRIC4; }
  public static boolean isBric5( int type )     { return type == DISTO_BRIC5; }
  public static boolean isDistoXBLE( int type ) { return type == DISTO_XBLE; } // SIWEI_TIAN
  public static boolean isSap( int type )       { return type == DISTO_SAP5 || type == DISTO_SAP6; }
  public static boolean isSap5( int type )      { return type == DISTO_SAP5; }
  public static boolean isSap6( int type )      { return type == DISTO_SAP6; }
  public static boolean isCavway( int type )    { return type == DISTO_CAVWAY; }

  // SIWEI_TIAN
  // public boolean canSendCommand() { return mType == DISTO_X310 mType == DISTO_XBLE || mType == DISTO_BRIC4 || mType == DISTO_BRIC5 || mType == DISTO_SAP6; }
  // public static boolean canSendCommand( int type ) { return type == DISTO_X310 || type == DISTO_XBLE type == DISTO_BRIC4 || type == DISTO_BRIC5 || type == DISTO_SAP6; }

  // supported BLE models
  final static String[] mBleModels = { "DistoXBLE-", "BRIC4_", "BRIC5_", "Shetland_", "SAP6_", "CAVWAY-" };

  /** @return the list of the supported BLE models
   */
  public static String[] getBleModels() { return mBleModels; }

  /** @return the device name given the model string
   * @param model   model string
   */
  public static String modelToName( String model )
  {
    // TDLog.v("DEVICE model to name <" + model + ">");
    if ( model.startsWith("DistoX-") ) {
      return model.replace("DistoX-", "" );
    } 
    if ( model.startsWith("Shetland") ) {
      return model.replace("Shetland_", "" );
    }
    if ( model.startsWith("SAP5_") ) {
      return model.replace("SAP5_", "" );
    }
    if ( model.startsWith("SAP6_") ) {
      return model.replace("SAP6_", "" );
    }
    if ( model.startsWith("BRIC4_") ) {
      return model.replace("BRIC4_", "" );
    }
    if ( model.startsWith("BRIC5_") ) {
      return model.replace("BRIC5_", "" );
    }
    if ( model.startsWith("DistoXBLE-") ) { // SIWEI_TIAN
      return model.replace("DistoXBLE-", "" );
    }
    if ( model.startsWith("CavwayX1-") ) {
      return model.replace("CavwayX1-", "" );
    }
    // if ( model.startsWith("Ble-") ) { // FIXME BLE_5
    //   return model.replace("Ble-", "" );
    // }
    return "--";
  }

  /** @return the integer code given the model string
   * @param model   model string
   */
  public static int modelToType( String model ) 
  {
    if ( model != null ) {
      // TDLog.v( "DEVICE modelToType " + model );
      if ( model.equals( "XBLE" )  || model.startsWith( "DistoXBLE-" ) )  return DISTO_XBLE; // SIWEI_TIAN
      if ( model.equals( "X310" )  || model.startsWith( "DistoX-" ) )   return DISTO_X310;
      if ( model.equals( "A3" )    || model.equals( "DistoX" ) )        return DISTO_A3;
      if ( model.equals( "BRIC4" ) || model.startsWith( "BRIC4" ) )     return DISTO_BRIC4; 
      if ( model.equals( "BRIC5" ) || model.startsWith( "BRIC5" ) )     return DISTO_BRIC5; 
      if ( model.equals( "SAP5" )  || model.startsWith( "Shetland_" ) ) return DISTO_SAP5;
      if ( model.equals( "SAP6" )  /* || model.startsWith( "Shetland_" ) */ ) return DISTO_SAP6;
      if ( model.equals( "Cavway" )|| model.startsWith("CavwayX1-")     ) return DISTO_CAVWAY;
      // if ( model.equals( "BLEX" ) ) return DISTO_BLEX; // FIXME BLE_5
      // if ( model.equals( "X000" ) || model.equals( "DistoX0" ) ) return DISTO_X000; // FIXME VirtualDistoX
    }
    return DISTO_NONE;
  }

  /** @return the string presentation of the device type
   * @param model   model string
   */
  public static String modelToString( String model ) 
  {
    return typeString[ modelToType( model ) ];
  }

  // -------------------------------------------------------------------------------

  /** cstr
   * @param addr     bluetooth address
   * @param model    model string
   * @param h        head (only for A3) 
   * @param t        tail (only for A3)
   * @param name     ???
   * @param nickname device nickname (can be null)
   */
  public Device( String addr, String model, int h, int t, String name, String nickname )
  {
    // TDLog.v( "[1] Device: " + addr + " " + model + " " + name + " addr " + addr );
    mAddress = addr;
    mModel = model;
    mType = modelToType( model );
    mName = fromName( name );
    mNickname = nickname;
    mHead = h;
    mTail = t;
  }

  /** cstr
   * @param addr     bluetooth address
   * @param model    model string
   * @param name     ???
   * @param nickname device nickname (can be null)
   */
  public Device( String addr, String model, String name, String nickname )
  {
    // TDLog.v( "[2] Device: " + addr + " " + model + " " + name + " addr " + addr );
    mAddress = addr;
    mModel = model;
    mType = modelToType( model );
    mName = fromName( name );
    mNickname = nickname;
    mHead = 0;
    mTail = 0;
  }

  // public void dump( )
  // {
  //   TDLog.v( "Device addr " + mAddress + " model " + mModel + " type " + mType + " name " + mName + " nick " + ((mNickname == null)? "null" : mNickname ) );
  // }

  private String fromName( String name )
  {
    // TDLog.v( "DEVICE from name <" + name + "> model <" + mModel + ">" );
    if ( name != null ) name = name.trim();
    if ( name == null || name.length() == 0 || name.equals("null") ){
      name = mModel;
    }
    if ( name.startsWith("DistoXBLE-") ) return name.replace("DistoXBLE-", ""); // SIWEI_TIAN
    if ( name.startsWith("DistoX-") )    return name.replace("DistoX-", "");
    if ( name.startsWith("SAP6-" ) )     return name.replace("SAP6-", "");
    if ( name.startsWith("SAP-" ) )      return name.replace("SAP-", "");
    if ( name.startsWith("BRIC-" ) )     return name.replace("BRIC-", "");
    if ( name.startsWith("BRIC4-" ) )    return name.replace("BRIC4-", "");
    if ( name.startsWith("BRIC5-" ) )    return name.replace("BRIC5-", "");
    if ( name.startsWith("CavwayX1-" ) )   return name.replace("CavwayX1-", "");
    return name;
  }

  /** @return the device nickname, if not null, or the name, otherwise
   */
  public String getNickname()
  {
    if ( mNickname != null && mNickname.length() > 0 ) return mNickname;
    return mName;
  }

  /** @return true if the device supports firmware
   *
   * @note X310 is the only device that has firmware support 
   */
  public boolean hasFirmwareSupport() { return mType == DISTO_X310 || mType == DISTO_XBLE; }

  /** @return string presentation
   */
  // @RecentlyNonNull
  public String toString()
  { 
    // TDLog.v( "to String <" + mName + "> type <" + mType + "> <" + typeString[ mType ] +">" );
    if ( mNickname != null && mNickname.length() > 0 ) {
      return typeString[ mType ] + " " + mName + " " + mNickname;
    }
    return typeString[ mType ] + " " + mName + " " + mAddress;
  }

  /** @return simple string presentation
   */
  public String toSimpleString() { return typeSimpleString[ mType ] + " " + mName; }
  
}
