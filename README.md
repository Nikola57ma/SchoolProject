# Domovi za Ostarele - Sistem za upravljanje

## Opis projekta

Projekt "Domovi za Ostarele" omogoča upravljanje podatkov o domovih za ostarele, vključno z njihovimi ocenami, storitvami in komentarji uporabnikov. Aplikacija omogoča dve glavni vloge uporabnikov:
- **Uporabniki**, ki si lahko ogledajo seznam domov za ostarele, oddajo ocene in pustijo komentarje.
- **Admini**, ki lahko dodajajo nove domove, brišejo obstoječe domove, in upravljajo storitve, ki jih ponujajo posamezni domovi.

## Funkcionalnosti

### 1. **Prijava in registracija**
- Uporabniki se lahko prijavijo v sistem z uporabo uporabniškega imena in gesla, ki je shranjeno z uporabo varnega bcrypt algoritma.
- Uporabniki se lahko registrirajo v sistem s prijavo novih uporabnikov.

### 2. **Pregled in ocene domov za ostarele**
- Uporabniki lahko pregledajo seznam domov za ostarele, ki vključuje ime doma, število sob, ter kraj.
- Uporabniki lahko oddajo ocene za domove, ob tem pa napišejo tudi komentar.

### 3. **Admin funkcionalnosti**
- Admini imajo možnost dodajanja novih domov za ostarele.
- Admini lahko urejajo obstoječe domove, dodajajo ali brišejo storitve, ki jih ponujajo posamezni domovi.
- Admini lahko brišejo domove iz sistema, pri tem pa se izbrišejo tudi povezane storitve.

### 4. **Storitev za domove**
- Ob vsakem domu lahko admin dodeli storitve, kot so "oskrba", "prevoz", "domača pomoč" ipd.
- Uporabniki lahko pregledajo storitve, ki jih dom ponuja, in komentirajo oziroma ocenijo te storitve.

## Tehnologije

- **Jezik**: Java
- **Baza podatkov**: MySQL
- **Za uporabniški vmesnik**: Swing
- **Varnost**: BCrypt za shranjevanje gesel uporabnikov

## Namestitev

### 1. Namestitev baze podatkov
Za uporabo sistema moraš imeti nastavljen MySQL strežnik. Ustvari bazo podatkov z naslednjo strukturo:

```sql
CREATE DATABASE domovi_za_ostarele;

USE domovi_za_ostarele;

CREATE TABLE uporabniki (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    pass VARCHAR(255) NOT NULL,
    isadmin BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE kraji (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ime VARCHAR(100) NOT NULL
);

CREATE TABLE domovi (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ime VARCHAR(255) NOT NULL,
    opis TEXT,
    email VARCHAR(255),
    telefon VARCHAR(50),
    st_sob INT,
    naslov VARCHAR(255),
    kraj_id INT,
    FOREIGN KEY (kraj_id) REFERENCES kraji(id)
);

CREATE TABLE ocene (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dom_id INT,
    uporabnik_id INT,
    ocena INT NOT NULL,
    komentar TEXT,
    FOREIGN KEY (dom_id) REFERENCES domovi(id),
    FOREIGN KEY (uporabnik_id) REFERENCES uporabniki(id)
);

CREATE TABLE storitve (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ime VARCHAR(100) NOT NULL
);

CREATE TABLE domovi_storitve (
    domovi_id INT,
    storitve_id INT,
    PRIMARY KEY (domovi_id, storitve_id),
    FOREIGN KEY (domovi_id) REFERENCES domovi(id),
    FOREIGN KEY (storitve_id) REFERENCES storitve(id)
);
