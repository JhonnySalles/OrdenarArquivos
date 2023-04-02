package com.fenix.ordenararquivos;

import com.fenix.ordenararquivos.process.GerarBancoDados;

public class App {

	public static void main(String[] args) {
		String caminho = "";
		boolean gerarBanco = false;
		
		for (String a : args)
			if (a.contains("gerarBanco")) {
				gerarBanco = true;
				caminho = a.substring(a.indexOf("=") +1).replace("\"", "");
			}
				
		if (gerarBanco)
			GerarBancoDados.processar(caminho);
		else		
			Run.main(args);
	}
}
