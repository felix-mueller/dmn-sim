package org.camunda.bpm;

import java.io.InputStream;
import java.util.List;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.camunda.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.camunda.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.camunda.bpm.dmn.engine.test.DmnEngineRule;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.Rule;
import org.junit.Test;

public class DecisionTest {

	  @Rule
	  public DmnEngineRule dmnEngineRule = new DmnEngineRule();

	  @Test
	  public void test() {

		  // TODO: Get DecisionTable and Input Data from JSON
		  
		  String season = "Winter";
		  int guestCount = 1;
		  Boolean guestsWithChildren = true;
		  // create evaluation listner to record matched rules
		  DishDecisionTableEvaluationListener evaluationListener = new DishDecisionTableEvaluationListener();

		  // get decision engine
		  DmnEngineConfiguration engineConfiguration = DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
		  engineConfiguration.getCustomPostDecisionTableEvaluationListeners().add(evaluationListener);
		  DmnEngine dmnEngine = engineConfiguration.buildEngine();
		  
		  // load decision table
		  InputStream inputStream = DecisionTest.class.getResourceAsStream("/beverages.dmn");
	      DmnDecision decision = dmnEngine.parseDecision("beverages", inputStream);
	    
	      // prepare input data
		  VariableMap variables = Variables
	        .putValue("season", season)
	        .putValue("guestCount", guestCount).putValue("guestsWithChildren", guestsWithChildren);
	      
		  // run decision table
		  DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);
		    List<String> beverages = result.collectEntries("beverages");
		    System.out.println("Beverages:\n\tI would recommend to serve: " + beverages);
		  // Get result
		    /*String desiredDish = result.getSingleResult().getSingleEntry();
		  
		  // println the result
		  System.out.println(desiredDish);
		  */
	      // print event
		   
	      DmnDecisionTableEvaluationEvent evaluationEventsingle = evaluationListener.getLastEvent();
	      List<DmnDecisionTableEvaluationEvent> evaluationEvents = evaluationListener.getLastEvents();
	      System.out.println("The following Rules matched:");
	      for (DmnDecisionTableEvaluationEvent evaluationEvent: evaluationEvents) {
	      for (DmnEvaluatedDecisionRule matchedRule : evaluationEvent.getMatchingRules()) {
	        System.out.println("\t" + matchedRule.getId() + ":");
	        for (DmnEvaluatedOutput output : matchedRule.getOutputEntries().values()) {
	          System.out.println("\t\t" + output);
	        }
	      }
	      }
	      // TODO: Wrap result and matched rules in JSON and send back
	  }

	}
