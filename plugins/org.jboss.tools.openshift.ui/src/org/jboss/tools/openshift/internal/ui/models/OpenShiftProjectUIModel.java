/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.internal.ui.models;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.jboss.tools.openshift.common.core.IRefreshable;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IBuild;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IProject;
import com.openshift.restclient.model.IResource;

/**
 * UI Model for an OpenShift Project
 * @author jeff.cantrill
 *
 */
public class OpenShiftProjectUIModel extends ResourcesUIModel implements IProjectAdapter, IResourceUIModel, IRefreshable, PropertyChangeListener{
	
	public static final String PROP_LOADING = "loading";
	
	private final IDeploymentResourceMapper mapper;
	private final IProject project;
	private AtomicBoolean deleting = new AtomicBoolean(false);

	public OpenShiftProjectUIModel(IOpenShiftConnection conn, IProject project) {
		super(conn);
		this.project = project;
		this.mapper = new DeploymentResourceMapper(conn, this);
		this.mapper.addPropertyChangeListener(PROP_DEPLOYMENTS, this);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		mapper.dispose();
	}
	
	@Override
	public void setDeleting(boolean deleting) {
		this.deleting.set(deleting);
	}

	@Override
	public boolean isDeleting() {
		return deleting.get();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt  instanceof IndexedPropertyChangeEvent) {
			fireIndexedPropertyChange(evt.getPropertyName(), ((IndexedPropertyChangeEvent)evt).getIndex(), evt.getOldValue(), evt.getNewValue());
		}else {
			firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
		}
			
	}

	@Override
	public Collection<Deployment> getDeployments() {
		return mapper.getDeployments();
	}

	@Override
	public void setDeployments(Collection<Deployment> deployment) {
	}

	@Override
	public void refresh() {
		mapper.refresh();
	}
	
	@Override
	public IProject getProject() {
		return this.project;
	}


	@Override
	public IResource getResource() {
		return getProject();
	}

	public boolean isLoading() {
		return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends IResource> void setResources(Collection<T> resources, String kind) {
		switch(kind) {
		case ResourceKind.BUILD:
			setBuilds(init((Collection<IBuild>) resources));
			break;
		case ResourceKind.BUILD_CONFIG:
			setBuildConfigs(init((Collection<IResource>) resources));
			break;
		case ResourceKind.DEPLOYMENT_CONFIG:
			setDeploymentConfigs(init((Collection<IResource>) resources));
			break;
		case ResourceKind.IMAGE_STREAM:
			setImageStreams(init((Collection<IResource>) resources));
			break;
		case ResourceKind.POD:
			setPods(init((Collection<IPod>) resources));
			break;
		case ResourceKind.ROUTE:
			setRoutes(init((Collection<IResource>) resources));
			break;
		case ResourceKind.REPLICATION_CONTROLLER:
			setReplicationControllers(init((Collection<IResource>) resources));
			break;
		case ResourceKind.SERVICE:
			setServices(init((Collection<IResource>) resources));
			break;
		default:
		}
	}

	/**
	 * Converts the given {@link List} of {@link IResource} into a {@link List} of {@link IResourcesUIModel}
	 * @param resources the list of items to wrap into {@link IResourceUIModel}
	 * @return the output list
	 */
	private <T extends IResource> List<IResourceUIModel> init(Collection<T> resources) {
		if(resources != null) {
			return resources.stream().map(r->new OpenShiftResourceUIModel(r,this)).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}
	
}
