package com.fenix.ordenararquivos.model.firebase;

import com.fenix.ordenararquivos.model.entities.Manga;

import java.util.HashMap;
import java.util.Objects;

public class Caminhos {

    private String capitulo;
    private String pasta;
    private Integer pagina;

    public String getCapitulo() {
        return capitulo;
    }

    public void setCapitulo(String capitulo) {
        this.capitulo = capitulo;
    }

    public String getPasta() {
        return pasta;
    }

    public void setPasta(String pasta) {
        this.pasta = pasta;
    }

    public Integer getPagina() {
        return pagina;
    }

    public void setPagina(Integer pagina) {
        this.pagina = pagina;
    }

    public Caminhos(String capitulo, String pasta, Integer pagina) {
        this.capitulo = capitulo;
        this.pasta = pasta;
        this.pagina = pagina;
    }

    public Caminhos(com.fenix.ordenararquivos.model.entities.Caminhos caminho) {
        this.capitulo = caminho.getCapitulo();
        this.pasta = caminho.getNomePasta();
        this.pagina = caminho.getNumero();
    }

    public static com.fenix.ordenararquivos.model.entities.Caminhos toCominhos(Manga manga, HashMap<String, ?> obj) {
        return new com.fenix.ordenararquivos.model.entities.Caminhos(0, manga, (String) obj.get("capitulo"), ((Long) obj.get("pagina")).intValue(), (String) obj.get("pasta"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Caminhos caminhos = (Caminhos) o;
        return Objects.equals(capitulo, caminhos.capitulo) && Objects.equals(pasta, caminhos.pasta) && Objects.equals(pagina, caminhos.pagina);
    }

    @Override
    public int hashCode() {
        return Objects.hash(capitulo, pasta, pagina);
    }
}
