package powerdesigner;

public class pwdOverrule {

	boolean isSource = false;
	String TableName;
	String ColumnName;
	String leftSide;
	String rightSide;
	String Construct;
	
	public pwdOverrule(boolean is)
	{
		isSource = is;
		TableName=null;
		ColumnName=null;
		leftSide=null;
		rightSide=null;
		Construct=null;
	}
}
