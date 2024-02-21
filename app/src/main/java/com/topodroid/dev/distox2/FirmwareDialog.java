/* @file FirmwareDialog.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid DistoX X310 device firmware dialog
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 *
 * this class is instantiated only by DeviceActivity
 */
package com.topodroid.dev.distox2;

import com.topodroid.utils.TDLog;
import com.topodroid.ui.MyDialog;
import com.topodroid.prefs.TDSetting;
import com.topodroid.TDX.TopoDroidApp;
import com.topodroid.TDX.DeviceActivity;
// import com.topodroid.TDX.TopoDroidAlertDialog;
import com.topodroid.TDX.TDToast;
import com.topodroid.TDX.TDPath;
import com.topodroid.TDX.R;
// import com.topodroid.dev.distox2.FirmwareFileDialog;
// import com.topodroid.dev.distox2.FirmwareUtils;

import java.io.File;
// import java.io.FileInputStream;
// import java.io.DataInputStream;
// import java.io.IOException;

import android.os.Bundle;

import android.content.Context;
import android.content.res.Resources;
// import android.content.Intent;
// import android.content.DialogInterface;
// import android.content.DialogInterface.OnCancelListener;
// import android.content.DialogInterface.OnDismissListener;

import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.EditText;
// import android.widget.TextView;

import android.text.method.KeyListener;

public class FirmwareDialog extends MyDialog
                            implements View.OnClickListener
{
  private DeviceActivity mParent;
  private RadioButton mBtnDump;
  private RadioButton mBtnUpload;
  private Button mBtnOK;
  private Button mBtnClose;

  private EditText mETfile;

  private final Resources mRes;
  private KeyListener    mETkeyListener;

  /** cstr
   * @param context  context
   * @param res      resources
   * @param app      application
   */
  public FirmwareDialog( Context context, DeviceActivity parent, Resources res, TopoDroidApp app )
  {
    super( context, app, R.string.FirmwareDialog );
    mParent = parent;
    mRes    = res;
  }

  @Override
  public void onCreate( Bundle bundle )
  {
    super.onCreate( bundle );

    initLayout( R.layout.firmware_dialog, R.string.firmware_title );

    mETfile  = (EditText) findViewById( R.id.firmware_file );

    mBtnUpload = (RadioButton) findViewById(R.id.firmware_upload );
    mBtnDump   = (RadioButton) findViewById(R.id.firmware_dump );
    mBtnOK     = (Button) findViewById(R.id.firmware_ok);
    mBtnClose  = (Button) findViewById(R.id.firmware_close);

    mETkeyListener = mETfile.getKeyListener();
    mETfile.setOnClickListener( this );
    // mETfile.setEnabled( false );
    mETfile.setFocusable( false );
    mETfile.setFocusableInTouchMode( false );
    // mETfile.setClickable( true );
    mETfile.setKeyListener( null );

    mBtnUpload.setOnClickListener( this );
    mBtnDump.setOnClickListener( this );
    mBtnOK.setOnClickListener( this );
    mBtnClose.setOnClickListener( this );
    
  }

  /** set the firmware filename in the edit text-view
   * @param filename   name of the firmware file
   */
  public void setFirmwareFile( String filename )
  {
    mETfile.setText( filename );
  }

  @Override
  public void onClick( View view )
  {
    int vid = view.getId();
    if ( vid == R.id.firmware_file ) {
      if ( mBtnUpload.isChecked() ) {
        (new FirmwareFileDialog( mContext, this )).show(); // select file from bin directory
      }
    } else if ( vid == R.id.firmware_upload ) {
      // mETfile.setEnabled( false );
      mETfile.setFocusable( false );
      mETfile.setFocusableInTouchMode( false );
      // mETfile.setClickable( true );
      mETfile.setKeyListener( null );
    } else if ( vid == R.id.firmware_dump ) {
      // mETfile.setEnabled( true );
      mETfile.setFocusable( true );
      mETfile.setFocusableInTouchMode( true );
      // mETfile.setClickable( true );
      mETfile.setKeyListener( mETkeyListener );
    } else if ( vid == R.id.firmware_ok ) {
      String filename = null;
      if ( mETfile.getText() != null ) { 
        filename = mETfile.getText().toString().trim();
        if ( filename.length() == 0 ) filename = null;
      }
      if ( filename == null ) {
        TDToast.makeBad( R.string.firmware_file_missing );
        return;
      }
      if ( mBtnDump.isChecked() ) {
        if ( ! filename.endsWith(".bin") ) filename = filename + ".bin";
        // TDLog.f( "Firmware dump to " + filename );
        TDLog.v( "Firmware dump to " + filename );
        // File fp = new File( TDPath.getBinFile( filename ) );
        File fp = TDPath.getBinFile( filename );
        if ( fp.exists() ) {
          TDToast.makeBad( R.string.firmware_file_exists );
          return;
        }
        askDump( filename );
      } else if ( mBtnUpload.isChecked() ) {
        // TDLog.f( "Firmware upload from " + filename );
        TDLog.v( "Firmware upload from " + filename );
        // File fp = new File( TDPath.getBinFile( filename ) );
        File fp = TDPath.getBinFile( filename );
        if ( ! fp.exists() ) {
          TDLog.e( "non-existent upload firmware file " + filename );
          return;    
        }
        int fw = FirmwareUtils.readFirmwareFirmware( fp ); // guess firmware version
        // TDLog.f( "Detected Firmware version " + fw );
        boolean check = (fw > 0) && FirmwareUtils.firmwareChecksum( fw, fp );
        TDLog.v( "Detected Firmware version " + fw + " check " + check );
        askUpload( filename, fw, check );
      }
    } else if ( vid == R.id.firmware_close ) {
      dismiss();
    }
  }


  /** ask the user whether to dump the firmware
   * @param filename   firmware file name including ".bin" extension
   */
  private void askDump( final String filename )
  {
    String title = mRes.getString( R.string.ask_download );
    dismiss();
    mParent.askFirmwareDownload( filename, title );
/*
    TopoDroidAlertDialog.makeAlert( mContext, mRes, R.string.ask_dump,
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick( DialogInterface dialog, int btn ) {
          // TDLog.f( "Firmware dump to file " + filename );
          TDLog.v( "Firmware dump to file " + filename );
          // FIXME ASYNC_FIRMWARE_TASK
          // TDToast.makeLong( R.string.firmware_wait_dump );
          // mApp.dumpFirmware( filename );
          int ret = mApp.dumpFirmware( filename );
          // TDLog.f( "Firmware dump to " + filename + " result: " + ret );
          TDLog.v( "Firmware dump to " + filename + " result: " + ret );
          if ( ret > 0 ) {
            TDToast.makeLong( String.format( mRes.getString(R.string.firmware_file_dumped), filename, ret ) );
          } else {
            TDToast.makeLong( R.string.firmware_file_dump_fail );
            TDPath.deleteBinFile( filename );
          }

          // finish(); 
        }
      }
    );
*/
  }

  /** ask the user whether to upload the firmware
   * @param filename  name of the firmware file
   * @param fw        firmware version
   * @param check     ???
   */
  private void askUpload( final String filename, int fw, boolean check )
  {
    // boolean compatible = (fw == 2100 || fw == 2200 || fw == 2300 || fw == 2400 || fw == 2500 || fw == 2412 || fw == 2501 || fw == 2512 );
    // final String pathname = TDPath.getBinFile( filename );
    int hw = FirmwareUtils.getHardware( fw );
    boolean compatible = FirmwareUtils.isCompatible( fw );
    // TDLog.f( "FW/HW compatible " + compatible + " FW check " + check );
    TDLog.v( "FW " + fw + " compatible " + compatible + " check " + check );
    compatible = compatible && check;

    // get the hardware version from the signature of the firmware on the DistoX
    // FIXME ASYNC_FIRMWARE_TASK
    // TDToast.makeLong( R.string.firmware_wait_check );
	// SIWEI FIXME the signature should be checked
    byte[] signature = mApp.readFirmwareSignature( hw );

    if ( signature == null ) { // could not get firmware signature
      TDToast.makeWarn( R.string.firmware_upload_no_sign );
      if ( TDSetting.mFirmwareSanity ) return;    
    } else if ( hw != FirmwareUtils.getDeviceHardwareSignature( signature ) ) {
      TDToast.makeWarn( R.string.firmware_upload_bad_sign );
      if ( TDSetting.mFirmwareSanity ) return;    
    }
    String title = mRes.getString( compatible? R.string.ask_upload : R.string.ask_upload_not_compatible );
    dismiss();
    mParent.askFirmwareUpload( filename, title );
/*
    TopoDroidAlertDialog.makeAlert( mContext, mRes, title,
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick( DialogInterface dialog, int btn ) {
          mParent.doFirmwareUpload( filename, title );
          // finish(); 
        }
      }
    );
*/
  }

}
