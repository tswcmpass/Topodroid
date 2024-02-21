/* @file CalibImportDialog.java
 *
 * @author marco corvi
 * @date sept 2015
 *
 * @brief TopoDroid import file list dialog
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.calib;

import com.topodroid.utils.TDLog;
import com.topodroid.ui.MyDialog;
// import com.topodroid.prefs.TDSetting;
import com.topodroid.TDX.R;
import com.topodroid.utils.TDUtil;
import com.topodroid.TDX.TDToast;
import com.topodroid.TDX.TDPath;
import com.topodroid.TDX.DeviceActivity;

import java.io.File;
// import java.util.Set;
import java.util.ArrayList;

// import android.app.Activity;
// import android.app.Dialog;
import android.os.Bundle;

// import android.content.Intent;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;

// import android.content.IntentFilter;
import android.content.Context;


public class CalibImportDialog extends MyDialog
                        implements OnItemClickListener
                        , OnClickListener
{ 
  private final DeviceActivity mParent;

  private ListView mList;
  // private Button mBtnCancel;

  public CalibImportDialog( Context context, DeviceActivity parent )
  {
    super( context, null, R.string.CalibImportDialog ); // null app
    mParent  = parent;
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate( savedInstanceState );

    initLayout( R.layout.import_dialog, R.string.calib_import_title );

    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>( mContext, R.layout.message );
    mList = (ListView) findViewById(R.id.list);
    mList.setOnItemClickListener( this );
    mList.setDividerHeight( 2 );

    Button btnCancel = (Button)findViewById( R.id.button_cancel );
    btnCancel.setOnClickListener( this );

    // setTitleColor( TDColor.TITLE_NORMAL );

    File[] files = TDPath.getCalibFiles();
    ArrayList< String > names = new ArrayList<>();
    if ( files != null ) {
      for ( File f : files ) { 
        names.add( f.getName() );
      }
    }
    if ( names.size() > 0 ) {
      // sort files by name (alphabetical order)
      TDUtil.sortStringList( names );

      for ( int k=0; k<names.size(); ++k ) {
        arrayAdapter.add( names.get(k) );
      }
      mList.setAdapter( arrayAdapter );
    } else {
      TDToast.makeBad( R.string.import_none );
      dismiss();
    }
  }

  @Override
  public void onClick( View v ) 
  {
    dismiss();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id)
  {
    if ( ! ( view instanceof TextView ) ) {
      TDLog.Error("calib import view instance of " + view.toString() );
      return;
    }
    String item = ((TextView) view).getText().toString();

    // hide();
    mList.setOnItemClickListener( null );
    // setTitle(" W A I T ");
    dismiss();

    mParent.importCalibFile( item );
  }

}


