package pdfApp;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.BorderFactory;
import javax.swing.JButton;

public class GUI {

	private JFrame frame;
	private JTextField inputPath, outputPath;
	private JLabel inputLabel, outputLabel, statusBar;

	/**
	 * Launch the application.
	 */
	public void run() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI window = new GUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 490, 240);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setResizable(false);

		statusBar = new JLabel();
		statusBar.setBorder(BorderFactory.createLineBorder(Color.black));
		statusBar.setBounds(0, 180, 484, 25);
		statusBar.setText("Select pdf file");
		frame.getContentPane().add(statusBar);

		inputLabel = new JLabel();
		inputLabel.setBounds(10, 10, 100, 21);
		inputLabel.setText("Input File");
		frame.getContentPane().add(inputLabel);

		inputPath = new JTextField();
		inputPath.setBounds(85, 10, 290, 21);
		frame.getContentPane().add(inputPath);
		inputPath.setColumns(10);

		JButton inputBrowse = new JButton("Browse");
		inputBrowse.setBounds(385, 8, 87, 23);
		frame.getContentPane().add(inputBrowse);

		inputBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();

				fileChooser.setFileFilter(new FileFilter() {
					public String getDescription() {
						return "Portable Document Format (*.pdf)";
					}

					@Override
					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						} else {
							String filename = f.getName().toLowerCase();
							return filename.endsWith(".pdf");
						}
					}
				});

				fileChooser.setAcceptAllFileFilterUsed(false);

				int rVal = fileChooser.showOpenDialog(null);
				if (rVal == JFileChooser.APPROVE_OPTION) {
					inputPath.setText(fileChooser.getSelectedFile().toString());
					if (outputPath.getText().length() > 0)
						statusBar.setText("Ready for conversion");
					else
						statusBar.setText("Select output directory (optional)");
					statusBar.setForeground(Color.BLACK);
				}
			}
		});

		outputLabel = new JLabel();
		outputLabel.setBounds(10, 50, 100, 21);
		outputLabel.setText("Output File");
		frame.getContentPane().add(outputLabel);

		outputPath = new JTextField();
		outputPath.setBounds(85, 50, 290, 21);
		frame.getContentPane().add(outputPath);
		outputPath.setColumns(10);

		JButton outputBrowse = new JButton("Browse");
		outputBrowse.setBounds(385, 48, 87, 23);
		frame.getContentPane().add(outputBrowse);

		outputBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setAcceptAllFileFilterUsed(false);

				int rVal = fileChooser.showOpenDialog(null);
				if (rVal == JFileChooser.APPROVE_OPTION) {
					outputPath.setText(fileChooser.getSelectedFile().toString());
				}
				if (inputPath.getText().length() > 0)
					statusBar.setText("Ready for conversion");
				else
					statusBar.setText("Select pdf file");
				statusBar.setForeground(Color.BLACK);
			}
		});

		JButton convert = new JButton("Convert");
		convert.setBounds(200, 100, 100, 21);
		frame.getContentPane().add(convert);

		convert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (inputPath.getText().length() == 0) {
					statusBar.setText("Select pdf file");
					statusBar.setForeground(Color.RED);
				} else {
					PDF2HTML converter;
					try {
						converter = new PDF2HTML();
						converter.convert(inputPath.getText(), outputPath.getText());
						statusBar.setText("Successful conversion");
						statusBar.setForeground(Color.BLACK);
					} catch (Exception e1) {
						statusBar.setText("Invalid pdf file");
						statusBar.setForeground(Color.RED);
						e1.printStackTrace();
					}
				}
			}
		});
	}
}