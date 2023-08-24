package com.fenix.ordenararquivos.model;

public class Capa {

	private String arquivo;
	private TipoCapa tipo;
	private Boolean isDupla;

	private Capa esquerda;

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

	public Capa getEsquerda() {
		return esquerda;
	}

	public void setEsquerda(Capa esquerda) {
		this.esquerda = esquerda;
	}

	public Capa(String arquivo, TipoCapa tipo, Boolean isDupla) {
		this.arquivo = arquivo;
		this.tipo = tipo;
		this.isDupla = isDupla;
		this.esquerda = null;
	}

	public Capa() {
		this.arquivo = "";
		this.tipo = TipoCapa.CAPA;
		this.isDupla = false;
		this.esquerda = null;
	}

}
