package pdfApp;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.PDFStreamEngine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;

/**
 * Modified from PDFBox example by Ben Litchfield
 */
public class PrintImageLocations extends PDFStreamEngine {
	/**
	 * Represent image with location, dimensions and id
	 *
	 */
	public static class Image {
		double X, Y, width, height;
		int id;
	}

	public ArrayList<Image> images = new ArrayList<Image>();
	int imageCount = 0;

	/**
	 * Default constructor.
	 *
	 * @throws IOException
	 *             If there is an error loading text stripper properties.
	 */
	public PrintImageLocations() throws IOException {
		addOperator(new Concatenate());
		addOperator(new DrawObject());
		addOperator(new SetGraphicsStateParameters());
		addOperator(new Save());
		addOperator(new Restore());
		addOperator(new SetMatrix());
	}

	/**
	 * This is used to handle an operation.
	 *
	 * @param operator
	 *            The operation to perform.
	 * @param operands
	 *            The list of arguments.
	 *
	 * @throws IOException
	 *             If there is an error processing the operation.
	 */
	@Override
	protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
		String operation = operator.getName();
		if ("Do".equals(operation)) {
			COSName objectName = (COSName) operands.get(0);
			PDXObject xobject = getResources().getXObject(objectName);
			if (xobject instanceof PDImageXObject) {
				Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();
				images.add(new Image());
				images.get(imageCount).width = ctmNew.getScalingFactorX();
				images.get(imageCount).height = ctmNew.getScalingFactorY();
				images.get(imageCount).X = ctmNew.getTranslateX();
				images.get(imageCount).Y = ctmNew.getTranslateY();
				images.get(imageCount).id = imageCount;
				imageCount++;
			} else if (xobject instanceof PDFormXObject) {
				PDFormXObject form = (PDFormXObject) xobject;
				showForm(form);
			}
		} else {
			super.processOperator(operator, operands);
		}
	}
}