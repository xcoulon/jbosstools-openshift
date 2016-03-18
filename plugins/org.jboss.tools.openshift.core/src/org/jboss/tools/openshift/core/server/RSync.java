/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.openshift.core.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.Executors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServer;
import org.jboss.tools.openshift.internal.core.OpenShiftCoreActivator;
import org.jboss.tools.openshift.internal.core.preferences.OCBinary;

import com.openshift.restclient.OpenShiftContext;
import com.openshift.restclient.OpenShiftException;
import com.openshift.restclient.capability.CapabilityVisitor;
import com.openshift.restclient.capability.IBinaryCapability;
import com.openshift.restclient.capability.resources.IRSyncable;
import com.openshift.restclient.capability.resources.IRSyncable.LocalPeer;
import com.openshift.restclient.capability.resources.IRSyncable.PodPeer;
import com.openshift.restclient.model.IPod;
import com.openshift.restclient.model.IService;

public class RSync extends OCBinaryOperation {

	private IService service;
	private String podPath;
	private IServer server;

	public RSync(IService service, String podPath, IServer server) {
		this.service = service;
		this.podPath = sanitizePath(podPath);
		this.server = server;
	}

	private static String sanitizePath(String path) {
		if (path == null) {
			return null;
		}
		if (path.endsWith("/") || path.endsWith("/.")) {
			return path;
		}
		return path+"/";
	}
	
	
	public void syncPodsToDirectory(final File deployFolder, final MultiStatus status, final OutputStream outputStream) {
		String location = OCBinary.getInstance().getLocation();
		OpenShiftContext.get().put(IBinaryCapability.OPENSHIFT_BINARY_LOCATION, location);

		
		// If our deploy folder is empty, sync all pods to this directory
		boolean shouldSync = true;
		//boolean shouldSync = !deployFolder.exists() || deployFolder.listFiles().length == 0; 
		if (shouldSync) {
			for (IPod pod : service.getPods()) {
				try {
					syncPodToDirectory(pod, podPath, deployFolder, outputStream);
				} catch (IOException | OpenShiftException e) {
					status.add(new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, e.getMessage()));
				}
			}
		}
	}

	// Sync the directory back to all pods
	public void syncDirectoryToPods(final File deployFolder, final MultiStatus status, final OutputStream outputStream) {
		String location = OCBinary.getInstance().getLocation();
		OpenShiftContext.get().put(IBinaryCapability.OPENSHIFT_BINARY_LOCATION, location);

		for (IPod pod : service.getPods()) {
			try {
				syncDirectoryToPod(pod, deployFolder, podPath, outputStream);
			} catch (IOException | OpenShiftException e) {
				status.add(new Status(IStatus.ERROR, OpenShiftCoreActivator.PLUGIN_ID, e.getMessage()));
			}
		}
	}
	
	
	@Override
	@Deprecated
	protected void runOCBinary(MultiStatus status) {
		// Deprecated?  Doesn't fit the workflow really since we have to split the functionality
	}

	private void syncPodToDirectory(final IPod pod, final String podPath, final File destination,
			final OutputStream outputStream) throws IOException {
		destination.mkdirs();
		String destinationPath = sanitizePath(destination.getAbsolutePath());
		pod.accept(new CapabilityVisitor<IRSyncable, IRSyncable>() {
			@SuppressWarnings("resource")
			@Override
			public IRSyncable visit(IRSyncable rsyncable) {
				final InputStream syncStream = rsyncable.sync(new PodPeer(podPath, pod),
						new LocalPeer(destinationPath));
				asyncWriteLogs(syncStream, outputStream);
				try {
					rsyncable.await();
				} catch (InterruptedException e) {
					OpenShiftCoreActivator.logError("Thread interrupted while running rsync", e);
					Thread.currentThread().interrupt();
				}
				return rsyncable;
			}

			
		}, null);
	}

	private void syncDirectoryToPod(final IPod pod, final File source, final String podPath,
			final OutputStream outputStream) throws IOException {
		String sourcePath = sanitizePath(source.getAbsolutePath());
		pod.accept(new CapabilityVisitor<IRSyncable, IRSyncable>() {
			@Override
			public IRSyncable visit(IRSyncable rsyncable) {
				rsyncable.sync(new LocalPeer(sourcePath), new PodPeer(podPath, pod));
				try {
					rsyncable.await();
				} catch (InterruptedException e) {
					OpenShiftCoreActivator.logError("Thread interrupted while running rsync", e);
					Thread.currentThread().interrupt();
				}
				return rsyncable;
			}
		}, null);
	}
	
	/**
	 * Asynchronously writes the logs from the 'rsync' command, provided by the
	 * given {@code syncStream} into the given {@code outputStream}.
	 * 
	 * @param syncStream the {@link InputStream} to read from
	 * @param outputStream the {@link OutputStream} to write into
	 */
	private static void asyncWriteLogs(final InputStream syncStream, final OutputStream outputStream) {
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
			try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(syncStream));
					final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);) {
				String line;
					while ((line = bufferedReader.readLine()) != null) {
						outputStreamWriter.write(line);
					}
			}
			} catch(IOException e)  {
				OpenShiftCoreActivator.logError("Error occurred while printing 'rsync' command output", e);
			}

		});
	}

	private static void writeLogs(final InputStream syncStream, final OutputStream outputStream) throws IOException {
		
	}

}