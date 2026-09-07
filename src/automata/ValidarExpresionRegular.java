package automata;

public class ValidarExpresionRegular {

    String estado;

    // ======================================================================
    // EJERCICIO 1:  L = (ab)+   (una o más repeticiones de "ab")
    // ======================================================================


    public String definicionFormalEjercicio1() {
        return "A = (Q, Σ, δ, q₀, F)\n"
             + "Q = {Q0, Q1, Q2, Q_trampa}\n"
             + "Σ = {a, b}\n"
             + "δ = tabla de transiciones\n"
             + "q₀ = Q0\n"
             + "F = {Q2}";
    }

    public String validarEjercicio1(String cadena) {

        if (cadena == null || cadena.isEmpty()) {
            return "Cadena inválida";
        }

        cadena = cadena.toLowerCase();

        estado = "Q0";

        for (char letra : cadena.toCharArray()) {

            switch (estado) {

                case "Q0":
                    switch (letra) {
                        case 'a':
                            estado = "Q1";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q1":
                    switch (letra) {
                        case 'b':
                            estado = "Q2";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q2":
                    switch (letra) {
                        case 'a':
                            estado = "Q1";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q_trampa":
                    estado = "Q_trampa";
                    break;
            }
        }

        if (estado == "Q2") {
            return "Cadena válida";
        } else {
            return "Cadena inválida";
        }
    }

    public String[] columnasEjercicio1() {
        return new String[]{"Estado", "a", "b"};
    }

    public String[][] tablaEjercicio1() {
        return new String[][]{
            {"-> Q0", "Q1", "Q_trampa"},
            {"Q1",    "Q_trampa", "* Q2"},
            {"* Q2",  "Q1", "Q_trampa"},
            {"Q_trampa",    "Q_trampa", "Q_trampa"}
        };
    }

    // ======================================================================
    // EJERCICIO 2:  Acepta todas las cadenas que terminan en "ab"
    // ======================================================================

    public String definicionFormalEjercicio2() {
        return "A = (Q, Σ, δ, q₀, F)\n"
             + "Q = {Q0, Q1, Q2}\n"
             + "Σ = {a, b}\n"
             + "δ = tabla de transiciones\n"
             + "q₀ = Q0\n"
             + "F = {Q2}";
    }

    public String validarEjercicio2(String cadena) {

        if (cadena == null || cadena.isEmpty()) {
            return "Cadena inválida";
        }

        cadena = cadena.toLowerCase();

        estado = "Q0";

        for (char letra : cadena.toCharArray()) {

            switch (estado) {

                case "Q0":
                    switch (letra) {
                        case 'a':
                            estado = "Q1";
                            break;
                        case 'b':
                            estado = "Q0";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q1":
                    switch (letra) {
                        case 'a':
                            estado = "Q1";
                            break;
                        case 'b':
                            estado = "Q2";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q2":
                    switch (letra) {
                        case 'a':
                            estado = "Q1";
                            break;
                        case 'b':
                            estado = "Q0";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q_trampa":
                    estado = "Q_trampa";
                    break;
            }
        }

        if (estado == "Q2") {
            return "Cadena válida";
        } else {
            return "Cadena inválida";
        }
    }

    public String[] columnasEjercicio2() {
        return new String[]{"Estado", "a", "b"};
    }

    public String[][] tablaEjercicio2() {
        return new String[][]{
            {"-> Q0", "Q1", "Q0"},
            {"Q1",    "Q1", "* Q2"},
            {"* Q2",  "Q1", "Q0"}
        };
    }

    // ======================================================================
    // EJERCICIO 3:  Cadenas que NO contienen 3 o más "b" consecutivas
    // ======================================================================

    public String definicionFormalEjercicio3() {
        return "A = (Q, Σ, δ, q₀, F)\n"
             + "Q = {Q0, Q1, Q2, Q_trampa}\n"
             + "Σ = {a, b}\n"
             + "δ = tabla de transiciones\n"
             + "q₀ = Q0\n"
             + "F = {Q0, Q1, Q2}";
    }

    public String validarEjercicio3(String cadena) {

        if (cadena == null || cadena.isEmpty()) {
            return "Cadena inválida";
        }

        cadena = cadena.toLowerCase();

        estado = "Q0";

        for (char letra : cadena.toCharArray()) {

            switch (estado) {

                case "Q0":
                    switch (letra) {
                        case 'a':
                            estado = "Q0";
                            break;
                        case 'b':
                            estado = "Q1";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q1":
                    switch (letra) {
                        case 'a':
                            estado = "Q0";
                            break;
                        case 'b':
                            estado = "Q2";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q2":
                    switch (letra) {
                        case 'a':
                            estado = "Q0";
                            break;
                        case 'b':
                            estado = "Q_trampa";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q_trampa":
                    estado = "Q_trampa";
                    break;
            }
        }

        if (estado != "Q_trampa") {
            return "Cadena válida";
        } else {
            return "Cadena inválida";
        }
    }

    public String[] columnasEjercicio3() {
        return new String[]{"Estado", "a", "b"};
    }

    public String[][] tablaEjercicio3() {
        return new String[][]{
            {"-> * Q0", "* Q0", "* Q1"},
            {"* Q1",    "* Q0", "* Q2"},
            {"* Q2",    "* Q0", "Q_trampa"},
            {"Q_trampa",      "Q_trampa",   "Q_trampa"}
        };
    }

    // ======================================================================
    // EJERCICIO 4:  Autómata de paridad (número par de "a" y número par de "b")
    // ======================================================================

    public String definicionFormalEjercicio4() {
        return "A = (Q, Σ, δ, q₀, F)\n"
             + "Q = {Q0, Q1, Q2, Q3}\n"
             + "Σ = {a, b}\n"
             + "δ = tabla de transiciones\n"
             + "q₀ = Q0\n"
             + "F = {Q0}";
    }

    public String validarEjercicio4(String cadena) {

        if (cadena == null || cadena.isEmpty()) {
            return "Cadena inválida";
        }

        cadena = cadena.toLowerCase();

        estado = "Q0";

        for (char letra : cadena.toCharArray()) {

            switch (estado) {

                case "Q0":
                    switch (letra) {
                        case 'a':
                            estado = "Q1";
                            break;
                        case 'b':
                            estado = "Q2";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q1":
                    switch (letra) {
                        case 'a':
                            estado = "Q0";
                            break;
                        case 'b':
                            estado = "Q3";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q2":
                    switch (letra) {
                        case 'a':
                            estado = "Q3";
                            break;
                        case 'b':
                            estado = "Q0";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q3":
                    switch (letra) {
                        case 'a':
                            estado = "Q2";
                            break;
                        case 'b':
                            estado = "Q1";
                            break;
                        default:
                            estado = "Q_trampa";
                    }
                    break;

                case "Q_trampa":
                    estado = "Q_trampa";
                    break;
            }
        }

        if (estado == "Q0") {
            return "Cadena válida";
        } else {
            return "Cadena inválida";
        }
    }

    public String[] columnasEjercicio4() {
        return new String[]{"Estado", "a", "b"};
    }

    public String[][] tablaEjercicio4() {
        return new String[][]{
            {"-> * Q0", "Q1",   "Q2"},
            {"Q1",      "* Q0", "Q3"},
            {"Q2",      "Q3",   "* Q0"},
            {"Q3",      "Q2",   "Q1"}
        };
    }
}