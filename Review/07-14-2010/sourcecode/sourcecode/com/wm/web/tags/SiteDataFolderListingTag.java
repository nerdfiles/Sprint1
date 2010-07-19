package com.wm.web.tags;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.wm.service.SiteInformation;

/**
 * Scans a folder and provides a listing of the content as XML.
 * 
 * @author acaskey
 *
 */
public class SiteDataFolderListingTag extends SimpleTagSupport {
	private String _folder = "";
	private int _depth = 1;
	private String _xml = null;
	private String _xsltFile = null;
	private String _param = null;
	
	public void setXsltFile(String file)
	{
	   this._xsltFile = file;
	}
	
	public void setParam(Object param)
	{
		if (param != null)
			this._param = String.valueOf(param);
	}	
	
	public void setFolder(String folder)
	{
	   this._folder = folder;
	}

	public void setDepth(int depth)
	{
	   this._depth = depth;
	}
	
    @Override
  	public void doTag() throws JspException, IOException {
  		super.doTag();

        PageContext context = (PageContext) getJspContext();
		HttpServletRequest request = (HttpServletRequest) context.getRequest();
		
        try {
    		String key = _folder + "|" + String.valueOf(_depth);
      		if (_xml == null) {
      			_xml = (String)request.getAttribute(key);
    			if (_xml == null) {
    	        	  SiteInformation site = new SiteInformation((HttpServletRequest)context.getRequest(), (HttpServletResponse)context.getResponse());
    	        	  
    	          		// write out no results found which comes original from TeamSite
    	          		JspFragment body = getJspBody(); 
    	          		String xmlSnippet = "";
    	          		if (body != null ) {
	    	          		StringWriter writer = new StringWriter();
	    	                body.invoke(writer); 
	    	                xmlSnippet = writer.toString();
    	          		}
    	        	  
    			      _xml = "<root>" + site.getFolderListing(_folder, _depth) + xmlSnippet + "</root>";
    				request.setAttribute(key, _xml);
    			}
      		}        	  
      		
          	String filePath = context.getServletContext().getRealPath(_xsltFile);

      		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

    		InputSource inStream = new InputSource();
    		inStream.setCharacterStream(new StringReader(_xml));
    		Document document = docBuilder.parse(inStream);
          	
      		Source xsltSource = new StreamSource(new File(filePath));
      		
			TransformerFactory transFact = TransformerFactory.newInstance();
			Transformer trans = transFact.newTransformer(xsltSource);
			DOMSource source = new DOMSource(document);
			
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			
			if (_param != null) {
				String[] params = _param.split("\\|");
				for(int i=0; i < params.length; i++) {
					trans.setParameter("param" + String.valueOf(i+1), params[i]);
				}
			}
			
			trans.transform(source, result);
      		context.getOut().print(writer.toString());	      		
  		} catch (Exception e) {
  			throw new JspException(e);
  		}
  	}
}
