/* @file SavePlotFileTask.java
 *
 * @author marco corvi
 * @date jan 2014
 *
 * @brief TopoDroid drawing: save drawing in therion format
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.TDX;

import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDFile;
import com.topodroid.utils.TDsafUri;
import com.topodroid.num.TDNum;
import com.topodroid.prefs.TDSetting;
import com.topodroid.common.PlotType;

import java.lang.ref.WeakReference;

import java.io.File; 
// import java.io.FileDescriptor;
// import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.DataOutputStream;

import java.util.List;

import android.content.Intent;
import android.content.Context;

import android.os.AsyncTask;
// import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.net.Uri;

// import android.graphics.Bitmap;
// import android.graphics.Bitmap.CompressFormat;

class SavePlotFileTask extends AsyncTask<Intent,Void,Boolean>
{
  private String mFormat; 
  private Handler mHandler;
  // private TopoDroidApp mApp;
  private final WeakReference<DrawingWindow> mParent;
  private final TDNum mNum;
  // private final DrawingUtil mUtil;
  private final DrawingCommandManager mManager;
  private List< DrawingPath > mPaths;
  private String mFullName; // file fullname, or shp basepath
  private int mType;        // plot type
  private PlotInfo mInfo;   // plot info (can be null for th2 export/overview)
  private int mProjDir; // projection azimuth
  private int mOblique; // oblique projection angle
  private int mSuffix;
  private int mRotate;  // nr. backups to rotate
  private String origin = null;
  private PlotSaveData psd1 = null;
  private PlotSaveData psd2 = null;
  private Uri mUri;
  private boolean mTh2Edit = false; // TH2EDIT

  /**
   * @param context  context
   * @param uri      output URI
   * @param parent   parent window
   * @param handler  result handler (to toast a feedback to the user)
   * @param num      data reduction
   * @param manager  drawing items
   * @param info     plot info
   * @param fullname plot fullname
   * @param type     plot type
   * @param proj_dir projection direction (only for projected profile)
   * @param suffix   plot save-mode: EXPORT (th2), SAVE (tdr), OVERVIEW (overview export th2)
   * @param rotate   whether to rotate the backups (only for TDR)
   */
  SavePlotFileTask( Context context, Uri uri, DrawingWindow parent, Handler handler,
		    TDNum num,
		    DrawingCommandManager manager, PlotInfo info,
                    String fullname, long type, int proj_dir, int oblique, int suffix, int rotate, boolean th2_edit )
  {
     mUri      = uri;
     mFormat   = context.getResources().getString(R.string.saved_file_2);
     mParent   = new WeakReference<DrawingWindow>( parent );
     mHandler  = handler;
     mNum      = num;
     mManager  = manager;
     mInfo     = info;
     mPaths    = null;
     mFullName = fullname;
     mType     = (int)type;
     mProjDir  = proj_dir;
     mOblique  = oblique;
     mSuffix   = suffix;    // plot save mode
     mRotate   = rotate;
     mTh2Edit  = th2_edit;
     if ( mRotate > TDPath.NR_BACKUP ) mRotate = TDPath.NR_BACKUP;
     // TDLog.v( "save plot file task [1] " + mFullName + " type " + mType + " suffix " + suffix );

     if ( TDLevel.overExpert && mSuffix == PlotSave.SAVE && TDSetting.mAutoExportPlotFormat == TDConst.SURVEY_FORMAT_CSX ) { // auto-export format cSurvey
       // TDLog.v( "auto export CSX");
       origin = parent.getOrigin();
       psd1 = parent.makePlotSaveData( 1, suffix, rotate );
       psd2 = parent.makePlotSaveData( 2, suffix, rotate );
     }
  }

  /**
   * @param info      split-plot info
   * @param fullname  split-plot full name (survey_name + plot_name)
   * @param type      split-plot tyoe
   * @param azimuth   azimuth (only for projected profile)
   * @note called to split a plot
   */
  SavePlotFileTask( Context context, Uri uri, DrawingWindow parent, Handler handler,
		    TDNum num,
		    List< DrawingPath > paths, PlotInfo info,
                    String fullname, long type, int proj_dir, int oblique )
  {
     mUri      = uri;
     mFormat   = context.getResources().getString(R.string.saved_file_2);
     mParent   = new WeakReference<DrawingWindow>( parent );
     mHandler  = handler;
     mNum      = num;
     mManager  = null;
     mPaths    = paths;
     mInfo     = info;
     mFullName = fullname;
     mType     = (int)type;
     mProjDir  = proj_dir;
     mOblique  = oblique;
     mSuffix   = PlotSave.CREATE;
     mRotate   = 0;
     mTh2Edit  = false;
     // TDLog.v( "save plot file task [2] " + mFullName + " type " + mType + " suffix CREATE");
  }

  @Override
  protected Boolean doInBackground(Intent... arg0)
  {
    // boolean do_binary = (TDSetting.mBinaryTh2 && mSuffix != PlotSave.EXPORT ); // TDR BINARY

    // TDLog.v( "save plot file task bkgr start");
    // synchronized( TDPath.mTherionLock ) // FIXME-THREAD_SAFE
    // TDLog.v( "save scrap files " + mFullName + " suffix " + mSuffix );

    if ( mManager == null && mPaths == null ) return false;
    if ( mSuffix == PlotSave.EXPORT ) {
      // TDLog.v( "save plot Therion file EXPORT " + mFullName );
      // File file2 = TDFile.getFile( TDPath.getTh2FileWithExt( mFullName ) );
      // DrawingIO.exportTherion( mManager, mType, file2, mFullName, PlotType.projName( mType ), mProjDir, mOblique, false ); // single sketch
      ParcelFileDescriptor pfd = TDsafUri.docWriteFileDescriptor( mUri );
      if ( pfd == null ) return false;
      float point_spacing = TDSetting.mBezierStep; // TH2EDIT save params that are temporarily changed
      try {
        if ( mTh2Edit ) { // TH2EDIT special params values:
          TDSetting.mBezierStep = 0.0f;
        }
        // BufferedWriter bw = new BufferedWriter( (pfd != null)? TDsafUri.docFileWriter( pfd ) : new FileWriter( TDPath.getTh2FileWithExt( mFullName ) ) );
        BufferedWriter bw = new BufferedWriter( TDsafUri.docFileWriter( pfd ) );
        DrawingIO.exportTherion( mManager, mType, bw, mFullName, PlotType.projName( mType ), mProjDir, mOblique, false, mTh2Edit ); // single sketch
        // bw.flush(); // FIXME system error
        bw.close();
      } catch ( IOException e ) {
        TDLog.Error( e.getMessage() );
        e.printStackTrace(); 
        return false;
      } finally {
        if ( mTh2Edit ) { // TH2EDIT restore params
          TDSetting.mBezierStep = point_spacing;
        }
        TDsafUri.closeFileDescriptor( pfd );
      }
      return true; 
    } else if ( mSuffix == PlotSave.OVERVIEW ) {
      // TDLog.v( "save plot OVERVIEW " + mFullName );
      // File file = TDFile.getFile( TDPath.getTh2FileWithExt( mFullName ) );
      // DrawingIO.exportTherion( mManager, mType, file, mFullName, PlotType.projName( mType ), mProjDir, mOblique, true ); // multi-sketch
      ParcelFileDescriptor pfd = TDsafUri.docWriteFileDescriptor( mUri );
      if ( pfd == null ) return false;
      try {
        // BufferedWriter bw = new BufferedWriter( (pfd != null)? TDsafUri.docFileWriter( pfd ) : new FileWriter( TDPath.getTh2FileWithExt( mFullName ) ) );
        BufferedWriter bw = new BufferedWriter( TDsafUri.docFileWriter( pfd ) );
        DrawingIO.exportTherion( mManager, mType, bw, mFullName, PlotType.projName( mType ), mProjDir, mOblique, true, false ); // multi-sketch, no th2_edit
        // bw.flush(); // FIXME necessary ???
        bw.close();
      } catch ( IOException e ) {
        TDLog.Error( e.getMessage() );
        e.printStackTrace(); 
        return false;
      } finally {
        TDsafUri.closeFileDescriptor( pfd );
      }
      return true;

    } else { // ( mSuffix == PlotSave.SAVE || mSuffix == PlotSave.CREATE )

      // boolean ret1 = true; // false = png failed
      // boolean ret2 = true; // false = binary cancelled
      // TDLog.v( "save plot SAVE (no action) " + mFullName );
      // // TDLog.v( "save plot Therion file SAVE " + mFullName );
      if ( TDLevel.overExpert && TDSetting.mAutoExportPlotFormat >= 0 && mManager != null ) {
        // APP_OUT_DIR
        switch ( TDSetting.mAutoExportPlotFormat ) { // auto-export format
          case TDConst.SURVEY_FORMAT_TH2:
            // TDLog.v("EXPORT AUTO th2 " + mFullName );
            File file2 = TDPath.getOutExportFile( mFullName + ".th2" ); // FIXME move to DrawingIO
            DrawingIO.exportTherionExport( mManager, mType, file2, mFullName, PlotType.projName( mType ), mProjDir, mOblique, false, false ); // false= single sketch
            break;
          case TDConst.SURVEY_FORMAT_DXF:
            if ( mParent.get() != null /* && ! mParent.get().isFinishing() */ ) { // APP_OUT_DIR was ! parent.isFinishing()
              // TDLog.v("EXPORT AUTO dxf " + mFullName );
              mParent.get().doSaveWithExt( null, mNum, mManager, mType, mFullName, "dxf", false );
            }
            break;
          case TDConst.SURVEY_FORMAT_SVG:
            if ( mParent.get() != null /* && ! mParent.get().isFinishing() */ ) {
              // TDLog.v("EXPORT AUTO svg " + mFullName );
              mParent.get().doSaveWithExt( null, mNum, mManager, mType, mFullName, "svg", false );
            }
            break;
          case TDConst.SURVEY_FORMAT_SHP:
            // TDLog.v("EXPORT AUTO shz " + mFullName );
            if ( mParent.get() != null /* && ! mParent.get().isFinishing() */ ) {
              mParent.get().doSaveWithExt( null, mNum, mManager, mType, mFullName, "shz", false );
            }
            break;
          case TDConst.SURVEY_FORMAT_XVI:
            // TDLog.v("EXPORT AUTO xvi " + mFullName );
            if ( mParent.get() != null /* && ! mParent.get().isFinishing() */ ) {
              mParent.get().doSaveWithExt( null, mNum, mManager, mType, mFullName, "xvi", false );
            }
            break;
          // case TDConst.SURVEY_FORMAT_C3D: // NO_C3D
          //   // TDLog.v("EXPORT AUTO c3d " + mFullName );
          //   if ( mParent.get() != null /* && ! mParent.get().isFinishing() */ ) {
          //     mParent.get().doSaveWithExt( null, mNum, mManager, mType, mFullName, "c3d", false );
          //   }
          //   break;
          case TDConst.SURVEY_FORMAT_CSX: // IMPORTANT CSX must come before PNG
            // TDLog.v("EXPORT AUTO csx " + mFullName );
            if ( PlotType.isSketch2D( mType ) ) {
              if ( mParent.get() != null /* && ! mParent.get().isFinishing() */ ) {
                mParent.get().doSaveCsx( null, origin, psd1, psd2, false );
              }
              break;
            } else { // X-Section cSurvey are exported as PNG
              // fall-through
            }
          // case TDConst.SURVEY_FORMAT_PNG: // NO_PNG
          //   // TDLog.v("EXPORT AUTO png " + mFullName );
          //   Bitmap bitmap = mManager.getBitmap();
          //   if (bitmap == null) {
          //     TDLog.Error( "cannot save PNG: null bitmap" );
          //     // ret1 = false;
          //   } else {
          //     float scale = mManager.getBitmapScale();
          //     if (scale > 0) {
          //       // FIXME execute must be called from the main thread, current thread is working thread
          //       (new ExportBitmapToFile( null, null, bitmap, scale, mFullName, false )).exec(); // null URI, null toast-format
          //     } else {
          //       TDLog.Error( "cannot save PNG: negative scale" );
          //       // ret1 = false;
          //     }
          //   }
          //   break;
          default:
            TDLog.e("EXPORT AUTO unknown " + mFullName + " " + TDSetting.mAutoExportPlotFormat );
            break;
        }
      }
      // TDLog.v( "save plot SAVE");
      assert( mInfo != null );
      String filename = TDPath.getTdrFileWithExt( mFullName ) + TDPath.BCK_SUFFIX;

      // TDLog.v( "rotate backups " + filename + " rotate " + mRotate );
      if ( mRotate > 0 ) {
        TDPath.rotateBackups( filename, mRotate ); // does not do anything if mRotate <= 0
      }

      long now  = System.currentTimeMillis();
      TDFile.clearExternalTempDir( 600000 ); // clean the cache ten minutes before now

      // TDLog.Log( TDLog.LOG_PLOT, "saving binary " + mFullName );
      // TDLog.v( "saving binary " + mFullName + " file " + file1.getPath() );
      String tempname1 = Integer.toString(mSuffix) + Long.toString(now);
      // File file1 = TDFile.getExternalTempFile( Integer.toString(mSuffix) + Long.toString(now) );
      try { 
        // File file1 = TDFile.getExternalTempFile( tempname1 );
        // FileOutputStream fos = new FileOutputStream( file1 );
        // BufferedOutputStream bos = new BufferedOutputStream( fos );
        // DataOutputStream dos = new DataOutputStream( bos );
        DataOutputStream dos = TDFile.getExternalTempFileOutputStream( tempname1 );
        if ( mSuffix == PlotSave.CREATE ) {
          // TDLog.v("Save Plot CREATE temp file, paths " + mPaths.size() );
          DrawingIO.exportDataStreamFile( mPaths, mType, mInfo, dos, mFullName, mProjDir, mOblique, 0 ); // set path scrap to 0
        } else {
          DrawingIO.exportDataStreamFile( mManager, mType, mInfo, dos, mFullName, mProjDir, mOblique );
        }
        dos.close();
      } catch ( IOException e ) {
        e.printStackTrace();
      }

      if ( isCancelled() ) {
        TDLog.Error( "binary save cancelled " + mFullName );
        // if ( ! file1.delete() ) TDLog.Error("File delete error"); // no need to delete cache file
        return false;
      } else {
        // TDLog.v( "save binary completed. file " + mFullName );

        String filename1 = TDPath.getTdrFileWithExt( mFullName );
        if ( ! TDFile.renameTopoDroidFile( filename1, filename1 + TDPath.BCK_SUFFIX ) ) {
          // TDLog.v("failed rename old " + filename1 + TDPath.BCK_SUFFIX );
        }
        if ( ! TDFile.renameExternalTempFile( tempname1, filename1 ) ) {
          TDLog.Error("failed rename itempfile to " + filename1 );
        }
        return true;
      }
      // TDLog.v( "save plot file task bkgr done");
    }
  }

  @Override
  protected void onPostExecute(Boolean bool)
  {
    super.onPostExecute(bool);
    // TDLog.v( "save plot file task post exec " + bool );
    if ( mHandler != null ) {
      mHandler.sendEmptyMessage( bool? 661 : 660 );
    }
  }
}

