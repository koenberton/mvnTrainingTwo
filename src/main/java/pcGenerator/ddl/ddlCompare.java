package pcGenerator.ddl;

import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

import java.util.ArrayList;
import java.util.StringTokenizer;

import office.xlsxWriterMark2;
import pcGenerator.powercenter.infaDataType;
import pcGenerator.powercenter.infaSource;
import pcGenerator.powercenter.infaSourceField;

public class ddlCompare {

	pcDevBoosterSettings xMSet = null;
	gpPrintStream fout = null;
	ddlDataTypeConvertor conv=null;
	String scrubList1 =null;
	String scrubList2 =null;
	rdbmsDatatype.DBMAKE rdbmsTipe1 = null;
	rdbmsDatatype.DBMAKE rdbmsTipe2 = null;
	int linesWritten=0;
	String FOutputName=null;
	String FOutputExcel=null;
	int lineCounter=0;
	boolean noCSV=true;
	String firstFile=null;
	String secondFile=null;
	
	String NOP = "§";
	
	private String sHeader = "MatchPerc,DBName,TableName,ColName,DataType,InfaType,Precision,Scale,DBName,TableName,ColName,DataType,InfaType,Precision,Scale,NameMatch,TypeMatch,SizeMatch,Match";
	
    private xlsxWriterMark2 xcw = null;
    boolean ExcelHasBeenCreated = false;
    
	
	class csv
	{
		String Owner1;
		String TabName1;
		String ColName1;
		String Type1;
		String InfaType1;
		int Prec1;
		int Scale1;
		String Owner2;
		String TabName2;
		String ColName2;
		String Type2;
		String InfaType2;
		int Prec2;
		int Scale2;
		int NameMatch;
		int TypeMatch;
		int SizeMatch;
		int Match;
		int matchPerc;
		csv()
		{
			Owner1=NOP;
			TabName1=NOP;
			ColName1=NOP;
			Type1=NOP;
			InfaType1=NOP;
			Prec1=-1;
			Scale1=-1;
			Owner2=NOP;
			TabName2=NOP;
			ColName2=NOP;
			Type2=NOP;
			InfaType2=NOP;
			Prec2=-1;
			Scale2=-1;
			NameMatch=0;
			TypeMatch=0;
			SizeMatch=0;
			Match=0;
			matchPerc=0;
		}
	}
	ArrayList<csv> tbldmp = null;
	
	//----------------------------------------------------------------
	public ddlCompare(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
		conv = new ddlDataTypeConvertor(xMSet);
		tbldmp = new ArrayList<csv>();
	}
	
	//----------------------------------------------------------------
	private void logit(int level , String sLog)
	//----------------------------------------------------------------
	{
			xMSet.logit(level, "[" +  this.getClass().getName() + "] " + sLog );
	}
					
	//----------------------------------------------------------------
	private void errit(String sLog)
	//----------------------------------------------------------------
	{
			logit(0,sLog);	
	}
	
	//----------------------------------------------------------------
	public boolean compareDDLScripts(String DDLScriptOne , String dbTipeOne , String scrub1 ,String DDLScriptTwo , String dbTipeTwo , String scrub2 )
	//----------------------------------------------------------------
	{
		firstFile = DDLScriptOne;
		secondFile=DDLScriptTwo;
		//
		logit( 1, "Matching [" + DDLScriptOne + "] [" + dbTipeOne + "] against [" + DDLScriptTwo + "] [" + dbTipeTwo + "]");
		//
		rdbmsDatatype dbt = new rdbmsDatatype(xMSet);
		//
		scrubList1 = scrub1;
		rdbmsTipe1 = dbt.getDBMAKEType(dbTipeOne);
		scrubList2 = scrub2;
		rdbmsTipe2 = dbt.getDBMAKEType(dbTipeTwo);
		if( (rdbmsTipe1 == null) || (rdbmsTipe2 == null) ) return false;
		// 
		//
		if( !xMSet.xU.IsBestand(DDLScriptOne)) {
			errit("Cannot find first DDL script [" + DDLScriptOne + "]");
			return false;
		}
		if( !xMSet.xU.IsBestand(DDLScriptTwo)) {
			errit("Cannot find second DDL script [" + DDLScriptTwo + "]");
			return false;
		}
		//
		makeFileName(DDLScriptOne , rdbmsTipe1 , DDLScriptTwo , rdbmsTipe2);
		//
		db2Tokenize t1 = new db2Tokenize(xMSet , "first" , rdbmsTipe1 , null);
		ArrayList<infaSource> firstList = t1.parseFile(DDLScriptOne);
		if( firstList == null ) return false;
		//
		db2Tokenize t2 = new db2Tokenize(xMSet , "second" , rdbmsTipe2 , null );
		ArrayList<infaSource> secondList = t2.parseFile(DDLScriptTwo);
		if( secondList == null ) return false;
		//
		if( openWrite() == false ) return false;
		//
		compareList( firstList , secondList , true);
		// swap
		String s = scrubList1;
		scrubList1 = scrubList2;
		scrubList2 = s;
		rdbmsDatatype.DBMAKE dd = rdbmsTipe1;
		rdbmsTipe1 = rdbmsTipe2;
		rdbmsTipe2 = dd;
		fprintln("SWAP");
		compareList(  secondList , firstList , false ); 
		//
		if( dumpExcel( FOutputExcel) == false ) return false;
		//
		if( fout != null ) fout.close();
		logit( 1 , "Written [" + linesWritten + "] to " + FOutputName );
		//
		return true;
	}
	
	
	
	//----------------------------------------------------------------
	private void makeFileName(String DDLScriptOne , rdbmsDatatype.DBMAKE dbTipeOne , String DDLScriptTwo , rdbmsDatatype.DBMAKE dbTipeTwo )
	//----------------------------------------------------------------
	{
		String s1 = ""+dbTipeOne + "                            ";
		String s2 = xMSet.xU.GetFileName(DDLScriptOne) + "                             ";
		String s3 = ""+dbTipeTwo + "                           ";
		String s4 = xMSet.xU.GetFileName(DDLScriptTwo) + "                             ";
		String s5 = xMSet.xU.prntDateTime(System.currentTimeMillis(), "yyMMDDHHmmss");
		//
		String FShort = s1.substring(0,5).trim() + "-" + 
		                s2.substring(0,10).trim() + "-" + 
				        s3.substring(0,5).trim() + "-" + 
		                s4.substring(0,10) + "-" + 
				        s5;
		FOutputName = xMSet.getProjectDir() + xMSet.xU.ctSlash + FShort + ".csv";
		FOutputExcel = xMSet.getProjectDir() + xMSet.xU.ctSlash + FShort + ".xlsx";
	}
	
	
	
	//----------------------------------------------------------------
	private boolean openWrite()
	//----------------------------------------------------------------
	{
		        // First time round then create the excelwriter
				if( ExcelHasBeenCreated == false ) {
					ExcelHasBeenCreated = true;
					xcw = new xlsxWriterMark2( xMSet );
					if( do_documentcontrol() == false ) return false;
				}
				if( xcw == null ) {
					errit("System error VII - no excelwriter");
					return false;
				}
	    if( noCSV ) return true;
		fout = new gpPrintStream( FOutputName , "ASCII");
		fprintln(sHeader);
		return true;
	}
	
	//----------------------------------------------------------------
	private void write( csv x )
	//----------------------------------------------------------------
	{
		//if( fout != null ) {
			
			String sL = "" + x.matchPerc + "%,";
			//
			sL += x.Owner1 + "," + x.TabName1 + "," + x.ColName1 + "," + x.Type1 + "," + x.InfaType1 + "," + x.Prec1 + "," + x.Scale1 + ",";
			sL += x.Owner2 + "," + x.TabName2 + "," + x.ColName2 + "," + x.Type2 + "," + x.InfaType2 + "," + x.Prec2 + "," + x.Scale2 + ",";
			sL += x.NameMatch + "," + x.TypeMatch + "," + x.SizeMatch + "," + x.Match + ",";
			//
			sL = xMSet.xU.Remplaceer( sL , NOP , "" );
			sL = xMSet.xU.Remplaceer( sL , ",-1," , ",," );
			//
 			fprintln( sL );
			linesWritten++;
		//}
	}
		
	//----------------------------------------------------------------
	private void dumpit()
	//----------------------------------------------------------------
	{
	  if( tbldmp == null  ) return;
	  // calc
	  int total = 0;
	  int match = 0;	
	  for(int i=0;i<tbldmp.size();i++)
	  {
		 csv x = tbldmp.get(i);
		 if( x.TabName1.compareToIgnoreCase(NOP) != 0) {
			 x.NameMatch = (x.TabName1.compareToIgnoreCase(x.TabName2)==0) ? 1 : 0;
		 }
		 if( x.ColName1.compareToIgnoreCase(NOP) != 0) {
			 x.NameMatch = (x.ColName1.compareToIgnoreCase(x.ColName2)==0) ? 1 : 0;
		 }
		 if( x.InfaType1.compareToIgnoreCase(NOP) != 0) {
			 x.TypeMatch = (x.InfaType1.compareToIgnoreCase(x.InfaType2)==0) ? 1 : 0;
		 }
		 if( x.Prec1 >= 0 ) {
			 x.SizeMatch = ( (x.Prec1 == x.Prec2) && (x.Scale1 == x.Scale2) )  ? 1 : 0;
		 }
		 else {   // -1 n -1 is valid in some cae e.g. INTEGER, DATE, etc
			 if( (x.ColName1.compareToIgnoreCase(NOP) != 0) && (x.ColName2.compareToIgnoreCase(NOP) != 0) ) {
				 x.SizeMatch = ( (x.Prec1 == x.Prec2) && (x.Scale1 == x.Scale2) )  ? 1 : 0;	 
			 }
		 }
		 int tel = x.NameMatch + x.TypeMatch + x.SizeMatch;
		 match  += tel;
		 total  += 3;
		 x.matchPerc = (100 * (tel)) / 3;
		 x.Match = (tel == 3) ? 1 : 0;
      }
	  double pp = (double)match / (double)total;
	  //
	  for(int i=0;i<tbldmp.size();i++)
	  {
		  if ( i == 0) tbldmp.get(i).matchPerc = (int)(pp * 100);
		  csv x = tbldmp.get(i);
		  write( x ); 
	  }
	  // clear
	  int ncount = tbldmp.size();
	  for(int i=0;i<ncount;i++)
	  {
		  tbldmp.remove(0);
	  }
	}
	
	
	//----------------------------------------------------------------
	private String scrub (String sIn , int idx )
	//----------------------------------------------------------------
	{
	  String sRet = sIn;
	  String scl = null;
	  if( idx == 1 ) scl = scrubList1; else scl = scrubList2;
	  if( scl == null ) return sRet;
	  if( scl.length() == 0 ) return sRet;
	  //
	  StringTokenizer st = new StringTokenizer( scl , ",");
	  while( st.hasMoreTokens() )
	  {
		  String sTok = st.nextToken();
		  sRet = xMSet.xU.Remplaceer( sRet , sTok , "" );
	  }
	  return sRet;
	}
	
	//----------------------------------------------------------------
	private boolean compareList( ArrayList<infaSource> list1 , ArrayList<infaSource> list2 , boolean firstpass )
	//----------------------------------------------------------------
	{
		infaSource s1 = null;
		infaSource s2 = null;
		for(int i=0;i<list1.size();i++)
		{
			dumpit();
			s1 = list1.get(i);
			int found = 0 ;
			String sFirst = scrub( s1.Name , 1 );
			for(int k=0;k<list2.size();k++)
			{
				String sScnd = scrub( list2.get(k).Name , 2 );
				if( sFirst.compareToIgnoreCase(sScnd) == 0 ) {
					s2 = list2.get(k);
					if( firstpass ) compare( s1 , s2 );     // hiermee vermijden om nogmaals de velden te tonen
					found++;  // niet stoppen er kan nog een match zijn
				}
			}
			if( found > 0 ) continue;
			// not found
			csv x = new csv();
			x.Owner1  = s1.OwnerName;
			x.TabName1 = (s1.Name == null) ? "" : s1.Name;
	        tbldmp.add(x);	
		}
		dumpit();
		return true;
	}
	
	//----------------------------------------------------------------
	private void compare ( infaSource s1 ,  infaSource s2 )
	//----------------------------------------------------------------
	{
		// table match
		csv x = new csv();
		x.Owner1   = s1.OwnerName;
		x.TabName1 = s1.Name;
		x.Owner2   = s2.OwnerName;
		x.TabName2 = s2.Name;
		tbldmp.add(x);
		// first ddl versus second
		for(int i=0;i< s1.fieldList.size();i++)
		{
			infaSourceField f = s1.fieldList.get(i);
			csv y = new csv();
			y.ColName1  = f.Name;
			y.Type1     = f.DataType;
			y.InfaType1 = getInfaType( rdbmsTipe1 , f.DataType , f.Precision , f.scale);
			y.Prec1     = f.Precision;
			y.Scale1    = f.scale;
			//
			infaSourceField g = getEquivField( f.Name , s2.fieldList  );
			if( g != null ) {
				y.ColName2  = g.Name;
				y.Type2     = g.DataType;
				y.InfaType2 = getInfaType( rdbmsTipe2 , g.DataType , g.Precision , g.scale );
				y.Prec2     = g.Precision;
				y.Scale2    = g.scale;
			}
			//
			tbldmp.add(y);
		}
		// second versus first in order to find missing cols
		for(int i=0;i< s2.fieldList.size();i++)
		{
			infaSourceField g = s2.fieldList.get(i);
			// already present ?
			infaSourceField f = getEquivField( g.Name , s1.fieldList  );
			if( f != null ) continue;   // match, so must already be on tbldmp list
			csv y = new csv();
			y.ColName2  = g.Name;
			y.Type2     = g.DataType;
			y.InfaType2 = getInfaType( rdbmsTipe2 , g.DataType , g.Precision , g.scale );
			y.Prec2     = g.Precision;
			y.Scale2    = g.scale;
			//
			tbldmp.add(y);
		}	
	}
	
	//----------------------------------------------------------------
	private infaSourceField getEquivField ( String colname , ArrayList<infaSourceField> list)
	//----------------------------------------------------------------
	{
		for(int i=0;i<list.size();i++)
		{
			if( list.get(i).Name.trim().compareToIgnoreCase(colname) == 0) return list.get(i);
		}
		return null;
	}
	
	//----------------------------------------------------------------
	private String getInfaType( rdbmsDatatype.DBMAKE tipe , String datatype , int prec , int scale)
	//----------------------------------------------------------------
	{
	//logit(5,datatype);
		String sinfa = conv.getEquivalentDataType(datatype, prec, scale, tipe, rdbmsDatatype.DBMAKE.POWERCENTER );
		if( sinfa == null ) return NOP;
		return sinfa;
	}
	
	
	private void fprintln(String sIn)
	{
		if( fout != null ) fout.println(sIn);
		//
		lineCounter++;
		int width = xMSet.xU.TelDelims(sIn, ',');
		String aLine[] = new String[width];
		xlsxWriterMark2.XCOLOR[] aColor = new xlsxWriterMark2.XCOLOR[width];
		for(int k=0;k<width;k++) {
			aLine[k] = "";
			aColor[k] = xlsxWriterMark2.XCOLOR.NONE;
		}
		//
		int tabnametel=0;
		int colnametel=0;
		for(int j=0;j<width;j++)
		{
		  String ss = xMSet.xU.GetVeld(sIn, (j+1) , ',' ).trim();
		  aLine[j] = ss;
		  
		  switch( j)
		  {
		  case 2 : { if( ss.trim().length() > 1 ) tabnametel++; break; }
		  case 3 : { if( ss.trim().length() > 1 ) colnametel++; break; }
		  case 9 : { if( ss.trim().length() > 1 ) colnametel++; break; }
		  case 10 : { if( ss.trim().length() > 1 ) colnametel++; break; }
		  }
		}  
		// first line
		if(lineCounter == 1) {
			xcw.addSheet("Compare");
			for(int j=0;j<width;j++)
			{
				aColor[j] =xlsxWriterMark2.XCOLOR.BLUE;
			}
		}
		else
		if( tabnametel !=0 ) {
			for(int j=0;j<width;j++)
			{
				aColor[j] =xlsxWriterMark2.XCOLOR.GREEN;
			}
		}
		else
		if( colnametel == 1) {
			for(int j=0;j<width;j++)
			{
			    if( (j != 3) && (j !=10) ) continue;	
				aColor[j] =xlsxWriterMark2.XCOLOR.RED;
			}
		}
		if( writeExcelLine( "Compare" , aLine , aColor) == false ) return;
	}
	
	//---------------------------------------------------------------------------------
	private boolean do_documentcontrol()
	//---------------------------------------------------------------------------------
	{
				String TabSheetName = "DocControl";
				xcw.addSheet(TabSheetName);
			
				// Document information
				for(int i=0;i<10;i++)
				{
					String[] paar = new String[2];
					paar[0]=paar[1]=null;
					switch( i )
					{
					case 0 : { paar[0] = ""; paar[1] = "Utility to compare DDL scripts"; break; }
					case 1 : { paar[0] = "Application"; paar[1] = xMSet.getApplicationId(); break; }
					case 2 : { paar[0] = "Created"; paar[1] = ""+xMSet.xU.prntStandardDateTime(System.currentTimeMillis()); break; }
					case 3 : { paar[0] = "First Excel"; paar[1] = firstFile; break; }
					case 4 : { paar[0] = "Second Excel"; paar[1] = secondFile; break; }
					case 5 : { paar[0] = ""; paar[1] = ""; break; }
					default : break;
					}
					if( paar[0] == null ) continue;
					if( paar[1] == null ) paar[1] = "";
					if( writeExcelLine( TabSheetName , paar ) == false ) return false;
				}
				
			
				return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean dumpExcel(String ExcelOutName)
	//---------------------------------------------------------------------------------
	{
			return xcw.dumpToExcel(ExcelOutName);
	}
		
	//---------------------------------------------------------------------------------
	private boolean writeExcelLine( String TabSheetName , String[] sLineIn )
	//---------------------------------------------------------------------------------
	{
			Object[] oi = new Object[sLineIn.length];
			for(int k=0;k<sLineIn.length;k++) oi[k]=sLineIn[k];
			return xcw.addRow( TabSheetName , oi);
	}
		
	//---------------------------------------------------------------------------------
	private boolean writeExcelLine( String TabSheetName , String[] sLineIn , xlsxWriterMark2.XCOLOR[] aColorIn)
	//---------------------------------------------------------------------------------
	{
				Object[] oi = new Object[sLineIn.length];
				Object[] cc = new Object[sLineIn.length];
				for(int k=0;k<sLineIn.length;k++) {
					oi[k]=sLineIn[k];
					cc[k]=aColorIn[k];
				}
				return xcw.addRow( TabSheetName , oi , cc );
	}
}
