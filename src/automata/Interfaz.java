package automata;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Interfaz extends JFrame {
/*
 * Tiene que llevar key las validaciones, y con arrays 
 * va a hacer examen practico y va a llevar esoo..
 */
    private final ValidarExpresionRegular validador = new ValidarExpresionRegular();

    public Interfaz() {

        setTitle("Validador de cadenas - Autómatas Finitos");
        setSize(600, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane pestañas = new JTabbedPane();

        pestañas.addTab("1. L=(ab)+",
                crearPanel("Ejercicio 1: L = (ab)+  (una o más repeticiones de \"ab\")",
                        validador.definicionFormalEjercicio1(),
                        validador.columnasEjercicio1(),
                        validador.tablaEjercicio1(),
                        validador::validarEjercicio1));

        pestañas.addTab("2. Termina en ab",
                crearPanel("Ejercicio 2: Acepta todas las cadenas que terminan en \"ab\"",
                        validador.definicionFormalEjercicio2(),
                        validador.columnasEjercicio2(),
                        validador.tablaEjercicio2(),
                        validador::validarEjercicio2));

        pestañas.addTab("3. Sin 3 b's seguidas",
                crearPanel("Ejercicio 3: Lenguaje formado por cadenas que no contengan 3 o más \"b\" consecutivas",
                        validador.definicionFormalEjercicio3(),
                        validador.columnasEjercicio3(),
                        validador.tablaEjercicio3(),
                        validador::validarEjercicio3));

        pestañas.addTab("4. Paridad a/b",
                crearPanel("Ejercicio 4: Lenguaje formado por un número par de \"a\" y un número par de \"b\"",
                        validador.definicionFormalEjercicio4(),
                        validador.columnasEjercicio4(),
                        validador.tablaEjercicio4(),
                        validador::validarEjercicio4));

        add(pestañas);
        setVisible(true);
    }

    private JPanel crearPanel(String descripcion, String definicionFormal,
                               String[] columnas, String[][] datosTabla,
                               java.util.function.Function<String, String> funcionValidacion) {

        JPanel panelPrincipal = new JPanel(new BorderLayout(0, 8));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panelSuperior = new JPanel();
        panelSuperior.setLayout(new BoxLayout(panelSuperior, BoxLayout.Y_AXIS));

        JLabel lblDescripcion = new JLabel("<html><body style='width: 500px'>" + descripcion + "</body></html>");
        lblDescripcion.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblDescripcion.setFont(new Font("SansSerif", Font.BOLD, 13));

        JTextArea areaDefinicion = new JTextArea(definicionFormal);
        areaDefinicion.setEditable(false);
        areaDefinicion.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaDefinicion.setBackground(new Color(245, 245, 245));
        areaDefinicion.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JPanel panelDefinicion = new JPanel(new BorderLayout());
        panelDefinicion.setBorder(BorderFactory.createTitledBorder("Definición formal"));
        panelDefinicion.add(areaDefinicion, BorderLayout.CENTER);
        panelDefinicion.setMaximumSize(new Dimension(500, 130));
        panelDefinicion.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel panelTexto = new JPanel(new FlowLayout());
        JLabel lblIngresar = new JLabel("Cadena sobre {a,b}:");
        JTextField campoTexto = new JTextField(18);

        campoTexto.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char tecla = e.getKeyChar();
                if (tecla != 'a' && tecla != 'b' &&
                    tecla != 'A' && tecla != 'B' &&
                    tecla != KeyEvent.VK_BACK_SPACE) {
                    e.consume();
                }
            }
        });

        panelTexto.add(lblIngresar);
        panelTexto.add(campoTexto);
        panelTexto.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelTexto.setMaximumSize(panelTexto.getPreferredSize());

        JPanel panelResultado = new JPanel(new FlowLayout());
        JLabel etiquetaResultado = new JLabel(" ");
        etiquetaResultado.setFont(new Font("SansSerif", Font.BOLD, 14));
        panelResultado.add(etiquetaResultado);
        panelResultado.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelResultado.setPreferredSize(new Dimension(300, 30));
        panelResultado.setMaximumSize(new Dimension(500, 30));

        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnValidar = new JButton("Validar Cadena");
        JButton btnReiniciar = new JButton("Reiniciar");

        btnValidar.addActionListener(e -> {
            String cadena = campoTexto.getText();
            String resultado = funcionValidacion.apply(cadena);
            etiquetaResultado.setText(resultado);
            etiquetaResultado.setForeground(
                    resultado.equals("Cadena válida") ? new Color(0, 128, 0) : Color.RED);
            panelResultado.revalidate();
            panelResultado.repaint();
        });

        btnReiniciar.addActionListener(e -> {
            campoTexto.setText("");
            etiquetaResultado.setText(" ");
            campoTexto.requestFocus();
        });

        campoTexto.addActionListener(e -> btnValidar.doClick());

        panelBotones.add(btnValidar);
        panelBotones.add(btnReiniciar);
        panelBotones.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelBotones.setMaximumSize(panelBotones.getPreferredSize());

        panelSuperior.add(lblDescripcion);
        panelSuperior.add(Box.createVerticalStrut(6));
        panelSuperior.add(panelDefinicion);
        panelSuperior.add(Box.createVerticalStrut(6));
        panelSuperior.add(panelTexto);
        panelSuperior.add(panelBotones);
        panelSuperior.add(panelResultado);

        DefaultTableModel modelo = new DefaultTableModel(datosTabla, columnas) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabla = new JTable(modelo);
        tabla.setEnabled(false);
        tabla.setRowHeight(24);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createTitledBorder("Tabla de transiciones"));

        JLabel lblLeyenda = new JLabel("(\"->\" = estado inicial, \"*\" = estado de aceptación)");
        lblLeyenda.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblLeyenda.setHorizontalAlignment(SwingConstants.CENTER);

        panelPrincipal.add(panelSuperior, BorderLayout.NORTH);
        panelPrincipal.add(scrollTabla, BorderLayout.CENTER);
        panelPrincipal.add(lblLeyenda, BorderLayout.SOUTH);

        return panelPrincipal;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Interfaz::new);
    }
}