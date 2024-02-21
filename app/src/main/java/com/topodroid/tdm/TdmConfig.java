/** @file TdmConfig.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroid Manager cave-project object
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */
package com.topodroid.tdm;

import com.topodroid.utils.TDLog;
import com.topodroid.utils.TDFile;
import com.topodroid.utils.TDVersion;
import com.topodroid.utils.TDUtil;
import com.topodroid.utils.TDString;
import com.topodroid.TDX.TglColor;

// import java.io.File;
import java.io.IOException;
// import java.io.FileNotFoundException;
// import java.io.FileWriter;
// import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;

import java.util.ArrayList;
// import java.util.Date;
// import java.text.SimpleDateFormat;
// import java.util.Locale;
import java.util.Iterator;

class TdmConfig extends TdmFile
{
  // String mParentDir;            // parent directory
  String mSurveyName;
  // TdmSurvey mSurvey;             // inline survey in the tdconfig file TODO UNUSED
  private ArrayList< TdmSurvey > mViewSurveys = null; // current view surveys
  private ArrayList< TdmInput >  mInputs; // input surveys
  private ArrayList< TdmEquate > mEquates;
  private boolean mRead;        // whether this Tdm_Config has read the file
  private boolean mSave;        // whether this Tdm_Config needs to be saved

  /** cstr
   * @param filepath ...
   * @param save     ...
   */
  public TdmConfig( String filepath, boolean save )
  {
    super( filepath, null );
    // TDLog.v("Tdm_Config cstr filepath " + filepath );
    // mParentDir = (new File( filepath )).getParentFile().getName() + "/";
    // mSurvey    = null;
    mInputs    = new ArrayList< TdmInput >();
    mEquates   = new ArrayList< TdmEquate >();
    mRead      = false;
    mSave      = save;
    mSurveyName = getNameFromFilepath( filepath );
  }

  /** @return the survey name
   */
  public String getSurveyName() { return mSurveyName; }

  /** create and populate the list of survey-views
   * @param surveys   surveys
   */
  void populateViewSurveys( ArrayList< TdmSurvey > surveys )
  {
    mViewSurveys = new ArrayList< TdmSurvey >(); // current view surveys
    for ( TdmSurvey survey : surveys ) {
      // TDLog.v("Populate survey " + survey.getName() );
      survey.reduce();
      mViewSurveys.add( survey );
    }
  }

  /** drop the equates with a given survey
   * @param survey    survey
   */
  void dropEquates( String survey )
  {
    // TDLog.v("drop equates with " + survey + " before " + mEquates.size() );
    if ( survey == null || survey.length() == 0 ) return;
    ArrayList< TdmEquate > equates = new ArrayList<>();
    for ( TdmEquate equate : mEquates ) {
      if ( equate.dropStations( survey ) > 1 ) {
        equates.add( equate );
        setSave();
      }
    }
    mEquates = equates;
    // TDLog.v("dropped equates with " + survey + " after " + mEquates.size() );
  }

  /** insert an equate
   * @param equate  equate
   */
  void addEquate( TdmEquate equate ) 
  {
    if ( equate == null ) return;
    mEquates.add( equate );
    setSave();
    // TDLog.v("nr. equates " + mEquates.size() );
  }

  /** unconditionally remove an equate
   * @param equate  equate to remove
   */
  void removeEquate( TdmEquate equate ) 
  { 
    mEquates.remove( equate );
    setSave();
  }
    
  /** @return true if the cave-project has a given input
   * @param name   input survey name
   */
  boolean hasInput( String name )
  {
    if ( name == null ) return false;
    // TDLog.v("Tdm_Config check input name " + name );
    for ( TdmInput input : mInputs ) {
      // TDLog.v("Tdm_Config check input " + input.mName );
      if ( name.equals( input.getSurveyName() ) ) return true;
    }
    return false;
  }

  /** @return the last array index with name after the given string (0 if array is empty, the array-length if all name are before the given string)
   * @param name  given string
   */
  private int getInputIndex( String name )
  {
    int len = mInputs.size();
    if ( len == 0 ) {
      // TDLog.v(name + " in [len 0] -> 0" );
      return 0;
    }
    TdmInput input = mInputs.get( len-1 );
    if ( len == 1 ) {
      return ( input.getName().compareToIgnoreCase( name ) <= 0 )? 1 : 0;
      // TDLog.v(name + " in [len 1]: " + input.getName() + " -> " + ret );
      // return ret;
    }
    if ( input.getName().compareToIgnoreCase( name ) <= 0 ) {
      // TDLog.v(name + " in [len " + len + "]: last " + input.getName() + " -> " + len );
      return len;
    }
    input = mInputs.get( 0 );
    if ( input.getName().compareToIgnoreCase( name ) > 0 ) {
      // TDLog.v(name + " in [len " + len + "]: first " + input.getName() + " -> " + 0 );
      return 0;
    }
    int a1 = 0;
    int a2 = len;
    while ( a1+1 < a2 ) {
      int a = (a1 + a2)/2;
      input = mInputs.get( a );
      if ( input.getName().compareToIgnoreCase( name ) <= 0 ) { a1 = a; } else { a2 = a; }
    }
    // LOG
    // StringBuilder names = new StringBuilder();
    // names.append(name).append( " in [len " + len + "]: " );
    // for ( int i=0; i<len; ++i ) names.append(" ").append( mInputs.get(i).getName() );
    // names.append(" -> ").append(a2);
    // TDLog.v( names.toString() );

    return a2;
  } 

  // DEBUG
  // void printInputs()
  // {
  //   int len = mInputs.size();
  //   StringBuilder names = new StringBuilder();
  //   names.append( "INPUTS [len " + len + "]: " );
  //   for ( int i=0; i<len; ++i ) names.append(" ").append( mInputs.get(i).getName() );
  //   TDLog.v( names.toString() );
  // }

  /** insert an input
   * @param name  input name
   * @param color input color
   * @note this is called by readFile
   */
  private void insertInput( String name, int color )
  {
    if ( name == null ) return;
    // TDLog.v( "insert input " + name );
    int at = getInputIndex( name );
    mInputs.add( at, new TdmInput( name, color ) );
  }

  /** insert an input
   * @param input   input
   * @note this is called by the Config activity 
   */
  void addInput( TdmInput input )
  {
    if ( input == null ) return;
    // TDLog.v( "add input " + input.mName );
    int at = getInputIndex( input.getName() );
    mInputs.add( at, input );
    setSave();
  }

  /** @return the input at a given position in the array of inputs
   * @param pos   input position
   */
  TdmInput getInputAt( int pos ) { return mInputs.get(pos); }

  /** @return the input with the given name
   * @param name  input name
   */
  TdmInput getInput( String name )
  {
    for ( TdmInput input : mInputs ) {
      if ( input.getSurveyName().equals( name ) ) return input;
    }
    return null;
  }

  /** @return an iterator on the inputs
   */
  Iterator getInputsIterator() { return mInputs.iterator(); }

  /** @return the number of inputs
   */
  int getInputsSize() { return mInputs.size(); }

  /** @return the array of inputs
   */
  ArrayList< TdmInput > getInputs() { return mInputs; }

  /** @return the array of survey views
   */
  ArrayList< TdmSurvey > getViewSurveys() { return mViewSurveys; }

  /** @return the array of equates
   */
  ArrayList< TdmEquate > getEquates() { return mEquates; }

  /** remove an input
   * @param name   input name
   */
  private void dropInput( String name )
  {
    if ( name == null ) return;
    // TDLog.v( "drop input " + name );
    for ( TdmInput input : mInputs ) {
      if ( name.equals( input.getSurveyName() ) ) {
        mInputs.remove( input );
        setSave();
        return;
      }
    }
  }

  /** remove an input
   * @param input   TdmInput to romove
   */
  public void dropInput( TdmInput input )
  {
    mInputs.remove( input );
  }

  // /** remove chacked inputs UNUSED
  //  */
  // public void dropChecked( ) 
  // {
  //   final Iterator it = mInputs.iterator();
  //   while ( it.hasNext() ) {
  //     TdmInput input = (TdmInput) it.next();
  //     if ( input.isChecked() ) {
  //       mInputs.remove( input );
  //     }
  //   }
  // }

  /** set the array of inputs
   * @param inputs   array of inputs
   * @note this is called by the Config Activity
   */
  void setInputs( ArrayList< TdmInput > inputs ) 
  {
    if ( inputs != null ) {
      // TDLog.v( "set inputs " + inputs.size() );
      mInputs = inputs;
      setSave();
    }
  }

  /** set the "save" flag
   * @note used also by Config Activity when a source is added
   */
  void setSave() { mSave = true; }

  // ---------------------------------------------------------------
  // this is TDUtil.currentDate()
  // static String currentDate()
  // {
  //   SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd", Locale.US );
  //   return sdf.format( new Date() );
  // }

  // ---------------------------------------------------------------

  // this is called by the TdmConfigActivity when it goes on pause
  /** write the cave project info to the tdconfig file
   * @param force    whether to force writing
   * @return true if file has been successfully written
   */
  boolean writeTdmConfig( boolean force )
  {
    // TDLog.v( "save tdconfig " + this + " save " + mSave + " force " + force );
    boolean ret = false;
    if ( mSave || force ) { // was mRead || force
      String filepath = getFilepath();
      try {
        BufferedWriter bw = TDFile.getTopoDroidFileWriter( filepath );
        PrintWriter pw = new PrintWriter( bw );
        writeTd( pw );
        bw.close(); // close the stream flushing it first
        ret = true;
      } catch ( IOException e ) { 
        TDLog.Error("Tdm Config write file " + filepath + " I/O error " + e.getMessage() );
      }
      mSave = false;
    }
    return ret;
  }

  /** read the cave project info from the tdconfig file
   * @note the file is read only once - if already read it is skipped
   */
  void readTdmConfig()
  {
    // TDLog.v( "read tdconfig " + this + " " + mRead );
    if ( mRead ) return;
    readFile();
    mRead = true;
  }

  // ---------------------------------------------------------
  // READ and WRITE

  /** write the cave project info to a tdconfig file
   * @param pw   file writer
   */
  private void writeTd( PrintWriter pw ) throws IOException
  {
    // TDLog.v("save config " + mSurveyName );
    pw.format("# created by TopoDroid Manager %s - %s\n", TDVersion.string(), TDUtil.currentDate() );
    pw.format("source\n");
    pw.format("  survey \"%s\"\n", mSurveyName );
    for ( TdmInput input : mInputs ) {
      // FIXME path
      String path = input.getSurveyName();
      // TDLog.v("config write add survey <" + path + ">" );
      pw.format("    load \"%s\" -color %d\n", path, (input.getColor() & 0xffffff) );
    }
    for ( TdmEquate equate : mEquates ) {
      pw.format("    equate");
      for ( String st : equate.mStations ) pw.format(" \"%s\"", st );
      pw.format("\n");
    }
    pw.format("  endsurvey\n");
    pw.format("endsource\n");
  }

  /** extract the project name from a filepath
   * @param filepath    file pathname
   * @return the project name, ie, the name before the extension (namely ".tdconfig")
   */
  private String getNameFromFilepath( String filepath )
  {
    int start = filepath.lastIndexOf('/') + 1;
    int end   = filepath.lastIndexOf('.');
    return ( end > start )? filepath.substring( start, end ) : filepath.substring( start );
  }

  /** read the config file
   * @note if the file does not exist creates it and write an empty tdconfig file
   */
  private void readFile( )
  {
    String filepath = getFilepath();
    try {
      BufferedReader br = TDFile.getTopoDroidFileReader( filepath );
      if ( br == null ) { // file does not exist (or is not readable)
        TDLog.Error("file no-exist or no-read: " + filepath );
        mSurveyName = getNameFromFilepath( filepath );
        writeTdmConfig( true );
        return;
      }

      // TDLog.v( "read config " + filepath );
      String line = br.readLine();
      int cnt = 1;
      // TDLog.v( Integer.toString(cnt) + ":" + line );
      while ( line != null ) {
        line = line.trim();
        int pos = line.indexOf( '#' );
        if ( pos >= 0 ) line = line.substring( 0, pos );
        if ( line.length() > 0 ) {
          String[] vals = TDString.splitOnStrings( line );
          if ( vals.length > 0 ) {
            if ( vals[0].equals( "source" ) ) {
            } else if ( vals[0].equals( "survey" ) ) {
              for (int k=1; k<vals.length; ++k ) {
                if ( vals[k].length() > 0 ) {
                  mSurveyName = vals[k];
                  break;
                }
              }
            } else if ( vals[0].equals( "load" ) ) {
              for (int k=1; k<vals.length; ++k ) {
                // TDLog.v("vals[" + k + "]: <" + vals[k] );
                if ( vals[k].length() > 0 ) {
                  String surveyname = vals[k];
                  int color = TglColor.getSurveyColor(); // random color
                  for ( ++k; k<vals.length; ++k ) {
                    if ( vals[k].length() > 0 ) {
                      if ( vals[k].equals("-color") ) {
                        ++k;
                        if ( k < vals.length ) {
                          color = 0xff000000 | Integer.parseInt( vals[k] );
                        }
                      }
                      // break;
                    }
                  }
                  insertInput( surveyname, color );
                  break;
                }
              }    
            } else if ( vals[0].equals( "include" ) ) {
              String config_name = vals[1];
              // TODO include a tdconfig file 

            } else if ( vals[0].equals( "equate" ) ) {
              TdmEquate equate = new TdmEquate();
              for (int k=1; k<vals.length; ++k ) {
                if ( vals[k].length() > 0 ) {
                  equate.addStation( vals[k] );
                }
              }
              mEquates.add( equate );
            }
          }
        }
        line = br.readLine();
        ++ cnt;
      }
      br.close();
    } catch ( IOException e ) {
      // TODO
      TDLog.Error( "TdManager exception " + e.getMessage() );
    }
    // TDLog.v( "Tdm_Config read file: nr. sources " + getInputsSize() );
  }
 
  // ---------------------------------------------------------
  // EXPORT

  /** export the project to therion format
   * @param overwrite  whether to overwrite the output file (if it exists)
   * @param bw         buffered writer
   * @return non-null string if success
   */
  String exportTherion( boolean overwrite, PrintWriter bw ) throws IOException
  {
    bw.format("# created by TopoDroid Manager %s - %s\n", TDVersion.string(), TDUtil.currentDate() );
    bw.format("source\n");
    bw.format("  survey \"%s\"\n", mSurveyName );
    for ( TdmInput input : mInputs ) {
      // FIXME path
      String path = "../th/" + input.getSurveyName() + ".th";
      // TDLog.v("config write add survey " + path );
      bw.format("    input \"%s\"\n", path );
    }
    for ( TdmEquate equate : mEquates ) {
      bw.format("    equate");
      for ( String st : equate.mStations ) bw.format(" \"%s\"", st );
      bw.format("\n");
    }
    bw.format("  endsurvey\n");
    bw.format("endsource\n");
    return "thconfig";
  }

  /** export the project to survex format
   * @param overwrite  whether to overwrite the output file (if it exists)
   * @param bw         buffered writer
   * @return non-null string if success
   */
  String exportSurvex( boolean overwrite, PrintWriter bw ) throws IOException
  {
    bw.format("; created by TopoDroid Manager %s - %s\n", TDVersion.string(), TDUtil.currentDate() );
    // TODO EXPORT
    for ( TdmInput s : mInputs ) {
      String path = "../svx/" + s.getSurveyName() + ".svx";
      bw.format("*include \"%s\"\n", path );
    }
    for ( TdmEquate equate : mEquates ) {
      bw.format("*equate");
      for ( String st : equate.mStations ) bw.format(" \"%s\"", toSvxStation( st ) );
      bw.format("\n");
    }
    return "survex";
  }

  /** convert a station name from therion to survex syntax
   * @param st   therion name of the station
   * @return survex name of the station
   */
  private String toSvxStation( String st )
  {
    int pos = st.indexOf('@');
    return st.substring(pos+1) + "." + st.substring(0,pos);
  }

}
