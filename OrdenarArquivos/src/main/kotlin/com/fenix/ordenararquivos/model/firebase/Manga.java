package com.fenix.ordenararquivos.model.firebase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Manga {

    private String nome;
    private String volume;
    private String capitulo;
    private String arquivo;
    private String capitulos;
    private Integer quantidade;
    private List<Caminhos> caminhos;
    private String sincronizacao;

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

    public String getCapitulos() {
        return capitulos;
    }

    public void setCapitulos(String capitulos) {
        this.capitulos = capitulos;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public List<Caminhos> getCaminhos() {
        return caminhos;
    }

    public void setCaminhos(List<Caminhos> caminhos) {
        this.caminhos = caminhos;
    }

    public String getSincronizacao() {
        return sincronizacao;
    }

    public void setSincronizacao(String sincronizacao) {
        this.sincronizacao = sincronizacao;
    }

    public Manga(String nome, String volume, String capitulo, String arquivo, String capitulos, Integer quantidade, List<Caminhos> caminhos, String sincronizacao) {
        this.nome = nome;
        this.volume = volume;
        this.capitulo = capitulo;
        this.arquivo = arquivo;
        this.capitulos = capitulos;
        this.quantidade = quantidade;
        this.caminhos = caminhos;
        this.sincronizacao = sincronizacao;
    }

    public Manga(com.fenix.ordenararquivos.model.Manga manga) {
        this.nome = manga.getNome();
        this.volume = manga.getVolume();
        this.capitulo = manga.getCapitulo();
        this.arquivo = manga.getArquivo();
        this.capitulos = manga.getCapitulos();
        this.quantidade = manga.getQuantidade();
        this.caminhos = manga.getCaminhos().isEmpty() ? new ArrayList<>() : manga.getCaminhos().parallelStream().map(Caminhos::new).collect(Collectors.toList());
    }

    public static com.fenix.ordenararquivos.model.Manga toManga(Long id, HashMap<String, ?> obj) {
        return new com.fenix.ordenararquivos.model.Manga(id, (String) obj.get("nome"), (String) obj.get("volume"), (String) obj.get("capitulo"), (String) obj.get("arquivo"), (String) obj.get("capitulos"), ((Long) obj.get("quantidade")).intValue(), LocalDateTime.now(), new ArrayList<>());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Manga manga = (Manga) o;
        return Objects.equals(nome, manga.nome) && Objects.equals(volume, manga.volume) && Objects.equals(capitulo, manga.capitulo) && Objects.equals(arquivo, manga.arquivo) && Objects.equals(capitulos, manga.capitulos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome, volume, capitulo, arquivo, capitulos);
    }
}
