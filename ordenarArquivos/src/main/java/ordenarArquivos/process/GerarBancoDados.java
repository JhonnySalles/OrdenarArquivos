package ordenarArquivos.process;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ordenarArquivos.model.Caminhos;
import ordenarArquivos.model.Manga;
import ordenarArquivos.service.MangaServices;

public class GerarBancoDados {

	private static final Logger LOG = LoggerFactory.getLogger(GerarBancoDados.class);

	private static MangaServices SERVICE;

	private static final String CAPA = "[\\d]+ capa$";
	private static final String VOLUME = "((?i)\\bvolume\\b [\\d.]+)";
	private static final String CAPITULO = "((?i)\\bcapítulo\\b [\\d.]+)";
	private static final String EXTRA = "((?i)\\bextra\\b [\\d.]+)";
	
	private static final String ARQUIVO_SUFIX = " (Jap)";

	public static void processar(String diretorio) {
		if (diretorio == null || diretorio.isEmpty()) {
			LOG.info("Necessário informar um caminho para processar os arquivos.");
			return;
		}

		File origem = new File(diretorio);

		if (!origem.exists()) {
			LOG.error("Caminho não localizado.");
			return;
		}

		if (origem.isFile())
			origem = new File(origem.getPath());

		SERVICE = new MangaServices();

		Manga manga = new Manga(null, "", "", "", "", 0, "");

		String nome = "";
		String volume = "";
		String capitulo = "";
		String arquivo = "";
		Integer quantidade = 1;
		String capitulos;

		for (File pasta : origem.listFiles()) {
			String processar = pasta.getName();
			
			Matcher matcher = Pattern.compile(CAPA, Pattern.CASE_INSENSITIVE).matcher(processar);

			if (pasta.isFile() || matcher.find())
				continue;
			
			LOG.info("Processando pasta: " + processar);
			
			matcher = Pattern.compile(VOLUME, Pattern.CASE_INSENSITIVE).matcher(processar);
			if (matcher.find())
				volume = matcher.group();
			else
				volume = processar.split(processar.replaceAll(VOLUME, ""))[1];
			
			matcher = Pattern.compile(EXTRA, Pattern.CASE_INSENSITIVE).matcher(processar);
						
			if (matcher.find())
				capitulo = matcher.group();
			else {
				matcher = Pattern.compile(CAPITULO, Pattern.CASE_INSENSITIVE).matcher(processar);
				if (matcher.find())
					capitulo = matcher.group();
				else
					capitulo = processar.split(processar.replaceAll(CAPITULO, ""))[1];
			}
			
			if (processar.contains("]"))
				nome = processar.substring(processar.indexOf("]")).replace("]", "");
			else
				nome = processar;

			nome = nome.replace(volume, "").replace(capitulo, "").trim();

			if (nome.endsWith("-"))
				nome = nome.substring(0, nome.length() - 1).trim();

			manga.addCaminhos(new Caminhos(null, manga, capitulo.replaceAll("[\\D]", "").trim(), quantidade, capitulo));
			quantidade += pasta.listFiles().length;

			if (!nome.equalsIgnoreCase(manga.getNome()) || !volume.equalsIgnoreCase(manga.getVolume())) {
				capitulos = "";

				for (Caminhos caminho : manga.getCaminhos())
					if (caminho.getNumero().compareTo(1) == 0)
						capitulos += caminho.getCapitulo() + "-" + "\n";
					else
						capitulos += caminho.getCapitulo() + "-" + caminho.getNumero() + "\n";

				if (!capitulos.isEmpty())
					capitulos += capitulos.substring(0, capitulos.length() - 1);

				manga.setCapitulos(capitulos);
				manga.setArquivo(manga.getNome() + " " + manga.getVolume() + ARQUIVO_SUFIX + ".cbr");
				
				LOG.info("Salvando manga: " + manga.getNome() + " - " + manga.getVolume());

				if (!manga.getNome().isEmpty())
					SERVICE.save(manga);
				
				manga = new Manga(null, nome, volume, capitulo, arquivo, quantidade, "");

				arquivo = "";
				quantidade = 1;
			}
		}
		
		if (!manga.getNome().isEmpty()) {
			capitulos = "";

			for (Caminhos caminho : manga.getCaminhos())
				if (caminho.getNumero().compareTo(1) == 0)
					capitulos += caminho.getCapitulo() + "-" + "\n";
				else
					capitulos += caminho.getCapitulo() + "-" + caminho.getNumero() + "\n";

			if (!capitulos.isEmpty())
				capitulos += capitulos.substring(0, capitulos.length() - 1);

			manga.setCapitulos(capitulos);
			manga.setArquivo(manga.getNome() + " " + manga.getVolume() + ARQUIVO_SUFIX + ".cbr");
			
			LOG.info("Salvando manga: " + manga.getNome() + " - " + manga.getVolume());

			if (!manga.getNome().isEmpty())
				SERVICE.save(manga);
		}
		
		LOG.info("Fim do processamento.");

		SERVICE.closeConnection();
	}
}
