package office;

import generalpurpose.pcDevBoosterSettings;

import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
//import org.apache.poi.xssf.usermodel.XSSFFont;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;


//  STREAMING version - use this when writing large excel files
public class xlsxWriterMark2 {
	
	pcDevBoosterSettings xMSet=null;
	String CURRFONT = "Calibri";
    public enum XCOLOR { NONE , RED , GREEN , BLUE , LIGHT_RED , LIGHT_GREEN , LIGHT_BLUE , ORANGE}
 	
 	class mySheet {
 		String Name;
 		int currow;
 		ArrayList<Object[]> row;
 		ArrayList<Object[]> color;
 		mySheet()
 		{
 			Name=null;
 			currow=0;
 			row   = new ArrayList<Object[]>();
 			color = new ArrayList<Object[]>();
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
	public xlsxWriterMark2(pcDevBoosterSettings im )
	//----------------------------------------------------------------
	{
		xMSet = im;
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
	public boolean addRow( String sSheetName , Object[] oi )
	//----------------------------------------------------------------
	{
			XCOLOR[] col = new XCOLOR[oi.length];
			for(int i=0 ; i<oi.length ; i++ ) 
			{
			 col[i] = XCOLOR.NONE;
			}
			return addRow( sSheetName , oi , col );
	}
	
	//----------------------------------------------------------------
	public boolean addRow( String sSheetName , Object[] oi ,  Object[] col)
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
			Object[] cc = new Object[ oi.length ];
			for(int i=0;i<on.length;i++)
			{
				on[i] = oi[i];
				cc[i] = col[i];
			}
			mySheet s = sheetlist.get(idx);
			s.currow++;
			s.row.add( on );
			s.color.add( cc);
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
	/*
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
	*/
	
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
			long startt = System.currentTimeMillis();
			try {
				//XSSFWorkbook workbook = new XSSFWorkbook();
				SXSSFWorkbook workbook = new SXSSFWorkbook(100);
				
				// white font
				Font whitefont = workbook.createFont();
				whitefont.setFontName(CURRFONT);
				short white = IndexedColors.WHITE.getIndex();
				whitefont.setColor(white);
				//
				CellStyle red = workbook.createCellStyle();
				red.setFillBackgroundColor(IndexedColors.RED.getIndex());
				red.setFillPattern(CellStyle.ALIGN_FILL);
				red.setFont(whitefont);
				//
				CellStyle lightred = workbook.createCellStyle();
				lightred.setFillBackgroundColor(IndexedColors.PINK.getIndex());
				lightred.setFillPattern(CellStyle.ALIGN_FILL);
				lightred.setFont(whitefont);
	            //
		        CellStyle green = workbook.createCellStyle();
				green.setFillBackgroundColor(IndexedColors.GREEN.getIndex());
				green.setFillPattern(CellStyle.ALIGN_FILL);
				green.setFont(whitefont);
			    //
		        CellStyle lightgreen = workbook.createCellStyle();
				lightgreen.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.getIndex());
				lightgreen.setFillPattern(CellStyle.ALIGN_FILL);
				lightgreen.setFont(whitefont);
		        //
		        CellStyle blue = workbook.createCellStyle();
				blue.setFillBackgroundColor(IndexedColors.BLUE.getIndex());
				blue.setFillPattern(CellStyle.ALIGN_FILL);
				blue.setFont(whitefont);
				//
		        CellStyle lightblue = workbook.createCellStyle();
				lightblue.setFillBackgroundColor(IndexedColors.LIGHT_BLUE.getIndex());
				lightblue.setFillPattern(CellStyle.ALIGN_FILL);
			    //
		        CellStyle orange = workbook.createCellStyle();
				orange.setFillBackgroundColor(IndexedColors.YELLOW.getIndex());
				orange.setFillPattern(CellStyle.ALIGN_FILL);
		        //
				for(int i=0;i<sheetlist.size();i++)
	        	{
	        		mySheet sh = sheetlist.get(i);
	        		//XSSFSheet spreadsheet = workbook.createSheet( sh.Name);
	        		Sheet spreadsheet = workbook.createSheet(sh.Name);
	                for(int j=0; j<sh.row.size(); j++ )
	        		{
	            	 Row row = spreadsheet.createRow((short)j);
	            	 for(int k=0 ; k<sh.row.get(j).length ; k++)
	            	 {
	            		  String ss =  (String)sh.row.get(j)[k];
	            		  //if( ss == null ) ss = "(null)";   // you need to avoid NULL at all times; if not you wil get an error (null)
	            		  XCOLOR cc = (XCOLOR)sh.color.get(j)[k];
	            		  if( cc == null ) cc = XCOLOR.BLUE;
		            	  // Determine type
	            		  int tipe = 0;
	            		  // todo date
	            		  if( ss == null ) tipe = 0;
	            		  else
	            		  if( xMSet.xU.isBoolean(ss) ) tipe = 3;
	            		  else
	            		  if( xMSet.xU.isDouble(ss) ) tipe = 2;
	            		  else
	            		  if( xMSet.xU.isWholeNumber(ss) ) tipe = 1;
	            		  else tipe = 10;
	            		  // 
	            		  switch( tipe )
	            		  {
	            		  case 0  : break;  // BLANCK or NULL 
		            	  case 1 : { long z = xMSet.xU.NaarLong(ss);
	            		             row.createCell(k).setCellValue(z);
	            		             break;
	            		           }
	            		  case 2 : { double z = xMSet.xU.NaarDouble(ss);
     		             			 row.createCell(k).setCellValue(z);
     		             			 break;
	            		  		   }
	            		  case 3 : {  if ( ss.trim().compareToIgnoreCase("TRUE") == 0) row.createCell(k).setCellValue(true);
	            		  														  else row.createCell(k).setCellValue(false);
	            		  			  break;	}
	            		  default : { row.createCell(k).setCellValue((String)ss); break; }
	            		  }
	            		 
	              	      //
	            		  switch(cc)
	            		  { 
	            		  case RED         : {row.getCell(k).setCellStyle(red); break; }
	            		  case LIGHT_RED   : {row.getCell(k).setCellStyle(lightred); break; }
	            		  case GREEN       : {row.getCell(k).setCellStyle(green); break; }
	            		  case LIGHT_GREEN : {row.getCell(k).setCellStyle(lightgreen); break; }
	            		  case BLUE        : {row.getCell(k).setCellStyle(blue); break; }
	            		  case LIGHT_BLUE  : {row.getCell(k).setCellStyle(lightblue); break; }
	            		  case ORANGE      : {row.getCell(k).setCellStyle(orange); break; }
	            		  case NONE        : break; 
	            		  default          : break;
	            		  }
	             	 }
	            	 //
	            	}
	        	}
	        	FileOutputStream out = new FileOutputStream(new File( sFileName ));
	        	workbook.write(out);
	        	out.close();
	        	workbook.dispose();
	        	workbook.close();
	        	logit( 1 , "Excel created succesfully [" + sFileName + "] [Elpased=" + (System.currentTimeMillis() - startt) + "msec]");
			}
			catch(Exception e) {
				errit("Error writing the Excel file [" + sFileName + "] " + e.getMessage());
				return false;
			}
	        sheetlist = null;
	        return true;
	}
	
	
}
