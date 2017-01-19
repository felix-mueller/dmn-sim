package org.camunda.bpm;

import static org.camunda.spin.Spin.JSON;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnDecisionResult;
import org.camunda.bpm.dmn.engine.DmnDecisionResultEntries;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.camunda.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.camunda.bpm.dmn.engine.delegate.DmnEvaluatedInput;
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

		SpinJsonNode rootNode = JSON("{}");

		try {
		
			BufferedReader reader = req.getReader();
			SpinJsonNode requestNode = JSON(reader);
			String decisionTable = null;
			if (requestNode.hasProp("decisionTable")) {
				decisionTable = requestNode.prop("decisionTable").stringValue();
			}
			
			SpinList<SpinJsonNode> inputs = requestNode.prop("inputs").elements();
			
		    // prepare input data
			VariableMap variables = Variables.createVariables();
			for (SpinJsonNode inputNode : inputs) {
				String varname = inputNode.prop("varname").stringValue();
				if (inputNode.prop("type").stringValue().equals("string")) {
					String myVariable = inputNode.prop("value").stringValue();
				
					variables.putValue(varname, myVariable);
				} else 	if (inputNode.prop("type").stringValue().equals("integer") || inputNode.prop("type").stringValue().equals("long") || inputNode.prop("type").stringValue().equals("double")) {
					Number myVariable = inputNode.prop("value").numberValue();
					variables.putValue(varname, myVariable);
				} else 	if (inputNode.prop("type").stringValue().equals("boolean") ) {
					// little workaround because boolean currently comes as string
					boolean myVariable = false;
					String myBoolTest = inputNode.prop("value").stringValue();
					if (myBoolTest.equals("true")) myVariable = true;
					variables.putValue(varname, myVariable);
				} else 	if (inputNode.prop("type").stringValue().equals("date") ) {
					String myVariableString = inputNode.prop("value").stringValue();
				    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date myVariable = null;
				    try {
				    	myVariable =  df.parse(myVariableString);
				    } catch (Exception e) {
				    	throw new Exception (e.getMessage() + "<p>The correct format is yyyy-MM-dd HH:mm:ss </p>");
				    }
					variables.putValue(varname, myVariable);
				} else {
					throw new Exception ("Data Type <i>" + inputNode.prop("type").stringValue() + "</i> is not yet supported in Simulation.");
				}
			}
		
			  // create evaluation listner to record matched rules
			  DishDecisionTableEvaluationListener evaluationListener = new DishDecisionTableEvaluationListener();
			  // get decision engine
			  DmnEngineConfiguration engineConfiguration = DmnEngineConfiguration.createDefaultDmnEngineConfiguration();
			  engineConfiguration.getCustomPostDecisionTableEvaluationListeners().add(evaluationListener);
			  DmnEngine dmnEngine = engineConfiguration.buildEngine();
			  
			  // load decision table
			  InputStream inputStream = new ByteArrayInputStream(requestNode.prop("xml").stringValue().getBytes(StandardCharsets.UTF_8));
			  System.out.println(requestNode.prop("xml").stringValue());
			  //DmnDecision decision = dmnEngine.parseDecision("decision", inputStream);
			  DmnDecision decision;
			  if (decisionTable != null) {
				  decision = dmnEngine.parseDecision(decisionTable,inputStream);
			  } else {
				  decision = dmnEngine.parseDecisions(inputStream).get(0);
			  }
			  // run decision table
			  DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);
			  Iterator itr = result.iterator();
	    		while(itr.hasNext()) {
	    			SpinJsonNode vars = JSON("[]");
	    			DmnDecisionResultEntries entry = (DmnDecisionResultEntries) itr.next();
	    			Map<String,Object> e = entry.getEntryMap();
	    			for (String key : e.keySet()) {
	    				SpinJsonNode var = JSON("{}");
	    				var.prop(key,e.get(key).toString());
	    				vars.append(var);
	    			}
	    			rootNode.prop("tableResults",vars);
	    		}
			    List collectionList = new LinkedList();
			    SpinJsonNode resultNode = JSON(result.collectEntries(decisionTable));

	  		List rulesList = new LinkedList();
		      // print event
	  		 List<DmnDecisionTableEvaluationEvent> evaluationEvents = evaluationListener.getLastEvents();
		      System.out.println("The following Rules matched:");
		      for (DmnDecisionTableEvaluationEvent evaluationEvent: evaluationEvents) {
		      //System.out.println("The following Rules matched:");
		    	  List<DmnEvaluatedInput> inputsDmn = evaluationEvent.getInputs();
		    	  SpinJsonNode vars = JSON("[]");
		    	  for (DmnEvaluatedInput input : inputsDmn) {
		    		  SpinJsonNode var = JSON("{}");
		    		  var.prop(input.getInputVariable(),input.getValue().getValue().toString());
		    		  vars.append(var);
		    	  }
		    	  rootNode.prop("inputs",vars);
		      for (DmnEvaluatedDecisionRule matchedRule : evaluationEvent.getMatchingRules()) {
		    	  SpinJsonNode rulesNode = JSON("{}");
		    	  rulesNode.prop("ruleId", matchedRule.getId());
		    	  List outputList = new LinkedList();
		    	  //System.out.println("\t" + matchedRule.getId() + ":");
		        for (DmnEvaluatedOutput output : matchedRule.getOutputEntries().values()) {
		        	SpinJsonNode outputProp = JSON("{}");
		        	outputProp.prop(output.getId(), output.getValue().getValue().toString());
			    	outputList.add(outputProp);
		        	//System.out.println("\t\t" + output);
		        }
		       
		          rulesNode.prop("outputs", outputList);
		        rulesList.add(rulesNode);
		      
		      }	
		      }
				rootNode.prop("collection", collectionList);
				rootNode.prop("rules", rulesList);
		} catch (Exception e) {
			rootNode.prop("error", e.getMessage());
		}
		
		
		
		
		resp.setHeader("Content-Type", "application/json;charset=UTF-8");
		
		String json = rootNode.toString();
		resp.getWriter().write(json);
	};

}
