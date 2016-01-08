package de.oveits.velocitytemple;



import java.util.Iterator;
import java.util.Map;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Headers;


public class VerifyData {
	//
	// will read files with content of the format
	// Variable1 = Value1
	// Variable2 = Value2
	// ...
	// and write them to the headers of the message
	// header("Variable1") = Value1
	// header("Variable2") = Value2
	//

	public String verifyRegex(@Body Object body, @Headers Map<String,Object> headers) throws Exception {
		return verifyRegex(body.toString(), headers);
	}

//	public String verifyRegex(@Body String body, @Headers Map<String,Object> headers) throws Exception {
	public String verifyRegex(String body, Map<String,Object> headers) throws Exception {

//		Iterator<String> headerIterator = headers.keySet().iterator();
//		while (headerIterator.hasNext()) {
//		    String headerName = headerIterator.next();
//		    System.out.println(headerName + "=" + headers.get(headerName));
//		    
//		    if (headerName.split(".").length >= 2 && headerName.split(".")[headerName.split(".").length - 1].toString().equals("regex")){
//		    	String headerToBeChecked = headerName.substring(0, headerName.split(".")[headerName.split(".").length - 1].length());
//		    	if (headers.get(headerToBeChecked) == null || !headers.get(headerToBeChecked).toString().matches(headers.get(headerName).toString())){
//		    		throw (new Exception(""));
//		    	}
//		    }
//
//		}
		
		Iterator<Map.Entry<String, Object>> mapIterator = headers.entrySet().iterator();
		while (mapIterator.hasNext()) {
		    Map.Entry<String, Object> myHeader = mapIterator.next();
		    String myHeaderRegexName = myHeader.getKey();
		    Object myHeaderRegexValue = myHeader.getValue();
//			// for debugging:
//		    System.out.println(myHeaderRegexName + ": " + myHeaderRegexValue);
//		    if (myHeaderRegexName.matches(".")) {
//		    	System.out.println(myHeaderRegexName + " matches .");
//		    }
//		    if (myHeaderRegexName.matches("\\.")) {
//		    	System.out.println(myHeaderRegexName + " matches \\.");
//		    }
//		    if (myHeaderRegexName.matches(".*")) {
//		    	System.out.println(myHeaderRegexName + " matches .*");
//		    }
//		    if (myHeaderRegexName.matches(".*\\.regex[0-9]*")) {
//		    	System.out.println(myHeaderRegexName + " matches .*\\.regex[0-9]*");
//		    }
			if (myHeaderRegexName.matches(".*\\.regex[0-9]*")) {
				// e.g. if myHeaderRegexName is OSVIP.regex1, then headerToBeChecked is OSVIP:
		    	String headerToBeChecked = myHeaderRegexName.replaceAll(".regex[0-9]*", "");
		    	
		    	// e.g. get value of OSVIP. Returns null, if OSVIP is not defined
		    	Object headerToBeCheckedValue = headers.get(headerToBeChecked);
		    	if (headerToBeCheckedValue == null)
		    		headerToBeCheckedValue = ""; // needed to allow for a regex pattern1|pattern2... with one of the pattern allowing an empty string, e.g. pattern1 = ^$
		    	
		    	if (headerToBeCheckedValue == null || !headerToBeCheckedValue.toString().matches(myHeaderRegexValue.toString())){
		    		String myExceptionText;
		    		if (headers.get(myHeaderRegexName + ".failureText.en") != null)
		    			myExceptionText = headers.get(myHeaderRegexName + ".failureText.en").toString();
		    		else
		    			myExceptionText = headerToBeChecked.toString() + " does not match the regular expression " + myHeaderRegexValue.toString();
		    		
		    		throw (new Exception(myExceptionText));
		    	}
		    		
		    }
		    	
//		    String myHeaderRegexNameArray[] = myHeaderRegexName.split("\\.regex[0-9]*$"); // if headerName = a.bb.ccc, then myHeaderRegexNameArray[] = {a, bb, ccc}
//		    if(myHeaderRegexNameArray.length == 1 )
//		    	if ( myHeaderRegexNameArray[myHeaderRegexNameArray.length - 1].equals("regex")){
//		    	String headerToBeChecked = myHeaderRegexName.substring(0, myHeaderRegexNameArray.length - myHeaderRegexNameArray[myHeaderRegexNameArray.length - 1].length());
//		    	String headerToBeCheckedValue = headers.get(headerToBeChecked).toString();
//		    	if (headerToBeCheckedValue == null || !headerToBeCheckedValue.matches(myHeaderRegexName)){
//		    		throw (new Exception(headerToBeChecked + " does not match the regular expression " + myHeaderRegexValue));
//		    	}
//		    }
		    
		} 
			
		return body; // keep body
	}
	
	public String verifyTemplateAfter (@Body String body, @Headers Map<String,Object> headers) throws Exception {
		//
		// requires:
		// 	header:templateName
		// 	body
		//
		int unresolvedVarStart = body.indexOf("${header");
		int unresolvedVarEnd; // = body.indexOf("}", unresolvedVarStart);
		String unresolvedParams = new String("");
		int maxVariables = 3;
		Boolean trunkated = false;
		int variableMaxlength = 50;
		
		int i=0;
		while(unresolvedVarStart != -1 && i <= maxVariables)
		{
			unresolvedVarEnd = body.indexOf("}", unresolvedVarStart);
			if(body.indexOf("\n", unresolvedVarStart) != -1 && body.indexOf("\n", unresolvedVarStart) < unresolvedVarEnd)
				unresolvedVarEnd = body.indexOf("\n", unresolvedVarStart) - 1;
			if(unresolvedVarEnd == -1) break;
			
			// allow only for very long variable names...
			if(unresolvedVarEnd - unresolvedVarStart > variableMaxlength){
				unresolvedVarEnd = unresolvedVarStart + variableMaxlength;
				trunkated = true;
			} else {
				trunkated = false;
			}
				
			String newUnresolvedParam = body.substring(unresolvedVarStart + 2, unresolvedVarEnd);
			if(trunkated) newUnresolvedParam += "...";
			
			unresolvedVarStart = body.indexOf("${header", unresolvedVarEnd);
			
			// DONE: each variable should be shown only once...
			if(!unresolvedParams.contains(newUnresolvedParam + ",") && !unresolvedParams.endsWith(newUnresolvedParam)){
				if (unresolvedParams.equals(""))
					unresolvedParams += newUnresolvedParam;
				else if(i<maxVariables) unresolvedParams += "," + newUnresolvedParam;
				else unresolvedParams += ",...";
//				if(unresolvedVarStart != -1){
//					unresolvedParams += ",";
//				}
//				if(i == maxVariables) unresolvedParams += ",...";
				i += 1;
			}

		}

		if(!unresolvedParams.isEmpty()){
			String exceptionMessage = null;
			if(headers.containsKey("templateName") && headers.get("templateName") != null)
				exceptionMessage = "Could not resolve following parameters in velocity script " + headers.get("templateName") + ": " + unresolvedParams;
			else {
				exceptionMessage = "Could not resolve following parameters in velocity script: " + unresolvedParams;
			}
			System.out.println("-------------- " + exceptionMessage + " --------------");
			throw (new RuntimeException(exceptionMessage)) ;				
		}
		
		
		return body; // keep body
		
	}
			
}
