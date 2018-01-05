package pcGenerator;

import java.util.ArrayList;

import generalpurpose.pcDevBoosterSettings;

public class devBoosterClean {

    pcDevBoosterSettings xMSet = null;
    
	public devBoosterClean(pcDevBoosterSettings xi)
	{
		xMSet = xi;
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
	public boolean CleanIt()
	//----------------------------------------------------------------
	{
		logit(5,"Purging [" + xMSet.getProjectDir() + "]" );
		if( cleanSourcesDir() == false ) return false;
		if( cleanTargetDir() == false ) return false;
		if( cleanExportDir() == false ) return false;
		if( cleanLookupDir() == false ) return false;
		if( cleanDDLXMLDir() == false ) return false;
		if( cleanDDLDir() == false ) return false;
		
		return true;
	}
	//----------------------------------------------------------------
	private boolean cleanSourcesDir()
	//----------------------------------------------------------------
	{
		return removeAllXMLFrom( xMSet.getProjectDir()  + xMSet.xU.ctSlash + "Sources" );
	}
	//----------------------------------------------------------------
	private boolean cleanTargetDir()
	//----------------------------------------------------------------
	{
		return removeAllXMLFrom( xMSet.getProjectDir()  + xMSet.xU.ctSlash + "Targets" );
	}
	//----------------------------------------------------------------
	private boolean cleanExportDir()
	//----------------------------------------------------------------
	{
		return removeAllXMLFrom( xMSet.getProjectDir()  + xMSet.xU.ctSlash + "Export" );
	}
	//----------------------------------------------------------------
	private boolean cleanLookupDir()
	//----------------------------------------------------------------
	{
			return removeAllXMLFrom( xMSet.getProjectDir()  + xMSet.xU.ctSlash + "Lookup" );
	}
	//----------------------------------------------------------------
	private boolean cleanDDLXMLDir()
	//----------------------------------------------------------------
	{
		return removeAllXMLFrom( xMSet.getProjectDir()  + xMSet.xU.ctSlash + "Ddl" );
	}
	//----------------------------------------------------------------
	private boolean removeAllXMLFrom(String sDir)
	//----------------------------------------------------------------
	{
		if( xMSet.xU.IsDir(sDir) == false )  {
			errit("Cannot stat directory [" + sDir + "]");
			return false;
		}
		logit(5,"Purging directory [" + sDir + "]");
		ArrayList<String> lst = xMSet.xU.GetFilesInDir( sDir , null );
		for(int i=0;i<lst.size();i++)
		{
			String sFileName = sDir + xMSet.xU.ctSlash + lst.get(i);
			if( sFileName.toUpperCase().endsWith(".XML") == false ) continue;
			if( safeDelete( sFileName ) == false ) return false;
		}
		return true;
	}
	//----------------------------------------------------------------
	private boolean safeDelete(String sF)
	//----------------------------------------------------------------
	{
		if( sF.length() < 5 )  {
			errit("Sorry too close to root dir [" + sF + "]");
			return false;
		}
		if( xMSet.xU.IsBestand( sF ) == false ) {
			errit("Cannot access file [" + sF + "]");
			return false;
		}
		logit(5,"Deleting [" + sF + "]");
		if( xMSet.xU.VerwijderBestand( sF ) == false ) return false;
		if( xMSet.xU.IsBestand( sF ) == true ) {
			errit("Coudl not remove file [" + sF + "]");
			return false;
		}
		return true;
	}
	//----------------------------------------------------------------
	private boolean cleanDDLDir()
	//----------------------------------------------------------------
	{
		    String sDir = xMSet.getProjectDir()  + xMSet.xU.ctSlash + "Ddl"; 
			if( xMSet.xU.IsDir(sDir) == false )  {
				errit("Cannot stat directory [" + sDir + "]");
				return false;
			}
			logit(5,"Purging directory [" + sDir + "] from remaining non-XML files");
			ArrayList<String> lst = xMSet.xU.GetFilesInDir( sDir , null );
			for(int i=0;i<lst.size();i++)
			{
				String sFileName = sDir + xMSet.xU.ctSlash + lst.get(i);
				String sText = xMSet.xU.ReadContentFromFile(sFileName , 1000 );
				int aantal = xMSet.xU.TelDelims( sText , '\n' );
				int found = 0;
				for(int k=1;k<aantal;k++)
				{
					String sLijn = xMSet.xU.GetVeld( sText , k , '\n' ).trim().toUpperCase();
					if( k > 5 ) break;
					//logit(5,"" + k + " " + sLijn);
					if( (k==1) && ( sLijn.startsWith("-- APPLICATION")) ) found++;
					if( (k==2) && ( sLijn.startsWith("-- SOURCE")) ) found++;
					if( (k==3) && ( sLijn.startsWith("-- FROM")) ) found++;
				}
				logit(5,"score [" + sFileName + "] [" + found + "]");
				if( found == 3 ) safeDelete( sFileName );
			}
			return true;
	}
}
