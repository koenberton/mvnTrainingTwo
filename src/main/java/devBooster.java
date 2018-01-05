import generalpurpose.pcDevBoosterSettings;
import pcGenerator.devBoosterController;
import pcGenerator.devBoosterRobot;

/*
 *   JUN V01 : Initial version - Global Substitute
 *   JUL V02 : MAP version  (Mark2)
 *   JUL V03 : PK/CRC and Multi table version
 *   AUG V04 : support for FKs 
 * 01AUG V05 : (generatorMappingMark2.makeports) length and datatype of key/crc/fk target column is recuperated from corresponding column in target table
 * 25AUG V06 : (db2Tokenize.lookahead) added NATIONAL VARYING CHARACTER and NATIONAL CHARACTER  
 *             (generatorMappingMark2.makeports) fixed bug on key/crc/fk target DataType was uppercase whereas this must be DataTypeDisplay
 *             (generatorMapping.firstpass) overrule of the mappingname if specified in MAP file
 *             (generatorMapSpecReader.?) support for MAPPINGNAME instruction
 * 31AUG V07 : (generatorMapspecReader.?) support for SQLOVERRIDE instruction
 * 10SEP V08 : removed SQL override and added SOURCEOPTION/TARGETOPTION and SOURCEQUALIFIEROPTION 
 * 14SEP V09 : added package PowerDesigner 
 * 22OCT V10 : added Data Quality functionality  (eg. DQTIMESTAMP, TSTQUAL, etc)
 * 26OCT V11 : changes for ePurch project (DDL parser mainly)
 * 05NOV V12 : changes for modified PowerDesigner export 
 * 16NOV V14 : MDHUB changes 
 * 25NOV V15 : DVST Native version / additional swithces on translate ddl
 * 01DEC V16 : UCHP Native and automated
 * 14DEC V17 : bugfix issue (no new features) : FIELDOPTION=0 on FlatFile, RDBMS option on IMP-DLL
 * 05JAN V18 : update TYPEOFCONCERN=00000001   COMPANYNO=00000003   SOURCEKEY=2 /  claimtraveldebit CONCAT function / overrule file / DQNUMBER / COMP-EXCEL
 * 26JAN V19 : DATAMART version (recuperation of the former DQ version-V10 ) - created CSSDSSNULL function / DDL2PWD module / SAPFM timestamps
 * 01APR V19 : added GLAD to PwdReadXML
 * 28APR V20 : added IMP-INFA-SRCTGT as a tasklist command (upon request Lestjek)
 * 09MAY V21 : MDHUB CR2/3 : fixed the delimiter issue / added EDWNULL switch
 * 07JUL V22 : Added the MINUSONE generator
 * 03NOV V23 : View switcher
 * 22NOV V24 : Added detail anomaly report on the nzviewer / fixed bug on round brackets inside quotes / dbtokenize ADD CONSTRAINT
 *           : readPwDXml - Native plus
 */

public class devBooster {

	
	static pcDevBoosterSettings xMSet = null;
	static devBoosterController xCtrl = null;
	
	public static void main(String[] args) {
		xMSet = new pcDevBoosterSettings(args);
		if( xMSet.getValid() ) {
			xCtrl = new devBoosterController(xMSet);

			devBoosterRobot rbt = new devBoosterRobot(xMSet);
			rbt.processTaskList( xMSet.getTaskListName() );
				
			xCtrl.close();
		}
	}
	
}
