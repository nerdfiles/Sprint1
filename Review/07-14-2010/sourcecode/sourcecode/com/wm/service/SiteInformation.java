package com.wm.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class contains methods that are used to extract data from ITB and will as other
 * site specific actions.
 * 
 * @author acaskey
 *
 */
public class SiteInformation {
	final String SERVICE_IDS_KEY = "serviceIds";
	final String SITE_ID_KEY = "siteId";
	final String EMPTY_STRING = "";
	
	private HttpServletRequest _request = null;
	private HttpServletResponse _response = null;
	private HttpSession _session = null;
	
	String _segment = EMPTY_STRING;
	String _zipCode = EMPTY_STRING;
	String _facility = EMPTY_STRING;
	String _cookie = null;
	
	/**
	 * Returns the segment if one is known otherwise is will default to residential
	 * 
	 * @return the chosen segment (res, smb, ent)
	 */
	public String getNonBlankSegment() {
		getSiteInfo();
		if (_segment.length() > 0)
			return _segment;
		else
			return "res";
	}	
	
	public String getSegment() {
		return _segment;
	}
	
	public void setSegment(String segment) {
		_segment = segment;
		setSiteInfo();
	}
	
	public String getZipCode() {
		return _zipCode;
	}
	
	public void setZipCode(String zipCode) {
		_session.removeAttribute(SERVICE_IDS_KEY);
		_zipCode = zipCode;
		setSiteInfo();
	}
	
	public String getFacility() {
		return _facility;
	}
	
	public void setFacility(String facility) {
		_session.removeAttribute(SERVICE_IDS_KEY);
		_facility = facility;
		setSiteInfo();
	}
	
	private void setSiteInfo() {
		String cookieValue = _segment + "|" + _zipCode + "|" + _facility;
    	Cookie cookie = new Cookie(SITE_ID_KEY, cookieValue);
    	cookie.setPath("/");
    	cookie.setMaxAge(31536000);
    	this._response.addCookie(cookie);
    	this._session.setAttribute(SITE_ID_KEY,  cookieValue);
	}
	
	private void getSiteInfo() {
		String cookie = null;
		
		if (this._session.getAttribute(SITE_ID_KEY) != null) {
			cookie = (String)this._session.getAttribute(SITE_ID_KEY);
		}
		else if (_cookie == null) {
	    	cookie = getCookieValue(SITE_ID_KEY);
		}
		
    	if (cookie != null) {
    		String[] tokens = cookie.split("\\|");
    		if (tokens.length > 0) _segment = tokens[0]; else _segment = EMPTY_STRING;
    		if (tokens.length > 1) _zipCode = tokens[1]; else _zipCode = EMPTY_STRING;
    		if (tokens.length > 2) _facility = tokens[2]; else _facility = EMPTY_STRING;
    	}
	}	
	
	/**
	 * This method is used to determine if the app is running in teamsite or
	 * on the WAS servers and then return a prefix that can be used with property
	 * file to pull values out specific to the environment. This assumes that
	 * Tomcat is used for TeamSite.   
	 * 
	 * @return the prefix for the current environment
	 */
	public String getPropertyPrefix()
	{
		if (System.getProperty("catalina.home") != null)
			return "teamsite.";
		else
			return "runtime.";
	}
	
	/**
	 * Gets a single property value from /layout/site.properties file without having to load 
	 * the property file in a Properties object and then reading it.  This should only be used
	 * when only one property is needed.  If multiple properties are going to be requested the
	 * the properties should be stored in code and extracted without re-reading the file each time.
	 * 
	 * @param name	property name to retrieve
	 * @return		value of property
	 */
	public String getSiteProperty(String name) {
		return (getSiteProperties()).getProperty(name);
	}
	
	/**
	 * Loads the /layout/site.properties file into a Properties object.
	 * 
	 * @return a Properties object
	 */
	@SuppressWarnings("deprecation")
	public Properties getSiteProperties()
	{
		Properties props = new Properties();
  		String propPath = _request.getRealPath("/layout/site.properties");
  		try {
			props.load(new FileInputStream(propPath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
  		return props;
	}
	
	/**
	 * Initializes the class and set the segment if it can be found in the query string.
	 * 
	 * @param request	web request object 
	 * @param response	web response object
	 */
    public SiteInformation(HttpServletRequest request, HttpServletResponse response) {
    	this._request = request;
    	this._session = request.getSession();
    	this._response = response;

		getSiteInfo();

    	String seg = _request.getParameter("seg");
    	if (seg != null) {
    		this.setSegment(seg);
    	}   
    	
    	//String fac = _request.getParameter("fac");
    	//if (fac != null) {
    	//	this.setFacility(fac);
    	//}        	
    }	

    /**
	 * Returns XML fragment on the files found in a single folder of the web site.  It
	 * does this by navigating the directory structure and for each file found calling a
	 * web http head request to get additional metadata about the file.  This method will 
	 * only navigate folders that contain web pages. 
	 * 
	 * @param path	the relative path to the root of the site where the listing should start
	 * @return		an xml fragment that needs to be enclosed in parent tag (i.e. <root>) to become valid
     */
	public String getFolderListing(String path) {
		return getFolderListing(path, 1);
	}
	
	/**
	 * Returns XML fragment on the files found in the folders of the web site.  It
	 * does this by navigating the directory structure and for each file found calling a
	 * web http head request to get additional metadata about the file.  This method will 
	 * only navigate folders that contain web pages.
	 * 
	 * @param path	the relative path to the root of the site where the listing should start
	 * @param depth	determine how many sub folders to recurse into 
	 * @return		an xml fragment that needs to be enclosed in parent tag (i.e. <root>) to become valid
	 */
    @SuppressWarnings("deprecation")
	public String getFolderListing(String path, int depth) {
    	
    	// don't allow calls when a HEAD is being executed otherwise if may go 
    	// into an infinite loop
    	if (_request.getMethod().equals("HEAD"))
    		return EMPTY_STRING;
    	
    	StringBuilder sb = new StringBuilder();
    	String filePath = _request.getRealPath(EMPTY_STRING);
    	
		String currentUrl = _request.getRequestURL().toString();
		String requestURL = _request.getRequestURI();

		currentUrl = currentUrl.replaceFirst(requestURL, EMPTY_STRING);            	
    	
		File file = new File(filePath + path);
		File[] children = file.listFiles(); 

		if (children == null) { 
			// Either directory does not exist or is not a directory 
		} 
		else { 
			for (int cnt=0; cnt<children.length; cnt++) { 
				String fileName = children[cnt].getName();
				
				if (children[cnt].isDirectory() && depth > 1 && !fileName.equals("WEB-INF") && !fileName.equals("pods") && !fileName.equals("_assets")  && !fileName.equals("promos")  && !fileName.equals("layout")  && !fileName.equals("apps")  && !fileName.equals("META-INF")) {
					sb.append(getFolderListing(path + fileName + "/", depth - 1));
					
				} else if (children[cnt].isFile() && !fileName.equals("_navigation.xml") && !fileName.equals("header.jsp") && !fileName.equals("footer.jsp")) {
					sb.append(getHeadXmlFragment(currentUrl, path + fileName));
				}		    		
				
			} 
		}
		return sb.toString();
    }
    
    /**
     * Returns an XML fragment about a single file on the web by making a HTTP HEAD request to 
     * the file and storing the head values in a XML fragment.
     * 
     * @param urlPath		domain to call (i.e. http://www.wm.com)
     * @param urlFragment	the relative path to file (i.e. /about/index.jsp)
     * @return				an xml fragment containing the head values
     */
    public String getHeadXmlFragment(String urlPath, String urlFragment) {
    	StringBuilder sb = new StringBuilder();
    	HttpURLConnection  conn = null;
    	
		try {
			URL url = new URL(urlPath + urlFragment);
			
			conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("HEAD");
			
			boolean dateFound = false;
			boolean lastModifiedFound = false;
			boolean dateValueFound = false;
			String lastModifiedDate = EMPTY_STRING;
			String dateValue = EMPTY_STRING;

			sb.append("<url path=\"" + urlFragment + "\">");
		   
		    for (int i=0; ; i++) 
		    {
	            String name = conn.getHeaderFieldKey(i);
	            String value = conn.getHeaderField(i);
	            
	            if (name == null && value == null) {
	              break;         
	            }
	            if (name == null) {
	            
	            }
	            else {
	            	if (name.equals("Content-Date")) {
	            		dateFound = true;
	            		sb.append("<header name=\"" + name + "\">" + value + "</header>");
	            	} else if (name.equals("Last-Modified")) {
	            		lastModifiedFound = true;
	            		lastModifiedDate = getDateString(value);
	            		sb.append("<header name=\"" + name + "\">" + value + "</header>");
	            	} else if (name.equals("Date")) {
	            		dateValueFound = true;
	            		dateValue = getDateString(value);
	            		sb.append("<header name=\"" + name + "\">" + value + "</header>");
	            	} else if (name.equals("Content-Segment") || name.equals("Content-Type") || name.equals("Content-Length") || name.equals("Content-Language")) {
	            		sb.append("<header name=\"" + name + "\">" + value + "</header>");
	            	} else if (name.startsWith("Content-")) {
	            		sb.append("<header name=\"" + name + "\"><![CDATA[" + value + "]]></header>");
	            	}
	            }
		    }
			      
	        if (!dateFound) {
	        	if (lastModifiedFound) {
		        	sb.append("<header name=\"Content-Date\">" + lastModifiedDate + "</header>");
	        	}
	        	else if (dateValueFound) {
		        	sb.append("<header name=\"Content-Date\">" + dateValue + "</header>");
	        	}
	        }
	        
	        sb.append("</url>");
		      
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}   
		finally {
			if (conn != null)
				conn.disconnect();
		}
		
		return sb.toString();
    }
	
    /**
     * Returns an XML Document for the facility stored in the cookie from ITB
     * 
     * @return	a Document object 
     */
    public Document getFacilityDetail() {
    	return getFacilityDetail(_facility);
    }
    
    /**
     * Returns an XML Document for the facility from ITB.  The ITB information is pulled 
     * from a web page and comes back as XML content.  The XML is first read in as a string
     * and then trim because of formatting issues that can occur with invalid content. 
     * 
     * @param facilityId	the a facility id
     * @return				a Document object
     */
 	public Document getFacilityDetail(String facilityId) {
  		Document document = null;
  		InputStream stream = null;
  		BufferedReader inputReader = null;
  		
		String currentUrl = getSiteProperty("itbXmlUrl");
		
		if (facilityId != null && facilityId.length() > 0) {
			currentUrl += "?facIdu=" + facilityId;
		}
		else {
			return null;
		}
		
		System.out.println("ITB Request: " + currentUrl);
		
  		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
  		DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			
	        URL url = new URL(currentUrl); 

	        inputReader = new BufferedReader(
					new InputStreamReader(
							url.openStream()));
	        
			String inputLine;
			StringBuilder contents = new StringBuilder();
	
			while ((inputLine = inputReader.readLine()) != null)
				contents.append(inputLine);
			inputReader.close();
			inputReader = null;
			
			String c = contents.toString().trim();
	
			InputSource inStream = new InputSource();
			inStream.setCharacterStream(new StringReader(c));
			document = docBuilder.parse(inStream);
					
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		finally {
			if (inputReader != null) {
				try {
					inputReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return document;
    }
    
 	/**
 	 * Returns an XML Document from a file stored on the file system.  The files
 	 * are normally stored in the /layout/ folder and the filePath can be any
 	 * valid path accessible by the server.
 	 * 
 	 * @param filePath	the full path and file name to be loaded
 	 * @return			a Document object
 	 */
	@SuppressWarnings("unused")
	private Document getXmlFile(String filePath) {
  		Document document = null;

    	File file = new File(filePath);
    	if (file.exists()) {
	    	DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	  		DocumentBuilder docBuilder = null;
			try {
				docBuilder = docBuilderFactory.newDocumentBuilder();
				
				InputSource inStream = new InputSource();
				inStream.setCharacterStream(new FileReader(filePath));
				document = docBuilder.parse(inStream);   
				
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
  		}
    	return document;
    }

	/**
	 * Returns a string containing the customer service phone number for the zip code
	 * that is currently stored in the cookie.
	 * 
	 * @return zip code or empty string
	 */
	public String getCustSrvPhone() {
		return getCustSrvPhone(_zipCode);
	}
	
	/**
	 * Returns a string containing the customer service phone number stored in ITB for the
	 * provided zip code.
	 * 
	 * @param zip 	a U.S. zip code or Candian postal code
	 * @return		zip code or empty string
	 */
	public String getCustSrvPhone(String zip) {
  		BufferedReader inputReader = null;
		String currentUrl = null;
		StringBuilder contents = new StringBuilder();
		
		if (zip != null && zip.length() > 0) {
			currentUrl = getSiteProperty("itbCsPhoneUrl") + "?zip=" + zip;
	
			System.out.println("ITB Request: " + currentUrl);
			
			try {
				URL url = new URL(currentUrl);
		        inputReader = new BufferedReader(
						new InputStreamReader(
								url.openStream()));
	
				String inputLine;
	
				while ((inputLine = inputReader.readLine()) != null)
					contents.append(inputLine);
				inputReader.close();		
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}

		String number = contents.toString().trim();
		if (number.length() == 10) {
			number = number.replaceAll("(\\d{3})-*(\\d{3})-*(\\d{4})", "$1-$2-$3");
		}
		return number;
	}	
	
	/**
	 * Returns an XML Document object containing the search results from ITB.  A search
	 * can either be on a zip code or stage but not both.
	 * 
	 * @param zip		zip code to search for
	 * @param state		state abbreviation to search for
	 * @param category	the category to search (i.e. landfill, commerical, etc)
	 * @return			a Document object
	 */
	public Document getFacilitiesByZipState(String zip, String state, String category) {
		Document document = null;
		String currentUrl = null;
		InputStream stream = null;
		
		if (zip != null && zip.length() > 0) {
			currentUrl = getSiteProperty("itbListingByZipUrl") + "?zip=" + zip;
		}
		else if (state != null && state.length() > 0) {
			currentUrl = getSiteProperty("itbListingByStateUrl") + "?state=" + state;
		}
		else {
			return null;
		}

		// default to all categories if none is passed
		if (category == null)
			category = "0";
		
		currentUrl += "&servCat=" + category;
		
		System.out.println("ITB Request: " + currentUrl);
		
  		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
  		DocumentBuilder docBuilder;
		try {
	        URL url = new URL(currentUrl); 
			docBuilder = docBuilderFactory.newDocumentBuilder();
	        stream = url.openStream();
	        document = docBuilder.parse(stream);      		
					
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		return document;
	}
	
	/**
	 * Converts a ISO date to a simple date for lends itself to better sorting capabilities.
	 * 
	 * @param dateText	ISO date as provide from a web site
	 * @return			a reformatted date
	 */
    private String getDateString(String dateText) {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMMM yyyy HH:mm:ss z");
    	SimpleDateFormat strFormat = new SimpleDateFormat("yyyy-MM-dd");
    	Date date = null;
    	String returnDate = EMPTY_STRING;
		try {
			date = dateFormat.parse(dateText);
			returnDate = strFormat.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return returnDate;
    }

    /**
     * Request the value of a cookie from the request object
     * 
     * @param cookieName	name of cookie
     * @return				value of cookie
     */
	private String getCookieValue(String cookieName)
	{
		Cookie cookies[] = this._request.getCookies();
		if (cookies != null) {
			for (int i=0; i<cookies.length; i++) {
				if (cookies[i].getName().equals(cookieName)) {
					return cookies[i].getValue();
				}
			}
		}
		return null;
	}    
}
