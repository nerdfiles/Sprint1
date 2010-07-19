package com.wm.web.tags;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.io.*;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import com.wm.service.SiteInformation;

/**
 * Displays the bread crumb that is displayed on the web pages.  It is dependent on the 
 * _navigation.xml files that are located throughout the site and the XSLT file
 * for rendering the page.
 * 
 * @author acaskey
 *
 */
public class BreadcrumbsTag extends NavigationTag {
	
	@Override
	public void doTag() throws JspException, IOException {
		super.doTag();
		Document document = null;
		try {
			document = getDocument();
		} catch (Exception e) {
			throw new JspException(e);
		}
        PageContext context = (PageContext) getJspContext();
        try {
    		
        	String filePath = context.getServletContext().getRealPath("");
        	SiteInformation site = new SiteInformation((HttpServletRequest)context.getRequest(), (HttpServletResponse) context.getResponse());

    		File xsltFile = new File(filePath + "/layout/xslt/breadcrumb.xslt");

    		Source xsltSource = new StreamSource(xsltFile);

            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);
            DOMSource source = new DOMSource(document);

            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            trans.setParameter("category", site.getNonBlankSegment());
            trans.transform(source,result);
    		
    		context.getOut().print(writer.toString());			
		} catch (Exception e) {
			throw new JspException(e);
		}
	}
}
