/* @file SymbolEnableDialog.java
 *
 * @author marco corvi
 * @date 
 *
 * @brief TopoDroid drawing symbol: 
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDColor;
import com.topodroid.ui.MyDialog;
import com.topodroid.common.SymbolType;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.AsyncTask;

// import android.app.Dialog;
// import android.app.Activity;
import android.content.Context;
// import android.content.Intent;
// import android.content.DialogInterface;
// import android.content.DialogInterface.OnClickListener;

// import android.view.Window;

import android.view.View;
// import android.view.ViewGroup.LayoutParams;
import android.widget.Button;

// import android.widget.AdapterView;
// import android.widget.AdapterView.OnItemClickListener;

// import android.widget.TextView;
import android.widget.ListView;

class SymbolEnableDialog extends MyDialog
                         implements View.OnClickListener
{
  private int mType; // symbols type

  private  Button mBTpoint;
  private  Button mBTline;
  private  Button mBTarea;

  // private  Button mBTreload;

  // private  Button mBTsave;
  // private  Button mBTcancel;
  // private  Button mBTok;

  private ListView    mList;
  private SymbolAdapter mPointAdapter;
  private SymbolAdapter mLineAdapter;
  private SymbolAdapter mAreaAdapter;


  SymbolEnableDialog( Context context )
  {
    super( context, null, R.string.SymbolEnableDialog ); // null app
    mType    = SymbolType.LINE; // default symbols are lines
  }

  @Override
  public void onCreate( Bundle savedInstanceState )
  {
    super.onCreate( savedInstanceState );

    // requestWindowFeature(Window.FEATURE_NO_TITLE);
    initLayout(R.layout.symbol_enable_dialog, null );
    // getWindow().setLayout( LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT );

    mList = (ListView) findViewById(R.id.symbol_list);
    // mList.setOnItemClickListener( this );
    mList.setDividerHeight( 2 );
    
    // mBTsave    = (Button) findViewById(R.id.symbol_save );
    // mBTcancel  = (Button) findViewById(R.id.symbol_cancel );

    mBTline  = (Button) findViewById(R.id.symbol_line );
    mBTpoint = (Button) findViewById(R.id.symbol_point);
    mBTarea  = (Button) findViewById(R.id.symbol_area );

    // mBTreload = (Button) findViewById(R.id.symbol_reload );
    // if ( TDLevel.overNormal ) {
    //   mBTreload.setOnClickListener( this );
    // } else {
    //   mBTreload.setVisibility( View.GONE );
    // }
 
    mBTline.setOnClickListener( this );
    if ( TDLevel.overBasic ) {
      mBTpoint.setOnClickListener( this );
      mBTarea.setOnClickListener( this );
    }

    // TDLog.v( "Symbol-Enable dialog ... create adapters" );
    if ( ! createAdapters() ) dismiss();

    // mList.setAdapter( mPointAdapter );
    updateList( /* -1, */ mType );
  }

  private boolean createAdapters()
  {
    mPointAdapter = new SymbolAdapter( mContext, R.layout.symbol, new ArrayList< EnableSymbol >() );
    mLineAdapter  = new SymbolAdapter( mContext, R.layout.symbol, new ArrayList< EnableSymbol >() );
    mAreaAdapter  = new SymbolAdapter( mContext, R.layout.symbol, new ArrayList< EnableSymbol >() );

    if ( TDLevel.overBasic ) {
      SymbolPointLibrary point_lib = BrushManager.getPointLib();
      if ( point_lib == null ) return false;
      int np = point_lib.size();
      for ( int i=0; i<np; ++i ) {
	Symbol point = point_lib.getSymbolByIndex( i );
	if ( ! point.isThName( SymbolLibrary.SECTION ) ) { // FIXME_SECTION_POINT always enabled
          mPointAdapter.add( new EnableSymbol( mContext, SymbolType.POINT, i, point ) );
        }
      }
    }

    SymbolLineLibrary line_lib   = BrushManager.getLineLib();
    if ( line_lib == null ) return false;
    int nl = line_lib.size();
    for ( int j=0; j<nl; ++j ) {
      mLineAdapter.add( new EnableSymbol( mContext, SymbolType.LINE, j, line_lib.getSymbolByIndex( j ) ) );
    }

    if ( TDLevel.overBasic ) {
      SymbolAreaLibrary area_lib   = BrushManager.getAreaLib();
      if ( area_lib == null ) return false;
      int na = area_lib.size();
      for ( int k=0; k<na; ++k ) {
        mAreaAdapter.add( new EnableSymbol( mContext, SymbolType.AREA, k, area_lib.getSymbolByIndex( k ) ) );
      }
    }

    // TDLog.v( "Symbol-Enable dialog ... symbols " + np + " " + nl + " " + na );
    return true;
  }

  private void updateList( /* int old_type, */ int new_type )
  {
    // TDLog.v( "Symbol-Enable dialog ... updateList type " + mType );
    // switch ( old_type ) {
    //   case SymbolType.POINT:
    //     mBTpoint.setTextColor( TDColor.SYMBOL_TAB );
    //     break;
    //   case SymbolType.LINE:
    //     mBTline.setTextColor( TDColor.SYMBOL_TAB );
    //     break;
    //   case SymbolType.AREA:
    //     mBTarea.setTextColor( TDColor.SYMBOL_TAB );
    //     break;
    // }
    switch ( new_type ) {
      case SymbolType.POINT:
        if ( TDLevel.overBasic ) {
          mList.setAdapter( mPointAdapter );
          // mBTpoint.getBackground().setColorFilter( TDColor.LIGHT_BLUE, PorterDuff.Mode.LIGHTEN );
          // mBTline.getBackground().setColorFilter(  TDColor.LIGHT_GRAY, PorterDuff.Mode.DARKEN );
          // mBTarea.getBackground().setColorFilter(  TDColor.LIGHT_GRAY, PorterDuff.Mode.DARKEN );
          mBTpoint.setTextColor( TDColor.SYMBOL_ON );
          mBTline.setTextColor(  TDColor.SYMBOL_TAB );
          mBTarea.setTextColor(  TDColor.SYMBOL_TAB );
        }
        break;
      case SymbolType.LINE:
        mList.setAdapter( mLineAdapter );
        // mBTpoint.getBackground().setColorFilter( TDColor.LIGHT_GRAY, PorterDuff.Mode.DARKEN );
        // mBTline.getBackground().setColorFilter(  TDColor.LIGHT_BLUE, PorterDuff.Mode.LIGHTEN );
        // mBTarea.getBackground().setColorFilter(  TDColor.LIGHT_GRAY, PorterDuff.Mode.DARKEN );
        mBTpoint.setTextColor( TDColor.SYMBOL_TAB );
        mBTline.setTextColor(  TDColor.SYMBOL_ON );
        mBTarea.setTextColor(  TDColor.SYMBOL_TAB );
        break;
      case SymbolType.AREA:
        if ( TDLevel.overBasic ) {
          mList.setAdapter( mAreaAdapter );
          // mBTpoint.getBackground().setColorFilter( TDColor.LIGHT_GRAY, PorterDuff.Mode.DARKEN );
          // mBTline.getBackground().setColorFilter(  TDColor.LIGHT_GRAY, PorterDuff.Mode.DARKEN );
          // mBTarea.getBackground().setColorFilter(  TDColor.LIGHT_BLUE, PorterDuff.Mode.LIGHTEN );
          mBTpoint.setTextColor( TDColor.SYMBOL_TAB );
          mBTline.setTextColor(  TDColor.SYMBOL_TAB );
          mBTarea.setTextColor(  TDColor.SYMBOL_ON );
        }
        break;
    }
    mType = new_type;
    mList.invalidate();
  }

  @Override
  public void onClick(View view)
  {
    // TDLog.Log( TDLog.LOG_PLOT, "DrawingLinePickerDialog::onClick" );
    int type = -1;
    int vid = view.getId();
    if ( vid == R.id.symbol_point ) {
      if ( TDLevel.overBasic ) type = SymbolType.POINT;
    } else if ( vid == R.id.symbol_line ) {
      type = SymbolType.LINE;
    } else if ( vid == R.id.symbol_area ) {
      if ( TDLevel.overBasic ) type = SymbolType.AREA;
    // } else if ( vid == R.id.symbol_reload ) {
    //   String old_version = mApp.mDData.getValue( "symbol_version" );
    //   if ( old_version == null ) old_version = "-";
    //   String message = String.format( mContext.getResources().getString( R.string.symbols_ask ), 
    //     mApp.SYMBOL_VERSION, old_version );
    //   TopoDroidAlertDialog.makeAlert( mContext, mContext.getResources(), message, // R.string.symbols_ask,
    //     new DialogInterface.OnClickListener() {
    //       @Override
    //       public void onClick( DialogInterface dialog, int btn ) {
    //         mApp.installSymbols( true );
    //         BrushManager.loadAllLibraries( mContext.getResources() );
    //         createAdapters();
    //         updateList();
    //       }
    //     }
    //   );
    }
    if ( type >= 0 && type != mType ) {
      updateList( /* mType, */ type );
    }
    // dismiss();
  }

  // static SaveSymbols mSaveSymbols = null; // new SaveSymbols();

  public void onBackPressed()
  {
    SaveSymbols save = new SaveSymbols();
    save.setAdapters( mPointAdapter, mLineAdapter, mAreaAdapter );
    save.execute();
    // if ( mSaveSymbols == null ) mSaveSymbols = new SaveSymbols();
    // if ( mSaveSymbols.setAdapters( mPointAdapter, mLineAdapter, mAreaAdapter ) ) {
    //   mSaveSymbols.execute();
    // }
    dismiss();
  }

  private static class SaveSymbols extends AsyncTask< Void, Void, Void > // FIXME static or LEAK
  {
    boolean run = false;
    SymbolAdapter mPtAdapter = null;
    SymbolAdapter mLnAdapter = null;
    SymbolAdapter mArAdapter = null;

    boolean setAdapters( SymbolAdapter pt_adapter, SymbolAdapter ln_adapter, SymbolAdapter ar_adapter )
    {
      if ( run ) return false;
      run = true;
      mPtAdapter = pt_adapter;
      mLnAdapter = ln_adapter;
      mArAdapter = ar_adapter;
      return true;
    }

    protected Void doInBackground( Void ... v )
    { 
      if ( TDLevel.overBasic ) {
        mPtAdapter.updateSymbols( "p_" );
        SymbolPointLibrary point_lib = BrushManager.getPointLib();
        if ( point_lib != null ) point_lib.makeEnabledList();
      }

      mLnAdapter.updateSymbols( "l_" );
      SymbolLineLibrary line_lib   = BrushManager.getLineLib();
      if ( line_lib  != null ) line_lib.makeEnabledList();

      if ( TDLevel.overBasic ) {
        mArAdapter.updateSymbols( "a_" );
        SymbolAreaLibrary area_lib   = BrushManager.getAreaLib();
        if ( area_lib  != null ) area_lib.makeEnabledList();
      }
      run = false;
      return null;
    }

    // protected void onProgressUpdate() { }
    // protected void onPostExecute( Void result ) { }
  }
}

