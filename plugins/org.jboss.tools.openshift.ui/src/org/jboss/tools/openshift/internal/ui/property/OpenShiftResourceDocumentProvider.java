/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.property;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.editors.text.WorkspaceOperationRunner;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.AbstractDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistryAdapter;
import org.jboss.tools.openshift.common.core.connection.ConnectionsRegistrySingleton;
import org.jboss.tools.openshift.common.core.connection.IConnection;
import org.jboss.tools.openshift.core.connection.Connection;
import org.jboss.tools.openshift.core.connection.ConnectionProperties;
import org.jboss.tools.openshift.internal.common.ui.utils.UIUtils;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.dialog.DismissableNagDialog;

import com.openshift.restclient.IClient;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.resources.IClientCapability;
import com.openshift.restclient.model.IResource;

public class OpenShiftResourceDocumentProvider extends AbstractDocumentProvider {

	private WorkspaceOperationRunner operationRunner;
	private Map<OpenShiftResourceInput, ConnectionListener> inputToListeners = new HashMap<>();

	@Override
	protected IDocument createDocument(Object element) throws CoreException {
		OpenShiftResourceInput input = getInput(element);
		Document document = null;
		if (input != null) {
			IResource resource = input.getResource();
			document = new Document(resource.toJson());
		}
		return document;
	}
	
	@Override
	protected ElementInfo createElementInfo(Object element) throws CoreException {
		OpenShiftResourceInput input = getInput(element);
		if(input != null) {
			synchronized(inputToListeners) {
				ConnectionListener listener = new ConnectionListener(input, this);
				addElementStateListener(listener);
				ConnectionsRegistrySingleton.getInstance().addListener(listener);
				inputToListeners.put(input, listener);
			}
		}
		return super.createElementInfo(element);
	}

	@Override
	protected void disposeElementInfo(Object element, ElementInfo info) {
		OpenShiftResourceInput input = getInput(element);
		if(input != null) {
			synchronized(inputToListeners) {
				ConnectionListener listener = inputToListeners.remove(input);
				if(listener != null) {
					ConnectionsRegistrySingleton.getInstance().removeListener(listener);
					removeElementStateListener(listener);
				}
			}
		}
		super.disposeElementInfo(element, info);
	}

	@Override
	protected synchronized void disconnected() {
	}

	@Override
	protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
		return null;
	}

	@Override
	protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite)
			throws CoreException {
		final OpenShiftResourceInput input = getInput(element);
		if (input == null) {
			return;
		}
		final IProgressService service = PlatformUI.getWorkbench().getProgressService();
		final Connection connection = input.getConnection();
		final IResource resource = input.getResource();
		final UpdateResourceJob updateResourceJob = new UpdateResourceJob(connection, resource, document);
		
		updateResourceJob.schedule();
		service.showInDialog(Display.getCurrent().getActiveShell(), updateResourceJob);
		// In the really really unlikely event the jobs finished before the end of this method call,
		// we need to ensure the dirty flag stays set to true
		if(updateResourceJob.getCaughtException() != null) {
			Display.getDefault().asyncExec(() -> setDirty(element));
			throw new CoreException(OpenShiftUIActivator.statusFactory().errorStatus(updateResourceJob.getCaughtException()));
		}
	}

	@Override
	@SuppressWarnings("restriction")
	protected IRunnableContext getOperationRunner(IProgressMonitor monitor) {
		if (operationRunner == null) {
			operationRunner = new WorkspaceOperationRunner();
		}
		operationRunner.setProgressMonitor(monitor);
		return operationRunner;
	}

	private OpenShiftResourceInput getInput(Object o) {
		return o instanceof OpenShiftResourceInput ? (OpenShiftResourceInput) o : null;
	}

	@Override
	public boolean isReadOnly(Object element) {
		return getInput(element) == null;
	}

	@Override
	public boolean isModifiable(Object element) {
		return getInput(element) != null;
	}

	private void setDirty(Object element) {
		ElementInfo elementInfo = getElementInfo(element);
		if (elementInfo != null) {
			elementInfo.fCanBeSaved = true;
			fireElementDirtyStateChanged(element, true);
		}
	}
	
	private static class UpdateResourceJob extends Job {
		
		/** The OpenShift connection. */
		private final Connection connection;
		
		/** the IResource to update. */
		private final IResource resource;
		
		/** The {@link IDocument} containing the new resource representation. */
		private final IDocument document;
		
		/** the updated version of the IResource. */
		private IResource updatedResource;

		/** The caught exception if something went wrong during the update. */
		private Exception caughtException;
		
		/**
		 * Constructor
		 * @param connection 
		 */
		UpdateResourceJob(final Connection connection, final IResource resource, final IDocument document) {
			super("Updating " + resource.getName());
			this.connection = connection;
			this.resource = resource;
			this.document = document;
		}
		
		IResource getUpdatedResource() {
			return updatedResource;
		}
		
		Exception getCaughtException() {
			return caughtException;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				final IClient client = resource.accept(new CapabilityVisitor<IClientCapability, IClient>() {
					@Override
					public IClient visit(IClientCapability cap) {
						return cap.getClient();
					}
				}, null);
				this.updatedResource = client.update(connection.getResourceFactory().create(document.get()));
			} catch (Exception e) {
				this.caughtException = e;
				final String problem = getErrorMessage(e);
				final IStatus error = OpenShiftUIActivator.statusFactory()
						.errorStatus(NLS.bind("Could not update \"{0}\" for project \"{1}\" : {2}",
								new String[] { resource.getName(), resource.getNamespace(), problem }), e);
				return error;
			}
			return Status.OK_STATUS;			}

		private String getErrorMessage(Exception e) {
			if (e instanceof OpenShiftException) {
				final OpenShiftException oe = (OpenShiftException)e;
				if (oe.getStatus()!=null) {
					return oe.getStatus().getMessage();
				}
			}
			return e.getMessage();
		}
	}
	
	private static class ConnectionListener extends ConnectionsRegistryAdapter implements IElementStateListener {

		private final OpenShiftResourceInput input;
		private final OpenShiftResourceDocumentProvider provider;
		private AtomicBoolean dirty = new AtomicBoolean(false);
		private AtomicBoolean nag = new AtomicBoolean(true);
		private static final String NAG_MESSAGE = "The resource {0} has been changed on the server.  Do you want to replace the editor contents with these changes.";
		private DismissableNagDialog dialog;
		private AtomicReference<IResource> resource = new AtomicReference<IResource>();
		
		ConnectionListener(OpenShiftResourceInput input, OpenShiftResourceDocumentProvider provider){
			this.input = input;
			this.provider = provider;
			dialog = new DismissableNagDialog(UIUtils.getShell(), "File Changed", NLS.bind(NAG_MESSAGE, input.getResource().getName()));
			resource.set(input.getResource());
		}
		
		@Override
		public void elementDirtyStateChanged(Object element, boolean isDirty) {
			dirty.set(isDirty);
		}

		@Override
		public void connectionChanged(IConnection connection, String property, Object oldValue, Object newValue) {
			if (ConnectionProperties.PROPERTY_RESOURCE.equals(property)) {
				if (input.getConnection() == null || !input.getConnection().equals(connection))
					return;
				if (oldValue != null && newValue != null) {
					IResource resource = (IResource) newValue;
					if(!this.resource.get().equals(resource)){
						return;
					}
					//get updates incase of multiple changes while the dialog is open
					this.resource.set(resource); 
					if(dialog.isOpen()) {
						return;
					}
					// update
					Display.getDefault().asyncExec(() -> {
						if(!dirty.get()) {
							updateEditor(this.resource.get());
						}else if(nag.get()){
							switch(dialog.open()) {
							case DismissableNagDialog.ALWAYS:
								nag.set(false);
							case DismissableNagDialog.YES:
								break;
							case DismissableNagDialog.NO:
								return;
							}
						}
						updateEditor(this.resource.get());
					});
				}
			}
		}
		
		private void updateEditor(IResource resource) {
			input.setResource(resource);
			try {
				provider.resetDocument(input);
			} catch (Exception e) {
				OpenShiftUIActivator.getDefault().getLogger().logError(e);
				provider.setDirty(input);
				throw new RuntimeException(e);
			}
		}

		@Override
		public void elementContentAboutToBeReplaced(Object element) {
		}

		@Override
		public void elementContentReplaced(Object element) {
		}

		@Override
		public void elementDeleted(Object element) {
		}

		@Override
		public void elementMoved(Object originalElement, Object movedElement) {
		}

	}
}
