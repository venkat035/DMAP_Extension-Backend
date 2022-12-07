package org.newtglobal.dmapextension.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

@Component
public class DmapCall {

	@Autowired
	private SqlComparision sqlComparision;
	
	@Autowired
	private AppendSchemaNameThroughFlask appendSchemaNameThroughFlask;

	@Value("${dmapBaseURL}")
	private String dmapBaseURL;

	private static final Logger LOGGER = LoggerFactory.getLogger(DmapCall.class);

	public Map<String, String> getSqlStats(String oracleQuery, String dirPath, String fileName, String applicationName,
			String variableLineNumber, MultiValuedMap<String, List<String>> dynamicQueryPartsDetails,
			boolean isFunctionParametersUsedInQuery) {
		String convertedQuery = "";
		String remediationMessage = "";
		try {
			if (!oracleQuery.isBlank() || !oracleQuery.isEmpty() || !oracleQuery.equals("")) {
				if (!oracleQuery.equals(" ")) {

					Response response = establishConnectionWithDmap(oracleQuery);
					ArrayList<String> convertedQueryList = response.jsonPath().get("convertedSQL");
					for (int i = 0; i < convertedQueryList.size(); i++) {
						convertedQuery = convertedQueryList.get(0);
					}

// 					Response response1 = appendSchemaNameThroughFlask
// 							.establishConnectionWithFlaskToAppendSchemaName(applicationName, fileName, convertedQuery);
// 					ArrayList<String> convertedQueryWithSchemaNameList = response1.jsonPath()
// 							.get("convertedQueryWithSchemaName");
// 					for (int j = 0; j < convertedQueryWithSchemaNameList.size(); j++) {
// 						convertedQuery = convertedQueryWithSchemaNameList.get(0);
// 					}

					remediationMessage = sqlComparision.getRemediationMessage(oracleQuery, convertedQuery, dirPath,
							fileName, applicationName, variableLineNumber, dynamicQueryPartsDetails,isFunctionParametersUsedInQuery);
				}

			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return prepareMap(remediationMessage, convertedQuery);
	}

	public Response establishConnectionWithDmap(String oracleQuery) {

		Response response = null;
		try {
			LOGGER.info("Oracle Query at the time of establishConnectionWithDmap --> " + oracleQuery);
			RestAssured.baseURI = dmapBaseURL;
			RequestSpecification httpRequest = RestAssured.given();
			httpRequest.header("Content-Type", "application/json");
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("sql", oracleQuery);
			httpRequest.body(jsonObj);
			response = httpRequest.request(Method.POST, "/dmap_extension");
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return response;
	}

	public Map<String, String> prepareMap(String remediationMessage, String convertedSQL) {
		HashMap<String, String> hm = new HashMap<String, String>();
		try {
			if (remediationMessage.equalsIgnoreCase("queryRemediatedButFoundSame")) {
				hm.put("queryRemediatedButFoundSame", "true");
				hm.put("queryRemediated", "false");
				hm.put("queryNotRemediated", "false");
			} else if (remediationMessage.equalsIgnoreCase("queryRemediated")) {
				hm.put("queryRemediatedButFoundSame", "false");
				hm.put("queryRemediated", "true");
				hm.put("queryNotRemediated", "false");
			} else if (remediationMessage.equalsIgnoreCase("queryNotRemediated")) {
				hm.put("queryRemediatedButFoundSame", "false");
				hm.put("queryRemediated", "false");
				hm.put("queryNotRemediated", "true");
			}
			hm.put("convertedQuery", convertedSQL);
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return hm;
	}

}
