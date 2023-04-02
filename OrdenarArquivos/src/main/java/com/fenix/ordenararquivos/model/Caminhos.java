package com.fenix.ordenararquivos.model;

public class Caminhos {

	private Long id;
	private Manga manga;
	private String capitulo;
	private Integer numero;
	private String numeroPagina;
	private String nomePasta;
	
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Manga getManga() {
		return manga;
	}

	public void setManga(Manga manga) {
		this.manga = manga;
	}

	public String getCapitulo() {
		return capitulo;
	}

	public void setCapitulo(String capitulo) {
		this.capitulo = capitulo;
	}

	public Integer getNumero() {
		return numero;
	}
	
	public String getNumeroPagina() {
		return numeroPagina;
	}

	public void setNumero(Integer numero) {
		this.numero = numero;
		this.numeroPagina = numero.toString();
	}
	
	public void setNumero(String numeroPagina) {
		this.numero = numeroPagina == null || numeroPagina.isEmpty() ? 0 : Integer.valueOf(numeroPagina);
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

	public Caminhos(String capitulo, Integer numero, String nomePasta) {
		this.capitulo = capitulo;
		this.numero = numero;
		this.numeroPagina = numero.toString();
		this.nomePasta = nomePasta;
	}
	
	public Caminhos(String capitulo, String numero, String nomePasta) {
		this.capitulo = capitulo;
		this.numero = numero == null || numero.isEmpty() ? 0 : Integer.valueOf(numero);
		this.numeroPagina = numero;
		this.nomePasta = nomePasta;
	}

	public Caminhos(Long id, Manga manga, String capitulo, Integer numero, String nomePasta) {
		this.id = id;
		this.manga = manga;
		this.capitulo = capitulo;
		this.numero = numero;
		this.numeroPagina = numero.toString();
		this.nomePasta = nomePasta;
	}

	@Override
	public String toString() {
		return "Caminhos [capitulo=" + capitulo + ", numero=" + numero + ", nomePasta=" + nomePasta + "]";
	}

}
