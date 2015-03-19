package models;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ResponseWrapper {

	public static JsonNode success() {
		ObjectNode result = Json.newObject();
		result.put("code", 200);
		result.put("success", 1);
		return result;
	}
	
	public static JsonNode success(String code, String message) {
		ObjectNode result = Json.newObject();
		result.put("code", 200);
		result.put("success", 1);
		result.put(code, message);
		return result;
	}
	
	public static JsonNode success(JsonNode result) {
		ObjectNode response = Json.newObject();
		response.put("code", 200);
		response.put("success", 1);
		response.putPOJO("results", result);
		return response;
	}
	
	public static JsonNode success(ArrayNode result) {
		ObjectNode response = Json.newObject();
		response.put("code", 200);
		response.put("success", 1);
		response.putPOJO("results", result);
		return response;
	}
	
	public static JsonNode badRequest(String message) {
		ObjectNode result = Json.newObject();
		result.put("success", 0);
		result.put("error", message);
		return result;
	}
	
	public static JsonNode badRequestNoJson() {
		ObjectNode result = Json.newObject();
		result.put("success", 0);
		result.put("error", "This endpoint accepts JSON input. Please make sure you set your headers correctly.");
		return result;
	}
	
	public static JsonNode unauthorized(String message) {
		ObjectNode result = Json.newObject();
		result.put("success", 0);
		result.put("error", message);
		return result;
	}
	
	public static JsonNode notFound(String message) {
		ObjectNode result = Json.newObject();
		result.put("success", 0);
		result.put("error", message);
		return result;
	}
	
	public static JsonNode internalServerError(String message) {
		ObjectNode result = Json.newObject();
		result.put("success", 0);
		result.put("error", message);
		return result;
	}
}