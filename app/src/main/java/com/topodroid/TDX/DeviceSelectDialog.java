/* @file DeviceSelectDialog.java
 *
 * @author marco corvi
 * @date nov 2016
 *
 * @brief TopoDroid DistoX device selection dialog (for multi-DistoX)
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.utils.TDLog;
import com.topodroid.ui.MyDialog;
import com.topodroid.dev.Device;
import com.topodroid.dev.DataType;

// import java.util.Set;
// import java.util.List;
import java.util.ArrayList;

// import android.app.Dialog;
import android.os.Bundle;

import android.content.Context;
// import android.content.DialogInterface;

import android.widget.TextView;
import android.widget.ListView;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.view.View;
import android.view.View.OnClickListener;

class DeviceSelectDialog extends MyDialog
                         implements OnItemClickListener
                         , OnClickListener
{
  private final Context mContext;
  private final DataDownloader mDownloader;
  private final ILister mLister;

  private ListView mList;
  // private Button  mBtnCancel;

  // ---------------------------------------------------------------
  /** cstr
   * @param context    context
   * @param app        application
   * @param downloader data downloader
   * @param lister     data lister
   */
  DeviceSelectDialog( Context context, TopoDroidApp app, DataDownloader downloader, ILister lister )
  {
    super( context, app, R.string.DeviceSelectDialog );
    mContext = context;
    mDownloader = downloader;
    mLister = lister;
    // TDLog.v( "device select dialog created");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
 
    // TDLog.v( "device select dialog init layout");
    initLayout( R.layout.device_select_dialog, R.string.title_device_select );

    // mBtnCancel = (Button) findViewById( R.id.button_cancel );
    // mBtnCancel.setOnClickListener( this );
    ( (Button) findViewById( R.id.button_cancel ) ).setOnClickListener( this );

    mList = (ListView) findViewById(R.id.dev_list);
    mList.setOnItemClickListener( this );
    // mList.setLongClickable( true );
    mList.setDividerHeight( 2 );
    updateList();
  }

  /** update the list of known devices
   */
  private void updateList( )
  {
    ListItemAdapter array_adapter = new ListItemAdapter( mContext, R.layout.message );
    // mArrayAdapter.clear();
    // if ( TDLevel.overTester ) { // FIXME VirtualDistoX
    //   array_adapter.add( "X000" );
    // }
    ArrayList< Device > devices = TopoDroidApp.mDData.getDevices();
    for ( Device device : devices ) {
      // String addr  = device.mAddress;
      // String model = device.mName;
      // String name  = device.mName;
      // String nick  = device.mNickname;
      if ( device.isBT() ) { // FIXME BLE : only classic BT
        array_adapter.add( device.toString() );
      }
    }
    mList.setAdapter( array_adapter );
  }

  /** respond to a user tap on a device of the list
   * @param parent ...
   * @param view   tapped view
   * @param pos    device position in the list
   * @param id     ...
   */
  @Override 
  public void onItemClick(AdapterView<?> parent, View view, int pos, long id)
  {
    dismiss();
    if ( ! ( view instanceof TextView ) ) {
      TDLog.Error("device select view instance of " + view.toString() );
      return;
    }
    CharSequence item = ((TextView) view).getText();
    StringBuffer buf = new StringBuffer( item );
    int k = buf.lastIndexOf(" ");
    String[] vals = item.toString().split(" ", 3 );

    // FIXME VirtualDistoX
    // String address = ( vals[0].equals("X000") )? Device.ZERO_ADDRESS : vals[2];
    String model   = vals[0];
    String name    = vals[1];
    String address = vals[2];

    mApp.setDevicePrimary( address, model, name, null ); // FIXME BLEX only BT devices
    mLister.setTheTitle();
    mDownloader.toggleDownload();
    mLister.setConnectionStatus( mDownloader.getStatus() );
    mDownloader.doDataDownload( mApp.mListerSet, DataType.DATA_ALL );
  }

  /** respond to a user tap - dismiss the dialog
   * @param v  tapped view
   */
  @Override
  public void onClick(View v) 
  {
    // Button b = (Button) v;
    // if ( b == mBtnCancel ) {
    //   /* nothing */
    // }
    dismiss();
  }

}

