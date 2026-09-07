package analizadorLexico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AnalizadorSemantico8086 {

	private enum TipoDato {
		NUMERICO, CARACTER, DESCONOCIDO
	}

	public static class ErrorSemantico {
		public int linea;
		public String mensaje;
		public boolean esAdvertencia;

		public ErrorSemantico(int linea, String mensaje, boolean esAdvertencia) {
			this.linea = linea;
			this.mensaje = mensaje;
			this.esAdvertencia = esAdvertencia;
		}
	}

	private final Map<String, TipoDato> tablaSimbolos = new HashMap<>();

	public List<ErrorSemantico> analizar(List<AnalizadorLexico8086.Token> tokens) {
		tablaSimbolos.clear();
		List<ErrorSemantico> errores = new ArrayList<>();

		Map<Integer, List<AnalizadorLexico8086.Token>> porLinea = new TreeMap<>();
		for (AnalizadorLexico8086.Token t : tokens) {
			if (t.tipo == AnalizadorLexico8086.TipoToken.ESPACIO) {
				continue;
			}
			porLinea.computeIfAbsent(t.fila, k -> new ArrayList<>()).add(t);
		}
//intruccion
		for (Map.Entry<Integer, List<AnalizadorLexico8086.Token>> entrada : porLinea.entrySet()) {
			procesarLinea(entrada.getKey(), entrada.getValue(), errores);
		}

		return errores;
	}

	private void procesarLinea(int linea, List<AnalizadorLexico8086.Token> grupo, List<ErrorSemantico> errores) {
		if (grupo.isEmpty()) {
			return;
		}

		for (AnalizadorLexico8086.Token t : grupo) {
			if (t.tipo == AnalizadorLexico8086.TipoToken.DESCONOCIDO) {
				errores.add(new ErrorSemantico(
						linea, "no se puede validar el significado de esta linea porque contiene "
								+ "un caracter invalido (\"" + t.lexema + "\"); corrige primero el error lexico.",
						false));
				return;
			}
		}

		AnalizadorLexico8086.Token primero = grupo.get(0);

		switch (primero.tipo) {
		case DATO:
			validarDeclaracion(linea, grupo, errores);
			break;

		case OPERADOR:
			validarInstruccion(linea, grupo, primero.lexema, errores);
			break;

		default:
			errores.add(new ErrorSemantico(linea,
					"la linea no comienza con un operador (ADD, SUB, INC, DEC) ni con una "
							+ "declaracion de tipo (NUM, CHAR). Los tokens pueden ser validos por "
							+ "separado, pero esa combinacion no tiene sentido como instruccion.",
					false));
			break;
		}
	}

	private void validarDeclaracion(int linea, List<AnalizadorLexico8086.Token> grupo, List<ErrorSemantico> errores) {
		String tipoDeclarado = grupo.get(0).lexema;

		if (grupo.size() != 2) {
			errores.add(new ErrorSemantico(linea, "declaracion mal formada. Se esperaba exactamente: " + tipoDeclarado
					+ " nombre_variable (ejemplo: " + tipoDeclarado + " contador).", false));
			return;
		}

		AnalizadorLexico8086.Token nombre = grupo.get(1);
		if (nombre.tipo != AnalizadorLexico8086.TipoToken.IDENTIFICADOR) {
			errores.add(new ErrorSemantico(linea, "despues de " + tipoDeclarado
					+ " se esperaba el nombre de una variable, no \"" + nombre.lexema + "\".", false));
			return;
		}

		if (tablaSimbolos.containsKey(nombre.lexema)) {
			errores.add(new ErrorSemantico(linea, "la variable \"" + nombre.lexema
					+ "\" ya habia sido declarada anteriormente; " + "no se puede declarar dos veces.", false));
			return;
		}

		TipoDato tipo = "NUM".equals(tipoDeclarado) ? TipoDato.NUMERICO : TipoDato.CARACTER;
		tablaSimbolos.put(nombre.lexema, tipo);
	}

	private void validarInstruccion(int linea, List<AnalizadorLexico8086.Token> grupo, String mnemonico,
			List<ErrorSemantico> errores) {
		switch (mnemonico) {
		case "ADD":
		case "SUB":
			validarDosOperandos(linea, grupo, mnemonico, errores);
			break;

		case "INC":
		case "DEC":
			validarUnOperando(linea, grupo, mnemonico, errores);
			break;

		default:

			break;
		}
	}

	private void validarDosOperandos(int linea, List<AnalizadorLexico8086.Token> grupo, String mnemonico,
			List<ErrorSemantico> errores) {
		if (grupo.size() != 4) {
			errores.add(new ErrorSemantico(linea,
					mnemonico + " requiere exactamente dos operandos separados por una coma " + "(ejemplo: " + mnemonico
							+ " AL, BH). Se encontraron " + (grupo.size() - 1) + " token(s) despues del operador.",
					false));
			return;
		}

		AnalizadorLexico8086.Token op1 = grupo.get(1);
		AnalizadorLexico8086.Token coma = grupo.get(2);
		AnalizadorLexico8086.Token op2 = grupo.get(3);

		boolean formaValida = true;

		if (!esOperandoValido(op1.tipo)) {
			errores.add(new ErrorSemantico(linea,
					mnemonico + " espera un registro o una variable como primer operando, no \"" + op1.lexema + "\".",
					false));
			formaValida = false;
		}
		if (coma.tipo != AnalizadorLexico8086.TipoToken.COMA) {
			errores.add(new ErrorSemantico(linea, mnemonico + " requiere una coma entre los dos operandos.", false));
			formaValida = false;
		}
		if (!esOperandoValido(op2.tipo)) {
			errores.add(new ErrorSemantico(linea,
					mnemonico + " espera un registro o una variable como segundo operando, no \"" + op2.lexema + "\".",
					false));
			formaValida = false;
		}

		if (!formaValida) {
			return;
		}

		TipoDato tipo1 = resolverTipo(linea, op1, errores);
		TipoDato tipo2 = resolverTipo(linea, op2, errores);

		if (tipo1 == TipoDato.CARACTER || tipo2 == TipoDato.CARACTER) {
			errores.add(new ErrorSemantico(linea,
					"no se puede " + (mnemonico.equals("ADD") ? "sumar" : "restar") + " \"" + op1.lexema + "\" ("
							+ nombreTipo(tipo1) + ") con \"" + op2.lexema + "\" (" + nombreTipo(tipo2) + "): "
							+ mnemonico + " solo trabaja con valores numericos, y una variable de "
							+ "tipo CHAR guarda un caracter de texto, no un numero.",
					false));
			return;
		}

		if (tipo1 == TipoDato.NUMERICO && tipo2 == TipoDato.NUMERICO && op1.lexema.equals(op2.lexema)) {
			errores.add(new ErrorSemantico(linea, mnemonico + " " + op1.lexema + ", " + op2.lexema
					+ " usa el mismo operando dos veces: " + "es valido, pero en la practica no tiene un efecto util.",
					true));
		}
	}


	private void validarUnOperando(int linea, List<AnalizadorLexico8086.Token> grupo, String mnemonico,
			List<ErrorSemantico> errores) {
		if (grupo.size() != 2) {
			errores.add(
					new ErrorSemantico(linea,
							mnemonico + " admite un unico operando, sin comas (ejemplo: " + mnemonico + " AL). "
									+ "Se encontraron " + (grupo.size() - 1) + " token(s) despues del operador.",
							false));
			return;
		}

		AnalizadorLexico8086.Token op = grupo.get(1);
		if (!esOperandoValido(op.tipo)) {
			errores.add(new ErrorSemantico(linea,
					mnemonico + " espera un registro o una variable como operando, no \"" + op.lexema + "\".", false));
			return;
		}

		TipoDato tipo = resolverTipo(linea, op, errores);
		if (tipo == TipoDato.CARACTER) {
			errores.add(new ErrorSemantico(linea,
					"no se puede " + (mnemonico.equals("INC") ? "incrementar" : "decrementar") + " \"" + op.lexema
							+ "\": es una variable de tipo CHAR (texto), y " + mnemonico
							+ " solo trabaja con valores numericos.",
					false));
		}
	}

	private boolean esOperandoValido(AnalizadorLexico8086.TipoToken tipo) {
		return tipo == AnalizadorLexico8086.TipoToken.REGISTRO || tipo == AnalizadorLexico8086.TipoToken.IDENTIFICADOR;
	}

	private TipoDato resolverTipo(int linea, AnalizadorLexico8086.Token token, List<ErrorSemantico> errores) {
		if (token.tipo == AnalizadorLexico8086.TipoToken.REGISTRO) {
			return TipoDato.NUMERICO;
		}

		TipoDato tipo = tablaSimbolos.get(token.lexema);
		if (tipo == null) {
			errores.add(new ErrorSemantico(linea,
					"la variable \"" + token.lexema + "\" se usa sin haber sido declarada antes " + "(usa NUM "
							+ token.lexema.toLowerCase() + " o CHAR " + token.lexema.toLowerCase()
							+ " en una linea anterior).",
					false));
			return TipoDato.DESCONOCIDO;
		}
		return tipo;
	}

	private String nombreTipo(TipoDato tipo) {
		switch (tipo) {
		case NUMERICO:
			return "NUMERICO";
		case CARACTER:
			return "CARACTER";
		default:
			return "DESCONOCIDO";
		}
	}
}