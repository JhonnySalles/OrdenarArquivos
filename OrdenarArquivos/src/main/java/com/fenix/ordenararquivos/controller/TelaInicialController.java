package com.fenix.ordenararquivos.controller;

import com.fenix.ordenararquivos.configuration.Configuracao;
import com.fenix.ordenararquivos.model.Caminhos;
import com.fenix.ordenararquivos.model.Capa;
import com.fenix.ordenararquivos.model.Manga;
import com.fenix.ordenararquivos.model.TipoCapa;
import com.fenix.ordenararquivos.service.MangaServices;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class TelaInicialController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(TelaInicialController.class);

    private static String WINRAR;

    private static final String IMAGE_PATTERN = "(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|PNG|JPG|GIF|BMP|JPEG)$";

    @FXML
    private AnchorPane apGlobal;

    @FXML
    private JFXButton btnLimparTudo;

    @FXML
    private JFXButton btnProcessar;

    @FXML
    private JFXButton btnCompactar;

    @FXML
    private JFXButton btnGerarCapa;

    @FXML
    private JFXTextField txtSimularPasta;

    @FXML
    private JFXTextField txtPastaOrigem;

    @FXML
    private JFXButton btnPesquisarPastaOrigem;

    @FXML
    private JFXTextField txtPastaDestino;

    @FXML
    private JFXButton btnPesquisarPastaDestino;

    @FXML
    private JFXTextField txtNomePastaManga;

    @FXML
    private JFXTextField txtVolume;

    @FXML
    private JFXButton btnVolumeMenos;

    @FXML
    private JFXButton btnVolumeMais;

    @FXML
    private JFXTextField txtNomeArquivo;

    @FXML
    private JFXTextField txtNomePastaCapitulo;

    @FXML
    private JFXCheckBox cbVerificaPaginaDupla;

    @FXML
    private JFXCheckBox cbCompactarArquivo;

    @FXML
    private JFXCheckBox cbMesclarCapaTudo;

    @FXML
    private JFXCheckBox cbAjustarMargemCapa;

    @FXML
    private JFXListView<String> lsVwListaImagens;

    @FXML
    private JFXTextField txtGerarInicio;

    @FXML
    private JFXTextField txtGerarFim;

    @FXML
    private JFXTextField txtSeparador;

    @FXML
    private JFXTextArea txtAreaImportar;

    @FXML
    private JFXButton btnLimpar;

    @FXML
    private JFXButton btnImportar;

    @FXML
    private JFXTextField txtQuantidade;

    @FXML
    private JFXButton btnSubtrair;

    @FXML
    private JFXButton btnSomar;

    @FXML
    private TableView<Caminhos> tbViewTabela;

    @FXML
    private TableColumn<Caminhos, String> clCapitulo;

    @FXML
    private TableColumn<Caminhos, String> clNumeroPagina;

    @FXML
    private TableColumn<Caminhos, String> clNomePasta;

    @FXML
    private Label lblAviso;

    @FXML
    private Label lblAlerta;

    @FXML
    private Label lblProgresso;

    @FXML
    private ProgressBar pbProgresso;

    @FXML
    private JFXButton btnScrollSubir;

    @FXML
    private JFXButton btnScrollDescer;

    @FXML
    private ImageView imgTudo;

    @FXML
    private ImageView imgFrente;

    @FXML
    private ImageView imgTras;

    private ObservableList<Caminhos> obsLCaminhos;
    private ObservableList<String> obsLListaItens;
    private List<Caminhos> lista;
    private File caminhoOrigem;
    private File caminhoDestino;
    private String selecionada;
    private ObservableList<Capa> obsLImagesSelected;

    private MangaServices service = new MangaServices();

    private void limpaCampos() {
        limparCapas();

        lista = new ArrayList<>();
        obsLCaminhos = FXCollections.observableArrayList(lista);
        tbViewTabela.setItems(obsLCaminhos);

        caminhoOrigem = null;
        caminhoDestino = null;

        lblAlerta.setText("");
        lblAviso.setText("");

        manga = null;

        txtSimularPasta.setText("");
        txtPastaOrigem.setText("");
        txtPastaDestino.setText("");
        txtNomePastaManga.setText("[JPN] Manga -");
        txtVolume.setText("Volume 01");
        txtNomePastaCapitulo.setText("Capítulo");
        txtSeparador.setText("-");
        onBtnLimpar();

        obsLListaItens = FXCollections.<String>observableArrayList("");
        lsVwListaImagens.setItems(obsLListaItens);
        selecionada = null;

        lblProgresso.setText("");
        pbProgresso.setProgress(0);
    }

    private FilenameFilter getFilterNameFile() {
        return (dir, name) -> {
            if (name.lastIndexOf('.') > 0) {
                Pattern p = Pattern.compile(IMAGE_PATTERN);

                if (p.matcher(name).matches())
                    return true;
                else
                    return false;
            }
            return false;
        };
    }

    @FXML
    private void onBtnScrollSubir() {
        if (!lsVwListaImagens.getItems().isEmpty())
            lsVwListaImagens.scrollTo(0);
    }

    @FXML
    private void onBtnScrollBaixo() {
        if (!lsVwListaImagens.getItems().isEmpty())
            lsVwListaImagens.scrollTo(lsVwListaImagens.getItems().size());
    }

    @FXML
    private void onBtnLimparTudo() {
        limpaCampos();
    }

    private static List<File> LAST_PROCESS_FOLDERS = new ArrayList<File>();

    @FXML
    private void onBtnCompactar() {
        if (caminhoDestino.exists() && !txtNomeArquivo.getText().isEmpty() && !LAST_PROCESS_FOLDERS.isEmpty())
            compactaArquivo(new File(caminhoDestino.getPath().trim() + "\\" + txtNomeArquivo.getText().trim()),
                    LAST_PROCESS_FOLDERS);
    }

    @FXML
    private void onBtnGerarCapa() {

    }

    private static final String NUMBER_PATTERN = "[\\d.]+$";

    @FXML
    private void onBtnVolumeMenos() {
        // Matches retorna se toda a string for o patern, no caso utiliza-se o inicio
        // para mostrar que tenha em toda a string.
        if (txtVolume.getText().matches(".*" + NUMBER_PATTERN)) {
            String texto = txtVolume.getText().trim();
            String volume = texto.replaceAll(texto.replaceAll(NUMBER_PATTERN, ""), "").trim();
            Integer padding = volume.length();

            try {
                Integer number = Integer.valueOf(volume);
                texto = texto.substring(0, texto.lastIndexOf(volume));
                number--;
                volume = texto + String.format("%0" + padding + "d", number);
                txtVolume.setText(volume);
                simulaNome();
                carregaManga();
            } catch (NumberFormatException e) {
                try {
                    Double number = Double.valueOf(volume);
                    texto = texto.substring(0, texto.lastIndexOf(volume));
                    number--;
                    volume = texto + String.format("%0" + padding + ".1f", number).replaceAll("\\.", "")
                            .replaceAll("\\,", ".");
                    txtVolume.setText(volume);
                    simulaNome();
                    carregaManga();
                } catch (NumberFormatException e1) {
                    LOG.info("Erro ao incrementar valor.", e);
                }
            }
        }
    }

    @FXML
    private void onBtnVolumeMais() {
        if (txtVolume.getText().matches(".*" + NUMBER_PATTERN)) {
            String texto = txtVolume.getText().trim();
            String volume = texto.replaceAll(texto.replaceAll(NUMBER_PATTERN, ""), "").trim();
            Integer padding = volume.length();

            try {
                Integer number = Integer.valueOf(volume);
                texto = texto.substring(0, texto.lastIndexOf(volume));
                number++;
                volume = texto + String.format("%0" + padding + "d", number);
                txtVolume.setText(volume);
                simulaNome();
                carregaManga();
            } catch (NumberFormatException e) {
                try {
                    Double number = Double.valueOf(volume);
                    texto = texto.substring(0, texto.lastIndexOf(volume));
                    number++;
                    volume = texto + String.format("%0" + padding + ".1f", number).replaceAll("\\.", "")
                            .replaceAll("\\,", ".");
                    txtVolume.setText(volume);
                    simulaNome();
                    carregaManga();
                } catch (NumberFormatException e1) {
                    LOG.info("Erro ao incrementar valor.", e);
                }
            }
        }
    }

    private void desabilita() {
        btnLimparTudo.setDisable(true);
        btnCompactar.setDisable(true);

        txtPastaOrigem.setDisable(true);
        btnPesquisarPastaOrigem.setDisable(true);
        txtPastaDestino.setDisable(true);
        btnPesquisarPastaDestino.setDisable(true);
        txtNomePastaManga.setDisable(true);
        txtVolume.setDisable(true);

        btnImportar.setDisable(true);
        btnLimpar.setDisable(true);
        btnImportar.setDisable(true);
        tbViewTabela.setDisable(true);
    }

    private void habilita() {
        btnLimparTudo.setDisable(false);
        btnCompactar.setDisable(false);

        txtPastaOrigem.setDisable(false);
        btnPesquisarPastaOrigem.setDisable(false);
        txtPastaDestino.setDisable(false);
        btnPesquisarPastaDestino.setDisable(false);
        txtNomePastaManga.setDisable(false);
        txtVolume.setDisable(false);

        btnImportar.setDisable(false);
        btnLimpar.setDisable(false);
        btnImportar.setDisable(false);
        tbViewTabela.setDisable(false);

        btnProcessar.accessibleTextProperty().set("PROCESSA");
        btnProcessar.setText("Processar");
        apGlobal.cursorProperty().set(null);
    }

    private Boolean validaCampos() {
        Boolean valida = true;

        if ((caminhoOrigem == null) || (!caminhoOrigem.exists())) {
            txtSimularPasta.setText("Origem não informado.");
            txtPastaOrigem.setUnFocusColor(Color.RED);
            valida = false;
        }

        if ((caminhoDestino == null) || (!caminhoDestino.exists())) {
            txtSimularPasta.setText("Destino não informado.");
            txtPastaDestino.setUnFocusColor(Color.RED);
            valida = false;
        }

        if (lsVwListaImagens.getSelectionModel().getSelectedItem() == null)
            lsVwListaImagens.getSelectionModel().select(0);

        if (obsLCaminhos.isEmpty())
            valida = false;

        if ((cbCompactarArquivo.isSelected()) && (txtNomeArquivo.getText().isEmpty())) {
            txtSimularPasta.setText("Não informado nome do arquivo.");
            txtNomeArquivo.setUnFocusColor(Color.RED);
            valida = false;
        }

        if ((cbCompactarArquivo.isSelected()) && (WINRAR == null || WINRAR.isEmpty())) {
            txtSimularPasta.setText("Winrar não configurado.");
            valida = false;
        }

        return valida;
    }

    private Manga manga = null;

    private Manga geraManga(Long id) {
        String nome = txtNomePastaManga.getText();
        if (nome.contains("]"))
            nome = nome.substring(nome.indexOf("]")).replace("]", "").trim();

        if (nome.substring(nome.length() - 1).equalsIgnoreCase("-"))
            nome = nome.substring(0, nome.length() - 1).trim();

        Integer quantidade = obsLListaItens == null ? 0 : obsLListaItens.size();

        return new Manga(id, nome, txtVolume.getText(), txtNomePastaCapitulo.getText().trim(),
                txtNomeArquivo.getText().trim(), quantidade, txtAreaImportar.getText(), LocalDateTime.now());
    }

    private void carregaManga() {
        manga = service.find(geraManga(null));

        if (manga != null) {
            txtNomePastaManga.setText("[JPN] " + manga.getNome() + " - ");
            txtVolume.setText(manga.getVolume());
            txtNomePastaCapitulo.setText(manga.getCapitulo());
            txtNomeArquivo.setText(manga.getArquivo());
            txtAreaImportar.setText(manga.getCapitulos());

            Integer quantidade = obsLListaItens == null ? 0 : obsLListaItens.size();
            if (manga.getQuantidade().compareTo(quantidade) != 0)
                lblAlerta.setText("Difereça na quantidade de imagens.");
            else
                lblAlerta.setText("");

            lista = new ArrayList<>(manga.getCaminhos());
            obsLCaminhos = FXCollections.observableArrayList(lista);
            tbViewTabela.setItems(obsLCaminhos);

            lblAviso.setText("Manga localizado.");
        } else
            lblAviso.setText("Manga não localizado.");
    }

    private void salvaManga() {
        if (manga == null)
            manga = geraManga(null);
        else
            manga = geraManga(manga.getId());

        manga.getCaminhos().clear();

        for (Caminhos caminho : lista)
            manga.addCaminhos(caminho);

        service.save(manga);

        Platform.runLater(() -> {
            lblAlerta.setText("");
            lblAviso.setText("Manga salvo.");
        });
    }

    private File criaPasta(String caminho) {
        File arquivo = new File(caminho);
        if (!arquivo.exists())
            arquivo.mkdir();

        return arquivo;
    }

    private Path copiaItem(File arquivo, File destino) throws IOException {
        return copiaItem(arquivo, destino, arquivo.getName());
    }

    private Path copiaItem(File arquivo, File destino, String nome) throws IOException {
        Path arquivoDestino = Paths.get(destino.toPath() + "/" + nome);
        Files.copy(arquivo.toPath(), arquivoDestino, StandardCopyOption.COPY_ATTRIBUTES,
                StandardCopyOption.REPLACE_EXISTING);
        return arquivoDestino;
    }

    private File renomeiaItem(Path arquivo, String nome) throws IOException {
        return Files.move(arquivo, arquivo.resolveSibling(nome), StandardCopyOption.REPLACE_EXISTING).toFile();
    }

    private void deletaItem(String item) {
        File arquivo = new File(item);
        if (arquivo.exists())
            arquivo.delete();
    }

    private Boolean verificaPaginaDupla(File arquivo) {
        boolean result = false;
        try {
            BufferedImage img = null;
            img = ImageIO.read(arquivo);

            result = (img.getWidth() / img.getHeight()) > 0.9;
        } catch (IOException e) {
            LOG.error("Erro ao verificar a página dupla.", e);
        }
        return result;
    }

    final File PASTA_TEMPORARIA = new File(System.getProperty("user.dir"), "temp/");

    private void limparCapas() {
        imgTudo.setImage(null);
        imgFrente.setImage(null);
        imgTras.setImage(null);
        obsLImagesSelected.clear();
        if (!PASTA_TEMPORARIA.exists())
            PASTA_TEMPORARIA.mkdir();
        else {
            for (File item : PASTA_TEMPORARIA.listFiles())
                item.delete();
        }
    }

    private void simularCapa(TipoCapa tipo, Image imagem) {
        switch (tipo) {
            case CAPA -> imgFrente.setImage(imagem);
            case TRAS -> imgTras.setImage(imagem);
            case CAPA_COMPLETA -> imgTudo.setImage(imagem);
        }
    }

    private void remCapa(String arquivo) {
        var capa = obsLImagesSelected.stream().filter(it -> it.getNome().equalsIgnoreCase(arquivo)).findFirst();

        if (capa.isPresent()) {
            obsLImagesSelected.remove(capa.get());

            if (capa.get().getTipo().compareTo(TipoCapa.CAPA_COMPLETA) == 0) {
                final Optional<Capa> frente = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.CAPA_COMPLETA) == 0).findFirst();
                if (frente.isPresent())
                    CompletableFuture.runAsync(() -> {
                        simularCapa(capa.get().getTipo(), carregaImagem(new File(txtPastaOrigem.getText() + "\\" + frente.get().getArquivo())));
                        simularCapa(TipoCapa.TRAS, null);
                    });
                else
                    simularCapa(TipoCapa.CAPA_COMPLETA, null);
            } else
                simularCapa(capa.get().getTipo(), null);
        }
    }

    private void addCapa(TipoCapa tipo, String arquivo) {
        File img = new File(txtPastaOrigem.getText() + "\\" + arquivo);
        Boolean isDupla = isPaginaDupla(img);
        if (tipo == TipoCapa.CAPA_COMPLETA) {
            var capas = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(tipo) == 0 && it.getDireita() != null).findFirst();
            if (capas.isEmpty())
                capas = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(tipo) == 0).findFirst();

            final Capa frente = capas.isPresent() ? capas.get() : null;
            final Capa tras = capas.isPresent() ? capas.get().getDireita() : null;

            if (isDupla) {

                String nome = img.getName().substring(0, img.getName().lastIndexOf("."));
                String ext = img.getName().substring(img.getName().lastIndexOf("."));

                File direita = new File(PASTA_TEMPORARIA + "\\" + nome + TRAS + ext);
                File esquerda = new File(PASTA_TEMPORARIA + "\\" + nome + FRENTE + ext);

                obsLImagesSelected.removeIf(it -> it.getTipo().compareTo(TipoCapa.CAPA_COMPLETA) == 0);
                obsLImagesSelected.add(new Capa(arquivo, esquerda.getName(), tipo, isDupla));
                obsLImagesSelected.removeIf(it -> it.getTipo().compareTo(TipoCapa.TRAS) == 0);
                obsLImagesSelected.add(new Capa(arquivo, direita.getName(), TipoCapa.TRAS, false));

                CompletableFuture.runAsync(() -> {
                    try {
                        copiaItem(img, PASTA_TEMPORARIA);
                        divideImagens(img, esquerda, direita);
                        simularCapa(tipo, carregaImagem(new File(PASTA_TEMPORARIA + "\\" + img.getName())));
                        simularCapa(TipoCapa.TRAS, new Image(direita.getAbsolutePath()));
                    } catch (IOException e) {
                        LOG.info("Erro ao processar imagem: Capa completa, pagina dupla.", e);
                    }
                });
            } else if (frente == null) {
                remCapa(arquivo);
                obsLImagesSelected.add(new Capa(arquivo, img.getName(), tipo, isDupla));
                CompletableFuture.runAsync(() -> {
                    try {
                        copiaItem(img, PASTA_TEMPORARIA);
                        simularCapa(tipo, carregaImagem(new File(PASTA_TEMPORARIA + "\\" + img.getName())));
                    } catch (IOException e) {
                        LOG.info("Erro ao processar imagem: Capa completa frente.", e);
                    }
                });
            } else if (tras == null) {
                frente.setDireita(new Capa(arquivo, img.getName(), tipo, isDupla));
                obsLImagesSelected.add(frente.getDireita());
                obsLImagesSelected.removeIf(it -> it.getTipo().compareTo(TipoCapa.TRAS) == 0);
                obsLImagesSelected.add(new Capa(arquivo, img.getName(), TipoCapa.TRAS, false));
                CompletableFuture.runAsync(() -> {
                    try {
                        copiaItem(img, PASTA_TEMPORARIA);
                        File imagem = new File(PASTA_TEMPORARIA + "\\" + img.getName());
                        simularCapa(tipo, carregaImagem(new File(PASTA_TEMPORARIA + "\\" + frente.getArquivo()), imagem));
                        simularCapa(TipoCapa.TRAS, new Image(imagem.getAbsolutePath()));
                    } catch (IOException e) {
                        LOG.info("Erro ao processar imagem: Capa completa trazeira.", e);
                    }
                });
            }
        } else {
            var capa = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(tipo) == 0).findFirst().orElse(new Capa());

            capa.setTipo(tipo);
            capa.setNome(arquivo);
            capa.setArquivo(img.getName());
            capa.setDupla(isDupla);

            obsLImagesSelected.remove(capa);
            obsLImagesSelected.add(capa);
            CompletableFuture.runAsync(() -> {
                try {
                    copiaItem(img, PASTA_TEMPORARIA);
                    simularCapa(tipo, carregaImagem(new File(PASTA_TEMPORARIA + "\\" + img.getName())));
                } catch (IOException e) {
                    LOG.info("Erro ao processar imagem: Capa Tipo " + tipo + ".", e);
                }
            });
        }
    }

    private void reloadCapa() {
        if (obsLImagesSelected.isEmpty())
            return;

        CompletableFuture.runAsync(() -> {
            for (Capa capa : obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.CAPA_COMPLETA) != 0).toList()) {
                try {
                    copiaItem(new File(txtPastaOrigem.getText() + "\\" + capa.getNome()), PASTA_TEMPORARIA);
                    simularCapa(capa.getTipo(), carregaImagem(new File(PASTA_TEMPORARIA + "\\" + capa.getArquivo())));
                } catch (IOException e) {
                    LOG.info("Erro ao reprocessar imagem: " + capa.getTipo() + ".", e);
                }
            }

            Optional<Capa> completa = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.getDireita() != null).findFirst();

            if (completa.isEmpty())
                completa = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.CAPA_COMPLETA) == 0).findFirst();

            if (completa.isPresent())
                if (completa.get().getDireita() != null) {
                    try {
                        copiaItem(new File(txtPastaOrigem.getText() + "\\" + completa.get().getNome()), PASTA_TEMPORARIA);
                        copiaItem(new File(txtPastaOrigem.getText() + "\\" + completa.get().getDireita().getNome()), PASTA_TEMPORARIA);
                        simularCapa(TipoCapa.CAPA_COMPLETA, carregaImagem(new File(PASTA_TEMPORARIA + "\\" + completa.get().getArquivo()), new File(PASTA_TEMPORARIA + "\\" + completa.get().getDireita().getArquivo())));
                    } catch (IOException e) {
                        LOG.info("Erro ao reprocessar imagem: " + completa.get().getTipo() + ".", e);
                    }
                } else {
                    try {
                        copiaItem(new File(txtPastaOrigem.getText() + "\\" + completa.get().getNome()), PASTA_TEMPORARIA);
                        simularCapa(TipoCapa.CAPA_COMPLETA, carregaImagem(new File(PASTA_TEMPORARIA + "\\" + completa.get().getArquivo())));
                    } catch (IOException e) {
                        LOG.info("Erro ao reprocessar imagem: " + completa.get().getTipo() + ".", e);
                    }
                }
        });
    }

    private Boolean CANCELAR = false;
    final private String FRENTE = " Frente";
    final private String TUDO = " Tudo";
    final private String TRAS = " Tras";
    final private String SUMARIO = " zSumário";

    private void processar() {
        Task<Boolean> movimentaArquivos = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    salvaManga();

                    if (lsVwListaImagens.getSelectionModel().getSelectedItem() != null)
                        selecionada = lsVwListaImagens.getSelectionModel().getSelectedItem();

                    CANCELAR = false;
                    int i = 0;
                    int max = caminhoOrigem.listFiles(getFilterNameFile()).length;

                    List<File> pastasCompactar = new ArrayList<>();
                    LAST_PROCESS_FOLDERS.clear();

                    String arquivoZip = caminhoDestino.getPath().trim() + "\\" + txtNomeArquivo.getText().trim();

                    boolean mesclarCapaTudo = cbMesclarCapaTudo.isSelected();
                    boolean gerarArquivo = cbCompactarArquivo.isSelected();
                    boolean verificaPagDupla = cbVerificaPaginaDupla.isSelected();

                    updateProgress(i, max);
                    updateMessage("Criando diretórios...");

                    String nomePasta = caminhoDestino.getPath().trim() + "\\" + txtNomePastaManga.getText().trim() + " "
                            + txtVolume.getText().trim();

                    updateMessage("Criando diretórios - " + nomePasta + " Capa\\");
                    File destinoCapa = criaPasta(nomePasta + " Capa\\");
                    pastasCompactar.add(destinoCapa);

                    if (!obsLImagesSelected.isEmpty()) {
                        String nome = txtNomePastaManga.getText().trim() + " " + txtVolume.getText().trim();
                        if (nome.contains("]"))
                            nome = nome.substring(nome.indexOf(']') + 1).trim();

                        Optional<Capa> capa = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.CAPA) == 0).findFirst();
                        if (capa.isPresent())
                            limpaMargemImagens(renomeiaItem(copiaItem(new File(caminhoOrigem.getPath() + "\\" + capa.get().getNome()), destinoCapa), nome + FRENTE + capa.get().getNome().substring(capa.get().getNome().lastIndexOf("."))), false);

                        Optional<Capa> tras = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.TRAS) == 0).findFirst();
                        if (tras.isPresent())
                            limpaMargemImagens(renomeiaItem(copiaItem(new File(caminhoOrigem.getPath() + "\\" + tras.get().getNome()), destinoCapa), nome + TRAS + tras.get().getNome().substring(tras.get().getNome().lastIndexOf("."))), false);

                        Optional<Capa> sumario = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.SUMARIO) == 0).findFirst();
                        if (sumario.isPresent())
                            renomeiaItem(copiaItem(new File(caminhoOrigem.getPath() + "\\" + sumario.get().getNome()), destinoCapa), nome + SUMARIO + sumario.get().getNome().substring(sumario.get().getNome().lastIndexOf(".")));

                        if (obsLImagesSelected.stream().anyMatch(it -> it.getTipo().compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.getDireita() != null)) {
                            Optional<Capa> tudo = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.getDireita() != null).findFirst();

                            if (mesclarCapaTudo) {
                                copiaItem(new File(caminhoOrigem.getPath() + "\\" + tudo.get().getNome()), PASTA_TEMPORARIA);
                                copiaItem(new File(caminhoOrigem.getPath() + "\\" + tudo.get().getDireita().getNome()), PASTA_TEMPORARIA);

                                File esquerda = new File(PASTA_TEMPORARIA, tudo.get().getNome());
                                File direita = new File(PASTA_TEMPORARIA, tudo.get().getDireita().getNome());

                                limpaMargemImagens(esquerda, true);
                                limpaMargemImagens(direita, true);

                                mesclarImagens(new File(destinoCapa.getPath() + "\\" + nome + TUDO + ".png"), esquerda, direita);
                            } else {
                                File arquivo = new File(caminhoOrigem.getPath() + "\\" + nome + TUDO + tudo.get().getNome().substring(tudo.get().getNome().lastIndexOf(".")));
                                renomeiaItem(copiaItem(new File(caminhoOrigem, tudo.get().getNome()), destinoCapa), arquivo.getName());
                                limpaMargemImagens(arquivo, true);
                            }
                        } else if (obsLImagesSelected.stream().anyMatch(it -> it.getTipo().compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.isDupla()) || obsLImagesSelected.stream().anyMatch(it -> it.getTipo().compareTo(TipoCapa.SUMARIO) != 0 && it.isDupla())) {
                            Optional<Capa> tudo = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.CAPA_COMPLETA) == 0 && it.isDupla()).findFirst();

                            if (tudo.isEmpty())
                                tudo = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.SUMARIO) != 0 && it.isDupla()).findFirst();

                            File arquivo = new File(caminhoOrigem.getPath() + "\\" + nome + TUDO + tudo.get().getNome().substring(tudo.get().getNome().lastIndexOf(".")));
                            renomeiaItem(copiaItem(new File(caminhoOrigem, tudo.get().getNome()), destinoCapa), arquivo.getName());
                            limpaMargemImagens(arquivo, true);

                            if (tras.isEmpty() || capa.isEmpty()) {
                                File esquerda = new File(PASTA_TEMPORARIA, tudo.get().getNome());
                                File direita = new File(PASTA_TEMPORARIA, tudo.get().getDireita().getNome());

                                if (divideImagens(arquivo, esquerda, direita)) {
                                    renomeiaItem(copiaItem(esquerda, destinoCapa), nome + FRENTE + ".png");
                                    renomeiaItem(copiaItem(direita, destinoCapa), nome + TRAS + ".png");
                                }
                            }
                        } else {
                            Optional<Capa> tudo = obsLImagesSelected.stream().filter(it -> it.getTipo().compareTo(TipoCapa.CAPA_COMPLETA) == 0).findFirst();
                            if (tudo.isPresent()) {
                                File arquivo = new File(caminhoOrigem.getPath() + "\\" + nome + TUDO + tudo.get().getNome().substring(tudo.get().getNome().lastIndexOf(".")));
                                renomeiaItem(copiaItem(new File(caminhoOrigem, tudo.get().getNome()), destinoCapa), arquivo.getName());
                                limpaMargemImagens(arquivo, true);
                            }
                        }

                    }

                    int pagina = 0, proxCapitulo = 0, contadorCapitulo = 0;
                    boolean contar = false;

                    File destino = criaPasta(nomePasta + " " + lista.get(pagina).getNomePasta() + "\\");
                    pastasCompactar.add(destino);
                    contadorCapitulo = Integer.valueOf(lista.get(pagina).getNumeroPagina());
                    pagina++;

                    if (lista.size() > 1)
                        proxCapitulo = lista.get(pagina).getNumero();

                    for (File arquivos : caminhoOrigem.listFiles(getFilterNameFile())) {

                        if (CANCELAR)
                            return true;

                        LOG.info("Contar: " + contar + " - Contador: " + contadorCapitulo + " - Prox cap: "
                                + proxCapitulo + " - Nome Imagem: " + arquivos.getName());

                        if (arquivos.getName().equalsIgnoreCase(selecionada))
                            contar = true;

                        if (contar && verificaPagDupla) {
                            if (verificaPaginaDupla(arquivos))
                                contadorCapitulo++;
                        }

                        if ((contadorCapitulo >= proxCapitulo) && (pagina < lista.size())) {

                            updateMessage(
                                    "Criando diretório - " + nomePasta + " " + lista.get(pagina).getNomePasta() + "\\");
                            destino = criaPasta(nomePasta + " " + lista.get(pagina).getNomePasta() + "\\");
                            pastasCompactar.add(destino);

                            pagina++;

                            if ((pagina < lista.size()))
                                proxCapitulo = Integer.valueOf(lista.get(pagina).getNumeroPagina());
                        }

                        i++;
                        updateProgress(i, max);
                        updateMessage(
                                "Processando item " + i + " de " + max + ". Copiando - " + arquivos.getAbsolutePath());
                        copiaItem(arquivos, destino);

                        if (contar)
                            contadorCapitulo++;

                        if (!btnProcessar.accessibleTextProperty().getValue().equalsIgnoreCase("CANCELA"))
                            break;
                    }

                    if (gerarArquivo) {
                        updateMessage("Compactando arquivo: " + arquivoZip);

                        destino = new File(arquivoZip);
                        if (destino.exists())
                            destino.delete();
                        LAST_PROCESS_FOLDERS = pastasCompactar;
                        if (!compactaArquivo(destino, pastasCompactar))
                            Platform.runLater(() -> txtSimularPasta
                                    .setText("Erro ao gerar o arquivo, necessário compacta-lo manualmente."));
                    }
                } catch (Exception e) {
                    LOG.error("Erro ao processar.", e);
                    Alert a = new Alert(AlertType.NONE);
                    a.setAlertType(AlertType.ERROR);
                    a.setContentText(e.toString());
                    a.show();
                }

                return true;

            }

            @Override
            protected void succeeded() {
                getValue();
                updateMessage("Arquivos movidos com sucesso.");
                pbProgresso.progressProperty().unbind();
                lblProgresso.textProperty().unbind();
                habilita();
            }

            @Override
            protected void failed() {
                super.failed();

                updateMessage("Erro ao mover os arquivos.");

                Alert a = new Alert(AlertType.NONE);
                a.setAlertType(AlertType.ERROR);
                a.setContentText("Erro ao mover arquivos.");
                a.show();
                habilita();
            }

        };
        pbProgresso.progressProperty().bind(movimentaArquivos.progressProperty());
        lblProgresso.textProperty().bind(movimentaArquivos.messageProperty());

        Thread t = new Thread(movimentaArquivos);
        t.setDaemon(true);
        t.start();
    }

    private Process proc = null;

    @SuppressWarnings("unused")
    private boolean compactaArquivo(File rar, File arquivos) {
        boolean success = true;
        String comando = "rar a -ma4 -ep1 " + '"' + rar.getPath() + '"' + " " + '"'
                + arquivos.getPath() + '"';

        LOG.info(comando);

        comando = "cmd.exe /C cd \"" + WINRAR + "\" &&" + comando;

        proc = null;
        try {
            Runtime rt = Runtime.getRuntime();
            proc = rt.exec(comando);

            Platform.runLater(() -> {
                try {
                    LOG.info("Resultado: " + proc.waitFor());
                } catch (InterruptedException e) {
                    LOG.error("Erro ao executar o comando cmd.", e);
                }
            });

            String resultado = "";

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            String s = null;
            while ((s = stdInput.readLine()) != null)
                resultado += s + "\n";

            if (!resultado.isEmpty())
                LOG.info("Output comand:\n" + resultado);

            s = null;
            resultado = "";
            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            while ((s = stdError.readLine()) != null)
                resultado += s + "\n";

            if (!resultado.isEmpty()) {
                success = false;
                LOG.info("Error comand:\n" + resultado
                        + "\nNecessário adicionar o rar no path e reiniciar a aplicação.");
            }

            return success;
        } catch (Exception e) {
            LOG.error("Erro ao compactar o arquivo.", e);
            return false;
        } finally {
            if (proc != null)
                proc.destroy();
        }
    }

    private boolean compactaArquivo(File rar, List<File> arquivos) {
        boolean success = true;
        String compactar = "";
        for (File arquivo : arquivos)
            compactar += '"' + arquivo.getPath() + '"' + ' ';

        String comando = "rar a -ma4 -ep1 " + '"' + rar.getPath() + '"' + " " + compactar;

        LOG.info(comando);

        comando = "cmd.exe /C cd \"" + WINRAR + "\" &&" + comando;

        try {
            Runtime rt = Runtime.getRuntime();
            proc = rt.exec(comando);

            Platform.runLater(() -> {
                try {
                    LOG.info("Resultado: " + proc.waitFor());
                } catch (InterruptedException e) {
                    LOG.error("Erro ao executar o comando.", e);
                }
            });

            String resultado = "";

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            String s = null;
            while ((s = stdInput.readLine()) != null)
                resultado += s + "\n";

            if (!resultado.isEmpty())
                LOG.info("Output comand:\n" + resultado);

            s = null;
            resultado = "";
            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            while ((s = stdError.readLine()) != null)
                resultado += s + "\n";

            if (!resultado.isEmpty()) {
                success = false;
                LOG.info("Error comand:\n" + resultado
                        + "\nNecessário adicionar o rar no path e reiniciar a aplicação.");
            }

            return success;
        } catch (Exception e) {
            LOG.error("Erro ao compactar o arquivo.", e);
            return false;
        } finally {
            if (proc != null)
                proc.destroy();
        }
    }

    private boolean mesclarImagens(File arquivoDestino, File frente, File tras) {
        if (arquivoDestino == null || frente == null || tras == null)
            return false;

        BufferedImage img1;
        BufferedImage img2;
        try {
            img1 = ImageIO.read(frente);
            img2 = ImageIO.read(tras);

            int offset = 0;
            int width = img1.getWidth() + img2.getWidth() + offset;
            int height = Math.max(img1.getHeight(), img2.getHeight()) + offset;
            BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = newImage.createGraphics();
            java.awt.Color oldColor = g2.getColor();
            g2.setPaint(java.awt.Color.WHITE);
            g2.fillRect(0, 0, width, height);
            g2.setColor(oldColor);
            if (img1.getHeight() > img2.getHeight()) {
                int diff = (img1.getHeight() - img2.getHeight()) / 2;
                g2.drawImage(img1, null, 0, 0);
                g2.drawImage(img2, null, img1.getWidth() + offset, diff);
            } else if (img1.getHeight() < img2.getHeight()) {
                int diff = (img2.getHeight() - img1.getHeight()) / 2;
                g2.drawImage(img1, null, 0, diff);
                g2.drawImage(img2, null, img1.getWidth() + offset, 0);
            } else {
                g2.drawImage(img1, null, 0, 0);
                g2.drawImage(img2, null, img1.getWidth() + offset, 0);
            }

            g2.dispose();

            return ImageIO.write(newImage, "png", arquivoDestino);
        } catch (IOException e) {
            LOG.error("Erro ao mesclar as imagens.", e);
        }
        return false;
    }

    private boolean divideImagens(File arquivo, File destinoFrente, File destinoTras) {
        if (arquivo == null || destinoFrente == null || destinoTras == null)
            return false;

        BufferedImage image;
        try {
            image = ImageIO.read(arquivo);

            int width = image.getWidth() / 2;
            int height = image.getHeight();

            BufferedImage frente = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D grFrente = frente.createGraphics();
            java.awt.Color colorFrente = grFrente.getColor();
            grFrente.setPaint(java.awt.Color.WHITE);
            grFrente.fillRect(0, 0, width, height);
            grFrente.setColor(colorFrente);
            grFrente.drawImage(image, null, 0, 0);
            grFrente.dispose();

            int offset = width;

            BufferedImage tras = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D grTras = tras.createGraphics();
            java.awt.Color colorTras = grTras.getColor();
            grTras.setPaint(java.awt.Color.WHITE);
            grTras.fillRect(0, 0, width, height);
            grTras.setColor(colorTras);
            grTras.drawImage(image, null, -offset, 0);
            grTras.dispose();

            return ImageIO.write(frente, "png", destinoFrente) && ImageIO.write(tras, "png", destinoTras);
        } catch (IOException e) {
            LOG.error("Erro ao dividir as imagens.", e);
        }
        return false;
    }

    private File limpaMargemImagens(File arquivo, Boolean clearTopBottom) {
        if (arquivo == null || !cbAjustarMargemCapa.isSelected())
            return arquivo;

        BufferedImage image;
        try {
            image = ImageIO.read(arquivo);

            int branco = java.awt.Color.WHITE.getRGB();

            int startX = 0;
            int endX = image.getWidth();
            for (var x = 0; x < image.getWidth(); x++) {
                for (var y = 0; y < image.getHeight(); y++) {
                    if (image.getRGB(x, y) != branco) {
                        startX = x;
                        break;
                    }
                }
                if (startX > 0)
                    break;
            }

            for (var x = image.getWidth() - 1; x >= 0; x--) {
                for (var y = 0; y < image.getHeight(); y++) {
                    if (image.getRGB(x, y) != branco) {
                        endX = x;
                        break;
                    }
                }
                if (endX < image.getWidth())
                    break;
            }

            int startY = 0;
            int endY = image.getHeight();
            if (clearTopBottom) {
                for (var y = 0; y < image.getHeight(); y++) {
                    for (var x = 0; x < image.getWidth(); x++) {
                        if (image.getRGB(x, y) != branco) {
                            startY = y;
                            break;
                        }
                    }
                    if (startY > 0)
                        break;
                }

                for (var y = image.getHeight() - 1; y >= 0; y--) {
                    for (var x = 0; x < image.getWidth(); x++) {
                        if (image.getRGB(x, y) != branco) {
                            endY = y;
                            break;
                        }
                    }
                    if (endY < image.getHeight())
                        break;
                }
            }

            Integer width = endX - startX;
            Integer height = endY - startY;
            BufferedImage frente = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D grFrente = frente.createGraphics();
            java.awt.Color colorFrente = grFrente.getColor();
            grFrente.setPaint(java.awt.Color.WHITE);
            grFrente.fillRect(0, 0, width, height);
            grFrente.setColor(colorFrente);
            grFrente.drawImage(image, null, -startX, -startY);
            grFrente.dispose();

            ImageIO.write(frente, "png", arquivo);
        } catch (IOException e) {
            LOG.error("Erro ao dividir as imagens.", e);
        }
        return arquivo;
    }

    public Boolean isPaginaDupla(File arquivo) {
        if (arquivo == null || !arquivo.exists())
            return false;

        BufferedImage image;
        try {
            image = ImageIO.read(arquivo);
            return (image.getWidth() / image.getHeight()) > 0.9;
        } catch (IOException e) {
            LOG.error("Erro ao verificar imagem.", e);
        }
        return false;
    }

    public Image carregaImagem(File esquerda, File direita) {
        if (direita == null || esquerda == null || !direita.exists() || !esquerda.exists())
            return null;

        try {
            limpaMargemImagens(direita, true);
            limpaMargemImagens(esquerda, true);

            File img = new File(PASTA_TEMPORARIA, "tudo.png");
            if (img.exists())
                img.delete();
            img.createNewFile();

            if (cbMesclarCapaTudo.isSelected())
                mesclarImagens(img, esquerda, direita);
            else
                copiaItem(esquerda, PASTA_TEMPORARIA, img.getName());

            return new Image(img.getAbsolutePath());
        } catch (IOException e) {
            LOG.error("Erro ao verificar imagem.", e);
        }

        return null;
    }

    public Image carregaImagem(File arquivo) {
        if (arquivo == null || !arquivo.exists())
            return null;

        limpaMargemImagens(arquivo, false);
        return new Image(arquivo.getAbsolutePath());
    }

    @FXML
    private void onBtnProcessa() {
        if (validaCampos()) {
            if (btnProcessar.accessibleTextProperty().getValue().equalsIgnoreCase("PROCESSA")) {
                btnProcessar.accessibleTextProperty().set("CANCELA");
                btnProcessar.setText("Cancelar");
                apGlobal.cursorProperty().set(Cursor.WAIT);
                desabilita();
                processar();
            } else
                CANCELAR = true;

        }
    }

    @FXML
    private void onBtnCarregarPastaOrigem() {
        caminhoOrigem = selecionaPasta(txtPastaOrigem.getText());
        if (caminhoOrigem != null)
            txtPastaOrigem.setText(caminhoOrigem.getAbsolutePath());
        else
            txtPastaOrigem.setText("");
        listaItens();
    }

    private void carregaPastaOrigem() {
        caminhoOrigem = new File(txtPastaOrigem.getText());
        limparCapas();
        listaItens();
    }

    @FXML
    private void onBtnCarregarPastaDestino() {
        caminhoDestino = selecionaPasta(txtPastaDestino.getText());
        if (txtPastaDestino != null)
            txtPastaDestino.setText(caminhoDestino.getAbsolutePath());
        else
            txtPastaDestino.setText("");

        simulaNome();
    }

    private void carregaPastaDestino() {
        caminhoDestino = new File(txtPastaDestino.getText());
        simulaNome();
    }

    private void listaItens() {
        if ((caminhoOrigem != null) && (caminhoOrigem.list() != null))
            obsLListaItens = FXCollections.<String>observableArrayList(caminhoOrigem.list(getFilterNameFile()));
        else
            obsLListaItens = FXCollections.<String>observableArrayList("");
        lsVwListaImagens.setItems(obsLListaItens);
        limparCapas();
        selecionada = obsLListaItens.get(0);
    }

    private void simulaNome() {
        txtSimularPasta.setText(txtNomePastaManga.getText().trim() + " " + txtVolume.getText().trim() + " "
                + txtNomePastaCapitulo.getText().trim() + " 00");

        String nome = txtNomePastaManga.getText().contains("]")
                ? txtNomePastaManga.getText().substring(txtNomePastaManga.getText().indexOf("]") + 1).trim()
                : txtNomePastaManga.getText().trim();
        String posFix = txtNomePastaManga.getText().contains("[JPN]") ? " (Jap)" : "";
        txtNomeArquivo.setText(nome + " " + txtVolume.getText().trim() + posFix + ".cbr");
    }

    private File selecionaPasta(String pasta) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Selecione o arquivo.");
        if (!pasta.isEmpty()) {
            File defaultDirectory = new File(pasta);
            fileChooser.setInitialDirectory(defaultDirectory);
        }
        return fileChooser.showDialog(null);
    }

    @FXML
    private void onBtnLimpar() {
        lista = new ArrayList<>();
        obsLCaminhos = FXCollections.observableArrayList(lista);
        tbViewTabela.setItems(obsLCaminhos);
    }

    @FXML
    private void onBtnSubtrair() {
        if (!txtQuantidade.getText().isEmpty())
            modificaNumeroPaginas(Integer.valueOf(txtQuantidade.getText()) * -1);
    }

    @FXML
    private void onBtnSomar() {
        if (!txtQuantidade.getText().isEmpty())
            modificaNumeroPaginas(Integer.valueOf(txtQuantidade.getText()));
    }

    private void modificaNumeroPaginas(Integer quantidade) {
        for (Caminhos caminho : lista) {
            int qtde = caminho.getNumero() + quantidade;
            if (qtde < 1)
                qtde = 1;
            caminho.setNumero(qtde);
        }

        obsLCaminhos = FXCollections.observableArrayList(lista);
        tbViewTabela.setItems(obsLCaminhos);
        txtQuantidade.setText("");
    }

    private void limpaCampo() {
        txtGerarInicio.requestFocus();
    }

    @FXML
    private void onBtnImporta() {
        if (!txtAreaImportar.getText().trim().isEmpty()) {
            String nomePasta = "";
            String separador = txtSeparador.getText().trim();

            if (separador.isEmpty())
                separador = " ";

            txtSeparador.setText(separador);

            String linhas[] = txtAreaImportar.getText().split("\\r?\\n");
            String linha[];

            lista = new ArrayList<>();
            for (String ls : linhas) {
                linha = ls.split(txtSeparador.getText());

                if (txtNomePastaCapitulo.getText().trim().equalsIgnoreCase("Capítulo")
                        && linha[0].toUpperCase().contains("EXTRA"))
                    nomePasta = linha[0].trim();
                else
                    nomePasta = txtNomePastaCapitulo.getText().trim() + " " + linha[0].trim();

                lista.add(new Caminhos(linha[0], linha[1], nomePasta));
            }

            obsLCaminhos = FXCollections.observableArrayList(lista);
            tbViewTabela.setItems(obsLCaminhos);
            tbViewTabela.refresh();

        }
    }

    @FXML
    private void onBtnGerarCapitulos() {
        if (!txtGerarInicio.getText().trim().isEmpty() && !txtGerarFim.getText().trim().isEmpty()) {
            if (manga == null)
                manga = geraManga(null);

            Integer inicio = Integer.parseInt(txtGerarInicio.getText().trim());
            Integer fim = Integer.parseInt(txtGerarFim.getText().trim());

            if (inicio <= fim) {
                String texto = ""; // txtAreaImportar.getText();
                // if (!texto.isEmpty())
                // texto += "\r\n";

                String padding = "%0" + (fim.toString().length() > 3 ? String.valueOf(fim.toString().length()) : "3")
                        + "d";
                for (Integer i = inicio; i <= fim; i++)
                    texto += String.format(padding, i) + "-" + (i < fim ? "\r\n" : "");

                txtAreaImportar.setText(texto);
            } else
                txtGerarInicio.setUnFocusColor(Color.GRAY);
        } else {
            if (txtGerarInicio.getText().trim().isEmpty())
                txtGerarInicio.setUnFocusColor(Color.GRAY);

            if (txtGerarFim.getText().trim().isEmpty())
                txtGerarFim.setUnFocusColor(Color.GRAY);
        }
    }

    final Set<TextField> mostraFinalTexto = new HashSet<TextField>();

    private void textFieldMostraFinalTexto(JFXTextField txt) {
        mostraFinalTexto.add(txt);
        final Set<TextField> onFocus = new HashSet<TextField>();
        final Set<TextField> overrideNextCaratChange = new HashSet<TextField>();
        final ChangeListener<Boolean> onLoseFocus = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                ReadOnlyProperty<? extends Boolean> property = (ReadOnlyProperty<? extends Boolean>) observable;
                TextField tf = (TextField) property.getBean();

                if (oldValue && onFocus.contains(tf))
                    onFocus.remove(tf);

                if (newValue)
                    onFocus.add(tf);

                if (!newValue.booleanValue())
                    overrideNextCaratChange.add(tf);
            }
        };

        final ChangeListener<Number> onCaratChange = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                ReadOnlyProperty<? extends Number> property = (ReadOnlyProperty<? extends Number>) observable;
                TextField tf = (TextField) property.getBean();
                if (overrideNextCaratChange.contains(tf)) {
                    tf.end();
                    overrideNextCaratChange.remove(tf);
                } else if (!onFocus.contains(tf) && mostraFinalTexto.contains(tf))
                    tf.end();
            }
        };

        txt.focusedProperty().addListener(onLoseFocus);
        txt.caretPositionProperty().addListener(onCaratChange);
    }

    public Boolean contemTipoSelecionado(TipoCapa tipo, String caminho) {
        if (obsLImagesSelected.isEmpty())
            return false;
        return obsLImagesSelected.stream()
                .anyMatch(capa -> capa.getTipo().equals(tipo) && capa.getNome().equalsIgnoreCase(caminho));
    }


    private Timer dellaySubir = null;
    private Timer dellayDescer = null;

    private void selecionaImagens() {
        obsLImagesSelected = FXCollections.observableArrayList();

        lsVwListaImagens.addEventFilter(ScrollEvent.ANY, e -> {
            if (e.getDeltaY() > 0) {
                if (e.getDeltaY() > 10) {
                    btnScrollSubir.setVisible(true);
                    btnScrollSubir.setDisable(false);

                    if (dellaySubir != null)
                        dellaySubir.cancel();

                    dellaySubir = new Timer();
                    dellaySubir.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            btnScrollSubir.setVisible(false);
                            btnScrollSubir.setDisable(true);
                            dellaySubir = null;
                        }
                    }, 3000);
                }
            } else {
                if (e.getDeltaY() < 10) {
                    btnScrollDescer.setVisible(true);
                    btnScrollDescer.setDisable(false);

                    if (dellayDescer != null)
                        dellayDescer.cancel();

                    dellayDescer = new Timer();
                    dellayDescer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            btnScrollDescer.setVisible(false);
                            btnScrollDescer.setDisable(true);
                            dellayDescer = null;
                        }
                    }, 3000);
                }
            }
        });

        lsVwListaImagens.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent click) {
                if (click.getClickCount() > 1) {
                    if (click.isControlDown())
                        limparCapas();
                    else {
                        String item = lsVwListaImagens.getSelectionModel().getSelectedItem();

                        if (item != null) {
                            if (obsLImagesSelected.stream().anyMatch(e -> e.getNome().equalsIgnoreCase(item)))
                                remCapa(item);
                            else {
                                TipoCapa tipo = TipoCapa.CAPA;
                                if (click.isShiftDown())
                                    tipo = TipoCapa.SUMARIO;
                                else if (click.isAltDown())
                                    tipo = TipoCapa.CAPA_COMPLETA;

                                addCapa(tipo, item);
                            }
                        }
                    }

                }
            }
        });

        PseudoClass capaSelected = PseudoClass.getPseudoClass("capaSelected");
        PseudoClass capaCompletaSelected = PseudoClass.getPseudoClass("capaCompletaSelected");
        PseudoClass sumarioSelected = PseudoClass.getPseudoClass("sumarioSelected");

        lsVwListaImagens.setCellFactory(lv -> {

            JFXListCell<String> cell = new JFXListCell<String>() {

                @Override
                protected void updateItem(String images, boolean empty) {
                    super.updateItem(images, empty);
                    setText(images);
                }

            };

            InvalidationListener listenerCapa = obs -> cell.pseudoClassStateChanged(capaSelected,
                    cell.getItem() != null && contemTipoSelecionado(TipoCapa.CAPA, cell.getItem()));
            InvalidationListener listenerCapaCompleta = obs -> cell.pseudoClassStateChanged(capaCompletaSelected,
                    cell.getItem() != null && contemTipoSelecionado(TipoCapa.CAPA_COMPLETA, cell.getItem()));
            InvalidationListener listenerSumario = obs -> cell.pseudoClassStateChanged(sumarioSelected,
                    cell.getItem() != null && contemTipoSelecionado(TipoCapa.SUMARIO, cell.getItem()));

            cell.itemProperty().addListener(listenerCapa);
            cell.itemProperty().addListener(listenerCapaCompleta);
            cell.itemProperty().addListener(listenerSumario);

            obsLImagesSelected.addListener(listenerCapa);
            obsLImagesSelected.addListener(listenerCapaCompleta);
            obsLImagesSelected.addListener(listenerSumario);

            return cell;

        });

    }

    private void editaColunas() {
        clCapitulo.setCellFactory(TextFieldTableCell.forTableColumn());
        clCapitulo.setOnEditCommit(e -> {
            e.getTableView().getItems().get(e.getTablePosition().getRow()).setCapitulo(e.getNewValue());
            e.getTableView().getItems().get(e.getTablePosition().getRow())
                    .setNomePasta(txtNomePastaCapitulo.getText().trim() + " " + e.getNewValue());
        });

        clNumeroPagina.setCellFactory(TextFieldTableCell.forTableColumn());
        clNumeroPagina.setOnEditCommit(
                e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setNumero(e.getNewValue()));

        clNomePasta.setCellFactory(TextFieldTableCell.forTableColumn());
        clNomePasta.setOnEditCommit(
                e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setNomePasta(e.getNewValue()));
    }

    private void linkaCelulas() {
        clCapitulo.setCellValueFactory(new PropertyValueFactory<>("capitulo"));
        clNumeroPagina.setCellValueFactory(new PropertyValueFactory<>("numeroPagina"));
        clNomePasta.setCellValueFactory(new PropertyValueFactory<>("nomePasta"));
        editaColunas();
        selecionaImagens();
    }

    @SuppressWarnings("unused")
    private void carregarTeste() {
        txtPastaOrigem.setText(
                "D:\\Arquivos\\Mangas arrumar\\Japonês\\Kakegurui\\Kakegurui Kari v01-04\\[河本ほむら×川村拓] 賭ケグルイ(仮) 第01巻");
        txtPastaDestino.setText("D:\\Arquivos\\Mangas arrumar\\Japonês\\Kakegurui\\Kakegurui Kari v01-04\\New folder");

        carregaPastaOrigem();
        carregaPastaDestino();

        txtNomePastaManga.setText("[JPN] Kakegurui Kari -");
        txtVolume.setText("Volume 01");
        txtNomePastaCapitulo.setText("Capítulo");

        lista.add(new Caminhos("001", "3", "Capítulo 001"));
        lista.add(new Caminhos("002", "17", "Capítulo 002"));
        lista.add(new Caminhos("003", "29", "Capítulo 003"));
        lista.add(new Caminhos("004", "41", "Capítulo 004"));
        lista.add(new Caminhos("005", "57", "Capítulo 005"));
        obsLCaminhos = FXCollections.observableArrayList(lista);
        tbViewTabela.setItems(obsLCaminhos);
        tbViewTabela.refresh();
    }

    private String pastaAnterior = "";

    private void configuraTextEdit() {
        textFieldMostraFinalTexto(txtSimularPasta);
        textFieldMostraFinalTexto(txtPastaOrigem);

        txtPastaOrigem.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> {
            if (newPropertyValue)
                pastaAnterior = txtPastaOrigem.getText();

            if (oldPropertyValue && txtPastaOrigem.getText().compareToIgnoreCase(pastaAnterior) != 0)
                carregaPastaOrigem();

            txtPastaOrigem.setUnFocusColor(Color.GRAY);
        });

        txtPastaOrigem.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER))
                txtVolume.requestFocus();
            else if (e.getCode().equals(KeyCode.TAB) && !e.isControlDown() && !e.isAltDown() && !e.isShiftDown()) {
                txtPastaDestino.requestFocus();
                e.consume();
            }
        });

        txtPastaDestino.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> {
            if (oldPropertyValue)
                carregaPastaDestino();

            txtPastaDestino.setUnFocusColor(Color.GRAY);
        });

        txtPastaDestino.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER))
                txtNomePastaManga.requestFocus();
            else if (e.getCode().equals(KeyCode.TAB) && !e.isControlDown() && !e.isAltDown() && !e.isShiftDown()) {
                txtNomePastaManga.requestFocus();
                e.consume();
            }
        });

        txtNomePastaManga.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> {
            if (oldPropertyValue)
                simulaNome();
        });

        txtNomePastaManga.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER))
                clickTab();
        });

        txtNomeArquivo.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> txtPastaDestino.setUnFocusColor(Color.GRAY));

        txtNomeArquivo.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER))
                clickTab();
        });

        txtNomeArquivo.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> {
            if (oldPropertyValue) {
                if (manga == null)
                    manga = geraManga(null);
            }
        });

        txtVolume.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
                                Boolean newPropertyValue) {
                if (oldPropertyValue) {
                    simulaNome();
                    carregaManga();
                }
            }
        });

        txtVolume.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER))
                txtGerarInicio.requestFocus();
            else if (e.getCode().equals(KeyCode.TAB) && !e.isControlDown() && !e.isAltDown() && !e.isShiftDown()) {
                txtGerarInicio.requestFocus();
                e.consume();
            }
        });

        txtNomePastaCapitulo.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> {
            if (oldPropertyValue)
                simulaNome();
        });

        txtNomePastaCapitulo.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER"))
                clickTab();
        });

        txtGerarInicio.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> txtPastaDestino.setUnFocusColor(Color.GRAY));

        txtGerarInicio.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*"))
                txtGerarInicio.setText(oldValue);
            else if (newValue != null && newValue.isEmpty())
                txtGerarInicio.setText("0");
        });

        txtGerarInicio.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER"))
                clickTab();
        });

        txtGerarFim.focusedProperty().addListener((arg0, oldPropertyValue, newPropertyValue) -> txtPastaDestino.setUnFocusColor(Color.GRAY));

        txtGerarFim.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*"))
                txtGerarFim.setText(oldValue);
            else if (newValue != null && newValue.isEmpty())
                txtGerarFim.setText("0");
        });

        txtGerarFim.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                onBtnGerarCapitulos();
                txtAreaImportar.requestFocus();
                int position = txtAreaImportar.getText().indexOf('-') + 1;
                txtAreaImportar.positionCaret(position);
                e.consume();
            } else if (e.getCode().equals(KeyCode.TAB) && !e.isShiftDown()) {
                txtAreaImportar.requestFocus();
                e.consume();
            }
        });

        txtAreaImportar.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode().equals(KeyCode.ENTER))
                onBtnImporta();
        });

        txtQuantidade.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
                                Boolean newPropertyValue) {
                txtPastaDestino.setUnFocusColor(Color.GRAY);
            }
        });

        txtQuantidade.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*"))
                txtGerarFim.setText(oldValue);
        });

        cbMesclarCapaTudo.selectedProperty().addListener((obs, oldValue, newValue) -> reloadCapa());
        cbAjustarMargemCapa.selectedProperty().addListener((obs, oldValue, newValue) -> reloadCapa());
    }

    public void configurarAtalhos(Scene scene) {
        KeyCombination kcInicioFocus = new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN);
        KeyCombination kcFimFocus = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
        KeyCombination kcImportFocus = new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN);
        KeyCombination kcImportar = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN);

        KeyCombination kcProcessar = new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN);
        Mnemonic mnProcessar = new Mnemonic(btnProcessar, kcProcessar);
        scene.addMnemonic(mnProcessar);

        KeyCombination kcProcessarAlter = new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN);
        Mnemonic mnProcessarAlter = new Mnemonic(btnProcessar, kcProcessarAlter);
        scene.addMnemonic(mnProcessarAlter);

        KeyCombination kcCompactar = new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN);
        Mnemonic mnCompactar = new Mnemonic(btnCompactar, kcCompactar);
        scene.addMnemonic(mnCompactar);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.isControlDown() && lsVwListaImagens.getSelectionModel().getSelectedItem() != null)
                selecionada = lsVwListaImagens.getSelectionModel().getSelectedItem();

            if (kcInicioFocus.match(ke))
                txtGerarInicio.requestFocus();

            if (kcFimFocus.match(ke))
                txtGerarFim.requestFocus();

            if (kcImportFocus.match(ke))
                txtAreaImportar.requestFocus();

            if (kcImportar.match(ke))
                btnImportar.fire();

            if (kcProcessar.match(ke) || kcProcessarAlter.match(ke))
                btnProcessar.fire();

            if (kcCompactar.match(ke))
                btnCompactar.fire();
        });

    }

    private void clickTab() {
        Robot robot = new Robot();
        robot.keyPress(KeyCode.TAB);
    }

    @Override
    public synchronized void initialize(URL arg0, ResourceBundle arg1) {
        linkaCelulas();
        limpaCampos();
        configuraTextEdit();

        try {
            WINRAR = Configuracao.loadProperties().getProperty("caminho_winrar");
        } catch (Exception e) {
            LOG.error("Erro ao obter o caminho do winrar.", e);
        }
    }

    public static URL getFxmlLocate() {
        return TelaInicialController.class.getResource("/view/TelaInicial.fxml");
    }

    public static String getIconLocate() {
        return "/images/icoProcessar_512.png";
    }

}
