/* @file OverviewWindow.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid sketch overview activity
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDMath;
import com.topodroid.utils.TDTag;
import com.topodroid.utils.TDsafUri;
// import com.topodroid.utils.TDRequest;
import com.topodroid.utils.TDStatus;
import com.topodroid.utils.TDColor;
import com.topodroid.utils.TDLocale;
import com.topodroid.utils.TDUtil;
import com.topodroid.num.TDNum;
import com.topodroid.num.NumStation;
import com.topodroid.num.NumShot;
import com.topodroid.num.NumSplay;
import com.topodroid.math.Point2D;
import com.topodroid.ui.MyButton;
import com.topodroid.ui.MyHorizontalListView;
import com.topodroid.ui.MyHorizontalButtonView;
import com.topodroid.ui.MotionEventWrap;
import com.topodroid.help.HelpDialog;
import com.topodroid.help.UserManualActivity;
import com.topodroid.prefs.TDSetting;
import com.topodroid.prefs.TDPrefCat;
import com.topodroid.common.PlotType;

import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Configuration;

import android.util.TypedValue;

import android.graphics.PointF;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.Matrix;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;

// import android.app.Activity;

import android.view.MotionEvent;
import android.view.View;
import android.view.KeyEvent;

import android.widget.Button;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

// import android.graphics.RectF;
// import android.print.PrintAttributes;
// import android.print.pdf.PrintedPdfDocument;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfDocument.Page;
import android.graphics.pdf.PdfDocument.PageInfo;

import android.net.Uri;

import java.io.File;
// import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;

/**
 */
public class OverviewWindow extends ItemDrawer
                             implements View.OnTouchListener
                                      , View.OnClickListener
                                      , OnItemClickListener
                                      , OnZoomListener
                                      , IZoomer
				      , IExporter
{
  private static final int[] izons = {
                        R.drawable.iz_ruler_off,       // iz_measure
                        R.drawable.iz_mode,
                        R.drawable.iz_angle,
			R.drawable.iz_undo
                        // FIXME_OVER R.drawable.iz_plan,
                        // R.drawable.iz_menu,
                        // R.drawable.iz_ruler_on, // iz_measure_on,
                        // R.drawable.iz_polyline
			// R.drawable.iz_empty // EMPTY
                      };
  // FIXME_OVER private static int BTN_PLOT = 2;

  private static final int[] menus = {
                        R.string.menu_close,
			R.string.menu_export,
                        R.string.menu_options,
                        R.string.menu_help
                     };

  private static final int[] help_icons = {
                        R.string.help_measure,
                        R.string.help_refs,
                        R.string.help_measure_type,
                        // FIXME_OVER R.string.help_toggle_plot,
                        R.string.help_measure_undo,
                      };
  private static final int[] help_menus = {
                        R.string.help_close,
			R.string.help_save_plot,
                        R.string.help_prefs,
                        R.string.help_help
                      };

  private static final int HELP_PAGE = R.string.OverviewWindow;

  // FIXME_OVER BitmapDrawable mBMextend;
  // FIXME_OVER BitmapDrawable mBMplan;
  private BitmapDrawable mBMselect;
  private BitmapDrawable mBMselectOn;
  private BitmapDrawable mBMcontinueNo;
  private BitmapDrawable mBMcontinueOn;

  private final int IC_SELECT   = 0;
  private final int IC_CONTINUE = 2;
  private final int IC_UNDO     = 3;

  private float mDDtotal = 0;
  private int mTotal = 0;

  private float mUnitRuler = 1;

  private TopoDroidApp mApp;
  private DataHelper mData;
  // private DrawingUtil mDrawingUtil;

  // long getSID() { return TDInstance.sid; }
  // String getSurvey() { return TDInstance.survey; }
  private DrawingSurface  mOverviewSurface;

  private TDNum mNum;
  private Path mCrossPath;
  // private Path mCirclePath;

  // String mName1;  // first name (PLAN)
  // String mName2;  // second name (EXTENDED)

  private boolean mZoomBtnsCtrlOn = false;
  private ZoomButtonsController mZoomBtnsCtrl = null;
  private View mZoomView;
    // ZoomControls mZoomCtrl;
    // ZoomButton mZoomOut;
    // ZoomButton mZoomIn;

    private static final float ZOOM_INC = 1.4f;
    private static final float ZOOM_DEC = 1.0f/ZOOM_INC;

    static final private int MODE_MOVE = 2;
    static final private int MODE_ZOOM = 4;

    final static private int MEASURE_OFF   = 0;
    final static private int MEASURE_START = 1;
    final static private int MEASURE_ON    = 2;
    

    private int mTouchMode = MODE_MOVE;
    private int mOnMeasure = MEASURE_OFF;
    private boolean mIsContinue = false;
    private ArrayList< Point2D > mMeasurePts;

    private float mSaveX;
    private float mSaveY;
    private PointF mOffset  = new PointF( 0f, 0f );
    private PointF mDisplayCenter;
    private float mZoom  = 1.0f;

    private long mSid;     // survey id
    private long mType;    // current plot type
    private boolean mLandscape;
    // private String mFrom;
    // private PlotInfo mPlot1;

    @Override
    public void onVisibilityChanged(boolean visible)
    {
      if ( mZoomBtnsCtrlOn && mZoomBtnsCtrl != null ) {
        mZoomBtnsCtrl.setVisible( visible || ( TDSetting.mZoomCtrl > 1 ) );
      }
    }

    @Override
    public void onZoom( boolean zoomin )
    {
      if ( zoomin ) changeZoom( ZOOM_INC );
      else changeZoom( ZOOM_DEC );
    }

    private void changeZoom( float f ) 
    {
      float zoom = mZoom;
      mZoom     *= f;
      // TDLog.v( "zoom " + mZoom );
      mOffset.x -= mDisplayCenter.x*(1/zoom-1/mZoom);
      mOffset.y -= mDisplayCenter.y*(1/zoom-1/mZoom);
      mOverviewSurface.setTransform( this, mOffset.x, mOffset.y, mZoom, mLandscape );
      // mOverviewSurface.refresh();
      // mZoomCtrl.hide();
      // mZoomBtnsCtrl.setVisible( false );
    }

    // private void resetZoom() 
    // {
    //   int w = mOverviewSurface.width();
    //   int h = mOverviewSurface.height();
    //   mOffset.x = w/4;
    //   mOffset.y = h/4;
    //   mZoom = mApp.mScaleFactor;
    //   // TDLog.Log(TDLog.LOG_PLOT, "zoom one " + mZoom + " off " + mOffset.x + " " + mOffset.y );
    //   if ( mType == PlotType.PLOT_PLAN ) {
    //     float zx = w/(mNum.surveyEmax() - mNum.surveyEmin());
    //     float zy = h/(mNum.surveySmax() - mNum.surveySmin());
    //     mZoom = (( zx < zy )? zx : zy)/40;
    //   } else if ( PlotType.isProfile( mType ) ) { // FIXME OK PROFILE
    //     float zx = w/(mNum.surveyHmax() - mNum.surveyHmin());
    //     float zy = h/(mNum.surveyVmax() - mNum.surveyVmin());
    //     mZoom = (( zx < zy )? zx : zy)/40;
    //   } else {
    //     mZoom = mApp.mScaleFactor;
    //     mOffset.x = 0.0f;
    //     mOffset.y = 0.0f;
    //   }
    //     
    //   // TDLog.Log(TDLog.LOG_PLOT, "zoom one to " + mZoom );
    //     
    //   mOverviewSurface.setTransform( this, mOffset.x, mOffset.y, mZoom, mLandscape );
    //   // mOverviewSurface.refresh();
    // }

    public void zoomIn()  { changeZoom( ZOOM_INC ); }
    public void zoomOut() { changeZoom( ZOOM_DEC ); }
    // public void zoomOne() { resetZoom( ); }

    // public void zoomView( )
    // {
    //   // TDLog.Log( TDLog.LOG_PLOT, "zoomView ");
    //   DrawingZoomDialog zoom = new DrawingZoomDialog( mOverviewSurface.getContext(), this );
    //   zoom.show();
    // }


    // splay = false
    // selectable = false
    private void addFixedSplayLine( DBlock blk, double x1, double y1, double x2, double y2 ) // float xoff, float yoff,
    {
      DrawingSplayPath dpath = null;
      dpath = new DrawingSplayPath( blk, mOverviewSurface.scrapIndex() );
      if ( blk.mClino > TDSetting.mVertSplay ) {
        dpath.setPathPaint( BrushManager.paintSplayXBdot );
      } else if ( blk.mClino < -TDSetting.mVertSplay ) {
        dpath.setPathPaint( BrushManager.paintSplayXBdash );
      } else {
        dpath.setPathPaint( BrushManager.paintSplayXB );
      }
      DrawingUtil.makeDrawingSplayPath( dpath, (float)x1, (float)y1, (float)x2, (float)y2 );
      mOverviewSurface.addFixedSplayPath( dpath, false ); // false: non-selectable
    }
      
    private void addFixedLine( DBlock blk, double x1, double y1, double x2, double y2 ) // float xoff, float yoff,
    {
      DrawingPath dpath = null;
      dpath = new DrawingPath( DrawingPath.DRAWING_PATH_FIXED, blk, mOverviewSurface.scrapIndex() );
      dpath.setPathPaint( BrushManager.fixedShotPaint );
      // DrawingUtil.makeDrawingPath( dpath, x1, y1, x2, y2, xoff, yoff );
      DrawingUtil.makeDrawingPath( dpath, (float)x1, (float)y1, (float)x2, (float)y2 );
      mOverviewSurface.addFixedLegPath( dpath, false ); // false: non-selectable
    }

    // --------------------------------------------------------------------------------------

    // @Override // overridden method is empty
    // public void setTheTitle()
    // {
    //   // setTitle( res.getString( R.string.title_move ) );
    // }

  private void computeReferences( int type,
                                  // float xoff, float yoff,
                                  float zoom )
  {
    // TDLog.v( "Overview compute reference. off " + xoff + " " + yoff + " zoom " + zoom );
    // FIXME_OVER
    // mOverviewSurface.clearReferences( type );
    // mOverviewSurface.setManager( DrawingSurface.DRAWING_OVERVIEW, type ); 
    mOverviewSurface.newReferences( DrawingSurface.DRAWING_OVERVIEW, type ); 
   
    float decl = ( type == PlotType.PLOT_PLAN )? TopoDroidApp.mData.getSurveyDeclination(mSid) : 0;
    mOverviewSurface.addScaleRef( DrawingSurface.DRAWING_OVERVIEW, type, decl );

    // float xoff = 0; float yoff = 0;

    if ( type == PlotType.PLOT_PLAN ) {
      DrawingUtil.addGrid( mNum.surveyEmin(), mNum.surveyEmax(), mNum.surveySmin(), mNum.surveySmax(), mOverviewSurface );
                           // xoff, yoff, mOverviewSurface );
    } else {
      DrawingUtil.addGrid( mNum.surveyHmin(), mNum.surveyHmax(), mNum.surveyVmin(), mNum.surveyVmax(), mOverviewSurface );
                           // xoff, yoff, mOverviewSurface );
    }

    List< NumStation > stations = mNum.getStations();
    List< NumShot >    shots    = mNum.getShots();
    List< NumSplay >   splays   = mNum.getSplays();
    // TDLog.v( "Overview stations " + stations.size() + " shots " + shots.size() + " splays " + splays.size() );

    if ( type == PlotType.PLOT_PLAN ) {
      for ( NumShot sh : shots ) {
        NumStation st1 = sh.from;
        NumStation st2 = sh.to;
        addFixedLine( sh.getFirstBlock(), st1.e, st1.s, st2.e, st2.s ); // xoff, yoff
      }
      for ( NumSplay sp : splays ) {
        if ( Math.abs( sp.getBlock().mClino ) < TDSetting.mSplayVertThrs ) {
          NumStation st = sp.from;
          addFixedSplayLine( sp.getBlock(), st.e, st.s, sp.e, sp.s ); // xoff, yoff
        }
      }
      for ( NumStation st : stations ) {
        DrawingStationName dst;
        // dst = mOverviewSurface.addDrawingStationName( null, st, DrawingUtil.toSceneX(st.e,st.s) - xoff,
        //                                                   DrawingUtil.toSceneY(st.e,st.s) - yoff, true, null );
        dst = mOverviewSurface.addDrawingStationName( null, st, DrawingUtil.toSceneX(st.e,st.s), DrawingUtil.toSceneY(st.e,st.s), true, null, null );
      }
    } else { // if ( PlotType.isProfile( type ) // FIXME OK PROFILE
      for ( NumShot sh : shots ) {
        if  ( ! sh.mIgnoreExtend ) {
          NumStation st1 = sh.from;
          NumStation st2 = sh.to;
          addFixedLine( sh.getFirstBlock(), st1.h, st1.v, st2.h, st2.v ); // xoff, yoff
        }
      } 
      for ( NumSplay sp : splays ) {
        NumStation st = sp.from;
        addFixedSplayLine( sp.getBlock(), st.h, st.v, sp.h, sp.v ); // xoff, yoff
      }
      for ( NumStation st : stations ) {
        DrawingStationName dst;
        // dst = mOverviewSurface.addDrawingStationName( null, st, DrawingUtil.toSceneX(st.h,st.v) - xoff,
        //                                                   DrawingUtil.toSceneY(st.h,st.v) - yoff, true, null );
        dst = mOverviewSurface.addDrawingStationName( null, st, DrawingUtil.toSceneX(st.h,st.v), DrawingUtil.toSceneY(st.h,st.v), true, null, null );
      }
    }

    mOverviewSurface.commitReferences();

    // FIXME mCheckExtend
    // if ( (! mNum.surveyAttached) && TDSetting.mCheckAttached ) {
    //   TDToast.makeBad( R.string.survey_not_attached );
    // }
  }
    

    // ------------------------------------------------------------------------------
    // BUTTON BAR
  
    private Button[] mButton1;  // primary
    private int mNrButton1 = 4; // main-primary
    private MyHorizontalListView mListView;
    private MyHorizontalButtonView mButtonView1;
    private ListView   mMenu;
    private Button     mMenuImage;
    private boolean onMenu;

    private List< DBlock > mBlockList = null;
  
    public float zoom() { return mZoom; }


    // this method is a callback to let other objects tell the activity to use zooms or not
    private void switchZoomCtrl( int ctrl )
    {
      // TDLog.v( "DEBUG switchZoomCtrl " + ctrl + " ctrl is " + ((mZoomBtnsCtrl == null )? "null" : "not null") );
      if ( mZoomBtnsCtrl == null ) return;
      mZoomBtnsCtrlOn = (ctrl > 0);
      switch ( ctrl ) {
        case 0:
          mZoomBtnsCtrl.setOnZoomListener( null );
          mZoomBtnsCtrl.setVisible( false );
          mZoomBtnsCtrl.setZoomInEnabled( false );
          mZoomBtnsCtrl.setZoomOutEnabled( false );
          mZoomView.setVisibility( View.GONE );
          break;
        case 1:
          mZoomView.setVisibility( View.VISIBLE );
          mZoomBtnsCtrl.setOnZoomListener( this );
          mZoomBtnsCtrl.setVisible( false );
          mZoomBtnsCtrl.setZoomInEnabled( true );
          mZoomBtnsCtrl.setZoomOutEnabled( true );
          break;
        case 2:
          mZoomView.setVisibility( View.VISIBLE );
          mZoomBtnsCtrl.setOnZoomListener( this );
          mZoomBtnsCtrl.setVisible( true );
          mZoomBtnsCtrl.setZoomInEnabled( true );
          mZoomBtnsCtrl.setZoomOutEnabled( true );
          break;
      }
    }

    static final private int ROD = 10; // measure path size

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
      super.onCreate(savedInstanceState);

      TDandroid.setScreenOrientation( this );

      mUnitRuler = TDSetting.mUnitMeasure;
      if ( mUnitRuler < 0 ) mUnitRuler = TDSetting.mUnitGrid;

      // mCrossPath = new Path();
      // mCrossPath.moveTo(  ROD,  ROD);
      // mCrossPath.lineTo( -ROD, -ROD);
      // mCrossPath.moveTo(  ROD, -ROD);
      // mCrossPath.lineTo( -ROD, ROD);
      // mCirclePath = new Path();
      // mCirclePath.addCircle( 0, 0, ROD, Path.Direction.CCW );
      // mCirclePath.moveTo( -ROD,   0);
      // mCirclePath.lineTo(  ROD,   0);
      // mCirclePath.moveTo(   0, -ROD);
      // mCirclePath.lineTo(   0,  ROD);

      mMeasurePts = new ArrayList< Point2D >();

      // Display display = getWindowManager().getDefaultDisplay();
      // DisplayMetrics dm = new DisplayMetrics();
      // display.getMetrics( dm );
      // int width = dm widthPixels;
      int width = getResources().getDisplayMetrics().widthPixels;

      setContentView(R.layout.overview_activity);
      mApp = (TopoDroidApp)getApplication();
      mActivity = this;
      Resources res = getResources();
      // mZoom = mApp.mScaleFactor;    // canvas zoom

      mDisplayCenter = new PointF(TopoDroidApp.mDisplayWidth  / 2, TopoDroidApp.mDisplayHeight / 2);

      mOverviewSurface = (DrawingSurface) findViewById(R.id.drawingSurface);
      mOverviewSurface.setZoomer( this );
      mOverviewSurface.setOnTouchListener(this);

      mZoomView = (View) findViewById(R.id.zoomView );
      mZoomBtnsCtrl = new ZoomButtonsController( mZoomView );

      switchZoomCtrl( TDSetting.mZoomCtrl );

      mListView = (MyHorizontalListView) findViewById(R.id.listview);
      mListView.setEmptyPlaceholder( true );
      /* int size = */ TopoDroidApp.setListViewHeight( getApplicationContext(), mListView );

      mButton1 = new Button[ mNrButton1 + 1 ];
      for ( int k=0; k < mNrButton1; ++k ) {
        mButton1[k] = MyButton.getButton( mActivity, this, izons[k] );
        if ( k == 0 ) {
          mBMselect = MyButton.getButtonBackground( this, res, izons[k] );
        } else if ( k == 2 ) {
          mBMcontinueNo = MyButton.getButtonBackground( this, res, izons[k] );
        }  
        // FIXME_OVER } else if ( k == 2 ) { // IC_PLAN = 2;
        // FIXME_OVER   mBMplan = bm;
      }
      mButton1[mNrButton1] = MyButton.getButton( mActivity, null, R.drawable.iz_empty );
      mBMselectOn   = MyButton.getButtonBackground( this, res, R.drawable.iz_ruler_on ); // iz_measure_on
      mBMcontinueOn = MyButton.getButtonBackground( this, res, R.drawable.iz_polyline );
      // FIXME_OVER mBMextend  = MyButton.getButtonBackground( this, res, izons[IC_EXTEND] ); 

      mButtonView1 = new MyHorizontalButtonView( mButton1 );
      mListView.setAdapter( mButtonView1.mAdapter );

      setTheTitle();

      mData         = TopoDroidApp.mData; 
      Bundle extras = getIntent().getExtras();
      if ( extras == null ) { finish(); return; } // extra can be null [ Galaxy S7, Galaxy A30s ] 
      mSid          = extras.getLong( TDTag.TOPODROID_SURVEY_ID );
      // mFrom      = extras.getString( TDTag.TOPODROID_PLOT_FROM );
      mZoom         = extras.getFloat( TDTag.TOPODROID_PLOT_ZOOM );
      mType         = (int)extras.getLong( TDTag.TOPODROID_PLOT_TYPE );
      mLandscape    = extras.getBoolean( TDTag.TOPODROID_PLOT_LANDSCAPE );
      // // mDrawingUtil = mLandscape ? (new DrawingUtilLandscape()) : (new DrawingUtilPortrait());
      // mDrawingUtil = new DrawingUtilPortrait();

      // TDLog.v( "Overview from " + mFrom + " Type " + mType + " Zoom " + mZoom );

      mMenuImage = (Button) findViewById( R.id.handle );
      mMenuImage.setOnClickListener( this );
      TDandroid.setButtonBackground( mMenuImage, MyButton.getButtonBackground( this, res, R.drawable.iz_menu ) );
      mMenu = (ListView) findViewById( R.id.menu );
      mMenu.setOnItemClickListener( this );

      doStart();
      // TDLog.v( "Overview offset " + mOffset.x + " " + mOffset.y );

      mOffset.x   += extras.getFloat( TDTag.TOPODROID_PLOT_XOFF );
      mOffset.y   += extras.getFloat( TDTag.TOPODROID_PLOT_YOFF );
      mOverviewSurface.setTransform( this, mOffset.x, mOffset.y, mZoom, mLandscape );
    }

  /** react to the activity "resume"
   */
  @Override
  protected synchronized void onResume()
  {
    super.onResume();
    doResume();
  }

  /** react to the activity "pause"
   */
  @Override
  protected synchronized void onPause() 
  { 
    super.onPause();
    // TDLog.v( "Drawing Activity onPause " + ((mDataDownloader!=null)?"with DataDownloader":"") );
    doPause();
  }

  /** react to the activity "start"
   */
  @Override
  public void onStart() 
  {
    super.onStart();
    TDLocale.resetTheLocale();
    setMenuAdapter( getResources() );
    closeMenu();
  }

  /** react to the activity "stop"
   */
  @Override
  protected synchronized void onStop()
  {
    super.onStop();
    mOverviewSurface.setDisplayMode( mOverviewSurface.getDisplayMode() & DisplayMode.DISPLAY_OVERVIEW );
  }

  /** implement the actions of a "resume"
   */
  private void doResume()
  {
    // PlotInfo info = mApp.mData.getPlotInfo( mSid, mName );
    // mOffset.x = info.xoffset;
    // mOffset.y = info.yoffset;
    // mZoom     = info.zoom;
    mOverviewSurface.setDrawing( true );
    switchZoomCtrl( TDSetting.mZoomCtrl );
  }

  /** implement the actions of a "pause"
   */
  private void doPause()
  {
    switchZoomCtrl( 0 );
    mOverviewSurface.setDrawing( false );
  }

  /** implement the actions of a "start"
   */
  private void doStart()
  {
    if ( mData == null ) {
      TDLog.Error("OverviewWindow start with null DB");
      finish();
      return;
    }
    // TDLog.Log( TDLog.LOG_PLOT, "do Start " + mName1 + " " + mName2 );
    // mBlockList = mData.selectAllLegShots( mSid, TDStatus.NORMAL );
    mBlockList = mData.selectAllShots( mSid, TDStatus.NORMAL );
    if ( TDUtil.isEmpty(mBlockList) ) {
      TDToast.makeBad( R.string.few_data );
      finish();
    } else {
      loadFiles( mType ); 
    }
  }

// ----------------------------------------------------------------------------

  // boolean mAllSymbols = true;

  private void loadFiles( long type )
  {
    // List< PlotInfo > plots = mApp.mData.selectAllPlotsWithType( mSid, TDStatus.NORMAL, type, landscape );
    List< PlotInfo > plots = TopoDroidApp.mData.selectAllPlotsWithType( mSid, TDStatus.NORMAL, type );

    // TDLog.v( "Overview plots " + plots.size() );

    // if ( plots.size() < 1 ) { // N.B. this should never happen
    //   TDToast.makeBad( R.string.few_plots );
    //   finish();
    //   return;
    // }
    // mAllSymbols  = true; // by default there are all the symbols
    // SymbolsPalette missingSymbols = new SymbolsPalette(); 

    NumStation mStartStation = null;

    mOverviewSurface.resetManager( DrawingSurface.DRAWING_OVERVIEW, null, false ); // is_extended = false

    for ( int k=0; k<plots.size(); ++k ) {
      PlotInfo plot = plots.get(k);
      // TDLog.v( "plot " + plot.name );

      String start = plot.start;
      float xdelta = 0.0f;
      float ydelta = 0.0f;
      if ( k == 0 ) {
        String view  = plot.view;
        // mPlot1 = plot;
        // mPid = plot.id;
        // NOTE Overview only for plan or extended plots
        // float decl = mData.getSurveyDeclination( mSid );
        mNum = new TDNum( mBlockList, start, null, null, 0.0f, null ); // null formatClosure
        mStartStation = mNum.getStation( start );
        // computeReferences( (int)type, mOffset.x, mOffset.y, mZoom );
        computeReferences( (int)type, mZoom );
        // TDLog.v( "Overview num stations " + mNum.stationsNr() + " shots " + mNum.shotsNr() );
      } else {
        NumStation st = mNum.getStation( start );
        if ( st == null ) continue;
        if ( type == PlotType.PLOT_PLAN ) {
          xdelta = (float)(st.e - mStartStation.e); // FIXME SCALE FACTORS ???
          ydelta = (float)(st.s - mStartStation.s);
        } else {
          xdelta = (float)(st.h - mStartStation.h);
          ydelta = (float)(st.v - mStartStation.v);
        }
      }
      xdelta *= DrawingUtil.SCALE_FIX;
      ydelta *= DrawingUtil.SCALE_FIX;
      // TDLog.v( " delta " + xdelta + " " + ydelta );

      // now try to load drawings from therion file
      String fullName = TDInstance.survey + "-" + plot.name;
      // TDLog.v( "load tdr file " + fullName );

      String tdr = TDPath.getTdrFileWithExt( fullName );
      mOverviewSurface.addLoadDataStream( tdr, xdelta, ydelta, /* null, */ fullName ); // save plot fullname in paths
    }

    // if ( ! mAllSymbols ) {
    //   String msg = missingSymbols.getMessage( getResources() );
    //   TDLog.Log( TDLog.LOG_PLOT, "Missing " + msg );
    //   TDToast.makeBad( "Missing symbols \n" + msg );
    //   // (new MissingDialog( this, this, msg )).show();
    //   // finish();
    // }

    // // resetZoom();
    // resetReference( mPlot1 );
  }

  /** export drawing 
   // * @param uri    URI of the export file
   * @param ext    export type (ie, extension)
   * @note called only by export menu
   */
  private void saveWithExt( /* Uri uri, */ final String ext )
  {
    TDNum num = mNum;
    final String fullname = TDInstance.survey + ( (mType == PlotType.PLOT_PLAN )? "-p" : "-s" );
    TDLog.v( "OVERVIEW export plot type " + mType + " with extension " + ext + " " + fullname );
    DrawingCommandManager manager = mOverviewSurface.getManager( DrawingSurface.DRAWING_OVERVIEW );

    // APP_OUT_DIR
    // if ( uri == null ) return;
    String filename = fullname + "." + ext;
    Uri uri = Uri.fromFile( new File( TDPath.getOutFile( filename ) ) );
    // TDLog.v("EXPORT " + TDPath.getOutFile( filename ) );

    if ( "pdf".equals( ext ) ) {
      savePdf( uri );
    } else {
      if ( ext.equals("th2") ) {
        Handler th2Handler = new Handler() {
           @Override public void handleMessage(Message msg) {
             if (msg.what == 661 ) {
               TDToast.make( String.format( getString(R.string.saved_file_1), fullname ) ); 
             } else {
               TDToast.makeBad( R.string.saving_file_failed );
             }
           }
        };
        // parent is null because this is user-requested EXPORT
        // fullname is null
        // azimuth = 0
        // oblique = 0
        // save = OVERVIEW
        // rotate  = 0
        (new SavePlotFileTask( this, uri, null, th2Handler, mNum, manager, null, fullname, mType, 0, 0, PlotSave.OVERVIEW, 0, false )).execute(); // TH2EDIT false
      } else {
        GeoReference station = null;
        if ( mType == PlotType.PLOT_PLAN && ext.equals("shz") ) {
         String origin = mNum.getOriginStation();
         station = TDExporter.getGeolocalizedStation( mSid, mData, 1.0f, true, origin, true );
        }
        SurveyInfo info = mData.selectSurveyInfo( mSid );
        // null PlotInfo, null FixedInfo, true toast
        (new ExportPlotToFile( this, uri, info, null, null, mNum, manager, mType, fullname, ext, true, station )).execute();
      }
    }
  }

  private static int mExportIndex;
  private static String mExportExt;

  /** export the drawing
   * @param export_type   export file format
   * @param filename      export file "name"
   * @param prefix        station name prefix (not used)
   * @param second        whether to export the second view (unused)
   * @note called by the ExportPlotDialog
   */
  public void doExport( String export_type, String filename, String prefix, boolean second ) // EXPORT
  {
    if ( export_type == null ) return;
    mExportIndex = TDConst.plotExportIndex( export_type );
    mExportExt   = TDConst.plotExportExt( export_type );
    if ( mExportIndex < 0 ) { 
      TDLog.Error("Error. Overview export: type " + export_type + " index " + mExportIndex + " ext " + mExportExt );
      return;
    }
    // TDLog.v("Overview export: type " + export_type + " index " + mExportIndex + " ext " + mExportExt );

    // APP_OUT_DIR
    // Intent intent = new Intent( Intent.ACTION_CREATE_DOCUMENT );
    // intent.setType( TDConst.mMimeType[ mExportIndex ] );
    // intent.addCategory(Intent.CATEGORY_OPENABLE);
    // intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    // // intent.putExtra( "exporttype", index ); // index is not returned to the app
    // intent.putExtra( Intent.EXTRA_TITLE, filename );
    // startActivityForResult( Intent.createChooser(intent, getResources().getString( R.string.export_overview_title ) ), TDRequest.REQUEST_GET_EXPORT );

    saveWithExt( mExportExt );
  }

  // /** react to a called activity result ---- APP_OUT_DIR
  //  * @param request   request passed to the activity
  //  * @param result    result code (OK or CANCEL)
  //  * @param intent    intent with result data
  //  *
  //  * used only with request GET_EXPORT. The result data contains the export URI.
  //  */
  // public void onActivityResult( int request, int result, Intent intent ) 
  // {
  //   // TDLog.Log( TDLog.LOG_MAIN, "on Activity Result: request " + mRequestName[request] + " result: " + result );
  //   // if ( ! TDSetting.mExportUri ) return;
  //   if ( intent == null ) return;
  //   // Bundle extras = intent.getExtras();
  //   switch ( request ) {
  //     case TDRequest.REQUEST_GET_EXPORT:
  //       if ( result == Activity.RESULT_OK ) {
  //         // int index = intent.getIntExtra( "exporttype", -1 );
  //         Uri uri = intent.getData();
  //         // TDLog.v( "URI Export " + mExportIndex + " uri " + uri.toString() );
  //         doUriExport( uri );
  //       }
  //     // default:
  //     //   super.onActivityResult( request, result, intent );
  //   }
  // }

  // APP_OUT_DIR
  // /** export drawing: either export with a specific format (extension) or export as PDF
  //  * @param uri   export URI
  //  */
  // public void doUriExport( Uri uri ) 
  // {
  //   // TDLog.v( "Overview URI export: index " + mExportIndex );
  //   switch ( mExportIndex ) {
  //     case TDConst.SURVEY_FORMAT_TH2: saveWithExt( uri, "th2" ); break;
  //     case TDConst.SURVEY_FORMAT_DXF: saveWithExt( uri, "dxf" ); break; 
  //     case TDConst.SURVEY_FORMAT_SVG: saveWithExt( uri, "svg" ); break;
  //     case TDConst.SURVEY_FORMAT_SHP: saveWithExt( uri, "shz" ); break;
  //     case TDConst.SURVEY_FORMAT_XVI: saveWithExt( uri, "xvi" ); break;
  //     case TDConst.SURVEY_FORMAT_PDF: savePdf( uri ); break;
  //     default:
  //       TDLog.Error("Unexpected export index " + mExportIndex );
  //       break;
  //   }
  // }

  // PDF ------------------------------------------------------------------
  /** export drawing as PDF
   * @param uri   export URI
   */
  private void savePdf( Uri uri ) 
  {
    String fullname = TDInstance.survey + ( (mType == PlotType.PLOT_PLAN )? "-p" : "-s" );
    if ( fullname != null ) { // always true
      DrawingCommandManager manager = mOverviewSurface.getManager( DrawingSurface.DRAWING_OVERVIEW );
      doSavePdf( uri, manager, fullname );
    } else {
      TDLog.Error("ERROR PDF fullname is null");
    }
  }

  /** export drawing as PDF
   * @param uri      export URI
   * @param manager  drawing items
   * @param fullname export "name" - used only in the toast
   * TODO with background task
   */
  private void doSavePdf( Uri uri, DrawingCommandManager manager, final String fullname )
  {
    if ( manager == null ) {
      TDToast.makeBad( R.string.null_bitmap );
      return;
    }
    if ( TDandroid.BELOW_API_19 ) { // Android-4.4 (KITKAT)
      TDToast.makeBad( R.string.no_feature_pdf );
      return;
    }

    // if ( ! TDSetting.mExportUri ) uri = null; // FIXME_URI

    // TDPath.getPdfDir();
    // String filename = TDPath.getPdfFileWithExt( fullname );
    // TDLog.v( "Overview PDF export <" + filename + ">");
    ParcelFileDescriptor pfd = TDsafUri.docWriteFileDescriptor( uri );
    if ( pfd == null ) return;
    try {
      OutputStream fos = null;
      // if ( uri != null ) {
        // TDLog.v( "Export overview PDF: uri " + uri.toString() );
        fos = TDsafUri.docFileOutputStream( pfd );
      // } else {
      //   // TDLog.v( "Export overview PDF " + fullname + " --> " + TDPath.getPdfFileWithExt( fullname ) );
      //   fos = new FileOutputStream( TDPath.getPdfFileWithExt( fullname ) );
      // }

      PageInfo info = getPdfPage( manager );
      PdfDocument pdf = new PdfDocument( );
      Page page = pdf.startPage( info );

      manager.executeAll( page.getCanvas(), -1.0f, null, true ); // zoom is 1.0, true = inverted_color
      // manager.executeAll( page.getCanvas(), -1.0f, null ); // zoom is 1.0
      pdf.finishPage( page );
      pdf.writeTo( fos );
      pdf.close();
      /* if ( fos != null ) */ fos.close(); // test always true
      TDToast.make( String.format( getResources().getString(R.string.saved_file_1), fullname ) ); // PDF
    } catch ( IOException e ) {
      TDLog.Error("Failed PDF export " + e.getMessage() );
    } finally {
      TDsafUri.closeFileDescriptor( pfd );
    }
  }

  // private void saveReference( PlotInfo plot, long pid )
  // {
  //   // TDLog.v( "save ref " + mOffset.x + " " + mOffset.y + " " + mZoom );
  //   plot.xoffset = mOffset.x;
  //   plot.yoffset = mOffset.y;
  //   plot.zoom    = mZoom;
  //   mData.updatePlot( pid, mSid, mOffset.x, mOffset.y, mZoom );
  // }

  // private void resetReference( PlotInfo plot )
  // {
  //   mOffset.x = plot.xoffset; 
  //   mOffset.y = plot.yoffset; 
  //   mZoom     = plot.zoom;    
  //   // TDLog.v( "reset ref " + mOffset.x + " " + mOffset.y + " " + mZoom );
  //   mOverviewSurface.setTransform( this, mOffset.x, mOffset.y, mZoom, mLandscape );
  //   // mOverviewSurface.refresh();
  // }

  private float mSave0X, mSave0Y;
  private float mSave1X, mSave1Y;

  /*
  private void dumpEvent( MotionEventWrap ev )
  {
    String name[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "PTR_DOWN", "PTR_UP", "7?", "8?", "9?" };
    StringBuilder sb = new StringBuilder();
    int action = ev.getAction();
    int actionCode = action & MotionEvent.ACTION_MASK;
    sb.append( "Event action_").append( name[actionCode] );
    if ( actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP ) {
      sb.append( "(pid " ).append( action>>MotionEvent.ACTION_POINTER_ID_SHIFT ).append( ")" );
    }
    sb.append( " [" );
    for (int i=0; i<ev.getPointerCount(); ++i ) {
      sb.append( "#" ).append( i );
      sb.append( "(pid " ).append( ev.getPointerId(i) ).append( ")=" ).append( (int)(ev.getX(i)) ).append( "." ).append( (int)(ev.getY(i)) );
      if ( i+1 < ev.getPointerCount() ) sb.append( ":" );
    }
    sb.append( "]" );
    // TDLog.Log(TDLog.LOG_PLOT, sb.toString() );
  }
  */

  /** @return the distance between the first two pointer of the touch event, 0 if the event does not have at least two pointers
   * @param ev   touch event
   */
  private float spacing(MotionEventWrap ev)
  {
    int np = ev.getPointerCount();
    if ( np < 2 ) return 0.0f;
    float x = ev.getX(1) - ev.getX(0);
    float y = ev.getY(1) - ev.getY(0);
    return TDMath.sqrt(x*x + y*y);
  }

  /** store the touch event pointer coordinates for later use
   * @param ev   touch event
   * @note the pointers are stored in mSave0 and mSave1
   */
  private void saveEventPoint(MotionEventWrap ev)
  {
    int np = ev.getPointerCount();
    if ( np >= 1 ) {
      mSave0X = ev.getX(0);
      mSave0Y = ev.getY(0);
      if ( np >= 2 ) {
        mSave1X = ev.getX(1);
        mSave1Y = ev.getY(1);
      } else {
        mSave1X = mSave0X;
        mSave1Y = mSave0Y;
      } 
    }
  }

  /** shift the drawing 
   * @param ev   touch event
   */
  private void shiftByEvent(MotionEventWrap ev)
  {
    float x0 = 0.0f;
    float y0 = 0.0f;
    float x1 = 0.0f;
    float y1 = 0.0f;
    int np = ev.getPointerCount();
    if ( np >= 1 ) {
      x0 = ev.getX(0);
      y0 = ev.getY(0);
      if ( np >= 2 ) {
        x1 = ev.getX(1);
        y1 = ev.getY(1);
      } else {
        x1 = x0;
        y1 = y0;
      } 
    }
    float x_shift = ( x0 - mSave0X + x1 - mSave1X ) / 2;
    float y_shift = ( y0 - mSave0Y + y1 - mSave1Y ) / 2;
    mSave0X = x0;
    mSave0Y = y0;
    mSave1X = x1;
    mSave1Y = y1;
  
    if ( Math.abs( x_shift ) < 60 && Math.abs( y_shift ) < 60 ) {
      mOffset.x += x_shift / mZoom;                // add shift to offset
      mOffset.y += y_shift / mZoom; 
      mOverviewSurface.setTransform( this, mOffset.x, mOffset.y, mZoom, mLandscape );
    }

  }

  /** shift the canvas
   * @param x_shift   X shift
   * @param y_shift   Y shift
   */
  private void moveCanvas( float x_shift, float y_shift )
  {
    if ( Math.abs( x_shift ) < 60 && Math.abs( y_shift ) < 60 ) {
      mOffset.x += x_shift / mZoom;                // add shift to offset
      mOffset.y += y_shift / mZoom; 
      mOverviewSurface.setTransform( this, mOffset.x, mOffset.y, mZoom, mLandscape );
      // mOverviewSurface.refresh();
    }
  }


  private float oldDist = 0;
  private float mStartX = 0;
  private float mStartY = 0;
  private float mBaseX = 0;
  private float mBaseY = 0;

  
  private float deltaX( float x1, float x0 ) 
  { return (  (x1 - x0) / DrawingUtil.SCALE_FIX ) / mUnitRuler; }

  private float deltaY( float y1, float y0 )
  { return ( -(y1 - y0) / DrawingUtil.SCALE_FIX ) / mUnitRuler; }

  private double angleBase( float bx, float by )
  {
    double ba = Math.atan2( bx, by ) * 180 / Math.PI;
    if ( ba < 0 ) ba += 360;

    if ( mType == PlotType.PLOT_PLAN ) {
      /* nothing */
    } else {
      if ( ba <= 180 ) {
        ba = 90 - ba;
      } else {
        ba = ba - 270;
      } 
    }
    ba *= TDSetting.mUnitAngle;
    return ba;
  }

  /** react to a user touch
   * @param view     touched view
   * @param rawEvent touch event
   * @return true if event has been handled
   *
   * @note Studio: onTouch() should call View#performClick when a click is detected
   */
  public boolean onTouch( View view, MotionEvent rawEvent )
  {
    float d0 = TDSetting.mCloseCutoff + TDSetting.mSelectness / mZoom;
    checkZoomBtnsCtrl();

    MotionEventWrap event = MotionEventWrap.wrap(rawEvent);
    // TDLog.Log( TDLog.LOG_INPUT, "DrawingWindow onTouch() " );
    // dumpEvent( event );

    float x_canvas = event.getX();
    float y_canvas = event.getY();
    // TDLog.v("touch canvas " + x_canvas + " " + y_canvas + " center_y " + (DrawingUtil.CENTER_Y*2-20) ); 

    // if ( mZoomBtnsCtrlOn && y_canvas > DrawingUtil.CENTER_Y*2-20 ) {
    //   mZoomBtnsCtrl.setVisible( true );
    //   // mZoomCtrl.show( );
    // }

    float x_scene = x_canvas/mZoom - mOffset.x;
    float y_scene = y_canvas/mZoom - mOffset.y;
    // TDLog.v( "touch scene " + x_scene + " " + y_scene );

    int action = event.getAction() & MotionEvent.ACTION_MASK;

    if (action == MotionEvent.ACTION_POINTER_DOWN) {
      if ( mIsContinue ) {
        setOnMeasure( MEASURE_OFF );
      } else {
        if ( mOnMeasure == MEASURE_ON ) mOnMeasure = MEASURE_START;
      }
      mTouchMode = MODE_ZOOM;
      oldDist = spacing( event );
      saveEventPoint( event );
    } else if ( action == MotionEvent.ACTION_POINTER_UP) {
      if ( mIsContinue ) {
        setOnMeasure( MEASURE_START );
      } else {
        if ( mOnMeasure == MEASURE_START ) mOnMeasure = MEASURE_ON;
      }
      mTouchMode = MODE_MOVE;
      /* nothing */

    // ---------------------------------------- DOWN

    } else if (action == MotionEvent.ACTION_DOWN) {
      // check side-drag and zoom controls
      if ( y_canvas > TopoDroidApp.mBorderBottom ) {
        if ( mZoomBtnsCtrlOn && x_canvas > TopoDroidApp.mBorderInnerLeft && x_canvas < TopoDroidApp.mBorderInnerRight ) {
          mTouchMode = MODE_ZOOM;
          mZoomBtnsCtrl.setVisible( true );
          // mZoomCtrl.show( );
          return true;
        } else if ( TDSetting.mSideDrag && ( x_canvas > TopoDroidApp.mBorderRight || x_canvas < TopoDroidApp.mBorderLeft ) ) {
          mTouchMode = MODE_ZOOM;
          return true;
        }
      } else if ( TDSetting.mSideDrag && (y_canvas < TopoDroidApp.mBorderTop) && ( x_canvas > TopoDroidApp.mBorderRight || x_canvas < TopoDroidApp.mBorderLeft ) ) {
        mTouchMode = MODE_ZOOM;
        return true;
      }

      mSaveX = x_canvas; // FIXME-000
      mSaveY = y_canvas;
      Matrix mm = new Matrix();
      mm.postScale( 1.0f/mZoom, 1.0f/mZoom );
      if ( mOnMeasure == MEASURE_START ) {
        mStartX = x_canvas/mZoom - mOffset.x;
        mStartY = y_canvas/mZoom - mOffset.y;
        mBaseX = mStartX;
        mBaseY = mStartY;
        mOnMeasure = MEASURE_ON;
        // add reference point
        // DrawingMeasure path1 = new DrawingMeasureStartPath( DrawingPath.DRAWING_PATH_NORTH, null, -1 );
        // path1.setPathPaint( BrushManager.highlightPaint );
        // path1.makePath( mCirclePath, mm, mStartX, mStartY );
        // TDLog.v( "first ref " + mStartX + " " + mStartY + " scale " + mOverviewSurface.getScale() + " zoom " + mZoom );
        DrawingMeasureStartPath path1 = new DrawingMeasureStartPath( mStartX, mStartY, 5 * TDSetting.mDotRadius * mOverviewSurface.getScale() * mZoom );
        mOverviewSurface.setFirstReference( path1 );
        if ( mIsContinue ) {
          mTotal   = 0;
          mDDtotal = 0;
          // DrawingPath path = new DrawingPath( DrawingPath.DRAWING_PATH_NORTH, null, -1 );
          // path.setPathPaint( BrushManager.fixedBluePaint );
          // path.makePath( null, mm, mStartX, mStartY ); // default path 
          // // path.mPath.moveTo( mStartX, mStartY ); FIXME-PATH
          DrawingMeasureEndPath path = new DrawingMeasureEndPath( mStartX, mStartY, 5 * TDSetting.mDotRadius * mOverviewSurface.getScale() * mZoom );
          mOverviewSurface.setSecondReference( path );
	  mMeasurePts.clear();
	  mMeasurePts.add( new Point2D( mStartX, mStartY ) );
        }
      } else if ( mOnMeasure == MEASURE_ON ) {
        // FIXME use scene values
        float x = x_canvas/mZoom - mOffset.x;
        float y = y_canvas/mZoom - mOffset.y;

        // segment displacement
        float dx = deltaX(x, mStartX);
        float dy = deltaY(y, mStartY);

        // total displacement, with respect to base
        float bx = deltaX(x, mBaseX);
        float by = deltaY(y, mBaseY);

        // angle with respect to base
        double ba = angleBase( bx, by );
        float dd = TDMath.sqrt( dx * dx + dy * dy );
        float bb = TDMath.sqrt( bx * bx + by * by );

        String format = ( mType == PlotType.PLOT_PLAN )?
          getResources().getString( R.string.format_measure_plan ) :
          getResources().getString( R.string.format_measure_profile );

        if ( mIsContinue ) {
          mDDtotal += dd;
          mTotal   ++;
          mOverviewSurface.addSecondReference( x, y );
          mStartX = x;
          mStartY = y;
	  mMeasurePts.add( new Point2D( mStartX, mStartY ) );
        } else {
          mDDtotal = dd;
          mTotal   = 1;
          // replace target point
          // DrawingPath path = new DrawingPath( DrawingPath.DRAWING_PATH_NORTH, null, -1 );
          // path.setPathPaint( BrushManager.fixedBluePaint );
          // path.makePath( mCrossPath, mm, x, y );
          // path.mPath.moveTo( mStartX, mStartY );
          // path.mPath.lineTo( x, y );
          DrawingMeasureEndPath path = new DrawingMeasureEndPath( mStartX, mStartY, x, y, 5 * TDSetting.mDotRadius * mOverviewSurface.getScale() * mZoom );
          mOverviewSurface.setSecondReference( path );
        }
        mActivity.setTitle( String.format( format, bb, mDDtotal, bx, by, ba ) );
      }
    // ---------------------------------------- MOVE

    } else if ( action == MotionEvent.ACTION_MOVE ) {
      if ( mTouchMode == MODE_MOVE) {
        float x_shift = x_canvas - mSaveX; // compute shift
        float y_shift = y_canvas - mSaveY;
        if ( mOnMeasure == MEASURE_OFF ) {
          if ( Math.abs( x_shift ) < 60 && Math.abs( y_shift ) < 60 ) {
            mOffset.x += x_shift / mZoom;                // add shift to offset
            mOffset.y += y_shift / mZoom; 
            mOverviewSurface.setTransform( this, mOffset.x, mOffset.y, mZoom, mLandscape );
          }
          mSaveX = x_canvas; 
          mSaveY = y_canvas;
        }
      } else { // mTouchMode == MODE_ZOOM
        float newDist = spacing( event );
        if ( newDist > 16.0f && oldDist > 16.0f ) {
          float factor = newDist/oldDist;
          if ( factor > 0.05f && factor < 4.0f ) {
            changeZoom( factor );
            oldDist = newDist;
          }
        }
        shiftByEvent( event );
      }

    // ---------------------------------------- UP

    } else if (action == MotionEvent.ACTION_UP) {
      if ( onMenu ) {
        closeMenu();
        return true;
      }

      if ( mTouchMode == MODE_ZOOM ) {
        mTouchMode = MODE_MOVE;
      // } else {
        // NOTHING
        // if ( mOnMeasure == MEASURE_OFF ) {
        //   // float x_shift = x_canvas - mSaveX; // compute shift
        //   // float y_shift = y_canvas - mSaveY;
        // } else {
        // }
      }
    }
    return true;
  }


  private Button makeButton( String text )
  {
    Button myTextView = new Button( mActivity );
    myTextView.setHeight( 42 );

    myTextView.setText( text );
    myTextView.setTextColor( TDColor.WHITE );
    myTextView.setTextSize( TypedValue.COMPLEX_UNIT_DIP, 16 );
    myTextView.setBackgroundColor( TDColor.VERYDARK_GRAY );
    myTextView.setSingleLine( true );
    myTextView.setGravity( 0x03 ); // left
    myTextView.setPadding( 4, 4, 4, 4 );
    // TDLog.v("makeButton " + text );
    return myTextView;
  }

    /* FIXME_OVER
    private void switchPlotType()
    {
      if ( mType == PlotType.PLOT_PLAN ) {
        // saveReference( mPlot1, mPid1 );
        // mPid  = mPid2;
        mType = PlotInfo.mPlot2.type; 
        TDandroid.setButtonBackground( mButton1[ BTN_PLOT ], mBMextend );
        mOverviewSurface.setManager( DrawingSurface.DRAWING_PLAN, (int)mType ); 
        resetReference( mPlot2 );
      } else if ( mType == PlotType.PLOT_EXTENDED ) { // PROJECTED not supported on overview
        // saveReference( mPlot2, mPid2 );
        // mPid  = mPid1;
        // mName = mName1;
        mType = mPlot1.type;
        TDandroid.setButtonBackground( mButton1[ BTN_PLOT ], mBMplan );
        mOverviewSurface.setManager( DrawingSurface.DRAWING_PROFILE, (int)mType );
        resetReference( mPlot1 );
      }
    }
    */

    /** set the OnMeasure flag
     * @param measure     flag, either OFF or START
     */
    private void setOnMeasure( int measure )
    {
      mOnMeasure = measure;
      if ( mOnMeasure == MEASURE_OFF ) {
        TDandroid.setButtonBackground( mButton1[IC_SELECT], mBMselect );
        mOverviewSurface.setFirstReference( null );
        mOverviewSurface.setSecondReference( null );
      } else if ( mOnMeasure == MEASURE_START ) {
        TDandroid.setButtonBackground( mButton1[IC_SELECT], mBMselectOn );
        mDDtotal = 0;
        mTotal = 0;
        mOverviewSurface.setSecondReference( null );
      }
    }
  
    public void onClick(View view)
    {
      if ( onMenu ) {
        closeMenu();
        return;
      }

      Button b = (Button)view;
      if ( b == mMenuImage ) {
        if ( mMenu.getVisibility() == View.VISIBLE ) {
          mMenu.setVisibility( View.GONE );
          onMenu = false;
        } else {
          mMenu.setVisibility( View.VISIBLE );
          onMenu = true;
        }
        return;
      }
      if ( b == mButton1[0] ) { // measure
        if ( mOnMeasure == MEASURE_OFF ) {
          setOnMeasure( MEASURE_START );
        } else {
          setOnMeasure( MEASURE_OFF );
        }
      } else if ( b == mButton1[1] ) { // references
        new OverviewModeDialog( mActivity, this, mOverviewSurface ).show();
      } else if ( b == mButton1[2] ) { // continue
        toggleIsContinue( );
      } else if ( b == mButton1[3] ) { // undo
	undoMeasurePoint();

      // FIXME_OVER } else if ( b == mButton1[2] ) { // toggle plan/extended
      // FIXME_OVER   switchPlotType();
      }
    }

  private void undoMeasurePoint()
  {
    if ( ! mIsContinue ) return;
    int sz = mMeasurePts.size() - 1;
    if ( sz <= 0 ) return;
    Point2D pt1 = mMeasurePts.get(sz);
    Point2D pt0 = mMeasurePts.get(sz-1);
    float dx = deltaX( pt1.x, pt0.x );
    float dy = deltaX( pt1.y, pt0.y );
    float bx = deltaX( pt1.x, mBaseX );
    float by = deltaX( pt1.y, mBaseY );
    double ba = angleBase( bx, by );
    float dd = TDMath.sqrt( dx * dx + dy * dy );
    float bb = TDMath.sqrt( bx * bx + by * by );
    mDDtotal -= dd;
    mTotal   --;
    mStartX = pt0.x;
    mStartY = pt0.y;
    mMeasurePts.remove( sz );

    // DrawingPath path = new DrawingPath( DrawingPath.DRAWING_PATH_NORTH, null, -1 );
    // path.setPathPaint( BrushManager.fixedBluePaint );
    // path.makePath( null, new Matrix(), mBaseX, mBaseY ); // default-path
    // // path.mPath.moveTo( mBaseX, mBaseY ); FIXME-PATH
    Matrix mm = new Matrix();
    mm.postScale( 1.0f/mZoom, 1.0f/mZoom );
    DrawingMeasureEndPath path = new DrawingMeasureEndPath( mStartX, mStartY, 5 * TDSetting.mDotRadius * mOverviewSurface.getScale() * mZoom );
    mOverviewSurface.setSecondReference( path );
    for ( int k=1; k<sz; ++k ) {
      Point2D pt = mMeasurePts.get(k);
      mOverviewSurface.addSecondReference( pt.x, pt.y );
    }

    String format = ( mType == PlotType.PLOT_PLAN )?
      getResources().getString( R.string.format_measure_plan ) :
      getResources().getString( R.string.format_measure_profile );

    mActivity.setTitle( String.format( format, bb, mDDtotal, bx, by, ba ) );
  }

  private void toggleIsContinue( )
  {
    mIsContinue = ! mIsContinue;
    TDandroid.setButtonBackground( mButton1[IC_CONTINUE], (mIsContinue? mBMcontinueOn : mBMcontinueNo) );
  }


  @Override
  public boolean onKeyDown( int code, KeyEvent event )
  {
    switch ( code ) {
      case KeyEvent.KEYCODE_BACK: // HARDWARE BACK (4)
        super.onBackPressed();
        return true;
      case KeyEvent.KEYCODE_MENU:   // HARDWARE MENU (82)
        UserManualActivity.showHelpPage( mActivity, getResources().getString( HELP_PAGE ));
        return true;
      case KeyEvent.KEYCODE_VOLUME_UP:   // (24)
        takeScreenshot( mOverviewSurface );
        return true;
      case KeyEvent.KEYCODE_VOLUME_DOWN: // (25)
      default:
        // TDLog.Error( "key down: code " + code );
    }
    return false;
  }


  private void setMenuAdapter( Resources res )
  {
    ArrayAdapter< String > menu_adapter = new ArrayAdapter<>(mActivity, R.layout.menu );

    menu_adapter.add( res.getString( menus[0] ) );
    if ( TDLevel.overExpert ) menu_adapter.add( res.getString( menus[1] ) );
    menu_adapter.add( res.getString( menus[2] ) );
    menu_adapter.add( res.getString( menus[3] ) );
    mMenu.setAdapter( menu_adapter );
    mMenu.invalidate();
  }

  private void closeMenu()
  {
    mMenu.setVisibility( View.GONE );
    onMenu = false;
  }

  private void handleMenu( int pos )
  {
    closeMenu();
    int p = 0;
    if ( p++ == pos ) { // CLOSE
      super.onBackPressed();
    } else if ( TDLevel.overExpert && p++ == pos ) { // EXPORT THERION
      String fullname = TDInstance.survey + ( (mType == PlotType.PLOT_PLAN )? "-p" : "-s" );
      new ExportDialogPlot( mActivity, this, TDConst.mOverviewExportTypes, R.string.title_plot_save, fullname, null ).show();
    } else if ( p++ == pos ) { // OPTIONS
      Intent intent = new Intent( mActivity, com.topodroid.prefs.TDPrefActivity.class );
      intent.putExtra( TDPrefCat.PREF_CATEGORY, TDPrefCat.PREF_CATEGORY_PLOT );
      mActivity.startActivity( intent );
    } else if ( p++ == pos ) { // HELP
      new HelpDialog(mActivity, izons, menus, help_icons, help_menus, mNrButton1, help_menus.length, getResources().getString( HELP_PAGE ) ).show();
    }
  }


  @Override 
  public void onItemClick(AdapterView<?> parent, View view, int pos, long id)
  {
    if ( mMenu == (ListView)parent ) {
      handleMenu( pos );
    }
  }

  public void checkZoomBtnsCtrl()
  {
    if ( TDSetting.mZoomCtrl == 2 && ! mZoomBtnsCtrl.isVisible() ) {
      mZoomBtnsCtrl.setVisible( true );
    }
  }

  /** react to a change in the configuration
   * @param new_cfg   new configuration
   */
  @Override
  public void onConfigurationChanged( Configuration new_cfg )
  {
    super.onConfigurationChanged( new_cfg );
    TDLocale.resetTheLocale();
    mOverviewSurface.setTransform( this, mOffset.x, mOffset.y, mZoom, mLandscape );
  }

}
