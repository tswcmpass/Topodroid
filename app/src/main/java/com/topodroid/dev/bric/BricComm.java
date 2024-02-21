/* @file BricComm.java
 *
 * @author marco corvi
 * @date jan 2021
 *
 * @brief BRIC4 communication 
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 * TopoDroid implementation of BLE callback follows the guidelines of 
 *   Chee Yi Ong,
 *   "The ultimate guide to Android bluetooth low energy"
 *   May 15, 2020
 */
package com.topodroid.dev.bric;

import com.topodroid.dev.Device;
import com.topodroid.dev.ConnectionState;
import com.topodroid.dev.TopoDroidComm;
import com.topodroid.dev.ble.BleComm;
import com.topodroid.dev.ble.BleCallback;
import com.topodroid.dev.ble.BleOperation;
import com.topodroid.dev.ble.BleOpConnect;
import com.topodroid.dev.ble.BleOpDisconnect;
import com.topodroid.dev.ble.BleOpNotify;
import com.topodroid.dev.ble.BleOpChrtRead;
import com.topodroid.dev.ble.BleOpChrtWrite;
import com.topodroid.dev.ble.BleUtils;
import com.topodroid.dev.ble.BleConst;
import com.topodroid.dev.ble.BleBuffer;
import com.topodroid.dev.ble.BleQueue;
import com.topodroid.TDX.TDInstance;
import com.topodroid.TDX.TDToast;
import com.topodroid.TDX.TopoDroidApp;
import com.topodroid.TDX.ListerHandler;
import com.topodroid.TDX.R;
// import com.topodroid.utils.TDUtil;
import com.topodroid.utils.TDLog;
import com.topodroid.prefs.TDSetting;

// import android.os.Looper;
// import android.os.Handler;
import android.content.Context;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
// import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
// import android.bluetooth.BluetoothGattDescriptor;
// import android.bluetooth.BluetoothGattCallback;

// import java.util.ArrayList;
import java.util.Arrays;
// import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.Timer;
import java.util.TimerTask;

public class BricComm extends TopoDroidComm
                      implements BleComm
{
  private BricInfoDialog mBricInfoDialog = null;
  // private BricChrtChanged mChrtChanged = null;

  private ConcurrentLinkedQueue< BleOperation > mOps;
  // private int mPendingCommands; // FIXME COMPOSITE_COMMANDS

  // data buffer types
  final static int DATA_PRIM    = 1;
  final static int DATA_META    = 2;
  final static int DATA_ERR     = 3;
  final static int DATA_TIME    = 4;
  // final static int DATA_INFO_23 = 123;
  // final static int DATA_INFO_24 = 124;
  // final static int DATA_INFO_25 = 125;
  final static int DATA_INFO_26 = 126;
  final static int DATA_INFO_27 = 127;
  final static int DATA_INFO_28 = 128;
  // final static int DATA_INFO_29 = 129;
  final static int DATA_BATTERY_LVL = 219;
  final static int DATA_DEVICE_00   = 300;
  // final static int DATA_DEVICE_01   = 301;
  // final static int DATA_DEVICE_04   = 304;
  // final static int DATA_DEVICE_06   = 306;
  final static int DATA_QUIT = -1;  // used to tell the consumer thread to exit

  private Context mContext;
  BleCallback mCallback;
  private String          mRemoteAddress;
  private BluetoothDevice mRemoteBtDevice;
  private int mDataType;    // packet datatype 
  private BleQueue mQueue; // buffer queue - filled with buffers from the reading
                            // and emptied by the consumer thread
  private boolean mReconnect = false;

  private Timer mTimer = null;
  private long onData = 0;
  private Thread mConsumer = null;

  /** cstr
   * @param ctx        context
   * @param app        application
   * @param address    device address
   * @param bt_device  bluetooth device
   */
  public BricComm( Context ctx, TopoDroidApp app, String address, BluetoothDevice bt_device ) 
  {
    super( app );
    TDLog.v( "BRIC comm cstr - mode " + TDSetting.mBricMode );
    mContext  = ctx;
    mRemoteAddress = address;
    mRemoteBtDevice = bt_device;
    mQueue = new BleQueue();
    mBricInfoDialog = null;
    mConsumer = new Thread(){
      public void run()
      {
        TDLog.v("BRIC comm tread start");
        boolean do_consume = true;
        while ( do_consume ) {
          // TDLog.v( "BRIC comm: Queue size " + mQueue.size );
          BleBuffer buffer = mQueue.get();
          if ( buffer == null ) continue;
          TDLog.v( "BRIC5 comm: Queue buffer type " + buffer.type );
          switch ( buffer.type ) {
            case DATA_PRIM:
              // TDLog.v( "BRIC comm: Queue buffer PRIM");
              // BricDebug.logMeasPrim( buffer.data );
              if ( TDSetting.mBricMode == BricMode.MODE_PRIM_ONLY ) {
                ((BricProto)mProtocol).addMeasPrimAndProcess( buffer.data );
              } else {
                ((BricProto)mProtocol).addMeasPrim( buffer.data ); // ANDROID-11 ANDROID-12 null ptr
              }
              break;
            case DATA_META:
              // TDLog.v( "BRIC comm: Queue buffer META");
              // BricDebug.logMeasMeta( buffer.data );
              if ( TDSetting.mBricMode >= BricMode.MODE_ALL ) {
                ((BricProto)mProtocol).addMeasMeta( buffer.data );
              }
              break;
            case DATA_ERR:
              // TDLog.v( "BRIC comm: Queue buffer ERR");
              // BricDebug.logMeasErr( buffer.data );
              if ( TDSetting.mBricMode >= BricMode.MODE_ALL ) {
                ((BricProto)mProtocol).addMeasErr( buffer.data );
                ((BricProto)mProtocol).processData(); 
              }
              break;
            // case DATA_INFO_24:
            //   // TDLog.v( "BRIC comm: Queue buffer INFO");
            //   // BricDebug.logAscii( buffer.data );
            //   if ( mBricInfoDialog != null ) mBricInfoDialog.setValue( DATA_INFO_24, buffer.data );
            //   break;
            // case DATA_INFO_25:
            //   if ( mBricInfoDialog != null ) mBricInfoDialog.setValue( DATA_INFO_25, buffer.data );
            //   break;
            case DATA_INFO_26:
              if ( mBricInfoDialog != null ) mBricInfoDialog.setValue( DATA_INFO_26, buffer.data );
              break;
            case DATA_INFO_27:
              if ( mBricInfoDialog != null ) mBricInfoDialog.setValue( DATA_INFO_27, buffer.data );
              break;
            case DATA_INFO_28:
              if ( mBricInfoDialog != null ) mBricInfoDialog.setValue( DATA_INFO_28, buffer.data );
              break;
            case DATA_DEVICE_00:
              // TDLog.v( "BRIC comm: Queue buffer DEVICE 00");
              if ( mBricInfoDialog != null ) mBricInfoDialog.setValue( DATA_DEVICE_00, buffer.data );
              break;
            case DATA_BATTERY_LVL:
              // TDLog.v( "BRIC comm: Queue buffer BATTERY LVL");
              // BricDebug.logString( buffer.data );
              if ( mBricInfoDialog != null ) mBricInfoDialog.setValue( DATA_BATTERY_LVL, buffer.data );
              registerInfo( null ); // battery level is the last info
              break;
            case DATA_QUIT:
              do_consume = false;
              break;
            default:
              TDLog.e("BRIC comm: Queue - unknown buffer type " + buffer.type );
          }
        }
        TDLog.v("BRIC comm tread exit");
      } 
    };
    mConsumer.start();
  }

  /* terminate the consumer thread - put a "quit" buffer on the queue
   * @note this method has still to be used
   */
  @Override
  public void terminate()
  {
    TDLog.v("BRIC comm terminate");
    if ( mConsumer != null ) {
      // put a DATA_QUIT buffer on the queue
      mQueue.put( DATA_QUIT, new byte[0] );
    }
  }

  /** register an info-dialog
   * @param info   info dialog, use null to unregister
   */
  public void registerInfo( BricInfoDialog info ) { mBricInfoDialog = info; }

  /** reset the BRIC memory (?)
   * @param bytes ... (use null to clear the memory)
   * @return true if successful
   */
  public boolean setMemory( byte[] bytes )
  {
    if ( bytes == null ) { // CLEAR
      // TDLog.v( "BRIC clear memory");
      return sendCommand( BricConst.CMD_CLEAR );
    } else { // LAST TIME
      // TDLog.v( "BRIC reset memory ... ");
      enqueueOp( new BleOpChrtWrite( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.LAST_TIME_UUID, bytes ) );
      clearPending();
      return true;
    }
    // return false;
  }

  /** read a characteristics
   * @param srvUuid  service UUID
   * @param chrtUuid characteristics UUID
   * @return true if successful
   * @note this is run by BleOpChrtRead
   */
  public boolean readChrt( UUID srvUuid, UUID chrtUuid ) 
  { 
    TDLog.v( "BRIC5 comm: read chrt " + chrtUuid.toString() );
    return mCallback.readChrt( srvUuid, chrtUuid ); 
  }

  /** write a characteristics
   * @param srvUuid  service UUID
   * @param chrtUuid characteristics UUID
   * @return true if successful
   * @note this is run by BleOpChrtWrite
   */
  public boolean writeChrt( UUID srvUuid, UUID chrtUuid, byte[] bytes )
  { 
    TDLog.v( "BRIC5 comm: write chrt " + chrtUuid.toString() );
    return mCallback.writeChrt( srvUuid, chrtUuid, bytes ); 
  }

  /** enlist a read from a characteristics
   * @param srvUuid  service UUID
   * @param chrtUuid characteristics UUID
   * @return true if successful
   */
  public boolean enlistRead( UUID srvUuid, UUID chrtUuid )
  {
    BluetoothGattCharacteristic chrt = mCallback.getReadChrt( srvUuid, chrtUuid );
    if ( chrt == null ) {
      TDLog.e("BRIC5 comm enlist read: null read chrt");
      return false;
    }
    if ( ! BleUtils.isChrtRead( chrt ) ) {
      TDLog.e("BRIC comm enlist read: chrt not permission readable");
      // return false;
    }
    TDLog.v( "BRIC5 comm: enlist chrt read " + chrtUuid.toString() );
    enqueueOp( new BleOpChrtRead( mContext, this, srvUuid, chrtUuid ) );
    doNextOp();
    return true;
  }

  // enlist a write from a characteristics (not used)
  // public boolean enlistWrite( UUID srvUuid, UUID chrtUuid, byte[] bytes )
  // {
  //   BluetoothGattCharacteristic chrt = mCallback.getWriteChrt( srvUuid, chrtUuid );
  //   if ( chrt == null ) {
  //     TDLog.e("BRIC comm enlist write: null write chrt");
  //     return false;
  //   }
  //   if ( ! BleUtils.isChrtWrite( chrt ) ) {
  //     TDLog.e("BRIC comm enlist write: cannot write chrt");
  //     return false;
  //   }
  //   // TDLog.v( "BRIC comm: enlist chrt write " + chrtUuid.toString() );
  //   enqueueOp( new BleOpChrtWrite( mContext, this, srvUuid, chrtUuid, bytes ) );
  //   doNextOp();
  //   return true;
  // }

  // public boolean enablePNotify( UUID srvUuid, BluetoothGattCharacteristic chrt ) { return mCallback.enablePNotify( srvUuid, chrt ); }
  public boolean enablePNotify( UUID srvUuid, UUID chrtUuid ) { return mCallback.enablePNotify( srvUuid, chrtUuid ); }
  public boolean enablePIndicate( UUID srvUuid, UUID chrtUuid ) { return mCallback.enablePIndicate( srvUuid, chrtUuid ); }
  
  // ---------------------------------------------------------------------------
  // send data to the application

  // --------------------------------------------------------------
  /*
  private void addService( BluetoothGattService srv ) 
  { 
    String srv_uuid = srv.getUuid().toString();
    // TDLog.v( "BRIC comm add S: " + srv_uuid );
  }
  */

  /*
  // register characteristics for notification
  // doNextOp() is done by serviceDiscovered when it completes
  private void addChrt( UUID srvUuid, BluetoothGattCharacteristic chrt ) 
  {
    int ret;
    UUID chrtUuid = chrt.getUuid();
    String chrt_uuid = chrtUuid.toString();
    // TDLog.v( "BRIC comm ***** add chrt " + chrtUuid );
    if ( chrt_uuid.equals( BricConst.MEAS_PRIM ) ) {
      ret = enqueueOp( new BleOpNotify( mContext, this, srvUuid, chrt, true ) );
    } else if ( chrt_uuid.equals( BricConst.MEAS_META ) ) {
      // ret = enqueueOp( new BleOpNotify( mContext, this, srvUuid, chrt, true ) );
    } else if ( chrt_uuid.equals( BricConst.MEAS_ERR ) ) {
      // ret = enqueueOp( new BleOpNotify( mContext, this, srvUuid, chrt, true ) );
    } else if ( chrt_uuid.equals( BricConst.LAST_TIME ) ) { // LAST_TIME is not notified
      ret = enqueueOp( new BleOpNotify( mContext, this, srvUuid, chrt, true ) );
    } else {
      // TDLog.v( "BRIC comm add: unknown chrt " + chrt_uuid );
    }
  }
  */

  /*
  private void addDesc( UUID srv_uuid, UUID chrt_uuid, BluetoothGattDescriptor desc ) 
  {
    String desc_uuid = desc.getUuid().toString();
    // TDLog.v( "BRIC comm add     +D: " + desc_uuid );
  }
  */

  // ---------------------------------------------------------------------------
  // callback action completions - these methods must clear the pending action by calling
  // clearPending() which starts a new action if there is one waiting

  // UNUSED
  // void subscribe( UUID service, UUID characteristic )
  // {
  //    enqueueOp( new BleOpNotify( mContext, this, service, characteristic, true ) );
  // }

  /** react to service discovery
   * @param gatt   bluetooth GATT
   * @note from onServicesDiscovered
   */
  public int servicesDiscovered( BluetoothGatt gatt )
  {
    // TDLog.Log( TDLog.LOG_COMM, "BRIC comm service discovered");
    /*
    // (new Handler( Looper.getMainLooper() )).post( new Runnable() {
    //   public void run() {
        List< BluetoothGattService > services = gatt.getServices();
        for ( BluetoothGattService service : services ) {
          // addService() does not do anything
          // addService( service );
          UUID srv_uuid = service.getUuid();
          // TDLog.v( "BRIC comm Srv  " + srv_uuid.toString() );
          List< BluetoothGattCharacteristic> chrts = service.getCharacteristics();
          for ( BluetoothGattCharacteristic chrt : chrts ) {
            addChrt( srv_uuid, chrt );

            // addDesc() does not do anything
            // UUID chrt_uuid = chrt.getUuid();
            // // TDLog.v( "BRIC comm Chrt " + chrt_uuid.toString() + BleUtils.chrtPermString(chrt) + BleUtils.chrtPropString(chrt) );
            // List< BluetoothGattDescriptor> descs = chrt.getDescriptors();
            // for ( BluetoothGattDescriptor desc : descs ) {
            //   addDesc( srv_uuid, chrt_uuid, desc );
            //   // TDLog.v( "BRIC comm Desc " + desc.getUuid().toString() + BleUtils.descPermString( desc ) );
            // }
          }
        }
    //   }
    // } );
    */

    // enqueueOp( new BleOpNotify( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.MEAS_PRIM_UUID, true ) );
    // doNextOp();
    // clearPending();

    // THIS IS THE BEST 
    // it looks as if there are pending data, after connection, the very first prim is not sent 
    // enqueueOp( new BleOpNotify( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.MEAS_PRIM_UUID, true ) );
    // doNextOp();
    if ( TDSetting.mBricMode >= BricMode.MODE_ALL ) {
      enqueueOp( new BleOpNotify( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.MEAS_META_UUID, true ) );
      // clearPending();
      enqueueOp( new BleOpNotify( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.MEAS_ERR_UUID, true ) );
      // clearPending();
      enqueueOp( new BleOpNotify( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.LAST_TIME_UUID, true ) );
    }
    enqueueOp( new BleOpNotify( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.MEAS_PRIM_UUID, true ) );
    doNextOp();
    // clearPending();

    mBTConnected = true;
    TDLog.v( "BRIC comm discovered services status CONNECTED" );
    notifyStatus( ConnectionState.CONN_CONNECTED ); 

    return 0;
  }

  /** react after a characteristics has been read: the read bytes are put on the buffer queue
   * @param uuid_str characteristics string UUID
   * @param bytes    read bytes
   * @note from onCharacteristicRead
   */
  public void readedChrt( String uuid_str, byte[] bytes )
  {
    // TDLog.Log( TDLog.LOG_COMM, "BRIC comm: readed chrt " + uuid_str );
    if ( uuid_str.equals( BricConst.MEAS_PRIM ) ) { // this is not executed: PRIM is read from onCharacteristicChanged
      mQueue.put( DATA_PRIM, bytes ); 
    } else if ( uuid_str.equals( BricConst.MEAS_META ) ) {
      mQueue.put( DATA_META, bytes );
    } else if ( uuid_str.equals( BricConst.MEAS_ERR  ) ) {
      mQueue.put( DATA_ERR, bytes ); 
      // FIXME COMPOSITE_COMMANDS
      // doPendingCommand();
  
      /* LAST_TIME could be read, but it is zero-filled
      ret = enqueueOp( new BleOpChrtRead( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.LAST_TIME_UUID ) );
      */
    } else if ( uuid_str.equals( BricConst.LAST_TIME  ) ) {
      mQueue.put( DATA_TIME, bytes );
    // } else if ( uuid_str.equals( BleConst.INFO_23 ) ) { // ???
    //   mQueue.put( DATA_INFO_23, bytes );
    // } else if ( uuid_str.equals( BleConst.INFO_24 ) ) { // device name
    //   mQueue.put( DATA_INFO_24, bytes );
    // } else if ( uuid_str.equals( BleConst.INFO_25 ) ) { // device number
    //   mQueue.put( DATA_INFO_25, bytes );
    } else if ( uuid_str.equals( BleConst.INFO_26 ) ) { // firmware
      mQueue.put( DATA_INFO_26, bytes );
    } else if ( uuid_str.equals( BleConst.INFO_27 ) ) { // hardware
      mQueue.put( DATA_INFO_27, bytes );
    } else if ( uuid_str.equals( BleConst.INFO_28 ) ) { // fw number
      mQueue.put( DATA_INFO_28, bytes );
    // } else if ( uuid_str.equals( BleConst.INFO_29 ) ) { // manufacturer
    //   mQueue.put( DATA_INFO_29, bytes );
    } else if ( uuid_str.equals( BleConst.DEVICE_00 ) ) { // device fullname
      mQueue.put( DATA_DEVICE_00, bytes );
    // } else if ( uuid_str.equals( BleConst.DEVICE_01 ) ) {
    //   mQueue.put( DATA_DEVICE_01, bytes );
    // } else if ( uuid_str.equals( BleConst.DEVICE_04 ) ) {
    //   mQueue.put( DATA_DEVICE_04, bytes );
    // } else if ( uuid_str.equals( BleConst.DEVICE_06 ) ) {
    //   mQueue.put( DATA_DEVICE_06, bytes );
    } else if ( uuid_str.equals( BleConst.BATTERY_LVL ) ) {
      mQueue.put( DATA_BATTERY_LVL, bytes );
    }
    clearPending();
  }

  /** react after a characteristics has been written
   * @param uuid_str characteristics string UUID
   * @param bytes    written bytes
   * @note from onCharacteristicWrite
   */
  public void writtenChrt( String uuid_str, byte[] bytes )
  {
    // TDLog.v( "BRIC5 comm chrt written " + uuid_str + " " + BleUtils.bytesToString( bytes ) );
    clearPending();
  }

  /** react after a descriptor has been read
   * @param uuid_str       characteristics string UUID
   * @param uuid_chrt_str  descriptor string UUID
   * @param bytes          read bytes
   * @note from onDescriptorRead
   */
  public void readedDesc( String uuid_str, String uuid_chrt_str, byte[] bytes )
  {
    // TDLog.v( "BRIC5 comm desc readed " + uuid_chrt_str + " " + BleUtils.bytesToString( bytes ) );
    clearPending();
  }

  /** react after a descriptor has been written
   * @param uuid_str       descriptor string UUID
   * @param uuid_chrt_str  characteristics string UUID
   * @param bytes          read bytes
   * @note from onDescriptorWrite
   */
  public void writtenDesc( String uuid_str, String uuid_chrt_str, byte[] bytes )
  {
    TDLog.v( "BRIC5 comm desc written " + uuid_chrt_str );
    clearPending();
  }

  // from onMtuChanged
  public void changedMtu( int mtu )
  {
    // TDLog.v( "BRIC comm changed MTU " + mtu );
    clearPending();
  }

  // from onReadRemoteRssi
  public void readedRemoteRssi( int rssi )
  {
    // TDLog.v( "BRIC comm readed RSSI " + rssi );
    clearPending();
  }

  /** from onCharacteristicChanged - this is called when the BRIC4 signals
   *  MEAS_META, MEAS_ERR, and LAST_TIME are change-notified 
   * @param chrt   read characteristic
   */
  public void changedChrt( BluetoothGattCharacteristic chrt )
  {
    String chrt_uuid = chrt.getUuid().toString();
    // TDLog.v( "BRIC5 comm changed chrt " + chrt_uuid );
    // delay closing one second after a characteristic change
    if ( chrt_uuid.equals( BricConst.MEAS_PRIM ) ) {
      TDLog.v( "BRIC comm changed char PRIM" );
      onData = System.currentTimeMillis() + 1000;
      mQueue.put( DATA_PRIM, chrt.getValue() );
    } else if ( chrt_uuid.equals( BricConst.MEAS_META ) ) { 
      TDLog.v( "BRIC comm changed char META" ); 
      mQueue.put( DATA_META, chrt.getValue() );
    } else if ( chrt_uuid.equals( BricConst.MEAS_ERR  ) ) {
      TDLog.v( "BRIC comm changed char ERR" ); 
      mQueue.put( DATA_ERR, chrt.getValue() );
    } else if ( chrt_uuid.equals( BricConst.LAST_TIME  ) ) {
      TDLog.v( "BRIC5 comm changed char TIME " /* + BleUtils.bytesToString( chrt.getValue() ) */ );
      // mQueue.put( DATA_TIME, chrt.getValue() ); 
      // // TDLog.v( "BRIC comm last time " + BleUtils.bytesToString( chrt.getValue() ) );
    } else {
      TDLog.e("Bric comm UNKNOWN chrt changed " + chrt_uuid );
    }
    // this is not necessary
    // clearPending();
    doNextOp();
  }

  // from onReliableWriteCompleted
  public void completedReliableWrite() 
  { 
    // TDLog.v( "BRIC comm: reliable write" );
    clearPending();
  }

  // general error condition
  // the action may depend on the error status TODO
  // @param status error code
  // @param extra  error message
  // @param what   error source (unused)
  public void error( int status, String extra, String what )
  {
    switch ( status ) {
      case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH: 
        TDLog.e("BRIC COMM: invalid attr length " + extra );
        break;
      case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
        TDLog.e("BRIC COMM: write not permitted " + extra );
        break;
      case BluetoothGatt.GATT_READ_NOT_PERMITTED:
        TDLog.e("BRIC COMM: read not permitted " + extra );
        break;
      case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
        TDLog.e("BRIC COMM: insufficient encrypt " + extra );
        break;
      case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
        TDLog.e("BRIC COMM: insufficient auth " + extra );
        break;
      case BleCallback.CONNECTION_TIMEOUT:
      case BleCallback.CONNECTION_133: // unfortunately this happens
        TDLog.v( "BRIC5 comm: connection timeout or 133");
        // notifyStatus( ConnectionState.CONN_WAITING );
        reconnectDevice();
        break;
      default:
        TDLog.e("BRIC comm ***** ERROR " + status + ": reconnecting ...");
        reconnectDevice();
    }
    clearPending();
  }

  /** handle a failure error: clear ops, disconnect, close GATT
   * @param status   error status code
   * @param extra    failure message
   * @param what     failure source (unused)
   */
  public void failure( int status, String extra, String what )
  {
    TDLog.v( "BRIC5 comm Failure: disconnect and close GATT ...");
    // notifyStatus( ConnectionState.CONN_DISCONNECTED ); // this will be called by disconnected
    clearPending();
    closeDevice();
    mCallback.closeGatt();
  }
    
  // ----------------- CONNECT -------------------------------

  private boolean connectBricDevice( Device device, ListerHandler lister, int data_type ) // FIXME BLEX_DATA_TYPE
  {
    if ( mRemoteBtDevice == null ) {
      TDToast.makeBad( R.string.ble_no_remote );
      // TDLog.e("BRIC comm ERROR null remote device");
      // TDLog.Log( TDLog.LOG_COMM, "BRIC comm ***** connect Device: null = [3b] status DISCONNECTED" );
      notifyStatus( ConnectionState.CONN_DISCONNECTED );
      return false;
    } 
    notifyStatus( ConnectionState.CONN_WAITING );
    mReconnect   = true;
    mOps         = new ConcurrentLinkedQueue< BleOperation >();
    mProtocol    = new BricProto( mContext, mApp, lister, device, this );
    // mChrtChanged = new BricChrtChanged( this, mQueue );
    // mCallback    = new BleCallback( this, mChrtChanged, false ); // auto_connect false
    mCallback    = new BleCallback( this, false ); // auto_connect false

    // mPendingCommands = 0; // FIXME COMPOSITE_COMMANDS
    // clearPending();

    TDLog.v( "BRIC comm ***** connect Device = [3a] status WAITING" );
    int ret = enqueueOp( new BleOpConnect( mContext, this, mRemoteBtDevice ) ); // exec connectGatt()
    TDLog.v( "BRIC comm connects ... " + ret);
    clearPending();
    // doNextOp();
    return true;
  }

  public void connectGatt( Context ctx, BluetoothDevice bt_device ) // called from BleOpConnect
  {
    TDLog.v( "BRIC5 comm ***** connect GATT");
    mContext = ctx;
    mCallback.connectGatt( mContext, bt_device );
    // setupNotifications(); // FIXME_BRIC
  }

  /** connect to a BRIC device 
   * @param address   device address
   * @param lister    data lister
   * @param data_type ???
   * @param timeout   timeout (unused)
   * @return true if successful
   */
  @Override
  public boolean connectDevice( String address, ListerHandler lister, int data_type, int timeout )
  {
    TDLog.v( "BRIC5 comm ***** connect Device");
    mNrReadPackets = 0;
    mDataType      = data_type;
    return connectBricDevice( TDInstance.getDeviceA(), lister, data_type );
  }

  // try to recover from an error ... 
  private void reconnectDevice()
  {
    mOps.clear();
    // mPendingCommands = 0; // FIXME COMPOSITE_COMMANDS
    clearPending();
    mCallback.closeGatt();
    if ( mReconnect ) {
      TDLog.v( "BRIC comm ***** reconnect yes Device = [4a] status WAITING" );
      notifyStatus( ConnectionState.CONN_WAITING );
      enqueueOp( new BleOpConnect( mContext, this, mRemoteBtDevice ) ); // exec connectGatt()
      doNextOp();
      mBTConnected = true;
    } else {
      TDLog.v( "BRIC comm ***** reconnect no Device = [4b] status DISCONNECTED" );
      notifyStatus( ConnectionState.CONN_DISCONNECTED );
    }
  }


  // ----------------- DISCONNECT -------------------------------

  // from onConnectionStateChange STATE_DISCONNECTED
  public void disconnected()
  {
    TDLog.v( "BRIC comm ***** disconnected" );
    clearPending();
    mOps.clear(); 
    // mPendingCommands = 0; // FIXME COMPOSITE_COMMANDS
    mBTConnected = false;
    notifyStatus( ConnectionState.CONN_DISCONNECTED );
  }

  public void connected()
  {
    clearPending();
  }

  public void disconnectGatt()  // called from BleOpDisconnect
  {
    TDLog.v( "BRIC5 comm ***** disconnect GATT" );
    notifyStatus( ConnectionState.CONN_DISCONNECTED );
    mCallback.closeGatt();
  }

  /** disconnect from the remote device
   */
  @Override
  public boolean disconnectDevice()
  {
    if ( mTimer != null ) {
      mTimer.cancel();
      mTimer = null;
    }
    TDLog.v( "BRIC5 comm ***** disconnect device = connected:" + mBTConnected );
    return closeDevice();
/*
    mReconnect = false;
    if ( mBTConnected ) {
      mBTConnected = false;
      notifyStatus( ConnectionState.CONN_DISCONNECTED );
      mCallback.closeGatt();
    }
*/
  }

  // this is called only on a GATT failure, or the user disconnects 
  private boolean closeDevice()
  {
    mReconnect = false;
    if ( System.currentTimeMillis() < onData ) {
      if ( mBTConnected ) notifyStatus( ConnectionState.CONN_WAITING );
      if ( mTimer == null ) {
        TDLog.v( "BRIC5 schedule a delayed closeDevice" );
        mTimer = new Timer();
        mTimer.schedule(  new TimerTask() { @Override public void run() { disconnectDevice(); } }, 1000 );
      }
      return false;
    }
    if ( mBTConnected ) {
      mBTConnected = false;
      notifyStatus( ConnectionState.CONN_DISCONNECTED ); // not necessary
      // TDLog.Log( TDLog.LOG_COMM, "BRIC comm ***** close device");
      int ret = enqueueOp( new BleOpDisconnect( mContext, this ) ); // exec disconnectGatt
      doNextOp();
      TDLog.v( "BRIC comm: close Device - disconnect ... ops " + ret );
    }
    return true;
  }

  // ----------------- SEND COMMAND -------------------------------
  @Override
  public boolean sendCommand( int cmd )
  {
    if ( ! isConnected() ) return false;
    byte[] command = null;
    switch ( cmd ) {
      case BricConst.CMD_SCAN:  command = Arrays.copyOfRange( BricConst.COMMAND_SCAN,  0,  4 ); break;
      case BricConst.CMD_SHOT:  command = Arrays.copyOfRange( BricConst.COMMAND_SHOT,  0,  4 ); break;
      case BricConst.CMD_LASER: command = Arrays.copyOfRange( BricConst.COMMAND_LASER, 0,  5 ); break;
      case BricConst.CMD_CLEAR: command = Arrays.copyOfRange( BricConst.COMMAND_CLEAR, 0, 12 ); break;
      case BricConst.CMD_OFF:   command = Arrays.copyOfRange( BricConst.COMMAND_OFF,   0,  9 ); break;
/*
      case BricConst.CMD_SPLAY: 
        // TDLog.v( "BRIC comm send cmd SPLAY");
        mPendingCommands += 1;
        break;
      case BricConst.CMD_LEG: 
        // TDLog.v( "BRIC comm send cmd LEG");
        mPendingCommands += 3;
        break;
*/
    }
    if ( command != null ) {
      // TDLog.v( "BRIC comm send cmd " + cmd );
      enqueueOp( new BleOpChrtWrite( mContext, this, BricConst.CTRL_SRV_UUID, BricConst.CTRL_CHRT_UUID, command ) );
      doNextOp();
    // } else { // FIXME COMPOSITE_COMMANDS
    //   if ( mPendingOp == null ) doPendingCommand();
    }
    return true;
  }

  // UNUSED
  // private void enqueueShot( final BleComm comm )
  // {
  //   (new Thread() {
  //     public void run() {
  //       // TDLog.v( "BRIC comm: enqueue LASER cmd");
  //       byte[] cmd1 = Arrays.copyOfRange( BricConst.COMMAND_LASER, 0, 5 );
  //       enqueueOp( new BleOpChrtWrite( mContext, comm, BricConst.CTRL_SRV_UUID, BricConst.CTRL_CHRT_UUID, cmd1 ) );
  //       doNextOp();
  //       TDUtil.slowDown( 600 );
  //       // TDLog.v( "BRIC comm: enqueue SHOT cmd");
  //       byte[] cmd2 = Arrays.copyOfRange( BricConst.COMMAND_SHOT, 0, 4 );
  //       enqueueOp( new BleOpChrtWrite( mContext, comm, BricConst.CTRL_SRV_UUID, BricConst.CTRL_CHRT_UUID, cmd2 ) );
  //       doNextOp();
  //       TDUtil.slowDown( 800 );
  //     }
  //   } ).start();
  // }

  // UNUSED
  // private boolean sendLastTime( )
  // {
  //   byte[] last_time = ((BricProto)mProtocol).getLastTime();
  //   // TDLog.v( "BRIC comm send last time: " + BleUtils.bytesToString( last_time ) );
  //   if ( last_time == null ) return false;
  //   enqueueOp( new BleOpChrtWrite( mContext, this, BricConst.MEAS_SRV_UUID, BricConst.LAST_TIME_UUID, last_time ) );
  //   doNextOp();
  //   return true;
  // } 

  // --------------------------------------------------------------------------
  private BleOperation mPendingOp = null;

  private void clearPending() 
  { 
    mPendingOp = null; 
    // if ( ! mOps.isEmpty() || mPendingCommands > 0 ) doNextOp();
    if ( ! mOps.isEmpty() ) doNextOp();
  }

  // @return the length of the ops queue
  private int enqueueOp( BleOperation op ) 
  {
    mOps.add( op );
    // printOps(); // DEBUG
    return mOps.size();
  }

  // access by BricChrtChanged
  private void doNextOp() 
  {
    if ( mPendingOp != null ) {
      // TDLog.v( "BRIC comm: next op with pending not null, ops " + mOps.size() ); 
      return;
    }
    mPendingOp = mOps.poll();
    // TDLog.v( "BRIC comm: polled, ops " + mOps.size() );
    if ( mPendingOp != null ) {
      mPendingOp.execute();
    } 
    // else if ( mPendingCommands > 0 ) {
    //   enqueueShot( this );
    //   -- mPendingCommands;
    // }
  }

/* FIXME COMPOSITE_COMMANDS
  private void doPendingCommand()
  {
    if ( mPendingCommands > 0 ) {
      enqueueShot( this );
      -- mPendingCommands;
    }
  }
*/

  /* DEBUG
  private void printOps()
  {
    StringBuilder sb = new StringBuilder();
    sb.append( "BRIC comm Ops: ");
    for ( BleOperation op : mOps ) sb.append( op.name() ).append(" ");
    TDLog.v( sb.toString() );
  }
  */
    
  /** request a new MTU
   * @param mtu   new value
   * @return always false
   */
  public boolean requestMtu( int mtu )
  {
    TDLog.Error( "BRIC requestMtu not implemented" );
    return false;
  }

}
