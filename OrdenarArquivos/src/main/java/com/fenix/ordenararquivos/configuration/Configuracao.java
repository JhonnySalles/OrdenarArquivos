package com.fenix.ordenararquivos.configuration;

import java.io.*;
import java.util.Properties;

public class Configuracao {
	
	private static String ARQUIVO = "app.properties";

	public static void createProperties(String winrar) {
		try (OutputStream os = new FileOutputStream(ARQUIVO)) {
			Properties props = new Properties();
			props.setProperty("caminho_winrar", winrar);
			props.store(os, "");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Properties loadProperties() {
		File f = new File(ARQUIVO);
		if (!f.exists())
			createProperties("");

		try (FileInputStream fs = new FileInputStream(ARQUIVO)) {
			Properties props = new Properties();
			props.load(fs);
			return props;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
