/* @file ProjectionDialog.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid main drawing activity
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDMath;
import com.topodroid.utils.TDStatus;
import com.topodroid.utils.TDUtil;
import com.topodroid.num.TDNum;
import com.topodroid.num.NumStation;
import com.topodroid.num.NumShot;
// import com.topodroid.num.NumSplay;
import com.topodroid.ui.MyDialog;
import com.topodroid.ui.MotionEventWrap;
import com.topodroid.prefs.TDSetting;

import android.content.Context;

import android.graphics.PointF;
// import android.graphics.Path;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ZoomButtonsController;
import android.widget.ZoomButtonsController.OnZoomListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import android.text.TextWatcher;
import android.text.Editable;

import java.util.List;
import java.util.Locale;

// import java.util.Deque; // REQUIRES API-9

/**
 */
class ProjectionDialog extends MyDialog
                       implements View.OnTouchListener
                       , View.OnClickListener
                       , View.OnLongClickListener
                       , OnZoomListener
                       , IZoomer
{
  private View mZoomView;

  private final ShotWindow mParent;

  private ProjectionSurface mProjectionSurface;
  private SeekBar mSeekBar;
  private SeekBar mProjBar;
  private Button  mBtnOk;
  private Button  mBtnPlus;
  private Button  mBtnMinus;
  private Button  mBtnReset;
  private EditText mETazimuth;
  // private EditText mETproject;
  private TDNum mNum = null;

  private ZoomButtonsController mZoomBtnsCtrl = null;
  private boolean mZoomBtnsCtrlOn = false;
  private float oldDist;  // zoom pointer-spacing

  private int mTouchMode = DrawingWindow.MODE_MOVE;
  private float mSaveX;
  private float mSaveY;
  private float mSave0X;
  private float mSave0Y;
  private float mSave1X;
  private float mSave1Y;
  private PointF mOffset = new PointF( 0f, 0f );

  private PointF mDisplayCenter;
  private float mZoom  = 1.0f;

  private long   mSid;  // survey id
  private String mName;
  private String mFrom;
  private int  mAzimuth = 0; // azimuth of the projection plane
  private int  mOblique = 0; // oblique projection direction

  private List< DBlock > mList = null;

  private boolean mETazimuthChanged = false;
  private boolean mWithOblique;

  /** cstr
   * @param context context
   * @param parent  parent activity (ShotWindow)
   * @param sid     survey ID
   * @param name    plot name
   * @param from    plot origin station
   */
  ProjectionDialog( Context context, ShotWindow parent, long sid, String name, String from )
  {
    super( context, null, R.string.ProjectionDialog ); // FIXME // null app
    mParent = parent;
    // mDrawingUtil = new DrawingUtilPortrait();
    mSid    = sid;
    mName   = name;
    mFrom   = from;
    mAzimuth = 0;
    mOblique = 0;
    // mApp     = mParent.getApp();
    mWithOblique = TDLevel.overExpert && TDSetting.mObliqueMax > 10;
  }

  /** update the text in the edit field with the current value of the azimuth
   */
  private void updateEditText()
  { 
    mETazimuth.setText( String.format(Locale.US, "%d", mAzimuth ) );
    // mETproject.setText( String.format(Locale.US, "%d", mOblique ) );
    // TDLog.v( "set azimuth " + mAzimuth );
  }

  // -------------------------------------------------------------------
  // ZOOM CONTROLS

  /** @return the zoom
   */
  public float zoom() { return mZoom; }

  /** react to a change of visibility
   * @param visible ...
   */
  @Override
  public void onVisibilityChanged(boolean visible)
  {
    if ( mZoomBtnsCtrlOn && mZoomBtnsCtrl != null ) {
      mZoomBtnsCtrl.setVisible( visible || ( TDSetting.mZoomCtrl > 1 ) );
    }
  }

  /** react to a zoom tap
   * @param zoomin   whether to zoom IN or OUT
   */
  @Override
  public void onZoom( boolean zoomin )
  {
    if ( zoomin ) changeZoom( DrawingWindow.ZOOM_INC );
    else changeZoom( DrawingWindow.ZOOM_DEC );
  }

  /** change the value of the zoom
   * @param f    multiplicative change factor
   */
  private void changeZoom( float f ) 
  {
    float zoom = mZoom;
    mZoom     *= f;
    // TDLog.v( "zoom " + mZoom );
    mOffset.x -= mDisplayCenter.x * (1/zoom - 1/mZoom);
    mOffset.y -= mDisplayCenter.y * (1/zoom - 1/mZoom);
    // TDLog.v( "change zoom " + mOffset.x + " " + mOffset.y + " " + mZoom );
    mProjectionSurface.setTransform( mOffset.x, mOffset.y, mZoom );
    // mProjectionSurface.refresh();
    // mZoomCtrl.hide();
    // if ( mZoomBtnsCtrlOn ) mZoomBtnsCtrl.setVisible( false );
  }

  /** zoom IN (increase resolution)
   */
  void zoomIn()  { changeZoom( DrawingWindow.ZOOM_INC ); }

  /** zoom OUT (decrease resolution)
   */
  void zoomOut() { changeZoom( DrawingWindow.ZOOM_DEC ); }
  // public void zoomOne() { resetZoom( ); }

  // public void zoomView( )
  // {
  //   // TDLog.Log( TDLog.LOG_PLOT, "zoomView ");
  //   DrawingZoomDialog zoom = new DrawingZoomDialog( mProjectionSurface.getContext(), this );
  //   zoom.show();
  // }

  // -----------------------------------------------------------------

  /** add a splay line
   * @param blk   splay data
   * @param x1    X coord of first point
   * @param y1    Y coord of first point
   * @param x2    X coord of second point
   * @param y2    Y coord of second point
   */
  private void addFixedSplayLine( DBlock blk, float x1, float y1, float x2, float y2 )
  {
    DrawingSplayPath dpath = null;
    dpath = new DrawingSplayPath( blk, 0 );
    dpath.setPathPaint( BrushManager.fixedShotPaint );
    dpath.makePath( x1, y1, x2, y2 );
    mProjectionSurface.addFixedSplayPath( dpath );
  }
    
  /** add a leg line
   * @param blk   leg data
   * @param x1    X coord of first point [scene ref.]
   * @param y1    Y coord of first point
   * @param x2    X coord of second point [scene ref.]
   * @param y2    Y coord of second point
   */
  private void addFixedLegLine( DBlock blk, float x1, float y1, float x2, float y2 )
  {
    DrawingPath dpath = null;
    dpath = new DrawingPath( DrawingPath.DRAWING_PATH_FIXED, blk, 0 );
    dpath.setPathPaint( BrushManager.labelPaint );
    // mDrawingUtil.makeDrawingPath( dpath, x1, y1, x2, y2, mOffset.x, mOffset.y );
    dpath.makePath( x1, y1, x2, y2 );
    mProjectionSurface.addFixedLegPath( dpath );
  }

  // --------------------------------------------------------------------------------------
  //        ^ N
  //  `-.   |
  //     `-.|
  // -------+----------> E
  //       /|`-.
  //      / |   `-. x
  //   n /  v S    
  //      a
  //
  // n = ( -sin(a), cos(a) ) [E-S frame] normal to the projection plane
  // x = (  cos(a), sin(a) )
  // p = ( -sin(b), cos(b) ) projection direction
  //
  // P=(E,S) projects to Q=(E',S') s.t.
  //   Q = P + t * p
  // ie,
  //   E' = E - t sin(b)
  //   S' = S + t cos(b)
  // and Q * n = 0, ie, - E' sin(a) + S' cos(a) = 0
  //    -E sin(a) + S cos(a) + t ( sin(b) sin(a) + cos(b) cos(a) ) = 0
  //    t = ( E sin(a) - S cos(a) ) / ( sb sa + ccb ca )
  // Therefore
  //   x = E' cos(a) + S' sin(a) 
  //     = E ca + S sa + (E sa - S ca) * ( - sb ca + cb sa )/( sb sa + cb ca )
  // let g = ( - sb ca + cb sa )/( sb sa + cb ca )
  //   x = E ( ca + g sa ) + S ( oa - g ca )

  /** compute the midline projection 
   */
  private void computeReferences( )
  {
    mProjectionSurface.clearReferences( );
    // TDLog.v( "refs " + mOffset.x + " " + mOffset.y + " " + mZoom + " " + mAzimuth );
    // mProjectionSurface.newReferences( ... ); DoubleBuffer NOT NEEDED

    float sina = TDMath.sind( mAzimuth );
    float cosa = TDMath.cosd( mAzimuth ); // N~ = ( cosp, sinp )
    float sinb = TDMath.sind( mAzimuth + mOblique ); // P  = ( sinb, cosb )
    float cosb = TDMath.cosd( mAzimuth + mOblique ); 
    float gamma = ( - sinb * cosa + cosb * sina ) / ( sinb * sina + cosb * cosa );

    // TDLog.v("Gamma " + gamma );

    float cx = cosa + gamma * sina;
    float sx = sina - gamma * cosa;

    float e0 = 0;
    float s0 = 0;
    float v0 = 0;
    NumStation origin = mNum.getOrigin();
    if ( origin != null ) {
      e0 = (float)origin.e;
      s0 = (float)origin.s;
      v0 = (float)origin.v;
    }

    List< NumStation > stations = mNum.getStations();
    List< NumShot > shots       = mNum.getShots();
    // List< NumSplay > splays     = mNum.getSplays();


    float h1, h2, v1, v2;
    // float dx = 0; // mOffset.x;
    // float dy = 0; // mOffset.y;
    for ( NumShot sh : shots ) {
      NumStation st1 = sh.from;
      NumStation st2 = sh.to;
      if ( st1.show() && st2.show() ) {
	double x1 = (st1.e - e0) * cx + (st1.s - s0) * sx; // - dx;
	double x2 = (st2.e - e0) * cx + (st2.s - s0) * sx; // - dx;
	double y1 = st1.v - v0; // - dy;
	double y2 = st2.v - v0; // - dy;
        h1 = DrawingUtil.toSceneX( x1, y1 ); // CENTER_X + x1 * SCALE_FIX = 100 + x1 * 20
        h2 = DrawingUtil.toSceneX( x2, y2 ); // CENTER_Y + Y1 * SCALE_FIX = 120 + y1 * 20
        v1 = DrawingUtil.toSceneY( x1, y1 );
        v2 = DrawingUtil.toSceneY( x2, y2 );
        addFixedLegLine( sh.getFirstBlock(), h1, v1, h2, v2 );
      }
    } 
    // for ( NumSplay sp : splays ) {
    //   NumStation st = sp.from;
    //   if ( st.show() ) {
    //     double x1 = (st.e - e0) * cx + (st.s - s0) * sx; // - dx;
    //     double x2 = (sp.e - e0) * cx + (sp.s - s0) * sx; // - dx;
    //     double y1 = st.v - v0; // - dy;
    //     double y2 = sp.v - v0; // - dy;
    //     h1 = DrawingUtil.toSceneX( x1, y1 );
    //     h2 = DrawingUtil.toSceneX( x2, y2 );
    //     v1 = DrawingUtil.toSceneY( x1, y1 );
    //     v2 = DrawingUtil.toSceneY( x2, y2 );
    //     addFixedSplayLine( sp.getBlock(), h1, v1, h2, v2 );
    //   }
    // }
    for ( NumStation st : stations ) {
      if ( st.show() ) {
	double x1 = (st.e -e0) * cx + (st.s - s0) * sx; // - dx;
	double y1 = st.v - v0; // - dy;
        h1 = DrawingUtil.toSceneX( x1, y1 );
        v1 = DrawingUtil.toSceneY( x1, y1 );
        mProjectionSurface.addDrawingStationName( st, h1, v1 );
      }
    }

    // mProjectionSurface.commitReferences();... ); // DoubleBuffer NOT NEEDED
    setTheTitle();
  }

  private void setTheTitle()
  {
    if ( mWithOblique ) {
      setTitle( String.format( mContext.getResources().getString(R.string.title_projection_oblique), mAzimuth, mOblique ) );
    } else {
      setTitle( String.format( mContext.getResources().getString(R.string.title_projection), mAzimuth ) );
    }
  }

  // --------------------------------------------------------------

  /** switch the visibility of zoom-controls
   * @param ctrl   control value: 0 not used, 1 not visible, 2 visible
   * @note this method is a callback to let other objects tell the activity to use zooms or not
   */
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

  @Override
  public void onCreate(Bundle savedInstanceState) 
  {
    super.onCreate(savedInstanceState);
    mZoomBtnsCtrlOn = (TDSetting.mZoomCtrl > 1);  // do before setting content

    // mIsNotMultitouch = ! TDandroid.checkMultitouch( this );

    setContentView( R.layout.projection_dialog );
    mSeekBar   = (SeekBar) findViewById(R.id.seekbar );
    mProjBar   = (SeekBar) findViewById(R.id.projbar );
    mETazimuth = (EditText) findViewById( R.id.textform );
    // mETproject = (EditText) findViewById( R.id.textform );
    mBtnOk     = (Button) findViewById( R.id.btn_ok );
    mBtnPlus   = (Button) findViewById( R.id.btn_plus );
    mBtnMinus  = (Button) findViewById( R.id.btn_minus );
    mBtnReset  = (Button) findViewById( R.id.btn_reset );
    mBtnOk.setOnClickListener( this );
    mBtnPlus.setOnClickListener( this );
    mBtnMinus.setOnClickListener( this );
    mBtnPlus.setOnLongClickListener( this );
    mBtnMinus.setOnLongClickListener( this );

    mProjectionSurface = (ProjectionSurface) findViewById(R.id.drawingSurface);
    // mProjectionSurface.setZoomer( this );
    mProjectionSurface.setProjectionDialog( this );
    mProjectionSurface.setOnTouchListener(this);
    // mProjectionSurface.setOnLongClickListener(this);
    // mProjectionSurface.setBuiltInZoomControls(true);

    mZoomView = (View) findViewById(R.id.zoomView );
    mZoomBtnsCtrl = new ZoomButtonsController( mZoomView );
    // FIXME ZOOM_CTRL mZoomCtrl = (ZoomControls) mZoomBtnsCtrl.getZoomControls();
    // ViewGroup vg = mZoomBtnsCtrl.getContainer();
    // switchZoomCtrl( TDSetting.mZoomCtrl );

    updateEditText();

    mSeekBar.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
      // progress ranges in [0, 400] see layoit/projection_dialog.xml
      public void onProgressChanged( SeekBar seekbar, int progress, boolean fromUser) {
        mAzimuth = (160 + progress)%360; // when progress = 200, azimuth = 0
        if ( progress < 10 ) {
          seekbar.setProgress( progress + 360 );
          setTheTitle();
        } else if ( progress > 390 ) {
          seekbar.setProgress( progress - 360 );
          setTheTitle();
        } else {
          computeReferences(); // this calls setTheTitle
          if ( ! mETazimuthChanged ) updateEditText();
	  mETazimuthChanged = false;
        }
        // TDLog.v( "set azimuth: oblique " + mOblique + " azimuth " + mAzimuth );
      }
      public void onStartTrackingTouch(SeekBar seekbar) { }
      public void onStopTrackingTouch(SeekBar seekbar) { }
    } );

    if ( mWithOblique ) {
      mBtnReset.setOnClickListener( this );
      mProjBar.setOnSeekBarChangeListener( new OnSeekBarChangeListener() {
        // progress ranges in [0, 400] see layoit/projection_dialog.xml
        public void onProgressChanged( SeekBar seekbar, int progress, boolean fromUser) {
          int max = TDSetting.mObliqueMax; 
          mOblique = (int)( progress/200.0f*max - max); // oblique angle between -max and +max (thx HB)
          computeReferences(); // this calls setTheTitle
          // TDLog.v( "set project: oblique " + mOblique + " azimuth " + mAzimuth );
        }
        public void onStartTrackingTouch(SeekBar seekbar) { }
        public void onStopTrackingTouch(SeekBar seekbar) { }
      } );
    } else {
      LinearLayout layout = (LinearLayout) findViewById( R.id.layoutproj );
      layout.setVisibility( View.GONE );
    }

    // mETazimuth.setOnFocusChangeListener( new View.OnFocusChangeListener() {
    //   public void onFocusChange( View v, boolean b ) {
    //     if ( ! b ) { // focus lost
    //       try {
    //         setAzimuth( Integer.parseInt( mETazimuth.getText().toString() ) );
    //       } catch ( NumberFormatException e ) { }
    //     }
    //   }
    // } );

    // TODO mETproject
    mETazimuth.addTextChangedListener( new TextWatcher() {
      @Override
      public void afterTextChanged( Editable e ) { }

      @Override
      public void beforeTextChanged( CharSequence cs, int start, int cnt, int after ) { }

      @Override
      public void onTextChanged( CharSequence cs, int start, int before, int cnt ) 
      {
        try {
          int azimuth = Integer.parseInt( mETazimuth.getText().toString() );
          // if ( azimuth < 0 || azimuth > 360 ) azimuth = 0;
          setAzimuth( azimuth, false );
          // updateSeekBar();
          // updateView();
        } catch ( NumberFormatException e ) {
          TDLog.Error("Non-integer azimuth");
        }
      }
    } );

    // mETazimuth.setOnKeyListener( new View.OnKeyListener() {
    //   public boolean onKey( View v, int code, KeyEvent event )
    //   {
    //     // TDLog.v( "key code " + code );
    //     if ( code == 66 /* KeyEvent.KEYCODE_ENTER */ ) {
    //       try {
    //         setAzimuth( Integer.parseInt( mETazimuth.getText().toString() ) );
    //       } catch ( NumberFormatException e ) { }
    //       return true;
    //     }
    //     return false; 
    //   }
    // } );

    // Display display = getWindowManager().getDefaultDisplay();
    // DisplayMetrics dm = new DisplayMetrics();
    // display.getMetrics( dm );
    // int width = dm widthPixels;
    // int width = mContext.getResources().getDisplayMetrics().widthPixels; // same as TopoDroidApp.mDisplayWidth

    doStart();
  }

  /** set the value of the azimuth
   * @param a         new value of the azimuth
   * @param edit_text whether the set came from the edit text field
   */
  private void setAzimuth( int a, boolean edit_text )
  {
    mAzimuth = a;
    if ( mAzimuth < 0 || mAzimuth >= 360 ) { mAzimuth = 0; edit_text = true; }
    mETazimuthChanged = ! edit_text;
    computeReferences(); // this calls setTheTitle
    mSeekBar.setProgress( ( mAzimuth < 180 )? 200 + mAzimuth : mAzimuth - 160 );
    if ( edit_text ) updateEditText();
  }

  /** @note called by ProjectionSurface
   * @param w   surface width
   * @param h   surface height
   */
  synchronized void setSize( int w, int h )
  {
    // TDLog.v("PROJ set size " + w + " " + h );
    if ( w == 0 || h == 0 ) return;
    float e1 = DrawingUtil.toSceneX( mNum.surveyEmin(), mNum.surveySmin() );  // CENTER_X + Emin * SCALE referred to upper-left corner
    float s1 = DrawingUtil.toSceneY( mNum.surveyEmin(), mNum.surveySmin() ); 
    float e2 = DrawingUtil.toSceneX( mNum.surveyEmax(), mNum.surveySmax() ); 
    float s2 = DrawingUtil.toSceneY( mNum.surveyEmax(), mNum.surveySmax() ); 

    float de = ( e2 - e1 ) / 2; // ( Emax - Emin)/2 * SCALE
    float ds = ( s2 - s1 ) / 2;
    float dx = (float)Math.sqrt( de*de + ds*ds );
    float v1 = DrawingUtil.toSceneY( mNum.surveyHmin(), mNum.surveyVmin() );
    float v2 = DrawingUtil.toSceneY( mNum.surveyHmax(), mNum.surveyVmax() );
    float dy = ( v2 - v1 ) / 2;
    // mZoom = TopoDroidApp.mScaleFactor;    // canvas zoom = 19.153126, DisplayWidth = 1080, DisplayHeight = 2043
    float zoom = ( (dx > dy)? w / dx :  h / dy ) * TopoDroidApp.mScaleFactor / 100;

    int h0 = findViewById( R.id.layout1 ).getHeight();
    
    // mDisplayCenter = new PointF( w / 2 - DrawingUtil.CENTER_X, h / 2 - DrawingUtil.CENTER_Y );


    mDisplayCenter = new PointF( w / 2.0f, (h + h0)/ 2.0f );

    float centerx = ( e1 + e2 )/ 2; // FIXME this is DrawingUtil.CENTER_X = 100
    float centery = ( v1 + v2 )/ 2; // FIXME this is DrawingUtil.CENTER_Y = 120

    NumStation origin = mNum.getOrigin();
    if ( origin != null ) {
      centerx -= (float)origin.e;
      centery -= (float)origin.s;
    }
    // TDLog.v("Projection origin " +  (float)origin.e + " " + (float)origin.s + " " +  (float)origin.v );
    // TDLog.v("Projection center " + centerx + " " + centery );
    // TDLog.v("Projection display center " + mDisplayCenter.x + " " + mDisplayCenter.y );

    // X --> X' = ( sceneX + offsetX ) * zoom 
    //          = ( (CENTER_X + X*SCALE) + offsetX ) * zoom
    //          = DisplayCenter.X + X * SCALE * zoom - (CENTER_X + cX * SCALE) * zoom;
    mOffset.x = mDisplayCenter.x / zoom - centerx;
    mOffset.y = mDisplayCenter.y / zoom - centery;
    mZoom     = zoom;
    mProjectionSurface.setTransform( mOffset.x, mOffset.y, mZoom );
    if ( mNum != null ) {
      computeReferences();
    }
  }

  // ----------------------------------------------------------------------------

  /** lifecycle: start
   * set the progress-bar and compute the initial offset and zoom
   */
  synchronized private void doStart()
  {
    mList = TopoDroidApp.mData.selectAllShots( mSid, TDStatus.NORMAL );
    if ( TDUtil.isEmpty(mList) ) {
      dismiss();
      TDToast.makeBad( R.string.few_data );
    } else {
      // float decl = mApp.mData.getSurveyDeclination( mSid );
      mNum = new TDNum( mList, mFrom, "", "", 0.0f, null, true ); // null formatClosure, true: midline_only
      mNum.recenter();
      mSeekBar.setProgress( 200 );
      mProjBar.setProgress( 200 );
      if ( mProjectionSurface != null ) {
        setSize( mProjectionSurface.width(), mProjectionSurface.height() );
      }
    }
  }

  /** compute the spacing of a two-finger touch
   * @param ev   touch event 
   * @return spacing between the two finger touch points
   */
  private float spacing( MotionEventWrap ev )
  {
     int np = ev.getPointerCount();
     if ( np < 2 ) return 0.0f;
     float x = ev.getX(1) - ev.getX(0);
     float y = ev.getY(1) - ev.getY(0);
     return (float)Math.sqrt(x*x + y*y);
  }

  /** save the screen coordinates of a tough event
   * @param ev   touch event 
   */
  private void saveEventPoint( MotionEventWrap ev )
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
  private void shiftByEvent( MotionEventWrap ev )
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

     moveCanvas( x_shift, y_shift );
  }

  /** move the canvas
   * @param x_shift   shift in the horizontal direction
   * @param y_shift   shift in the vertical direction
   */
  private void moveCanvas( float x_shift, float y_shift )
  {
     if ( Math.abs( x_shift ) < 60 && Math.abs( y_shift ) < 60 ) {
       mOffset.x += x_shift / mZoom;                // add shift to offset
       mOffset.y += y_shift / mZoom; 
       // TDLog.v( "move canvas " + mOffset.x + " " + mOffset.y + " " + mZoom );
       mProjectionSurface.setTransform( mOffset.x, mOffset.y, mZoom );
       // mProjectionSurface.refresh();
     }
  }

  /** react to a zoom touch: if the controls should be visible and are not visible
   * make them visible
   */
  public void checkZoomBtnsCtrl()
  {
     // if ( mZoomBtnsCtrl == null ) return; // not necessary
     if ( TDSetting.mZoomCtrl == 2 && ! mZoomBtnsCtrl.isVisible() ) {
       mZoomBtnsCtrl.setVisible( true );
     }
  }

  // private boolean pointerDown = false;

  /** react to a user touch
   * @param view     touched view
   * @param rawEvent raw touch event
   */
  public boolean onTouch( View view, MotionEvent rawEvent )
  {
     checkZoomBtnsCtrl();

     MotionEventWrap event = MotionEventWrap.wrap(rawEvent);
     // TDLog.Log( TDLog.LOG_INPUT, "DrawingWindow onTouch() " );
     // dumpEvent( event );

     int act = event.getAction();
     int action = act & MotionEvent.ACTION_MASK;
     int id = 0;

     if (action == MotionEvent.ACTION_POINTER_DOWN) {
       mTouchMode = DrawingWindow.MODE_ZOOM;
       oldDist = spacing( event );
       saveEventPoint( event );
       // pointerDown = true;
       return true;
     } else if ( action == MotionEvent.ACTION_POINTER_UP) {
       int np = event.getPointerCount();
       if ( np > 2 ) return true;
       mTouchMode = DrawingWindow.MODE_MOVE;
       id = 1 - ((act & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
       // int idx = rawEvent.findPointerIndex( id );
       /* fall through */
     }
     float x_canvas = event.getX(id);
     float y_canvas = event.getY(id);
     // TDLog.v( "touch " + x_canvas + " " + y_canvas + " (" + mOffset.x + " " + mOffset.y + " " + mZoom + ")" );

     // ---------------------------------------- DOWN
     if (action == MotionEvent.ACTION_DOWN) {
       if ( y_canvas > TopoDroidApp.mBorderBottom ) {
         if ( mZoomBtnsCtrlOn && x_canvas > TopoDroidApp.mBorderInnerLeft && x_canvas < TopoDroidApp.mBorderInnerRight ) {
           mTouchMode = DrawingWindow.MODE_ZOOM;
           mZoomBtnsCtrl.setVisible( true );
           // mZoomCtrl.show( );
         } else if ( TDSetting.mSideDrag ) {
           mTouchMode = DrawingWindow.MODE_ZOOM;
         }
       } else if ( TDSetting.mSideDrag && ( x_canvas > TopoDroidApp.mBorderRight || x_canvas < TopoDroidApp.mBorderLeft ) ) {
         mTouchMode = DrawingWindow.MODE_ZOOM;
       }

       // setTheTitle( );
       mSaveX = x_canvas; // FIXME-000
       mSaveY = y_canvas;
       return false;

     // ---------------------------------------- MOVE
     } else if ( action == MotionEvent.ACTION_MOVE ) {
       if ( mTouchMode == DrawingWindow.MODE_MOVE) {
         float x_shift = x_canvas - mSaveX; // compute shift
         float y_shift = y_canvas - mSaveY;
         moveCanvas( x_shift, y_shift );
         mSaveX = x_canvas; 
         mSaveY = y_canvas;
       } else { // mTouchMode == DrawingWindow.MODE_ZOOM
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
       if ( mTouchMode == DrawingWindow.MODE_ZOOM ) {
         mTouchMode = DrawingWindow.MODE_MOVE;
       } else {
         float x_shift = x_canvas - mSaveX; // compute shift
         float y_shift = y_canvas - mSaveY;
       }
     }
     return true;
  }

  /** react to user long-tap 
   * @param v tapped view
   */
  @Override
  public boolean onLongClick( View v ) 
  {
    if ( v.getId() == R.id.btn_plus ) {
      setAzimuth( mAzimuth + 10, true ); // calls computeReferences which calls setTheTitle
    } else if ( v.getId() == R.id.btn_minus ) {
      setAzimuth( mAzimuth - 10, true ); // calls computeReferences which calls setTheTitle
    } else {
      return false;
    }
    return true;
  }

  /** react to a user tap
   * @param view tapped view
   *   - button OK: tell the parent to create the projection profile with the current azimuth
   *   - button PLUS: increase azimuth
   *   - button MINUS: decrease azimuth
   */
  @Override
  public void onClick(View view)
  {
    Button b = (Button)view;
    if ( b == mBtnOk ) {
      mProjectionSurface.stopDrawingThread();
      mParent.doProjectedProfile( mName, mFrom, mAzimuth, mOblique );
      dismiss();
    } else if ( b == mBtnPlus ) {
      setAzimuth( mAzimuth + 1, true ); // calls computeReferences which calls setTheTitle
    } else if ( b == mBtnMinus ) {
      setAzimuth( mAzimuth - 1, true );
    } else if ( mWithOblique && b == mBtnReset ) {
      TDLog.v("reset oblique to 0");
      mOblique = 0;
      mProjBar.setProgress( 200 );
      computeReferences();
    }
  }

  /** react to a user tap on BACK: stop drawing and close the dialog with no further action
   */
  @Override
  public void onBackPressed()
  {
    mProjectionSurface.stopDrawingThread();
    dismiss();
  }

}
