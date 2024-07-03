/* @file ExportDialogCalib.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid calib export dialog
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.ui.MyDialog;
// import com.topodroid.prefs.TDSetting;

// import android.app.Dialog;
// import android.app.Activity;
import android.os.Bundle;

// import android.content.Intent;

import android.content.Context;

import android.widget.Button;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.view.View;
// import android.view.View.OnKeyListener;
// import android.view.KeyEvent;
// import android.view.ViewGroup.LayoutParams;

public class ExportDialogCalib extends MyDialog
                   implements AdapterView.OnItemSelectedListener
                   , View.OnClickListener
{
  private Button   mBtnOk;
  // private Button   mBtnBack;

  private final IExporter mParent;
  private String[]  mTypes;
  private String    mSelected;
  private final int mTitle;

  /** cstr
   * @param context  context
   * @param parent   parent activity
   * @param types    export types
   * @param title    title resource index
   */
  public ExportDialogCalib( Context context, IExporter parent, String[] types, int title )
  {
    super( context, null, R.string.ExportDialogCalib ); // null app
    mParent = parent;
    mTypes  = types;
    mSelected = null;
    mTitle = title;
  }

// -------------------------------------------------------------------
  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);

    initLayout( R.layout.export_dialog_calib, mTitle );

    Spinner spin = (Spinner)findViewById( R.id.spin );
    spin.setOnItemSelectedListener( this );
    ArrayAdapter adapter = new ArrayAdapter<>( mContext, R.layout.menu, mTypes );
    spin.setAdapter( adapter );

    mBtnOk   = (Button) findViewById(R.id.button_ok );
    mBtnOk.setOnClickListener( this );
    // mBtnBack = (Button) findViewById(R.id.button_back );
    // mBtnBack.setOnClickListener( this );
    ( (Button) findViewById(R.id.button_back ) ).setOnClickListener( this );

    // Bundle extras = getIntent().getExtras();
    // String title  = extras.getString( TopoDroidApp.TOPODROID_SURVEY );

  }

  /** react to an item selection
   * @param av    item adapter
   * @param v     item view
   * @param pos   item position
   * @param id    ?
   */
  @Override
  public void onItemSelected( AdapterView av, View v, int pos, long id ) { mSelected = mTypes[ pos ]; }

  /** react to a deselection
   * @param av    item adapter
   */
  @Override
  public void onNothingSelected( AdapterView av ) { mSelected = null; }

  /** react to a user tap
   * @param v  tapped view
   */
  @Override
  public void onClick(View v) 
  {
    // TDLog.v("CALIB selected " + mSelected );
    Button b = (Button)v;
    if ( b == mBtnOk && mSelected != null ) {
      mParent.doExport( mSelected, TDInstance.calib, null, -1L, false ); // calib name is not used, null prefix, -1=first
    // } else if ( b == mBtnBack ) {
    //   /* nothing */
    }
    dismiss();
  }

}


