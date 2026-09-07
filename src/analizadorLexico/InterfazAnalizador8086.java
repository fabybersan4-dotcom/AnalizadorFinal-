package analizadorLexico;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class InterfazAnalizador8086 extends JFrame {

	private static final Color VERDE_OSCURO = new Color(27, 67, 50);
	private static final Color VERDE_MEDIO = new Color(64, 145, 108);
	private static final Color VERDE_CLARO = new Color(212, 237, 218);
	private static final Color VERDE_HOVER = new Color(82, 165, 128);
	private static final Color VERDE_TABLA_1 = new Color(233, 245, 236);
	private static final Color VERDE_TABLA_2 = Color.WHITE;
	private static final Color VERDE_REGISTRO = new Color(198, 230, 210);
	private static final Color AMARILLO_DATO = new Color(255, 243, 205);
	private static final Color LILA_IDENTIFICADOR = new Color(225, 222, 240);
	private static final Color ROJO_ERROR = new Color(255, 214, 214);
	private static final Color TEXTO_OSCURO = new Color(27, 67, 50);

	private JTextArea areaEntrada;
	private JTable tablaTokens;
	private DefaultTableModel modeloTabla;
	private JLabel lblOperadores, lblRegistros, lblComas, lblDesconocidos, lblErroresSemanticos, lblTotal;

	private final AnalizadorLexico8086 analizador = new AnalizadorLexico8086();
	private final AnalizadorSemantico8086 semantico = new AnalizadorSemantico8086();

	public InterfazAnalizador8086() {
		super("Analizador Léxico + Sintactico + Semántico 8086");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1080, 680);
		setLocationRelativeTo(null);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().setBackground(Color.WHITE);

		getContentPane().add(construirHeader(), BorderLayout.NORTH);
		getContentPane().add(construirCentro(), BorderLayout.CENTER);
		getContentPane().add(construirResumen(), BorderLayout.SOUTH);

		cargarEjemplo();
	}

	private JPanel construirHeader() {
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(VERDE_OSCURO);
		header.setBorder(new EmptyBorder(18, 24, 18, 24));

		JLabel titulo = new JLabel("Analizador Lexico + Sintactico + Semantico....Instrucciones 8086");
		titulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
		titulo.setForeground(Color.WHITE);

		JLabel subtitulo = new JLabel(
				"ADD · SUB · INC · DEC   |   AL AH BL BH CL CH DL DH   |   NUM / CHAR nombre_variable");
		subtitulo.setFont(new Font("Consolas", Font.PLAIN, 13));
		subtitulo.setForeground(VERDE_CLARO);

		JPanel textos = new JPanel(new GridLayout(2, 1));
		textos.setOpaque(false);
		textos.add(titulo);
		textos.add(subtitulo);

		header.add(textos, BorderLayout.WEST);
		return header;
	}

	private JPanel construirCentro() {
		JPanel centro = new JPanel(new GridLayout(1, 2, 16, 0));
		centro.setBorder(new EmptyBorder(16, 16, 8, 16));
		centro.setBackground(Color.WHITE);

		JPanel panelEntrada = new JPanel(new BorderLayout(0, 10));
		panelEntrada.setBackground(Color.WHITE);

		JLabel lblEntrada = new JLabel("Bloque de entrada");
		lblEntrada.setFont(new Font("Segoe UI", Font.BOLD, 14));
		lblEntrada.setForeground(TEXTO_OSCURO);

		areaEntrada = new JTextArea();
		areaEntrada.setFont(new Font("Consolas", Font.PLAIN, 14));
		areaEntrada.setLineWrap(false);
		areaEntrada.setBorder(BorderFactory.createLineBorder(VERDE_MEDIO, 2));
		areaEntrada.setBackground(new Color(250, 253, 251));
		JScrollPane scrollEntrada = new JScrollPane(areaEntrada);
		scrollEntrada.setBorder(BorderFactory.createEmptyBorder());

		JButton btnAnalizar = crearBoton("Analizar");
		btnAnalizar.addActionListener(e -> analizarEntrada());

		JButton btnLimpiar = crearBotonSecundario("Limpiar");
		btnLimpiar.addActionListener(e -> {
			areaEntrada.setText("");
			modeloTabla.setRowCount(0);
			actualizarResumen(0, 0, 0, 0, 0);
		});

		JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		panelBotones.setBackground(Color.WHITE);
		panelBotones.add(btnAnalizar);
		panelBotones.add(btnLimpiar);

		panelEntrada.add(lblEntrada, BorderLayout.NORTH);
		panelEntrada.add(scrollEntrada, BorderLayout.CENTER);
		panelEntrada.add(panelBotones, BorderLayout.SOUTH);

		JPanel panelTokens = new JPanel(new BorderLayout(0, 10));
		panelTokens.setBackground(Color.WHITE);

		JLabel lblTokens = new JLabel("Tokens reconocidos");
		lblTokens.setFont(new Font("Segoe UI", Font.BOLD, 14));
		lblTokens.setForeground(TEXTO_OSCURO);

		String[] columnas = { "Tipo", "Lexema", "Línea", "Columna" };
		modeloTabla = new DefaultTableModel(columnas, 0) {
			@Override
			public boolean isCellEditable(int row, int col) {
				return false;
			}
		};
		tablaTokens = new JTable(modeloTabla);
		tablaTokens.setFont(new Font("Consolas", Font.PLAIN, 13));
		tablaTokens.setRowHeight(26);
		tablaTokens.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
		tablaTokens.getTableHeader().setBackground(VERDE_MEDIO);
		tablaTokens.getTableHeader().setForeground(Color.BLACK);
		tablaTokens.setSelectionBackground(VERDE_HOVER);
		tablaTokens.setGridColor(new Color(220, 235, 225));
		tablaTokens.setDefaultRenderer(Object.class, new RenderizadorFilas());

		JScrollPane scrollTabla = new JScrollPane(tablaTokens);
		scrollTabla.setBorder(BorderFactory.createLineBorder(VERDE_MEDIO, 2));

		panelTokens.add(lblTokens, BorderLayout.NORTH);
		panelTokens.add(scrollTabla, BorderLayout.CENTER);

		centro.add(panelEntrada);
		centro.add(panelTokens);
		return centro;
	}

	private JPanel construirResumen() {
		JPanel resumen = new JPanel(new GridLayout(1, 6, 10, 0));
		resumen.setBorder(new EmptyBorder(8, 16, 16, 16));
		resumen.setBackground(Color.WHITE);

		lblOperadores = crearBadge("Operadores", VERDE_MEDIO);
		lblRegistros = crearBadge("Registros", VERDE_OSCURO);
		lblComas = crearBadge("Comas", new Color(90, 130, 110));
		lblDesconocidos = crearBadge("Errores léxicos", new Color(180, 90, 90));
		lblErroresSemanticos = crearBadge("Errores semánticos", new Color(150, 60, 120));
		lblTotal = crearBadge("Total tokens", new Color(40, 100, 75));

		resumen.add(lblOperadores);
		resumen.add(lblRegistros);
		resumen.add(lblComas);
		resumen.add(lblDesconocidos);
		resumen.add(lblErroresSemanticos);
		resumen.add(lblTotal);
		return resumen;
	}

	private JLabel crearBadge(String titulo, Color color) {
		JLabel lbl = new JLabel("<html><center>" + titulo + "<br><span style='font-size:18px'>0</span></center></html>",
				SwingConstants.CENTER);
		lbl.setOpaque(true);
		lbl.setBackground(color);
		lbl.setForeground(Color.WHITE);
		lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
		lbl.setBorder(new EmptyBorder(10, 10, 10, 10));
		return lbl;
	}

	private void actualizarBadge(JLabel lbl, String titulo, int valor) {
		lbl.setText(
				"<html><center>" + titulo + "<br><span style='font-size:18px'>" + valor + "</span></center></html>");
	}

	private JButton crearBoton(String texto) {
		JButton btn = new JButton(texto);
		btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
		btn.setBackground(SystemColor.inactiveCaption);
		btn.setForeground(Color.WHITE);
		btn.setFocusPainted(false);
		btn.setBorder(new EmptyBorder(10, 24, 10, 24));
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btn.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent e) {
				btn.setBackground(VERDE_HOVER);
			}

			public void mouseExited(java.awt.event.MouseEvent e) {
				btn.setBackground(VERDE_MEDIO);
			}
		});
		return btn;
	}

	private JButton crearBotonSecundario(String texto) {
		JButton btn = new JButton(texto);
		btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		btn.setBackground(Color.WHITE);
		btn.setForeground(VERDE_OSCURO);
		btn.setFocusPainted(false);
		btn.setBorder(BorderFactory.createLineBorder(VERDE_MEDIO, 2));
		btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		return btn;
	}

	private void cargarEjemplo() {

		areaEntrada.setText("NUM contador\n" + "CHAR letra\n" + "ADD AL, BH\n" + "SUB AH, BL\n" + "INC AL\n"
				+ "DEC BH\n" + "ADD BL, CH\n" + "SUB CH, DL\n" + "INC DL\n" + "DEC AL\n" + "ADD AH, BL\n"
				+ "SUB BL, AL\n" + "ADD contador, letra\n");
	}

	private void analizarEntrada() {
		String entrada = areaEntrada.getText();
		List<AnalizadorLexico8086.Token> tokens = analizador.analizar(entrada);

		List<AnalizadorSemantico8086.ErrorSemantico> erroresSemanticos = semantico.analizar(tokens);

		modeloTabla.setRowCount(0);
		int operadores = 0, registros = 0, comas = 0, desconocidos = 0;

		List<AnalizadorLexico8086.Token> erroresLexicos = new ArrayList<>();

		for (AnalizadorLexico8086.Token t : tokens) {
			if (t.tipo == AnalizadorLexico8086.TipoToken.ESPACIO)
				continue;

			modeloTabla.addRow(new Object[] { t.tipo.name(), t.lexema, t.fila, t.columna });

			switch (t.tipo) {
			case OPERADOR:
				operadores++;
				break;
			case REGISTRO:
				registros++;
				break;
			case COMA:
				comas++;
				break;
			case DESCONOCIDO:
				desconocidos++;
				erroresLexicos.add(t);
				break;
			default:
				break;
			}
		}

		long totalErroresSemanticos = erroresSemanticos.stream().filter(e -> !e.esAdvertencia).count();

		actualizarResumen(operadores, registros, comas, desconocidos, (int) totalErroresSemanticos);

		
		mostrarVentanaDiagnostico(erroresLexicos, erroresSemanticos, operadores + registros + comas + desconocidos);
	}

	private void mostrarVentanaDiagnostico(List<AnalizadorLexico8086.Token> erroresLexicos,
			List<AnalizadorSemantico8086.ErrorSemantico> erroresSemanticos, int totalTokens) {

		List<AnalizadorSemantico8086.ErrorSemantico> soloErrores = new ArrayList<>();
		List<AnalizadorSemantico8086.ErrorSemantico> soloAdvertencias = new ArrayList<>();
		for (AnalizadorSemantico8086.ErrorSemantico e : erroresSemanticos) {
			if (e.esAdvertencia) {
				soloAdvertencias.add(e);
			} else {
				soloErrores.add(e);
			}
		}

		if (erroresLexicos.isEmpty() && soloErrores.isEmpty() && soloAdvertencias.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					"No se encontraron errores lexicos ni semanticos.\n" + "Se reconocieron correctamente "
							+ totalTokens + " tokens y todas\n" + "las instrucciones tienen sentido logico.",
					"Analisis exitoso", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		StringBuilder mensaje = new StringBuilder();

		if (!erroresLexicos.isEmpty()) {
			mensaje.append("=== ERRORES LEXICOS (").append(erroresLexicos.size()).append(") ===\n\n");
			for (AnalizadorLexico8086.Token e : erroresLexicos) {
				mensaje.append("- Linea ").append(e.fila).append(", columna ").append(e.columna).append(": el texto \"")
						.append(e.lexema).append("\" no es un token valido.\n").append("  Sugerencia: ")
						.append(analizador.sugerenciaParaError(e.lexema)).append("\n\n");
			}
		}

		if (!soloErrores.isEmpty()) {
			mensaje.append("=== ERRORES SEMANTICOS (").append(soloErrores.size()).append(") ===\n\n");
			for (AnalizadorSemantico8086.ErrorSemantico e : soloErrores) {
				mensaje.append("- Linea ").append(e.linea).append(": ").append(e.mensaje).append("\n\n");
			}
		}

		if (!soloAdvertencias.isEmpty()) {
			mensaje.append("=== ADVERTENCIAS (").append(soloAdvertencias.size()).append(") ===\n\n");
			for (AnalizadorSemantico8086.ErrorSemantico e : soloAdvertencias) {
				mensaje.append("- Linea ").append(e.linea).append(": ").append(e.mensaje).append("\n\n");
			}
		}

		mensaje.append("Reglas del lenguaje:\n").append("  Operadores    -> ADD, SUB, INC, DEC\n")
				.append("  Registros     -> AL, AH, BL, BH, CL, CH, DL, DH\n")
				.append("  Declaraciones -> NUM nombre  |  CHAR nombre\n")
				.append("  ADD/SUB/INC/DEC solo admiten operandos NUMERICOS");

		boolean hayErroresGraves = !erroresLexicos.isEmpty() || !soloErrores.isEmpty();

		JOptionPane.showMessageDialog(this, mensaje.toString(),
				hayErroresGraves ? "Se detectaron errores" : "Analisis con advertencias",
				hayErroresGraves ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE);
	}

	private void actualizarResumen(int operadores, int registros, int comas, int desconocidos, int erroresSemanticos) {
		actualizarBadge(lblOperadores, "Operadores", operadores);
		actualizarBadge(lblRegistros, "Registros", registros);
		actualizarBadge(lblComas, "Comas", comas);
		actualizarBadge(lblDesconocidos, "Errores léxicos", desconocidos);
		actualizarBadge(lblErroresSemanticos, "Errores semánticos", erroresSemanticos);
		actualizarBadge(lblTotal, "Total tokens", operadores + registros + comas + desconocidos);
	}

	private class RenderizadorFilas extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (!isSelected) {
				String tipo = (String) table.getValueAt(row, 0);
				if ("DESCONOCIDO".equals(tipo)) {
					c.setBackground(ROJO_ERROR);
				} else if ("REGISTRO".equals(tipo)) {
					c.setBackground(VERDE_REGISTRO);
				} else if ("DATO".equals(tipo)) {
					c.setBackground(AMARILLO_DATO);
				} else if ("IDENTIFICADOR".equals(tipo)) {
					c.setBackground(LILA_IDENTIFICADOR);
				} else if ("OPERADOR".equals(tipo)) {
					c.setBackground(row % 2 == 0 ? VERDE_TABLA_1 : VERDE_TABLA_2);
				} else {
					c.setBackground(row % 2 == 0 ? VERDE_TABLA_1 : VERDE_TABLA_2);
				}
			}
			setForeground(TEXTO_OSCURO);
			return c;
		}
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {
		}

		SwingUtilities.invokeLater(() -> new InterfazAnalizador8086().setVisible(true));
	}
}