package powerdesigner;

public class pwdColumn {   // CAUTION - ook altijd pwdColumnKloon aanpassen
	
	String Table;
	String Name;
	String DataType;
	String Code;
	String Comment;
	int Length;
	int Precision;
	boolean IsPrimary;
	boolean IsMandatory;
	int ColumnSequence;
	String BusinessLongDesc;
	String BusinessShortDesc;
	String ComposedKeyDefinition;
	boolean IsNewColumnIndicator_SST;
	boolean IsNewColumnIndicator_DVST;
	String VarcharConvertedDataType;
	String SourceFilePosition;
	String ReferencedTable;
	boolean EDWCheckSum;
	boolean IsSourceMandatory;
	String SourceDataType;
    String CR;
    String CR_Hist;
    String SourceColumnName;
    int positionStart;
    int positionStop;
    
	public pwdColumn()
	{
		Table=null;
		Name=null;
		DataType=null;
		Code=null;
		Comment=null;
		Length=-1;
		Precision=-1;
		IsPrimary=false;
		IsMandatory=false;
		ColumnSequence=-1;
		BusinessLongDesc=null;
		BusinessShortDesc=null;
		ComposedKeyDefinition=null;
		IsNewColumnIndicator_SST=false;
		IsNewColumnIndicator_DVST=false;
		VarcharConvertedDataType=null;
		SourceFilePosition=null;
		EDWCheckSum=false;
		//
		ReferencedTable=null;
		IsSourceMandatory=false;
		SourceDataType=null;
		CR=null;
		CR_Hist=null;
		SourceColumnName=null;
		positionStart=-1;
		positionStop=-1;
	}
}
