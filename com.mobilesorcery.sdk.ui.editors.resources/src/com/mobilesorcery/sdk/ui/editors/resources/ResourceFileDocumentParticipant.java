package com.mobilesorcery.sdk.ui.editors.resources;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitionerExtension3;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.rules.FastPartitioner;

public class ResourceFileDocumentParticipant implements IDocumentSetupParticipant {

	public void setup(IDocument document) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			IDocumentPartitioner partitioner = new FastPartitioner(Activator.getDefault().getResourceFilePartitionScanner(),
					ResourceFilePartitionScanner.PARTITION_TYPES);
			extension3.setDocumentPartitioner(ResourceFilePartitionScanner.PARTITIONING, partitioner);
			((IDocumentPartitionerExtension3)partitioner).connect(document, false);
		}
	}
	
}
