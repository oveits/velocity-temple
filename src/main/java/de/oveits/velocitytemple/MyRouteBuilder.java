package de.oveits.velocitytemple;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;


/**
 * A Camel Java DSL Router
 */
public class MyRouteBuilder extends RouteBuilder {

    /**
     * Let's configure the Camel routing rules using Java code...
     */
    public void configure() {
    	
    	String cached="cached=false";
    	
    	// Standard Exception Handler
		onException(Exception.class)
				.setHeader("ResultCode", constant(1))
				.setHeader("ResultText", constant("Failure"))
				.convertBodyTo(String.class).bean(MyExceptionHandler.class)
				.log("failed request: ${body}")
//				.setHeader("Content-Type", constant("application/x-www-form-urlencoded"))
				.setHeader("Content-Type", constant("text/html"))
//				.setHeader("Content-Type", constant("text/plain"))
				.handled(false)
				.end();
    	
    	//
		// MAIN ROUTES
		//
		
		// at least a single jetty consumer is needed, so the rest routes can be created based on the jetty component:
		// in our case, we implement a redirect on the root URL:
    	from("jetty:http://0.0.0.0:{{inputport}}/")
		    .setHeader("Location", simple("${headers.CamelHttpUrl}templates"))
			.setHeader("CamelHttpResponseCode", constant("301"))		
			;
		

    	// define source interfaces and port:
		restConfiguration().host("0.0.0.0").port("{{inputport}}");
		
		// define REST service:
		rest("/templates")
		// List
			.get() 
			.route().pipeline("direct:before", "direct:listTemplates", "direct:after").endRest()
		// Apply Velocity from Body
			.post("/apply")
			.route().pipeline("direct:before", "direct:applyTemplateFromBody", "direct:after").endRest()
		// Read as text:
			.get("/{templateName}") // Read
			.route().pipeline("direct:before", "direct:readTemplate", "direct:after").endRest()
		// Read as json (not supported)
//			.get("/{templateName}/json")
//			.route().pipeline("direct:before", "direct:readTemplate", "direct:toJson").endRest()
		// Apply Velocity
			.get("/{templateName}/apply")
			.route().pipeline("direct:before", "direct:applyTemplate", "direct:after").endRest()
		// Create
			.post("/{templateName}")
			.route().pipeline("direct:before", "direct:createTemplate", "direct:after").endRest()
		// Update
			.put("/{templateName}")
			.route().pipeline("direct:before", "direct:updateTemplate", "direct:after").endRest()
		// Delete
			.delete("/{templateName}")
			.route().pipeline("direct:before", "direct:deleteTemplate", "direct:after").endRest()
		;
		
//		// define REST service:
//		rest("/ssh")
//		// Apply Velocity
//			.get("/{templateName}/apply")
//			.route().pipeline("direct:before", "direct:applySSH", "direct:after").endRest()
//		;
		
		from("direct:before")
		// default settings:
			// cached = false
			.choice().when(header("cached").isNull()).setHeader("cached", constant("false")).end()
		;
		
		from("direct:after")
		// set format of the response:
			.to("direct:toText")
			;
				
		
		from("direct:toText")
		.setHeader("Content-Type", constant("text/html; charset=UTF-8"))
//		.setBody(simple("<pre>${body}</pre>"))
//		.throwException(new RuntimeException("lerhoziwrhzehireoihreaooi"))
		;
		
		// not yet implemented:
//		from("direct:toJson")
//		.setHeader("Content-Type", constant("text/html; charset=UTF-8"))
//		;

	    Boolean allowRoutingSlip=false;	    
		if(allowRoutingSlip){
			// testing of recursive routingSlip; not yet implemented:
		    from("jetty:http://0.0.0.0:{{inputport}}/routingSlip/") //?continuationTimeout=3600000")
				.routeId("routingSlip")
		    	.routingSlip(header("routingSlip"))
			;
		}
//		
//	    from("direct:routingSlip")
//	    .routeId("routingSlip")
//	    	// for tracing, but we need a better solution:
////	    	.log("begin of direct:routingSlip")
//	    	//
//	    	// not implemented: if we allow for nextHop routing, we need to implement a process on how to limit the effect of loops: 
////			.choice().when(header("maxHops"))
////				.setHeader("maxHops", simple("${headers.maxHops} - 1"))
////			.otherwise()
////			    .setHeader("maxHops", simple("1"))
////			.end()
//	    	//
//	    	// as long as loop prevention is not implemented, we cannot allow for nextHop routing: 
////	    	.choice().when(header("nextHop"))
////	    	    .setHeader("routingSlip", simple("${headers.nextHop}"))
////	    	    .removeHeader("nextHop")
////	    	.end()
//	    	.routingSlip(header("routingSlip"))
//	    	//
//	    	// as long as loop prevention is not implemented, we cannot allow for nextHop routing: 
////	    	.choice().when(header("nextHop").isNotNull())
////	    	    .setHeader("routingSlip", simple("${headers.nextHop}"))
////	    	    .removeHeader("nextHop")
////	    	    .to("direct:routingSlip")
////	    	.end()
//	    	// for tracing, but we need a better solution:	    	
////	    	.log("end of direct:routingSlip")
//	    	
////	    	.removeHeaders(".*")
//	    	
//	    	// default response: pure text
//	    	.setHeader("Content-Type", constant("text/plain"))
//		;
//	    
//	    from("seda:routingSlip?concurrentConsumers=10")
//	    	.routingSlip(header("routingSlip"))
//	    ;
//	    
//	    from("vm:routingSlip?concurrentConsumers=10")
//	    	.routingSlip(header("routingSlip"))
//	    ;
	    
	    	
		
		//
		// CRUD ROUTES
		//
		
		from("direct:createTemplate")
			.routeId("createTemplate")
//			.log("direct:createTemplate started with template=${headers.templateName}")
			.setHeader(Exchange.FILE_NAME, simple("${headers.templateName}"))
			.to("direct:verifyTemplateName")
			.doTry()
				.to("file:src/main/resources/templates/?autoCreate=true&fileExist=Fail") // Ignore will not overwrite existing files
			    .setHeader("Location", simple("${headers.CamelHttpUrl}"))
				.setHeader("CamelHttpResponseCode", constant("201"))
				.setBody(simple("Template ${headers.templateName} created: href=${headers.CamelHttpUrl}"))
			.doCatch(Exception.class)
				.setHeader("CamelHttpResponseCode", constant("409"))
				.setHeader("Location", simple("${headers.CamelHttpUrl}"))
				.setBody(simple("Template ${headers.templateName} exists already: href=${headers.CamelHttpUrl}"))
			.endDoTry()			
//			.log("direct:createTemplate ended with template=${headers.templateName}")
		;
		
		from("direct:listTemplates")	
			.routeId("listTemplates")			
//			.log("direct:listTemplates started")
			.setHeader("folderList",	simple("templates, src/main/resources/templates"))
			.setHeader("directoryName",	simple("src/main/resources/templates"))
			.bean(FileUtilBeans.class, "listFiles")
//			.throwException(new RuntimeException("errhdpgoiwehrohrhwiohrwod"))
//			.log("direct:listTemplates ended")
		;
		
		
		from("direct:readTemplate")
			.routeId("readTemplate")
//			.log("direct:readTemplate started with template=${headers.templateName}")
			.setHeader("fileName", simple("${headers.templateName}"))
			.doTry()
				.to("direct:verifyTemplateName")
				.setHeader("folderList",	simple("templates, src/main/resources/templates"))
				.bean(FileUtilBeans.class, "readFile")
			    .setHeader("Location", simple("${headers.CamelHttpUrl}"))
				.setHeader("CamelHttpResponseCode", constant("200"))
			.doCatch(Exception.class)
				.setHeader("CamelHttpResponseCode", constant("404"))
				.removeHeader("Location")
				.setBody(simple("404 Not Found: template ${headers.templateName} does not exist"))
			.endDoTry()
//			.log("direct:readTemplate ended with template=${headers.templateName}")
		;
		
		
		
		from("direct:updateTemplate")
			.routeId("updateTemplate")
//			.log("direct:updateTemplate started with template=${headers.templateName}")
			.setHeader(Exchange.FILE_NAME, simple("${headers.templateName}"))
			.to("direct:verifyTemplateName")
			.removeHeader("CamelHttpResponseCode")
			.doTry()
				.to("file:src/main/resources/templates/?autoCreate=true&fileExist=Fail")
				.setHeader("CamelHttpResponseCode", constant("201"))
			    .setHeader("Location", simple("${headers.CamelHttpUrl}"))
				.setBody(simple("Template ${headers.templateName} created: href=${headers.CamelHttpUrl}"))
			.doCatch(Exception.class)
				.to("file:src/main/resources/templates/?autoCreate=true&fileExist=Override") // will always overwrite
				.setHeader("CamelHttpResponseCode", constant("200"))
				.setHeader("Location", simple("${headers.CamelHttpUrl}"))
				.setBody(simple("Template ${headers.templateName} updated: href=${headers.CamelHttpUrl}"))
			.endDoTry()
//			.log("direct:updateTemplate ended with template=${headers.templateName}")
		;
		
		from("direct:deleteTemplate")
			.routeId("deleteTemplate")
//			.log("direct:deleteTemplate started with template=${headers.templateName}")			
			.setHeader("fileName", simple("src/main/resources/templates/${headers.templateName}"))
			.to("direct:verifyTemplateName")
			.setHeader("folderList",	simple("templates, src/main/resources/templates"))
			.bean(FileUtilBeans.class, "deleteFile")
		    .setHeader("Location", simple("${headers.CamelHttpUrl}"))
			.choice()
				.when(body().isEqualTo("true"))
					.setHeader("CamelHttpResponseCode", constant("204"))
					.setBody(constant(null))
				.otherwise()
					.setHeader("CamelHttpResponseCode", constant("404"))
					.setBody(simple("404 Not Found: template ${headers.templateName} does not exist"))
			.end()
//			.log("direct:deleteTemplate ended with template=${headers.templateName}")
		;
		
		//
		// APPLY VELOCITY ROUTES
		//
		
		from("direct:applyTemplate")
			.routeId("applyTemplate")
//			.log("direct:applyTemplate ended with template=${headers.templateName}")
			.choice()
				.when(header("cached").isEqualTo("false"))
					.to("direct:applyTemplateNonCachedWithNativeFileRead")
			.otherwise()
					.to("direct:applyTemplateCached")
			.end()
			.choice().when(header("resolution").isEqualTo("forced"))
				.doTry()
					.bean(VerifyData.class, "verifyTemplateAfter")
					.setHeader("CamelHttpResponseCode", constant("200"))
				    .setHeader("Location", simple("${headers.CamelHttpUrl}"))
				.doCatch(Exception.class)
					.setHeader("CamelHttpResponseCode", constant("404"))
				    .setHeader("Location", simple("${headers.CamelHttpUrl}"))
					.setBody(simple("404 header(s) not found: ${exception.message} (unrecoverable since resolution was set to forced)."))
				.endDoTry()			
			.end()
//			.log("direct:applyTemplate ended with template=${headers.templateName}")			
		;
		
		
		from("direct:applyTemplateCached")
			.routeId("applyTemplateCached")
			.to("direct:verifyTemplateName")
			.setHeader("CamelVelocityResourceUri", simple("templates/${headers.templateName}"))
			.to("velocity:dummy")
		;
		
		// seems to have same performance as direct:applyTemplateCached (which uses CamelVelocityResourceUri instead of a RecipentList):
//		from("direct:applyTemplateCachedWithRecipientList")
//			.routeId("direct:applyTemplateCachedWithRecipientList")
//			.to("direct:verifyTemplateName")
//			.recipientList(simple("velocity:./templates/${headers.templateName}?loaderCache=true&contentCache=true"))
//		;

		// seems to have same performance as direct:applyTemplateCached (which uses CamelVelocityResourceUri instead of a RecipentList):
		from("direct:applyTemplateNonCached")
			.routeId("direct:applyTemplateNonCached")
			.choice()
				.when(header("traditionalVelocityStyle").isEqualTo("true"))
					.to("direct:applyTemplateNonCachedTraditional")
				.otherwise()
					.to("direct:applyTemplateNonCachedWithNativeFileRead")
			.end()
		;
		
		// for testing caching:
//		// hot reloading does not work on Windows (to be tested on Linux):
//		from("direct:applyTemplateNonCachedTraditional")
//			.routeId("direct:applyTemplateNonCachedTraditional")
//			.to("direct:verifyTemplateName")
//			// not sure whether loaderCache=false&contentCache=false works on Linux for dummy endpoints:
//			//			.setHeader("CamelVelocityResourceUri", simple("templates/${headers.templateName}"))
//			//			.to("velocity:dummy?loaderCache=false&contentCache=false")
//			// we know that loaderCache=false&contentCache=false works on Linux for recipientLists:
//			.recipientList(simple("velocity:./templates/${headers.templateName}?loaderCache=false&contentCache=false"))
//		;
		
		
		// hot reloading by native file read (works also on Windows):
		from("direct:applyTemplateNonCachedWithNativeFileRead")
//			.throwException(new RuntimeException("1 direct:applyTemplateNonCachedWithNativeFileRead"))
			.routeId("direct:applyTemplateNonCachedWithNativeFileRead")
//			.throwException(new RuntimeException("2 direct:applyTemplateNonCachedWithNativeFileRead"))
			.to("direct:verifyTemplateName")
//			.throwException(new RuntimeException("3 direct:applyTemplateNonCachedWithNativeFileRead"))
			.to("direct:readTemplate")
//			.throwException(new RuntimeException("4 direct:applyTemplateNonCachedWithNativeFileRead"))
			.to("direct:velocityFromBody")
//			.throwException(new RuntimeException("5 direct:applyTemplateNonCachedWithNativeFileRead"))
		;


		// not yet exposed to the RESTful interface, but used by direct:applyTemplateNonCachedWithNativeFileRead
		from("direct:velocityFromBody")
			.routeId("direct:velocityFromBody")
			.convertBodyTo(String.class)
			.setHeader("CamelVelocityTemplate", simple("${body}"))
			.to("velocity:dummy")
		;
		
//		direct:applyTemplateFromBody
		from("direct:applyTemplateFromBody")
			.routeId("direct:applyTemplateFromBody")
			.convertBodyTo(String.class)
			.setHeader("CamelVelocityTemplate", simple("${body}"))
			.to("velocity:dummy")
			.setHeader("CamelHttpResponseCode", constant("200"))
		    .setHeader("Location", simple("${headers.CamelHttpUrl}"))
			.choice().when(header("resolution").isEqualTo("forced"))
				.doTry()
					.bean(VerifyData.class, "verifyTemplateAfter")
				.doCatch(Exception.class)
					.setHeader("CamelHttpResponseCode", constant("404"))
					.setBody(simple("404 header(s) not found: ${exception.message} (unrecoverable since resolution was set to forced)."))
				.endDoTry()			
			.end()
		;
		// not used yet:
//		from("direct:velocityFromUriInBody")
//			.convertBodyTo(String.class)
//			.setHeader("CamelVelocityResourceUri", simple("${body}"))
//			.to("velocity:dummy")
//		;
				
		//
		// Helper Routes
		//
		
		from("direct:verifyTemplateName")
			.routeId("direct:verifyTemplateName")
			.choice()
				.when(header("templateName").isNull())
					.throwException(new RuntimeException("direct:verifyTemplateName called with null templateName"))
				.when(header("templateName").isEqualTo(""))
					.throwException(new RuntimeException("direct:verifyTemplateName called with empty templateName"))
				.when(header("templateName").contains(".."))
					.throwException(new RuntimeException("direct:verifyTemplateName called with invalid templateName"))
			.end()
		;
		
		// can be used for for concurrency tests, if needed:
//		from("direct:sleep")
//			.bean(Sleep.class)
//			;
		
//		from("direct:applySSH")
//		.routeId("applySSH")
////		.log("direct:applyTemplate ended with template=${headers.templateName}")
//		.to("ssh:username:password@host:port")
////		.choice().when(header("resolution").isEqualTo("forced"))
////			.doTry()
////				.bean(VerifyData.class, "verifyTemplateAfter")
////				.setHeader("CamelHttpResponseCode", constant("200"))
////			    .setHeader("Location", simple("${headers.CamelHttpUrl}"))
////			.doCatch(Exception.class)
////				.setHeader("CamelHttpResponseCode", constant("404"))
////			    .setHeader("Location", simple("${headers.CamelHttpUrl}"))
////				.setBody(simple("404 header(s) not found: ${exception.message} (unrecoverable since resolution was set to forced)."))
////			.endDoTry()			
////		.end()
////		.log("direct:applyTemplate ended with template=${headers.templateName}")			
//	;
        	

    }

}
