/*

    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
package org.dash.valid.report;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.dash.valid.handler.HaplotypePairFileHandler;
import org.dash.valid.handler.HaplotypePairWarningFileHandler;
import org.xml.sax.SAXException;

public class SummaryWriter {	
	private static SummaryWriter instance = null;
	private static Logger FILE_LOGGER = Logger.getLogger(SummaryWriter.class.getName());

	static {
		try {
			FILE_LOGGER.addHandler(new HaplotypePairFileHandler());
			FILE_LOGGER.addHandler(new HaplotypePairWarningFileHandler());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private SummaryWriter() {
		
	}
	
	public static SummaryWriter getInstance() {
		if (instance == null) {
			instance = new SummaryWriter();
		}
		
		return instance;
	}
	/**
	 * @param linkagesFound 
	 */
	public synchronized void reportDetectedLinkages(DetectedLinkageFindings findings) {				
		String linkages = formatDetectedLinkages(findings);
	
		if (findings.hasAnomalies()) {
			FILE_LOGGER.warning(linkages);
		}
		else {
			FILE_LOGGER.info(linkages);
		}
	}

	public String formatDetectedLinkages(DetectedLinkageFindings findings) {
        JAXBContext context;
        StringWriter writer = new StringWriter();
        
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); 
        
        try {
        	URL url = SummaryWriter.class.getClassLoader().getResource("schema/LinkageFindings.xsd");
        	Schema schema = sf.newSchema(url);
        	context = JAXBContext.newInstance(DetectedLinkageFindings.class);

	        Marshaller m = context.createMarshaller();
	        //for pretty-print XML in JAXB
	        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	        
	        m.setSchema(schema);
	
	        m.marshal(findings, writer);
        }
        catch (JAXBException | SAXException e) {
        	e.printStackTrace();
        }
        
        return writer.toString();
	}
}
