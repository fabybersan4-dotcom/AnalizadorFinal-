package analizadorLexico;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Analizador de Sentencias Condicionales
 * -----------------------------------------------------
 * Simula el AFD (Autómata Finito Determinista) que reconoce los tokens
 * de una sentencia del tipo:  if ( ID/NUM OP_REL ID/NUM )
 *
 * Tokens reconocidos:
 *   PR_IF     -> palabra reservada "if"
 *   PR_WHILE  -> palabra reservada "while"
 *   PI        -> paréntesis de apertura "("
 *   PD        -> paréntesis de cierre  ")"
 *   ID        -> [a-zA-Z][a-zA-Z0-9]*
 *   NUM       -> [0-9]+
 *   OP_REL    -> < , > , == , !=
 *   ERROR     -> cualquier símbolo no perteneciente al alfabeto (Σ)
 *
 * Gramática usada para validar la cadena completa (Tarea 3),
 * ahora extendida para aceptar sentencias if / while anidadas
 * (una sentencia seguida inmediatamente de otra representa anidamiento,
 * ya que el lenguaje no usa llaves de bloque):
 *
 *   S         -> SENTENCIA S | SENTENCIA
 *   SENTENCIA -> if ( OPERANDO OP_REL OPERANDO )
 *              | while ( OPERANDO OP_REL OPERANDO )
 *   OPERANDO  -> ID | NUM
 *
 * Ejemplos válidos:
 *   if(a>b)
 *   if(a>b)while(c>d)
 *   while(x<10)if(y==5)if(z!=0)
 */
public class AnalizadorCondicional extends JFrame {

    // ---------- Paleta de colores ----------
    private static final Color COLOR_HEADER   = new Color(30, 41, 59);
    private static final Color COLOR_ACCENT   = new Color(20, 184, 166);
    private static final Color COLOR_BG       = new Color(248, 250, 252);
    private static final Color COLOR_VALID    = new Color(22, 163, 74);
    private static final Color COLOR_INVALID  = new Color(220, 38, 38);
    private static final Color COLOR_TABLE_HEAD = new Color(51, 65, 85);
    private static final Color COLOR_ROW_ALT  = new Color(241, 245, 249);

    private JTextField campoEntrada;
    private JLabel etiquetaResultado;
    private JLabel etiquetaDetalle;
    private DefaultTableModel modeloTabla;
    private JTable tablaSimbolos;
    private JTextArea areaTokens;

    public AnalizadorCondicional() {
        super("Analizador de Sentencias Condicionales - AFD");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(880, 640);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout());

        add(construirHeader(), BorderLayout.NORTH);
        add(construirCentro(), BorderLayout.CENTER);

        setVisible(true);
    }

    // ================= INTERFAZ =================

    private JPanel construirHeader() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_HEADER);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel titulo = new JLabel("Analizador de Sentencias Condicionales");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titulo.setForeground(Color.WHITE);

        JLabel subtitulo = new JLabel("AFD  →  Tabla de Símbolos  →  Gramática (if / while anidados)");
        subtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitulo.setForeground(new Color(203, 213, 225));
        subtitulo.setBorder(new EmptyBorder(4, 0, 0, 0));

        panel.add(titulo);
        panel.add(subtitulo);
        return panel;
    }

    private JPanel construirCentro() {
        JPanel contenedor = new JPanel(new BorderLayout(0, 16));
        contenedor.setBackground(COLOR_BG);
        contenedor.setBorder(new EmptyBorder(20, 24, 20, 24));

        contenedor.add(construirPanelEntrada(), BorderLayout.NORTH);
        contenedor.add(construirPanelResultados(), BorderLayout.CENTER);

        return contenedor;
    }

    private JPanel construirPanelEntrada() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(16, 16, 16, 16)));

        JLabel etiqueta = new JLabel("Ingresa la sentencia condicional:");
        etiqueta.setFont(new Font("Segoe UI", Font.BOLD, 14));
        etiqueta.setForeground(COLOR_HEADER);

        campoEntrada = new JTextField("if(a>b)");
        campoEntrada.setFont(new Font("Consolas", Font.PLAIN, 16));
        campoEntrada.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(8, 10, 8, 10)));

        JButton botonAnalizar = new JButton("Analizar");
        botonAnalizar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        botonAnalizar.setBackground(COLOR_ACCENT);
        botonAnalizar.setForeground(Color.WHITE);
        botonAnalizar.setFocusPainted(false);
        botonAnalizar.setBorder(new EmptyBorder(10, 22, 10, 22));
        botonAnalizar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botonAnalizar.addActionListener(e -> analizar());
        campoEntrada.addActionListener(e -> analizar());

        JPanel filaSuperior = new JPanel(new BorderLayout());
        filaSuperior.setBackground(Color.WHITE);
        filaSuperior.add(etiqueta, BorderLayout.WEST);

        JPanel filaEntrada = new JPanel(new BorderLayout(10, 0));
        filaEntrada.setBackground(Color.WHITE);
        filaEntrada.add(campoEntrada, BorderLayout.CENTER);
        filaEntrada.add(botonAnalizar, BorderLayout.EAST);
        filaEntrada.setBorder(new EmptyBorder(8, 0, 0, 0));

        panel.add(filaSuperior, BorderLayout.NORTH);
        panel.add(filaEntrada, BorderLayout.CENTER);
        return panel;
    }

    private JPanel construirPanelResultados() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(COLOR_BG);

        // ---- Panel de veredicto ----
        JPanel veredicto = new JPanel();
        veredicto.setLayout(new BoxLayout(veredicto, BoxLayout.Y_AXIS));
        veredicto.setBackground(Color.WHITE);
        veredicto.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(14, 16, 14, 16)));

        etiquetaResultado = new JLabel("Ingresa una cadena y presiona \"Analizar\"");
        etiquetaResultado.setFont(new Font("Segoe UI", Font.BOLD, 18));
        etiquetaResultado.setForeground(COLOR_HEADER);

        etiquetaDetalle = new JLabel(" ");
        etiquetaDetalle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        etiquetaDetalle.setForeground(new Color(100, 116, 139));
        etiquetaDetalle.setBorder(new EmptyBorder(4, 0, 0, 0));

        veredicto.add(etiquetaResultado);
        veredicto.add(etiquetaDetalle);

        // ---- Tabla de símbolos ----
        String[] columnas = {"#", "Lexema", "Token", "Atributo"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaSimbolos = new JTable(modeloTabla);
        tablaSimbolos.setFont(new Font("Consolas", Font.PLAIN, 14));
        tablaSimbolos.setRowHeight(26);
        tablaSimbolos.setShowGrid(false);
        tablaSimbolos.setIntercellSpacing(new Dimension(0, 0));
        tablaSimbolos.setSelectionBackground(new Color(204, 251, 241));
        tablaSimbolos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tablaSimbolos.getTableHeader().setBackground(COLOR_TABLE_HEAD);
        tablaSimbolos.getTableHeader().setForeground(Color.WHITE);
        tablaSimbolos.getTableHeader().setPreferredSize(new Dimension(0, 32));
        tablaSimbolos.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                            boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : COLOR_ROW_ALT);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        });
        tablaSimbolos.getColumnModel().getColumn(0).setMaxWidth(40);
        tablaSimbolos.getColumnModel().getColumn(1).setPreferredWidth(120);
        tablaSimbolos.getColumnModel().getColumn(2).setPreferredWidth(100);
        tablaSimbolos.getColumnModel().getColumn(3).setPreferredWidth(250);

        JScrollPane scrollTabla = new JScrollPane(tablaSimbolos);
        scrollTabla.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(0, 0, 0, 0)));

        JLabel tituloTabla = new JLabel("Tabla de Símbolos (orden de ingreso)");
        tituloTabla.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tituloTabla.setForeground(COLOR_HEADER);
        tituloTabla.setBorder(new EmptyBorder(0, 2, 8, 0));

        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBackground(COLOR_BG);
        panelTabla.add(tituloTabla, BorderLayout.NORTH);
        panelTabla.add(scrollTabla, BorderLayout.CENTER);

        panel.add(veredicto, BorderLayout.NORTH);
        panel.add(panelTabla, BorderLayout.CENTER);
        return panel;
    }

    // ================= LÓGICA (AFD + Gramática) =================

    /** Representa un token reconocido por el AFD */
    private static class Token {
        String lexema, tipo, atributo;
        Token(String lexema, String tipo, String atributo) {
            this.lexema = lexema; this.tipo = tipo; this.atributo = atributo;
        }
    }

    private void analizar() {
        String entrada = campoEntrada.getText();
        modeloTabla.setRowCount(0);

        List<String> errores = new ArrayList<>();
        List<Token> tokens = tokenizar(entrada, errores);

        // Llenar tabla de símbolos con TODOS los tokens reconocidos, en orden
        int contador = 1;
        for (Token t : tokens) {
            modeloTabla.addRow(new Object[]{contador++, t.lexema, t.tipo, t.atributo});
        }

        if (!errores.isEmpty()) {
            mostrarInvalida("Se encontraron símbolos fuera del alfabeto (Σ): " + errores.get(0));
            return;
        }

        String errorGramatica = validarGramatica(tokens);
        if (errorGramatica == null) {
            mostrarValida();
        } else {
            mostrarInvalida(errorGramatica);
        }
    }

    private void mostrarValida() {
        etiquetaResultado.setText("✓ Cadena VÁLIDA");
        etiquetaResultado.setForeground(COLOR_VALID);
        etiquetaDetalle.setText("La cadena cumple la gramática:  S → SENTENCIA S | SENTENCIA   (if/while anidados)");
    }

    private void mostrarInvalida(String motivo) {
        etiquetaResultado.setText("✗ Cadena INVÁLIDA");
        etiquetaResultado.setForeground(COLOR_INVALID);
        etiquetaDetalle.setText(motivo);
    }

    /**
     * Simula el AFD recorriendo la cadena carácter por carácter.
     * Reconoce: PR_IF, PI, PD, ID, NUM, OP_REL. Cualquier símbolo
     * fuera de Σ se reporta como ERROR.
     */
    private List<Token> tokenizar(String input, List<String> errores) {
        List<Token> tokens = new ArrayList<>();
        Map<String, Integer> direcciones = new LinkedHashMap<>();
        int siguienteDireccion = 1000;
        int i = 0, n = input.length();

        while (i < n) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) { i++; continue; }

            // ---- Estado: letras -> identificador o palabra reservada "if" ----
            if (Character.isLetter(c)) {
                int inicio = i;
                while (i < n && Character.isLetterOrDigit(input.charAt(i))) i++;
                String lexema = input.substring(inicio, i);
                if (lexema.equals("if")) {
                    tokens.add(new Token(lexema, "PR_IF", "Palabra reservada"));
                } else if (lexema.equals("while")) {
                    tokens.add(new Token(lexema, "PR_WHILE", "Palabra reservada"));
                } else {
                    int dir;
                    if (direcciones.containsKey(lexema)) {
                        dir = direcciones.get(lexema);
                    } else {
                        dir = siguienteDireccion;
                        direcciones.put(lexema, dir);
                        siguienteDireccion += 4;
                    }
                    tokens.add(new Token(lexema, "ID", "Dirección de memoria: " + dir));
                }
                continue;
            }

            // ---- Estado: dígitos -> número ----
            if (Character.isDigit(c)) {
                int inicio = i;
                while (i < n && Character.isDigit(input.charAt(i))) i++;
                String lexema = input.substring(inicio, i);
                tokens.add(new Token(lexema, "NUM", "Valor numérico: " + lexema));
                continue;
            }

            // ---- Delimitadores ----
            if (c == '(') { tokens.add(new Token("(", "PI", "Delimitador de apertura")); i++; continue; }
            if (c == ')') { tokens.add(new Token(")", "PD", "Delimitador de cierre")); i++; continue; }

            // ---- Operadores relacionales ----
            if (c == '<') { tokens.add(new Token("<", "OP_REL", "Relación: menor que")); i++; continue; }
            if (c == '>') { tokens.add(new Token(">", "OP_REL", "Relación: mayor que")); i++; continue; }
            if (c == '=') {
                if (i + 1 < n && input.charAt(i + 1) == '=') {
                    tokens.add(new Token("==", "OP_REL", "Relación: igual que"));
                    i += 2;
                } else {
                    tokens.add(new Token("=", "ERROR", "Símbolo no válido (¿quisiste '=='?)"));
                    errores.add("'=' no es un operador válido en la posición " + (i + 1) + " (usa '==')");
                    i++;
                }
                continue;
            }
            if (c == '!') {
                if (i + 1 < n && input.charAt(i + 1) == '=') {
                    tokens.add(new Token("!=", "OP_REL", "Relación: distinto de"));
                    i += 2;
                } else {
                    tokens.add(new Token("!", "ERROR", "Símbolo no válido"));
                    errores.add("'!' no es válido sin '=' en la posición " + (i + 1));
                    i++;
                }
                continue;
            }

            // ---- Cualquier otro símbolo no pertenece a Σ ----
            tokens.add(new Token(String.valueOf(c), "ERROR", "Símbolo fuera del alfabeto"));
            errores.add("carácter '" + c + "' en la posición " + (i + 1) + " no pertenece a Σ");
            i++;
        }
        return tokens;
    }

    /**
     * Valida la secuencia de tokens contra la gramática:
     *   S         -> SENTENCIA S | SENTENCIA
     *   SENTENCIA -> if ( OPERANDO OP_REL OPERANDO )
     *              | while ( OPERANDO OP_REL OPERANDO )
     *   OPERANDO  -> ID | NUM
     *
     * Esto permite cualquier cantidad de sentencias if/while
     * concatenadas (anidadas), una tras otra.
     * Devuelve null si es válida, o un mensaje describiendo el primer error.
     */
    private String validarGramatica(List<Token> tokens) {
        if (tokens.isEmpty()) return "La cadena está vacía.";

        int idx = 0;
        int numSentencias = 0;

        while (idx < tokens.size()) {
            String palabraClave = tokens.get(idx).tipo.equals("PR_IF") ? "if"
                    : tokens.get(idx).tipo.equals("PR_WHILE") ? "while" : null;

            if (palabraClave == null) {
                return "Se esperaba 'if' o 'while' en la posición del token " + (idx + 1)
                        + " (se encontró '" + tokens.get(idx).lexema + "').";
            }
            idx++;

            if (idx >= tokens.size() || !tokens.get(idx).tipo.equals("PI"))
                return "Falta '(' inmediatamente después de '" + palabraClave + "'.";
            idx++;

            if (idx >= tokens.size() || !(tokens.get(idx).tipo.equals("ID") || tokens.get(idx).tipo.equals("NUM")))
                return "Se esperaba un identificador o número después de '('.";
            idx++;

            if (idx >= tokens.size() || !tokens.get(idx).tipo.equals("OP_REL"))
                return "Se esperaba un operador relacional (<, >, ==, !=).";
            idx++;

            if (idx >= tokens.size() || !(tokens.get(idx).tipo.equals("ID") || tokens.get(idx).tipo.equals("NUM")))
                return "Se esperaba un identificador o número después del operador relacional.";
            idx++;

            if (idx >= tokens.size() || !tokens.get(idx).tipo.equals("PD"))
                return "Falta ')' para cerrar la condición de '" + palabraClave + "'.";
            idx++;

            numSentencias++;
            // Si siguen más tokens, deben ser otra sentencia if/while (anidamiento).
        }

        if (numSentencias == 0) return "No se reconoció ninguna sentencia if/while.";

        return null;
    }

    private String reconstruir(List<Token> tokens, int desde) {
        StringBuilder sb = new StringBuilder();
        for (int i = desde; i < tokens.size(); i++) {
            sb.append(tokens.get(i).lexema).append(" ");
        }
        return sb.toString().trim();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AnalizadorCondicional::new);
    }
}