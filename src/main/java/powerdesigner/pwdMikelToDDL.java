package powerdesigner;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

public class pwdMikelToDDL {
	
	pcDevBoosterSettings xMSet=null;
	gpPrintStream fout = null;
	private int outctr=0;
	private int intctr=0;
    private int posteller=0;
    private String stabname=null;
    
    
    
    //Helper application to convert Michael's Excel files comprisign Oracle Ext table defintions into plain SQL
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
	
	public pwdMikelToDDL(pcDevBoosterSettings im)
	{
		xMSet = im;
	}

	
	//
	//---------------------------------------------------------------------------------
	private void writeln(String sIn)
	//---------------------------------------------------------------------------------
	{
		if( sIn == null ) return;
		String sTemp = xMSet.xU.removeBelowIncludingSpaces(sIn).trim().toUpperCase();
		
		if( sTemp.startsWith("FLATFILENAME")) return;
		if( sTemp.startsWith("EXTERNALTABLE")) return;
		if( sTemp.startsWith("FLATFILE")) return;
		
		if( sTemp.startsWith("EXTTABLE")) return;
		if( sTemp.startsWith("FLATFILEDESC")) return;
		if( sTemp.indexOf("POSITION(") >=0 ) {
			posteller++;
			if( posteller == 1 ) fout.println( " ) ORGANIZATION EXTERNAL FIELDS ( ");
		}
		fout.println(sIn);
	}
	
	//
	//---------------------------------------------------------------------------------	
	public boolean extract( String Fin , String Fot) 
	//---------------------------------------------------------------------------------
	{
		String FInName = Fin;
		String FOutName = Fot;
		
		if( xMSet.xU.IsBestand(FInName) == false ) {
			errit("Cannot find [" + FInName + "]");
			return false;
		}
	    //	
        try
        {
            FileInputStream file = new FileInputStream(new File(FInName));
 
            // output
        	fout = new gpPrintStream( FOutName, "UTF-8");
        	//
        	writeln("-- ");
    		writeln("-- Beat Excel format to DDL script");
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
           	
            String sTemp = workbook.getSheetAt(idx).getSheetName();
            writeln("--");
            writeln("--" + sTemp );
            
            sTemp = xMSet.xU.removeBelowIncludingSpaces(sTemp);
            if( sTemp.compareToIgnoreCase("ALLFILES")==0) continue;
            
            writeln("create table beat." + sTemp + " (");
            
            posteller=0;
            
            //Get sheet from the workbook
            XSSFSheet sheet = workbook.getSheetAt(idx);
 
            //Iterate through each rows one by one
            int rowIdx=-1;
            String sRow;
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext())
            {
            	intctr++;
                Row row = rowIterator.next();
                //
                rowIdx++;
                sRow="";
                //
                //For each row, iterate through all the columns
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext())
                {
                    Cell cell = cellIterator.next();
                    switch (cell.getCellType())
                    {
                        case Cell.CELL_TYPE_NUMERIC:
                        	sRow += "" + cell.getNumericCellValue() + " ";
                            break;
                        case Cell.CELL_TYPE_STRING:
                         	sRow += "" + cell.getStringCellValue() + " ";
                            break;
                        case Cell.CELL_TYPE_BOOLEAN:
                        	sRow += "" + cell.getBooleanCellValue() + " ";
                            break;
                        case Cell.CELL_TYPE_FORMULA:
                        	sRow += "" + cell.getCellFormula() + " ";
                            break;
                        case Cell.CELL_TYPE_BLANK:
                        	sRow += " ";
                            break;
                        default : 
                        	break;  
                    }
                }
                
                sTemp = xMSet.xU.removeBelowIncludingSpaces(sRow).trim().toUpperCase();
                if( sTemp.startsWith("INDEX")) break;
                
                sRow = xMSet.xU.Remplaceer(sRow , " BYTE)" , ")" );
                writeln( sRow ); sRow = "";
            }
            writeln(");");
            writeln("--");
            
            }
            file.close();
            workbook.close();
            
            fout.close();
            logit(1,"Extracted (" + intctr + ") lines from [" + FInName + "] to [" + FOutName + "] (" + outctr + ")");
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
	}
}
