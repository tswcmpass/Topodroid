/* @file BleUtils.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief Bluetooth low-energy utility functions and constants
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.dev.ble;

import com.topodroid.utils.TDLog;
// import com.topodroid.TDX.TDToast;

// import android.bluetooth.BluetoothDevice;
// import android.bluetooth.BluetoothProfile;
// import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattCharacteristic;
// import android.bluetooth.BluetoothGattCallback;
// import android.bluetooth.BluetoothGattService;

import java.util.UUID;
// import java.util.Arrays;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BleUtils
{
  // ------------------------------------------------------------------------------
  // UTILITIES

  public final static String STANDARD_UUID =  "-0000-1000-8000-00805f9b34fb";

  public final static String CCCD_UUID_STR = "00002902-0000-1000-8000-00805f9b34fb"; // 20230118 new string
  public final static UUID CCCD_UUID = UUID.fromString( CCCD_UUID_STR );
  
  // ------------------------------------------------------------------------------
  /* DEBUG
  
  public static String deviceToString( BluetoothDevice device )
  {
    String name = null;
    try {
      name = device.getName();
    } catch ( SecurityException e ) {
      TDLog.e("SECURITY device name " + e.getMessage() );
      // TDToast.makeBad("Security error: device name");
      return "-- --";
    }
    if ( name == null ) name = "--";
    return name + " " + device.getAddress();
  }

  public static String chrtPropString( BluetoothGattCharacteristic chrt )
  {
    int prop = chrt.getProperties();
    String B = isChrtPBcast(prop)? "B" : ".";
    String I = isChrtPIndicate(prop)? "I" : ".";
    String N = isChrtPNotify(prop)? "N" : ".";
    String r = isChrtPRead(prop)? "R" : ".";
    String w = isChrtPWrite(prop)? "W" : ".";
    String s = isChrtPWriteSign(prop)? "Ws" : ".";
    String n = isChrtPWriteNoResp(prop)? "Wn" : ".";
    String write_type = "";
    if ( canChrtPWrite( chrt ) ) {
      int type = chrt.getWriteType();
      write_type = (isChrtWDflt(type)? "d" : "-") + (isChrtWNoResp(type)? "n" : "-") + (isChrtWSign(type)? "s" : "-");
    }
    return " props(" + prop + ") " + B + I + N + " " + r + w + s + n + " w-type " + write_type;
  } 

  public static String chrtPermString( BluetoothGattCharacteristic chrt )
  {
    int perm = chrt.getPermissions();
    boolean r = isChrtRead(perm)  || isChrtReadEnc(perm);
    boolean w = isChrtWrite(perm) || isChrtWriteEnc(perm) || isChrtWriteSign(perm);
    return " perms(" + perm + ") " + (r? "R" : ".") + (w? "W" : ".");
  } 

  public static String descPermString( BluetoothGattDescriptor desc )
  {
    int perm = desc.getPermissions();
    boolean r = isDescRead(perm)  || isDescReadEnc(perm);
    boolean w = isDescWrite(perm) || isDescWriteEnc(perm) || isDescWriteSign(perm);
    return " perms(" + perm + ") " + (r? "R" : ".") + (w? "W" : ".");
  } 

  public static String bytesToString( byte[] bytes )
  {
    if ( bytes == null ) return "null";
    StringBuilder sb = new StringBuilder();
    for ( byte b : bytes ) sb.append( (int)b ).append(" ");
    return sb.toString();
  }
  
  public static String bytesToHexString( byte[] bytes )
  {
    StringBuilder sb = new StringBuilder();
    for ( int k=0; k<bytes.length; ++k ) sb.append( String.format(" %02x", bytes[k] ) );
    return sb.toString();
  }
  */

  /** @return string presentation of a byte array
   * @param bytes  byte array
   */
  public static String bytesToAscii( byte[] bytes )
  {
    if ( bytes == null ) return null;
    StringBuilder sb = new StringBuilder();
    for ( int k=0; k<bytes.length; ++k ) {
      sb.append( (char)(bytes[k]) );
    }
    return sb.toString();
  }

  // ------------------------------------------------------------------------------
  /** @return true if the property has flag "broadcast"
   * @param prop   property
   */
  public static boolean isChrtPBcast( int prop )       { return (prop & BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0; }

  /** @return true if the property has flag "indicate"
   * @param prop   property
   */
  public static boolean isChrtPIndicate( int prop )    { return (prop & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0; }

  /** @return true if the property has flag "notify"
   * @param prop   property
   */
  public static boolean isChrtPNotify( int prop )      { return (prop & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0; }

  /** @return true if the property has flag "read"
   * @param prop   property
   */
  public static boolean isChrtPRead( int prop )        { return (prop & BluetoothGattCharacteristic.PROPERTY_READ) != 0; }

  /** @return true if the property has flag "write"
   * @param prop   property
   */
  public static boolean isChrtPWrite( int prop )       { return (prop & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0; }

  /** @return true if the property has flag "signed write"
   * @param prop   property
   */
  public static boolean isChrtPWriteSign( int prop )   { return (prop & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0; }

  /** @return true if the property has flag "write-no-response"
   * @param prop   property
   */
  public static boolean isChrtPWriteNoResp( int prop ) { return (prop & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0; }

  // // public static boolean canChrtPIndicate( BluetoothGattCharacteristic chrt )
  // // { 
  // //   int prop = chrt.getProperties();
  // //   return isChrtPIndicate( prop );
  // // }

  /** @return true if the characteristic supports notify or indicate
   * @param chrt  characteristic
   */
  public static boolean canChrtPNotify( BluetoothGattCharacteristic chrt )
  { 
    int prop = chrt.getProperties();
    return ( isChrtPIndicate( prop ) || isChrtPNotify( prop ) );
  }

  /** @return the notification/indication value on the characteristic
   * @param chrt   characteristic
   */
  public static byte[] getChrtPNotify( BluetoothGattCharacteristic chrt )
  { 
    int prop = chrt.getProperties();
    if ( isChrtPIndicate( prop ) ) return BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
    if ( isChrtPNotify( prop ) ) return BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
    return null;
  }

  /** @return the indication value on the characteristic
   * @param chrt   characteristic
   */
  public static byte[] getChrtPIndicate( BluetoothGattCharacteristic chrt )
  {
    int prop = chrt.getProperties();
    if ( isChrtPIndicate( prop ) ) return BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
    TDLog.e("char is not INDICATE " + chrt.getUuid().toString() );
    return null;
  }

  /** @return true if the characteristic supports reading
   * @param chrt   characteristic
   */
  public static boolean canChrtPRead( BluetoothGattCharacteristic chrt )
  { 
    return isChrtPRead( chrt.getProperties() );
  }

  /** @return true if the characteristic supports any writing
   * @param chrt   characteristic
   */
  public static boolean canChrtPWrite( BluetoothGattCharacteristic chrt )
  { 
    int prop = chrt.getProperties();
    return isChrtPWrite(prop) || isChrtPWriteSign(prop) || isChrtPWriteNoResp(prop);
  }

  /** @return the characteristic writing type (either default of no-response)
   * @param chrt   characteristic
   */
  public static int getChrtWriteType( BluetoothGattCharacteristic chrt )
  {
    int prop = chrt.getProperties();
    if (  isChrtPWrite(prop) ) return BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
    if ( isChrtPWriteNoResp(prop) ) return BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
    return -1;
  }

  public static boolean isChrtWDflt( int type )         { return (type & BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) != 0; }
  public static boolean isChrtWNoResp( int type )       { return (type & BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) != 0; }
  public static boolean isChrtWSign( int type )         { return (type & BluetoothGattCharacteristic.WRITE_TYPE_SIGNED) != 0; }

  public static boolean isChrtRead( int perm )          { return (perm & BluetoothGattCharacteristic.PERMISSION_READ) != 0; }
  public static boolean isChrtWrite( int perm )         { return (perm & BluetoothGattCharacteristic.PERMISSION_WRITE) != 0; }
  // public static boolean isChrtReadWrite( int perm )     { return isChrtRead( perm ) && isChrtWrite( perm ); }
  public static boolean isChrtReadEnc( int perm )       { return (perm & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED) != 0; }
  public static boolean isChrtWriteEnc( int perm )      { return (perm & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED) != 0; }
  public static boolean isChrtWriteSign( int perm )     { return (perm & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED) != 0; }
  public static boolean isChrtReadEncMitm( int perm )   { return (perm & BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM) != 0; }
  public static boolean isChrtWriteEncMitm( int perm )  { return (perm & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM) != 0; }
  public static boolean isChrtWriteSignMitm( int perm ) { return (perm & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM) != 0; }

  public static boolean isChrtRead( BluetoothGattCharacteristic chrt )
  { 
    int perm = chrt.getPermissions();
    return isChrtRead( perm ) || isChrtReadEnc( perm ) || isChrtReadEncMitm( perm );
  }

  public static boolean isChrtWrite( BluetoothGattCharacteristic chrt )
  { 
    int perm = chrt.getPermissions();
    return isChrtWrite( perm ) || isChrtWriteSign( perm ) || isChrtWriteSignMitm( perm ) || isChrtWriteEnc( perm ) || isChrtWriteEncMitm( perm );
  }

  public static boolean isDescRead( int perm )          { return (perm & BluetoothGattDescriptor.PERMISSION_READ) != 0; }
  public static boolean isDescWrite( int perm )         { return (perm & BluetoothGattDescriptor.PERMISSION_WRITE) != 0; }
  // public static boolean isDescReadWrite( int perm )     { return isDescRead( perm ) && isDescWrite( perm ); }
  public static boolean isDescReadEnc( int perm )       { return (perm & BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED) != 0; }
  public static boolean isDescWriteEnc( int perm )      { return (perm & BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED) != 0; }
  public static boolean isDescWriteSign( int perm )     { return (perm & BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED) != 0; }
  public static boolean isDescReadEncMitm( int perm )   { return (perm & BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM) != 0; }
  public static boolean isDescWriteEncMitm( int perm )  { return (perm & BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM) != 0; }
  public static boolean isDescWriteSignMitm( int perm ) { return (perm & BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM) != 0; }

  public static boolean isDescRead( BluetoothGattDescriptor desc )
  { 
    int perm = desc.getPermissions();
    return isDescRead( perm ) || isDescReadEnc( perm ) || isDescReadEncMitm( perm );
  }

  public static boolean isDescWrite( BluetoothGattDescriptor desc )
  { 
    int perm = desc.getPermissions();
    return isDescWrite( perm ) || isDescWriteSign( perm ) || isDescWriteSignMitm( perm ) || isDescWriteEnc( perm ) || isDescWriteEncMitm( perm );
  }

  // ---------------------------------------------------------------------------------
  /** @return the float (little-endian) encoded in a byte array at a given offset
   * @param bytes  byte array
   * @param offset offset
   */
  public static float getFloat( byte[] bytes, int offset )
  {
    if ( offset+4 > bytes.length ) return 0;
    return ByteBuffer.wrap( bytes ).order(ByteOrder.LITTLE_ENDIAN).getFloat( offset );
  }

  /** @return the int (little-endian) encoded in a byte array at a given offset
   * @param bytes  byte array
   * @param offset offset
   */
  public static int getInt( byte[] bytes, int offset )
  {
    if ( offset+4 > bytes.length ) return 0;
    return ByteBuffer.wrap( bytes ).order(ByteOrder.LITTLE_ENDIAN).getInt( offset );
  }

  /** @return the short (little-endian) encoded in a byte array at a given offset
   * @param bytes  byte array
   * @param offset offset
   */
  public static short getShort( byte[] bytes, int offset )
  {
    if ( offset+2 > bytes.length ) return 0;
    return ByteBuffer.wrap( bytes ).order(ByteOrder.LITTLE_ENDIAN).getShort( offset );
  }

  /** @return the char in a byte array at a given offset
   * @param bytes  byte array
   * @param offset offset
   */
  public static int getChar( byte[] bytes, int offset )
  {
    if ( offset >= bytes.length ) return 0;
    int i = bytes[offset];
    return ( i < 0 )? i+256 : i;
  }

  /** put short (little-endian) encoded in a byte array at a given offset
   * @param bytes  byte array
   * @param offset offset
   * @param s      short value
   */
  public static void putShort( byte[] bytes, int offset, short s )
  {
    if ( offset+2 > bytes.length ) return;
    bytes[offset  ] = (byte)( s & 0xff );
    bytes[offset+1] = (byte)( (s>>8) & 0xff );
  }

  /** put a char in a byte array at a given offset
   * @param bytes  byte array
   * @param offset offset
   * @param c      char value
   */
  public static void putChar( byte[] bytes, int offset, int c )
  {
    if ( offset >= bytes.length ) return;
    bytes[offset] = (byte)c;
  }

}
