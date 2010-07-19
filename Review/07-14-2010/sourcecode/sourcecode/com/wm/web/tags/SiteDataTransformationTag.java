package com.wm.web.tags;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
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

/***
 * Transforms a XML file using the provide XSLT file.
 * 
 * @author acaskey
 *
 */
public class SiteDataTransformationTag extends SimpleTagSupport {
	private String _xmlPath = "";
	private String _xslPath = "";

	public void setXmlPath(String path)
	{
	   this._xmlPath = path;
	}
	
	public void setXslPath(String path)
	{
	   this._xslPath = path;
	}	
	
    @Override
  	public void doTag() throws JspException, IOException {
  		super.doTag();
   		
          PageContext context = (PageContext) getJspContext();
          try {
      		
				String filePath = context.getServletContext().getRealPath("");
				
				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
				Document document = docBuilder.parse (new File(filePath + _xmlPath));
				
				File xsltFile = new File(filePath + _xslPath);
				
				Source xsltSource = new StreamSource(xsltFile);
				
				PageContext ctx = (PageContext) getJspContext();
				HttpServletRequest request = (HttpServletRequest) ctx.getRequest();
				String currentUrl = request.getRequestURL().toString();
				String requestURL = request.getRequestURI();
				
				currentUrl = currentUrl.replaceFirst(requestURL, "");
      		
				TransformerFactory transFact = TransformerFactory.newInstance();
				Transformer trans = transFact.newTransformer(xsltSource);
				DOMSource source = new DOMSource(document);
				
				StringWriter writer = new StringWriter();
				StreamResult result = new StreamResult(writer);
				trans.setParameter("contextRoot", currentUrl);
				trans.transform(source,result);
      		
				context.getOut().print(writer.toString());			
  			} catch (Exception e) {
  			throw new JspException(e);
  		}
  	}
}
