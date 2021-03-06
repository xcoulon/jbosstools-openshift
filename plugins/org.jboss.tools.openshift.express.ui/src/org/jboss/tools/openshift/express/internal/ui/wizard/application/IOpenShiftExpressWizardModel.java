/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard.application;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServerType;
import org.jboss.tools.common.databinding.IObservablePojo;
import org.jboss.tools.openshift.egit.ui.util.EGitUIUtils;
import org.jboss.tools.openshift.express.internal.ui.wizard.IConnectionAwareModel;

import com.openshift.client.ApplicationScale;
import com.openshift.client.IApplication;
import com.openshift.client.ICartridge;
import com.openshift.client.IEmbeddableCartridge;
import com.openshift.client.IGearProfile;
import com.openshift.client.OpenShiftException;

/**
 * @author André Dietisheim
 */
public interface IOpenShiftExpressWizardModel extends IConnectionAwareModel, IObservablePojo {

	public static final String NEW_PROJECT = "enableProject";
	public static final String CONNECTION = "user";
	public static final String APPLICATION = "application";
	public static final String APPLICATION_NAME = "applicationName";
	public static final String APPLICATION_CARTRIDGE = "applicationCartridge";
	public static final String APPLICATION_GEAR_PROFILE = "applicationGearProfile";
	public static final String APPLICATION_SCALE = "applicationScale";
	//public static final String USE_EXISTING_APPLICATION = "uswizardModel.getApplicationName()eExistingApplication";
	public static final String USE_EXISTING_APPLICATION = "useExistingApplication";
	public static final String REMOTE_NAME = "remoteName";
	public static final String REPOSITORY_PATH = "repositoryPath";
	public static final String PROJECT_NAME = "projectName";
	public static final String MERGE_URI = "mergeUri";
	public static final String RUNTIME_DELEGATE = "runtimeDelegate";
	public static final String CREATE_SERVER_ADAPTER = "createServerAdapter";
	public static final String SERVER_TYPE = "serverType";
	public static final String NEW_PROJECT_REMOTE_NAME_DEFAULT = "origin";
	public static final String EXISTING_PROJECT_REMOTE_NAME_DEFAULT = "openshift";
	public static final String DEFAULT_REPOSITORY_PATH = EGitUIUtils.getEGitDefaultRepositoryPath();

	/**
	 * Imports the project that the user has chosen into the workspace.
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws OpenShiftException
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws URISyntaxException
	 * @throws InvocationTargetException
	 * @throws IOException
	 */
	public void importProject(IProgressMonitor monitor) throws OpenShiftException, CoreException, InterruptedException,
			URISyntaxException, InvocationTargetException, IOException;

	/**
	 * Enables the user chosen, unshared project to be used on the chosen
	 * OpenShift application. Clones the application git repository, copies the
	 * configuration files to the user project (in the workspace), shares the
	 * user project with git and creates the server adapter.
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws URISyntaxException
	 *             The OpenShift application repository could not be cloned,
	 *             because the uri it is located at is not a valid git uri
	 * @throws OpenShiftException
	 * 
	 * @throws InvocationTargetException
	 *             The OpenShift application repository could not be cloned, the
	 *             clone operation failed.
	 * @throws InterruptedException
	 *             The OpenShift application repository could not be cloned, the
	 *             clone operation was interrupted.
	 * @throws IOException
	 *             The configuration files could not be copied from the git
	 *             clone to the user project
	 * @throws CoreException
	 *             The user project could not be shared with the git
	 */
	public void configureUnsharedProject(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException;

	/**
	 * Enables the user chosen, unshared project to be used on the chosen
	 * OpenShift application. Clones the application git repository, copies the
	 * configuration files to the user project (in the workspace), adds the
	 * appication git repo as remote and creates the server adapter.
	 * 
	 * @param monitor
	 *            the monitor to report progress to
	 * @throws URISyntaxException
	 *             The OpenShift application repository could not be cloned,
	 *             because the uri it is located at is not a valid git uri
	 * @throws OpenShiftException
	 * 
	 * @throws InvocationTargetException
	 *             The OpenShift application repository could not be cloned, the
	 *             clone operation failed.
	 * @throws InterruptedException
	 *             The OpenShift application repository could not be cloned, the
	 *             clone operation was interrupted.
	 * @throws IOException
	 *             The configuration files could not be copied from the git
	 *             clone to the user project
	 * @throws CoreException
	 *             The user project could not be shared with the git
	 * @throws GitAPIException 
	 * @throws NoWorkTreeException 
	 */
	public void configureGitSharedProject(IProgressMonitor monitor)
			throws OpenShiftException, InvocationTargetException, InterruptedException, IOException, CoreException,
			URISyntaxException, NoWorkTreeException, GitAPIException;

	public File getRepositoryFile();

	public Object setProperty(String key, Object value);

	public Object getProperty(String key);

	public IApplication getApplication();

	public String setApplicationName(String name);

	public String getApplicationName();

	public ICartridge setApplicationCartridge(ICartridge cartridge);

	public ICartridge getApplicationCartridge();

	public void setApplication(IApplication application);

	public String setRemoteName(String remoteName);

	public String getRemoteName();

	public String setRepositoryPath(String repositoryPath);

	public String getRepositoryPath();

	public boolean isNewProject();

	public boolean isExistingProject();

	public Boolean setNewProject(boolean newProject);

	public Boolean setExistingProject(boolean existingProject);

	public String setProjectName(String projectName);

	public IProject setProject(IProject project);

	public boolean isGitSharedProject();

	public Boolean setCreateServerAdapter(Boolean createServerAdapter);

	public String getProjectName();

	public String setMergeUri(String mergeUri);

	public String getMergeUri();

	public IRuntime getRuntime();

	public boolean isCreateServerAdapter();

	public IServerType getServerType();

	public IServerType setServerType(IServerType serverType);

	public boolean isUseExistingApplication();

	public boolean setUseExistingApplication(boolean useExistingApplication);

	public Set<IEmbeddableCartridge> setSelectedEmbeddableCartridges(
			Set<IEmbeddableCartridge> selectedEmbeddableCartridges);

	public Set<IEmbeddableCartridge> getSelectedEmbeddableCartridges();

	public IGearProfile getApplicationGearProfile();

	public IGearProfile setApplicationGearProfile(IGearProfile gearProfile);

	public ApplicationScale getApplicationScale();

	public ApplicationScale setApplicationScale(final ApplicationScale scale);

	public IProject getProject();

}