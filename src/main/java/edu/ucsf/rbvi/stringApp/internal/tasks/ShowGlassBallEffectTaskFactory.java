package edu.ucsf.rbvi.stringApp.internal.tasks;

import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

public class ShowGlassBallEffectTaskFactory extends AbstractNetworkViewTaskFactory {

	final StringManager manager;

	public ShowGlassBallEffectTaskFactory(final StringManager manager) {
		this.manager = manager;
	}

	public boolean isReady(CyNetworkView netView) {
		if (netView == null)
			return false;
		return ModelUtils.isStringNetwork(netView.getModel());
	}

	public TaskIterator createTaskIterator(CyNetworkView netView) {
		return new TaskIterator(new ShowGlassBallEffectTask(manager, netView, this));
	}

	public void reregister() {
		manager.unregisterService(this, NetworkViewTaskFactory.class);
		Properties props = new Properties();
		props.setProperty(PREFERRED_MENU, "Apps.STRING");
		if (manager.showGlassBallEffect()) {
			props.setProperty(TITLE, "Disable STRING glass balls effect");
		} else {
			props.setProperty(TITLE, "Enable STRING glass balls effect");
		}
		props.setProperty(MENU_GRAVITY, "9.0");
		props.setProperty(IN_MENU_BAR, "true");
		manager.registerService(this, NetworkViewTaskFactory.class, props);
	}
}