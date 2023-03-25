package ordenarArquivos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Manga {

	private Long id;
	private String nome;
	private String volume;
	private String capitulo;
	private String arquivo;
	private String capitulos;
	private Integer quantidade;
	private LocalDateTime atualizacao = LocalDateTime.now();
	private ArrayList<Caminhos> caminhos = new ArrayList<Caminhos>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getVolume() {
		return volume;
	}

	public void setVolume(String volume) {
		this.volume = volume;
	}

	public String getCapitulo() {
		return capitulo;
	}

	public void setCapitulo(String capitulo) {
		this.capitulo = capitulo;
	}

	public String getArquivo() {
		return arquivo;
	}

	public void setArquivo(String arquivo) {
		this.arquivo = arquivo;
	}

	public Integer getQuantidade() {
		return quantidade;
	}

	public void setQuantidade(Integer quantidade) {
		this.quantidade = quantidade;
	}

	public String getCapitulos() {
		return capitulos;
	}

	public void setCapitulos(String capitulos) {
		this.capitulos = capitulos;
	}

	public ArrayList<Caminhos> getCaminhos() {
		return caminhos;
	}

	public void setCaminhos(ArrayList<Caminhos> caminhos) {
		this.caminhos = caminhos;
	}

	public void addCaminhos(Caminhos caminhos) {
		this.caminhos.add(caminhos);
	}

	public LocalDateTime getAtualizacao() {
		return atualizacao;
	}

	public void setAtualizacao(LocalDateTime atualizacao) {
		this.atualizacao = atualizacao;
	}

	public Manga(Long id, String nome, String volume, String capitulo, String arquivo, Integer quantidade,
			String capitulos) {
		this.id = id;
		this.nome = nome;
		this.volume = volume;
		this.capitulo = capitulo;
		this.arquivo = arquivo;
		this.quantidade = quantidade;
		this.capitulos = capitulos;
	}

	public Manga(Long id, String nome, String volume, String capitulo, String arquivo, Integer quantidade,
			String capitulos, LocalDateTime atualizacao) {
		this.id = id;
		this.nome = nome;
		this.volume = volume;
		this.capitulo = capitulo;
		this.arquivo = arquivo;
		this.atualizacao = atualizacao;
		this.quantidade = quantidade;
		this.capitulos = capitulos;
	}

	@Override
	public String toString() {
		return "Manga [id=" + id + ", nome=" + nome + ", volume=" + volume + ", capitulo=" + capitulo + ", arquivo="
				+ arquivo + "]";
	}

}
