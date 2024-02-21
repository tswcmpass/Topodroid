/** @file GlRenderer.java
 *
 * @author marco corvi
 * @date may 2020
 *
 * @brief Cave3D Renderer
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.utils.TDLog;
// import com.topodroid.c3in.ParserBluetooth;
// import com.topodroid.c3in.ParserSketch; // NO_C3D
import com.topodroid.c3out.ExportGltf;
import com.topodroid.c3out.ExportData;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

// import android.os.SystemClock;
// import android.content.Context;
// import android.app.Activity;

// import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.RectF;

// import android.net.Uri;

import java.util.List;
// import java.util.ArrayList;
import java.util.Locale;
import java.io.DataOutputStream;

public class GlRenderer implements Renderer 
{
  private static final float SCALE_P = 6.0f;
  private static final float SCALE_O = 1.5f;

  private static float ratio;

  static float NEAR_O = -1000.0f; // -Float.MAX_VALUE / 8;
  static float FAR_O  = 1000.0f;
  static float NEAR_P =  0.1f;
  static float FAR_P  = 10.0f;
  static float FOCAL  = 0.25f;
  static float SIDE   = 1.0f;

  static final float FOCAL_MIN  = 0.01f;
  static final float FOCAL_MAX  = 1.0f;
  static final float NEAR_P_MIN = 0.05f;
  static final float NEAR_P_MAX = 1.0f;
  static final float FAR_P_MIN  = 1.1f;
  static final float FAR_P_MAX  = 100.0f;

  static final float SIDE_MIN   = 0.01f;
  static final float SIDE_MAX   = 5.0f;
  static final float NEAR_O_MIN = -10000.0f;
  static final float NEAR_O_MAX = 0.0f;
  static final float FAR_O_MIN  = 0.0f;
  static final float FAR_O_MAX  = 10000.0f;

  private TopoGL mApp;
  private GlModel mModel = null;
  private TglParser mParser = null;
  private ParserDEM mDEM = null;
  private float[] mCenter = null;
  private float[] mOffset = null;  // center offset

  static final int MODE_TRANSLATE = 0;
  static final int MODE_ROTATE    = 1;
  // static final int MODE_ZOOM      = 2;
  static final int MODE_MAX       = 2;
  public static int mMinClino = 0;
  static int mMode = MODE_ROTATE;

  static private int mHalfWidth = 1;
  static private int mHalfHeight = 1;

  static final int PROJ_PERSPECTIVE  = 0;
  static final int PROJ_ORTHOGRAPHIC = 1;
  static int projectionMode = PROJ_ORTHOGRAPHIC;
  static void toggleProjectionMode() { projectionMode = 1 - projectionMode; }

  static boolean mMeasureCompute = false;

  static float nearZ() { return (projectionMode == PROJ_ORTHOGRAPHIC)? NEAR_O : NEAR_P; }
  static float farZ()  { return (projectionMode == PROJ_ORTHOGRAPHIC)? FAR_O  : FAR_P;  }

  private boolean stationDistance = false;
  void toggleStationDistance( boolean on ) { stationDistance = on; }

  private static volatile float mXAngle = -90;
  private static volatile float mYAngle = 180;
  private static volatile float mScaleP = SCALE_P; // perspective scale
  private static volatile float mScaleO = SCALE_O; // orthogonal scale
  private static volatile float mDXP    = 0;
  private static volatile float mDYP    = 0;
  // private static volatile float mDZP    = 0;
  private static volatile float mDXO    = 0;
  private static volatile float mDYO    = 0;
  // private static volatile float mDZO    = 0;

  private static float mScale0 = 1;
  // private static float mDX0    = 0;
  // private static float mDY0    = 0;
  // private static float mDZ0    = 0;

  private final float[] mMVPMatrix = new float[16];
  private float[] mMVPMatrixInv    = new float[16];

  private final float[] mPerspectiveMatrix  = new float[16];
  private final float[] mOrthographicMatrix = new float[16]; // 202301018 fixed typo
  private final float[] mViewMatrix       = new float[16];
  private final float[] mModelMatrix      = new float[16];
  private final float[] mModelViewMatrix  = new float[16];
  private final float[] mMVMatrixInvT     = new float[16];

  private float[] mXRotationMatrix = new float[16];
  private float[] mYRotationMatrix = new float[16];

  private boolean doMakeModel = false;
  // ------------------------------------------------------------------------

  RectF getSurfaceBounds() { return ( mModel == null )? null : mModel.mSurfaceBounds; }

  // ------------------------------------------------------------------------
  void resetTopGeometry() // same as setting top-view
  {
    projectionMode = PROJ_ORTHOGRAPHIC;
    NEAR_O = -1000.0f; // -Float.MAX_VALUE / 8;
    FAR_O  = 1000.0f;
    NEAR_P =  0.1f;
    FAR_P  = 10.0f;
    FOCAL  = 0.25f;
    SIDE   = 1.0f;
    mCenter = null;
    mYAngle = 180;
    mXAngle = -90;
    zoomOne();
    if ( mModel != null ) mModel.resetColorMode();
    makePerspectiveMatrix();
    makeOrthographicMatrix();
  }

  void zoomOne()
  { 
    mScaleP = SCALE_P;
    mScaleO = SCALE_O;
    mDXP = 0;
    mDYP = 0;
    // mDZP = 0;
    mDXO = 0;
    mDYO = 0;
    // mDZO = 0;
    makeModelMatrix();
  }

  /** unbind the textures
   */
  void unbindTextures()
  {
    mModel.unbindTextures();
  }

  // /** rebind the textures - empty : unused
  //  */
  // void rebindTextures()
  // {
  //   // mModel.rebindTextures(); // this method is empty
  // }

  /** prepare the 3D model
   * @param parser   data parser
   * @param reduce   ...   
   */
  private void prepareModel( TglParser parser, boolean reduce )
  {
    // mModel = new GlModel( mApp, mHalfWidth*2, mHalfHeight*2, parser );
    // if ( mModel == null ) return;
    mModel.prepareModel( parser, reduce );
    float d = (float)mModel.getDiameter();
    mScale0 = 1.0f / d;
    // mDX0 = - mModel.getDx0() / d;
    // mDY0 =   mModel.getDy0() / d; 
    // mDZ0 = - mModel.getDz0() / d;
    // TDLog.v("Renderer Prepare model: diam " + d + " scale " + mScale0 + " D0 " + mDX0 + " " + mDY0 + " " + mDZ0 );
    // TDLog.v("Renderer Prepare model: diam " + d + " scale " + mScale0 );
    makeModelMatrix();
    doMakeModel = true;
  }

  /** construct the 3D model
   * @note this must run on the rendering thread
   */
  private void makeModel()
  {
    mModel.createModel();
    doMakeModel = false;
  }

  /** set the parser
   * @param parser   data parser
   * @param reduce   ...   
   * @note this is called on an external thread
   */
  void setParser( TglParser parser, boolean reduce )
  {
    // TDLog.v("Renderer set parser" );
    mParser = parser;
    // if ( parser.isEmpty() ) {
    //   prepareEmptyModel( parser );
    // } else {
      prepareModel( parser, reduce );
    // }
  }

  // FIXME BLUETOOTH INCREMENTAL
  // private void prepareEmptyModel( TglParser parser )
  // {
  //   // mModel = new GlModel( mApp, mHalfWidth*2, mHalfHeight*2, parser );
  //   // if ( mModel == null ) return;
  //   mModel.prepareEmptyModel( parser );
  //   mScale0 = 1.0f / 10.0f;
  //   makeModelMatrix();
  //   doMakeModel = true;
  // }

  // void setEmptyParser( TglParser parser )
  // {
  //   // TDLog.v("Renderer set empty parser" );
  //   mParser = parser;
  //   prepareEmptyModel( parser );
  // }
  // END INCREMENTAL

  // ------------------------------------------------------------------------
  /** cstr
   * @param app    3D activity
   * @param model  3D model
   */
  public GlRenderer( TopoGL app, GlModel model )
  {
    super();
    mApp = app;
    mModel = model;
  }

  /** @return the point-of-view / lighting-direction as a string
   */
  String getAngleString()
  {
    float clino   = -mXAngle;
    float azimuth = mYAngle; // (mYAngle>=180)? mYAngle-180 : mYAngle+180;
    float scale = mScaleO;
    float dx = -mDXO;
    float dy = mDYO;
    // float dz = mDZO;
    if (projectionMode == PROJ_PERSPECTIVE ) {
      scale = mScaleP;
      dx = mDXP;
      dy = mDYP;
    }

    String light = "";
    // String station = "";
    if ( mParser != null ) {
      if ( mParser.hasSurface() ) {
        float x_light = 90 - mXLight;
        float y_light = 90 - mYLight; if ( y_light < 0 ) y_light += 360;
        light = String.format(Locale.US, "L %.0f %.0f ", x_light, y_light );
      }
      // if ( mParser.mStartStation != null ) station = mParser.mStartStation.short_name;
    }
    return String.format(Locale.US, "C %.0f A %.0f S %.2f T %.2f %.2f %s", clino, azimuth, scale, dx, dy, light );
  }
 
  @Override
  public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
  {
    // TDLog.v( "Renderer surface created" );
    // Set the background clear color to red. The first component is
    // red, the second is green, the third is blue, and the last
    // component is alpha, which we don't use in this lesson.
    GL.clearColor(0.0f, 0.0f, 0.0f, 0.0f); 
    GL.enableAlpha( true );
    GL.enableDepth( true );
    // GL.enableCull( true ); 

    makeModelMatrix();
    // model can be created here were not for names
  }

  /**
   * onSurfaceChanged is called whenever the surface has changed. This is
   * called at least once when the surface is initialized. Keep in mind that
   * Android normally restarts an Activity on rotation, and in that case, the
   * renderer will be destroyed and a new one created.
   * 
   * @param width
   *            The new width, in pixels.
   * @param height
   *            The new height, in pixels.
   *
   * This is called at start-up
   */
  @Override
  public void onSurfaceChanged(GL10 glUnused, int width, int height)
  {
    // TDLog.v( "Renderer surface changed " + width + " " + height );
    // Set the OpenGL viewport to fill the entire surface.
    mHalfWidth = width / 2;
    mHalfHeight = height / 2;
    GL.viewport(0, 0, width, height);
    ratio = (float) width / height;
    makePerspectiveMatrix( );
    makeOrthographicMatrix( );

    mModel.initGL();
    // mModel.createModel( width ); // must be done on surface created
  }

  /** make perspective matrixA
   *   focal/aspect    0               0                      0
   *        0        focal             0                      0
   *        0          0    -(far+near)/(far-near)  2*far*near/(far-near)
   *        0          0               1                      0
   *
   * focal  = 1 / tg( field_of_vision / 2 )
   * aspect = aspect ration of the screen
   * far    = far plane (positive and far > near)
   * near   = near plane (positive, eg near=1 means plane z=-1)
   */
  void makePerspectiveMatrix( )
  {
    // TDLog.v("Renderer perspective " + FOCAL + " " + NEAR_P + " " + FAR_P );
    Matrix.frustumM(mPerspectiveMatrix, 0, -FOCAL*ratio, FOCAL*ratio, -FOCAL, FOCAL, NEAR_P, FAR_P ); // 0 ... : offset, left, right, bottom, top, near, far 
  }

  /** make orthographic matrix
   */
  void makeOrthographicMatrix( )
  {
    // TDLog.v("Renderer orthographic " + SIDE + " " + NEAR_O + " " + FAR_O );
    Matrix.orthoM( mOrthographicMatrix, 0, -SIDE*ratio, SIDE*ratio, -SIDE, SIDE, NEAR_O, FAR_O );
  }

  /** @return the X angle
   */
  public float getXAngle() { return mXAngle; }

  /** @return the Y angle
   */
  public float getYAngle() { return mYAngle; }

  /** normalize a vector
   * @param v  vector
   */
  private void normalize( float[] v ) {
    v[0] /= v[3];
    v[1] /= v[3];
    v[2] /= v[3];
    v[3] = 1;
  }

  // FIXME BLUETOOTH INCREMENTAL
  // void setModelPath( ArrayList< Cave3DStation > path )
  // { 
  //   if ( mModel != null ) mModel.setPath( path, mApp.hasBluetoothName() ); 
  // }

  /** clear the highlight of stations
   */
  void clearStationHighlight() 
  { 
    if ( mModel != null ) mModel.clearStationHighlight();
    mCenter = null;
  }

  void onTouch( float x, float y )
  {
    // TDLog.v("GL renderer on Touch() " + x + " " + y );
    // if ( ! GlNames.showStationNames() ) return;
    if ( GlNames.hiddenStations() ) return;
    if ( mMeasureCompute ) return;
    mMeasureCompute = true;

    x = ( x - mHalfWidth  ) / mHalfWidth;
    y = ( mHalfHeight - y ) / mHalfHeight;
    // if ( mMVPMatrixInv != null ) {
      /*
      final float[] near = { x, y, GlRenderer.nearZ(), 1 };
      final float[] far  = { x, y, GlRenderer.farZ(),  1 };
      float[] zn = new float[4];
      float[] zf = new float[4];
      Matrix.multiplyMV(zn, 0, mMVPMatrixInv, 0, near, 0 );
      Matrix.multiplyMV(zf, 0, mMVPMatrixInv, 0, far,  0 );
      normalize( zn );
      normalize( zf );
      final String fullname = mModel.checkNames( zn, zf, TopoGL.mSelectionRadius, (mParser.mStartStation == null) );
      */
      new MeasureComputer( mApp, x, y, mMVPMatrix, mParser, mDEM, mModel ).execute();
    // }
  }


  void setAngles( float azimuth, float clino )
  {
    mXAngle = clino;
    mYAngle = azimuth;
    makeModelMatrix();
    // refresh ?
  }

  public void setXYAngle( float dx_angle, float dy_angle )
  {
    if ( mMode == MODE_ROTATE ) {
      float x_angle = mXAngle - dx_angle;
      float y_angle = mYAngle + dy_angle; // TODO TDMath.in360( ... )
      // if ( x_angle < -90)  { x_angle = -90;  } else if ( x_angle > 90 ) { x_angle = 90; }
      if ( x_angle < -90)  { x_angle = -90;  } else if ( x_angle > mMinClino ) { x_angle = mMinClino; }
      if ( y_angle > 360 ) { y_angle -= 360; } else if ( y_angle < 0 )  { y_angle += 360; }
      mXAngle = x_angle;
      mYAngle = y_angle;
      makeModelMatrix();
      // setXYLight( dx_angle, dy_angle );
    } else {
      setScaleTranslation( 1.0f, dx_angle, dy_angle );
    }
  }

  private static final int DMAX = 2;

  public void setScaleTranslation( float scale, float dx, float dy )
  {
    // perspective
    float df = 25 * FOCAL / mHalfHeight; 
    float dxp = mDXP - dy * df;
    float dyp = mDYP - dx * df;

    float scale_p = mScaleP * scale; // 20230118 local var "scale_p"
    if ( scale_p < 0.05f ) {
      scale_p = 0.05f;
    } else {
      // if ( scale_p > 100.0f ) { scale_p = 100.0f;
      dxp *= scale;
      dyp *= scale;
    }
    mDXP = dxp;
    mDYP = dyp;
    mScaleP = scale_p;

    // orthogonal
    // TDLog.v("Renderer scale " + mScaleO + " " + scale + " at " + mDXO + " " + mDYO );
    // float dh = dx / (mHalfHeight);  // not working
    // mDZO -= dh * (float)Math.cos(mYAngle * Math.PI/180f);
    // mDYO -= dh * (float)Math.sin(mYAngle * Math.PI/180f);

    float dxo = mDXO - dy / (mHalfHeight);
    float dyo = mDYO - dx / (mHalfHeight);

    if ( mCenter != null ) {
      mOffset[0] -= dy / (mHalfHeight);
      mOffset[1] -= dx / (mHalfHeight);
    }

    float scale_o = mScaleO * scale; // 20230118 local var "scale_o"
    if ( scale_o < 0.05f ) {
      scale_o = 0.05f;
    } else {
      // if ( scale_o > 100.0f ) { scale_o = 100.0f;
      dxo *= scale;
      dyo *= scale;
    }
    mDXO = dxo;
    mDYO = dyo;
    mScaleO = scale_o;
    makeModelMatrix();
  }

  // x -> (x - dx0) * s0 * s, etc.
  // mat = |  s0*s   0    0   -dx0*s0*s |
  //       |    0  s0*s   0   -dy0*s0*s |
  //       |    0    0   ...     ...    |
  //       |    0    0    0       1     |
  private void initM( float[] m, float s )
  {
    for ( int k=0; k<16; ++k ) m[k] = 0;
    m[0] = m[5] = m[10] = mScale0 * s;
    // m[12] = mDX0 * s;
    // m[13] = mDY0 * s;
    // m[14] = mDZ0 * s;
    m[15] = 1;
  }

  private void identityM( float[] m ) 
  {
    for ( int k=0; k<16; ++k ) m[k] = 0;
    m[0] = m[5] = m[10] = m[15] = 1.0f;
  }

  private void makeModelMatrix()
  {
    float[] matrix1 = new float[16];
    float[] matrix2 = new float[16]; // rotation
    float[] matrix3 = new float[16];
    float dx = 0;
    float dy = 0;
    float dz = 0;

    Matrix.setRotateM( mXRotationMatrix, 0, mXAngle, 1.0f, 0.0f, 0.0f );
    Matrix.setRotateM( mYRotationMatrix, 0, mYAngle, 0.0f, 1.0f, 0.0f );
    Matrix.multiplyMM( matrix2, 0, mXRotationMatrix, 0, mYRotationMatrix, 0);

    if ( projectionMode == PROJ_PERSPECTIVE ) {
      // initM( matrix1, mScale*5 );
      initM( matrix1, mScaleP );
      dx = mDXP;
      dy = mDYP;
    } else {
      // initM( matrix1, mScale ); // bring model to (-2/3,2/3) cube
      initM( matrix1, mScaleO );
      dx = mDXO;
      dy = mDYO;
    }

    Matrix.multiplyMM( matrix3, 0, matrix2, 0, matrix1, 0);

    if ( mCenter != null ) { 
      float[] cx = new float[4];
      Matrix.multiplyMV( cx, 0, matrix3, 0, mCenter, 0);
      dx = (mOffset[0] - cx[0]); // * mScaleO;
      dy = (mOffset[1] - cx[1]); // * mScaleO;
      dz = (mOffset[2] - cx[2]); // * mScaleO;
      // TDLog.v("Renderer D0 " + mDXO + " " + mDYO + " D " + dx + " " + dy + " " + dz );
    }

    identityM( matrix1 ); // TRANSLATION
    matrix1[12] = dx;
    matrix1[13] = dy;
    matrix1[14] = dz;

    Matrix.multiplyMM( mModelMatrix, 0, matrix1, 0, matrix3, 0);

    // model matrix maps the coordinates into the unit cube
    // logMatrix( mModelMatrix );
  }

  // void logMatrix( float[] m )
  // {
  //    // TDLog.v("TopoGL m " + m[0] + " " + m[1] + " " + m[2] + " " + m[3] 
  //                + "\n  " + m[4] + " " + m[5] + " " + m[6] + " " + m[7] 
  //                + "\n  " + m[8] + " " + m[9] + " " + m[10] + " " + m[11] 
  //                + "\n " + m[12] + " " + m[13] + " " + m[14] + " " + m[15] );
  // }

  /**
   * OnDrawFrame is called whenever a new frame needs to be drawn. Normally,
   * this is done at the refresh rate of the screen.
   */
  @Override
  public void onDrawFrame(GL10 glUnused) 
  {
    // TDLog.v( "Renderer surface draw frame" );
    // Clear the rendering surface.
    GL.clear();
    if ( mModel == null ) return;
    if ( mParser != null && doMakeModel ) makeModel( );
    if ( ! mModel.hasParser() ) return;

    // model matrix maps model to world
    //    glLoadIdentity() + gltranslate() glRotate() glScale() etc.
    //    glGetFloatv( GL_MODELVIEW_MATRIX, &array )
    // view matrix maps world to camera
    //    glLoadIdentity() glTranslate() glRotate() glLookAt()
    //    glGetFloatv( GL_MODELVIEW_MATRIX, &array )
    // proj matrix maps camera to screen

    Matrix.setLookAtM( mViewMatrix, 0,   0, 0, -3,  0f, 0f, 0f,   0f, 1.0f, 0.0f); // offset, eye-XYZ, center-XYZ, up-XYZ
    // Matrix.invertM( mInverseViewMatrix, 0, mViewMatrix, 0 );

    // long time = SystemClock.uptimeMillis() % 4000L;
    // float angle = 0.090f * ((int) time);

    float[] matrix2 = new float[16];
    Matrix.multiplyMM( mModelViewMatrix, 0, mViewMatrix, 0, mModelMatrix, 0 );
    if ( projectionMode == PROJ_PERSPECTIVE ) {
      Matrix.multiplyMM( mMVPMatrix, 0, mPerspectiveMatrix, 0, mModelViewMatrix, 0);
    } else {
      Matrix.multiplyMM( mMVPMatrix, 0, mOrthographicMatrix, 0, mModelViewMatrix, 0);
    }
    Matrix.invertM( mMVPMatrixInv, 0, mMVPMatrix, 0 );
    Matrix.invertM( matrix2, 0, mModelViewMatrix, 0 );
    Matrix.transposeM( mMVMatrixInvT, 0, matrix2, 0 );

    // N.B. make sure GLSurfaceView does not have setRenderMode( GLSurfaceView.RENDERMODE_WHEN_DIRTY );
    mModel.draw( mMVPMatrix, mMVMatrixInvT, mLight );
  }

  static int lightMode = 0;
  static Vector3D mLight  = new Vector3D( 0.0f, 1.0f, 0.0f ); // must be normalized
  
  static Vector3D getLight() { return mLight; }

  static float mXLight = 10;
  static float mYLight = 0;

  // dx: vertical swipe,  dy: horizontal swipe
  static void setXYLight( float dx, float dy )
  {
    mYLight += dy*2;
    mXLight += dx;
    if ( mXLight < 0 ) { mXLight = 0; } else if ( mXLight >  90 ) { mXLight =  90; }
    if ( mYLight < 0 ) { mYLight += 360; } else if ( mYLight > 360 ) { mYLight -= 360; }
    float cx = (float)Math.cos( mXLight * Math.PI/180f);
    float sx = (float)Math.sin( mXLight * Math.PI/180f);
    float cy = (float)Math.cos( mYLight * Math.PI/180f);
    float sy = (float)Math.sin( mYLight * Math.PI/180f);
    mLight.x = sy * sx;
    mLight.y = cx;
    mLight.z = cy * sx;
    // mLight.normalized();
  }

  void resetLight()
  {
    mXLight  = 0;
    mYLight  = 0;
    mLight.x = 0;
    mLight.y = 1;
    mLight.z = 0;
  }

  // private float xoff, yoff, zoff; // model offsets in the view (changed in resetGeometry and changeParams )
  // private float xc, yc, zc;       // camera coords (computed in makeNZ )

  // void resetGeometry()
  // {
  //   mCenter = null;
  //   mXAngle = 0;
  //   mYAngle = 0;

  //   mScaleP = SCALE_P;
  //   mScaleO = SCALE_O;
  //   mDXP = 0; 
  //   mDYP = 0; 
  //   mDXO = 0; 
  //   mDYO = 0; 
  //   // mApp.setTheTitle( String );
  // }

  // ---------------------------------------------------------------------------
  // CENTER

  // boolean clearCenter()
  // {
  //   mCenter = null;
  //   return false;
  // }

  boolean setCenter()
  {
    Vector3D c = ( mModel != null) ? mModel.getCenter() : null;
    if ( c == null || mCenter != null ) {
      mCenter = null;
      return false;
    } else {
      mCenter = new float[4];
      mCenter[0] = (float)c.x;
      mCenter[1] = (float)c.y;
      mCenter[2] = (float)c.z;
      mCenter[3] = 1.0f;
      mOffset = new float[4];
      Matrix.multiplyMV( mOffset, 0, mModelMatrix, 0, mCenter, 0 );
      // TDLog.v("Renderer center offset " + mOffset[0] + " " + mOffset[1] + " " + mOffset[2] );
      return true;
    }
  }

  // ------------------------------- DISPLAY MODEs
  void toggleStations()    { GlNames.toggleStations(); }
  // void toggleSplays()      { GlModel.toggleSplays(); }
  // void togglePlanview()    { GlModel.togglePlanview(); }
  // void toggleSurface()     { GlModel.toggleSurface(); }
  // void toggleWallMode()    { GlModel.toggleWallMode(); }
  void toggleColorMode()   { if ( mModel != null ) mModel.toggleColorMode(); }
  void toggleFrameMode()   { GlModel.toggleFrameMode(); }
  // void toggleSurfaceLegs() { GlModel.toggleSurfaceLegs(); }

  int getColorMode() { return ( mModel != null )? mModel.getColorMode() : GlLines.COLOR_NONE; }

  boolean hasSurface() { return ( mModel != null ) && mModel.hasSurface(); }

  void notifyWall( int type, boolean b )
  { 
    // TDLog.v("renderer notify wall " + type + " " + b );
    if ( mModel != null ) {
      if ( type == TglParser.WALL_CW ) {
        mModel.prepareWalls( mParser.convexhullcomputer, b );
      } else if ( type == TglParser.WALL_POWERCRUST ) {
        mModel.prepareWalls( mParser.powercrustcomputer, b );
      } else if ( type == TglParser.WALL_HULL ) {
        mModel.prepareWalls( mParser.hullcomputer, b );
      } else if ( type == TglParser.WALL_TUBE ) {
        mModel.prepareWalls( mParser.tubecomputer, b );
      } else if ( type == TglParser.WALL_BUBBLE ) {
        mModel.prepareWalls( mParser.bubblecomputer, b );
      } else {
        mModel.clearWalls( );
      }
    }
  }

  // NO_C3D
  // void notifySketch( ParserSketch sketch_parser )
  // {
  //   if ( mModel != null ) mModel.prepareSketch( sketch_parser );
  // }

  void notifyDEM( ParserDEM dem ) 
  {
    if ( mModel != null ) {
      mDEM = dem;
      if ( mDEM != null ) {
        mModel.prepareDEM( dem );
        // mModel.prepareSurfaceLegs( mParser, dem );
      }
    }
  }

  double getDEM_Z( double e, double n )
  {
    return ( mDEM != null )? mDEM.computeZ( e, n ) : -1;
  }

  void notifyTexture( Bitmap bitmap )
  {
    if ( mModel != null && bitmap != null ) {
      // (new DialogBitmap( mApp, bitmap )).show();
      mModel.prepareTexture( bitmap );
    }
  }

  // void prepareTemperatures() // TEMPERATURE
  // {
  //   if ( mModel != null ) mModel.prepareTemperatures();
  // }

  void clearModel()
  {
    if ( mModel != null ) mModel.clearAll();
  }

  // ------- NO_C3D ----------------------------------------------
  // void updateSketches() // NO_C3D
  // {
  //   if ( mModel != null ) mModel.updateSketches();
  // }

  // List< GlSketch > getSketches() // NO_C3D
  // {
  //   return ( mModel != null )? mModel.getSketches() : null;
  // }

  // user null vector to clear location
  void setLocation( Vector3D v ) // WITH-GPS
  {
    if ( mModel != null ) mModel.setLocation( v );
  }

  void hideOrShow( List< Cave3DSurvey > surveys )
  {
    if ( mModel != null ) mModel.hideOrShow( surveys );
  }

  // @param pathname export path - can be filepath ending in gltf, or a folder name
  //     filepath.gltf:   additional files saved as filepath-XXX
  //     folder:          additional files saved as folder/XXX, gltf file saved as "folder.gltf"
  boolean exportGltf( DataOutputStream fos, String pathname, ExportData export )
  {
    if ( mModel == null ) return false;
    ExportGltf gltf = new ExportGltf( mModel );
    return gltf.write( fos, pathname, export.mName );
  }

  // FIXME BLUETOOTH INCREMENTAL
  // void addBluetoothSplay( Cave3DShot splay )        { mModel.addBluetoothSplay( splay ); }
  // void addBluetoothLeg( Cave3DShot leg )            { mModel.addBluetoothLeg( leg ); }
  // void addBluetoothStation( Cave3DStation station ) { mModel.addBluetoothStation( station ); }
}
