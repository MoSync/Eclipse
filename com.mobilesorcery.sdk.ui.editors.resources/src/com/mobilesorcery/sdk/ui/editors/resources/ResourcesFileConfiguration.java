package com.mobilesorcery.sdk.ui.editors.resources;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;

public class ResourcesFileConfiguration extends SourceViewerConfiguration {

	private ResourcesFileScanner scanner;
	private RuleBasedScanner mlCommentScanner;
	private SyntaxColorPreferenceManager syntaxManager;

	public ResourcesFileConfiguration(ColorManager manager) {
		syntaxManager = Activator.getDefault().getSyntaxColorPreferenceManager();
		scanner = new ResourcesFileScanner(syntaxManager, ResourcesFileScanner.CODE_SCANNER);
		mlCommentScanner = new RuleBasedScanner();
		reinit();
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(ResourceFilePartitionScanner.PARTITIONING);

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		DefaultDamagerRepairer dr2 = new DefaultDamagerRepairer(mlCommentScanner);

		reconciler.setDamager(dr2, ResourceFilePartitionScanner.COMMENT);
		reconciler.setRepairer(dr2, ResourceFilePartitionScanner.COMMENT);

		/*
		 * DefaultDamagerRepairer mlDr = new DefaultDamagerRepairer(scanner);
		 * reconciler.setDamager(mlDr, ResourceFilePartitionScanner.COMMENT);
		 * reconciler.setRepairer(mlDr, ResourceFilePartitionScanner.COMMENT);
		 */

		return reconciler;
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, ResourceFilePartitionScanner.COMMENT };
	}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		   ContentAssistant assistant = new ContentAssistant();		  
		   assistant.enableAutoInsert(true);
		   assistant.enableAutoActivation(true);
		   assistant.setAutoActivationDelay(500);
		   IContentAssistProcessor processor = new ResourceFileContentAssistProcessor();		   
		   assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		   
		   return assistant;
		}

	public void reinit() {
		mlCommentScanner.setDefaultReturnToken(new Token(ResourcesFileScanner.createTextAttribute(syntaxManager, ResourcesFileScanner.COMMENT_COLOR)));
		scanner.reinit();
	}
}