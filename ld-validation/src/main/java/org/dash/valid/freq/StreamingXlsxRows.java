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
package org.dash.valid.freq;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Streams the first sheet of an XLSX file row by row via POI's low-level SAX (event) API,
 * instead of the full in-memory DOM that WorkbookFactory.create()/XSSFWorkbook build. That DOM
 * needs on the order of 10x a file's uncompressed XML size in heap, which made the larger NMDP
 * nine-locus reference files (some ~150-170MB compressed) impractical to read at all: even a
 * mid-sized 57MB file passed 2GB RSS and ran 15+ minutes without finishing the parse, still
 * short of processing a single row.
 *
 * Each row is delivered as a plain List&lt;String&gt; of cell text in column order
 * (blank/missing cells as empty strings, sized to that row's own last populated column),
 * pre-resolved exactly the way POI's own Cell would resolve it for that XML cell type --
 * numeric cells (t absent, or t="n") as the raw &lt;v&gt; text (the same text
 * Cell.getNumericCellValue() itself parses as a double, so callers get identical precision,
 * not a display-formatted approximation), "s" cells resolved via the shared strings table,
 * "str"/"inlineStr" cells as their literal text, and other types passed through as-is -- so
 * callers can keep working in terms of column index the same way they did against POI's
 * Row/Cell, just calling Double.parseDouble() themselves where they'd previously called
 * getNumericCellValue().
 */
class StreamingXlsxRows {

	interface RowHandler {
		void row(int rowNum, List<String> cellValues);
	}

	static void read(InputStream inStream, RowHandler handler) throws IOException {
		try {
			OPCPackage pkg = OPCPackage.open(inStream);
			try {
				XSSFReader reader = new XSSFReader(pkg);

				SharedStrings sharedStrings;
				try {
					sharedStrings = reader.getSharedStringsTable();
				}
				catch (Exception e) {
					// Not every workbook has a sharedStrings.xml part -- the NMDP nine-locus
					// release's files don't (they use inline strings exclusively). Absence
					// isn't an error here, it just means no "s"-typed cell will ever turn up
					// below for a file that lacks one.
					sharedStrings = null;
				}

				SAXParserFactory parserFactory = SAXParserFactory.newInstance();
				parserFactory.setNamespaceAware(true);
				SAXParser saxParser = parserFactory.newSAXParser();

				Iterator<InputStream> sheets = reader.getSheetsData();
				if (sheets.hasNext()) {
					InputStream sheetStream = sheets.next();
					try {
						saxParser.parse(sheetStream, new RowContentHandler(sharedStrings, handler));
					}
					finally {
						sheetStream.close();
					}
				}
			}
			finally {
				pkg.close();
			}
		}
		catch (OpenXML4JException | SAXException | ParserConfigurationException e) {
			throw new IOException("Failed to stream XLSX rows", e);
		}
	}

	private static class RowContentHandler extends DefaultHandler {
		private final SharedStrings sharedStrings;
		private final RowHandler handler;

		private List<String> currentRow;
		private int currentRowNum;

		private boolean inCell;
		private boolean inValue;
		private String cellType;
		private int cellColumn;
		private final StringBuilder valueBuffer = new StringBuilder();

		RowContentHandler(SharedStrings sharedStrings, RowHandler handler) {
			this.sharedStrings = sharedStrings;
			this.handler = handler;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if ("row".equals(localName)) {
				currentRow = new ArrayList<String>();
				String rowAttr = attributes.getValue("r");
				currentRowNum = rowAttr != null ? Integer.parseInt(rowAttr) : currentRowNum + 1;
			}
			else if ("c".equals(localName)) {
				inCell = true;
				cellType = attributes.getValue("t");
				String cellRef = attributes.getValue("r");
				cellColumn = cellRef != null ? new CellReference(cellRef).getCol() : currentRow.size();
				valueBuffer.setLength(0);
			}
			else if (inCell && ("v".equals(localName) || "t".equals(localName))) {
				// A cell holds either exactly one <v> (numeric/shared-string/boolean/error)
				// or one or more <t> runs nested in <is> (inline rich text, possibly split
				// across multiple runs); either way, accumulating into one per-cell buffer
				// and committing it once at </c> handles both correctly.
				inValue = true;
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) {
			if (inValue) {
				valueBuffer.append(ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) {
			if ("v".equals(localName) || "t".equals(localName)) {
				inValue = false;
			}
			else if ("c".equals(localName)) {
				inCell = false;
				commitCellValue();
			}
			else if ("row".equals(localName)) {
				handler.row(currentRowNum, currentRow);
				currentRow = null;
			}
		}

		private void commitCellValue() {
			String text = valueBuffer.toString();

			if ("s".equals(cellType) && sharedStrings != null) {
				int idx = Integer.parseInt(text);
				RichTextString richText = sharedStrings.getItemAt(idx);
				text = richText == null ? "" : richText.getString();
			}

			while (currentRow.size() <= cellColumn) {
				currentRow.add("");
			}
			currentRow.set(cellColumn, text);
		}
	}
}
