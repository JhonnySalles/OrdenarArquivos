package ordenarArquivos.model;

public class Capa {

	private String arquivo;
	private TipoCapa tipo;

	public String getArquivo() {
		return arquivo;
	}

	public void setArquivo(String arquivo) {
		this.arquivo = arquivo;
	}

	public TipoCapa getTipo() {
		return tipo;
	}

	public void setTipo(TipoCapa tipo) {
		this.tipo = tipo;
	}

	public Capa(String arquivo, TipoCapa tipo) {
		this.arquivo = arquivo;
		this.tipo = tipo;
	}

	public Capa() {
		this.arquivo = "";
		this.tipo = TipoCapa.CAPA;
	}

}
