package expresiones;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.regex.Pattern;

public class Expresiones extends JFrame {
	/*
	Ejemplo 4: Cadenas que tienen al menos dos unos
    Expresion regular: 0*10*1(0 U 1)*
	*/
    private static final String REGEX = "0*10*1(0|1)*";
    private static final Pattern PATRON = Pattern.compile(REGEX);

    private JTextField campoCadena;
    private JTextArea areaResultados;
    private JLabel labelEstado;

    public Expresiones() {
        super("Validador de Expresion Regular - 0*10*1(0 U 1)*");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(crearPanelExplicacion(), BorderLayout.NORTH);
        add(crearPanelCentral(), BorderLayout.CENTER);
        add(crearPanelEntrada(), BorderLayout.SOUTH);
    }

    private JPanel crearPanelExplicacion() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Como se resuelve la expresion"));
        panel.setLayout(new BorderLayout());

        JTextArea texto = new JTextArea();
        texto.setEditable(false);
        texto.setBackground(new Color(245, 245, 245));
        texto.setFont(new Font("Monospaced", Font.PLAIN, 13));
        texto.setText(
            "Expresion regular: 0*10*1(0 U 1)*\n" +
            "(Lenguaje: cadenas con AL MENOS dos unos)\n\n" +
            "Orden de precedencia:\n" +
            "  1) Cerradura de Kleene (*)  -> maxima prioridad\n" +
            "  2) Concatenacion            -> prioridad media\n" +
            "  3) Union (U)                -> minima prioridad\n\n" +
            "Pasos:\n" +
            "  Paso 1: 0*              -> ceros libres antes del 1er uno\n" +
            "  Paso 2: 0*1             -> se concatena el primer '1' obligatorio\n" +
            "  Paso 3: 0*10*           -> ceros libres entre el 1er y 2do uno\n" +
            "  Paso 4: 0*10*1          -> se concatena el segundo '1' obligatorio\n" +
            "  Paso 5: (0 U 1)*        -> cerradura de la union (sufijo libre)\n" +
            "  Paso 6: 0*10*1(0 U 1)*  -> se concatena el sufijo libre al final"
        );
        panel.add(texto, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelCentral() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Resultados"));

        areaResultados = new JTextArea();
        areaResultados.setEditable(false);
        areaResultados.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(areaResultados);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelEntrada() {
        JPanel panelExterno = new JPanel(new BorderLayout(5, 5));
        panelExterno.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JPanel panelInput = new JPanel(new BorderLayout(5, 5));
        campoCadena = new JTextField();
        campoCadena.addActionListener(this::validarAccion);

        JButton botonValidar = new JButton("Validar");
        botonValidar.addActionListener(this::validarAccion);

        JButton botonLimpiar = new JButton("Limpiar resultados");
        botonLimpiar.addActionListener(e -> areaResultados.setText(""));

        JPanel panelBotones = new JPanel(new GridLayout(1, 2, 5, 0));
        panelBotones.add(botonValidar);
        panelBotones.add(botonLimpiar);

        panelInput.add(new JLabel("Cadena: "), BorderLayout.WEST);
        panelInput.add(campoCadena, BorderLayout.CENTER);
        panelInput.add(panelBotones, BorderLayout.EAST);

        labelEstado = new JLabel(" ");
        labelEstado.setFont(new Font("SansSerif", Font.BOLD, 16));
        labelEstado.setHorizontalAlignment(SwingConstants.CENTER);

        panelExterno.add(panelInput, BorderLayout.NORTH);
        panelExterno.add(labelEstado, BorderLayout.SOUTH);
        return panelExterno;
    }

    private void validarAccion(ActionEvent e) {
        String cadena = campoCadena.getText();
        if (cadena.isEmpty()) {
            labelEstado.setText("Escribe una cadena para validar");
            labelEstado.setForeground(Color.GRAY);
            return;
        }

        boolean valida = PATRON.matcher(cadena).matches();
        String resultado = "Cadena: \"" + cadena + "\" -> " + (valida ? "VALIDA" : "NO VALIDA");
        areaResultados.append(resultado + "\n");

        if (valida) {
            labelEstado.setText("VALIDA");
            labelEstado.setForeground(new Color(0, 128, 0));
        } else {
            labelEstado.setText("NO VALIDA");
            labelEstado.setForeground(Color.RED);
        }

        campoCadena.setText("");
        campoCadena.requestFocus();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
           Expresiones ventana = new Expresiones();
            ventana.setVisible(true);
        });
    }
}