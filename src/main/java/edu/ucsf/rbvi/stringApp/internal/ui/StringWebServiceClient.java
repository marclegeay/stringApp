package edu.ucsf.rbvi.stringApp.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;

import edu.ucsf.rbvi.stringApp.internal.model.Annotation;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;

import edu.ucsf.rbvi.stringApp.internal.tasks.GetAnnotationsTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.ImportNetworkTaskFactory;

// TODO: [Optional] Improve non-gui mode
public class StringWebServiceClient extends AbstractWebServiceGUIClient 
                                    implements NetworkImportWebServiceClient, SearchWebServiceClient {
	JTextArea searchTerms;
	JTextField additionalNodesText;
	StringManager manager;
	JPanel mainPanel;
	JPanel mainSearchPanel;
	JComboBox speciesCombo;
	JSlider confidenceSlider;
	JButton importButton;
	JButton backButton;
	Map<String, List<String>> resolvedIdMap = null;
	Map<String, List<Annotation>> annotations = null;

	public StringWebServiceClient(StringManager manager) {
		super(manager.getURL(), "String DB", "The String Database");
		this.manager = manager;
		init();
	}

	public TaskIterator createTaskIterator(Object query) {
		if (query == null)
			throw new NullPointerException("null query");
		return new TaskIterator();
	}

	private void init() {
		// Create the surrounding panel
		mainPanel = new JPanel(new GridBagLayout());
		super.gui = mainPanel;
		mainPanel.setPreferredSize(new Dimension(800,800));
		EasyGBC c = new EasyGBC();

		// Create the database options
		//createDatabasePanel(manager.getDatabases());

		// Create the species panel
		List<Species> speciesList = Species.getSpecies();
		if (speciesList == null) {
			try {
				speciesList = Species.readSpecies(manager);
			} catch (Exception e) {
				manager.error("Unable to get species: "+e.getMessage());
				e.printStackTrace();
				return;
			}
		}
		JPanel speciesBox = createSpeciesComboBox(speciesList);
		super.gui.add(speciesBox, c.expandHoriz().insets(0,5,0,5));

		// Create the search list panel
		mainSearchPanel = createSearchPanel();
		super.gui.add(mainSearchPanel, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the confidence cutoff
		JPanel confidenceSlider = createConfidenceSlider();
		super.gui.add(confidenceSlider, c.down().expandBoth().insets(5,5,0,5));

		// Create the slider for the confidence cutoff
		// JPanel additionalNodesPanel = createAdditionalNodesPanel();
		// super.gui.add(additionalNodesPanel, c.down().expandBoth().insets(5,5,0,5));

		// Create the evidence types buttons
		// createEvidenceButtons(manager.getEvidenceTypes());

		// Add Query/Cancel buttons
		JPanel buttonPanel =  createControlButtons();
		super.gui.add(buttonPanel, c.down().expandHoriz().insets(0,5,5,5));
	}

	JPanel createSearchPanel() {
		JPanel searchPanel = new JPanel(new GridBagLayout());
		searchPanel.setPreferredSize(new Dimension(600,600));
		EasyGBC c = new EasyGBC();

		JLabel searchLabel = new JLabel("Enter protein or compound names:");
		c.noExpand().anchor("northwest").insets(0,5,0,5);
		searchPanel.add(searchLabel, c);
		searchTerms = new JTextArea();
		JScrollPane jsp = new JScrollPane(searchTerms);
		c.down().expandBoth().insets(5,10,5,10);
		searchPanel.add(jsp, c);
		return searchPanel;
	}

	void replaceSearchPanel() {
		mainSearchPanel.removeAll();
		mainSearchPanel.revalidate();
		mainSearchPanel.repaint();
		mainSearchPanel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		JLabel searchLabel = new JLabel("Enter protein or compound names:");
		c.noExpand().anchor("northwest").insets(0,5,0,5);
		mainSearchPanel.add(searchLabel, c);
		searchTerms = new JTextArea();
		JScrollPane jsp = new JScrollPane(searchTerms);
		c.down().expandBoth().insets(5,10,5,10);
		mainSearchPanel.add(jsp, c);
		mainSearchPanel.revalidate();
		mainSearchPanel.repaint();
	}

	JPanel createSpeciesComboBox(List<Species> speciesList) {
		JPanel speciesPanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel speciesLabel = new JLabel("Species:");
		c.noExpand().insets(0,5,0,5);
		speciesPanel.add(speciesLabel, c);
		speciesCombo = new JComboBox(speciesList.toArray());

		// Set Human as the default
		for (Species s: speciesList) {
			if (s.toString().equals("Homo sapiens")) {
				speciesCombo.setSelectedItem(s);
				break;
			}
		}
		c.right().expandHoriz().insets(0,5,0,5);
		speciesPanel.add(speciesCombo, c);
		return speciesPanel;
	}

	JPanel createControlButtons() {
		JPanel buttonPanel = new JPanel();
		BoxLayout layout = new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS);
		buttonPanel.setLayout(layout);
		JButton cancelButton = new JButton(new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent e) {
          cancel();
        }
      });

		backButton = new JButton(new AbstractAction("Back") {
        @Override
        public void actionPerformed(ActionEvent e) {
					resolvedIdMap = null;
					annotations = null;
					replaceSearchPanel();
					importButton.setEnabled(true);
					backButton.setEnabled(false);
					importButton.setAction(new InitialAction());
					mainPanel.getParent().revalidate();
        }
			});
		backButton.setEnabled(false);

		importButton = new JButton(new InitialAction());

		buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(cancelButton);
		buttonPanel.add(Box.createHorizontalGlue());
		// buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(backButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(10,0)));
		buttonPanel.add(importButton);
		return buttonPanel;
	}

	JPanel createConfidenceSlider() {
		JPanel confidencePanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel confidenceLabel = new JLabel("Required confidence (score):");
		Font labelFont = confidenceLabel.getFont();
		confidenceLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
		c.anchor("west").noExpand().insets(0,5,0,5);
		confidencePanel.add(confidenceLabel, c);
		confidenceSlider = new JSlider();
		Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		Font valueFont = new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()-4);
		NumberFormat formatter = new DecimalFormat("#0.00");
		for (int value = 0; value <= 100; value += 10) {
			double labelValue = (double)value/100.0;
			JLabel label = new JLabel(formatter.format(labelValue));
			label.setFont(valueFont);
			labels.put(value, label);
		}
		confidenceSlider.setLabelTable(labels);
		confidenceSlider.setPaintLabels(true);
		confidenceSlider.setValue(40);
		c.down().expandBoth().insets(0,5,10,5);
		confidencePanel.add(confidenceSlider, c);
		return confidencePanel;
	}

	JPanel createAdditionalNodesPanel() {
		JPanel nodePanel = new JPanel(new GridBagLayout());
		EasyGBC c = new EasyGBC();
		JLabel nodeLabel = new JLabel("Additional network nodes:");
		Font labelFont = nodeLabel.getFont();
		nodeLabel.setFont(new Font(labelFont.getFontName(), Font.BOLD, labelFont.getSize()));
		c.anchor("west").noExpand().insets(0,5,0,5);
		nodePanel.add(nodeLabel, c);
		additionalNodesText = new JTextField();
		c.right().expandHoriz().insets(0,5,0,5);
		nodePanel.add(additionalNodesText, c);
		return nodePanel;
	}

	boolean resolveAnnotations(final Map<String, List<Annotation>> annotations,
	                           Map<String, List<String>> resolvedIds) {
		boolean noAmbiguity = true;
		for (String key: annotations.keySet()) {
			if (annotations.get(key).size() > 1) {
				noAmbiguity = false;
				break;
			} else {
				List<String> ids = new ArrayList<String>();
				ids.add (annotations.get(key).get(0).getStringId());
				resolvedIds.put(key, ids);
			}
		}

		// Now trim the key set
		if (resolvedIds.size() > 0) {
			for (String key: resolvedIds.keySet()) {
				if (annotations.containsKey(key))
					annotations.remove(key);
			}
		}
		return noAmbiguity;
	}

	void importNetwork(int taxon, int confidence, int additional_nodes, Map<String,List<String>> resolvedIdMap) {
		Map<String, String> queryTermMap = new HashMap<>();
		List<String> stringIds = combineIds(resolvedIdMap, queryTermMap);
		// System.out.println("Importing "+stringIds);
		cancel();
		TaskFactory factory = new ImportNetworkTaskFactory(manager, speciesCombo.getSelectedItem().toString(), 
		                                                   taxon, confidence, additional_nodes, stringIds,
																											 queryTermMap);
		manager.execute(factory.createTaskIterator());
	}

	void createResolutionPanel(final Map<String, List<Annotation>> annotations) {
		mainSearchPanel.removeAll();
		mainPanel.revalidate();
		final Map<String, ResolveTableModel> tableModelMap = new HashMap<>();
		for (String term: annotations.keySet()) {
			tableModelMap.put(term, new ResolveTableModel(this, term, annotations.get(term)));
		}
		mainSearchPanel.setLayout(new GridBagLayout());
		EasyGBC c = new EasyGBC();

		{	
			String label = "<html><b>Multiple possible matches for some terms:</b> ";
			label += "Select the term in the left column to see the possibilities, then select the correct term from the table";
			label += "</html>";
	
			JLabel lbl = new JLabel(label);
			c.anchor("northeast").expandHoriz();
			mainSearchPanel.add(lbl, c);
		}


		{
			JPanel annPanel = new JPanel(new GridBagLayout());
			EasyGBC ac = new EasyGBC();
	
			final JTable table = new JTable();
			table.setRowSelectionAllowed(false);

			final JPanel selectPanel = new JPanel(new FlowLayout());
			final JButton selectAllButton = new JButton(new SelectEverythingAction(tableModelMap));
			final JButton clearAllButton = new JButton(new ClearEverythingAction(tableModelMap));
			final JButton selectAllTermButton = new JButton("Select All in Term");
			final JButton clearAllTermButton = new JButton("Clear All in Term");
			selectAllTermButton.setEnabled(false);
			clearAllTermButton.setEnabled(false);
			selectPanel.add(selectAllButton);
			selectPanel.add(clearAllButton);
			selectPanel.add(selectAllTermButton);
			selectPanel.add(clearAllTermButton);

			Object[] terms = annotations.keySet().toArray();
			final JList termList = new JList(terms);
			termList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			termList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					String term = (String)termList.getSelectedValue();
					showTableRow(table, term, tableModelMap);
					selectAllTermButton.setAction(new SelectAllTermAction(term, tableModelMap));
					selectAllTermButton.setEnabled(true);
					clearAllTermButton.setAction(new ClearAllTermAction(term, tableModelMap));
					clearAllTermButton.setEnabled(true);
				}
			});
			termList.setFixedCellWidth(75);

			JScrollPane termScroller = new JScrollPane(termList);
			termScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			ac.anchor("east").expandVert();
			annPanel.add(termScroller, ac);
	
			JScrollPane tableScroller = new JScrollPane(table);
			ac.right().expandBoth().insets(0,5,0,5);
			annPanel.add(tableScroller, ac);

			c.down().expandBoth().insets(5,0,5,0);
			mainSearchPanel.add(annPanel, c);

			// Now, select the first term
			termList.setSelectedIndex(0);

			c.down().spanHoriz(2).expandHoriz().insets(0,5,0,5);
			mainSearchPanel.add(selectPanel, c);
		}

		if (resolvedIdMap.size() == 0)
			importButton.setEnabled(false);
		importButton.setAction(new ResolvedAction());
		backButton.setEnabled(true);

		mainPanel.revalidate();
	}

	public void addResolvedStringID(String term, String id) {
		if (!resolvedIdMap.containsKey(term))
			resolvedIdMap.put(term, new ArrayList<String>());
		resolvedIdMap.get(term).add(id);
		if (resolvedIdMap.size() > 0) {
			importButton.setEnabled(true);
		}
	}

	public void removeResolvedStringID(String term, String id) {
		if (!resolvedIdMap.containsKey(term))
			return;
		List<String> ids = resolvedIdMap.get(term);
		ids.remove(id);
		if (ids.size() == 0)
			resolvedIdMap.remove(term);

		if (resolvedIdMap.size() == 0) {
			importButton.setEnabled(false);
		}
	}

	private void showTableRow(JTable table, String term, Map<String, ResolveTableModel> tableModelMap) {
		TableRowSorter sorter = new TableRowSorter(tableModelMap.get(term));
		sorter.setSortable(0, false);
		sorter.setSortable(1, true);
		sorter.setSortable(2, false);
		table.setModel(tableModelMap.get(term));
		table.setRowSorter(sorter);
		table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		table.getColumnModel().getColumn(2).setCellRenderer(new TextAreaRenderer());
		table.getColumnModel().getColumn(0).setPreferredWidth(50);
		table.getColumnModel().getColumn(1).setPreferredWidth(75);
		table.getColumnModel().getColumn(2).setPreferredWidth(400);
	}

	private List<String> combineIds(Map<String, List<String>> resolvedIdsMap, Map<String, String> reverseMap) {
		List<String> ids = new ArrayList<>();
		for (String term: resolvedIdsMap.keySet()) {
			for (String id: resolvedIdsMap.get(term)) {
				ids.add(id);
				reverseMap.put(id, term);
			}
		}
		return ids;
	}

	public void cancel() {
		resolvedIdMap = null;
		annotations = null;
		replaceSearchPanel();
		importButton.setEnabled(true);
		backButton.setEnabled(false);
		importButton.setAction(new InitialAction());
		((Window)mainPanel.getRootPane().getParent()).dispose();
	}


	class InitialAction extends AbstractAction implements TaskObserver {
		public InitialAction() {
			super("Import");
		}

    @Override
    public void actionPerformed(ActionEvent e) {
			// Start our task cascade
			Species species = (Species)speciesCombo.getSelectedItem();
			if (resolvedIdMap == null)
				resolvedIdMap = new HashMap<>();

			/*
			String addText = additionalNodesText.getText();
			if (addText != null && addText.length() > 0) {
				try {
					additionalNodes	= Integer.parseInt(addText);
				} catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(null, "Additional nodes must be an integer value", 
								                        "Additional Nodes Error", JOptionPane.ERROR_MESSAGE); 
					return;
				}
			}
			*/
	
			int taxon = species.getTaxId();
			String terms = searchTerms.getText();
			manager.info("Getting annotations for "+species.getName()+"terms: "+terms);

			// Launch a task to get the annotations. 
			manager.execute(new TaskIterator(new GetAnnotationsTask(manager, taxon, terms)),this);
		}

		@Override
		public void allFinished(FinishStatus finishStatus) {}

		@Override
		public void taskFinished(ObservableTask task) {
			if (!(task instanceof GetAnnotationsTask))
				return;
			GetAnnotationsTask annTask = (GetAnnotationsTask)task;
	
			annotations = annTask.getAnnotations();
			int taxon = annTask.getTaxon();
			if (annotations == null || annotations.size() == 0) {
				JOptionPane.showMessageDialog(null, "Your query returned no results",
							                        "No results", JOptionPane.ERROR_MESSAGE); 
				return;
			}
			boolean noAmbiguity = resolveAnnotations(annotations, resolvedIdMap);
			if (noAmbiguity) {
				int additionalNodes = 0;
				// This mimics the String web site behavior
				if (resolvedIdMap.size() == 1)
					additionalNodes = 10;
	
				importNetwork(taxon, confidenceSlider.getValue(), additionalNodes, resolvedIdMap);
			} else {
				createResolutionPanel(annotations);
			}
		}
	}

	class ResolvedAction extends AbstractAction {
		public ResolvedAction() {
			super("Import");
		}

    @Override
    public void actionPerformed(ActionEvent e) {
			Species species = (Species)speciesCombo.getSelectedItem();

			int additionalNodes = 0;
			/*
			String addText = additionalNodesText.getText();
			if (addText != null && addText.length() > 0) {
				try {
					additionalNodes	= Integer.parseInt(addText);
					System.out.println("Additional Nodes = "+additionalNodes);
				} catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(null, "Additional nodes must be an integer value", 
								                        "Additional Nodes Error", JOptionPane.ERROR_MESSAGE); 
					return;
				}
			}
			*/
			if (resolvedIdMap.size() == 1)
				additionalNodes = 10;

			int taxon = species.getTaxId();
			importNetwork(taxon, confidenceSlider.getValue(), additionalNodes, resolvedIdMap);
		}
	}

}
