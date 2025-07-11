alter table Caminho add column tag TEXT;
update Caminho set tag = '' where tag is null;