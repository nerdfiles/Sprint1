package com.wm.web.tags;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class provides the base functionality to building a site map based
 * upon the _navigation.xml file store with the content of the site.
 * 
 * @author acaskey
 *
 */
public abstract class NavigationTag extends SimpleTagSupport {
	public static final String DOCUMENT_ATTRIBUTE_NAME = NavigationTag.class.getName()+'.'+"document";
	public static final String NL = System.getProperty("line.separator");
	public static final String EMPTY_STRING = "";
	
	private String _overridePath = EMPTY_STRING;
	private String _overrideUrl = EMPTY_STRING;
	private String _overrideXPath = EMPTY_STRING;
	private Boolean _useXRef = false;
	private Boolean _useReferer = false;
	private Document _document = null;
	
	/**
	 * Determines if the site map should use the refering page instead of the current page
	 * in determining which elements should be selected in the navigation.
	 * 
	 * @param flag true or false
	 */
	public void setUseReferer(Boolean flag)
	{
	   this._useReferer = flag;
	}
	
	/**
	 * Determines if the site map should take the currently URL and look it up in the 
	 * /layout/xml/_navigationXRef.xml instead of the current page
	 * in determining which elements should be selected in the navigation.
	 * 
	 * @param useXRef true or false
	 */
	public void setUseXRef(Boolean useXRef)
	{
	   this._useXRef=useXRef;
	}
	
	/**
	 * This attribute allows you to provide the path to the _navigation.xml file
	 * 
	 * @param overridePath relative path to file
	 */
	public void setOverridePath(String overridePath)
	{
	   this._overridePath=overridePath;
	}	
	
	/**
	 * this attribute allows you to supply a url other than the one currently being 
	 * viewed.  This could be used on the press release detail page which normally 
	 * would not be in the sitemap
	 * 
	 * @param overrideUrl relative url
	 */
	public void setOverrideUrl(String overrideUrl)
	{
	   this._overrideUrl=overrideUrl;
	}	
	
	/**
	 * once the sitemap is built, the class will go through and flag the elements 
	 * that are in the path of the currently selected file.  This override allows 
	 * you to provide an XPath for a location in the sitemap other than the current 
	 * URL being viewed
	 * 
	 * @param overrideXPath valid xpath for xml
	 */
	public void setOverrideXPath(String overrideXPath)
	{
	   this._overrideXPath=overrideXPath;
	}	
	
	/**
	 * search for the _navigation.xml file is the current directory and if not 
	 * found it search the parent directory.
	 * 
	 * @param filePath folder path to look in
	 * 
	 * @return File object contains the file name
	 */
	private static File findFile(String filePath) {
		File file = new File(filePath + "_navigation.xml");
		if (!file.exists()) {
			// does parent exist
			file = new File(filePath + ".." + File.separator + "_navigation.xml");
		}
		return file;
	}
	
	/**
	 * locates and extracts the XML from the _navigation.xml file.  this method calls itself
	 * recursive to a upwards pattern meaning that the current folder location will be 
	 * processed and then the parent will be processed until the top is reached.  the _navigation.xml
	 * files are built in such a way the the folder name of the child is also stored in the parent.
	 * this method only processes the folders that are required so 80% of the time only 3 files will
	 * need to be processed.
	 * 
	 * @param filePath 	path to the starting location
	 * @return			XML fragment of the site map
	 */
	private static String xmlFromFile(String filePath) {
		String xml = null;
		String doc = null;

		if (filePath != null) {
			File file = findFile(filePath);
			boolean exists = file.exists();
			
			if (exists) {
				xml = xmlFromFile(file.getParent() + File.separator + ".." + File.separator);
				String[] contents = getContents(file);
				doc = contents[1];
				if (xml == null) {
					xml = doc;
				} else {
					String root = contents[0];
					if (root.length() > 0) {
						xml = xml.replaceAll(">" + root + "<", "> " + doc + " <");
					}
				}
			}

		}
		return xml;
	}

	/**
	 * loads the content of an _navigation.xml file and returns the contents and the
	 * contents of the <root> element in a String array.
	 * 
	 * @param file 	File object to be loaded
	 * @return		array contain the contents of the file and value of <root>
	 */
	private static String[] getContents(File file) {
		StringBuilder contents = new StringBuilder();
		String root = EMPTY_STRING;
		String[] retVal = { EMPTY_STRING, EMPTY_STRING };
		try {
			BufferedReader input = new BufferedReader(new FileReader(file));
			try {
				String line = null; // not declared within while loop
				
				// the following only works if the crlf are inserted into the
				// xml document after each line.
				while ((line = input.readLine()) != null) {
					if (line.indexOf("siteMap") == -1
							&& line.indexOf("<?xml") == -1) {
						contents.append(line);
						contents.append(NL);
					} else if (line.indexOf("<siteMap") != -1) {
						int i = line.indexOf("root=\"");
						if (i > -1) {
							root = line.substring(i + 6);
							i = root.indexOf("\">");
							root = root.substring(0, i);
						}
					}
				}
			} finally {
				if (input != null)
					input.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		retVal[0] = root;
		retVal[1] = contents.toString();

		return retVal;
	}
	
	/**
	 * this method starts the process of loading the _navigation.xml files into a complete
	 * site map that can then be used to generate html via xslt.  once the site map is built
	 * it is saved for the duration of the request so it can be reused.
	 * 
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	private void initialize() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		PageContext pc = (PageContext) getJspContext();
		HttpServletRequest request = (HttpServletRequest) pc.getRequest();
		ServletContext application = pc.getServletContext();
		String currentUrl = request.getServletPath();

		Document doc = null;
		NodeList selnodeList = null;
		XPath xpath = null;
		XPathExpression expr = null;
		String expression = null;
		
		if (_overrideUrl != null && _overrideUrl.length() > 0) {
			currentUrl = _overrideUrl;
		}
		
		// use the index.jsp as the default url, which is used if the current page
		// cannot be found in the _navigation.xml file
		String defaultUrl = currentUrl;
		int lastIndex = defaultUrl.lastIndexOf('/');
		if (lastIndex > -1) {
			defaultUrl = defaultUrl.substring(0,lastIndex) + "/index.jsp";
		} 
			
		// if the use referer is true then assume that the page is a service detail page
		// and it is being linked from a group page.  in that case, we need to find the
		// combination of the referer and the current page in the site map 
		if (_useReferer)
		{
			if (request.getHeader("referer") != null) {
				String refererUrl = request.getHeader("referer");
				String pageUrl = request.getRequestURL().toString();
				String requestURL = request.getRequestURI();				
				pageUrl = pageUrl.replaceFirst(requestURL, EMPTY_STRING); 
				int workAreaPosition = refererUrl.indexOf("WORKAREA/WebSite/"); //  test for teamsite
				
				if (refererUrl.startsWith(pageUrl) || workAreaPosition > -1) {
					if (workAreaPosition > -1) {
						// check for teamsite because teamsite causes the referer url to be
						// big and ugly.  below is an example of a teamsite url
						// refererUrl = "http://localhost:8080/iw-mount/default/main/WasteManagement/WORKAREA/WebSite/products-and-services/curbside-pick-up/index.jsp";
						refererUrl = refererUrl.substring(workAreaPosition + 16);
					} else {
						refererUrl = refererUrl.replaceFirst(pageUrl, EMPTY_STRING);  		
					}
					
					// build the xpath that will be used to determine if the refer is found
					// in the _navigation.xml file
					String xPath = String.format("//item[@url='%s']/item[@url='%s']",refererUrl,request.getServletPath());

					doc = getDocument(refererUrl, application);
					xpath = XPathFactory.newInstance().newXPath();
					expr = xpath.compile(xPath);
					selnodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
					if (selnodeList == null || selnodeList.getLength() == 0) {
						selnodeList = null;
						
						// if no referer is found then check for cached refererUrl.  If found see if the
						// current page is also referred to by the navigation.
						HttpSession session = request.getSession(true);
						refererUrl = (String) session.getAttribute("cachedRefererUrl");
						if (refererUrl != null) {
							xPath = String.format("//item[@url='%s']/item[@url='%s']",refererUrl,request.getServletPath());

							doc = getDocument(refererUrl, application);
							xpath = XPathFactory.newInstance().newXPath();
							expr = xpath.compile(xPath);
							selnodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
							if (selnodeList == null || selnodeList.getLength() == 0) {
								selnodeList = null;
							}
							else {
								currentUrl = refererUrl;
							}
						}
						
					} else {
						// save the referring url if case the user clicks on a link to another page
						// that is a child under the same referer
						currentUrl = refererUrl;
						HttpSession session = request.getSession(true);
						session.setAttribute("cachedRefererUrl", refererUrl);
					}
				}
			}
		}

		// if selnodeList is still null then that means it has not been populated
		// previously with the referer request to the processing will not use the
		// normal method of searching for the current url in the _navigation.xml
		
		if (selnodeList == null) {
			doc = getDocument(currentUrl, application);
			expression = "//item[@url='" + request.getContextPath() + currentUrl + "']";
			if (_overrideXPath != null && _overrideXPath.length() > 0) {
				expression = _overrideXPath; 
			}
			xpath = XPathFactory.newInstance().newXPath();
			expr = xpath.compile(expression);
	
			// find nodes that are part of the url path and flag them
			// so the xslt can properly tag them for high-lighting purposes
			selnodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		}
		
		// if the current page cannot be found for the current url then look for the index.jsp page
		// in the current path and each parent until found or null

		if (selnodeList == null || selnodeList.getLength() == 0) {
			String path = request.getContextPath() + defaultUrl;
			while ((selnodeList == null || selnodeList.getLength() == 0) && path.length() > 0) {
				expression = "//item[@url='" + path + "']";
				xpath = XPathFactory.newInstance().newXPath();
				expr = xpath.compile(expression);

				// find nodes that are part of the url path
				selnodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
				
				// strip out the last folder path before index.jsp
				String[] pathTokens = path.split("/");
				if (pathTokens.length <= 2)
					break;
				
				// need to skip first entry because it is blank
				path = EMPTY_STRING;
				for(int i=1; i<pathTokens.length-2; i++) 
					path += "/" + pathTokens[i];
				path += "/index.jsp";
			}
		}
		
		// Once the current location is found it needs to be flagged as selected and all parent nodes
		// also need to be flagged.  These flags will be used in the xslt for generating html markup
		
		if (selnodeList != null && selnodeList.getLength() > 0) {
			for(int i=0; i<selnodeList.getLength(); i++) {
				Node selnode = selnodeList.item(i);
			
				boolean firstChild = true;    
				if (selnode != null) {
					while (selnode != null) {
						NamedNodeMap attributes = selnode.getAttributes();
						if (attributes == null) {
							selnode = null;
						} else {
							Attr inPath = doc.createAttribute("selected");
							inPath.setValue("true");
							attributes.setNamedItem(inPath);
							if (!firstChild) {
								Attr childSelected = doc.createAttribute("childSelected");
								childSelected.setValue("true");
								attributes.setNamedItem(childSelected);
							} else {
								firstChild = false;
							}
							selnode = selnode.getParentNode();
						}
					}
				}
			}
		}
		
		
		// The following code is used to pull in a single child level of _navigation.xml when it 
		// needs to  be included even if it is not selected.  This would primary be used in the
		// top navigation when a drop down effect is desired.
		
		expression = "//item[@includeChildren='true']";
		xpath = XPathFactory.newInstance().newXPath();
		expr = xpath.compile(expression);

		// find nodes that are part of the url path
		selnodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		if (selnodeList != null && selnodeList.getLength() > 0) {
			for(int i=0; i<selnodeList.getLength(); i++) {
				Node selnode = selnodeList.item(i);
				String value = selnode.getTextContent();
				
				// Check to see if the content is a path. this is will evaluate to true
				// if the child _navigation.xml for that node hasn't already been pulled
				// in.  this value is replace with xml once the child content is loaded
				
				if (value.startsWith("/") && value.endsWith("/")) {
					String filepath = application.getRealPath(EMPTY_STRING) + value + "_navigation.xml";
					File fileImport = new File(filepath);
					if (fileImport.exists())
					{
		            	selnode.setTextContent(EMPTY_STRING);
						DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			            Document docXml = docBuilder.parse (fileImport);
			            NodeList nodes = docXml.getElementsByTagName("item");
						
						// loop through each of the nodes and individually add them.
			            for(int j=0; j<nodes.getLength(); j++) {
			            	selnode.appendChild( doc.importNode(nodes.item(j), true));
			            }
					}
				}
			}
		}

		
		_document = doc;
		
		// save the object so it can be re-used for the duration of the request.  the bread crumb,
		// left navigation and top navigation all use the same site map content.
		
		request.setAttribute(DOCUMENT_ATTRIBUTE_NAME, doc);
		
	}

	/**
	 * this method is used by classes that inherit from this to retrieve the XML document to
	 * use.  this method first determines if the document has already been created and if
	 * so it will return that otherwise it will build the document
	 * 
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws XPathExpressionException
	 */
	public Document getDocument() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
		if (_document == null) {
			PageContext pc = (PageContext) getJspContext();
			HttpServletRequest request = (HttpServletRequest) pc.getRequest();
			_document = (Document)request.getAttribute(DOCUMENT_ATTRIBUTE_NAME);
			if (_document == null)
				initialize();
		}
		return _document;
	}
	
	/**
	 * returns a XML document based upon the location of the current web page being 
	 * viewed.  the XML document will contain all the nodes that are required to
	 * render a navigation using XSLT.
	 * 
	 * @param currentUrl	the url to current page
	 * @param application	object to servlet
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private Document getDocument(String currentUrl, ServletContext application) throws ParserConfigurationException, SAXException, IOException {
		String filepath = null;
		
		if (_useXRef) {
			filepath = application.getRealPath(EMPTY_STRING) + findApplicationCrossReference(currentUrl);
		} else if (_overridePath != null && _overridePath.length() > 0) {
			filepath = application.getRealPath(EMPTY_STRING) + _overridePath;
		}
		else {
			filepath = application.getRealPath(currentUrl);
			int lastDelimiter = filepath.lastIndexOf(File.separator);
			if (lastDelimiter > -1) {
				filepath = filepath.substring(0, lastDelimiter + 1);
			}
		}
		
		StringBuilder contents = new StringBuilder();
		contents.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
		contents.append("<siteMap>");
		contents.append(xmlFromFile(filepath));
		contents.append("</siteMap>");
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

		InputSource inStream = new InputSource();
		inStream.setCharacterStream(new StringReader(contents.toString()));
		Document doc = docBuilder.parse(inStream);

		return doc;
	}
	
	/**
	 * this method opens the cross reference xml file and looks for the current url. when
	 * it is found the alternate url is return from the open file.
	 * 
	 * @param currentUrl	current url
	 * @return				new url
	 */
	private String findApplicationCrossReference(String currentUrl) {
		String filePath = ((PageContext) getJspContext()).getServletContext().getRealPath(EMPTY_STRING);
		File file = new File(filePath + "/layout/xml/_navigationXRef.xml");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		String path = null;
		
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			
			String expression = "/root/item[@url='" + currentUrl + "']/@folder";
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression expr = xpath.compile(expression);

			// find nodes that are part of the url path
			Node selnode = (Node) expr.evaluate(doc, XPathConstants.NODE);
			
			if (selnode != null) {
				path = selnode.getNodeValue();
			}
			
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return path;
	}

}
