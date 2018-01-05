package office;

import generalpurpose.gpPrintStream;
import generalpurpose.pcDevBoosterSettings;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// based on
// http://howtodoinjava.com/2013/06/19/readingwriting-excel-files-in-java-poi-tutorial/

public class xlsxToTxt {
	
	    pcDevBoosterSettings xMSet=null;
		gpPrintStream fout = null;
		private int outctr=0;
		private int intctr=0;

		
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
		
		//---------------------------------------------------------------------------------
		public xlsxToTxt(pcDevBoosterSettings iM)
		//---------------------------------------------------------------------------------
		{
	    		xMSet = iM;
		}
		
		//
		//---------------------------------------------------------------------------------
		private void write(String sIn)
		//---------------------------------------------------------------------------------
		{
			//System.out.print(sIn);
			fout.print(sIn);
			outctr++;
		}
		//
		//---------------------------------------------------------------------------------
		private void writeln(String sIn)
		//---------------------------------------------------------------------------------
		{
			write(sIn + "\n");
		}
	    //---------------------------------------------------------------------------------
		public static boolean IsFile( String sIn )
		//---------------------------------------------------------------------------------
		{
					if( sIn == null ) return false;
					try {
					 File fObj = new File(sIn);
					 if ( fObj.exists() == true )
					 {
						if ( fObj.isFile() == true ) return true;
					 } 
					 return false;
					} catch ( Exception e ) {
						e.printStackTrace();
						return false;
					}
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
	    		writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	    		writeln("<Excel>");
	    		writeln("<!-- " + FInName + " -->");
	    	    //  
	            //Create Workbook instance holding reference to .xlsx file
	            XSSFWorkbook workbook = new XSSFWorkbook(file);
	 
	            // KB
	            int nsheets = workbook.getNumberOfSheets();
	            for(int idx=0;idx<nsheets;idx++)
	            {
	         
	            writeln("<WorkSheet>");
	            writeln("<WorkSheetIndex>" + idx + "</WorkSheetIndex>");
	            writeln("<WorkSheetName>" + workbook.getSheetAt(idx).getSheetName() + "</WorkSheetName>");
	            
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
	                writeln("<Row>");
	                writeln("<RowIndex>" + rowIdx + "</RowIndex>");
	                writeln("<RowNumber>" + row.getRowNum() + "</RowNumber>");
	                sRow="[" + idx + "," + row.getRowNum() + "]";
	                //
	                //For each row, iterate through all the columns
	                Iterator<Cell> cellIterator = row.cellIterator();
	                while (cellIterator.hasNext())
	                {
	                    Cell cell = cellIterator.next();
	                    write("<Column>");
	                    write("<ColumnNumber>" + cell.getColumnIndex() + "</ColumnNumber>");
	                    sRow += "[" + cell.getColumnIndex() + ",";
	                    //Check the cell type and format accordingly
	                    switch (cell.getCellType())
	                    {
	                        case Cell.CELL_TYPE_NUMERIC:
	                        	write("<Value>" + cell.getNumericCellValue() + "</Value>");
	                        	sRow += "" + cell.getNumericCellValue() + "]";
	                            break;
	                        case Cell.CELL_TYPE_STRING:
	                         	write("<Value>" + cell.getStringCellValue() + "</Value>");
	                         	sRow += "" + cell.getStringCellValue() + "]";
	                            break;
	                        case Cell.CELL_TYPE_BOOLEAN:
	                         	write("<Value>" + cell.getBooleanCellValue() + "</Value>");
	                        	sRow += "" + cell.getBooleanCellValue() + "]";
	                            break;
	                        case Cell.CELL_TYPE_FORMULA:
	                        	write("<Value>" + cell.getCellFormula() + "</Value>");
	                        	sRow += "" + cell.getCellFormula() + "]";
	                            break;
	                        case Cell.CELL_TYPE_BLANK:
	                        	write("<Value>" + "" + "</Value>");
	                        	sRow += "]";
	                            break;
	                        default : 
	                        	write("<Value>" + cell.getStringCellValue() + "</Value>");
	                        	break;  
	                    }
	                    writeln("</Column>");
	                }
	                writeln("<RowDump>" + sRow + "</RowDump>");
	                writeln("</Row>");
	            }
	            
	            writeln("</WorkSheet>");
	            }
	            file.close();
	            workbook.close();
	            
	            writeln("</Excel>");
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
