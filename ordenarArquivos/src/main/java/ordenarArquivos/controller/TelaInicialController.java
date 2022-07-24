package ordenarArquivos.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXListCell;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;

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
import ordenarArquivos.model.Caminhos;
import ordenarArquivos.model.Capa;
import ordenarArquivos.model.TipoCapa;

public class TelaInicialController implements Initializable {

	// private final static Logger LOGGER =
	// Logger.getLogger(TelaInicialController.class.getName());

	private static final String IMAGE_PATTERN = "(.*/)*.+\\.(png|jpg|gif|bmp|jpeg|PNG|JPG|GIF|BMP|JPEG)$";

	@FXML
	private AnchorPane apGlobal;

	@FXML
	private JFXButton btnLimparTudo;

	@FXML
	private JFXButton btnProcessar;

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
	private JFXListView<String> lsVwListaImagens;

	@FXML
	private JFXTextField txtNomePastaCapitulo;

	@FXML
	private JFXTextField txtNumeroCapitulo;

	@FXML
	private JFXTextField txtNumeroPaginaCapitulo;

	@FXML
	private JFXTextField txtGerarInicio;

	@FXML
	private JFXTextField txtGerarFim;

	@FXML
	private JFXTextField txtSeparador;

	@FXML
	private JFXTextArea txtAreaImportar;

	@FXML
	private JFXCheckBox cbVerificaPaginaDupla;

	@FXML
	private JFXButton btnLimpar;

	@FXML
	private JFXButton btnExcluir;

	@FXML
	private JFXButton btnInserir;

	@FXML
	private JFXButton btnImportar;

	@FXML
	private TableView<Caminhos> tbViewTabela;

	@FXML
	private TableColumn<Caminhos, String> clCapitulo;

	@FXML
	private TableColumn<Caminhos, String> clNumeroPagina;

	@FXML
	private TableColumn<Caminhos, String> clNomePasta;

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

	private void limpaCampos() {
		lista = new ArrayList<>();
		obsLCaminhos = FXCollections.observableArrayList(lista);
		tbViewTabela.setItems(obsLCaminhos);

		caminhoOrigem = null;
		caminhoDestino = null;

		txtSimularPasta.setText("");
		txtPastaOrigem.setText("");
		txtPastaDestino.setText("");
		txtNomePastaManga.setText("[JPN] Manga -");
		txtVolume.setText("Volume 01");
		txtNomePastaCapitulo.setText("Capítulo");
		txtSeparador.setText("-");
		onBtnLimpar();

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

	private void desabilita() {
		btnLimparTudo.setDisable(true);

		txtPastaOrigem.setDisable(true);
		btnPesquisarPastaOrigem.setDisable(true);
		txtPastaDestino.setDisable(true);
		btnPesquisarPastaDestino.setDisable(true);
		txtNomePastaManga.setDisable(true);
		txtVolume.setDisable(true);

		btnImportar.setDisable(true);
		btnLimpar.setDisable(true);
		btnExcluir.setDisable(true);
		btnInserir.setDisable(true);
		btnImportar.setDisable(true);
		tbViewTabela.setDisable(true);
	}

	private void habilita() {
		btnLimparTudo.setDisable(false);

		txtPastaOrigem.setDisable(false);
		btnPesquisarPastaOrigem.setDisable(false);
		txtPastaDestino.setDisable(false);
		btnPesquisarPastaDestino.setDisable(false);
		txtNomePastaManga.setDisable(false);
		txtVolume.setDisable(false);

		btnImportar.setDisable(false);
		btnLimpar.setDisable(false);
		btnExcluir.setDisable(false);
		btnInserir.setDisable(false);
		btnImportar.setDisable(false);
		tbViewTabela.setDisable(false);

		btnProcessar.accessibleTextProperty().set("PROCESSA");
		btnProcessar.setText("Processar");
		apGlobal.cursorProperty().set(null);
	}

	private Boolean validaCampos() {
		Boolean valida = true;

		if ((caminhoOrigem == null) || (!caminhoOrigem.exists())) {
			txtPastaOrigem.setUnFocusColor(Color.RED);
			valida = false;
		}

		if ((caminhoDestino == null) || (!caminhoDestino.exists())) {
			txtPastaDestino.setUnFocusColor(Color.RED);
			valida = false;
		}

		if (lsVwListaImagens.getSelectionModel().getSelectedItem() == null)
			lsVwListaImagens.getSelectionModel().select(0);

		if (obsLCaminhos.isEmpty())
			valida = false;

		return valida;
	}

	private void criaPasta(String caminho) {
		File arquivo = new File(caminho);
		if (!arquivo.exists())
			arquivo.mkdir();
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
			e.printStackTrace();
		}
		return result;
	}

	private void processar() {
		Task<Boolean> movimentaArquivos = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				try {
					
					if (lsVwListaImagens.getSelectionModel().getSelectedItem() != null)
						selecionada = lsVwListaImagens.getSelectionModel().getSelectedItem();

					int i = 0;
					int max = caminhoOrigem.listFiles(getFilterNameFile()).length;
					boolean verificaPagDupla = cbVerificaPaginaDupla.isSelected();

					updateProgress(i, max);
					updateMessage("Criando diretórios...");

					String nomePasta = caminhoDestino.getPath().trim() + "\\" + txtNomePastaManga.getText().trim() + " "
							+ txtVolume.getText().trim();

					updateMessage("Criando diretórios - " + nomePasta + " Capa\\");
					criaPasta(nomePasta + " Capa\\");

					if (!obsLImagesSelected.isEmpty()) {
						File destinoCapa = new File(nomePasta + " Capa\\");
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
							case PAGINA_DUPLA:
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

					int pagina = 0, proxCapitulo, contadorCapitulo = 0;
					boolean contar = false;

					File destino = new File(nomePasta + " " + lista.get(pagina).getNomePasta() + "\\");
					criaPasta(nomePasta + " " + lista.get(pagina).getNomePasta() + "\\");
					contadorCapitulo = Integer.valueOf(lista.get(pagina).getNumeroPagina());
					pagina++;

					proxCapitulo = Integer.valueOf(lista.get(pagina).getNumeroPagina());

					for (File arquivos : caminhoOrigem.listFiles(getFilterNameFile())) {

						System.out.println("Contar: " + contar + " - Contador: " + contadorCapitulo + " - Prox cap: "
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
							criaPasta(nomePasta + " " + lista.get(pagina).getNomePasta() + "\\");
							destino = new File(nomePasta + " " + lista.get(pagina).getNomePasta() + "\\");
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

					obsLImagesSelected.clear();
				} catch (Exception e) {
					e.printStackTrace();
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

	@FXML
	private void onBtnProcessa() {
		if (validaCampos()) {
			if (btnProcessar.accessibleTextProperty().getValue().equalsIgnoreCase("PROCESSA")) {
				btnProcessar.accessibleTextProperty().set("CANCELA");
				btnProcessar.setText("Cancelar");
				apGlobal.cursorProperty().set(Cursor.WAIT);
				desabilita();
				processar();
			}
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
		if (!txtPastaDestino.getText().trim().isEmpty()) {
			txtSimularPasta.setText(txtPastaDestino.getText().trim() + "\\" + txtNomePastaManga.getText().trim() + " "
					+ txtVolume.getText().trim() + " " + txtNomePastaCapitulo.getText().trim() + " 00");
		}
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
	private void onBtnExcluir() {
		Caminhos selectedItem = tbViewTabela.getSelectionModel().getSelectedItem();
		if (selectedItem != null)
			tbViewTabela.getItems().remove(selectedItem);
	}

	@FXML
	private void onBtnInserir() {
		if ((!txtNumeroCapitulo.getText().isEmpty()) || (!txtNumeroPaginaCapitulo.getText().isEmpty())) {
			lista.add(new Caminhos(txtNumeroCapitulo.getText(), txtNumeroPaginaCapitulo.getText(),
					txtNomePastaCapitulo.getText() + " " + txtNumeroCapitulo.getText()));
			obsLCaminhos = FXCollections.observableArrayList(lista);
			tbViewTabela.setItems(obsLCaminhos);
			tbViewTabela.refresh();
			limpaCampo();
		}
	}

	private void limpaCampo() {
		txtNumeroCapitulo.clear();
		txtNumeroCapitulo.requestFocus();
		txtNumeroPaginaCapitulo.clear();
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
							if (obsLImagesSelected.stream().filter(e -> e.getArquivo().equalsIgnoreCase(item)).findFirst().isPresent())
								obsLImagesSelected.removeIf(capa -> capa.getArquivo().equalsIgnoreCase(item));
							else {
								TipoCapa tipo = TipoCapa.CAPA;
								if (click.isShiftDown())
									tipo = TipoCapa.SUMARIO;
								else if (click.isAltDown())
									tipo = TipoCapa.PAGINA_DUPLA;
	
								obsLImagesSelected.add(new Capa(item, tipo));
							}
						}
					}

				}
			}
		});

		PseudoClass capaSelected = PseudoClass.getPseudoClass("capaSelected");
		PseudoClass capaDualSelected = PseudoClass.getPseudoClass("capaDualSelected");
		PseudoClass sumarioDualSelected = PseudoClass.getPseudoClass("sumarioDualSelected");

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
			InvalidationListener listenerPaginaDupla = obs -> cell.pseudoClassStateChanged(capaDualSelected,
					cell.getItem() != null && contemTipoSelecionado(TipoCapa.PAGINA_DUPLA, cell.getItem()));
			InvalidationListener listenerSumario = obs -> cell.pseudoClassStateChanged(sumarioDualSelected,
					cell.getItem() != null && contemTipoSelecionado(TipoCapa.SUMARIO, cell.getItem()));

			cell.itemProperty().addListener(listenerCapa);
			cell.itemProperty().addListener(listenerPaginaDupla);
			cell.itemProperty().addListener(listenerSumario);

			obsLImagesSelected.addListener(listenerCapa);
			obsLImagesSelected.addListener(listenerPaginaDupla);
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
				e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setNumeroPagina(e.getNewValue()));

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

		txtVolume.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (oldPropertyValue)
					simulaNome();
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
			}
		});

		txtAreaImportar.setOnKeyPressed(e -> {
			if (e.isControlDown() && e.getCode().equals(KeyCode.ENTER))
				onBtnImporta();
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
		// carregarTeste();
	}

}
