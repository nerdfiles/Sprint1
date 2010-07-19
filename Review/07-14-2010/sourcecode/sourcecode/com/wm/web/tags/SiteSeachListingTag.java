package com.wm.web.tags;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
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

/**
 * Calls the Google search and renders the content.  The html is rendered using xslt.
 * 
 * @author acaskey
 *
 */
public class SiteSeachListingTag extends SimpleTagSupport {
	private String _query = "";	
	private Integer _start = 0;
	
	public void setQuery(Object query)
	{
		if (query != null)
	   this._query = String.valueOf(query);
	}
	
	public void setStart(Object start)
	{
		if (start != null)
		this._start = Integer.valueOf(String.valueOf(start));
	}
	
	@Override
  	public void doTag() throws JspException, IOException {
  		super.doTag();
  		PageContext context = (PageContext) getJspContext();
  		JspWriter out = context.getOut();   		
        
          try {
      		
          	String filePath = context.getServletContext().getRealPath("/layout/xslt/searchresults.xslt");

      		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      		
      		Properties props = new Properties();
      		String propPath = context.getServletContext().getRealPath("/layout/site.properties");
      		props.load(new FileInputStream(propPath));
      		
            String googleUrl = props.getProperty("googleUrl");
            googleUrl = googleUrl.replaceAll("queryKey", _query);
            googleUrl = googleUrl.replaceAll("startKey", _start.toString());

            URL url = new URL(googleUrl); 
      		
            InputStream stream = url.openStream();
            Document document = docBuilder.parse(stream);      		
          	stream.close();
          	
          	if (document.getElementsByTagName("RES").getLength() > 0) {
          	
	      		File xsltFile = new File(filePath);
	
	      		Source xsltSource = new StreamSource(xsltFile);
	
	      		
				TransformerFactory transFact = TransformerFactory.newInstance();
				Transformer trans = transFact.newTransformer(xsltSource);
				DOMSource source = new DOMSource(document);
				
				StringWriter writer = new StringWriter();
				StreamResult result = new StreamResult(writer);
				trans.transform(source,result);
	      		
				out.println(writer.toString());
          	}
          	else {
          		// write out no results found which comes original from TeamSite
          		JspFragment body = getJspBody(); 
          		StringWriter writer = new StringWriter();
                body.invoke(writer); 
                out.println(writer.toString());
          	}
  		} catch (Exception e) {
  			throw new JspException(e);
  		}
	}
}
