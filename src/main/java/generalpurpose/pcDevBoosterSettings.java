package generalpurpose;


public class pcDevBoosterSettings {

	public enum TRISTATE  { YES , NO , NULL }
	
	public gpUtils xU = null;
	gpLogger xLog = null;
	gpLogger errLog = null;
	
	private String APPLICATION        = "pcDevBooster";
	private String BUILD              = "29-Nov-2016";
	private String VERSION            = "V0.24";
	private String RootDir            = null;
	private boolean isValid           = false;
	private String CurrentProject     = null;
	private int LogLevel              = 10;
	private long UIDmaker             = 100L;
	private int  powermartversion     = 9;
	private boolean useNationalCharacterSet = true;
	private String TaskListName       = "taskList.txt";
	private String MyEdwNull          = "NULL";
	
	//---------------------------------------------------------------------------------
	public pcDevBoosterSettings(String[] args)
	//---------------------------------------------------------------------------------
	{
	  isValid = false;
	  xU = new gpUtils();
	  if( (args.length < 2) || (args.length>3) ) {
		  logit(0, "Usage: "+ this.getApplicationId() + " <FolderName> <Projectname> {Alt Tasklist}");
	  }
	  else {
		  if( checkDirs(args[0] , args[1]) ) {
			  xLog = new gpLogger( 9 , getProjectDir() + xU.ctSlash + "Logger.txt" , "DDMMSS" );
			  errLog = new gpLogger( 9 , getProjectDir() + xU.ctSlash + "Errors.txt" , "DDMMSS" );
			  errLog.setVerbose( false );
			  isValid = true;
			  if( args.length == 3 ) TaskListName = args[2].trim();
			  TaskListName = getProjectDir() + xU.ctSlash + TaskListName;
			  if( xU.IsBestand( TaskListName ) == false ) {
					System.err.println("Cannot find tasklist [" + TaskListName + "]");
					isValid=false;
			  }
		  }
		  logit( 1 , this.getApplicationId() );
	  }
	}
	
	//---------------------------------------------------------------------------------
	public boolean getValid()
	//---------------------------------------------------------------------------------
	{
		return isValid;
	}
	
	//---------------------------------------------------------------------------------
	public long getNextUID()
	//---------------------------------------------------------------------------------
	{
		UIDmaker += 10L;
		return UIDmaker;
	}
	
	//---------------------------------------------------------------------------------
	public void logit(int level , String sLogIn)
	//---------------------------------------------------------------------------------
	{
		if( level > LogLevel ) return;
		String sLog = sLogIn;
		sLog = ( level == 0 ) ? " E " + sLog : "   " + sLog; 
		//if( sLog.trim().startsWith("[") == false ) sLog = "[" +  this.getClass().getName() + "] " + sLog;
		if( xLog != null ) xLog.Logit(level, sLog);
		else {
		 if( level == 0 ) System.err.println(sLog);
		             else System.out.println(sLog);
		}
		if( (level == 0) && (errLog != null) ) errLog.Logit( 0 , sLogIn );
	}
	
	//---------------------------------------------------------------------------------
	public boolean checkDirs(String sDir , String sProj)
	//---------------------------------------------------------------------------------
	{
		isValid = true;
		RootDir = (sDir == null ) ? "" : sDir;
		if ( RootDir.length() == 0 )  {
			isValid = false;
			logit(0,"Please specify the root directory of " + this.getApplicationId() + " on the commandline");
			return false;
		}
		//
		if( xU.IsDir(sDir) == false ) {
			logit(0,"Cannot locate root directory [" + sDir + "]");
			isValid = false;
		}
		CurrentProject = sProj;
		//
		String sTest[] = {"Ddl","Export","Import","Sources","Targets","Temp","Templates","LookUp" };
		for(int i=0;i<sTest.length;i++)
		{
			String sTestDir = this.getProjectDir() + xU.ctSlash + sTest[i];
			if( xU.IsDir(sTestDir) == false ) {
				isValid = false;
				logit(0,"Cannot find folder [" + sTestDir + "]");
			}
		}
		//
		return isValid;
	}
	
	//---------------------------------------------------------------------------------
	public void closeall()
	//---------------------------------------------------------------------------------
	{
		if( xLog != null ) xLog.CloseLogs();
		if( errLog != null ) errLog.CloseLogs();
	}
	
	//---------------------------------------------------------------------------------
	public String getApplicationId()
	//---------------------------------------------------------------------------------
	{
		return APPLICATION + " [Version : " + VERSION + "] [Build : " + BUILD + "]";
	}
	
	//---------------------------------------------------------------------------------
	public String whoami()
	//---------------------------------------------------------------------------------
	{
		return System.getProperty("user.name").trim();
	}
	
	//---------------------------------------------------------------------------------
	public String getRootDir()
	//---------------------------------------------------------------------------------
	{
		return RootDir;
	}
	
	//---------------------------------------------------------------------------------
	public String getTaskListName()
	//---------------------------------------------------------------------------------
	{
		return TaskListName;
	}
	
	//---------------------------------------------------------------------------------
	public String getCurrentProject()
	//---------------------------------------------------------------------------------
	{
		return CurrentProject;
	}
		
	//---------------------------------------------------------------------------------
	public String getProjectDir()
	//---------------------------------------------------------------------------------
	{
		return RootDir + xU.ctSlash + getCurrentProject();
	}
	
	public void setUsenationalCharacterSet(boolean ib)
	{
		useNationalCharacterSet = ib;
		logit(1,"Use National Character Set [" + useNationalCharacterSet + "]");
	}
	public boolean getUsenationalCharacterSet()
	{
		return useNationalCharacterSet;
	}
	
	public void setpowermartversion(int i)
	{
		powermartversion = i;
		logit(5,"PowerMart version [" + powermartversion + "] set");
	}
	
	public int getpowermartversion()
	{
		return powermartversion;
	}
	
	public void setLoglevel(int i)
	{
		LogLevel = i;
		logit(5,"Loglevel set to [" + this.LogLevel + "]");
	}

	
	//----------------------------------------------------------------
	public int NetezzaSSTSizeNEW( int prec )
	//----------------------------------------------------------------
	{
				// char
				if( prec == 1 )  return 1;
				// large 
				if( (prec < 32) && (prec>1) ) return NetezzaSSTSizeOLD( prec) ;
				if( prec > 1024 ) return prec;
				//
				double pd = prec / 5;
				int pp =  prec + (int)pd;   // margin of 20%
				pp =  (( pp / 50 ) + 1) * 50;
				return pp;
	}
	//----------------------------------------------------------------
	private int NetezzaSSTSizeOLD( int prec )
	//----------------------------------------------------------------
	{
			// char
			if( prec == 1 )  return 1;
			//
			if( prec <   16 ) return 16;
			if( prec <   32 ) return 32;
			if( prec <   64 ) return 64;
			if (prec <  128 ) return 128;
			if( prec <  256 ) return 256;
			if( prec <  512 ) return 512;
			if( prec < 1024 ) return 1024;
			return prec;
	}
	
	public void setEDWNULL(String sin)
	{
		MyEdwNull = sin;
	}
	public String getEDWNULL()
	{
		return MyEdwNull;
	}
}
