package com.fenix.ordenararquivos.controller;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXListCell;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.Mnemonic;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.DirectoryChooser;
import com.fenix.ordenararquivos.configuration.Configuracao;
import com.fenix.ordenararquivos.model.Caminhos;
import com.fenix.ordenararquivos.model.Capa;
import com.fenix.ordenararquivos.model.Manga;
import com.fenix.ordenararquivos.model.TipoCapa;
import com.fenix.ordenararquivos.service.MangaServices;

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

	private ObservableList<Caminhos> obsLCaminhos;
	private ObservableList<String> obsLListaItens;
	private List<Caminhos> lista;
	private File caminhoOrigem;
	private File caminhoDestino;
	private String selecionada;
	private ObservableList<Capa> obsLImagesSelected;

	private MangaServices service = new MangaServices();

	private void limpaCampos() {
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
		obsLImagesSelected.clear();
		selecionada = null;

		lblProgresso.setText("");
		pbProgresso.setProgress(0);
	}

	private FilenameFilter getFilterNameFile() {
		return new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.lastIndexOf('.') > 0) {
					Pattern p = Pattern.compile(IMAGE_PATTERN);

					if (p.matcher(name).matches())
						return true;
					else
						return false;
				}
				return false;
			}
		};
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
		Path arquivoDestino = Paths.get(destino.toPath() + "/" + arquivo.getName());
		Files.copy(arquivo.toPath(), arquivoDestino, StandardCopyOption.COPY_ATTRIBUTES,
				StandardCopyOption.REPLACE_EXISTING);
		return arquivoDestino;
	}

	private void renomeiaItem(Path arquivo, String nome) throws IOException {
		Files.move(arquivo, arquivo.resolveSibling(nome), StandardCopyOption.REPLACE_EXISTING);
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

	private Boolean CANCELAR = false;

	private void processar() {
		Task<Boolean> movimentaArquivos = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				try {

					salvaManga();

					if (lsVwListaImagens.getSelectionModel().getSelectedItem() != null)
						selecionada = lsVwListaImagens.getSelectionModel().getSelectedItem();

					CANCELAR = false;
					int i = 0;
					int max = caminhoOrigem.listFiles(getFilterNameFile()).length;

					List<File> pastasCompactar = new ArrayList<File>();
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
					pastasCompactar.add(criaPasta(nomePasta + " Capa\\"));

					if (!obsLImagesSelected.isEmpty()) {
						File destinoCapa = new File(nomePasta + " Capa\\");

						if (mesclarCapaTudo) {
							List<Capa> capas = obsLImagesSelected.parallelStream()
									.filter(capa -> capa.getTipo().equals(TipoCapa.CAPA_COMPLETA)).toList();

							if (capas.size() > 1) {
								File capaFrente = null;
								File capaTras = null;

								for (Capa capa : capas) {
									for (File arquivoCapa : caminhoOrigem.listFiles(getFilterNameFile())) {
										if (capa.getArquivo().equalsIgnoreCase(arquivoCapa.getName())) {
											if (capaFrente == null)
												capaFrente = arquivoCapa;
											else if (capaTras == null)
												capaTras = arquivoCapa;

											if (capaFrente != null && capaTras != null)
												break;
										}

										if (capaFrente != null && capaTras != null)
											break;
									}
								}

								if (capaFrente != null && capaTras != null) {
									String nome = txtNomePastaManga.getText().trim() + " " + txtVolume.getText().trim();
									if (nome.contains("]"))
										nome = nome.substring(nome.indexOf(']') + 1, nome.length()).trim();

									nome += " Tudo";
									nome += capaFrente.getName().substring(capaFrente.getName().lastIndexOf("."),
											capaFrente.getName().length());
									if (mesclarImagens(new File(destinoCapa.getPath() + "\\" + nome), capaFrente,
											capaTras))
										obsLImagesSelected
												.removeIf(capa -> capa.getTipo().equals(TipoCapa.CAPA_COMPLETA));
									else {
										Platform.runLater(() -> txtSimularPasta
												.setText("Não foi possível gravar a nova imagem da junção."));
										renomeiaItem(copiaItem(capaFrente, destinoCapa), nome);
									}

									nome = txtNomePastaManga.getText().trim() + " " + txtVolume.getText().trim();
									if (nome.contains("]"))
										nome = nome.substring(nome.indexOf(']') + 1, nome.length()).trim();

									nome += " Tras";
									nome += capaFrente.getName().substring(capaFrente.getName().lastIndexOf("."),
											capaFrente.getName().length());
									renomeiaItem(copiaItem(capaTras, destinoCapa), nome);
								} else
									Platform.runLater(
											() -> txtSimularPasta.setText("Não localizado duas capas para junção."));

							} else
								Platform.runLater(
										() -> txtSimularPasta.setText("Não foi selecionado duas capas para junção."));
						}

						for (File arquivoCapa : caminhoOrigem.listFiles(getFilterNameFile())) {
							if (obsLImagesSelected.isEmpty())
								break;

							Capa item = obsLImagesSelected.stream()
									.filter(capa -> capa.getArquivo().equalsIgnoreCase(arquivoCapa.getName()))
									.findFirst().orElse(null);

							if (item == null)
								continue;

							String nome = txtNomePastaManga.getText().trim() + " " + txtVolume.getText().trim();
							if (nome.contains("]"))
								nome = nome.substring(nome.indexOf(']') + 1, nome.length());

							nome = nome.trim();

							switch (item.getTipo()) {
							case CAPA:
								nome += " Frente";
								break;
							case CAPA_COMPLETA:
								nome += " Tudo";
								break;
							case SUMARIO:
								nome += " zSumário";
								break;
							default:
							}

							nome += arquivoCapa.getName().substring(arquivoCapa.getName().lastIndexOf("."),
									arquivoCapa.getName().length());

							System.out.println("Copiando capa: " + arquivoCapa.getName() + " - Tipo: " + item.getTipo()
									+ " - Nome: " + nome);
							Path novoArquivo = copiaItem(arquivoCapa, destinoCapa);
							renomeiaItem(novoArquivo, nome);
							obsLImagesSelected.remove(item);
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

					obsLImagesSelected.clear();
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
					e.printStackTrace();
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
			g2.drawImage(img1, null, 0, 0);
			g2.drawImage(img2, null, img1.getWidth() + offset, 0);
			g2.dispose();

			return ImageIO.write(newImage, "png", arquivoDestino);
		} catch (IOException e) {
			LOG.error("Erro ao mesclar as imagens.", e);
		}
		return false;
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
		obsLImagesSelected.clear();
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
				.filter(capa -> capa.getTipo().equals(tipo) && capa.getArquivo().equalsIgnoreCase(caminho)).findFirst()
				.isPresent();
	}

	private void selecionaImagens() {
		obsLImagesSelected = FXCollections.observableArrayList();

		lsVwListaImagens.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() > 1) {
					if (click.isControlDown())
						obsLImagesSelected.clear();
					else {
						String item = lsVwListaImagens.getSelectionModel().getSelectedItem();

						if (item != null) {
							if (obsLImagesSelected.stream().filter(e -> e.getArquivo().equalsIgnoreCase(item))
									.findFirst().isPresent())
								obsLImagesSelected.removeIf(capa -> capa.getArquivo().equalsIgnoreCase(item));
							else {
								TipoCapa tipo = TipoCapa.CAPA;
								if (click.isShiftDown())
									tipo = TipoCapa.SUMARIO;
								else if (click.isAltDown())
									tipo = TipoCapa.CAPA_COMPLETA;

								obsLImagesSelected.add(new Capa(item, tipo));
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

		txtPastaOrigem.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (newPropertyValue)
					pastaAnterior = txtPastaOrigem.getText();

				if (oldPropertyValue && txtPastaOrigem.getText().compareToIgnoreCase(pastaAnterior) != 0)
					carregaPastaOrigem();

				txtPastaOrigem.setUnFocusColor(Color.GRAY);
			}
		});

		txtPastaOrigem.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER))
				txtVolume.requestFocus();
			else if (e.getCode().equals(KeyCode.TAB) && !e.isControlDown() && !e.isAltDown() && !e.isShiftDown()) {
				txtPastaDestino.requestFocus();
				e.consume();
			}
		});

		txtPastaDestino.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (oldPropertyValue)
					carregaPastaDestino();

				txtPastaDestino.setUnFocusColor(Color.GRAY);
			}
		});

		txtPastaDestino.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER))
				txtNomePastaManga.requestFocus();
			else if (e.getCode().equals(KeyCode.TAB) && !e.isControlDown() && !e.isAltDown() && !e.isShiftDown()) {
				txtNomePastaManga.requestFocus();
				e.consume();
			}
		});

		txtNomePastaManga.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (oldPropertyValue)
					simulaNome();
			}
		});

		txtNomePastaManga.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER))
				clickTab();
		});

		txtNomeArquivo.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				txtPastaDestino.setUnFocusColor(Color.GRAY);
			}
		});

		txtNomeArquivo.setOnKeyPressed(e -> {
			if (e.getCode().equals(KeyCode.ENTER))
				clickTab();
		});

		txtNomeArquivo.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (oldPropertyValue) {
					if (manga == null)
						manga = geraManga(null);
				}
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

		txtNomePastaCapitulo.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (oldPropertyValue)
					simulaNome();
			}
		});

		txtNomePastaCapitulo.setOnKeyPressed(e -> {
			if (e.getCode().toString().equals("ENTER"))
				clickTab();
		});

		txtGerarInicio.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				txtPastaDestino.setUnFocusColor(Color.GRAY);
			}
		});

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

		txtGerarFim.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				txtPastaDestino.setUnFocusColor(Color.GRAY);
			}
		});

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
			} else if (e.getCode().equals(KeyCode.TAB)) {
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

		scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			public void handle(KeyEvent ke) {
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
			}
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