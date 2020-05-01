package ordenarArquivos.model;

public class Caminhos {

	private String capitulo;
	private String numeroPagina;
	private String nomePasta;

	public String getCapitulo() {
		return capitulo;
	}

	public void setCapitulo(String capitulo) {
		this.capitulo = capitulo;
	}

	public String getNumeroPagina() {
		return numeroPagina;
	}

	public void setNumeroPagina(String numeroPagina) {
		this.numeroPagina = numeroPagina;
	}

	public String getNomePasta() {
		return nomePasta;
	}

	public void setNomePasta(String nomePasta) {
		this.nomePasta = nomePasta;
	}

	public Caminhos() {

	}

	public Caminhos(String capitulo, String numeroPagina, String nomePasta) {
		this.capitulo = capitulo;
		this.numeroPagina = numeroPagina;
		this.nomePasta = nomePasta;
	}

	@Override
	public String toString() {
		return "Caminhos [capitulo=" + capitulo + ", numeroPagina=" + numeroPagina + ", nomePasta=" + nomePasta + "]";
	}

}
