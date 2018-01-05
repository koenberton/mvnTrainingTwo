package generalpurpose;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class gpAppendStream {

	boolean AppendFileIsOpen=false;
	String AppendFileName=null;
	private PrintStream AppendFile=null;
	
	//
	//---------------------------------------------------------------------------------
	public gpAppendStream(String FNm )
	//---------------------------------------------------------------------------------
	{
		AppendFileName = FNm;
		OpenAppendFile();
	}
	//
	//---------------------------------------------------------------------------------
	private void OpenAppendFile()
	//---------------------------------------------------------------------------------
	{
		try{
			AppendFile=new PrintStream(new FileOutputStream(AppendFileName,true));
			AppendFileIsOpen=true;
		}catch(Exception e){
			e.printStackTrace(System.err);
		}
	}
	//
	//---------------------------------------------------------------------------------
	public void AppendIt(String sIn)
	//---------------------------------------------------------------------------------
	{
			if( AppendFileIsOpen ) AppendFile.println(sIn);
	}
	//
	//---------------------------------------------------------------------------------
	public void CloseAppendFile()
	//---------------------------------------------------------------------------------
	{
		if( AppendFileIsOpen == false ) return;
		if( AppendFile != null ) AppendFile.close();
		System.out.println("Closed appendfiles [" + AppendFileName + "]" );
	}
	
}
