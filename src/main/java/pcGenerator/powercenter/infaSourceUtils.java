package pcGenerator.powercenter;

import generalpurpose.pcDevBoosterSettings;

public class infaSourceUtils {
	
	pcDevBoosterSettings xMSet =null;
	
	//----------------------------------------------------------------
	public infaSourceUtils(pcDevBoosterSettings im)
	//----------------------------------------------------------------
	{
		xMSet = im;
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
	public void show(infaSource src)
	//----------------------------------------------------------------
	{
		String sLn = (src.flafle == null ) ? "[Table=" : "[Flatfile=";
		sLn += src.Name + "] [DatabaseType=" + src.Databasetype  + "] [Database=" + src.Dbdname +"]";
		logit(5,sLn);
		
		for(int j=0;j<src.fieldList.size();j++)
		{
		  infaSourceField f = src.fieldList.get(j);
		  sLn = "  [Field=" + f.Name + "] [Datatype=" + f.DataType + "] [Precision=" + f.Precision + "] [Scale=" + f.scale + "]";
		  if( src.flafle.isFixedWidth ) {
		   sLn += " [Position=(" + src.flafle.extTablePositionList.get(j).x + "," + src.flafle.extTablePositionList.get(j).y + ")]";
		  }
		  logit(5,sLn);
	 	}
			
	}
}
