package analizadorLexico;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class AnalizadorLexico8086 {

    // ======================================================================
    // NUEVO: se agregan DATO (palabra reservada de tipo: NUM / CHAR) e
    // IDENTIFICADOR (nombre de variable, ej. "contador", "letra").
    enum TipoToken {
        OPERADOR,
        REGISTRO,
        DATO,
        IDENTIFICADOR,
        COMA,
        ESPACIO,
        DESCONOCIDO
    }

    static class Token {
        String lexema;
        TipoToken tipo;
        int fila;
        int columna;

        Token(String lexema, TipoToken tipo, int fila, int columna) {
            this.lexema = lexema;
            this.tipo = tipo;
            this.fila = fila;
            this.columna = columna;
        }

        @Override
        public String toString() {
            return "[" + tipo + "]".concat(pad(tipo.name())) +
                    " lexema=\"" + lexema + "\"" +
                    "  (linea " + fila + ", col " + columna + ")";
        }

        private String pad(String s) {
            StringBuilder sb = new StringBuilder();
            int faltan = 15 - s.length();
            for (int i = 0; i < faltan; i++) {
                sb.append(' ');
            }
            return sb.toString();
        }
    }

    private static final int ESTADO_INICIO         = 0;
    private static final int ESTADO_LEYENDO_PALABRA = 1;
    private static final int ESTADO_ERROR           = 2;

    private static final String[] OPERADORES_VALIDOS = {"ADD", "SUB", "INC", "DEC"};
    private static final String[] REGISTROS_VALIDOS  = {
            "AL", "AH", "BL", "BH", "CL", "CH", "DL", "DH"
    };
  
    private static final String[] TIPOS_DATO_VALIDOS = {"NUM", "CHAR"};

    public List<Token> analizar(String bloqueEntrada) {
        List<Token> tokens = new ArrayList<>();

        int estado = ESTADO_INICIO;
        StringBuilder bufferPalabra = new StringBuilder();

        int fila = 1;
        int columna = 0;
        int inicioColumnaToken = 0;

        int longitud = bloqueEntrada.length();
        int i = 0;

        while (i < longitud) {
            char c = bloqueEntrada.charAt(i);
            columna++;

            int claseCaracter = clasificarCaracter(c);

            switch (estado) {

                case ESTADO_INICIO:
                    switch (claseCaracter) {
                        case 0:
                            bufferPalabra.setLength(0);
                            bufferPalabra.append(c);
                            inicioColumnaToken = columna;
                            estado = ESTADO_LEYENDO_PALABRA;
                            break;

                        case 1:
                            tokens.add(new Token(" ", TipoToken.ESPACIO, fila, columna));
                            estado = ESTADO_INICIO;
                            break;

                        case 2:
                            tokens.add(new Token(",", TipoToken.COMA, fila, columna));
                            estado = ESTADO_INICIO;
                            break;

                        case 3:
                            fila++;
                            columna = 0;
                            estado = ESTADO_INICIO;
                            break;

                        case 4:
                            estado = ESTADO_INICIO;
                            break;

                        default:
                            tokens.add(new Token(String.valueOf(c), TipoToken.DESCONOCIDO, fila, columna));
                            estado = ESTADO_ERROR;
                            break;
                    }
                    break;

                case ESTADO_LEYENDO_PALABRA:
                    switch (claseCaracter) {
                        case 0:
                            bufferPalabra.append(c);
                            estado = ESTADO_LEYENDO_PALABRA;
                            break;

                        case 1:
                            cerrarPalabra(tokens, bufferPalabra, fila, inicioColumnaToken);
                            tokens.add(new Token(" ", TipoToken.ESPACIO, fila, columna));
                            estado = ESTADO_INICIO;
                            break;

                        case 2:
                            cerrarPalabra(tokens, bufferPalabra, fila, inicioColumnaToken);
                            tokens.add(new Token(",", TipoToken.COMA, fila, columna));
                            estado = ESTADO_INICIO;
                            break;

                        case 3:
                            cerrarPalabra(tokens, bufferPalabra, fila, inicioColumnaToken);
                            fila++;
                            columna = 0;
                            estado = ESTADO_INICIO;
                            break;

                        case 4:
                            cerrarPalabra(tokens, bufferPalabra, fila, inicioColumnaToken);
                            estado = ESTADO_INICIO;
                            break;

                        default:
                            cerrarPalabra(tokens, bufferPalabra, fila, inicioColumnaToken);
                            tokens.add(new Token(String.valueOf(c), TipoToken.DESCONOCIDO, fila, columna));
                            estado = ESTADO_ERROR;
                            break;
                    }
                    break;

                case ESTADO_ERROR:
                    estado = ESTADO_INICIO;
                    i--;
                    columna--;
                    break;
            }

            i++;
        }

        if (estado == ESTADO_LEYENDO_PALABRA && bufferPalabra.length() > 0) {
            cerrarPalabra(tokens, bufferPalabra, fila, inicioColumnaToken);
        }

        return tokens;
    }

    private int clasificarCaracter(char c) {
        switch (c) {
            case ' ':
            case '\t':
                return 1;

            case ',':
                return 2;
            case '\n':
                return 3;

            case '\r':
                return 4;

            default:
                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                    return 0;
                }
                return 5;
        }
    }

    // ======================================================================
    // NUEVO orden de clasificacion de una palabra ya formada:
    //   1) es OPERADOR (ADD, SUB, INC, DEC)
    //   2) es REGISTRO fisico (AL, AH, BL, BH, CL, CH, DL, DH)
    //   3) es palabra reservada de TIPO (NUM, CHAR)               -> DATO
    //   4) cualquier otra palabra formada solo por letras         -> IDENTIFICADOR
    //      (se asume que es el nombre de una variable declarada
    //      por el usuario; el analizador SEMANTICO se encarga de
    //      verificar si en verdad fue declarada y de que tipo es)
    // ======================================================================
    private void cerrarPalabra(List<Token> tokens, StringBuilder buffer, int fila, int columnaInicio) {
        if (buffer.length() == 0) {
            return;
        }
        String palabra = buffer.toString();
        String palabraMayus = aMayusculas(palabra);

        TipoToken tipo;
        if (esOperador(palabraMayus)) {
            tipo = TipoToken.OPERADOR;
        } else if (esRegistro(palabraMayus)) {
            tipo = TipoToken.REGISTRO;
        } else if (esTipoDato(palabraMayus)) {
            tipo = TipoToken.DATO;
        } else {
            tipo = TipoToken.IDENTIFICADOR;
        }

        tokens.add(new Token(palabraMayus, tipo, fila, columnaInicio));
        buffer.setLength(0);
    }

    private String aMayusculas(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') {
                sb.append((char) (c - 32));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean esOperador(String palabra) {
        for (String op : OPERADORES_VALIDOS) {
            if (palabra.equals(op)) {
                return true;
            }
        }
        return false;
    }

    private boolean esRegistro(String palabra) {
        for (String reg : REGISTROS_VALIDOS) {
            if (palabra.equals(reg)) {
                return true;
            }
        }
        return false;
    }

    // NUEVO
    private boolean esTipoDato(String palabra) {
        for (String tipo : TIPOS_DATO_VALIDOS) {
            if (palabra.equals(tipo)) {
                return true;
            }
        }
        return false;
    }

  
    private void mostrarVentanaDiagnostico(List<Token> tokens, List<AnalizadorSemantico8086.ErrorSemantico> semErrores) {

        List<Token> erroresLexicos = new ArrayList<>();
        for (Token t : tokens) {
            if (t.tipo == TipoToken.DESCONOCIDO) {
                erroresLexicos.add(t);
            }
        }

        List<AnalizadorSemantico8086.ErrorSemantico> erroresSemanticos = new ArrayList<>();
        List<AnalizadorSemantico8086.ErrorSemantico> advertenciasSemanticas = new ArrayList<>();
        for (AnalizadorSemantico8086.ErrorSemantico e : semErrores) {
            if (e.esAdvertencia) {
                advertenciasSemanticas.add(e);
            } else {
                erroresSemanticos.add(e);
            }
        }

        if (erroresLexicos.isEmpty() && erroresSemanticos.isEmpty() && advertenciasSemanticas.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "No se encontraron errores lexicos ni semanticos.\n" +
                    "Todos los tokens y todas las instrucciones tienen sentido:\n" +
                    "operadores (ADD, SUB, INC, DEC), registros de 8 bits,\n" +
                    "declaraciones de variables (NUM, CHAR) y comas.",
                    "Analisis exitoso",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        StringBuilder mensaje = new StringBuilder();

        if (!erroresLexicos.isEmpty()) {
            mensaje.append("=== ERRORES LEXICOS (").append(erroresLexicos.size()).append(") ===\n\n");
            for (Token e : erroresLexicos) {
                mensaje.append("- Linea ").append(e.fila)
                       .append(", columna ").append(e.columna)
                       .append(": el texto \"").append(e.lexema)
                       .append("\" no es un token valido.\n")
                       .append("  Sugerencia: ").append(sugerenciaParaError(e.lexema)).append("\n\n");
            }
        }

        if (!erroresSemanticos.isEmpty()) {
            mensaje.append("=== ERRORES SEMANTICOS (").append(erroresSemanticos.size()).append(") ===\n\n");
            for (AnalizadorSemantico8086.ErrorSemantico e : erroresSemanticos) {
                mensaje.append("- Linea ").append(e.linea).append(": ").append(e.mensaje).append("\n\n");
            }
        }

        if (!advertenciasSemanticas.isEmpty()) {
            mensaje.append("=== ADVERTENCIAS (").append(advertenciasSemanticas.size()).append(") ===\n\n");
            for (AnalizadorSemantico8086.ErrorSemantico e : advertenciasSemanticas) {
                mensaje.append("- Linea ").append(e.linea).append(": ").append(e.mensaje).append("\n\n");
            }
        }

        mensaje.append("Tokens/reglas validos:\n")
               .append("  Operadores    -> ADD, SUB, INC, DEC\n")
               .append("  Registros     -> AL, AH, BL, BH, CL, CH, DL, DH\n")
               .append("  Declaraciones -> NUM nombre  |  CHAR nombre\n")
               .append("  ADD/SUB solo admiten operandos NUMERICOS (registros o variables NUM)\n")
               .append("  INC/DEC solo admiten un operando NUMERICO");

        JOptionPane.showMessageDialog(
                null,
                mensaje.toString(),
                (erroresLexicos.isEmpty() && erroresSemanticos.isEmpty()) ? "Analisis con advertencias" : "Se detectaron errores",
                (erroresLexicos.isEmpty() && erroresSemanticos.isEmpty()) ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE
        );
    }

 
    public String sugerenciaParaError(String lexema) {
        if (lexema.length() == 1 && clasificarCaracter(lexema.charAt(0)) == 5) {
            return "el caracter '" + lexema + "' no pertenece al lenguaje. Solo se permiten "
                    + "letras (para operadores, registros, tipos NUM/CHAR y nombres de variables), "
                    + "comas y espacios. Los numeros y simbolos no estan permitidos.";
        }
        return "verifica que el texto corresponda a un operador (ADD, SUB, INC, DEC), a un "
                + "registro de 8 bits (AL, AH, BL, BH, CL, CH, DL, DH), a una palabra de tipo "
                + "(NUM, CHAR) o a un nombre de variable valido (solo letras).";
    }

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorado) {
          
        }

        String bloqueEntrada =
                "NUM contador\n" +
                "CHAR letra\n" +
                "ADD AL, BH\n" +
                "SUB AH, BL\n" +
                "INC AL\n" +
                "DEC BH\n" +
                "ADD BL, CH\n" +
                "SUB CH, DL\n" +
                "INC DL\n" +
                "DEC AL\n" +
                "ADD AH, BL\n" +
                "SUB BL, AL\n" +
                "ADD contador, letra\n";

        AnalizadorLexico8086 analizador = new AnalizadorLexico8086();
        List<Token> tokens = analizador.analizar(bloqueEntrada);


        AnalizadorSemantico8086 semantico = new AnalizadorSemantico8086();
        List<AnalizadorSemantico8086.ErrorSemantico> erroresSemanticos = semantico.analizar(tokens);

        System.out.println("=================================================================");
        System.out.println(" ANALIZADOR LEXICO + SEMANTICO - PROCESADOR 8086");
        System.out.println("=================================================================");
        System.out.println();
        System.out.println("BLOQUE DE ENTRADA:");
        System.out.println("-----------------------------------------------------------------");
        System.out.print(bloqueEntrada);
        System.out.println("-----------------------------------------------------------------");
        System.out.println();
        System.out.println("TOKENS RECONOCIDOS:");
        System.out.println("-----------------------------------------------------------------");

        int contadorOperadores = 0;
        int contadorRegistros = 0;
        int contadorDatos = 0;
        int contadorIdentificadores = 0;
        int contadorComas = 0;
        int contadorDesconocidos = 0;

        for (Token t : tokens) {

            if (t.tipo == TipoToken.ESPACIO) {
                continue;
            }
            System.out.println(t);

            switch (t.tipo) {
                case OPERADOR:
                    contadorOperadores++;
                    break;
                case REGISTRO:
                    contadorRegistros++;
                    break;
                case DATO:
                    contadorDatos++;
                    break;
                case IDENTIFICADOR:
                    contadorIdentificadores++;
                    break;
                case COMA:
                    contadorComas++;
                    break;
                case DESCONOCIDO:
                    contadorDesconocidos++;
                    break;
                default:
                    break;
            }
        }

        System.out.println("-----------------------------------------------------------------");
        System.out.println("RESUMEN LEXICO:");
        System.out.println("  Operadores aritmeticos (mnemonicos) : " + contadorOperadores);
        System.out.println("  Registros de 8 bits                 : " + contadorRegistros);
        System.out.println("  Palabras de tipo (NUM/CHAR)          : " + contadorDatos);
        System.out.println("  Identificadores (variables)          : " + contadorIdentificadores);
        System.out.println("  Comas                                : " + contadorComas);
        System.out.println("  Tokens desconocidos / errores lexicos: " + contadorDesconocidos);
        System.out.println();
        System.out.println("RESUMEN SEMANTICO:");
        long totalErroresSem = erroresSemanticos.stream().filter(e -> !e.esAdvertencia).count();
        long totalAdvertSem = erroresSemanticos.stream().filter(e -> e.esAdvertencia).count();
        System.out.println("  Errores semanticos                   : " + totalErroresSem);
        System.out.println("  Advertencias semanticas               : " + totalAdvertSem);
        for (AnalizadorSemantico8086.ErrorSemantico e : erroresSemanticos) {
            System.out.println("    [" + (e.esAdvertencia ? "ADVERTENCIA" : "ERROR") + "] linea " + e.linea + ": " + e.mensaje);
        }
        System.out.println("=================================================================");

      
        analizador.mostrarVentanaDiagnostico(tokens, erroresSemanticos);
    }
}