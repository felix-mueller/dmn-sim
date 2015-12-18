package org.camunda.bpm;

import static org.camunda.spin.Spin.JSON;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

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
import org.camunda.spin.SpinList;
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

		BufferedReader reader = req.getReader();
		
		SpinJsonNode requestNode = JSON(reader);

		SpinList<SpinJsonNode> inputs = requestNode.prop("inputs").elements();
		
	    // prepare input data
		VariableMap variables = Variables.createVariables();
	    int inputCounter = 1;
		for (SpinJsonNode inputNode : inputs) {
			if (inputNode.prop("type").stringValue().equals("string")) {
				String myVariable = inputNode.prop("value").stringValue();
				variables.put("input" + inputCounter, myVariable);
			} else 	if (inputNode.prop("type").stringValue().equals("integer")) {
				Number myVariable = inputNode.prop("value").numberValue();
				variables.put("input" + inputCounter, myVariable);
			}
			inputCounter++;
		}
	
		  // create evaluation listner to record matched rules
		  DishDecisionTableEvaluationListener evaluationListener = new DishDecisionTableEvaluationListener();

		  // get decision engine
		  DmnEngineConfiguration engineConfiguration = DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
		  engineConfiguration.getCustomPostDecisionTableEvaluationListeners().add(evaluationListener);
		  DmnEngine dmnEngine = engineConfiguration.buildEngine();
		  
		  // load decision table
		  InputStream inputStream = new ByteArrayInputStream(requestNode.prop("xml").stringValue().getBytes(StandardCharsets.UTF_8));
		  DmnDecision decision = dmnEngine.parseDecision("decision", inputStream);
	      
		  // run decision table
		  DmnDecisionTableResult result = dmnEngine.evaluateDecisionTable(decision, variables);
		  
		 

  		List rulesList = new LinkedList();
	      // print event
	      DmnDecisionTableEvaluationEvent evaluationEvent = evaluationListener.getLastEvent();
	      //System.out.println("The following Rules matched:");
	      for (DmnEvaluatedDecisionRule matchedRule : evaluationEvent.getMatchingRules()) {
	    	  SpinJsonNode rulesNode = JSON("{}");
	    	  rulesNode.prop("ruleId", matchedRule.getId());
	    	  List outputList = new LinkedList();
	    	  //System.out.println("\t" + matchedRule.getId() + ":");
	        for (DmnEvaluatedOutput output : matchedRule.getOutputEntries().values()) {
		    	 outputList.add(output.getValue().getValue());
	        	//System.out.println("\t\t" + output);
	        }
	          rulesNode.prop("outputs", outputList);
	        rulesList.add(rulesNode);
	      
	      }		
		resp.setHeader("Content-Type", "application/json;charset=UTF-8");
		SpinJsonNode rootNode = JSON("{}");
		rootNode.prop("rules", rulesList);
		String json = rootNode.toString();
		resp.getWriter().write(json);
	};

}
