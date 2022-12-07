package org.newtglobal.dmapextension.service;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

@Component
public class AppendSchemaNameThroughFlask {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppendSchemaNameThroughFlask.class);

	@Value("${flaskBaseURL}")
	private String flaskBaseUrl;

	public Response establishConnectionWithFlaskToAppendSchemaName(String applicationName, String fileName, String convertedQuery) {
		Response response = null;
		try {
			LOGGER.info("Converted query for establishConnectionWithFlaskToAppendSchemaName --> " + convertedQuery);
			RestAssured.baseURI = flaskBaseUrl;
			RequestSpecification httpRequest = RestAssured.given();
			httpRequest.header("Content-Type", "application/json");
			JSONObject jsonObj = new JSONObject();

			jsonObj.put("applicationName", applicationName);
			jsonObj.put("fileName", fileName);
			jsonObj.put("convertedQuery", convertedQuery);

			httpRequest.body(jsonObj);
			response = httpRequest.request(Method.POST, "/append_schema_name");
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return response;
	}

}
