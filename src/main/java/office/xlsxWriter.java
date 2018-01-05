package office;

import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import generalpurpose.pcDevBoosterSettings;

public class xlsxWriter {
	
	 	pcDevBoosterSettings xMSet=null;
	 	
	 	public enum XCOLOR { NONE , RED , GREEN , BLUE , LIGHT_RED , LIGHT_GREEN , LIGHT_BLUE }
	 	
	 	class mySheet {
	 		String Name;
	 		int currow;
	 		ArrayList<Object[]> row;
	 		ArrayList<XCOLOR> style;
	 		mySheet()
	 		{
	 			Name=null;
	 			currow=0;
	 			row = new ArrayList<Object[]>();
	 			style = new ArrayList<XCOLOR>();
	 		}
	 	}
	 	ArrayList<mySheet> sheetlist = null;
	 
	 	XCOLOR current_color = XCOLOR.NONE;
	 	

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
		public xlsxWriter(pcDevBoosterSettings mi)
		//----------------------------------------------------------------
		{
			xMSet = mi;
            sheetlist = new ArrayList<mySheet>();
		}
	 
		//----------------------------------------------------------------
		private int getSheetIdxViaName(String sIn)
		//----------------------------------------------------------------
		{
			if( sIn == null ) return -1;
			for(int i=0 ; i< sheetlist.size(); i++)
			{
				if( sheetlist.get(i).Name.compareToIgnoreCase(sIn.trim()) ==0 ) return i;
			}
			return -1;
		}

		//----------------------------------------------------------------
		public boolean addSheet(String sName)
		//----------------------------------------------------------------
		{
			if( getSheetIdxViaName( sName ) >= 0) {
				errit("Sheet already exists");
				return false;
			}
            mySheet x = new mySheet();
            x.Name = sName.trim();
            sheetlist.add( x  );
            return true;
		}
	
		//----------------------------------------------------------------
		public boolean addRow( String sSheetName , Object[] oi)
		//----------------------------------------------------------------
		{
			try {
				int idx = getSheetIdxViaName( sSheetName ); 
				if(  idx < 0) {
					errit("Cannot find sheet [" + sSheetName + "]");
					return false;
				}
				//
				Object[] on = new Object[ oi.length ];
				for(int i=0;i<oi.length;i++)
				{
					on[i] = oi[i];
				}
				mySheet s = sheetlist.get(idx);
				s.currow++;
				s.row.add( on );
				XCOLOR cc = current_color;
				s.style.add(cc);
				return true;
			}
			catch(Exception e ) {
				errit("(addrow) " + e.getMessage() );
				return false;
			}
		}
		
		//----------------------------------------------------------------
		public void setColor( XCOLOR x)
		//----------------------------------------------------------------
		{
			current_color = x;
		}
		
		//----------------------------------------------------------------
		private CellStyle getColor(XSSFWorkbook workbook , XCOLOR co)
		//----------------------------------------------------------------
		{
			XSSFFont gfont = workbook.createFont();
			short white = IndexedColors.WHITE.getIndex();;
			short black = IndexedColors.BLACK.getIndex();
			short ic= IndexedColors.WHITE.getIndex();
			switch( co )
			{
			case RED   : { ic = IndexedColors.RED.getIndex(); gfont.setColor(white); break; }
			case GREEN : { ic = IndexedColors.GREEN.getIndex(); gfont.setColor(white); break; }
			case BLUE  : { ic = IndexedColors.BLUE.getIndex(); gfont.setColor(white); break; }
			case LIGHT_RED   : { ic = IndexedColors.ORANGE.getIndex(); gfont.setColor(white); break; }
			case LIGHT_GREEN : { ic = IndexedColors.LIGHT_GREEN.getIndex(); gfont.setColor(black); break; }
			case LIGHT_BLUE  : { ic = IndexedColors.LIGHT_BLUE.getIndex(); gfont.setColor(black); break; }
			default    : { ic = IndexedColors.WHITE.getIndex(); gfont.setColor(black); break; }
			}
			CellStyle clr = workbook.createCellStyle();
			clr.setFillBackgroundColor(ic);
			clr.setFillPattern(CellStyle.ALIGN_FILL);
			clr.setFont(gfont);
			return clr;
		}
		//----------------------------------------------------------------
		public boolean dumpToExcel(String sFileName )
		//----------------------------------------------------------------
		{
			// First try to remove the file if it exists
			if( xMSet.xU.IsBestand( sFileName ) ) {
				xMSet.xU.VerwijderBestand( sFileName );
				if( xMSet.xU.IsBestand( sFileName ) ) {
					errit("Cannot remove Excel file. It is probably open [" + sFileName + "]");
					return false;
				}
			}
			//
			
			try {
				XSSFWorkbook workbook = new XSSFWorkbook();
				//
				CellStyle red = workbook.createCellStyle();
				red.setFillBackgroundColor(IndexedColors.RED.getIndex());
				red.setFillPattern(CellStyle.ALIGN_FILL);
				XSSFFont rfont = workbook.createFont();
		        rfont.setColor(IndexedColors.WHITE.getIndex());
		        red.setFont(rfont);
                //
		        CellStyle green = workbook.createCellStyle();
				green.setFillBackgroundColor(IndexedColors.GREEN.getIndex());
				green.setFillPattern(CellStyle.ALIGN_FILL);
				XSSFFont gfont = workbook.createFont();
		        gfont.setColor(IndexedColors.WHITE.getIndex());
		        green.setFont(gfont);
		        //
		        CellStyle amber = workbook.createCellStyle();
				amber.setFillBackgroundColor(IndexedColors.MAROON.getIndex());
				amber.setFillPattern(CellStyle.ALIGN_FILL);
				XSSFFont afont = workbook.createFont();
		        afont.setColor(IndexedColors.WHITE.getIndex());
		        amber.setFont(afont);
		        //
				for(int i=0;i<sheetlist.size();i++)
            	{
            		mySheet sh = sheetlist.get(i);
            		XSSFSheet spreadsheet = workbook.createSheet( sh.Name);
                    for(int j=0; j<sh.row.size(); j++ )
            		{
                	 Row row = spreadsheet.createRow((short)j);
                	 for(int k=0 ; k<sh.row.get(j).length ; k++)
                	 {
                		  String ss =  (String)sh.row.get(j)[k];
                		  if( ss == null ) ss = "(null)";   // you need to avaoid NULL at all times; if not you wil get an error (null)
                		  // TODO check type
                		  row.createCell(k).setCellValue((String)ss);
                  		  
                		  XCOLOR cc = sh.style.get(j);
                		  if( cc != XCOLOR.NONE ) {row.getCell(k).setCellStyle(getColor(workbook,cc)); }
                		  
                  		  if( ss.toUpperCase().indexOf("ERROR") >= 0 ) {
                  		    row.getCell(k).setCellStyle(red);
                  		  }
                  		  else
                  		  if( ss.toUpperCase().indexOf("WARNING") >= 0 ) {
                  		    row.getCell(k).setCellStyle(green);
                  		  }
                  		  else
                  		  if( ss.toUpperCase().indexOf("EXCEPTION") >= 0 ) {
                   		    row.getCell(k).setCellStyle(amber);
                   		  }
                  		  else {
                  			 k=k;
                  		  }
                 	 }
                	 //
                	}
            	}
            	FileOutputStream out = new FileOutputStream(new File( sFileName ));
            	workbook.write(out);
            	out.close();
            	logit( 1 , "Excel created succesfully [" + sFileName + "]");
			}
			catch(Exception e) {
				errit("Error writing the Excel file [" + sFileName + "] " + e.getMessage());
				return false;
			}
            sheetlist = null;
            return true;
		}
		
}
