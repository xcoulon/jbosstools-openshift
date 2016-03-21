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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jboss.tools.common.ui.databinding.ObservableUIPojo;

import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IResource;

public abstract class ResourcesUIModel extends ObservableUIPojo implements IResourcesUIModel {

	private static final int NOT_FOUND = -1;
	protected Map<String, List<IResourceUIModel>> resources = new ConcurrentHashMap<>();
	private final Object parent;
	
	protected ResourcesUIModel(Object parent) {
		this.parent = parent;
		Arrays.asList(KINDS).forEach(k->resources.put(k, Collections.synchronizedList(new ArrayList<>())));
	}
	
	protected boolean hasModelFor(IResource resource) {
		if(resources.containsKey(resource.getKind())) {
			List<IResourceUIModel> models = resources.get(resource.getKind());
			return indexOf(models, resource) > NOT_FOUND;
		}
		return false;
	}
	
	@Override
	public Object getParent() {
		return this.parent;
	}

	@Override
	public Collection<IResourceUIModel> getImageStreams() {
		return resources.get(ResourceKind.IMAGE_STREAM);	
	}
	
	@Override
	public void setImageStreams(Collection<IResourceUIModel> models) {
		firePropertyChange(PROP_IMAGE_STREAMS, resources.get(ResourceKind.IMAGE_STREAM), resources.put(ResourceKind.IMAGE_STREAM, new ArrayList<>(models)));
	}
	
	@Override
	public Collection<IResourceUIModel> getDeploymentConfigs() {
		return resources.get(ResourceKind.DEPLOYMENT_CONFIG);	
	}
	
	@Override
	public void setDeploymentConfigs(Collection<IResourceUIModel> models) {
		firePropertyChange(PROP_DEPLOYMENT_CONFIGS, resources.get(ResourceKind.DEPLOYMENT_CONFIG), resources.put(ResourceKind.DEPLOYMENT_CONFIG, new ArrayList<>(models)));
	}
	
	@Override
	public Collection<IResourceUIModel> getBuilds() {
		return resources.get(ResourceKind.BUILD);	
	}
	
	@Override
	public void setBuilds(Collection<IResourceUIModel> builds) {
		firePropertyChange(PROP_BUILDS, resources.get(ResourceKind.BUILD), resources.put(ResourceKind.BUILD, new ArrayList<>(builds)));
	}
	
	@Override
	public Collection<IResourceUIModel> getPods() {
		return resources.get(ResourceKind.POD);
	}
	
	@Override
	public void setPods(Collection<IResourceUIModel> pods) {
		firePropertyChange(PROP_PODS, resources.get(ResourceKind.POD), resources.put(ResourceKind.POD, new ArrayList<>(pods)));
	}

	@Override
	public Collection<IResourceUIModel> getRoutes() {
		return resources.get(ResourceKind.ROUTE);
	}

	@Override
	public void setRoutes(Collection<IResourceUIModel> routes) {
		firePropertyChange(PROP_ROUTES, resources.get(ResourceKind.ROUTE), resources.put(ResourceKind.ROUTE, new ArrayList<>(routes)));
	}
	
	@Override
	public Collection<IResourceUIModel> getReplicationControllers() {
		return resources.get(ResourceKind.REPLICATION_CONTROLLER);
	}
	
	@Override
	public void setReplicationControllers(Collection<IResourceUIModel> rcs) {
		firePropertyChange(PROP_REPLICATION_CONTROLLERS, resources.get(ResourceKind.REPLICATION_CONTROLLER), resources.put(ResourceKind.REPLICATION_CONTROLLER, new ArrayList<>(rcs)));
	}

	@Override
	public Collection<IResourceUIModel> getBuildConfigs(){
		return resources.get(ResourceKind.BUILD_CONFIG);
	}
	
	@Override
	public void setBuildConfigs(Collection<IResourceUIModel> buildConfigs) {
		firePropertyChange(PROP_BUILD_CONFIGS, resources.get(ResourceKind.BUILD_CONFIG), resources.put(ResourceKind.BUILD_CONFIG, new ArrayList<>(buildConfigs)));
	}
	
	@Override
	public Collection<IResourceUIModel> getServices() {
		return resources.get(ResourceKind.SERVICE);
	}

	@Override
	public void setServices(Collection<IResourceUIModel> services) {
		firePropertyChange(PROP_SERVICES, resources.get(ResourceKind.SERVICE), resources.put(ResourceKind.SERVICE, new ArrayList<>(services)));
	}

	@Override
	public void add(IResource resource) {
		if(resource == null) {
			return;
		}
		final String property = getProperty(resource.getKind());
		final List<IResourceUIModel> models = resources.get(resource.getKind());
		final int index = indexOf(models, resource);
		if(models != null && index == NOT_FOUND) {
			List<IResourceUIModel> old = new ArrayList<>(models);
			models.add(new OpenShiftResourceUIModel(resource, this));
			fireIndexedPropertyChange(property, models.size() - 1, old, Collections.unmodifiableList(models));
		}
	}

	@Override
	public void remove(IResource resource) {
		if(resource == null) return;
		final String property = getProperty(resource.getKind());
		List<IResourceUIModel> models = resources.get(resource.getKind());
		if(models != null) {
			int index = indexOf(models, resource);
			if(index > NOT_FOUND) {
				List<IResourceUIModel> old = new ArrayList<>(models);
				models.remove(index);
				fireIndexedPropertyChange(property, index, old, Collections.unmodifiableList(models));
			}
		}
	}

	@Override
	public void update(final IResource resource) {
		if(resource == null) {
			return;
		}
		final String property = getProperty(resource.getKind());
		final List<IResourceUIModel> models = resources.get(resource.getKind());
		final int index = models.indexOf(resource);
		if(index > NOT_FOUND) {
			List<IResourceUIModel> old = new ArrayList<>(models);
			models.set(index, new OpenShiftResourceUIModel(resource, this));
			fireIndexedPropertyChange(property, index, old, Collections.unmodifiableList(models));
		}
	}
	
	/**
	 * 
	 * @param models
	 * @param resource
	 * @return
	 */
	private int indexOf(final List<IResourceUIModel> models, final IResource resource) {
		if(models != null) {
			for (int i = 0; i < models.size(); i++) {
				IResourceUIModel model = models.get(i);
				if(model != null) {
					IResource old = model.getResource();
					if(old.equals(resource)) {
						return i;
					}
				}
			}
		}
		return NOT_FOUND;
	}
	
	private static String getProperty(String kind) {
		return ResourceKind.pluralize(kind);
	}

	public Collection<IResource> getResources(String kind) {
		if (!resources.containsKey(kind)) {
			return Collections.emptyList();
		}
		return resources.get(kind).stream().map(r -> r.getResource()).collect(Collectors.toList());
	}
	
}
