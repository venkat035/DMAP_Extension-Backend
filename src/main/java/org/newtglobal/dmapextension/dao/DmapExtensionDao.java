package org.newtglobal.dmapextension.dao;

import java.util.Map;
import java.util.List;

public interface DmapExtensionDao {

	public Map<String, String> runQueryInPostgres(String convertedQuery);

	public List<Map<String, Object>> getAppRunIdFromDiscoveryTable(String appName);

	public List<Map<String, Object>> isConnectionStringPresent(String appName);

	public List<Map<String, Object>> getStoredConnectionStrings(String appName);

	public void updateApplicationDetails(String applicationRunId, String appName);

	public void insertDiscoveryDetails(String appName, String applicationRunId, int totalFilesScanned,
			int totalFilesWithQueries, String DateWithTime);
	
	public void updateDiscoveryDetails(String appName, int totalFilesScanned, int totalFilesWithQueries);

	public void insertFileDetails(String appName, String applicationRunId, String fileNameWithQueries, String Schema,
			String DateWithTime);

	public void insertErrorDetails(String applicationRunId, String fileName, String variableLineNumber,
			String oracleQueryWithoutSpaces, String solutionLink, String DateWithTime);

	public void insertProcedureCallDetails(String applicationRunId, String fileName, String variableLineNumber,
			String procedureCall, String DateWithTime);

}
