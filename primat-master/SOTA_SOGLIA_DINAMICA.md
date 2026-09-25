# Soglia di matching dinamica: stato dell'arte e metodo adottato

Scopo: la Linkage Unit sceglie da sola la soglia di similarità dalla distribuzione delle similarità delle coppie candidate del blocking (`"similarityThreshold": "auto" | "auto_precision" | "auto_recall"`), sposta il punto trovato di un epsilon per premiare precision o recall e dichiara quanto è affidabile. Implementazione: `primat-linkage-unit/.../lu/evaluation/threshold/` (stimatore puro) e `MultiSourceLinkage.classifyAndCluster` (integrazione a due passate). Note operative in `CLAUDE.md` (bullet 2026-09-25) e `TESTING.md`.

## 1. Il problema

Le coppie candidate del blocking hanno una similarità Jaccard che è la mescolanza di due popolazioni: i **non-match** (moda bassa, per RBF densi attorno a 0.4, molto numerosi) e i **match veri** (moda alta, a volte separata da un vuoto, a volte una spalla/altopiano nella coda dei non-match, a volte con un picco di duplicati esatti vicino a 1). La soglia ideale sta al "ginocchio" tra le due. Senza ground truth va stimata da un istogramma che il blocking ha già deformato.

**Effetto del blocking LSH.** Una coppia con Jaccard `s` supera il JaccardLSH con probabilità `P(s) = 1 − (1 − s^r)^b` (`r = keySize`, `b = keys`). L'istogramma osservato è quindi `popolazione × P(s)`: con r=6, b=20 P(0.4) ≈ 1.3 %, P(0.6) ≈ 61 %, P(0.8) ≈ 99.9 %. La moda dei non-match è **troncata** e una parte dei match è **persa** dal blocking. Nessun lavoro noto sfrutta questo fatto per stimare la soglia; qui è il cuore del modello di mistura.

## 2. Stato dell'arte (tre famiglie)

I riferimenti sono citati per autore/anno/sede; quelli con il segno ✓ sono stati riscontrati durante la stesura, per gli altri i dettagli bibliografici vanno ricontrollati prima di citarli in uno scritto formale.

1. **Record linkage probabilistico, mistura di punteggi.** Fellegi & Sunter 1969 e le stime EM di Winkler per m/u senza etichette. ✓ **Belin & Rubin 1995**, *A method for calibrating false-match rates in record linkage*, JASA 90(430), 694–707: i pesi osservati sono una mistura di pesi di match veri e falsi (normali con trasformazione), fittata con EM, da cui si stima il tasso di falsi match **per ogni soglia**. Larsen & Rubin 2001 (EM iterativo), Enamorado, Fifield & Imai 2019 (fastLink). Idea riusata: una mistura fittata restituisce precision/recall attese per ogni soglia senza ground truth.
2. **Soglie su istogrammi (image thresholding).** Otsu 1979 (massima varianza tra classi, equivalente a k-means 1D); ✓ Kittler & Illingworth 1986, *Minimum error thresholding* (mistura di gaussiane; Otsu ne è un caso particolare); Kapur et al. 1985 (entropia); ✓ **Rosin 2001**, *Unimodal thresholding*, Pattern Recognition 34, 2083–2096 (angolo dell'istogramma quando c'è un modo dominante e una coda: è il caso "spalla/altopiano" dei match); Zack et al. 1977 (triangolo). Limite noto: Otsu è sbilanciato quando le classi hanno pesi molto diversi (qui 10³ a 1), quindi non basta da solo.
3. **Distribuzioni dei punteggi in Information Retrieval.** Manmatha et al. 2001 (esponenziale + gaussiana); ✓ **Kanoulas, Pavlu, Dai & Aslam 2009/2010** (ICTIR 2009, SIGIR 2010: rilevanti = mistura di gaussiane, non rilevanti = Gamma, selezione di modello variazionale) per inferire curve precision–recall senza giudizi. Idea riusata: componenti asimmetriche e selezione di modello.
4. **Entity resolution a grafo e PPRL.** ✓ Papadakis, Efthymiou, Thanos & Hassanzadeh, EDBT 2022 (8 algoritmi bipartiti, soglia scelta a griglia **con** ground truth: non applicabile in produzione); Christen 2012 (*Data Matching*) e 2008 (classificazione non supervisionata seed + SVM); Vatsalan/Christen/Verykios, Schnell et al. (CLK), Durham (RBF): in PPRL la soglia è quasi sempre scelta a mano.
5. **Ginocchio e bimodalità.** Kneedle (Satopää et al. 2011), L-method (Salvador & Chan 2004), regressione a due segmenti; dip test (Hartigan & Hartigan 1985), *Ashman's D* (Ashman et al. 1994), bimodality coefficient (Pfister et al. 2013), Silverman 1981.

## 3. Il metodo

**Due passate.** La soglia serve mentre si classifica (`ThresholdClassificator` scarta le coppie sotto soglia e non le conserva), ma la distribuzione è nota solo dopo aver confrontato tutte le coppie. In modalità auto: passata di profilo (soglia 2.0: nessuna coppia classificata, si raccoglie solo l'istogramma a 200 bin, deduplicato per coppia) → stima → classificazione vera con la soglia stimata. Con soglia fissa il flusso è quello di prima.

**Ensemble di stimatori del ginocchio** (`ThresholdEstimator`):

| Stimatore | Idea | Copre |
|---|---|---|
| **mix** | mistura di 1–2 Beta per i non-match e 1–2 per i match, EM sul modello di Poisson `N·f(s)·P(s)`; le coppie scartate dal blocking sono dati mancanti (E-step); struttura (1,1)/(1,2)/(2,1) scelta per BIC; ginocchio = soglia con F1 stimata massima | valle, altopiano (2ª componente dei match), stima di precision/recall/match persi |
| **triangolo** | angolo del log-istogramma smussato tra picco e ultimo bin non vuoto (Rosin/Kneedle) | spalla, unimodale con coda |
| **cambio** | regressione continua a due segmenti sul log-istogramma; serve un cambio di pendenza verso l'alto | valle e spalla |
| **valle** | minimo tra i due picchi dell'istogramma smussato | valle netta |
| **otsu-log** | Otsu sui pesi `ln(1+h)`, attenua il bias verso la classe numerosa | sbilanciamento |
| **vuoto** | tratto di ≥ 4 bin a zero (0.02) con massa a destra ≥ max(30, 10⁻⁴·N) | modi separati |

**Consenso e regime.** Vuoto trovato → `SEPARATI`, ginocchio = punto medio del vuoto. Altrimenti, se il modello è bimodale (guadagno BIC ≥ 10 sul modello unimodale, ≥ 30 match attesi, modi distanti ≥ 0.10 sulle coppie osservate) e descrive bene i dati (distanza di variazione totale oltre il rumore di Poisson ≤ 0.10), **decide il modello**: valle e triangolo possono essere ingannati (con un altopiano uniforme più un picco di duplicati esatti la valle cade fra altopiano e picco) e restano come diagnostica di accordo. Se il modello è scadente decide la mediana degli altri stimatori. Nessuna evidenza di bimodalità → `INDETERMINATO` (ginocchio da triangolo/Otsu-log, affidabilità ≤ 0.25). Regimi: `SEPARATI`, `VALLE`, `ALTOPIANO`, `INDETERMINATO`.

**Epsilon.** `auto` → ginocchio; `auto_precision` → ginocchio + ε; `auto_recall` → ginocchio − ε (ε assoluto in Jaccard, default 0.03, massimo 0.2). Il risultato è limitato inferiormente al picco della moda dei non-match (ε non può portare la soglia dentro il modo sbagliato) e a 1.

## 4. Affidabilità

Numero in [0,1], media geometrica pesata di cinque componenti (una componente vicina a 0 la trascina giù; pavimento 0.02):

| Componente | Peso | Definizione |
|---|---|---|
| separazione | 0.35 | `1 − (1 − F1osservata al ginocchio)/0.2`: quanto sono sovrapposte le due popolazioni (difficoltà/rumore); 1 se c'è un vuoto |
| fit | 0.15 | 60 % distanza di variazione totale del modello oltre il rumore Poisson, 40 % guadagno BIC del modello a due popolazioni |
| accordo | 0.20 | punteggio medio per stimatore `1 − scarto/0.10` dal ginocchio (scarti dentro un vuoto non contano) |
| stabilità | 0.20 | `1 − σ/0.05`, σ del ginocchio su 100 replicati di bootstrap di Poisson dell'istogramma (seme fisso: deterministico) |
| supporto | 0.10 | numero di match attesi e di coppie totali (pochi match, soglia inaffidabile) |

ALTA ≥ 0.75, MEDIA ≥ 0.50, BASSA < 0.50. Con affidabilità BASSA o regime `INDETERMINATO` la LU stampa un WARN ma **applica comunque** la stima (scelta di progetto). Il bootstrap dà anche l'intervallo di confidenza al 95 % del ginocchio. Il modello stima inoltre precision, recall (inclusi i match persi dal blocking) e F1 attese alla soglia applicata.

## 5. Validazione

- Test sintetici (`ThresholdEstimatorTest`): valle sovrapposta, modi separati, altopiano uniforme + picco di duplicati (forma non Beta), rumore forte senza filtro LSH, sbilanciamento 4 000:1 e 133 000:1, solo non-match, campione troppo piccolo, determinismo, ordinamento dell'affidabilità (separati > sovrapposti > nessun match), ε nella direzione richiesta.
- Test end-to-end su RBF sintetici (`MultiSourceLinkageAutoThresholdTest`): la soglia stimata separa i duplicati rumorosi dai non-match e il clustering li ricollega (recall e precision ≥ 0.95).
- Con `debug: true` la LU confronta la stima con la **soglia oracolo** (F1 massima sulla ground truth) e scrive `similarity_threshold.csv`; `ThresholdBenchmark` fa lo stesso su CSV reali e confronta con soglia fissa 0.75, Otsu e valle.

## 6. Limiti dichiarati

- I test sintetici verificano la correttezza, non la **calibrazione** su dati reali: costanti (tolleranze, pesi dell'affidabilità) vanno riverificate con `ThresholdBenchmark` su FEBRL 2/3/4 e NCVR con più (r, b), hardening e rumore, controllando che l'affidabilità cresca quando l'errore `|soglia − oracolo|` diminuisce.
- Un altopiano di match non è distinguibile da una coda pesante di non-match sulla sola distribuzione: il modello usa il vincolo che i non-match siano concentrati (Beta a forma ≥ 1) e la 2ª componente dei match; nei casi ambigui l'accordo tra stimatori abbassa l'affidabilità.
- Con persistenza attiva la stima usa solo le coppie del run (record freschi + cluster storici candidati): la soglia può cambiare tra run. La LU lo segnala con un WARN.
- Costo: in modalità auto la fase di classificazione raddoppia (passata di profilo). Ottimizzazione futura: cache delle similarità sopra un pavimento per riusarle nella passata di classificazione.
- Il modello tratta la mistura sulle sole coppie candidate; i match persi dal blocking sono stimati, non osservati.
