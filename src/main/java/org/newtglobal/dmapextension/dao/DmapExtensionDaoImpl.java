package org.newtglobal.dmapextension.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.newtglobal.dmapextension.utility.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DmapExtensionDaoImpl implements DmapExtensionDao {

	@Autowired
	private Utils utils;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final Logger LOGGER = LoggerFactory.getLogger(DmapExtensionDaoImpl.class);

	@Override
	public Map<String, String> runQueryInPostgres(String convertedQuery) {
		HashMap<String, String> hm = new HashMap<String, String>();
		LOGGER.info("Converted query to run in postgres --> " + convertedQuery);
		if (!utils.isNull(convertedQuery)) {
			if (convertedQuery.toLowerCase().contains("select")) {
				try {
					jdbcTemplate.queryForList(convertedQuery);
					hm.put("Status", "Success");
					hm.put("queryRunMessage", "Success");
				} catch (Exception e) {
					System.out.println("ERROR MESSAGE ==========> " + e.getMessage());
					hm.put("Status", "Failure");
					hm.put("queryRunMessage", e.getMessage());

				}
			} else {
				try {
					jdbcTemplate.update(convertedQuery);
					hm.put("Status", "Success");
					hm.put("queryRunMessage", "Success");
				} catch (Exception e) {
					System.out.println("ERROR MESSAGE ==========> " + e.getMessage());
					hm.put("Status", "Failure");
					hm.put("queryRunMessage", e.getMessage());
				}
			}
		}

		return hm;

	}

	@Override
	public void insertErrorDetails(String applicationRunId, String fileName, String variableLineNumber,
			String oracleQueryWithoutSpaces, String solutionLink, String DateWithTime) {
		try {
			String q = "INSERT INTO app_migration.errordetails(applicationrunid,filename,lineno,query,solution,createdDate) values(?,?,?,?,?,?)";
			LOGGER.info("Insert error details query --> " + q);
			jdbcTemplate.update(q, applicationRunId, fileName, Integer.parseInt(variableLineNumber),
					oracleQueryWithoutSpaces, solutionLink, utils.getCreatedDateWithTime());
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

	@Override
	public void insertProcedureCallDetails(String applicationRunId, String fileName, String variableLineNumber,
			String procedureCall, String DateWithTime) {
		try {
			String q = "INSERT INTO app_migration.procedurecalldetails(applicationrunid,filename,lineno,procedurecall,createdDate) values(?,?,?,?,?)";
			LOGGER.info("Insert procedure call details query --> " + q);
			jdbcTemplate.update(q, applicationRunId, fileName, Integer.parseInt(variableLineNumber), procedureCall,
					DateWithTime);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

	@Override
	public List<Map<String, Object>> isConnectionStringPresent(String appName) {
		List<Map<String, Object>> rs = null;
		try {
			String query = "Select connectiondetails from app_migration.applicationdetails where applicationname = ?";
			LOGGER.info("Is connection string present query --> " + query);
			rs = jdbcTemplate.queryForList(query, appName);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return rs;
	}

	@Override
	public List<Map<String, Object>> getStoredConnectionStrings(String appName) {
		List<Map<String, Object>> rs = null;
		try {
			String query = "Select * from app_migration.connectionstringdetails where applicationname = ?";
			LOGGER.info("Get stored connection string query --> " + query);
			rs = jdbcTemplate.queryForList(query, appName);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return rs;
	}

	@Override
	public void insertDiscoveryDetails(String appName, String applicationRunId, int totalFilesScanned,
			int totalFilesWithQueries, String DateWithTime) {
		try {
			String q = "INSERT INTO app_migration.discoverydetails(applicationname, applicationrunid, totalfilesscanned, totalfileswithqueries, createdDate) values(?,?,?,?,?)";
			LOGGER.info("Insert discovery details query --> " + q);
			jdbcTemplate.update(q, appName, applicationRunId, totalFilesScanned, totalFilesWithQueries, DateWithTime);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
	}
	
	@Override
	public void updateDiscoveryDetails(String appName, int totalFilesScanned, int totalFilesWithQueries){
		try {
			String q = "update app_migration.discoverydetails set totalfilesscanned = ?, totalFilesWithQueries = ? where applicationname = ?";
			LOGGER.info("Insert discovery details query --> " + q);
			jdbcTemplate.update(q, totalFilesScanned, totalFilesWithQueries, appName);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
	}

	@Override
	public List<Map<String, Object>> getAppRunIdFromDiscoveryTable(String appName) {
		List<Map<String, Object>> rs = null;
		try {
			String query = "Select applicationrunid from app_migration.discoverydetails where applicationname = ?";
			LOGGER.info("getAppRunIdFromDiscoveryTable  query --> " + query);
			rs = jdbcTemplate.queryForList(query, appName);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return rs;
	}

	@Override
	public void updateApplicationDetails(String applicationRunId, String appName) {
		try {
			String query = "update app_migration.applicationdetails set applicationrunid = ? where applicationname = ?";
			LOGGER.info("updateApplicationDetails  query --> " + query);
			jdbcTemplate.update(query, applicationRunId, appName);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
	}

	@Override
	public void insertFileDetails(String appName, String applicationRunId, String fileNameWithQueries, String schema,
			String DateWithTime) {
		try {
			System.out.println(appName+" "+applicationRunId+" "+fileNameWithQueries+" "+" "+schema+" "+DateWithTime);
			String query = "INSERT INTO app_migration.filedetails(applicationname, applicationrunid, filenamewithqueries, schemaname, createdDate) values(?,?,?,?,?)";
			LOGGER.info("insertFileDetails  query --> " + query);
			jdbcTemplate.queryForList(query, appName, applicationRunId, fileNameWithQueries, schema, DateWithTime);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
	}

}
