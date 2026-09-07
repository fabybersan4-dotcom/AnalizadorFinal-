package analizadorLexico;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;

public class InterfazAnalizador extends JFrame {
	private JTextArea areaTexto;
	private JButton btnAbrir, btnAnalizar, btnTablaSimbolos, btnListaTokens;
	private JTextArea areaErrores;
	private LexicalAnalyzer analizador;

	public InterfazAnalizador() {
		analizador = new LexicalAnalyzer();
		inicializarComponentes();
	}

	private void inicializarComponentes() {
		setTitle("Analizador Léxico");
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(10, 10));

		// Panel superior con botones
		JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));

		btnAbrir = new JButton("Abrir");
		btnAnalizar = new JButton("Analizar");
		btnTablaSimbolos = new JButton("Tabla de Símbolos");
		btnListaTokens = new JButton("Lista de Tokens");

		panelBotones.add(btnAbrir);
		panelBotones.add(btnAnalizar);
		panelBotones.add(btnTablaSimbolos);
		panelBotones.add(btnListaTokens);

		// Área de texto para el código fuente
		areaTexto = new JTextArea();
		areaTexto.setFont(new Font("Monospaced", Font.PLAIN, 14));
		JScrollPane scrollTexto = new JScrollPane(areaTexto);
		scrollTexto.setBorder(BorderFactory.createTitledBorder("Código Fuente"));

		// Área de errores
		areaErrores = new JTextArea(8, 50);
		areaErrores.setEditable(false);
		areaErrores.setFont(new Font("Monospaced", Font.PLAIN, 12));
		areaErrores.setForeground(Color.RED);
		JScrollPane scrollErrores = new JScrollPane(areaErrores);
		scrollErrores.setBorder(BorderFactory.createTitledBorder("Errores"));

		// Agregar componentes al frame
		add(panelBotones, BorderLayout.NORTH);
		add(scrollTexto, BorderLayout.CENTER);
		add(scrollErrores, BorderLayout.SOUTH);

		// Eventos de botones
		btnAbrir.addActionListener(e -> abrirArchivo());
		btnAnalizar.addActionListener(e -> analizar());
		btnTablaSimbolos.addActionListener(e -> mostrarTablaSimbolos());
		btnListaTokens.addActionListener(e -> mostrarListaTokens());
	}

	private void abrirArchivo() {
		JFileChooser fileChooser = new JFileChooser();
		int resultado = fileChooser.showOpenDialog(this);

		if (resultado == JFileChooser.APPROVE_OPTION) {
			File archivo = fileChooser.getSelectedFile();
			try {
				BufferedReader reader = new BufferedReader(new FileReader(archivo));
				areaTexto.setText("");
				String linea;
				while ((linea = reader.readLine()) != null) {
					areaTexto.append(linea + "\n");
				}
				reader.close();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Error al abrir el archivo: " + ex.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void analizar() {
		String codigo = areaTexto.getText();

		if (codigo.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Por favor ingrese código para analizar", "Advertencia",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		analizador.analizar(codigo);

		areaErrores.setText("");
		if (analizador.getErrores().isEmpty()) {
			areaErrores.setText("Análisis completado sin errores.");
			areaErrores.setForeground(new Color(0, 128, 0));
		} else {
			for (String error : analizador.getErrores()) {
				areaErrores.append(error + "\n");
			}
			areaErrores.setForeground(Color.RED);
		}
	}

	private void mostrarTablaSimbolos() {
		if (analizador.getTablaSimbolos().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Primero debe analizar el código", "Advertencia",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		JDialog dialog = new JDialog(this, "Tabla de Símbolos", true);
		dialog.setSize(600, 400);
		dialog.setLocationRelativeTo(this);

		String[] columnas = { "ID", "Tipo", "Valor" };
		DefaultTableModel modelo = new DefaultTableModel(columnas, 0);

		for (Simbolo simbolo : analizador.getTablaSimbolos().values()) {
			modelo.addRow(new Object[] { simbolo.getId(), simbolo.getTipo(), simbolo.getValor() });
		}

		JTable tabla = new JTable(modelo);
		tabla.setFont(new Font("Monospaced", Font.PLAIN, 12));
		JScrollPane scroll = new JScrollPane(tabla);

		dialog.add(scroll);
		dialog.setVisible(true);
	}

	private void mostrarListaTokens() {
		if (analizador.getTokens().isEmpty()) {
			JOptionPane.showMessageDialog(this, "Primero debe analizar el código", "Advertencia",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		JDialog dialog = new JDialog(this, "Lista de Tokens", true);
		dialog.setSize(600, 400);
		dialog.setLocationRelativeTo(this);

		String[] columnas = { "Lexema", "Token" };
		DefaultTableModel modelo = new DefaultTableModel(columnas, 0);

		for (Token token : analizador.getTokens()) {
			modelo.addRow(new Object[] { token.getLexema(), token.getTipoString() });
		}

		JTable tabla = new JTable(modelo);
		tabla.setFont(new Font("Monospaced", Font.PLAIN, 12));
		JScrollPane scroll = new JScrollPane(tabla);

		dialog.add(scroll);
		dialog.setVisible(true);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			InterfazAnalizador interfaz = new InterfazAnalizador();
			interfaz.setVisible(true);
		});
	}
}