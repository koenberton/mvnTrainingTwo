package pcGenerator.powercenter;

import java.util.ArrayList;

public class infaFlatFile {

	public String CodePage = "";
	public String Consecdelimiterasone="";
	public String Delimited = "";
	public String Delimiters = "";
	public String EscapeCharacter="";
	public String Keepescapechar="";
	public String LineSequential="";
	public String Multidelimitersasand="";
	public String NullCharType="";
	public String Nullcharacter="";
	public String Padbytes="";
	public String QuoteCharacter="";
	public String Repeatable="";
	public String RowDelimiter="";
	public String ShiftSensitiveData="";
	public String Skiprows="";
	public String Striptrailingblanks="";
	//
	public boolean isFixedWidth = false;
	public ArrayList<infa2DCoordinate> extTablePositionList = null;
	
	// MAKE SURE TO UPDATE KLOON function 
	infaFlatFile()
	{
		isFixedWidth = false;
		extTablePositionList = new ArrayList<infa2DCoordinate>();
	}
	
	
	
	
	
}
