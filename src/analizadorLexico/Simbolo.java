package analizadorLexico;

public class Simbolo {
	private String id;
	private String tipo;
	private String valor;

	public Simbolo(String id) {
		this.id = id;
		this.tipo = "";
		this.valor = "";
	}

	public String getId() {
		return id;
	}

	public String getTipo() {
		return tipo;
	}

	public String getValor() {
		return valor;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}
}