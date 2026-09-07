package analizadorLexico;

import java.util.*;
import java.util.regex.*;

public class LexicalAnalyzer {
	private String codigoFuente;
	private List<Token> tokens;
	private Map<String, Simbolo> tablaSimbolos;
	private List<String> errores;

	// Expresiones regulares
	private static final String REGEX_IDENTIFICADOR = "[a-zA-Z][0-9]{0,3}";
	private static final String REGEX_ENTERO = "[0-9]{1,6}";
	private static final String REGEX_DECIMAL = "[0-9]{1,6}\\.[0-9]{1,3}";

	public LexicalAnalyzer() {
		tokens = new ArrayList<>();
		tablaSimbolos = new LinkedHashMap<>();
		errores = new ArrayList<>();
	}

	public void analizar(String codigo) {
		this.codigoFuente = codigo;
		tokens.clear();
		tablaSimbolos.clear();
		errores.clear();

		String[] lineas = codigo.split("\n");

		for (int numLinea = 0; numLinea < lineas.length; numLinea++) {
			analizarLinea(lineas[numLinea], numLinea + 1);
		}
	}

	private void analizarLinea(String linea, int numLinea) {
		int i = 0;
		while (i < linea.length()) {
			char c = linea.charAt(i);

			if (Character.isWhitespace(c)) {
				i++;
				continue;
			}

			String lexema = "";

			// Verificar palabras reservadas e identificadores
			if (Character.isLetter(c)) {
				int inicio = i;
				while (i < linea.length() && (Character.isLetterOrDigit(linea.charAt(i)))) {
					i++;
				}
				lexema = linea.substring(inicio, i);

				Token.TipoToken tipo = obtenerTipoPalabraReservada(lexema);

				if (tipo != null) {
					tokens.add(new Token(tipo, lexema, numLinea));
				} else {
					// Validar identificador
					if (validarIdentificador(lexema)) {
						tokens.add(new Token(Token.TipoToken.IDENTIFICADOR, lexema, numLinea));
						if (!tablaSimbolos.containsKey(lexema)) {
							tablaSimbolos.put(lexema, new Simbolo(lexema));
						}
					} else {
						errores.add("Error léxico en línea " + numLinea + ": Identificador inválido '" + lexema + "'");
						tokens.add(new Token(Token.TipoToken.ERROR, lexema, numLinea));
					}
				}
				continue;
			}

			// Números
			if (Character.isDigit(c)) {
				int inicio = i;
				boolean esDecimal = false;

				while (i < linea.length() && (Character.isDigit(linea.charAt(i)) || linea.charAt(i) == '.')) {
					if (linea.charAt(i) == '.') {
						if (esDecimal)
							break; // Dos puntos decimales
						esDecimal = true;
					}
					i++;
				}

				lexema = linea.substring(inicio, i);

				if (esDecimal) {
					if (validarDecimal(lexema)) {
						tokens.add(new Token(Token.TipoToken.NUMERO_DECIMAL, lexema, numLinea));
					} else {
						errores.add("Error léxico en línea " + numLinea + ": Número decimal inválido '" + lexema + "'");
						tokens.add(new Token(Token.TipoToken.ERROR, lexema, numLinea));
					}
				} else {
					if (validarEntero(lexema)) {
						tokens.add(new Token(Token.TipoToken.NUMERO_ENTERO, lexema, numLinea));
					} else {
						errores.add("Error léxico en línea " + numLinea + ": Número entero inválido '" + lexema + "'");
						tokens.add(new Token(Token.TipoToken.ERROR, lexema, numLinea));
					}
				}
				continue;
			}

			// Símbolos
			Token.TipoToken tipoSimbolo = obtenerTipoSimbolo(c);
			if (tipoSimbolo != null) {
				tokens.add(new Token(tipoSimbolo, String.valueOf(c), numLinea));
				i++;
				continue;
			}

			// Carácter no reconocido
			errores.add("Error léxico en línea " + numLinea + ": Carácter no reconocido '" + c + "'");
			tokens.add(new Token(Token.TipoToken.ERROR, String.valueOf(c), numLinea));
			i++;
		}
	}

	private Token.TipoToken obtenerTipoPalabraReservada(String palabra) {
		switch (palabra.toUpperCase()) {
		case "START":
			return Token.TipoToken.START;
		case "END":
			return Token.TipoToken.END;
		case "INTEGER":
			return Token.TipoToken.INTEGER;
		case "DECIMAL":
			return Token.TipoToken.DECIMAL;
		case "READ":
			return Token.TipoToken.READ;
		case "PRINT":
			return Token.TipoToken.PRINT;
		default:
			return null;
		}
	}

	private Token.TipoToken obtenerTipoSimbolo(char c) {
		switch (c) {
		case '{':
			return Token.TipoToken.LLAVE_IZQ;
		case '}':
			return Token.TipoToken.LLAVE_DER;
		case '(':
			return Token.TipoToken.PAREN_IZQ;
		case ')':
			return Token.TipoToken.PAREN_DER;
		case ';':
			return Token.TipoToken.PUNTO_COMA;
		case ',':
			return Token.TipoToken.COMA;
		case '.':
			return Token.TipoToken.PUNTO;
		case '=':
			return Token.TipoToken.IGUAL;
		case '+':
			return Token.TipoToken.MAS;
		case '-':
			return Token.TipoToken.MENOS;
		case '*':
			return Token.TipoToken.ASTERISCO;
		case '/':
			return Token.TipoToken.DIVISION;
		default:
			return null;
		}
	}

	private boolean validarIdentificador(String lexema) {
		return Pattern.matches(REGEX_IDENTIFICADOR, lexema);
	}

	private boolean validarEntero(String lexema) {
		return Pattern.matches(REGEX_ENTERO, lexema);
	}

	private boolean validarDecimal(String lexema) {
		return Pattern.matches(REGEX_DECIMAL, lexema);
	}

	public List<Token> getTokens() {
		return tokens;
	}

	public Map<String, Simbolo> getTablaSimbolos() {
		return tablaSimbolos;
	}

	public List<String> getErrores() {
		return errores;
	}
}