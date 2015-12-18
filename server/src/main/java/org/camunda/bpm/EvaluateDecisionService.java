package org.camunda.bpm;

import static org.camunda.spin.Spin.JSON;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.camunda.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.camunda.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.spin.json.SpinJsonNode;

@WebServlet(urlPatterns={"/evaluateDecision"})
public class EvaluateDecisionService extends HttpServlet {

	/*
	@Rule
	public ProcessEngineRule rule = new ProcessEngineRule();
	*/
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
			throws ServletException, IOException {

		String decisionTable = req.getParameter("decisionTable");
		System.out.println(decisionTable);
		//SpinJsonNode requestNode = JSON(requestJson);
		
		//String test = requestNode.prop("input").stringValue();		
	
		  // TODO: Get DecisionTable and Input Data from JSON
		  
		  String season = "Summer";
		  int guestCount = 800;

		  // create evaluation listner to record matched rules
		  DishDecisionTableEvaluationListener evaluationListener = new DishDecisionTableEvaluationListener();

		  // get decision engine
		  DmnEngineConfiguration engineConfiguration = DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
		  engineConfiguration.getCustomPostDecisionTableEvaluationListeners().add(evaluationListener);
		  DmnEngine dmnEngine = engineConfiguration.buildEngine();
		  
		  // load decision table
		  //InputStream inputStream = EvaluateDecisionService.class.getResourceAsStream("/dish.dmn");
		  InputStream inputStream = new ByteArrayInputStream(decisionTable.getBytes(StandardCharsets.UTF_8));

		  DmnDecision decision = dmnEngine.parseDecision("decision", inputStream);
	      
	      // prepare input data
		  VariableMap variables = Variables
	        .putValue("season", season)
	        .putValue("guestCount", guestCount);
	      
		  // run decision table
		  DmnDecisionTableResult result = dmnEngine.evaluateDecisionTable(decision, variables);

		  // Get result
		  String desiredDish = result.getSingleResult().getSingleEntry();
		  
		  // println the result
		  System.out.println(desiredDish);
		  
	      // print event
	      DmnDecisionTableEvaluationEvent evaluationEvent = evaluationListener.getLastEvent();
	      System.out.println("The following Rules matched:");
	      for (DmnEvaluatedDecisionRule matchedRule : evaluationEvent.getMatchingRules()) {
	        System.out.println("\t" + matchedRule.getId() + ":");
	        for (DmnEvaluatedOutput output : matchedRule.getOutputEntries().values()) {
	          System.out.println("\t\t" + output);
	        }
	      }		
		
		
		String response = "Test_";
		
		resp.setHeader("Content-Type", "application/json;charset=UTF-8");
		
		SpinJsonNode jsonNode = JSON("{}");
		jsonNode.prop("result", response);
		
		
		String json = jsonNode.toString();
		resp.getWriter().write(json);
		
	};


	

}
