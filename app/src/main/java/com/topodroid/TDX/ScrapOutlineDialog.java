/* @file ScrapOutlineDialog.java
 *
 * @author marco corvi
 * @date june 2017
 *
 * @brief TopoDroid list of scrap outlines
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.ui.MyDialog;
import com.topodroid.prefs.TDSetting;
import com.topodroid.common.PlotType;

import java.util.List;
// import java.util.ArrayList;

import android.os.Bundle;
// import android.app.Dialog;

import android.content.Context;
import android.content.res.Resources;

import android.view.View;
// import android.view.View.OnClickListener;

import android.widget.ArrayAdapter;
import android.widget.ListView;
// import android.widget.GridView;
import android.widget.Button;

// import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
// import android.widget.AdapterView.OnItemLongClickListener;

class ScrapOutlineDialog extends MyDialog
                         implements OnItemClickListener
                         // , OnItemLongClickListener
                         , View.OnClickListener
{
  private final DrawingWindow mParent;
  private ArrayAdapter<String> mArrayAdapter;
  // private ListItemAdapter mArrayAdapter;
  private Button mBtnBack;
  private Button mBtnClear;
  private Button mBtnMerge;
  private ListView mList;
  private List< PlotInfo > mPlots;

  /** cstr
   * @param context  context
   * @param parent   sketch window
   * @param app      application
   * @param plots    list of sketches (?)
   */
  ScrapOutlineDialog( Context context, DrawingWindow parent, TopoDroidApp app, List< PlotInfo > plots )
  {
    super( context, app, R.string.ScrapOutlineDialog );
    mParent = parent;
    mPlots  = plots;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
    initLayout(R.layout.scrap_outline_dialog, R.string.title_scraps_outline );
    
    mArrayAdapter = new ArrayAdapter<>( mContext, R.layout.message );
    // mArrayAdapter = new ListItemAdapter( mContext, R.layout.message );

    mList = (ListView) findViewById(R.id.list);
    mList.setAdapter( mArrayAdapter );
    mList.setOnItemClickListener( this );
    // mList.setDividerHeight( 2 );

    mBtnBack = (Button) findViewById(R.id.btn_back);
    mBtnBack.setOnClickListener( this );
    mBtnClear = (Button) findViewById(R.id.btn_clear);
    mBtnClear.setOnClickListener( this );
    mBtnMerge = (Button) findViewById(R.id.btn_merge);
    if ( TDLevel.overExpert && TDSetting.mPlotSplit ) {
      mBtnMerge.setOnClickListener( this );
    } else {
      mBtnMerge.setVisibility( View.GONE );
    }

    updateList();
  }

  /** update the list of sketches
   */
  private void updateList()
  {
    Resources res = mApp.getResources();
    mArrayAdapter.clear();
    for ( PlotInfo item : mPlots ) {
      String name = item.name.substring( 0, item.name.length() - 1 );
      mArrayAdapter.add( PlotType.plotTypeString( name, (int)PlotType.PLOT_PLAN, res ) );
    }
    mList.setAdapter( mArrayAdapter );
  }
 
  /** react to a user tap
   * @param v  tapped view
   */
  // @Override
  public void onClick(View v) 
  {
    // TDLog.Log(  TDLog.LOG_INPUT, "ScrapOutlineDialog onClick() " );
    Button b = (Button) v;
    if ( b == mBtnClear ) {
      mParent.addScrap( null );
    } else if ( TDSetting.mPlotSplit && b == mBtnMerge ) {
      mParent.mergeOutlineScrap( );
    // } else if ( b == mBtnBack ) {
      /* nothing */
    }
    dismiss();
  }

  // ---------------------------------------------------------------
  // list items click

  /** react to a user tap on an item in the list
   * @param parent parent window (unused)
   * @param view   tapped view (unused)
   * @param pos    tapped position
   * @param id     tapped ID (? unused)
   */
  @Override 
  public void onItemClick(AdapterView<?> parent, View view, int pos, long id)
  {
    // CharSequence item = ((TextView) view).getText();
    // String value = item.toString();
    // int from = value.indexOf('<');
    // if ( from < 0 ) return;
    // int to = value.lastIndexOf('>');
    // String plot_name = value.substring( from+1, to );
    // String type = value.substring( to+2 );

    // long plot_type = PlotType.PLOT_PLAN;
    // Resources res = mApp.getResources();
    // if ( res.getString( R.string.plan ).equals( type ) ) {
    //   plot_type = PlotType.PLOT_PLAN;
    // } else if ( res.getString( R.string.extended ).equals( type ) ) {
    //   plot_type = PlotType.PLOT_EXTENDED;
    // }
    mParent.addScrap( mPlots.get( pos ) );
    dismiss();
  }

}

