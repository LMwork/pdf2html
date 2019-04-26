package pdfApp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceRGBColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceRGBColor;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import pdfApp.PrintImageLocations.Image;

public class PDF2HTML extends PDFTextStripper {
	// output HTML file writer
	public PrintWriter writer;
	// PDF document
	public PDDocument doc = null;
	// current page number
	public int pageCount = 1;

	/**
	 * Try to get PDF title for HTML <title> tag
	 */
	public String getTitle() {
		{
			String titleGuess = doc.getDocumentInformation().getTitle();
			if (titleGuess != null && titleGuess.length() > 0) {
				return titleGuess;
			}
			return "";
		}
	}

	/**
	 * Start HTML document, prepare Javascript
	 */
	public void startHTML() {
		writer.println("<html>");
		writer.println("<head>");
		writer.println("<title>" + getTitle() + "</title>");
		writer.println("<meta id=\"page\" name=\"description\" content=\"" + pageCount + "\">");
		writer.println("<meta id=\"numpage\" name=\"description\" content=\"" + doc.getNumberOfPages() + "\">");
		writer.println("<script>");

		writer.println("function next() {");
		writer.println("var page = parseInt(document.getElementById(\"page\").content)+1;");
		writer.println("if(page > parseInt(document.getElementById(\"numpage\").content)) page -= 1;");
		writer.println("window.location = \"page\" + page + \".html\";}");

		writer.println("function previous() {");
		writer.println("var page = parseInt(document.getElementById(\"page\").content)-1;");
		writer.println("if(page < 1) page += 1;");
		writer.println("window.location = \"page\" + page + \".html\";}");

		writer.println("function goto() {");
		writer.println("var x =  parseInt(document.getElementById(\"currentPage\").value);");
		writer.println("if (x >= 1 && x <= parseInt(document.getElementById(\"numpage\").content)) {");
		writer.println("document.getElementById(\"alert\").innerHTML = \"\";");
		writer.println("window.location = \"page\" + x + \".html\";}");
		writer.println("else");
		writer.println("document.getElementById(\"alert\").innerHTML = \"invalid\";}");

		writer.println("</script>");
		writer.println("</head>");
		writer.println("<body>");
		writer.println("<button type=\"button\" onclick=\"previous()\"><</button>");
		writer.println("<input type=\"text\" id=\"currentPage\" name=\"currentPage\" size=1 value=\"" + pageCount
				+ "\" style=\"text-align:center\" onchange=\"goto()\"> / " + doc.getNumberOfPages());
		writer.println("<button type=\"button\" onclick=\"next()\">></button><div id=\"alert\"></div>");
	}

	/**
	 * Close HTML page
	 */
	public void endHTML() {
		writer.println("</body>");
		writer.println("</html>");
	}

	/**
	 * Main functionality, convert PDF to HTML
	 * 
	 * @param FILENAME
	 *            PDF document file
	 * @throws Exception
	 */
	public void convert(String FILENAME, String outDir) throws Exception {
		try {
			if (outDir.length() > 0) {
				if (FILENAME.contains("\\")) {
					outDir += "\\";
					FILENAME = FILENAME.substring(FILENAME.lastIndexOf("\\") + 1, FILENAME.length());
				} else {
					outDir += "/";
					FILENAME = FILENAME.substring(FILENAME.lastIndexOf("/") + 1, FILENAME.length());
				}
			}
			File input = new File(FILENAME);
			doc = PDDocument.load(input);
			String parentName = outDir + FILENAME.substring(0, FILENAME.length() - 4);
			File parent = new File(parentName);
			if (!parent.exists()) {
				try {
					parent.mkdir();
				} catch (Exception e) {
					// System.out.println(e);
					return;
				}
			}
			// Make separate directory for each page to store images
			PDPageTree allPages = doc.getDocumentCatalog().getPages();
			for (PDPage page : allPages) {
				File dir = new File(parentName + "/" + pageCount);
				if (!dir.exists()) {
					try {
						dir.mkdir();
					} catch (Exception e) {
//						System.out.println(e);
						return;
					}
				}

				writer = new PrintWriter(parentName + "/page" + pageCount + ".html", "UTF-8");
				float height = page.getMediaBox().getHeight();
				height *= 0.95;

				// Extract images to PNG files
				PDResources pdResources = page.getResources();
				int imageCount = 0;
				for (COSName c : pdResources.getXObjectNames()) {
					PDXObject o = pdResources.getXObject(c);
					if (o instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) {
						File file = new File(parentName + "/" + pageCount + "/" + imageCount++ + ".png");
						ImageIO.write(((org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) o).getImage(), "png",
								file);
					}
				}

				PrintImageLocations printer = new PrintImageLocations();
				printer.processPage(page);
				PDF2HTML reader = new PDF2HTML();

				// Add each page to temporary document to extract text
				PDDocument temp = new PDDocument();
				temp.addPage(page);
				reader.getText(temp);
				temp.close();

				startHTML();

				// Add text to HTML
				for (Line line : reader.content) {
					writer.print("<div style=\"position:absolute; left:" + Math.round(line.textPositions.get(0).getX())
							+ "px; top:" + Math.round(line.textPositions.get(0).getY()) + "px; font-size:"
							+ Math.round(line.textPositions.get(0).getFontSizeInPt()) + "px; font-family:"
							+ reader.getFont(line.textPositions.get(0)) + "\">");
					for (String word : line.words) {
						writer.print(word + " ");
					}
					writer.println("</div>");
				}

				// Add images to HTML

				imageCount = 0;
				for (Image image : printer.images) {
					writer.print("<div style=\"position:absolute; left:" + Math.round(image.X) + "px; top:"
							+ Math.round(height - image.Y) + "px;\">");
					writer.print("<img width=" + Math.round(image.width) + "; height=" + Math.round(image.height)
							+ "; src=\"" + pageCount + "/" + imageCount++ + ".png\">");
					writer.println("</div>");
				}
				endHTML();
				writer.close();
				pageCount++;
			}
		} finally {
			if (doc != null) {
				doc.close();
			}
		}
	}

	/**
	 * Represent Line of text with words and their positions
	 *
	 */
	public class Line {
		public ArrayList<TextPosition> textPositions = new ArrayList<TextPosition>();
		public ArrayList<String> words = new ArrayList<String>();
	}

	public ArrayList<Line> content;
	public int lineCount;
	public int imageCount;
	public boolean newLine;

	public PDF2HTML() throws IOException {
		super();
		addOperator(new SetStrokingColorSpace());
		addOperator(new SetNonStrokingColorSpace());
		addOperator(new SetStrokingDeviceCMYKColor());
		addOperator(new SetNonStrokingDeviceCMYKColor());
		addOperator(new SetNonStrokingDeviceRGBColor());
		addOperator(new SetStrokingDeviceRGBColor());
		addOperator(new SetNonStrokingDeviceGrayColor());
		addOperator(new SetStrokingDeviceGrayColor());
		addOperator(new SetStrokingColor());
		addOperator(new SetStrokingColorN());
		addOperator(new SetNonStrokingColor());
		addOperator(new SetNonStrokingColorN());
		content = new ArrayList<Line>();
		lineCount = -1;
		imageCount = 0;
		newLine = true;
	}

	@Override
	protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
		TextPosition firstProsition = textPositions.get(0);
		if (newLine == true) {
			newLine = false;
			content.add(new Line());
			lineCount++;
		}
		content.get(lineCount).textPositions.add(firstProsition);
		content.get(lineCount).words.add(escape(text));
	}

	@Override
	protected void writeLineSeparator() throws IOException {
		super.writeLineSeparator();
		newLine = true;
	}

	private static String escape(String chars) {
		StringBuilder builder = new StringBuilder(chars.length());
		for (int i = 0; i < chars.length(); i++) {
			appendEscaped(builder, chars.charAt(i));
		}
		return builder.toString();
	}

	private static void appendEscaped(StringBuilder builder, char character) {
		// write non-ASCII as named entities
		if ((character < 32) || (character > 126)) {
			int charAsInt = character;
			builder.append("&#").append(charAsInt).append(";");
		} else {
			switch (character) {
			case 34:
				builder.append("&quot;");
				break;
			case 38:
				builder.append("&amp;");
				break;
			case 60:
				builder.append("&lt;");
				break;
			case 62:
				builder.append("&gt;");
				break;
			default:
				builder.append(String.valueOf(character));
			}
		}
	}

	public String getFont(TextPosition textPosition) {
		String font = textPosition.getFont().toString();
		if (font.contains("Agency"))
			return "Agency FB";
		if (font.contains("Antiqua"))
			return "Antiqua";
		if (font.contains("Architect"))
			return "Architect";
		if (font.contains("Arial"))
			return "Arial";
		if (font.contains("BankFuturistic"))
			return "BankFuturistic";
		if (font.contains("BankGothic"))
			return "BankGothic";
		if (font.contains("Blackletter"))
			return "Blackletter";
		if (font.contains("Blagovest"))
			return "Blagovest";
		if (font.contains("Calibri"))
			return "Calibri";
		if (font.contains("Comic"))
			return "Comic Sans MS";
		if (font.contains("Courier"))
			return "Courier";
		if (font.contains("Cursive"))
			return "Cursive";
		if (font.contains("Decorative"))
			return "Decorative";
		if (font.contains("Fantasy"))
			return "Fantasy";
		if (font.contains("Fraktur"))
			return "Fraktur";
		if (font.contains("Frosty"))
			return "Frosty";
		if (font.contains("Garamond"))
			return "Garamond";
		if (font.contains("Georgia"))
			return "Georgia";
		if (font.contains("Helvetica"))
			return "Helvetica";
		if (font.contains("Impact"))
			return "Impact";
		if (font.contains("Minion"))
			return "Minion";
		if (font.contains("Modern"))
			return "Modern";
		if (font.contains("Monospace"))
			return "Monospace";
		if (font.contains("Open"))
			return "Open Sans";
		if (font.contains("Palatino"))
			return "Palatino";
		if (font.contains("Roman"))
			return "Times New Roman";
		if (font.contains("Sans-serif"))
			return "Sans-serif";
		if (font.contains("Serif"))
			return "Serif";
		if (font.contains("Script"))
			return "Script";
		if (font.contains("Swiss"))
			return "Swiss";
		if (font.contains("Times"))
			return "Times";
		if (font.contains("TRoman"))
			return "Times New Roman";
		if (font.contains("Tw"))
			return "Tw Cen MT";
		if (font.contains("Verdana"))
			return "Verdana";
		return "Times New Roman";
	}

}
