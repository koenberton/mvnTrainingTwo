package pcGenerator.powercenter;

import java.util.ArrayList;

import pcGenerator.ddl.readInfaXML;

public class infaSource {

	public readInfaXML.ParseType tipe = null; 
	public String Name;
	public String Description;
	public String Dbdname;
	public String Databasetype;
	public String OwnerName;
	public String BusinessName;
	public String ObjectVersion;
	public String VersionNumber;
	public String Constraint;
	public String TableOptions;
	public ArrayList<infaSourceField> fieldList=null;
    public infaFlatFile flafle=null;
    public ArrayList<infaPair> tableAttributeList=null;
    public ArrayList<infaConstraint> constraintList=null;
    public String SQLOverride=null;
    
	// MAKE SURE TO UPDATE KLOON function 

	public infaSource(String in, readInfaXML.ParseType it)
	{
		tipe = it;
		Name = in;
		Description="";
		Databasetype="Unknown";
		Dbdname="Unknown";
		OwnerName="Unknown";
		BusinessName="";
		ObjectVersion="1";
		VersionNumber="1";
		Constraint="";
		TableOptions="";
		fieldList = new ArrayList<infaSourceField>();
		flafle = new infaFlatFile();
		tableAttributeList = new ArrayList<infaPair>();
		constraintList = new ArrayList<infaConstraint>();
		SQLOverride=null;
	}
	
}
