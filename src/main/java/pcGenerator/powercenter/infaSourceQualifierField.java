package pcGenerator.powercenter;

public class infaSourceQualifierField {

	
	public long UID=-1L;
	public String Datatype=null;
	public String DefaultValue = null;
	public String Description =null;
	public String Name = null;
	public String PictureText = null;
	public String Porttype = null;
	public int Precision = -1;
	public int Scale = -1;
	
	public infaSourceQualifierField(String iName)
	{
		UID = System.nanoTime();
		Datatype=null;
		DefaultValue = "";
		Description = "";
		Name = iName;
		PictureText = null;
		Porttype = "INPUT/OUTPUT";
		Precision = -1;
		Scale = -1;
	}
	
	
}
