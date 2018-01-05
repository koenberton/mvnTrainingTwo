package powerdesigner;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import generalpurpose.pcDevBoosterSettings;

public class pwdReadOverrule {

	
	pcDevBoosterSettings xMSet=null;
	private ArrayList<pwdGeneralOverrule>   general_list = null;
	
	
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
	public pwdReadOverrule(pcDevBoosterSettings iM)
	//----------------------------------------------------------------
	{
		xMSet = iM;
		general_list = new ArrayList<pwdGeneralOverrule>();
	}
	
	//----------------------------------------------------------------
	public ArrayList<pwdOverrule> getOverrules(String FName)
	//----------------------------------------------------------------
	{
		   if( xMSet.xU.IsBestand( FName ) == false ) {
			   errit("Cannot locate lookup file [" + FName + "]");
			   return null;
		   }
		   //
		   ArrayList<pwdOverrule> list = new ArrayList<pwdOverrule>();
		   //
		   String ENCODING = "UTF-8";
		   boolean isOK = true;
		   try {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FName),ENCODING));
	        String sLijn = null;
	        while ((sLijn=reader.readLine()) != null) {
	        	String sClean = xMSet.xU.compress_spaces(sLijn.trim());
	        	if( sClean.trim().length() == 0 ) continue;
	        	if( sClean.startsWith("--") ) continue;
	        	
	        	if(  (sClean.startsWith("T ")) || (sClean.startsWith("D ")) ) {
	        	 pwdOverrule ov = extraheer( sClean );
	        	 if( ov == null ) isOK=false;
	        	 else {
	        		list.add( ov );
	        	 }
	        	}
	        	else {
	        	 pwdGeneralOverrule ge = extractGeneral( sClean );
	        	 if( ge == null ) isOK=false; else general_list.add( ge );
	        	}
	        }
	        reader.close();
	        if( !isOK )  return null;
	        return list;
		   }
		   catch( Exception e) {
			   errit("Error opening overrule file [" + FName + "] " + e.getMessage());
			   return null;
		   }
	}
	
	//----------------------------------------------------------------
	public ArrayList<pwdGeneralOverrule> getGeneralOverrules()
	//----------------------------------------------------------------
	{
		return general_list;
	}
	
	//----------------------------------------------------------------
	private pwdGeneralOverrule extractGeneral(String sIn)
	//----------------------------------------------------------------
	{
	  try {	
		
		String sTemp = sIn.toUpperCase().trim();
		String sCompr = xMSet.xU.removeBelowIncludingSpaces( sTemp ).trim();
		if( sCompr.startsWith("TRUNCATE=")) {
			String sRest = sCompr.substring(sCompr.indexOf("=")+1);
			String sval = "FALSE";
			if( sRest.startsWith("Y") ) sval = "TRUE";
			if( sRest.startsWith("TRUE") ) sval = "TRUE";
			if( sRest.startsWith("ON") ) sval = "TRUE";
			if( sRest.startsWith("1") ) sval = "TRUE";
			pwdGeneralOverrule x = new pwdGeneralOverrule( "TRUNCATE" , sval);
			return x;
		}
		else
		if( sCompr.startsWith("DATEFORMAT=")) {
			String sRest = sCompr.substring(sCompr.indexOf("=")+1);
			pwdGeneralOverrule x = new pwdGeneralOverrule( "DATEFORMAT" , sRest.trim());
			return x;
		}	
		else
		if( sCompr.startsWith("TIMESTAMPFORMAT=")) {
			String sRest = sCompr.substring(sCompr.indexOf("=")+1);
			pwdGeneralOverrule x = new pwdGeneralOverrule( "TIMESTAMPFORMAT" , sRest.trim());
			return x;
		}
		else
		if( sCompr.startsWith("SOURCEOPTION:")) {
			String sRest = sTemp.substring(sTemp.indexOf(":")+1);
			pwdGeneralOverrule x = new pwdGeneralOverrule( "SOURCEOPTION" , sRest.trim());
			return x;
		}
		else {
			errit("Unsupported general setting [" + sIn + "]");
			return null;
		}
		
	  }
	  catch(Exception e ) {
		  errit("Unsupported general setting [" + sIn + "]");
		  return null;
	  }
	}
	
	//----------------------------------------------------------------
	private pwdOverrule extraheer(String sIn)
	//----------------------------------------------------------------
	{
		// expected syntax : {S,T} <table>.<column> left right construct
		// special {S,T} <table>.<column> N/A
	    //         {S,T} <table>.<column> N/A <column>
		
		try {
		StringTokenizer st = new StringTokenizer(sIn, " ");
		int counter=-1;
		pwdOverrule x = null;
		while(st.hasMoreTokens())
		{
			 String sElem = st.nextToken().trim();
			 counter++;
			 switch( counter )
			 {
			 case 0 : { 
				 if( sElem.compareToIgnoreCase("S") == 0 ) x = new pwdOverrule(true);
				 else
				 if( sElem.compareToIgnoreCase("T") == 0 ) x = new pwdOverrule(false);
				 else {
					 errit("overrule does not start with {S,T} [" + sIn + "] " + sElem);
					 return null;
				 }
				 break;
			 }
			 case 1 : {
				 int idx = sElem.indexOf(".");
				 if( idx < 0 ) {
					 errit("overrule second element does not appear to be <table>.<column> [" + sIn + "]");
					 return null;
				 }
				 if( idx >= sElem.length() ) {
					 errit("overrule second element does not appear to be <table>.<column> [" + sIn + "]");
					 return null;
				 }
				 x.TableName = sElem.substring( 0 , idx );
				 x.ColumnName = sElem.substring( idx + 1 );
				 break;
			 }
			 case 2 : {
				 x.leftSide = sElem;
				 break;
			 }
			 case 3 : {
				 x.rightSide = sElem;
				 break;
			 }
			 case 4 : {
				 x.Construct = sElem;
				 break;
			 }
			 default : {
				 if( counter >= 4 ) {
					x.Construct += sElem;
					break;
				 }
				 else {
				   errit("Unsupported counter [" + counter + "] for [" + sIn + "] [" + sElem + "]");
				   return null;
				 }
			 }
			 }
		}
		if( (x.ColumnName == null) || (x.TableName == null) || (x.leftSide==null)  ) {
			errit("Syntax error [" + sIn + "]");
			return null;
		}
		/*
		if( (x.rightSide == null) && ( x.leftSide.compareToIgnoreCase("N/A")!=0) ) {   // is mogelijk N/A 
			errit("Syntax error [" + sIn + "]");
			return null;
		}
		if( (x.Construct == null) && ( x.leftSide.compareToIgnoreCase("N/A")!=0) ) {   // is mogelijk N/A <kolom>
			errit("Syntax error [" + sIn + "]");
			return null;
		}
		*/
		if( x.Construct != null ) {
			if( (x.Construct.trim().length() >= 2) && ((x.Construct.trim().startsWith("{") == false)||(x.Construct.trim().endsWith("}") == false)) ) {
				errit("Syntax error - missing accollades [" + sIn + "] " + x.Construct);
				return null;
			}
		}
		return x;
		}
		catch(Exception e ) {
			errit("System error " + e.getMessage() + " " + sIn );
			return null;
		}
	}
	
}
