/* @file ParserPocketTopo.java
 *
 * @author marco corvi
 * @date nov 2014
 *
 * @brief TopoDroid PocketTopo parser
 *
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * ----------------------------------------------------------
 */
package com.topodroid.inport;

import com.topodroid.utils.TDMath;
import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDFile;
import com.topodroid.utils.TDUtil;
// import com.topodroid.num.TDNum;
import com.topodroid.ptopo.PTFile;
import com.topodroid.ptopo.PTTrip;
import com.topodroid.ptopo.PTShot;
import com.topodroid.ptopo.PTDrawing;
import com.topodroid.ptopo.PTElement;
// import com.topodroid.ptopo.PTXSectionElement; // not handled
import com.topodroid.ptopo.PTPolygonElement;
import com.topodroid.ptopo.PTPoint;
// import com.topodroid.ptopo.PTMapping;
// import com.topodroid.prefs.TDSetting;
import com.topodroid.common.ExtendType;
import com.topodroid.common.PlotType;
import com.topodroid.common.PointScale;
import com.topodroid.TDX.DrawingIO;
import com.topodroid.TDX.DrawingUtil;
import com.topodroid.TDX.DrawingPath;
import com.topodroid.TDX.DrawingPointPath;
import com.topodroid.TDX.DrawingLinePath;
// import com.topodroid.TDX.DrawingPointLinePath;
import com.topodroid.TDX.BrushManager;
import com.topodroid.TDX.TDPath;
import com.topodroid.TDX.PtCmapActivity;

// import java.io.File;
// import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
// import java.io.PrintWriter; // FIXME_TH2

// import java.io.FileOutputStream;
// import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.ArrayList;

// import java.util.Stack;
// import java.util.regex.Pattern;
import java.util.Locale;

import android.graphics.RectF;

class ParserPocketTopo extends ImportParser
{
  String mTitle = "";
  String mComment;
  String mStartFrom;

  static final private float PT_SCALE = 0.1f; // 100/1000

  ParserPocketTopo( InputStream fis, String filename, String surveyname, boolean apply_declination )
                           throws ParserException
  {
    super( apply_declination );
    String mStartFrom = null;
    // TDLog.Log( TDLog.LOG_PTOPO, "PocketTopo parser " + surveyname );
    // mName     = surveyname.replace(".top", "");
    mName     = surveyname;
    readPocketTopoFile( fis, filename, surveyname + "/tdr" );
    checkValid();
  }

  private void readPocketTopoFile( InputStream fis, String filename, String tdr_dir ) throws ParserException
  {
    PTFile ptfile = new PTFile();
    // TDLog.v( "Parser PT survey " + mName + " read file " + filename );
    try {
      if ( fis == null ) fis = new FileInputStream( filename );
      ptfile.read( fis );
      fis.close();
    } catch ( FileNotFoundException e ) {
      TDLog.Error( "file not found: " + filename );
      throw new ParserException();
    } catch ( IOException e ) { // on close
      TDLog.Error( "IO exception: " + e );
      throw new ParserException();
    }
    int nr_trip = ptfile.tripCount();
    // TDLog.v( "PT trip count " + nr_trip );
    mComment = "";
    // mTeam = "";
    if ( nr_trip > 0 ) { // use only the first trip
      PTTrip trip = ptfile.getTrip(0);
      mDate = String.format(Locale.US, "%04d-%02d-%02d", trip._year, trip._month, trip._day );
      if ( trip.hasComment() ) mComment = trip.comment();
      // trip.declination(); NOT USED
      // TODO create a survey
    } else {
      mDate = TDUtil.currentDate();
    }

    int shot_count = ptfile.shotCount();
    // TDLog.v( "PT shots count " + shot_count );
    int extend = ExtendType.EXTEND_NONE;
    int ext_flag = extend;
    // DBlock b     = null;  // temporary block pointer
    // DBlock start = null;  // first block inserted
    // DBlock last  = null;  // last block on the list

    String from_prev = "";
    String to_prev   = "";
    // Pattern pattern = Pattern.compile( "0+" );
    // ArrayList< DBlock > data = new ArrayList<>();

    for ( int s=0; s < shot_count; ++s ) {
      PTShot shot = ptfile.getShot(s);
      String from = shot.from().toString();
      String to   = shot.to().toString();
      float da = shot.distance();
      float ba = TDMath.in360( shot.azimuth() );
      float ca = shot.inclination();
      float ra = shot.roll();
      // TDLog.v("Parser PT shot <" + from + ">-<" + to + ">: " + da + " " + ba + " " + ca );
      from = from.replaceAll( "^0+", "" );
      to   = to.replaceAll( "^0+", "" );
      if ( from.equals("-") ) from = "";
      if ( to.equals("-") )   to = "";
      if ( from.equals( from_prev ) && to.equals( to_prev ) && ! to_prev.equals("") ) {
        from = "";
        to   = "";
      } else {
        from_prev = from;
        to_prev   = to;
      }
      if ( shot.isFlipped() ) {
        if ( extend != ExtendType.EXTEND_LEFT ) {
          extend = ExtendType.EXTEND_LEFT;
        //   ext_flag = extend;
        // } else {
        //   ext_flag = ExtendType.EXTEND_NONE;
        }
      } else {
        if ( extend != ExtendType.EXTEND_RIGHT ) {
          extend = ExtendType.EXTEND_RIGHT;
        //   ext_flag = extend;
        // } else {
        //   ext_flag = ExtendType.EXTEND_NONE;
        }
      }
      // store both legs and splays in the shots array
      // splays "extend" is not set
      ext_flag = ( to.length() == 0 )? ExtendType.EXTEND_UNSET : extend;
      shots.add( new ParserShot( from, to,  da, ba, ca, ra, ext_flag, 0, false, false, false,
                                 shot.hasComment()? shot.comment() : "" ) );
      if ( mStartFrom == null && from.length() > 0 && to.length() > 0 ) {
        mStartFrom = from;
      }
      // if ( from.length() > 0 && to.length() > 0 ) {
      //   data.add( new DBlock( from, to,  da, ba, ca, ra, extend, ExtendType.BLOCK_MAIN_LEG ) );
      // }
    }
    // TDLog.v( "PT parser shot count " + shot_count + " size " + shots.size() );

    // TDLog.v("Parser PT start from " + mStartFrom );
    // float declination = mData.getSurveyDeclination( mSid );
    // TDNum num = new TDNum( data, mStartFrom, null, null, declination, null ); // null formatClosure
    // TDLog.v( "Num E " + (20*num.surveyEmin()) + " " + (20*num.surveyEmax()) +
    //                 " S " + (20*num.surveySmin()) + " " + (20*num.surveySmax()) +
    //                 " H " + (20*num.surveyHmin()) + " " + (20*num.surveyHmax()) +
    //                 " V " + (20*num.surveyVmin()) + " " + (20*num.surveyVmax()) );
    
 
    // FIXME PT parser uses therion scrap syntax
    if ( mStartFrom != null ) {
      // NumStation st = num.getStation( mStartFrom );
      // TDLog.v("Parser PT start " + st.e + " " + st.s );

      int over_scale = ptfile.getOverview().scale();

      PTDrawing outline = ptfile.getOutline();
      String scrap_name1 = mName + "-1p";
      // String filename1 = TDPath.getTh2File( mName + "-1p.th2" );
      String filename1 = TDPath.getTdrFileWithExt( tdr_dir, scrap_name1 );
      // TDLog.v( "PT parser scrap p: " + filename1 );
      writeDrawing( filename1, scrap_name1, outline, PlotType.PLOT_PLAN, over_scale );

      PTDrawing sideview = ptfile.getSideview();
      String scrap_name2 = mName + "-1s";
      // String filename2 = TDPath.getTh2File( mName + "-1s.th2" );
      String filename2 = TDPath.getTdrFileWithExt( tdr_dir, scrap_name2 );
      // TDLog.v( "PT parser scrap s: " + filename2 );
      writeDrawing( filename2, scrap_name2, sideview, PlotType.PLOT_EXTENDED, over_scale );
      // TDLog.v( "display " + TopoDroidApp.mDisplayWidth + " " + TopoDroidApp.mDisplayHeight ); 

    } else {
      TDLog.Error( "PT null StartFrom");
      // throw new ParserException();
    }
  }

  /** return true if successful
   */
  private boolean writeDrawing( String filename, String scrap_name, PTDrawing drawing, long type, int over_scale )
                  throws ParserException
  {
    if ( drawing == null ) return false;
    float xoff = DrawingUtil.CENTER_X; // * 5;
    float yoff = DrawingUtil.CENTER_Y; // * 5;

    int elem_count = drawing.elementNumber();
    // TDLog.v( "PT drawing elements " + elem_count );

    TDPath.checkPath( filename );
    // File file = new File( filename );
    boolean ret = false;
    // synchronized( TDPath.mTherionLock ) // FIXME-THREAD_SAFE
    List< DrawingPath > paths = new ArrayList<>();

    {
      try {
        // FileWriter fw = new FileWriter( file ); // FIXME_TH2
        // PrintWriter pw = new PrintWriter( fw );
        // if ( type == PlotType.PLOT_PLAN ) {
        //   pw.format("scrap 1p -proj plan "); // scrap_name
        // } else {
        //   pw.format("scrap 1s -proj extended ");
        // }
        // pw.format("[0 0 1 0 0.0 0.0 1.0 0.0 m]\n");

        float map_scale = drawing.mapping().scale();
        float scale = 0.02f;
        // float scale = 2.0f / map_scale;
	// TDLog.v("Parser PT map scale " + scale + " outline_scale " + map_scale );

        float x0 = 0; // (mapping.origin().x());
        float y0 = 0; // (mapping.origin().y());
        // TDLog.v("Parser PT map origin " + x0 + " " + y0 + " elements " + elem_count );

        float xmin=1000000f, xmax=-1000000f, 
              ymin=1000000f, ymax=-1000000f;

        if ( elem_count > 0 ) {
          for (int h=0; h<elem_count; ++h ) {
            PTElement element = drawing.getElement(h);
            if ( element instanceof PTPolygonElement ) {
              PTPolygonElement elem = (PTPolygonElement)element;
              int point_count = elem.pointCount();
              int col = elem.getColor();
              if ( point_count > 1 ) {
		String th_name = PtCmapActivity.getLineThName( col );
                // add a line to the plotCanvas
		int line_type = BrushManager.getLineIndexByThName( th_name );
		if ( line_type < 0 ) line_type = 0;
		DrawingLinePath line = new DrawingLinePath( line_type, 0 );

                PTPoint point = elem.point(0);
                // pw.format("line %s\n", th_name );
                int k=0;
                int x1 = (int)( xoff + scale*(point.x() - x0));
                int y1 = (int)( yoff + scale*(point.y() - y0));
		// update bbox
		if ( x1 < xmin ) { xmin = x1; } if ( x1 > xmax ) { xmax = x1; }
		if ( y1 < ymin ) { ymin = y1; } if ( y1 > ymax ) { ymax = y1; }
                // FIXME drawer->insertLinePoint( x1, y1, type, canvas );
                // pw.format("  %d %d \n", x1, -y1 ); FIXME_TH2
		line.addStartPoint( x1, y1 );
                // TDLog.v("Parser PT elem " + h + ":0 " + x1 + " " + y1 + " point " + point.x() + " " + point.y() );

                for (++k; k<point_count; ++k ) {
                  point = elem.point(k);
                  int x = (int)( xoff + scale*(point.x() - x0) );
                  int y = (int)( yoff + scale*(point.y() - y0) );
		  if ( x < xmin ) { xmin = x; } else if ( x > xmax ) { xmax = x; }
		  if ( y < ymin ) { ymin = y; } else if ( y > ymax ) { ymax = y; }
                  // if ( Math.abs(x - x1) >= 4 || Math.abs(y - y1) >= 4 ) { // FIXME_TH2
                  //   x1 = x;
                  //   y1 = y;
                  //   // FIXME drawer->insertLinePoint( x, y, type, canvas );
                  //   pw.format("  %d %d \n", x1, -y1 );
                  //   // TDLog.v("Parser PT elem " + h + ":" + k + " " + x1 + " " + y1 + " point " + point.x() + " " + point.y() );
                  // } 
		  line.addPoint( x, y );
                }
                // FIXME drawer->insertLinePoint( x1, y1, type, canvas ); // close the line
                // FIXME pw.format("  %d %d \n", x1, y1 );
                // pw.format("endline\n"); // FIXME_TH2
		paths.add( line );
              } else if ( point_count == 1 ) {
		String th_name = PtCmapActivity.getPointThName(col);
		int point_type = BrushManager.getPointIndexByThName( th_name );
		if ( point_type < 0 ) point_type = 0;

                PTPoint point = elem.point(0);
                int x = (int)( xoff + scale*(point.x() - x0) );
                int y = (int)( yoff + scale*(point.y() - y0) );
		if ( x < xmin ) { xmin = x; } if ( x > xmax ) { xmax = x; }
		if ( y < ymin ) { ymin = y; } if ( y > ymax ) { ymax = y; }
                // FIXME drawer->insertPoint(x, y, type, canvas );
                // pw.format("point %d %d %s \n", x, -y, th_name ); // FIXME_TH2
                // TDLog.v("Parser PT elem " + h + " single " + x + " " + y );
		paths.add( new DrawingPointPath( point_type, x, y, PointScale.SCALE_M, "", "", 0 ) ); // no text, no options
              }
            }
          }
        }
        // pw.format("endscrap\n"); // FIXME_TH2
        // fw.flush();
        // fw.close();

	RectF bbox = new RectF( xmin, ymin, xmax, ymax );

        // File file = new File( filename );
        // FileOutputStream fos = new FileOutputStream( file );
        // BufferedOutputStream b_fos = new BufferedOutputStream( fos );
        // DataOutputStream dos = new DataOutputStream( b_fos );
        DataOutputStream dos = TDFile.getTopoDroidFileOutputStream( filename );
	DrawingIO.exportDataStream( (int)type, dos, null, scrap_name, 0, 0, bbox, paths, 0 ); // proj_dir = 0, oblique = 0, scrap = 0
        dos.close();
        // fos.close();

        ret = true;
      } catch ( IOException e ) {
        TDLog.Error( mName + " scraps IO error " + e );
        // if ( ! file.delete() ) TDLog.Error("file delete error");
        TDFile.deleteFile( filename );
        throw new ParserException();
      }
    }
    return ret; // always true
  }


}
