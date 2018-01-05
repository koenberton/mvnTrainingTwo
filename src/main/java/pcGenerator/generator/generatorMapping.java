package pcGenerator.generator;

import java.util.ArrayList;

public class generatorMapping {

	int sourceTableIdx = -1;
	String sourceTableName = null;
	String sourceField = null;
	//
	int targetTableIdx = -1;
	String targetTableName = null;
	String targetField = null;
	//
	int rank           = 1;
	ArrayList<generatorInstruction> instr_list = null;
	boolean canberemoved = false;
	int lineno=0;
	
	public generatorMapping(int si , String sSrcT , String sSrcF , int ti , String sTgtT , String sTgtF)
	{
		sourceTableName = sSrcT;
		sourceField     = sSrcF;
		sourceTableIdx  = si; 
		targetTableName = sTgtT;
		targetField     = sTgtF;
		targetTableIdx  = ti;
		rank            = -9;
		lineno          = 0;
		instr_list = new ArrayList<generatorInstruction>();
	}
}
