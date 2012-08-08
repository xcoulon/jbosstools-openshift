package org.jboss.tools.openshift.ui.bot.test.explorer;

import java.io.File;

import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.jboss.tools.openshift.ui.bot.util.OpenShiftUI;
import org.jboss.tools.openshift.ui.bot.util.TestProperties;
import org.jboss.tools.ui.bot.ext.SWTTestExt;
import org.jboss.tools.ui.bot.ext.condition.NonSystemJobRunsCondition;
import org.jboss.tools.ui.bot.ext.types.IDELabel;
import org.junit.Before;
import org.junit.Test;

/**
 * Domain creation consists of creating the SSH key pair, storing user password
 * in the secure storage and creating the domain itself.
 * 
 * @author sbunciak
 * 
 */
public class CreateDomain extends SWTTestExt {

	private boolean keyAvailable = false;

	@Before
	public void prepareSSHPrefs() {
		// clear dir from libra stuff

		File sshDir = new File(System.getProperty("user.home") + "/.ssh");

		if (sshDir.exists() && sshDir.isDirectory()
				&& sshDir.listFiles().length > 0) {
			for (File file : sshDir.listFiles()) {
				if (file.getName().contains("id_rsa"))
					keyAvailable = true;
			}
		}

	}

	@Test
	public void canCreateDomain() throws InterruptedException {
		// open OpenShift Explorer
		SWTBotView openshiftExplorer = open
				.viewOpen(OpenShiftUI.Explorer.iView);

		openshiftExplorer.bot().tree()
				.getTreeItem(TestProperties.get("openshift.user.name"))
				.contextMenu(OpenShiftUI.Labels.EXPLORER_CREATE_EDIT_DOMAIN)
				.click();

		bot.waitForShell(OpenShiftUI.Shell.DOMAIN);

		SWTBotText domainText = bot.text(0);

		assertTrue("Domain should not be set at this stage!", domainText
				.getText().equals(""));

		domainText.setText(TestProperties.get("openshift.domain"));
		log.info("*** OpenShift SWTBot Tests: Domain name set. ***");

		if (keyAvailable) {

			assertTrue("SSH key should be set!",bot.text(1).getText().contains("id_rsa"));
			
		} else {
			throw new UnsupportedOperationException(
					"Creation of ssh key not implemented yet.");
			/*
			 * bot.button(IDELabel.Shell.NEW).click();
			 * bot.waitForShell(OpenShiftUI.Shell.NEW_SSH);
			 * bot.text(0).setText(TestProperties.getPassphrase());
			 * bot.button(IDELabel.Button.OK).click();
			 * bot.waitForShell(OpenShiftUI.Shell.DOMAIN);
			 * 
			 * log.info("*** OpenShift SWTBot Tests: SSH Keys created. ***");
			 */
		}

		bot.button(IDELabel.Button.FINISH).click();

		// wait while the domain is being created
		bot.waitUntil(Conditions.shellCloses(bot.activeShell()), TIME_60S);

		log.info("*** OpenShift SWTBot Tests: Domain created. ***");

		bot.waitUntil(new NonSystemJobRunsCondition(), TIME_20S);
	}

}
