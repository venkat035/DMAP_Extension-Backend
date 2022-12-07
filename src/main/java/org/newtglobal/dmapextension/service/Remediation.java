package org.newtglobal.dmapextension.service;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.collections4.MultiValuedMap;
import org.newtglobal.dmapextension.utility.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.restassured.response.Response;

@Component
public class Remediation {

	@Autowired
	private Utils utils;

	@Autowired
	private Comments comments;

	@Autowired
	private ReplaceWithFlask replaceWithFlask;

	private static final Logger LOGGER = LoggerFactory.getLogger(Remediation.class);

	public void findAndReplaceQuery(String oracleQuery, String convertedQuery, String dirPath,
			String remediationMessage) {
		String dmapExtensionEngineMessage = "";
		try {
			if (utils.isProcedureCall(convertedQuery)) {
				dmapExtensionEngineMessage = " // DEX Message : Needs Manual Intervention";
			} else {
				dmapExtensionEngineMessage = " // DEX Message : Changed Query - " + convertedQuery;
			}
			String convertedFileContents = "";
			String oracleQueryToMatch = "";

			Scanner sc = new Scanner(new File(dirPath));
			StringBuilder buffer = new StringBuilder();
			while (sc.hasNextLine()) {
				final String eachLine = sc.nextLine();
				if (oracleQuery.contains("=")) {
					String[] oracleQueryPart = oracleQuery.split("=");
					oracleQueryToMatch = oracleQueryPart[0];
				} else {
					oracleQueryToMatch = oracleQuery;
				}
				if (eachLine.contains(oracleQueryToMatch.trim())) {
					buffer.append(eachLine + dmapExtensionEngineMessage + System.lineSeparator());
				} else {
					buffer.append(eachLine + System.lineSeparator());
				}

			}
			String fileContents = buffer.toString();
			sc.close();

			Response response = replaceWithFlask.establishConnectionWithFlaskForRemediation(fileContents, oracleQuery,
					convertedQuery);
			convertedFileContents = response.jsonPath().get("convertedContent");
			FileWriter writer = new FileWriter(dirPath);
			writer.append(convertedFileContents);
			writer.flush();
			writer.close();
			buffer.setLength(0);
			buffer = null;

		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}
	}

	public void findAndReplaceDynamicQuery(String oracleQuery, String convertedQuery, String dirPath,
			MultiValuedMap<String, List<String>> dynamicQueryPartsDetails, String variableLineNumber,
			String remediationMessage) {
		String convertedFileContents = "";
		String fileContentUpdated = "";
		try {
			String[] oracleQueryParts = oracleQuery.strip().split("\\s+");
			String[] convertedQueryParts = convertedQuery.split("\\s+");
			int convertedQuerySize = convertedQueryParts.length;
			if (oracleQueryParts.length == convertedQuerySize) {
				Scanner sc = new Scanner(new File(dirPath));
				StringBuilder buffer = new StringBuilder();
				while (sc.hasNextLine()) {

					buffer.append(sc.nextLine() + System.lineSeparator());
				}
				String fileContents = buffer.toString();
				fileContentUpdated = fileContents;
				sc.close();
				for (int i = 0; i < convertedQuerySize; i++) {

					Response response = replaceWithFlask.establishConnectionWithFlaskForRemediation(fileContentUpdated,
							oracleQueryParts[i], convertedQueryParts[i]);
					convertedFileContents = response.jsonPath().get("convertedContent");
					fileContentUpdated = convertedFileContents;
				}

				FileWriter writer = new FileWriter(dirPath);
				writer.append(fileContentUpdated);
				writer.flush();
				writer.close();
				buffer.setLength(0);
				buffer = null;

				comments.plugComments(dirPath, Integer.parseInt(variableLineNumber) - 1, convertedQuery,
						remediationMessage, "equalDynamicQuery", false);
			} else {
				String lineNumbers = "";
				for (Map.Entry<String, List<String>> dynamicQueryPartsDetail : dynamicQueryPartsDetails.entries()) {
					lineNumbers = lineNumbers + dynamicQueryPartsDetail.getValue().get(1);
					lineNumbers = lineNumbers + ",";
				}
				lineNumbers = lineNumbers.substring(0, lineNumbers.length() - 1);
				System.out.println("Manual Remediation Suggested for the converted query: " + convertedQuery);
				System.out.println("\tPlease check on line no:" + variableLineNumber + "(" + lineNumbers + ")");
				lineNumbers = "";
				comments.plugComments(dirPath, Integer.parseInt(variableLineNumber) - 1, convertedQuery,
						remediationMessage, "unequalDynamicQuery", false);
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

	}

}
