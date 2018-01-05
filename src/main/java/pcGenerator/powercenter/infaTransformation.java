package pcGenerator.powercenter;

import java.util.ArrayList;

public class infaTransformation {
	
	public String Name         = null;
	public String Description  = null;
	public String Dbdname      = null;
	public String Assoc        = null;
	public String InstanceName = null;
	public int    stage        = -1;
	public infaInstance.TRANSFORMATION_TYPE TransformationType = infaInstance.TRANSFORMATION_TYPE.UNKNOWN;
	public ArrayList<infaTransformationField> txnfld_list = null; 
	public ArrayList<infaPair> AttrList = null;

	public infaTransformation(String sIn , infaInstance.TRANSFORMATION_TYPE tipe , String idb)
	{
		Name = sIn;
		TransformationType = tipe;
		Dbdname = idb;
		Assoc = null;
		InstanceName = null;
		stage = -1;
		txnfld_list = new ArrayList<infaTransformationField>();
		AttrList = new ArrayList<infaPair>();
	}
}
