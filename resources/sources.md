# Data exploitation notes

## Entities

+ drug
+ disease | 

## Data sources


### br08303.keg

tree

drug(atc-code) -> (name: string, dg-code (sometimes): string)

### chemical.sources.v5.0.tsv

drug(compound-id-m, compound-id-s (alias ?)) -> (atc-code: string)

### drugbank.xml

drug(drugbank-id: string*, cas-number: string, unii: string, name: string, atc-codes: Tree<string>, ahfs-codes: string*) -> (indication: string, toxicity: string, synonyms: string (name))

### hp.obo

symptom/disease(hp-id, name, umls-id, snomedct_us-id, msh-id, meddra-id) -> (synonym: string)

### hpo_annotations.csv

disease_label: name
sign_id : HP[0-9]{7} primary key (hp.obo)
col_7,8,9 : MIM, OMIM, PMID
col_12 : synonym

### meddra_all_indications.csv

drug(compound-id) <-> symptom/disease(cui1, cui2, name)

### meddra_all_se.csv

drug(compound-id-1, compound-id-2) <-> symptom/disease(cui1, cui2, name)

### meddra_freq.csv
compliqué de placer freq dans le diagramme

drug(compound-id-1, compound-id-2) <-> symptom/disease(cui1, cui2, name, freq)

### meddra.csv

symptom/disease(cui: string, meddra-id: string, name: string)

### omim_onto.csv

symtom/disease(omim-id, name, cui, synonym)

### omim.txt

disease(name (*FIELD* TI)) <-> symptom(name (*FIELD* CS))

### orphadata.json

disease(orpha-id-1, orpha-id-2, name) <-> symptom(orpha-id, name)


### OrphaData (JSON)

+ disease -> symptom*

Implements: DiseaseFinder

### OMIM (TXT & CSV)

+ disease -> disease*
+ disease -> symptom*
MySql)

+ drug -> (disease | symptom)* (cure)
+ drug -> symptom* (side effects)

Implements: CureFinder, SideEffectFinder

### HPO (OBO and SQLite)

+ symptom -> symptom*

Implements: SynonymFinder

### HPO Annotations (OBO & SQLite)

+ symptom* -> disease*

Implements: DiseaseFinder

### Stitch & ATC

Link database

## Interfaces

### DiseaseFinder & DiseaseFinderAggregator

symptom* -> disease* (weighted)

### SideEffectFinder & SideEffectFinderAggregator

symptom* -> drug* (weighted)

### CureFinder & CureFinderAggregator

(symtom | disease)* -> drug* (weighted)

### SynonymFinder

symptom -> symptom*

## Access

### Source 1 : Drugbank
**filename** : drugbank_all_full_database.xml.zip
scp adam115u@neptune.telecomnancy.univ-lorraine.fr:/home/depot/2A/gmd/drugbank/drugbank_all_full_database.xml.zip /

### Source 3 : Omim
**filename** : omim.txt
scp adam115u@neptune.telecomnancy.univ-lorraine.fr:/home/depot/2A/gmd/projet_2017-18/omim/omim.txt /home/tim/Bureau/MasseDeDonnee/
filename : omim_onto.csv
scp adam115u@neptune.telecomnancy.univ-lorraine.fr:/home/depot/2A/gmd/projet_2017-18/omim/omim_onto.csv /home/tim/Bureau/MasseDeDonnee/

### Source 5.1 : HPO
**filename** : hp.obo
scp adam115u@neptune.telecomnancy.univ-lorraine.fr:/home/depot/2A/gmd/projet_2017-18/hpo/hp.obo /home/tim/Bureau/MasseDeDonnee/

### Source 5.2 : HPO Annotations
**filename** : hpo_annotations.sqlite
scp adam115u@neptune.telecomnancy.univ-lorraine.fr:/home/depot/2A/gmd/projet_2017-18/hpo/hpo_annotations.sqlite /home/tim/Bureau/MasseDeDonnee/


### Source 6.1 : ATC
**filename** : br08303.keg
scp adam115u@neptune.telecomnancy.univ-lorraine.fr:/home/depot/2A/gmd/projet_2017-18/atc/br08303.keg /home/tim/Bureau/MasseDeDonnee/

### Source 6.2 : STITCH
**filename** : chemical.sources.v5.0.tsv
scp adam115u@neptune.telecomnancy.univ-lorraine.fr:/home/depot/2A/gmd/projet_2017-18/stitch/chemical.sources.v5.0.tsv /home/tim/Bureau/MasseDeDonnee/
