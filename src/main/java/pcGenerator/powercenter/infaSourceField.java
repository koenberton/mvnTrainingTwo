package pcGenerator.powercenter;

import java.io.ObjectStreamClass;

public class infaSourceField {

	public long UID=-1L;
	public int fieldNumber;
	public String Name;
	public String DataType;
	public int Length;
	public int Precision;
	public int scale;
	public int offset;
	public int physicalLength;
	public int physicalOffset;
	public boolean mandatory;
	public String KeyType;
	public String Description;
	public String referencedTable;
	public String referencedField;
	public String isFileNameField;
	//
	public String BusinessName="";
	public String FieldType="";
	public String FieldProperty="";
	public String Hidden="";
	public int Level=-1;
	public int Occurs=-1;
	public String PictureText="";
	public String UsageFlags="";
	
	
	// MAKE SURE TO UPDATE kloon function
	
	public infaSourceField(String in, long iUID)
	{
	 	//UID             = (long)this.hashCode();
	 	//UID             = System.nanoTime();
		UID             = iUID;
        Name            = in;
		fieldNumber     = -1;
		DataType        = "UNKNOWN";
		Precision       = -1;
		scale           = -1;
		Length          = -1;
		offset          = -1;
		physicalLength  = -1;
		physicalOffset  = -1;
		mandatory       = false;  // do not modify
		KeyType         = "NOT A KEY";
		Description     = "";
		referencedTable = null;
		referencedField = null;
		isFileNameField = null;
		BusinessName    = "";
		FieldType       = "";
		FieldProperty   = "";
		Hidden          ="NO";  // default NO
		Level           = 0;    // default 0
		Occurs          = 0;    // default 0
		PictureText     = "";
		UsageFlags      = "";
	}
	
}
