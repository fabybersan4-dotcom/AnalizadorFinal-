package analizadorLexico;

// Token.java
public class Token {
	private TipoToken tipo;
	private String lexema;
	private int linea;

	public enum TipoToken {

		START, END, INTEGER, DECIMAL, READ, PRINT,

		LLAVE_IZQ, LLAVE_DER, PAREN_IZQ, PAREN_DER, PUNTO_COMA, COMA, PUNTO, IGUAL, MAS, MENOS, ASTERISCO, DIVISION,

		IDENTIFICADOR, NUMERO_ENTERO, NUMERO_DECIMAL,

		ERROR
	}

	public Token(TipoToken tipo, String lexema, int linea) {
		this.tipo = tipo;
		this.lexema = lexema;
		this.linea = linea;
	}

	public TipoToken getTipo() {
		return tipo;
	}

	public String getLexema() {
		return lexema;
	}

	public int getLinea() {
		return linea;
	}

	public String getTipoString() {
		switch (tipo) {
		case START:
			return "tk.Start";
		case END:
			return "tk.End";
		case INTEGER:
			return "tk.TipoEntero";
		case DECIMAL:
			return "tk.TipoDecimal";
		case READ:
			return "tk.Read";
		case PRINT:
			return "tk.Print";
		case LLAVE_IZQ:
			return "tk.LlaveIzq";
		case LLAVE_DER:
			return "tk.LlaveDer";
		case PAREN_IZQ:
			return "tk.ParenIzq";
		case PAREN_DER:
			return "tk.ParenDer";
		case PUNTO_COMA:
			return "tk.PuntoComa";
		case COMA:
			return "tk.Coma";
		case PUNTO:
			return "tk.Punto";
		case IGUAL:
			return "tk.Igual";
		case MAS:
			return "tk.Suma";
		case MENOS:
			return "tk.Resta";
		case ASTERISCO:
			return "tk.Multiplicacion";
		case DIVISION:
			return "tk.Division";
		case IDENTIFICADOR:
			return "tk.Identificador";
		case NUMERO_ENTERO:
			return "tk.NumeroEntero";
		case NUMERO_DECIMAL:
			return "tk.NumeroDecimal";
		default:
			return "tk.Error";
		}
	}
}