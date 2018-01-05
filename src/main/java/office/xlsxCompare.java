package office;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

public class xlsxCompare {
	
	private final int MAXCOLS = 300;
	private final int MAXSHEETS = 50;
	
	enum MATCHTYPE { IGNORE , FULL_MATCH , IGNORE_CASE_MATCH , PARTIAL_MATCH , NO_MATCH }
	
	pcDevBoosterSettings xMSet=null;
	String OriFileName1 = null;
	String OriFileName2 = null;
	gpPrintStream fout = null;
	private int intctr=0;
    private xlsxWriterMark2 xcw = null;
    boolean ExcelHasBeenCreated = false;
	
	class sheet 
	{
	 int SheetIdx=-999;
	 boolean inscope=true;
	 String SheetName = null;
	 String SheetName1st=null;
	 String SheetName2nd=null;
	 ArrayList<Integer> SortColList = new ArrayList<Integer>();
	}
	ArrayList<sheet> sheetlist = null;
	
	
	class MyCell
	{
		String sValue=null;
		MATCHTYPE tipe = MATCHTYPE.IGNORE;
	}
	class MyRow
	{
		String Key=null;
		int RowIdx=-1;
		ArrayList<MyCell> cell_list = new ArrayList<MyCell>();
	}
	
	//----------------------------------------------------------------
	private void logit(int level , String sLog)
	//----------------------------------------------------------------
	{
		xMSet.logit(level, "[" +  this.getClass().getName() + "] " + sLog);
	}
									
	//----------------------------------------------------------------
	private void errit(String sLog)
	//----------------------------------------------------------------
	{
		   logit(0,sLog);	
	}
		
	//----------------------------------------------------------------
	private boolean Usage()
	//----------------------------------------------------------------
	{
		logit(1,"Usage : COMP-XCL  <file> <file> SHEET=(a,b,c,) ORDER=(n={a,b,c,..},..)");
		return false;
	}
	
	//----------------------------------------------------------------
	public xlsxCompare(pcDevBoosterSettings xi )
	//----------------------------------------------------------------
	{
		xMSet = xi;
		sheetlist = new ArrayList<sheet>();
		for(int i=0;i<MAXSHEETS;i++)   // just create a bunch of sheets and remove the ones not in scope
		{
			sheet x = new sheet();
			x.SheetIdx = i;
			x.inscope  = false;
			sheetlist.add(x);
		}
	}

	//----------------------------------------------------------------
	public boolean do_compare(String[] args)
	//----------------------------------------------------------------
	{
		boolean isOk = true;
		if( args.length < 2 ) return Usage();
		OriFileName1 = args[0].trim();
		String FileName1 = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Import" + xMSet.xU.ctSlash + OriFileName1;
	    if( xMSet.xU.IsBestand( FileName1 ) == false ) {
	    	errit("Cannot locat file [" + FileName1 + "]");
	    	isOk = false;
	    }
	    OriFileName2 = args[1].trim();
		String FileName2 = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Import" + xMSet.xU.ctSlash + OriFileName2;
		if( xMSet.xU.IsBestand( FileName2 ) == false ) {
		    	errit("Cannot locat file [" + FileName2 + "]");
		    	isOk = false;
		}
		//
		for(int i=2;i<10;i++)
		{
			if( i >= args.length ) break;
			String cmd = args[i].trim();
			if( cmd.toUpperCase().startsWith("SHEETSCOPE") == true ) {
				if( do_sheetscope(cmd) == false ) isOk = false;
			}
			else
			if( cmd.toUpperCase().startsWith("ORDERINFO") == true ) {
				if( do_orderinfo(cmd) == false ) isOk = false;
			}
			else {
				errit("Unsupported option [" + cmd + "]");
				isOk = false;
			}
		}
		//
		if( !isOk ) return Usage();
		//
		if( removeOutOfScope() == false ) return false;
		//
		String FOutName1 = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Temp" + xMSet.xU.ctSlash + "excl_comp_01.txt";
		String FOutName2 = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Temp" + xMSet.xU.ctSlash + "excl_comp_02.txt";
		//
		if( dumpFile( FileName1 , FOutName1 ) == false ) return false;
		if( dumpFile( FileName2 , FOutName2 ) == false ) return false;
		//
		if( compare_sheets( FOutName1 , FOutName2 ) == false )  return false;
		//
		String ExcelOutName = xMSet.getProjectDir() + xMSet.xU.ctSlash + "Temp" + xMSet.xU.ctSlash + "CMP_" + OriFileName1;
				;
		if( dumpExcel(ExcelOutName) == false ) return false;
		//
		return true;
	}
	
	//  SHEETSCOPE=(    n={a,b,c} ,  etc
	//----------------------------------------------------------------
	private boolean do_orderinfo(String sin)
	//----------------------------------------------------------------
	{
		boolean isOk = true;
		String cmd = sin.substring("ORDERINFO".length()).toUpperCase();
		if( cmd.startsWith("=(") == false ) {
			errit("ORDERINFO definition [" + sin + "] does not start with =(");
			isOk = false;
		}
		if( cmd.endsWith(")") == false ) {
			errit("ORDERINFO definition [" + sin + "] does not end with )");
			isOk = false;
		}
		if( ! isOk ) return false;
		cmd = cmd.substring(2);
		cmd = cmd.substring(0,cmd.length()-1);
		cmd = xMSet.xU.Remplaceer( cmd , "}," , "}|");
		StringTokenizer st = new StringTokenizer(cmd,"|");
		while(st.hasMoreTokens()) 
		{ 
	      String sElem = st.nextToken();
	      String bck = sElem;
	      if( sElem.endsWith("}") == false ) {
	    	  errit("Element [" + sElem + "] does not end with }");
	    	  return false;
	      }
	      if( sElem.indexOf("={") < 0 ) {
	    	  errit("Element [" + sElem + "] does not comprise ={");
	    	  return false;
	      }
	      sElem = xMSet.xU.Remplaceer( sElem , "}" , "");
	      sElem = xMSet.xU.Remplaceer( sElem , "{" , "");
	      sElem = xMSet.xU.Remplaceer( sElem , "=" , ",");
	      StringTokenizer stok = new StringTokenizer(sElem,",");
	      int teller=-1;
	      int idx=-1;
	      while(stok.hasMoreTokens())
	      {
	    	  teller++;
	    	  String ss = stok.nextToken();
	    	  int six = xMSet.xU.NaarInt(ss);
	    	  if( six < 0 ) {
	    		  errit("Incorrect nummeric values on [" + sin + "] [" + bck + "]");
	    		  return false;
	    	  }
	    	  if( teller == 0 ) {
	    		  if( six >= sheetlist.size() ) {
	    	    	  errit("maximum number of sheets has been reached [" + six + "] max=" + sheetlist.size() );
	    	    	  return false;
	    	      }
	    		  idx = getSheetViaIndex( six );
	    		  if( idx < 0 ) {
	    			  errit("Cannot find sheet with index [" + six + "]");
	    			  return false;
	    		  }
	    		  continue;
	    	  }
	    	  if( six >= MAXCOLS ) {
	    		  errit("Maximum number of columns has been reached [" + six + "]  max=" + MAXCOLS);
	    		  return false;
	    	  }
	          sheetlist.get(idx).SortColList.add( six );	  
	      }
		}
		// for those sheets without a sort kol, set the sort on the first col
		for(int i=0; i<sheetlist.size() ; i++)
		{
			if( sheetlist.get(i).SortColList.size() > 0) continue;
			int k=-1;
			sheetlist.get(i).SortColList.add(k);
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean do_sheetscope(String sin)
	//----------------------------------------------------------------
	{
		boolean isOk = true;
		String cmd = sin.substring("SHEETSCOPE".length()).toUpperCase();
		if( cmd.startsWith("=(") == false ) {
			errit("SHEETSCOPE definition [" + sin + "] does not start with =(");
			isOk = false;
		}
		if( cmd.endsWith(")") == false ) {
			errit("SHEETSCOPE definition [" + sin + "] does not end with )");
			isOk = false;
		}
		if( ! isOk ) return false;
		cmd = cmd.substring(0,cmd.length()-1);
		cmd = cmd.substring(2);
		StringTokenizer st = new StringTokenizer(cmd,",");
		while(st.hasMoreTokens()) 
		{ 
	      String sElem = st.nextToken();
	      int six = xMSet.xU.NaarInt(sElem);
    	  if( six < 0 ) {
    		  errit("Incorrect nummeric values on [" + sin + "] [" + sElem + "]");
    		  return false;
    	  }
    	  if( six >= sheetlist.size() ) {
	    	  errit("maximum number of sheets has been reached [" + six + "] max=" + sheetlist.size() );
	    	  return false;
	      }
	      int idx = getSheetViaIndex( six );
	      if( idx < 0 ) {
			  errit("Cannot find sheet with index [" + six + "]");
			  return false;
		  }
	      //
	      sheetlist.get( idx ).inscope = true;
	    }
		return true;
	}
	
	//----------------------------------------------------------------
	private boolean removeOutOfScope()
	//----------------------------------------------------------------
	{
		int nn = sheetlist.size();
		for(int j=0;j<nn;j++)
		{
			for(int i=0;i<sheetlist.size();i++)
			{
				if( sheetlist.get(i).inscope == true ) continue;
				sheetlist.remove(i);
				break;
			}
		}
		if( sheetlist.size() == 0 ) {
			errit("there are no sheets defined to be in scope. exiting");
			return false;
		}
	    //	
		for(int i=0;i<sheetlist.size();i++)
		{
			logit(5,"Sheet [" + sheetlist.get(i).SheetIdx + "] Sorted on " + sheetlist.get(i).SortColList);
		}
		return true;
	}
	
	//----------------------------------------------------------------
	private int getSheetViaIndex( int idx )
	//----------------------------------------------------------------
	{
	   for(int i=0;i<sheetlist.size();i++)
	   {
		   if( sheetlist.get(i).SheetIdx == idx )  return i;
	   }
	   return -1;
	}
	
	//---------------------------------------------------------------------------------
	private boolean isSheetInScopeViaIndex(int idx)
	//---------------------------------------------------------------------------------
	{
		int ndx = getSheetViaIndex( idx );
		if( ndx < 0 ) return false;
		if( sheetlist.get(ndx).inscope == false ) return false;
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private boolean storeSheetNameViaIndex( int idx , String sTabName )
	//---------------------------------------------------------------------------------
	{
		int ndx = getSheetViaIndex( idx );
		if( ndx < 0 ) return false;
        if( sheetlist.get(ndx).SheetName == null ) {
        	sheetlist.get(ndx).SheetName = sTabName;
        	sheetlist.get(ndx).SheetName1st = sTabName;
        	return true;
        }
        sheetlist.get(ndx).SheetName2nd = sTabName;
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private ArrayList<Integer> getSortColListViaIndex(int idx)
	//---------------------------------------------------------------------------------
	{
		int ndx = getSheetViaIndex( idx );
		if( ndx < 0 ) return null;	
		return sheetlist.get(ndx).SortColList;
	}
	
	//---------------------------------------------------------------------------------
	private void writeln(String sIn)
	//---------------------------------------------------------------------------------
	{
			if( sIn == null ) return;
			fout.println(sIn);
	}
	
	//----------------------------------------------------------------
	public String setEscapeChars(String sIn)
	//----------------------------------------------------------------
	{
	    	  String sRet = "";
	    	  char[] buf = sIn.toCharArray();
		      for(int i=0;i<buf.length;i++)
		      {
		    	  if( buf[i] == (char)'<') {
		    		  sRet = sRet + "§";
		    	  }
		    	  else
		    	  if( buf[i] == (char)'>') {
			    	  sRet = sRet + "µ";
			   	  }
		    	  else
		    	  sRet = sRet + buf[i];
		      }
	    	  return sRet;
	}
	
	//---------------------------------------------------------------------------------
	private boolean dumpFile(String Fin , String Fot)
	//---------------------------------------------------------------------------------
	{
		String FInName = Fin;
		String FOutName = Fot;
		
		// does the source exists
		if( xMSet.xU.IsBestand(FInName) == false ) {
			errit("Cannot find [" + FInName + "]");
			return false;
		}
		// can the output be removed
		if( xMSet.xU.IsBestand(FOutName) == true ) {
			if( xMSet.xU.VerwijderBestand( FOutName ) == false ) {
				errit("Cannot delete [" + FOutName + "]");
				return false;
			}
			if( xMSet.xU.IsBestand(FOutName) == true ) {
				errit("Cannot delete [" + FOutName + "]");
				return false;
			}		
			logit(5,"Deleted [" + FOutName + "]");
		}
		
		//
        try
        {
            FileInputStream file = new FileInputStream(new File(FInName));
 
            // output
        	fout = new gpPrintStream( FOutName, "UTF-8");
        	//
        	writeln("-- ");
    		writeln("-- devBooster Excel Compare Dump Format");
    		writeln("-- Application : " + xMSet.getApplicationId() );
    		writeln("-- Created from: " + FInName + "]");
        	writeln("-- Created on  : " + xMSet.xU.prntStandardDateTime(System.currentTimeMillis()) );
    	    writeln("-- Created by  : " + xMSet.whoami() );
    	    writeln("--");
    	    //  
            //Create Workbook instance holding reference to .xlsx file
            XSSFWorkbook workbook = new XSSFWorkbook(file);
 
            int nsheets = workbook.getNumberOfSheets();
            for(int idx=0;idx<nsheets;idx++)
            {
            	
            String sTabName = workbook.getSheetAt(idx).getSheetName();
            sTabName = xMSet.xU.removeBelowIncludingSpaces(sTabName);
            writeln("--");
            writeln("-- SHEET-BEGIN");
            writeln("-- SheetIndex [" + (idx+1) + "]");
            writeln("-- SheetName [" + sTabName + "]");
            //
            boolean inScope = isSheetInScopeViaIndex( (idx+1) );
            if( inScope == false ) {
                logit(1,"Sheet [" + sTabName + "] not is scope - skipping");
                writeln("-- Not in scope");
                writeln("-- SHEET-END");
                continue;
            }
            // store the sheetname
            if( storeSheetNameViaIndex( (idx+1) , sTabName ) == false ) {
            	errit("Cannot store tabname [" + sTabName + "]");
            	return false;
            }
            // set the key indicator
            ArrayList<Integer> ki = getSortColListViaIndex( idx+1 );
            if( ki == null ) {
            	errit("system error I");
                continue;
            }
            if( ki.size() == 0 ) {
            	errit("No sortcol list on " + sTabName );
            	continue;
            }
            // See whether there is a sort
            boolean hasSortBeenDefined = (ki.get(0) < 0) ? false : true;
            String sMsg = hasSortBeenDefined ? "Sorted on " + ki : "Not sorted Using rownumber";
            sMsg = "Sheet [" + (idx+1) + "] : " + sMsg;
            logit( 5 , sMsg );
            writeln("-- " + sMsg);
            //
            boolean iskey[] = new boolean[MAXCOLS];
            for(int i=0;i<iskey.length;i++) iskey[i]=false;
            for(int i=0;i<ki.size();i++)
            {
            	int z = ki.get(i) - 1;
            	if( (i==0) && (ki.get(i)==-1) ) continue;
            	if( (z<0) || (z>=iskey.length) ) 
            	{
            		errit("System error - Too few entries in iskey");
            		continue;
            	}
            	iskey[ z ] = true;
            }
            
            
            //Get sheet from the workbook
            XSSFSheet sheet = workbook.getSheetAt(idx);
 
            //Iterate through each rows one by one
            int rowIdx=-1;
            String sRow="";
            String sKey="";
            int keyteller=0;
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext())
            {
            	intctr++;
                Row row = rowIterator.next();
                //
                rowIdx++;
                sRow="";
                sKey="";
                keyteller=0;
                //
                //For each row, iterate through all the columns
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext())
                {
                    Cell cell = cellIterator.next();
                    String sValue=null;
                    switch (cell.getCellType())
                    {
                        case Cell.CELL_TYPE_NUMERIC:
                        	sValue = "" + cell.getNumericCellValue();
                            break;
                        case Cell.CELL_TYPE_STRING:
                         	sValue = "" + cell.getStringCellValue();
                            break;
                        case Cell.CELL_TYPE_BOOLEAN:
                        	sValue = "" + cell.getBooleanCellValue();
                            break;
                        case Cell.CELL_TYPE_FORMULA:
                        	sValue = "" + cell.getCellFormula();
                            break;
                        case Cell.CELL_TYPE_BLANK:
                        	sValue = "";
                            break;
                        default : 
                        	break;  
                    }
                    if( sValue == null ) continue;
                    //
                    // prepare content
                    sValue = setEscapeChars(sValue);
                    //
                    sRow += "<" + cell.getColumnIndex() + "|" + sValue + ">";
                    // key
                    if( hasSortBeenDefined ) {
                     int icol = cell.getColumnIndex();
                     if( (icol<0) || (icol>=iskey.length) ) {
                    	errit("Row is to wide [" + icol + "] only support [" + iskey.length + "] cols");
                    	continue;
                     }
                     if( iskey[ icol ] == true ) {
                    	keyteller++;
                    	if( keyteller != 1 ) sKey += "|";
                    	sKey += sValue;
                     }
                    }
                    else {  // there is no key so just use the row
                    	keyteller++;
                    	if( keyteller == 1) sKey = "Row" + cell.getRowIndex();
                    }
                    //
                }
                //
                writeln( "<" + sKey + "><" + row.getRowNum() + "><" + rowIdx + ">" + sRow ); 
            }
            writeln("-- SHEET-END");
            writeln("--");
            
            }
            file.close();
            workbook.close();
            
            fout.close();
            logit(1,"Extracted (" + intctr + ") lines from [" + FInName + "] to [" + FOutName + "]");
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
	}
	
	//---------------------------------------------------------------------------------
	private boolean compare_sheets(String FFirst , String FSecond)
	//---------------------------------------------------------------------------------
	{
	    
		// loop through the sheets in scope
		// iterate through the sheets and read the sheet on the first and second file in memory
		for(int i=0;i<sheetlist.size();i++)
		{
          if( sheetlist.get(i).inscope == false ) continue;
          //
          ArrayList<MyRow> memsheet1 = fetchSheetIntoMemory( FFirst , sheetlist.get(i).SheetIdx );
          if( memsheet1 == null ) return false;
          logit(1,"Read [" + memsheet1.size() + "] rows in memory for sheet [" + i + "] of [" + FFirst + "]");
          //
          ArrayList<MyRow> memsheet2 = fetchSheetIntoMemory( FSecond , sheetlist.get(i).SheetIdx );
          if( memsheet2 == null ) return false;
          logit(1,"Read [" + memsheet2.size() + "] rows in memory for sheet [" + i + "] of [" + FSecond + "]");
          // compare
          compare_rows( memsheet1 , memsheet2 );
          create_report( 1 , i , memsheet1 );
          //
          compare_rows( memsheet2 , memsheet1 );
          create_report( 2 , i , memsheet2 );
          //
          memsheet1 = null;
          memsheet2 = null;
        }
		return true;
	}

	//---------------------------------------------------------------------------------
	private  ArrayList<MyRow> fetchSheetIntoMemory(String FName , int sheetidx)
	//---------------------------------------------------------------------------------
	{
		 if( xMSet.xU.IsBestand(FName) == false ) {
			errit("Strange file [" + FName + "] cannot be accessed");
			return null;
		 }
		 logit(5,"Fetching sheet [" + sheetidx + "] from [" + FName + "]");
		 //
		 ArrayList<MyRow> memSheet = new ArrayList<MyRow>();
		 //
		 boolean Found = false;
		 try {
             BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FName),"UTF-8"));
             String sLine = null;
             while ((sLine=reader.readLine()) != null) {
            	 sLine = sLine.trim();
            	 //-- SHEET-BEGIN
            	 //-- SheetIndex [1]
            	 if( !Found ) {
            	   if( sLine.toUpperCase().startsWith("-- SHEETINDEX") == false) continue;
            	   String sNum = xMSet.xU.justkeepthenumerics( sLine.trim() ).trim();
            	   if( sNum.length() < 1 ) continue;
            	   int si = xMSet.xU.NaarInt(sNum);
            	   if( si != sheetidx ) continue;
            	   Found = true;
            	   //logit(1,"Found " + sLine);
            	   continue;
            	 }	
            	 // in textblock
            	 // -- SHEET-END
            	 if( sLine.toUpperCase().startsWith("-- SHEET-END") ) break;
            	 if( sLine.startsWith("--")) continue;
            	 if( sLine.startsWith("<") == false ) continue;
            	 String bckLine = sLine;
            	 sLine = xMSet.xU.Remplaceer( sLine, "<", "");
            	 // beperking van tokenize, indien er gestart wordt met een token wordt pas gestart vanaf 2de veld
            	 if( sLine.startsWith(">") ) sLine = " " + sLine;
            	 StringTokenizer st = new StringTokenizer(sLine,">");
            	 int colcounter=-1;
            	 MyRow row = null;
         		 while(st.hasMoreTokens()) {
         			String sElem = st.nextToken();
         			colcounter++;
         			
         	        // new line
         			if( colcounter == 0 ) {
         				 row = new MyRow();
         				 String sKey = sElem.trim();
         				 if( sKey == null ) sKey = "";
         				 if( sKey.length() == 0 ) {
         					 errit("Empty key on [" + bckLine + "] - please look at the definitions");
         					 return null;
         				 }
         				 row.Key = sElem;
         				 for(int i=0;i<MAXCOLS;i++)
         				 {
         					 MyCell x = new MyCell();
         					 row.cell_list.add(x);
         				 }
         	 //errit(""+colcounter + "[" + sElem + "]");
         				 continue;
         			}
         			// rowidx
         			if( colcounter == 1 ) {
         				int ii = xMSet.xU.NaarInt(sElem.trim());
         				if( ii < 0 ) {
         					errit("System error VI - Not a numeric on rowindex [" + sLine + "] [item=" + sElem + "]");
         					return null;
         				}
         				row.RowIdx = ii;
        	//errit(""+colcounter + "[" + sElem + "]");
         				continue;
         			}
         			// rownum
         			if( colcounter == 2 ) {
         				int ii = xMSet.xU.NaarInt(sElem.trim());
         				if( ii < 0 ) {
         					errit("System error VII - Not a numeric on rownumber [" + sLine + "] [item=" + sElem + "]");
         					errit("["+colcounter +"] [" + sElem + "] [" + bckLine + "] --- [" + sLine);
                 			return null;
         				}
         				continue;
         			}
         			int idx = sElem.indexOf("|");
         			if( idx < 0 ) {
         				errit("System error III - missing | in [" + sElem + "]");
         				break;
         			}
         			int ndx = xMSet.xU.NaarInt(  sElem.substring(0,idx) );
         			if( ndx < 0 ) {
         				errit("System error IV-  not a numeric in [" + sElem + "]");
         				break;
         			}
         			if( ndx >= row.cell_list.size() ) {
         				errit("System error V-  too many columns [" + sElem + "]");
         				break;
         			}
         			//
         			String sVal = sElem.substring(idx+1);
         			row.cell_list.get(ndx).sValue = sVal;
         			//
         			
         		 }
         		 //
         		 memSheet.add( row );
         		 //
              }
             reader.close();
		 }
		 catch( Exception e) {
			 errit( "Reading file [" + FName + "] " + xMSet.xU.LogStackTrace(e));
			 return null;
		 }
		 return memSheet;
	}
	
	//---------------------------------------------------------------------------------
	private boolean compare_rows(ArrayList<MyRow> mem1 , ArrayList<MyRow> mem2)
	//---------------------------------------------------------------------------------
	{
		for(int i=0;i<mem1.size();i++)
		{
		   MyRow row1 = mem1.get(i);	
		   MyRow row2 = getMatchingRow( row1.Key , mem2);
		   // Rown not found
		   if( row2 == null ) {
			   //logit( 1 ,  "No match for row [" + row1.Key + "]");
			   for(int j=0;j<row1.cell_list.size();j++) row1.cell_list.get(j).tipe = MATCHTYPE.NO_MATCH;
			   continue;
		   }
		   //
		   compare_cells( row1 , row2 );
		}
		return true;
	}
	
	//---------------------------------------------------------------------------------
	private MyRow getMatchingRow( String sKey , ArrayList<MyRow> mem)
	//---------------------------------------------------------------------------------
	{
		for(int i =0;i<mem.size();i++)
		{
			if( sKey.compareToIgnoreCase( mem.get(i).Key ) == 0 ) return mem.get(i);
		}
		return null;
	}

	//---------------------------------------------------------------------------------
	private boolean compare_cells( MyRow row1 , MyRow row2)
	//---------------------------------------------------------------------------------
	{
		for(int i=0;i<row1.cell_list.size();i++)
		{
			MyCell c1 = row1.cell_list.get(i);
			MyCell c2 = row2.cell_list.get(i);
			// NULL
			if( c1.sValue == null ) {
				if( c2.sValue == null ) c1.tipe = MATCHTYPE.FULL_MATCH;
				                    else c1.tipe = MATCHTYPE.NO_MATCH;
				continue;
			}
			// Not null but other cell is NULL
			if( c2.sValue == null ) {
				 c1.tipe = MATCHTYPE.NO_MATCH;
				 continue;
			}
			// both not null
			if( c1.sValue.compareTo( c2.sValue ) == 0 ) {
				 c1.tipe = MATCHTYPE.FULL_MATCH;
				continue;
			}
			if( c1.sValue.compareToIgnoreCase( c2.sValue ) == 0 ) {
				 c1.tipe = MATCHTYPE.IGNORE_CASE_MATCH;
				continue;
			}
			String s1 = xMSet.xU.removeBelowIncludingSpaces( c1.sValue.trim().toUpperCase() );
			String s2 = xMSet.xU.removeBelowIncludingSpaces( c2.sValue.trim().toUpperCase() );
			if( s1.compareToIgnoreCase( s2 ) == 0 ) {
				c1.tipe = MATCHTYPE.PARTIAL_MATCH;
				continue;
			}
			c1.tipe = MATCHTYPE.NO_MATCH;
		}
		return true;
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
	
	//---------------------------------------------------------------------------------
	private boolean create_report(int excelfileindex , int sheetindex , ArrayList<MyRow> mem)
	//---------------------------------------------------------------------------------
	{
		if( (sheetindex < 0) || (sheetindex >= sheetlist.size()) ) {
			errit("cannot fetch sheet [" + (sheetindex+1) + "]");
			return false;
		}
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
		//
		logit(1,"Dumping sheet [" + sheetlist.get(sheetindex).SheetName + "]");
		//
		String TabSheetName = sheetlist.get(sheetindex).SheetName;
		if( TabSheetName == null ) TabSheetName = ""+sheetindex;    // nakijken soms null
		TabSheetName = xMSet.xU.removeBelowIncludingSpaces(TabSheetName);
		TabSheetName = (excelfileindex == 1) ? "A_" + TabSheetName : "B_" + TabSheetName;
		xcw.addSheet(TabSheetName);
		//
		for(int z=0;z<mem.size();z++)
		{
			MyRow row = mem.get(z);
			// determine width
			int width = -1;
			for(int j=0;j<row.cell_list.size();j++) 
			{
				if( row.cell_list.get(j).sValue != null ) width = j;
			}
			// Empty Line
			if( width == 0 ) { 
				String sLine[] = new String[1];
				sLine[0] = "";
				if( writeExcelLine( TabSheetName , sLine) == false ) return false;
				continue;
			}
			//
			String aLine[] = new String[width];
			xlsxWriterMark2.XCOLOR[] aColor = new xlsxWriterMark2.XCOLOR[width];
			for(int k=0;k<width;k++) {
				aLine[k] = "";
				aColor[k] = xlsxWriterMark2.XCOLOR.NONE;
			}
			//
			for(int j=0;j<width;j++)
			{
			  aLine[j] = (row.cell_list.get(j).sValue == null) ? "" : row.cell_list.get(j).sValue;
			  // color
			  switch( row.cell_list.get(j).tipe )
			  {
			  case NO_MATCH      : { aColor[j] =xlsxWriterMark2.XCOLOR.RED; break; }
			  case PARTIAL_MATCH : { aColor[j] =xlsxWriterMark2.XCOLOR.BLUE; break; }
			  case IGNORE_CASE_MATCH : { aColor[j] =xlsxWriterMark2.XCOLOR.GREEN; break; }
			  case FULL_MATCH : ;
			  case IGNORE     : ;
			  default         :	{ aColor[j] =xlsxWriterMark2.XCOLOR.NONE; break; }	  
			  }
			}
		    if( writeExcelLine( TabSheetName , aLine , aColor) == false ) return false;
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
			case 0 : { paar[0] = ""; paar[1] = "Utility to compare Excel files"; break; }
			case 1 : { paar[0] = "Application"; paar[1] = xMSet.getApplicationId(); break; }
			case 2 : { paar[0] = "Created"; paar[1] = ""+xMSet.xU.prntStandardDateTime(System.currentTimeMillis()); break; }
			case 3 : { paar[0] = "First Excel"; paar[1] = OriFileName1; break; }
			case 4 : { paar[0] = "Second Excel"; paar[1] = OriFileName2; break; }
			case 5 : { paar[0] = ""; paar[1] = ""; break; }
			case 6 : { paar[0] = "TabSheets"; paar[1] = ""; break; }
			default : break;
			}
			if( paar[0] == null ) continue;
			if( paar[1] == null ) paar[1] = "";
			if( writeExcelLine( TabSheetName , paar ) == false ) return false;
		}
		//
		// compare sheetnames
		for(int i=0;i<sheetlist.size();i++)
		{
		   String[] triple = new String[3];
		   triple[0] = sheetlist.get(i).SheetName1st;
		   triple[1] = sheetlist.get(i).SheetName2nd;
		   triple[2] = triple[0].compareToIgnoreCase(triple[1]) == 0 ? "" : "No match";
		   if( writeExcelLine( TabSheetName , triple ) == false ) return false;
		}
	    //	
		return true;
	}
}
