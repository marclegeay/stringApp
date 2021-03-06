package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.SelectedNodesAndEdgesEvent;
import org.cytoscape.model.events.SelectedNodesAndEdgesListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.utils.ModelUtils;

/**
 * Displays information about a protein taken from STRING
 * @author Scooter Morris
 *
 */
public class StringEdgePanel extends AbstractStringPanel {
	JPanel subScorePanel;
	JPanel scorePanel;
	private Map<CyNetwork, Map<String, Boolean>> colors;

	public StringEdgePanel(final StringManager manager) {
		super(manager);
		filters.get(currentNetwork).put("score", new HashMap<>());

		colors = new HashMap<>();
		colors.put(currentNetwork, new HashMap<>());

		init();
		revalidate();
		repaint();
	}

	private void init() {
		setLayout(new GridBagLayout());
		{
			EasyGBC c = new EasyGBC();
			add(new JSeparator(SwingConstants.HORIZONTAL), c.anchor("west").expandHoriz());
			JComponent scoreSlider = createFilterSlider("score", "Score", currentNetwork, true, 100.0);
			{
				scorePanel = new JPanel();
				scorePanel.setLayout(new GridBagLayout());
				EasyGBC d = new EasyGBC();
				scorePanel.add(scoreSlider, d.anchor("west").expandHoriz());
			}
			add(scorePanel, c.down().anchor("west").expandHoriz());

			{
				subScorePanel = new JPanel();
				subScorePanel.setLayout(new GridBagLayout());
				EasyGBC d = new EasyGBC();
				subScorePanel.add(createSubScorePanel(), d.anchor("west").expandHoriz());
				subScorePanel.add(new JPanel(), d.down().anchor("west").expandBoth());
			}

			JScrollPane scrollPane = new JScrollPane(subScorePanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
		                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			add(scrollPane, c.down().anchor("west").expandBoth());
			// add(new JPanel(), c.down().anchor("west").expandBoth());
		}
	}

	private JPanel createSubScorePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		List<String> subScoreList = ModelUtils.getSubScoreList(currentNetwork);

		// OK, now we want to create 3 panels: Color, Label, and Filter
		{
			JPanel colorPanel = new JPanel();
			colorPanel.setMinimumSize(new Dimension(25,30));
			colorPanel.setLayout(new GridBagLayout());
			EasyGBC d = new EasyGBC();
			JLabel lbl = new JLabel("Color");
			lbl.setFont(labelFont);
			lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			colorPanel.add(lbl, d.anchor("north").noExpand());

			for (String subScore: subScoreList) {
				colorPanel.add(createScoreCheckBox(subScore), d.down().expandVert());
			}

			colorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			panel.add(colorPanel, c.anchor("northwest").expandVert());
		}

		{
			JPanel labelPanel = new JPanel();
			labelPanel.setLayout(new GridBagLayout());
			EasyGBC d = new EasyGBC();
			JLabel lbl = new JLabel("Subscore");
			lbl.setFont(labelFont);
			lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			labelPanel.add(lbl, d.anchor("north").noExpand());
			for (String subScore: subScoreList) {
				JLabel scoreLabel = new JLabel(subScore);
				scoreLabel.setFont(textFont);
				scoreLabel.setMinimumSize(new Dimension(100,30));
				scoreLabel.setMaximumSize(new Dimension(100,30));
				labelPanel.add(scoreLabel, d.down().expandVert());
			}
			labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			panel.add(labelPanel, c.right().expandVert());
		}

		{
			JPanel filterPanel = new JPanel();
			filterPanel.setLayout(new GridBagLayout());
			EasyGBC d = new EasyGBC();
			JLabel lbl = new JLabel("Filters");
			lbl.setFont(labelFont);
			lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			filterPanel.add(lbl, d.anchor("north").noExpand());
			for (String subScore: subScoreList) {
				JComponent scoreSlider = createFilterSlider("score", subScore, currentNetwork, false, 100.0);
				scoreSlider.setMinimumSize(new Dimension(100,30));
				// scoreSlider.setMaximumSize(new Dimension(100,30));
				filterPanel.add(scoreSlider, d.down().expandBoth());
			}
			filterPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			panel.add(filterPanel, c.right().expandBoth());
		}

		CollapsablePanel collapsablePanel = new CollapsablePanel(iconFont, "Subscores", panel, false, 10);
		collapsablePanel.setBorder(BorderFactory.createEtchedBorder());
		return collapsablePanel;

	}

	private JComponent createScoreCheckBox(String subScore) {
		Map<String, Color> colorMap = manager.getChannelColors();
		JCheckBox cb = new JCheckBox("");
		cb.setMinimumSize(new Dimension(20,30));
		cb.setMaximumSize(new Dimension(20,30));
		cb.setBackground(colorMap.get(subScore));
		cb.setOpaque(true);
		if (colors.containsKey(currentNetwork) && 
		    colors.get(currentNetwork).containsKey(subScore) &&
		    colors.get(currentNetwork).get(subScore))
			cb.setSelected(true);
		cb.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Boolean selected = Boolean.FALSE;
				if (e.getStateChange() == ItemEvent.SELECTED)
					selected = Boolean.TRUE;

				colors.get(currentNetwork).put(subScore, selected);

				doColors();
			}
		});
		return cb;
	}

	void doFilter(String type) {
		Map<String, Double> filter = filters.get(currentNetwork).get(type);
		CyNetworkView view = manager.getCurrentNetworkView();
		for (CyEdge edge: currentNetwork.getEdgeList()) {
			CyRow edgeRow = currentNetwork.getRow(edge);
			Double edgeScore = edgeRow.get(ModelUtils.SCORE, Double.class);
			if (edgeScore == null) 
				continue;
			boolean show = true;
			for (String lbl: filter.keySet()) {
				Double v = edgeRow.get(ModelUtils.STRINGDB_NAMESPACE, lbl.toLowerCase(), Double.class);
				double nv = filter.get(lbl);
				if ((v == null && nv > 0) || v < nv) {
					show = false;
					break;
				}
			}
			if (show) {
				// view.getEdgeView(edge).setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, true);
				view.getEdgeView(edge).clearValueLock(BasicVisualLexicon.EDGE_VISIBLE);
			} else {
				// view.getEdgeView(edge).setVisualProperty(BasicVisualLexicon.EDGE_VISIBLE, false);
				view.getEdgeView(edge).setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
				view.getModel().getRow(edge).set(CyNetwork.SELECTED, false);
			}
		}
	}

	void doColors() {
		Map<String, Boolean> color = colors.get(currentNetwork);
		Map<String, Color> colorMap = manager.getChannelColors();
		CyNetworkView view = manager.getCurrentNetworkView();
		for (CyEdge edge: currentNetwork.getEdgeList()) {
			CyRow edgeRow = currentNetwork.getRow(edge);
			double max = -1;
			Color clr = null;
			for (String lbl: color.keySet()) {
				if (!color.get(lbl))
					continue;
				Double v = edgeRow.get(ModelUtils.STRINGDB_NAMESPACE, lbl, Double.class);
				if (v != null && v > max) {
					max = v;
					clr = colorMap.get(lbl);
				}
			}
			if (clr == null)
				view.getEdgeView(edge).clearValueLock(BasicVisualLexicon.EDGE_UNSELECTED_PAINT);
			else
				view.getEdgeView(edge).setLockedValue(BasicVisualLexicon.EDGE_UNSELECTED_PAINT, clr);
		}
	}

	private void updateScore() {
		scorePanel.removeAll();
		JComponent scoreSlider = createFilterSlider("score", "Score", currentNetwork, true, 100.0);
		scorePanel.add(scoreSlider);
	}

	public void updateSubPanel() {
		subScorePanel.removeAll();
		EasyGBC d = new EasyGBC();
		subScorePanel.add(createSubScorePanel(), d.anchor("west").expandHoriz());
		subScorePanel.add(new JPanel(), d.down().anchor("west").expandBoth());
	}

	public void networkChanged(CyNetwork newNetwork) {
		this.currentNetwork = newNetwork;
		if (!filters.containsKey(currentNetwork)) {
			filters.put(currentNetwork, new HashMap<>());
			filters.get(currentNetwork).put("score", new HashMap<>());
		}
		if (!colors.containsKey(currentNetwork)) {
			colors.put(currentNetwork, new HashMap<>());
		}

		updateSubPanel();
		updateScore();
	}

	public void selectedEdges(Collection<CyEdge> edges) {
	}
}
