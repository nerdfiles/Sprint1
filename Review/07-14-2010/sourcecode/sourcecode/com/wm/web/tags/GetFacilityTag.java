package com.wm.web.tags;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.wm.service.SiteInformation;

/**
 * Returns the facility either by zip code or facility id. If a facility is provided then
 * the search results will be scanned to see if the facility is in the results and if so
 * it will return that one instead of the first.  
 * 
 * @author acaskey
 *
 */
public class GetFacilityTag extends NavigationTag {
    String _facilityId = null;
    String _zipCode = null;
    String _state = null;
    String _category = "0";
    String _xslt = "/layout/xslt/facilitydetail.xslt";

  	public void setXsltFile(String xslt)
	{
	   this._xslt = xslt;
	}
  	
  	public void setFacilityId(String facilityId)
	{
	   this._facilityId = facilityId;
	}
    
  	public void setZipCode(String zipCode)
	{
	   this._zipCode = zipCode;
	}
    
  	public void setCategory(String category)
	{
	   this._category = category;
	}
  	
    @Override
  	public void doTag() throws JspException, IOException {
  		super.doTag();
  		Document document = null;
        PageContext context = (PageContext) getJspContext();
    	SiteInformation site = new SiteInformation((HttpServletRequest)context.getRequest(), (HttpServletResponse) context.getResponse());
      	String filePath = context.getServletContext().getRealPath("");
		File xsltFile = null;
		boolean facilityFound = false;
		boolean facilityProvided = (_facilityId != null && _facilityId.length() > 0);
    	int matchCount = 0;

        try {
    		if (_zipCode != null && _zipCode.length() > 0) {
	    		if (_category == null || _category.length() == 0) {
	    			_category = site.getNonBlankSegment().equals("res") ? "1" : "2";
	    		}
    		
	        	// check to see if the current facility has the service to be displayed
	    		document = site.getFacilitiesByZipState(_zipCode, null, _category);
	    		if (document != null) {
	    			// site.setZipCode(_zipCode);
	        		
	        		// locate number of results returned
	        		NodeList list = document.getElementsByTagName("FAC_COUNT");
	    			if (list.getLength() == 1) {
	    				matchCount = Integer.parseInt(list.item(0).getTextContent());
	    				
	    				// if a zip code is provided and the facility is provided then
	    				// it needs to be checked to see if in was returned in the itb
	    				// results
	    				if (matchCount >= 1) {
	    					list = document.getElementsByTagName("FAC_IDU");
    						if (list.getLength() >= 1) {
    							if (_facilityId != null && _facilityId.length() > 0) {
	        						// loop through facilities and make sure that it is found
		    						for(int i=0; i<list.getLength(); i++) {
		    							if (list.item(i).getTextContent().equals(_facilityId)) {
		    								facilityFound = true;
		    								break;
		    							}
		    						}
	    						}
	    						if (!facilityFound) {
	    							// if the facility desired in not found in the available
	    							// facility for the zip code then use the first item
	    							_facilityId = list.item(0).getTextContent();
	    						}
	    					} 
	    				}
	    			}
	    		}
    		}
        	
    		document = site.getFacilityDetail(_facilityId);
        	if (document != null) {
    			xsltFile = new File(filePath + _xslt);
    			
      		  	Source xsltSource = new StreamSource(xsltFile);

      		  	TransformerFactory transFact = TransformerFactory.newInstance();
      		  	Transformer trans = transFact.newTransformer(xsltSource);
      		  	DOMSource source = new DOMSource(document);

      		  	StringWriter writer = new StringWriter();
      		  	StreamResult result = new StreamResult(writer);
      		  	trans.setParameter("category",String.valueOf(_category));
      		  	trans.setParameter("zip", String.valueOf(_zipCode));
      		  	if (facilityProvided) {
      		  		trans.setParameter("facility", String.valueOf(_facilityId));
      		  	}
      		  	trans.transform(source,result);
      		
      		  	context.getOut().print(writer.toString());        		
        	} else {
        		// it is possible to have no content for a state for a specific category
      		  	context.getOut().print("No Content Found");        		
        	}

  		} catch (Exception e) {
  			throw new JspException(e);
  		}
  	}
}
