create table Manga (
    id INTEGER primary key AUTOINCREMENT,
    nome TEXT not null,
    volume TEXT,
    capitulo TEXT,
    arquivo TEXT,
    capitulos TEXT,
    quantidade INTEGER,
    criacao TEXT,
    atualizacao TEXT
);

create table Caminho (
    id INTEGER primary key AUTOINCREMENT,
    id_manga INTEGER not null,
    capitulo TEXT,
    pagina INTEGER,
    pasta TEXT
);

