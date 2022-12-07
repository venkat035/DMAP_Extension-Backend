package org.newtglobal.dmapextension.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Comments {

	private static final Logger LOGGER = LoggerFactory.getLogger(Comments.class);

	public void plugComments(String dirPath, int lineNo, String convertedQuery, String remediationMessage,
			String queryType, boolean isFunctionParametersUsedInQuery) {
		String dmapExtensionEngineMessage = "";

		try {
			LOGGER.info("Plug comments info --> dirPath - " + dirPath + " line no - " + lineNo + " convertedQuery - "
					+ convertedQuery + " remediationMessage - " + remediationMessage + "queryType - " + queryType);
			if (remediationMessage.equals("queryRemediatedButFoundSame")) {
				if (isFunctionParametersUsedInQuery) {
					dmapExtensionEngineMessage = " // DEX Message : Query associated with (or) taken from Function parameter ";

				} else {
					dmapExtensionEngineMessage = " // DEX Message : Changed Query - " + convertedQuery;

				}
			} else if (remediationMessage.equals("queryNotRemediated")
					|| (convertedQuery.contains("call") && convertedQuery.contains("?"))) {
				dmapExtensionEngineMessage = " // DEX Message : Needs Manual Intervention";
			} else if (remediationMessage.equals("queryRemediated")) {
				if (queryType.equals("equalDynamicQuery")) {
					dmapExtensionEngineMessage = " // DEX Message : Changed Query - " + convertedQuery;
				} else if (queryType.equals("unequalDynamicQuery")) {
					dmapExtensionEngineMessage = " // DEX Message : Needs Manual Intervention (Changed Query : "
							+ convertedQuery + ")";
				}
			}

			Path path = Paths.get(dirPath);

			String line = Files.readAllLines(path).get(lineNo).concat(dmapExtensionEngineMessage);
			System.out.println("LINE CONTENT =====> " + line);
			List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

			lines.remove(Files.readAllLines(path).get(lineNo));
			lines.add(lineNo, line);
			Files.write(path, lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println(e);
			LOGGER.error("Exception occurred --> " + e);
		}

	}
}
