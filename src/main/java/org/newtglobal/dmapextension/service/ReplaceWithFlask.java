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
public class ReplaceWithFlask {

	@Value("${flaskBaseURL}")
	private String flaskBaseUrl;

	private static final Logger LOGGER = LoggerFactory.getLogger(ReplaceWithFlask.class);

	public Response establishConnectionWithFlaskForRemediation(String fileContents, String oracleQuery,
			String convertedQuery) {

		Response response = null;
		try {
			RestAssured.baseURI = flaskBaseUrl;
			RequestSpecification httpRequest = RestAssured.given();
			httpRequest.header("Content-Type", "application/json");
			JSONObject jsonObj = new JSONObject();

			jsonObj.put("fileContents", fileContents);
			jsonObj.put("oracleQuery", oracleQuery);
			jsonObj.put("convertedQuery", convertedQuery);

			httpRequest.body(jsonObj);
			response = httpRequest.request(Method.POST, "/replace_query");
		} catch (Exception e) {
			LOGGER.error("Exception occurred --> " + e);
		}

		return response;
	}

}
