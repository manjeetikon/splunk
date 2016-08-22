package com.splunktest.clouhub.splunk.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.munit.common.mocking.Attribute;
import org.mule.munit.common.mocking.MessageProcessorMocker;
import org.mule.munit.common.mocking.SpyProcess;
import org.mule.munit.runner.functional.FunctionalMunitSuite;


public class SplunkDataPushTest extends FunctionalMunitSuite {
	
	Logger log = Logger.getLogger(SplunkDataPushTest.class);
	
	@Override
	public boolean haveToMockMuleConnectors() {
		return true;
	}

	@Override
	protected String getConfigResources() {
		return "splunk-connect-test.xml;";
	}
		
	@Test
	public void testCloudHubToSplunkDataPush() throws Exception {
		
		log.info("testCloudHubToSplunkDataPush begin ...........");

		HashMap<String, String> map = new HashMap<String, String>();
		map.put("domain", "american-flights-api-test74");

		MuleEvent testEvent = testEvent(map);

		// Below is mocking code for cloudhub connector
		HashMap<String, String> jsonMsgMap = new HashMap<String, String>();
		jsonMsgMap.put("msg", "This is a Mock Message");
		MuleMessage messageToBeReturnedForCloudHubConnector = muleMessageWithPayload(jsonMsgMap);
		MessageProcessorMocker mockerCloudHub = whenMessageProcessor("retrieve-application-logs")
				.ofNamespace("cloudhub");
		mockerCloudHub.thenReturn(messageToBeReturnedForCloudHubConnector);

		// Below is mocking code for HTTP outbound endpoint
		MuleMessage messageToBeReturnedForHttpEndPoint = muleMessageWithPayload("Splunk data is registered");
		messageToBeReturnedForHttpEndPoint.setOutboundProperty("http.status", "200");
		messageToBeReturnedForHttpEndPoint.setOutboundProperty("http.reason", "OK");

		MessageProcessorMocker mockerHttp = whenMessageProcessor("request").ofNamespace("http");
		mockerHttp.thenReturn(messageToBeReturnedForHttpEndPoint);

		// Below is mock test for set Payload
		MuleMessage messageToBeReturnedForFinalSetPayload = muleMessageWithPayload(
				"HTTP Status : 200  HTTP Response :  OK");
		MessageProcessorMocker mockerSetPayload = whenMessageProcessor("set-payload")
				.withAttributes(Attribute.attribute("name").ofNamespace("doc").withValue("Set Payload"));
		mockerSetPayload.thenReturn(messageToBeReturnedForFinalSetPayload);

		// Spy Processor to run and after set Event Payload node
		SpyProcess beforeSpy = new SpyProcess() {

			@Override
			public void spy(MuleEvent event) throws MuleException {
				assertEquals("{\"msg\":\"This is a Mock Message\"}", event.getMessage().getPayload());
			}
		};
		SpyProcess afterSpy = new SpyProcess() {

			@Override
			public void spy(MuleEvent event) throws MuleException {
				assertEquals("{\"event\":{\"msg\":\"This is a Mock Message\"}}", event.getMessage().getPayload());
			}
		};
		spyMessageProcessor("set-payload")
				.withAttributes(Attribute.attribute("name").ofNamespace("doc").withValue("Set Event Payload"))
				.before(beforeSpy).after(afterSpy);

		MuleEvent resultEvent = runFlow("getCloudHubLogs-push-to-splunk-Flow", testEvent);
		assertNotNull(resultEvent);
		assertNotNull(resultEvent.getMessage().getPayload());

		log.info("payload is " + resultEvent.getMessage().getPayload());
		assertEquals("HTTP Status : 200  HTTP Response :  OK", resultEvent.getMessage().getPayloadAsString().trim());
		
		//assertEquals("200", resultEvent.getFlowVariable(key).getProperty("http.status"));
		//assertEquals("OK", resultEvent.getMessage().getInboundProperty("http.reason"));
        	
		
		/*final Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("cloudhub-to-splunk-data-push");

		Synchronizer synchronizer = new Synchronizer(muleContext, 120000) {

			@Override
			protected MuleEvent process(MuleEvent event) throws Exception {

				return flow.process(event);
			}
		};

		MuleEvent event = testEvent("Test Message, no input message is required for testing this flow.");
		MuleEvent resultEvent = synchronizer.runAndWait(event);
		assertNotNull(resultEvent);
		assertNotNull(resultEvent.getMessage().getPayload());
		
		log.info("payload is " + resultEvent.getMessage().getPayload());*/
		
	}
}
