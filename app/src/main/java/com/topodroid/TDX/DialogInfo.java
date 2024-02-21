/* @file DialogInfo.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief Cave3D drawing infos dialog
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

// import com.topodroid.utils.TDLog;
import com.topodroid.ui.MyDialog;

import android.os.Bundle;
// import android.content.Context;
import android.content.res.Resources;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TableRow;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.Locale;


class DialogInfo extends MyDialog 
                 implements OnItemClickListener
                 , View.OnClickListener
{
  // private Button mBtnOk;

  private final TopoGL mTopoGl;
  private final TglParser mParser;
  private final GlRenderer   mRenderer;

  private ArrayAdapter<String> mArrayAdapter;
  private ListView mList;
  private Button   mBTclose;

  /** cstr
   * @param topogl     Cave3D activity
   * @param parser     3D model parser
   * @param renderer   GL renderer
   */
  public DialogInfo( TopoGL topogl, TglParser parser, GlRenderer renderer )
  {
    super( topogl, null, R.string.DialogInfo ); // null app
    mTopoGl   = topogl;
    mParser   = parser;
    mRenderer = renderer;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    initLayout( R.layout.cave3d_info_dialog, R.string.INFO );


    Resources res = mTopoGl.getResources();

    TextView tv = ( TextView ) findViewById(R.id.info_grid);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_grid_value),  mParser.getGridSize() ) );

    tv = ( TextView ) findViewById(R.id.info_azimuth);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_view_value), mRenderer.getYAngle(), mRenderer.getXAngle() ) );

    tv = ( TextView ) findViewById(R.id.info_shot);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_shot_value), mParser.getShotNumber(), mParser.getSplayNumber() ) );

    tv = ( TextView ) findViewById(R.id.info_station);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_station_value), mParser.getStationNumber() ) );

    tv = ( TextView ) findViewById(R.id.info_survey);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_survey_value), mParser.getSurveyNumber() ) );

    tv = ( TextView ) findViewById(R.id.info_length);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_length_value), mParser.getCaveLength() ) );

    tv = ( TextView ) findViewById(R.id.info_surface);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_surface_value), mParser.getSurfaceLength() ) );

    tv = ( TextView ) findViewById(R.id.info_depth);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_depth_value), mParser.getCaveDepth() ) );

    tv = ( TextView ) findViewById(R.id.info_volume);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_volume_value), mParser.getVolume() ) );

    tv = ( TextView ) findViewById(R.id.info_origin);
    if ( mParser.hasOrigin() ) {
      Cave3DFix origin = mParser.getOrigin();
      tv.setText( String.format(Locale.US, res.getString(R.string.info_origin_value), origin.x, origin.y, origin.z ) );
      TextView tv1 = ( TextView ) findViewById(R.id.info_lonlat);
      if ( mParser.isWGS84() ) { 
        tv1.setText( String.format(Locale.US, res.getString(R.string.info_lonlat_geovalue), origin.longitude, origin.latitude ) );
        TextView tv2 = ( TextView ) findViewById(R.id.info_radii);
        tv2.setText( String.format(Locale.US, res.getString(R.string.info_radii_value), mParser.getWEradius(), mParser.getSNradius() ) );
      } else {
        tv1.setText( String.format(Locale.US, res.getString(R.string.info_lonlat_prjvalue), origin.longitude, origin.latitude ) );
        // FIXME origin.y 20230118
        TableRow tr2 = (TableRow) findViewById(R.id.info_row_radii);
        tr2.setVisibility( View.GONE );
      }
    } else { 
      tv.setText( R.string.info_origin_none );
    }

    tv = ( TextView ) findViewById(R.id.info_east);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_east_value), mParser.emin, mParser.emax ) );

    tv = ( TextView ) findViewById(R.id.info_north);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_north_value), mParser.nmin, mParser.nmax ) );

    tv = ( TextView ) findViewById(R.id.info_z);
    tv.setText( String.format(Locale.US, res.getString(R.string.info_z_value), mParser.zmin, mParser.zmax ) );

    int nr = mParser.getSurveyNumber();
    ListView mList = ( ListView ) findViewById(R.id.surveys_list );
    mArrayAdapter = new ArrayAdapter<String>( mTopoGl, R.layout.message );
    ArrayList< Cave3DSurvey > surveys = mParser.getSurveys();
    if ( surveys != null ) {
      for ( Cave3DSurvey s : surveys ) {
        mArrayAdapter.add( s.name );
      }
    }
    mList.setAdapter( mArrayAdapter );
    mList.setOnItemClickListener( this );
    mList.setDividerHeight( 2 );

    mBTclose = (Button) findViewById( R.id.button_close );
    mBTclose.setOnClickListener( this );
  }

  /** respond to user taps
   * @param view  tapped view (unused)
   */
  @Override
  public void onClick(View view)
  {
    // TDLog.v( "Info onClick()" );
    dismiss();
  }

  /** respond to taps on item views
   * @param parent    view parent container
   * @param view      clicked item view
   * @param position  position of the item in the container
   * @param id        item id (?)
   */
  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id)
  {
    CharSequence item = ((TextView) view).getText();
    String name = item.toString();
    Cave3DSurvey survey = mParser.getSurvey( name );
    if ( survey != null ) {
      ( new DialogSurvey( mTopoGl, survey ) ).show();
    } else {
      // TODO Toast.makeText( );
    }
  }

}

