package org.newtglobal.dmapextension.service;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;
import org.newtglobal.dmapextension.dao.DmapExtensionDao;
import org.newtglobal.dmapextension.utility.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SqlComparision {

	@Autowired
	private Utils utils;

	@Autowired
	private Comments comments;

	@Autowired
	private Remediation remediation;

	@Autowired
	private SolutionEngine solutionEngine;

	@Autowired
	private DmapExtensionDao dmapExtensionDao;

	private static final Logger LOGGER = LoggerFactory.getLogger(SqlComparision.class);

	public String getRemediationMessage(String oracleQuery, String convertedQuery, String dirPath, String fileName,
			String applicationName, String variableLineNumber,
			MultiValuedMap<String, List<String>> dynamicQueryPartsDetails, boolean isFunctionParametersUsedInQuery) {
		String remediationMessage = "";
		try {
			String oracleQueryWithoutSpaces = utils.getQueryWithoutExtraSpaces(oracleQuery);
			Map<String, String> queryMessageMap = dmapExtensionDao.runQueryInPostgres(convertedQuery);
			if (isFunctionParametersUsedInQuery) {
				remediationMessage = "queryRemediatedButFoundSame";
				comments.plugComments(dirPath, Integer.parseInt(variableLineNumber) - 1, convertedQuery,
						remediationMessage, null, isFunctionParametersUsedInQuery);
			}
			else if (queryMessageMap.get("Status") != null && queryMessageMap.get("Status").equals("Failure")) {
				if (queryMessageMap.get("queryRunMessage").contains("does not exist")
						|| queryMessageMap.get("queryRunMessage").contains("?")) {
					if (oracleQueryWithoutSpaces.equalsIgnoreCase(convertedQuery)) {
						remediationMessage = "queryRemediatedButFoundSame";
						comments.plugComments(dirPath, Integer.parseInt(variableLineNumber) - 1, convertedQuery,
								remediationMessage, null, false);
					} else {
						remediationMessage = "queryRemediated";
						if (dynamicQueryPartsDetails.size() > 0) {
							remediation.findAndReplaceDynamicQuery(oracleQuery, convertedQuery, dirPath,
									dynamicQueryPartsDetails, variableLineNumber, remediationMessage);
						} else {
							remediation.findAndReplaceQuery(oracleQuery, convertedQuery, dirPath, remediationMessage);

						}
					}
				} else {
					remediationMessage = "queryNotRemediated";
					comments.plugComments(dirPath, Integer.parseInt(variableLineNumber) - 1, convertedQuery,
							remediationMessage, null, false);
					if (!oracleQuery.equals("Procedure_call")) {
						List<Map<String, Object>> rs = dmapExtensionDao.getAppRunIdFromDiscoveryTable(applicationName);
						dmapExtensionDao.insertErrorDetails(rs.get(0).get("applicationrunid").toString(), fileName, variableLineNumber,
								oracleQueryWithoutSpaces, solutionEngine.getSqlSolution(oracleQueryWithoutSpaces),
								utils.getCreatedDateWithTime());
					}

				}
			} else {
				if (oracleQueryWithoutSpaces.equalsIgnoreCase(convertedQuery)) {
					remediationMessage = "queryRemediatedButFoundSame";
					comments.plugComments(dirPath, Integer.parseInt(variableLineNumber) - 1, convertedQuery,
							remediationMessage, null, false);
				} else {
					remediationMessage = "queryRemediated";
					if (dynamicQueryPartsDetails.size() > 0) {
						remediation.findAndReplaceDynamicQuery(oracleQuery, convertedQuery, dirPath,
								dynamicQueryPartsDetails, variableLineNumber, remediationMessage);
					} else {
						remediation.findAndReplaceQuery(oracleQuery, convertedQuery, dirPath, remediationMessage);

					}

				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
		return remediationMessage;
	}

}
