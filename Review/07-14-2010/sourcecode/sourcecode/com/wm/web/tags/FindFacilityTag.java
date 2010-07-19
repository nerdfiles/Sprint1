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
 * Searches ITB for facilities either by zip code or state. The content will be rendered as html using
 * xslt file.
 *  
 * @author acaskey
 *
 */
public class FindFacilityTag extends NavigationTag {
    String _facilityId = null;
    String _zipCode = null;
    String _state = null;
    String _category = "0";
      
  	public void setFacilityId(String facilityId)
	{
	   this._facilityId = facilityId;
	}
    
  	public void setZipCode(String zipCode)
	{
	   this._zipCode = zipCode;
	}
    
  	public void setState(String state)
	{
	   this._state = state;
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

        try {
    	
        	int matchCount = 0;
        	String facilityId = null;
        	String url = null;
        	
    		if (_zipCode != null && _zipCode.length() > 0)
    		{
	        	document = site.getFacilitiesByZipState(_zipCode, null, _category);
    		}
    		else if (_state != null && _state.length() > 0) {
	        	document = site.getFacilitiesByZipState(null, _state, _category);
    		}
    		
        	
        	if (document != null) {
        		
        		// locate number of results returned
        		NodeList list = document.getElementsByTagName("FAC_COUNT");
    			if (list.getLength() == 1) {
    				matchCount = Integer.parseInt(list.item(0).getTextContent());
    				
    				// if result found and zip code then get facility id
    				if (matchCount >= 1 && _zipCode != null && _zipCode.length() > 0) {
    	        		list = document.getElementsByTagName("FAC_IDU");
    	    			if (list.getLength() >= 1) {
    	    				facilityId = list.item(0).getTextContent();
    	    				
    	    				// check to see if the user should be redirected
    	    				list = document.getElementsByTagName("REDIRECT_URL");
    	    				if (list.getLength() >= 1) {
    	    					if (list.item(0).getTextContent().length() > 0) {
    	    						url = list.item(0).getTextContent();
    	    					}
    	    				}
    	    			}
    				}
    			}
        		
    			if (facilityId != null) {
    				if (url == null) {
    					url = String.format("/facility.jsp?zip=%s", _zipCode);
    				}
    				((HttpServletResponse) context.getResponse()).sendRedirect(url);
    				
    			} else if (matchCount > 0) {
        			xsltFile = new File(filePath + "/layout/xslt/findfacilitylisting.xslt");

          		  	Source xsltSource = new StreamSource(xsltFile);

          		  	TransformerFactory transFact = TransformerFactory.newInstance();
          		  	Transformer trans = transFact.newTransformer(xsltSource);
          		  	DOMSource source = new DOMSource(document);

          		  	StringWriter writer = new StringWriter();
          		  	StreamResult result = new StreamResult(writer);
          		  	trans.setParameter("category",String.valueOf(_category));
          		  	trans.setParameter("state", String.valueOf(_state));
          		  	trans.transform(source,result);
          		
          		  	context.getOut().print(writer.toString());
    			}
    			else {
    				context.getOut().print("No Results Found");
    			}
        	}
  		} catch (Exception e) {
  			throw new JspException(e);
  		}
  	}

  }
