package expresiones;

import java.util.Scanner;
import java.util.regex.Pattern;

public class ValidadorRegex {

    // Expresion regular: ((a*b) U c) a*
    private static final String REGEX = "((a*b)|c)a*";
    private static final Pattern PATRON = Pattern.compile(REGEX);

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Expresion regular: ((a*b) U c) a*");
        System.out.println("Ingresa cadenas para validar (escribe 'salir' para terminar)");
        System.out.println("------------------------------------------------------");

        while (true) {
            System.out.print("Cadena: ");
            String entrada = scanner.nextLine();

            if (entrada.equalsIgnoreCase("salir")) {
                break;
            }

            if (esValida(entrada)) {
                System.out.println(" -> VALIDA \n");
            } else {
                System.out.println(" -> NO VALIDA \n");
            }
        }

        scanner.close();
        System.out.println("Programa finalizado.");
    }

    public static boolean esValida(String cadena) {
        return PATRON.matcher(cadena).matches();
    }
}