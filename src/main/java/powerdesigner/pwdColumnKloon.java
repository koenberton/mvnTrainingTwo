package powerdesigner;

public class pwdColumnKloon {

	pwdColumnKloon()
	{
	}
	
	public boolean dupliceer( pwdColumn from , pwdColumn to )
	{
		if( from == null ) return false;
		if( to == null ) return false;
	    //	
		to.Table						= from.Table;
		to.Name							= from.Name;
		to.DataType						= from.DataType;
		to.Code							= from.Code;
		to.Comment						= from.Comment;
		to.Length						= from.Length;
		to.Precision					= from.Precision;
		to.IsPrimary					= from.IsPrimary;
		to.IsMandatory					= from.IsMandatory;
		to.ColumnSequence				= from.ColumnSequence;
		to.BusinessLongDesc				= from.BusinessLongDesc;
		to.BusinessShortDesc			= from.BusinessShortDesc;
		to.ComposedKeyDefinition		= from.ComposedKeyDefinition;
		to.IsNewColumnIndicator_SST		= from.IsNewColumnIndicator_SST;
		to.IsNewColumnIndicator_DVST	= from.IsNewColumnIndicator_DVST;
		to.VarcharConvertedDataType		= from.VarcharConvertedDataType;
		to.SourceFilePosition			= from.SourceFilePosition;
		//to.SourceFileName				= from.SourceFileName;
		to.EDWCheckSum					= from.EDWCheckSum;
        // 
		to.ReferencedTable				= from.ReferencedTable;
		to.IsSourceMandatory  			= from.IsSourceMandatory;
		to.SourceDataType 				= from.SourceDataType;
		to.CR 							= from.CR;
		to.CR_Hist 						= from.CR_Hist;
		//
		to.SourceColumnName 			= from.SourceColumnName ;
		to.positionStart 				= from.positionStart;
		to.positionStop 				= from.positionStop;
		//
		
		return true;
	}
	
}

