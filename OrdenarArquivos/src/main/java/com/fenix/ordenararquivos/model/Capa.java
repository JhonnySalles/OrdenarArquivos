package com.fenix.ordenararquivos.model;

public class Capa {

	private String nome;
	private String arquivo;
	private TipoCapa tipo;
	private Boolean isDupla;

	private Capa direita;


	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

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

	public Boolean isDupla() {
		return isDupla;
	}

	public void setDupla(Boolean dupla) {
		this.isDupla = dupla;
	}

	public Capa getDireita() {
		return direita;
	}

	public void setDireita(Capa direita) {
		this.direita = direita;
	}

	public Capa(String nome, String arquivo, TipoCapa tipo, Boolean isDupla) {
		this.nome = nome;
		this.arquivo = arquivo;
		this.tipo = tipo;
		this.isDupla = isDupla;
		this.direita = null;
	}

	public Capa(String nome, String arquivo, TipoCapa tipo, Boolean isDupla, Capa direita) {
		this.nome = nome;
		this.arquivo = arquivo;
		this.tipo = tipo;
		this.isDupla = isDupla;
		this.direita = direita;
	}

	public Capa() {
		this.nome = "";
		this.arquivo = "";
		this.tipo = TipoCapa.CAPA;
		this.isDupla = false;
		this.direita = null;
	}

}
