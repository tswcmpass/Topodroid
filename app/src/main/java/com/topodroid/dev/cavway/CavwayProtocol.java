/* @file DeviceXBLEProtocol.java
 *
 * @author Siwei Tian
 * @date aug 2022
 *
 * @brief TopoDroid DistoX XBLE data protocol
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.dev.cavway;

import com.topodroid.TDX.TopoDroidApp;
import com.topodroid.TDX.ListerHandler;
// import com.topodroid.dev.DataType;
import com.topodroid.dev.DataType;
import com.topodroid.dev.Device;
import com.topodroid.dev.TopoDroidProtocol;
import com.topodroid.packetX.MemoryOctet;
import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDUtil;

import android.content.Context;
// import android.os.Handler;

// import java.io.IOException;

public class CavwayProtocol extends TopoDroidProtocol
{
  private final static int DATA_LEN = 33;

  private final CavwayComm mComm;
  private final ListerHandler mLister;


  public static final int PACKET_REPLY          = 0x10;
  public static final int PACKET_INFO_SERIALNUM = 0x11;
  public static final int PACKET_INFO_FIRMWARE  = 0x12;
  public static final int PACKET_INFO_HARDWARE  = 0x13;
  public static final int PACKET_STATUS         = 0x14;
  public static final int PACKET_WRITE_REPLY    = 0x15;
  public static final int PACKET_COEFF          = 0x16;
  public static final int PACKET_FLASH_CHECKSUM = 0x17;
  public static final int PACKET_INFO_SHOTDATA  = 3;
  public static final int PACKET_INFO_CALIDATA  = 4;
  public static final int PACKET_FLASH_BYTES_1  = 0x18;
  public static final int PACKET_FLASH_BYTES_2  = 0x19;
  public static final int PACKET_SIGNATURE      = 0x1A;

  public static final int PACKET_MEASURE_DATA   = 0x20;
  // public static final int PACKET_SHOT_DATA   = 0x21; // PACKET_MEASURE_DATA | 0x01
  // public static final int PACKET_CALIB_DATA  = 0x22; // PACKET_MEASURE_DATA | 0x02

  public static final int PACKET_NONE           = 0;
  public static final int PACKET_ERROR          = 0x80;

  public String mFirmVer;
  public String mHardVer;
  public byte[] mRepliedData;
  public int mCheckCRC;
  public int mFwOpReturnCode;    //0: OK  1: Flash Error 2: Addr or CRC error

  public byte[] mMeasureDataPacket1;
  public byte[] mMeasureDataPacket2;

  public byte[] mFlashBytes;
  //public int mPacketType;

  private byte[] mPacketBytes;

  /** cstr
   * @param ctx      context
   * @param app      application
   * @param lister   data lister
   * @param device   BT device
   * @param comm     DistoX BLE comm object
   */
  public CavwayProtocol(Context ctx, TopoDroidApp app, ListerHandler lister, Device device, CavwayComm comm )
  {
    super( device, ctx );
    mLister = lister;
    mComm   = comm;
    mRepliedData = new byte[4];
    mFlashBytes  = new byte[256];
    mPacketBytes = new byte[DATA_LEN];
    for ( int k=0; k<DATA_LEN; ++k ) mPacketBytes[k] = (byte)0xa5;
  }

  public int handleCavwayPacket(byte [] packetdata)
  {
    byte flag = packetdata[1];   //leg, err flag
    double d =  (packetdata[2] << 8) + MemoryOctet.toInt( packetdata[4], packetdata[3] );
    double b = MemoryOctet.toInt( packetdata[6], packetdata[5] );   //AZM
    double c = MemoryOctet.toInt( packetdata[8], packetdata[7] );  //INCL
    double r = MemoryOctet.toInt( packetdata[10], packetdata[9] );  //ROLL

    mDistance = d / 1000.0;
    mBearing  = b * 180.0 / 32768.0; // 180/0x8000;
    mClino    = c * 90.0  / 16384.0; // 90/0x4000;
    if ( c >= 32768 ) { mClino = (65536 - c) * (-90.0) / 16384.0; }
    mRoll  = r * 180.0 / 32768.0; // 180/0x8000;

    double acc = MemoryOctet.toInt( packetdata[12], packetdata[11] );
    double mag = MemoryOctet.toInt( packetdata[14], packetdata[13] );
    double dip = MemoryOctet.toInt( packetdata[16], packetdata[15] );
    mAcceleration = acc;
    mMagnetic = mag;
    mDip = dip * 90.0  / 16384.0; // 90/0x4000;
    if ( dip >= 32768 ) { mDip = (65536 - dip) * (-90.0) / 16384.0; }

    mGX = MemoryOctet.toInt( packetdata[22], packetdata[21] );
    mGY = MemoryOctet.toInt( packetdata[24], packetdata[23] );
    mGZ = MemoryOctet.toInt( packetdata[26], packetdata[25] );
    if ( mGX > TDUtil.ZERO ) mGX = mGX - TDUtil.NEG;
    if ( mGY > TDUtil.ZERO ) mGY = mGY - TDUtil.NEG;
    if ( mGZ > TDUtil.ZERO ) mGZ = mGZ - TDUtil.NEG;

    mMX = MemoryOctet.toInt( packetdata[28], packetdata[27] );
    mMY = MemoryOctet.toInt( packetdata[30], packetdata[29] );
    mMZ = MemoryOctet.toInt( packetdata[32], packetdata[31] );
    if ( mMX > TDUtil.ZERO ) mMX = mMX - TDUtil.NEG;
    if ( mMY > TDUtil.ZERO ) mMY = mMY - TDUtil.NEG;
    if ( mMZ > TDUtil.ZERO ) mMZ = mMZ - TDUtil.NEG;

    if(packetdata[0] == 0x01)
      return DataType.PACKET_DATA;
    else if(packetdata[0] == 0x02)
      return DataType.PACKET_G;   //definite a new identifier? PACKET_G not suitable
    else return PACKET_NONE;
  }

  /** process a data array
   * @param databuf  input data array, 
   *        length is 16 for shot data, otherwise is a command reply
   *                   5 for flash checksum (0x3b)
   *                   3 for hw signature (0x3c)
   *        offset  0 is command: it can be 0x3a 0x3b 0x3c 0x3d 0x3e
   *        offsets 1,2 contain address (0x3d 0x3e), reply (0x3c)
   *        offset  3 payload length (0x3d 0x3e)
   * @return packet type
   */
  public int packetProcess( byte[] databuf )
  {
    if ( databuf.length == 0 ) {
      TDLog.e("XBLE proto 0-length data");
      return PACKET_NONE;
    }
    if ( (databuf[0] == MemoryOctet.BYTE_PACKET_DATA || databuf[0] == MemoryOctet.BYTE_PACKET_G ) && databuf.length == DATA_LEN ) { // shot / calib data
      if ( mComm.isDownloading() ) {
        for ( int kk=0; kk<DATA_LEN; ++kk ) {
          if ( mPacketBytes[kk] != databuf[kk] ) { // new packet data: send ack depends on handling packets
            System.arraycopy( databuf, 0, mPacketBytes, 0, DATA_LEN );
            int res = handleCavwayPacket(mPacketBytes);
            if ( res != PACKET_NONE) {
              mComm.sendCommand(mPacketBytes[1] | 0x55);
              if(res == DataType.PACKET_G) {
                mComm.handleRegularPacket( res, mLister, 0 );
                res = DataType.PACKET_M;
              }
              mComm.handleRegularPacket(res, mLister, 0);
              return PACKET_MEASURE_DATA; // with ( PACKET_MEASURE_DATA | databuf[0]) shots would be distinguished from calib
            } else {
              return PACKET_ERROR; // break for loop
            }
          }
        }
      } else {
        TDLog.Error("XBLE not downloading");
        return PACKET_NONE;
      }
    } else { // command packet
      byte command = databuf[0];
      if ( command == MemoryOctet.BYTE_PACKET_3D || command == MemoryOctet.BYTE_PACKET_3E ) { // 0x3d or 0x3e
        int addr = (databuf[2] << 8 | (databuf[1] & 0xff)) & 0xFFFF;
        int len = databuf[3];
        mRepliedData = new byte[len];
        TDLog.v("Cavway command packet " + command + " length " + len );
        for (int i = 0; i < len; i++)
          mRepliedData[i] = databuf[i + 4];
        if (addr == CavwayDetails.FIRMWARE_ADDRESS) {
          mFirmVer = Integer.toString(databuf[4]) + "." + Integer.toString(databuf[5]) + "." + Integer.toString(databuf[6]);
          TDLog.v("Cavway fw " + mFirmVer );
          return PACKET_INFO_FIRMWARE;
        } else if (addr == CavwayDetails.HARDWARE_ADDRESS) {
          float HardVer = ((float) databuf[4]) / 10;
          mHardVer = Float.toString(HardVer);
          return PACKET_INFO_HARDWARE;
        } else if (addr == CavwayDetails.STATUS_ADDRESS) {
            StringBuilder sb = new StringBuilder(); // DEBUG
            sb.append("STATUS length ").append( Integer.toString( len ) ).append(": ");
            for (int i=0; i<len; ++i ) sb.append( String.format(" 0x%02x", mRepliedData[i] ) );
            TDLog.v( sb.toString() );
          return PACKET_STATUS;
        } else if ( command == MemoryOctet.BYTE_PACKET_3D ) { // 0x3d
          return PACKET_REPLY;
        } else if ( command == MemoryOctet.BYTE_PACKET_3E ) { // 0x3e
          return PACKET_WRITE_REPLY;
        // } else {
        //   return PACKET_ERROR;
        }
      } else if ( command == MemoryOctet.BYTE_PACKET_HW_CODE ) { // 0x3c: signature: hardware ver. - 0x3d 0x3e only works in App mode not in the bootloader mode.
        // 0x3a 0x3b 0x3c are commands work in bootloader mode
        // TDLog.v("Cavway command packet " + command + " (signature) length " + databuf.length );
        if ( databuf.length == 3 ) { 
          mRepliedData[0] = databuf[1];
          mRepliedData[1] = databuf[2];
          return PACKET_SIGNATURE;
        }
      } else if ( databuf[0] == MemoryOctet.BYTE_PACKET_FW_WRITE ) { // 0x3b
        // TDLog.v("XBLE command packet " + command + " (checksum) length " + databuf.length );
        if ( databuf.length == 6 ) {
          mFwOpReturnCode = databuf[3];
          mCheckCRC = ((databuf[5] << 8) | (databuf[4] & 0xff)) & 0xffff;
          return PACKET_FLASH_CHECKSUM;
        }
      } else if ( command == MemoryOctet.BYTE_PACKET_FW_READ && (databuf.length == 131 || databuf.length == 133)) {   // 0x3a: 3 headers + 128 payloadsda
        // TDLog.v("Cavway command packet " + command + " (firmware) length " + databuf.length );
        if ( databuf[2] == 0x00 ) {        // firmware first packet (MTU=247)
          for ( int i=3; i<131; i++) mFlashBytes[i-3] = databuf[i]; // databuf is copied from offset 3
          return PACKET_FLASH_BYTES_1;
        } else if ( databuf[2] == 0x01 && databuf.length == 133) {   // firmware second packet, with 2 bytes CRC at the end
          for ( int i=3; i<131; i++) mFlashBytes[i+128-3] = databuf[i];
          mCheckCRC = ((databuf[132] << 8) | (databuf[131] & 0xff)) & 0xffff;
          return PACKET_FLASH_BYTES_2;
        // } else {
        //   // TDLog.Error("Cavway ...");
        //   return PACKET_ERROR;
        }
      }
    }
    return PACKET_ERROR;
  }

}
