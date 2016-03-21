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

import java.beans.PropertyChangeListener;
import java.util.Collection;

import org.eclipse.ui.services.IDisposable;
import org.jboss.tools.openshift.core.connection.IOpenShiftConnection;

import com.openshift.restclient.model.IProject;

public interface IProjectAdapter extends IDisposable {
	
	static final String PROP_DEPLOYMENTS = "deployments";
	
	IOpenShiftConnection getConnection();
	
	IProject getProject();
	
	/**
	 * Calling marks this 
	 */
	void setDeleting(boolean deleting);
	
	/**
	 * determine if this project is being deleted. 
	 * @return
	 */
	boolean isDeleting();

	Collection<Deployment> getDeployments();

	void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);
	
	void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);

}
