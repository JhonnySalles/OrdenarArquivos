package ordenarArquivos.controller;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.DirectoryChooser;
import ordenarArquivos.model.Caminhos;

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

		btnLimpar.setDisable(true);
		btnExcluir.setDisable(true);
		btnInserir.setDisable(true);
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

		btnLimpar.setDisable(false);
		btnExcluir.setDisable(false);
		btnInserir.setDisable(false);
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

		return valida;
	}

	private void criaPasta(String caminho) {
		File arquivo = new File(caminho);
		if (!arquivo.exists())
			arquivo.mkdir();
	}

	private void copiaItem(File arquivo, File destino) throws IOException {
		Path arquivoDestino = Paths.get(destino.toPath() + "/" + arquivo.getName());
		Files.copy(arquivo.toPath(), arquivoDestino, StandardCopyOption.COPY_ATTRIBUTES,
				StandardCopyOption.REPLACE_EXISTING);
	}

	private void processar() {
		Task<Boolean> movimentaArquivos = new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				try {
					selecionada = lsVwListaImagens.getSelectionModel().getSelectedItem();

					int i = 0;
					int max = caminhoOrigem.listFiles(getFilterNameFile()).length;

					updateProgress(i, max);
					updateMessage("Criando diretórios...");

					String nomePasta = caminhoDestino.getPath().trim() + "\\" + txtNomePastaManga.getText().trim() + " "
							+ txtVolume.getText().trim();

					updateMessage("Criando diretórios - " + nomePasta + " Capa\\");
					criaPasta(nomePasta + " Capa\\");

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
			txtPastaOrigem.setText("");

		simulaNome();
	}

	private void carregaPastaDestino() {
		caminhoDestino = new File(txtPastaDestino.getText());
		simulaNome();
	}

	private void listaItens() {
		if ((caminhoOrigem != null) && (caminhoOrigem.list() != null)) {

			obsLListaItens = FXCollections.<String>observableArrayList(caminhoOrigem.list(getFilterNameFile()));
		} else
			obsLListaItens = FXCollections.<String>observableArrayList("");
		lsVwListaImagens.setItems(obsLListaItens);
	}

	private void simulaNome() {
		if (!txtPastaDestino.getText().trim().isEmpty()) {
			txtSimularPasta.setText(txtPastaDestino.getText().trim() + "\\" + txtNomePastaManga.getText().trim()
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
				String texto = txtAreaImportar.getText();
				if (!texto.isEmpty())
					texto += "\r\n";
				
				for (Integer i=inicio;i<=fim;i++)
					texto += i.toString() + "-" + (i<fim? "\r\n":"");

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

	private void configuraTextEdit() {

		txtPastaOrigem.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (oldPropertyValue)
					carregaPastaOrigem();

				txtPastaOrigem.setUnFocusColor(Color.GRAY);
			}
		});

		txtPastaOrigem.setOnKeyPressed(e -> {
			if (e.getCode().toString().equals("ENTER"))
				clickTab();
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
			if (e.getCode().toString().equals("ENTER"))
				clickTab();
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
			if (e.getCode().toString().equals("ENTER"))
				clickTab();
		});

		txtVolume.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue,
					Boolean newPropertyValue) {
				if (newPropertyValue)
					simulaNome();
			}
		});

		txtVolume.setOnKeyPressed(e -> {
			if (e.getCode().toString().equals("ENTER"))
				clickTab();
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
			if (e.getCode().toString().equals("ENTER"))
				clickTab();
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
