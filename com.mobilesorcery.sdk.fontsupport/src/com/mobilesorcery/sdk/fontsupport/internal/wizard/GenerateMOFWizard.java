/*  Copyright (C) 2009 Mobile Sorcery AB

    This program is free software; you can redistribute it and/or modify it
    under the terms of the Eclipse Public License v1.0.

    This program is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public License v1.0 for
    more details.

    You should have received a copy of the Eclipse Public License v1.0 along
    with this program. It is also available at http://www.eclipse.org/legal/epl-v10.html
*/
package com.mobilesorcery.sdk.fontsupport.internal.wizard;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.mobilesorcery.sdk.core.Util;
import com.mobilesorcery.sdk.fontsupport.MOF;

public class GenerateMOFWizard extends Wizard {

	class GenerateMOFRunnable implements IRunnableWithProgress {
		private File fontFile;
		private File mofFile;
		private RGB fontColor;

		public GenerateMOFRunnable(File fontFile, File mofFile, RGB fontColor) {
			this.fontFile = fontFile;
			this.mofFile = mofFile;
			this.fontColor = fontColor;
		}
		
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			try {
				monitor.beginTask(Messages.GenerateMOFWizard_0, 2);
				MOF.generate(new SubProgressMonitor(monitor, 1), fontFile, mofFile, fontColor);
				
				IFile[] refreshThis = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(mofFile.getAbsolutePath()));
				for (int i = 0; i < refreshThis.length; i++) {
					refreshThis[i].getParent().refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(monitor, 1));
				}
			} catch (Exception e) {
				throw new InvocationTargetException(e, MessageFormat.format(Messages.GenerateMOFWizard_1, e.getMessage()));
			} finally {
				monitor.done();					
			}
		}
	}
	
	public class MOFConfigPage extends WizardPage {
		private File file;
		private Text outputFile;
		private ColorSelector color;


		protected MOFConfigPage(String pageName) {
			super(pageName);
			setNeedsProgressMonitor(true);
			setTitle(Messages.GenerateMOFWizard_2);
			setDescription(Messages.GenerateMOFWizard_3);
		}

		public void createControl(Composite parent) {
			Composite main = new Composite(parent, SWT.NONE);
			main.setLayout(new GridLayout(2, false));
			
			Label fileLabel = new Label(main, SWT.NONE);
			fileLabel.setText(Messages.GenerateMOFWizard_4);
			Label fileLocationLabel = new Label(main, SWT.NONE);
			fileLocationLabel.setText(file.getAbsolutePath());

			Label outputFileLabel = new Label(main, SWT.NONE);
			outputFileLabel.setText(Messages.GenerateMOFWizard_5);
			outputFile = new Text(main, SWT.BORDER);
			outputFile.setText(computeDefaultOutputFile(file));
			outputFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			Label colorLabel = new Label(main, SWT.NONE);
			colorLabel.setText(Messages.GenerateMOFWizard_6);
			color = new ColorSelector(main);
			color.setColorValue(MOF.DEFAULT_COLOR);
			
			setControl(main);
		}

		private String computeDefaultOutputFile(File inputFile) {
			inputFile.getName();
			return Util.replaceExtension(inputFile.getName(), MOF.MOF_FILE_EXTENSION);
		}

		public void setFile(File file) {
			this.file = file;
		}
		
		public String getOutputFile() {
			return this.outputFile.getText();
		}
		
		public RGB getOutputColor() {
			return color.getColorValue(); 
		}
	}

	private MOFConfigPage mofConfigPage;
	private File file;

	public GenerateMOFWizard() {
		super();
	}
	
	public void addPages() {
		mofConfigPage = new MOFConfigPage(Messages.GenerateMOFWizard_7);
		mofConfigPage.setFile(file);
		addPage(mofConfigPage);
	}
	
	public boolean performFinish() {
		String output = mofConfigPage.getOutputFile();
		File outputFile = Util.relativeTo(file, output);
		RGB color = mofConfigPage.getOutputColor();
		GenerateMOFRunnable mofRunnable = new GenerateMOFRunnable(file, outputFile, color);
		try {
			mofConfigPage.setErrorMessage(null);
			this.getContainer().run(true, true, mofRunnable);
			return true;
		} catch (Exception e) {
			String errorMessage = MessageFormat.format(
					Messages.GenerateMOFWizard_8,
					e.getMessage());
			mofConfigPage.setErrorMessage(errorMessage);
			return false;
		} 		
	}

	public void setFilename(File osFile) {
		this.file = osFile;
	}

}
