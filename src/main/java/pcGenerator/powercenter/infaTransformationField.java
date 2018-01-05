package pcGenerator.powercenter;

public class infaTransformationField {
	
	public String Name=null;
	public String Datatype=null;
	public String DefaultValue="";
	public String Description="";
	public String PictureText="";
	public String Porttype=null;
	public int Precision = -1;
	public int Scale=-1;
	public String Expression=null;
	public String ExpressionType=null;
	public long InstructionUID=-1;
	

	public infaTransformationField(String in)
	{
		Name = in;
	}
	
}
