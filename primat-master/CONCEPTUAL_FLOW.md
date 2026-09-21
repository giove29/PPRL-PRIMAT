# Il flusso della pipeline, a parole (vista concettuale)

`ARCHITECTURE_FLOW.md` descrive *chi chiama chi* in termini di classi e metodi Java. Questo documento descrive lo stesso flusso a un livello più alto: **cosa succede e perché**, senza nominare classi o metodi — utile per capire la logica del sistema prima ancora di aprire il codice.

## Il problema

Più soggetti (i "party") possiedono ciascuno un proprio elenco di persone (pazienti, cittadini, clienti...). Si vuole scoprire quali righe, sparse tra questi elenchi, si riferiscono alla stessa persona reale — senza che nessuno dei soggetti debba rivelare agli altri i propri dati in chiaro. È un problema di *collegamento di record* (record linkage) sotto un vincolo di *privacy*: la somiglianza tra due record deve poter essere valutata senza che il valore effettivo degli attributi (nome, cognome, città...) venga mai esposto a chi fa il confronto.

## Gli attori

- **Data Owner** — uno per ogni sorgente di dati. Ha in locale i dati in chiaro e non li condivide mai con nessuno: tutto quello che produce e invia all'esterno è già offuscato.
- **Linkage Unit** — l'unità che coordina l'intero processo e calcola i collegamenti. Non vede mai un dato in chiaro: riceve solo impronte offuscate e, al termine, sa soltanto "quali impronte sembrano appartenere alla stessa persona", non "chi sono quelle persone".

Il sistema è pensato per girare in continuo: i Data Owner restano attivi e rispondono a più "round" successivi (uno stesso processo può partecipare a molti collegamenti nel tempo, non solo a uno).

## Le fasi di un round

**1. Preparazione locale (dal lato di ogni Data Owner, in parallelo e indipendentemente).**
Ogni Data Owner pulisce e normalizza i propri dati (stessa maiuscola/minuscola, stessi accenti, stessa forma), poi trasforma ogni record in un'**impronta digitale offuscata**: una struttura compatta che conserva la capacità di misurare "quanto due record si somigliano" ma da cui non è possibile risalire ai valori originali degli attributi. Da questo momento in poi, per quel record, l'unico dato che esisterà fuori dalla macchina del Data Owner è questa impronta.

**2. Avvio e raccolta di un round (coordinamento).**
La Linkage Unit annuncia l'inizio di un round e chiede a tutti i Data Owner attesi di inviare le impronte correnti. Non ipotizza un ordine di avvio preciso: se qualcuno non ha ancora ricevuto la richiesta, la ripete finché tutti non hanno risposto o scade un tempo massimo. Ogni round ha un'identità propria, cosi' round diversi non si mescolano anche se usano gli stessi processi.

**3. Riduzione dello spazio di ricerca (blocking).**
Confrontare ogni impronta con ogni altra impronta di ogni altro Data Owner crescerebbe troppo in fretta al crescere dei dati. Per evitarlo, le impronte vengono raggruppate in "bucket" tramite una tecnica di hashing pensata apposta per la privacy e per la similarità: due impronte simili finiscono quasi sempre nello stesso bucket, due impronte molto diverse quasi mai. Solo le coppie che condividono almeno un bucket vengono davvero confrontate nel passo successivo — il resto viene scartato a priori, senza calcolarne la similarità.

**4. Decisione di corrispondenza (classificazione).**
Per ogni coppia di impronte sopravvissuta al blocking si calcola un punteggio numerico di somiglianza. Una soglia trasforma quel punteggio in una decisione binaria: "stessa persona" oppure "persone diverse". Più la soglia è alta, meno falsi collegamenti vengono creati, ma più veri collegamenti rischiano di essere persi (e viceversa) — è un compromesso deliberato tra precisione e copertura.

**5. Disambiguazione multi-sorgente (clustering).**
Le decisioni del passo precedente sono prese coppia per coppia e possono essere in conflitto tra loro: un record può risultare "collegato" a più record diversi della stessa altra sorgente, il che non ha senso se ogni persona compare una sola volta per sorgente (per le sorgenti che si sa già essere "pulite", senza duplicati interni). Serve quindi una regola per raggruppare, tra tutte le sorgenti coinvolte in un colpo solo, i collegamenti in entità coerenti. Si usa **una sola** tra due strategie alternative, scelta automaticamente in base alla composizione delle sorgenti di quel round: se almeno una sorgente è nota per essere pulita, si usa una strategia che rispetta esplicitamente questo vincolo (nessuna entità può contenere due record della stessa sorgente pulita); se tutte le sorgenti possono contenere duplicati interni, si usa un'altra strategia, basata sulla propagazione di un "flusso" di similarità attraverso la rete di collegamenti, che non ha bisogno di quel vincolo. Le due strategie non vengono mai eseguite insieme sullo stesso round: la scelta è univoca e dipende solo da quali sorgenti sono coinvolte.

**6. Consolidamento multi-sorgente (chiusura transitiva).**
I collegamenti accettati tra ogni coppia di sorgenti vengono messi insieme, e si applica una regola di buon senso: se A è collegato a B, e B è collegato a C, allora A, B e C sono la stessa entità, anche se A e C non erano mai stati confrontati direttamente (magari perché il blocking non li aveva messi nello stesso bucket). Il risultato di questo passo è un insieme di "entità": gruppi di impronte, provenienti anche da sorgenti diverse, che si ritiene rappresentino la stessa persona — comprese le impronte che non hanno trovato nessun collegamento, che restano entità di un solo elemento.

**7. Memoria tra round (solo per la strategia che rispetta il vincolo "sorgente pulita").**
Rifare tutto da zero ad ogni round butterebbe via il lavoro fatto nei round precedenti, e ogni run assegnerebbe alle entità identificativi nuovi, non confrontabili con quelli di prima. Per questa strategia, invece, il sistema tiene un archivio permanente di "quali impronte appartengono già a quale entità". Ad ogni nuovo round, lo storico viene ricaricato e unito al nuovo lotto di impronte ricevute prima di rifare blocking, classificazione e disambiguazione sull'insieme completo; il risultato viene poi riconciliato con l'archivio seguendo una regola precisa: un'entità già nota può solo **crescere** (accogliere nuove impronte mai viste) o **fondersi** con un'altra entità già nota (se nuove informazioni mostrano che in realtà erano la stessa persona), ma non viene mai smontata o divisa silenziosamente da un round successivo. Cosi' l'identità di un'entità resta stabile nel tempo: la si può ritrovare, riferire ed estendere round dopo round, invece di doverla ricostruire ogni volta.

Questo funziona per due motivi distinti: un record già visto in passato si riconosce dal suo identificativo in chiaro (banale confronto di uguaglianza); un record **nuovo** che rappresenta un'entità già nota si riconosce invece per somiglianza della sua impronta con quella storica — possibile solo perché la trasformazione in impronta è *deterministica* (stesso dato in chiaro → stessa impronta, sempre), non rigenerata a caso ad ogni round.

**8. Misura della qualità (solo in un contesto sperimentale/di laboratorio).**
Quando si lavora su dati sintetici che portano già un'etichetta con la "verità" (a quale persona reale appartiene ogni riga — informazione che nella pipeline reale non esiste e non viene mai trasmessa), si può confrontare il risultato del collegamento con questa verità nota: quanti collegamenti corretti sono stati trovati rispetto a quelli attesi, e quanti collegamenti sbagliati sono stati introdotti. Questo è ciò che permette di valutare oggettivamente, offline, quanto bene ha funzionato la strategia di disambiguazione usata in un round — e, confrontando round diversi, anche le due strategie del passo 5 tra loro.

## Le garanzie di fondo

- **Il dato in chiaro non esce mai dal Data Owner.** Solo le impronte offuscate viaggiano sulla rete e vengono viste dalla Linkage Unit.
- **Ogni round è ripetibile e indipendente.** Un round che fallisce (per timeout, per un Data Owner irraggiungibile) non corrompe lo stato: si può ripetere.
- **Blocking e classificazione sono condivisi, la scelta della strategia di disambiguazione no**: vengono fatti una sola volta per round sullo stesso dato grezzo, poi si applica l'unica strategia scelta in base alla composizione delle sorgenti (passo 5) — non un confronto tra alternative eseguite in parallelo.
- **La memoria tra round (fase 7) è additiva per costruzione**: la regola "un'entità può solo crescere o fondersi, mai dividersi" è una scelta di design esplicita, non un dettaglio implementativo — è ciò che rende affidabile riferirsi a un'entità con lo stesso identificativo nel tempo.
