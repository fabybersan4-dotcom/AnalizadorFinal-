package automata;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

public class AutomataNDA extends JFrame {

	private static class NFAState {
		String name;
		boolean initial;
		boolean fin;

		NFAState(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(name);
			if (initial)
				sb.append(" (inicial)");
			if (fin)
				sb.append(" [final]");
			return sb.toString();
		}
	}

	private static class Transition {
		String from;
		String symbol; // "" = epsilon
		String to;

		Transition(String from, String symbol, String to) {
			this.from = from;
			this.symbol = symbol;
			this.to = to;
		}
	}

	private final DefaultListModel<NFAState> stateListModel = new DefaultListModel<>();
	private final JList<NFAState> stateList = new JList<>(stateListModel);
	private final List<Transition> transitions = new ArrayList<>();

	private final JTextField nombreField = new JTextField(10);
	private final JLabel inicialLabel = new JLabel("Inicial: (ninguno)");
	private final JLabel finalesLabel = new JLabel("Finales: []");

	private final JComboBox<String> desdeCombo = new JComboBox<>();
	private final JComboBox<String> hastaCombo = new JComboBox<>();
	private final JTextField simboloField = new JTextField(6);
	private final DefaultTableModel tableModel = new DefaultTableModel(new Object[] { "Desde", "Símbolo", "Hasta" },
			0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};
	private final JTable transicionesTable = new JTable(tableModel);

	private final JTextField cadenaField = new JTextField(20);
	private final JTextArea resultadoArea = new JTextArea(8, 40);

	public AutomataNDA() {
		super("Simulador de AFND (Autómata Finito No Determinista)");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(8, 8));

		setJMenuBar(buildMenuBar());

		JPanel centro = new JPanel(new GridLayout(1, 2, 8, 8));
		centro.add(buildEstadosPanel());
		centro.add(buildTransicionesPanel());

		add(centro, BorderLayout.CENTER);
		add(buildProbarCadenaPanel(), BorderLayout.SOUTH);

		setSize(950, 620);
		setLocationRelativeTo(null);

		// Carga automáticamente el autómata para la expresión ((a*b) u c) a*
		cargarAutomataPredefinido();
	}

	private JMenuBar buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu archivoMenu = new JMenu("Archivo");

		JMenuItem nuevoItem = new JMenuItem("Nuevo autómata");
		nuevoItem.addActionListener(e -> nuevoAutomata());

		JMenuItem cargarPredefinidoItem = new JMenuItem("Cargar ((a*b) u c) a*");
		cargarPredefinidoItem.addActionListener(e -> {
			nuevoAutomataSilencioso();
			cargarAutomataPredefinido();
		});

		JMenuItem salirItem = new JMenuItem("Salir");
		salirItem.addActionListener(e -> System.exit(0));

		archivoMenu.add(nuevoItem);
		archivoMenu.add(cargarPredefinidoItem);
		archivoMenu.addSeparator();
		archivoMenu.add(salirItem);
		menuBar.add(archivoMenu);
		return menuBar;
	}

	private JPanel buildEstadosPanel() {
		JPanel panel = new JPanel(new BorderLayout(6, 6));
		panel.setBorder(BorderFactory.createTitledBorder("Estados"));

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(new JLabel("Nombre:"));
		topPanel.add(nombreField);
		JButton agregarBtn = new JButton("Agregar");
		agregarBtn.addActionListener(e -> agregarEstado());
		topPanel.add(agregarBtn);

		JScrollPane listScroll = new JScrollPane(stateList);

		JPanel botonesPanel = new JPanel(new GridLayout(3, 1, 4, 4));
		JButton marcarInicialBtn = new JButton("Marcar como inicial");
		marcarInicialBtn.addActionListener(e -> marcarComoInicial());
		JButton marcarFinalBtn = new JButton("Marcar/Desmarcar final");
		marcarFinalBtn.addActionListener(e -> marcarDesmarcarFinal());
		JButton eliminarBtn = new JButton("Eliminar estado");
		eliminarBtn.addActionListener(e -> eliminarEstado());
		botonesPanel.add(marcarInicialBtn);
		botonesPanel.add(marcarFinalBtn);
		botonesPanel.add(eliminarBtn);

		JPanel etiquetasPanel = new JPanel(new GridLayout(2, 1));
		etiquetasPanel.add(inicialLabel);
		etiquetasPanel.add(finalesLabel);

		JPanel southPanel = new JPanel(new BorderLayout(4, 4));
		southPanel.add(botonesPanel, BorderLayout.NORTH);
		southPanel.add(etiquetasPanel, BorderLayout.SOUTH);

		panel.add(topPanel, BorderLayout.NORTH);
		panel.add(listScroll, BorderLayout.CENTER);
		panel.add(southPanel, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel buildTransicionesPanel() {
		JPanel panel = new JPanel(new BorderLayout(6, 6));
		panel.setBorder(BorderFactory.createTitledBorder("Transiciones (deja el símbolo vacío para \u03b5 / epsilon)"));

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(new JLabel("Desde:"));
		topPanel.add(desdeCombo);
		topPanel.add(new JLabel("Símbolo:"));
		topPanel.add(simboloField);
		topPanel.add(new JLabel("Hasta:"));
		topPanel.add(hastaCombo);
		JButton agregarTransicionBtn = new JButton("Agregar transición");
		agregarTransicionBtn.addActionListener(e -> agregarTransicion());
		topPanel.add(agregarTransicionBtn);

		JScrollPane tableScroll = new JScrollPane(transicionesTable);

		panel.add(topPanel, BorderLayout.NORTH);
		panel.add(tableScroll, BorderLayout.CENTER);
		return panel;
	}

	private JPanel buildProbarCadenaPanel() {
		JPanel panel = new JPanel(new BorderLayout(6, 6));
		panel.setBorder(BorderFactory.createTitledBorder("Probar cadena"));

		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(new JLabel("Cadena:"));
		topPanel.add(cadenaField);
		JButton evaluarBtn = new JButton("Evaluar");
		evaluarBtn.addActionListener(e -> evaluarCadena());
		JButton reiniciarBtn = new JButton("Reiniciar");
		reiniciarBtn.addActionListener(e -> reiniciarPrueba());
		topPanel.add(evaluarBtn);
		topPanel.add(reiniciarBtn);

		resultadoArea.setEditable(false);
		resultadoArea.setLineWrap(true);
		resultadoArea.setWrapStyleWord(true);
		JScrollPane resultadoScroll = new JScrollPane(resultadoArea);

		panel.add(topPanel, BorderLayout.NORTH);
		panel.add(resultadoScroll, BorderLayout.CENTER);
		return panel;
	}

	// ---------- Autómata predefinido: ((a*b) u c) a* ----------

	/**
	 * Construye directamente (sin epsilon) el AFND para la expresión regular ((a*b)
	 * u c) a*.
	 *
	 * Idea: desde el estado inicial S se pueden consumir cero o más 'a'
	 * (permaneciendo en S), y luego: - una 'b' lleva a P (cubre la rama a*b), o -
	 * una 'c' lleva a P directamente (cubre la rama c). Una vez en P (estado
	 * final), se pueden consumir cero o más 'a' adicionales (la a* final),
	 * permaneciendo en P.
	 *
	 * Transiciones: S --a--> S S --b--> P S --c--> P P --a--> P
	 *
	 * S es el estado inicial; P es el (único) estado final.
	 */
	private void cargarAutomataPredefinido() {
		if (stateListModel.size() > 0 || !transitions.isEmpty()) {
			// Ya hay un autómata cargado; no lo sobrescribimos accidentalmente.
			return;
		}

		NFAState s = new NFAState("S");
		s.initial = true;
		NFAState p = new NFAState("P");
		p.fin = true;

		stateListModel.addElement(s);
		stateListModel.addElement(p);

		transitions.add(new Transition("S", "a", "S"));
		transitions.add(new Transition("S", "b", "P"));
		transitions.add(new Transition("S", "c", "P"));
		transitions.add(new Transition("P", "a", "P"));

		actualizarCombos();
		actualizarEtiquetas();
		refrescarTablaTransiciones();
	}

	// ---------- Lógica de estados ----------

	private void agregarEstado() {
		String nombre = nombreField.getText().trim();
		if (nombre.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Escribe un nombre para el estado.");
			return;
		}
		for (int i = 0; i < stateListModel.size(); i++) {
			if (stateListModel.get(i).name.equals(nombre)) {
				JOptionPane.showMessageDialog(this, "Ya existe un estado con ese nombre.");
				return;
			}
		}
		NFAState nuevo = new NFAState(nombre);
		stateListModel.addElement(nuevo);
		nombreField.setText("");
		actualizarCombos();
		actualizarEtiquetas();
	}

	private void marcarComoInicial() {
		NFAState seleccionado = stateList.getSelectedValue();
		if (seleccionado == null) {
			JOptionPane.showMessageDialog(this, "Selecciona un estado de la lista.");
			return;
		}
		for (int i = 0; i < stateListModel.size(); i++) {
			NFAState s = stateListModel.get(i);
			s.initial = (s == seleccionado);
			stateListModel.set(i, s);
		}
		actualizarEtiquetas();
	}

	private void marcarDesmarcarFinal() {
		NFAState seleccionado = stateList.getSelectedValue();
		if (seleccionado == null) {
			JOptionPane.showMessageDialog(this, "Selecciona un estado de la lista.");
			return;
		}
		int index = stateListModel.indexOf(seleccionado);
		seleccionado.fin = !seleccionado.fin;
		stateListModel.set(index, seleccionado);
		actualizarEtiquetas();
	}

	private void eliminarEstado() {
		NFAState seleccionado = stateList.getSelectedValue();
		if (seleccionado == null) {
			JOptionPane.showMessageDialog(this, "Selecciona un estado de la lista.");
			return;
		}
		stateListModel.removeElement(seleccionado);
		transitions.removeIf(t -> t.from.equals(seleccionado.name) || t.to.equals(seleccionado.name));
		refrescarTablaTransiciones();
		actualizarCombos();
		actualizarEtiquetas();
	}

	private void actualizarEtiquetas() {
		String inicial = "(ninguno)";
		List<String> finales = new ArrayList<>();
		for (int i = 0; i < stateListModel.size(); i++) {
			NFAState s = stateListModel.get(i);
			if (s.initial)
				inicial = s.name;
			if (s.fin)
				finales.add(s.name);
		}
		inicialLabel.setText("Inicial: " + inicial);
		finalesLabel.setText("Finales: " + finales);
	}

	private void actualizarCombos() {
		String desdeSel = (String) desdeCombo.getSelectedItem();
		String hastaSel = (String) hastaCombo.getSelectedItem();
		desdeCombo.removeAllItems();
		hastaCombo.removeAllItems();
		for (int i = 0; i < stateListModel.size(); i++) {
			String nombre = stateListModel.get(i).name;
			desdeCombo.addItem(nombre);
			hastaCombo.addItem(nombre);
		}
		if (desdeSel != null)
			desdeCombo.setSelectedItem(desdeSel);
		if (hastaSel != null)
			hastaCombo.setSelectedItem(hastaSel);
	}

	// ---------- Lógica de transiciones ----------

	private void agregarTransicion() {
		String desde = (String) desdeCombo.getSelectedItem();
		String hasta = (String) hastaCombo.getSelectedItem();
		String simbolo = simboloField.getText().trim();

		if (desde == null || hasta == null) {
			JOptionPane.showMessageDialog(this, "Primero agrega estados.");
			return;
		}
		if (simbolo.length() > 1) {
			JOptionPane.showMessageDialog(this, "El símbolo debe ser un solo carácter (o vacío para epsilon).");
			return;
		}
		transitions.add(new Transition(desde, simbolo, hasta));
		refrescarTablaTransiciones();
		simboloField.setText("");
	}

	private void refrescarTablaTransiciones() {
		tableModel.setRowCount(0);
		for (Transition t : transitions) {
			String simboloMostrado = t.symbol.isEmpty() ? "\u03b5" : t.symbol;
			tableModel.addRow(new Object[] { t.from, simboloMostrado, t.to });
		}
	}

	// ---------- Simulación ----------

	private Set<String> clausuraEpsilon(Set<String> estados) {
		Set<String> clausura = new HashSet<>(estados);
		Deque<String> pila = new ArrayDeque<>(estados);
		while (!pila.isEmpty()) {
			String actual = pila.pop();
			for (Transition t : transitions) {
				if (t.from.equals(actual) && t.symbol.isEmpty() && !clausura.contains(t.to)) {
					clausura.add(t.to);
					pila.push(t.to);
				}
			}
		}
		return clausura;
	}

	private void evaluarCadena() {
		String inicial = null;
		Set<String> finales = new HashSet<>();
		for (int i = 0; i < stateListModel.size(); i++) {
			NFAState s = stateListModel.get(i);
			if (s.initial)
				inicial = s.name;
			if (s.fin)
				finales.add(s.name);
		}

		if (inicial == null) {
			resultadoArea.setText("No hay un estado inicial definido. Marca uno como inicial.");
			return;
		}

		String cadena = cadenaField.getText();
		Set<String> actuales = clausuraEpsilon(new HashSet<>(Collections.singleton(inicial)));

		StringBuilder traza = new StringBuilder();
		traza.append("Estado(s) inicial(es): ").append(actuales).append("\n");

		for (int i = 0; i < cadena.length(); i++) {
			String simbolo = String.valueOf(cadena.charAt(i));
			Set<String> siguientes = new HashSet<>();
			for (String estado : actuales) {
				for (Transition t : transitions) {
					if (t.from.equals(estado) && t.symbol.equals(simbolo)) {
						siguientes.add(t.to);
					}
				}
			}
			actuales = clausuraEpsilon(siguientes);
			traza.append("Con '").append(simbolo).append("' -> ").append(actuales).append("\n");
			if (actuales.isEmpty()) {
				break;
			}
		}

		boolean aceptada = false;
		for (String estado : actuales) {
			if (finales.contains(estado)) {
				aceptada = true;
				break;
			}
		}

		traza.append("\n");
		if (aceptada) {
			traza.append("Cadena \"").append(cadena).append("\" ACEPTADA (VALIDA).");
		} else {
			traza.append("Cadena \"").append(cadena).append("\" RECHAZADA (INVALIDA).");
		}

		resultadoArea.setText(traza.toString());
	}

	private void reiniciarPrueba() {
		cadenaField.setText("");
		resultadoArea.setText("");
	}

	private void nuevoAutomata() {
		int confirm = JOptionPane.showConfirmDialog(this, "¿Borrar todos los estados y transiciones?", "Nuevo autómata",
				JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION)
			return;
		nuevoAutomataSilencioso();
	}

	private void nuevoAutomataSilencioso() {
		stateListModel.clear();
		transitions.clear();
		tableModel.setRowCount(0);
		desdeCombo.removeAllItems();
		hastaCombo.removeAllItems();
		nombreField.setText("");
		simboloField.setText("");
		reiniciarPrueba();
		actualizarEtiquetas();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new AutomataNDA().setVisible(true));
	}
}