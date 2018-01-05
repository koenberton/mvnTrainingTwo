package pcGenerator.generator;

import pcGenerator.ddl.rdbmsDatatype;


public class generatorInstruction {
    
	
	//
	long UID=-1L;
	generatorConstants.SELFTYPE selfTipe = null;
	generatorConstants.PORTTYPE fromPortTipe = null;
	generatorConstants.PORTTYPE toPortTipe = null;
	generatorConstants.INSTRUCTION_TYPE tipe = null;
	int stage=-1;
	String rVal = null;
	//
	String Datatype;
	rdbmsDatatype.DBMAKE dbmake = null;
	int Precision;
	int Scale;
	int Length;
	int PhysicalLength;
	
	
	generatorInstruction(long iUID , generatorConstants.SELFTYPE  isf)
	{
		UID            = iUID;
		selfTipe       = isf;
		fromPortTipe   = generatorConstants.PORTTYPE.UNKNOWN;
		toPortTipe     = generatorConstants.PORTTYPE.UNKNOWN;
		tipe           = generatorConstants.INSTRUCTION_TYPE.UNKNOWN;
		stage          = -1;
		rVal           = null;
		//
		Datatype       = null;
		dbmake         = null;
		Precision      = -1;
		Scale          = -1;
		Length         = -1;
		PhysicalLength = -1;
	}
	
}
