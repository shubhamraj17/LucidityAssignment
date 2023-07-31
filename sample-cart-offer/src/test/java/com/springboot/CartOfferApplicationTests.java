package com.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.controller.ApplyOfferRequest;
import com.springboot.controller.OfferRequest;
import com.springboot.controller.SegmentResponse;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CartOfferApplicationTests {


	@Test
	public void checkFlatXForOneSegment() throws Exception {
		List<String> segments = new ArrayList<>();
		segments.add("p1");
		OfferRequest offerRequest = new OfferRequest(1,"FLATX",10,segments);
		boolean result = addOffer(offerRequest);
		Assert.assertEquals(result,true); // able to add offer
	}

	public boolean addOffer(OfferRequest offerRequest) throws Exception {
		String urlString = "http://localhost:9001/api/v1/offer";
		URL url = new URL(urlString);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);
		con.setRequestProperty("Content-Type", "application/json");

		ObjectMapper mapper = new ObjectMapper();

		String POST_PARAMS = mapper.writeValueAsString(offerRequest);
		OutputStream os = con.getOutputStream();
		os.write(POST_PARAMS.getBytes());
		os.flush();
		os.close();
		int responseCode = con.getResponseCode();
		System.out.println("POST Response Code :: " + responseCode);

		if (responseCode == HttpURLConnection.HTTP_OK) { //success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// print result
			System.out.println(response.toString());
		} else {
			System.out.println("POST request did not work.");
		}
		return true;
	}


	public void addOfferHelper(String segment,String offerType,Integer DiscountValue, Integer restaurantId) throws Exception {
		List<String> segments = new ArrayList<>();
		segments.add(segment);
		OfferRequest offerRequest = new OfferRequest(restaurantId,offerType,DiscountValue,segments);
		Response response= addOfferApiCall(offerRequest);
		Assert.assertEquals(response.statusCode(),HttpStatus.SC_OK);
	}

	@Test
	public void validateCartWithSegmentOneUserFlatDiscount() throws Exception {
		//get user segment
		String segment= getUserSegment(1);

		//add Offer
		addOfferHelper(segment,"FLATX",10,1);

		//apply Offer
		ApplyOfferRequest applyOfferRequest= new ApplyOfferRequest(200,1,1);

		//api call
		Response applyOfferResponse= applyOfferHelper(applyOfferRequest);

		//validation
		Assert.assertEquals(applyOfferResponse.statusCode(), HttpStatus.SC_OK);
		validateCartOfferWithSegment(segment,applyOfferRequest.getCart_value(),applyOfferResponse,10);

	}
	@Test
	public void validateCartWithSegmentOneUserPercentageDiscount() throws Exception {
		//get user segment
		String segment= getUserSegment(1);

		//add Offer
		addOfferHelper(segment,"FLATX%",10,1);

		//apply Offer
		ApplyOfferRequest applyOfferRequest= new ApplyOfferRequest(200,1,1);

		//api call
		Response applyOfferResponse= applyOfferHelper(applyOfferRequest);

		//validation
		Assert.assertEquals(applyOfferResponse.statusCode(), HttpStatus.SC_OK);
		validateCartOfferWithSegment(segment,applyOfferRequest.getCart_value(),applyOfferResponse,10);

	}

	@Test
	public void validateCartWithSegmentTwoUserFlatDiscount() throws Exception {
		//get user segment
		String segment= getUserSegment(2);

		//add Offer
		addOfferHelper(segment,"FLATX",20,1);

		//apply Offer
		ApplyOfferRequest applyOfferRequest= new ApplyOfferRequest(200,1,2);

		//api call
		Response applyOfferResponse= applyOfferHelper(applyOfferRequest);

		//validation
		Assert.assertEquals(applyOfferResponse.statusCode(), HttpStatus.SC_OK);
		validateCartOfferWithSegment(segment,applyOfferRequest.getCart_value(),applyOfferResponse,20);
	}

	@Test
	public void validateCartWithSegmentTwoUserPercentageDiscount() throws Exception {
		//get user segment
		String segment= getUserSegment(2);

		//add Offer
		addOfferHelper(segment,"FLATX%",20,1);

		//apply Offer
		ApplyOfferRequest applyOfferRequest= new ApplyOfferRequest(200,1,2);

		//api call
		Response applyOfferResponse= applyOfferHelper(applyOfferRequest);

		//validation
		Assert.assertEquals(applyOfferResponse.statusCode(), HttpStatus.SC_OK);
		validateCartOfferWithSegment(segment,applyOfferRequest.getCart_value(),applyOfferResponse,20);
	}

	@Test
	public void validateCartWithSegmentOneUserNegativeFlatDiscount() throws Exception {
		//get user segment
		String segment= getUserSegment(1);

		//add Offer
		addOfferHelper(segment,"FLATX%",-10,1);

		//apply Offer
		ApplyOfferRequest applyOfferRequest= new ApplyOfferRequest(200,1,1);

		//api call
		Response applyOfferResponse= applyOfferHelper(applyOfferRequest);

		//validation
		Assert.assertEquals(applyOfferResponse.statusCode(), HttpStatus.SC_OK);
		validateCartOfferWithSegment(segment,applyOfferRequest.getCart_value(),applyOfferResponse,-10);

	}

	@Test
	public void validateCartWithInvalidFields() throws Exception {
		//get user segment
		String segment= getUserSegment(1);

		//add Offer
		addOfferHelper(segment," ",0,1);

		//apply Offer
		ApplyOfferRequest applyOfferRequest= new ApplyOfferRequest(0,1,1);

		//api call
		Response applyOfferResponse= applyOfferHelper(applyOfferRequest);

		//validation
		Assert.assertEquals(applyOfferResponse.statusCode(), HttpStatus.SC_OK);
		validateCartOfferWithSegment(segment,applyOfferRequest.getCart_value(),applyOfferResponse,0);

	}

	public void validateCartOfferWithSegment(String UserSegment,Integer cartValue,Response response,Integer DiscountValue) throws JSONException {
		JSONObject responseObj= new JSONObject(response.asString());
		Integer cartValueResponse=  responseObj.getInt("cart_value");
		if(UserSegment=="p1"){
			Integer expectedCartValue= cartValue-DiscountValue;
			Assert.assertEquals(expectedCartValue,cartValueResponse);
		}else if(UserSegment=="p2"){
			Integer expectedCartValue= (int) (cartValue - cartValue * DiscountValue*(0.01));
			Assert.assertEquals(expectedCartValue,cartValueResponse);
		}
	}

	public String getUserSegment(Integer userId) throws JSONException {
		Response response = getUserSegmentHelper(userId);
		Assert.assertEquals(response.getStatusCode(),200);
		JSONObject responseObj= new JSONObject(response.asString());
		String segment= responseObj.getString("segment");
		return segment;
	}

	public Response getUserSegmentHelper(Integer userId){
		RestAssured.baseURI="http://localhost:1080";
		Response response = RestAssured.given().log().all()
				.queryParam("user_id",userId)
				.when().get("/api/v1/user_segment")
				.thenReturn();
		response.prettyPrint();

		return response;
	}

	public Response applyOfferHelper(ApplyOfferRequest applyOfferRequest){
		RestAssured.baseURI="http://localhost:9001";

		Response response = RestAssured.given().log().all()
				.contentType(ContentType.JSON)
				.body(applyOfferRequest)
				.when().post("/api/v1/cart/apply_offer")
				.thenReturn();
		response.prettyPrint();

		return response;
	}

	public Response addOfferApiCall(OfferRequest offerRequest){
		RestAssured.baseURI= "http://localhost:9001";
		Response response= RestAssured.given().log().all()
				.contentType(ContentType.JSON)
				.body(offerRequest)
				.when().post("/api/v1/offer")
				.thenReturn();
		response.prettyPrint();

		return response;
	}

}
