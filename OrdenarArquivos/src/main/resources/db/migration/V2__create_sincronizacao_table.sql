create table Sincronizacao (
    id INTEGER primary key AUTOINCREMENT,
    conexao TEXT,
    envio TEXT,
    recebimento TEXT
);

insert into Sincronizacao (conexao, envio, recebimento) VALUES ('FIREBASE','2024-04-20T01:00:00.100000','2024-04-20T01:00:00.100000');
