#!/usr/bin/env python3
"""SMU (Schema Mapping Unit) - modulo Python, funzionalita' "Configura i DO".

Possiede uno schema per ciascun Data Owner (config/do_<party>.json: il
mapping colonna->QID, index/name/role, con la catena di preprocessing
esplicita per ogni colonna QID - le trasformazioni che allineano lo schema
locale a quello comune) e un encoding comune a tutti (config/encoding.json:
bloomFilter, missingValueHandling, hmacKey, parametri per-colonna QID).
Alla funzionalita' "Configura i DO" fonde, per ciascun
party, i due file in un unico payload e lo pubblica in un solo passaggio MQTT
su "primat/do/{party}/config" (vedi MqttTopics.java lato Java) - non ci sono
due fasi separate: schema ed encoding viaggiano insieme.

La SMU decide e spinge unilateralmente: l'ack di ciascun DO
("primat/smu/{party}/ack") e' solo conferma tecnica che il file locale e'
stato riscritto/ricaricato con successo, mai un consenso.

Vedi SOTA_RICALCOLO_CLUSTER_INCREMENTALE.md e CLAUDE.md per il contesto di
design completo.
"""

import base64
import hashlib
import hmac
import json
import os
import threading
import time

import paho.mqtt.client as mqtt

CONFIG_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "config")
STATE_PATH = os.path.join(CONFIG_DIR, "state.json")
ENCODING_PATH = os.path.join(CONFIG_DIR, "encoding.json")

MQTT_HOST = "localhost"
MQTT_PORT = 1883
ACK_TIMEOUT_SECONDS = 15

# Stessa chiave hardcoded di DeterministicHashing.DEFAULT_KEY (Java,
# primat-common/.../utils/DeterministicHashing.java) - usata SOLO per il
# digest di verifica di configurazione (configHash), non per l'hashing RBF
# vero e proprio (quello usa la chiave in 'hmacKey', configurabile).
DIGEST_KEY = b"PRIMAT_DETERMINISTIC"

DEFAULT_HASH_FUNCTIONS = 10
DEFAULT_MISSING_VALUE_TOKEN_COUNT = 7
DEFAULT_BLIP_SEED = 42


# --------------------------------------------------------------------------
# Topic MQTT (mirror di MqttTopics.java: primat/do/{party}/config,
# primat/smu/{party}/ack)
# --------------------------------------------------------------------------

def config_topic(party):
    return "primat/do/" + party + "/config"


def config_ack_topic(party):
    return "primat/smu/" + party + "/ack"


def config_ack_topic_wildcard():
    return "primat/smu/+/ack"


# --------------------------------------------------------------------------
# Config store (JSON locali, come da requisito "istanziabili e configurabili
# per adesso con il solito JSON")
# --------------------------------------------------------------------------

def _load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def _do_schema_path(party):
    return os.path.join(CONFIG_DIR, "do_" + party + ".json")


def discovered_parties():
    """Un DO per ciascun file config/do_<party>.json presente."""
    parties = []
    for filename in sorted(os.listdir(CONFIG_DIR)):
        if filename.startswith("do_") and filename.endswith(".json"):
            parties.append(filename[len("do_"):-len(".json")])
    return parties


def next_version():
    """Incrementa e persiste la versione, sempre, anche se poi la push fallisce
    (cosi' un retry usa sempre una versione fresca, mai ambigua)."""
    state = {"version": 0}
    if os.path.exists(STATE_PATH):
        state = _load_json(STATE_PATH)
    state["version"] = state.get("version", 0) + 1
    with open(STATE_PATH, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=2)
    return str(state["version"])


def build_config_push(party):
    """Fonde do_<party>.json (mapping + preprocessing, per-DO) + encoding.json
    (comune) in un 'columns' unico gia' risolto: per ogni colonna QID la
    catena 'preprocessing' viene dallo schema del DO (allinea il suo schema
    locale a quello comune), i parametri di encoding
    (hashFunctions/salt/constantWeightEncoding/missingValueTokenCount) sono
    presi da encoding.json['columns'][nome]; le colonne non-QID (PARTY/ID/
    GLOBAL_ID) restano quelle dichiarate dal DO. Ritorna (config_fragment,
    resolved_qid_columns) - il secondo serve solo per il calcolo del digest.
    """
    schema = _load_json(_do_schema_path(party))
    encoding = _load_json(ENCODING_PATH)
    encoding_columns = encoding.get("columns", {})

    merged_columns = []
    resolved_qid_columns = []
    for column in schema["columns"]:
        merged = dict(column)
        if column.get("role") == "QID":
            if not column.get("preprocessing"):
                raise ValueError(
                    "do_" + party + ".json non dichiara 'preprocessing' per la colonna QID '"
                    + column["name"] + "'")
            params = encoding_columns.get(column["name"])
            if params is None:
                raise ValueError(
                    "encoding.json non contiene parametri per la colonna QID '" + column["name"]
                    + "' richiesta da do_" + party + ".json")
            merged["hashFunctions"] = params.get("hashFunctions")
            merged["salt"] = params.get("salt")
            merged["constantWeightEncoding"] = params.get("constantWeightEncoding")
            merged["missingValueTokenCount"] = params.get("missingValueTokenCount")
            resolved_qid_columns.append(merged)
        merged_columns.append(merged)

    config_fragment = {
        "columns": merged_columns,
        "bloomFilter": encoding["bloomFilter"],
        "missingValueHandling": encoding.get("missingValueHandling", {"enabled": False, "anchorPriority": []}),
    }
    if encoding.get("hmacKey"):
        config_fragment["hmacKey"] = encoding["hmacKey"]
    return config_fragment, resolved_qid_columns


# --------------------------------------------------------------------------
# Digest: porting esatto di DataOwnerConfig.computeConfigHash() (Java,
# primat-data-owner-service/.../DataOwnerConfig.java) - deve produrre lo
# stesso valore Base64 calcolato lato Data Owner sulla stessa configurazione.
# --------------------------------------------------------------------------

def _describe_hardening(step):
    t = step.get("type")
    if t == "BLIP":
        seed = step.get("seed", DEFAULT_BLIP_SEED)
        return "BLIP(probability=" + _java_number(step["probability"]) + ", seed=" + str(seed) + ")"
    if t == "XOR_FOLD":
        return "XOR_FOLD(foldCount=" + str(step["foldCount"]) + ")"
    return str(t)


def _java_number(value):
    """Approssima StringBuilder.append(double)/(int) di Java: interi senza
    decimali, altrimenti la rappresentazione Python (coincide con quella Java
    per i valori 'semplici' usati nelle config, es. 0.1) - non e' garantita
    byte-identica per valori double esotici, che qui non si presentano."""
    if isinstance(value, float) and value.is_integer():
        return str(value)
    return str(value)


def _hardening_descriptions(bloom_filter):
    chain = bloom_filter.get("hardeningChain") or []
    if chain:
        return [_describe_hardening(step) for step in chain]
    single = bloom_filter.get("hardening")
    if single and single.get("type") not in (None, "NONE"):
        return [_describe_hardening(single)]
    return []


def _java_bool(value):
    return "true" if value else "false"


def _java_list(items):
    return "[" + ", ".join(items) + "]"


def _describe_merger(merger):
    if merger.get("type") == "SIMPLE":
        return merger["type"] + "(separator=" + merger["separator"] + ")"
    return merger["type"]


def _describe_splitter(splitter):
    if splitter.get("type") == "POSITION":
        return splitter["type"] + "(position=" + str(splitter["position"]) + ")"
    if splitter.get("type") == "REGEX":
        return splitter["type"] + "(pattern=" + splitter["pattern"] + ")"
    return splitter["type"]


def _describe_preprocessing_step(step):
    """Mirror esatto di PreprocessingStepFactory.describeStep() (Java) - TRIM/
    UPPERCASE/... a singola colonna, ma anche il primo step MERGE/SPLIT di una
    colonna QID virtuale (calcola il suo valore da colonne RAW)."""
    if step.get("type") == "TRUNCATE":
        return "TRUNCATE(" + str(step["from"]) + "," + str(step["to"]) + ")"
    if step.get("type") == "MERGE":
        return ("MERGE(sources=" + _java_list(step["sources"]) + ",merger=" + _describe_merger(step["merger"]) + ")")
    if step.get("type") == "SPLIT":
        return ("SPLIT(source=" + step["source"] + ",splitter=" + _describe_splitter(step["splitter"])
                + ",parts=" + str(step["parts"]) + ",part=" + str(step["part"]) + ")")
    return step["type"]


def _describe_preprocessing(steps):
    """Mirror esatto di PreprocessingStepFactory.describe() (Java)."""
    return ">".join(_describe_preprocessing_step(step) for step in steps)


def compute_config_hash(bloom_filter, missing_value_handling, qid_columns):
    parts = []
    parts.append("rbfLength=" + str(bloom_filter["length"]))
    parts.append(";hardening=" + ">".join(_hardening_descriptions(bloom_filter)))
    mvh_enabled = bool(missing_value_handling and missing_value_handling.get("enabled"))
    parts.append(";missingValueHandling=" + _java_bool(mvh_enabled))
    if mvh_enabled:
        parts.append("(anchorPriority=" + _java_list(missing_value_handling.get("anchorPriority", [])) + ")")

    for column in qid_columns:
        hash_functions = column.get("hashFunctions")
        if hash_functions is None:
            hash_functions = DEFAULT_HASH_FUNCTIONS
        salt = column.get("salt")
        if not salt:
            salt = column["name"] + "_"
        cwe = column.get("constantWeightEncoding")
        cwe_enabled = bool(cwe and cwe.get("enabled"))
        if cwe_enabled:
            cwe_str = "On(minTrigrams=" + str(cwe["minTrigrams"]) + ",maxTrigrams=" + str(cwe["maxTrigrams"]) + ")"
        else:
            cwe_str = "Off"
        missing_token_count = column.get("missingValueTokenCount")
        if missing_token_count is None:
            missing_token_count = cwe["minTrigrams"] if cwe_enabled else DEFAULT_MISSING_VALUE_TOKEN_COUNT
        parts.append(";col=" + column["name"] + ",preprocessing=" + _describe_preprocessing(column["preprocessing"])
                      + ",hashFunctions=" + str(hash_functions) + ",salt=" + salt + ",cwe=" + cwe_str
                      + ",missingValueTokenCount=" + str(missing_token_count))

    canonical = "".join(parts)
    digest = hmac.new(DIGEST_KEY, canonical.encode("utf-8"), hashlib.sha384).digest()
    return base64.b64encode(digest).decode("ascii")


# --------------------------------------------------------------------------
# MQTT
# --------------------------------------------------------------------------

def _new_client(client_id):
    try:
        return mqtt.Client(mqtt.CallbackAPIVersion.VERSION1, client_id=client_id)
    except AttributeError:
        # paho-mqtt < 2.0: nessun CallbackAPIVersion.
        return mqtt.Client(client_id=client_id)


# --------------------------------------------------------------------------
# Funzionalita' 1: Configura i DO
# --------------------------------------------------------------------------

def run_configure_dos():
    parties = discovered_parties()
    if not parties:
        print("Nessuno schema DO trovato in config/do_*.json")
        return

    version = next_version()
    print("Configurazione v" + version + ", party coinvolte: " + ", ".join(parties))

    pushes = {}
    digests = {}
    for party in parties:
        try:
            config_fragment, qid_columns = build_config_push(party)
        except (OSError, KeyError, ValueError) as e:
            print("ERRORE nella preparazione della configurazione per " + party + ": " + str(e))
            return
        pushes[party] = config_fragment
        digests[party] = compute_config_hash(config_fragment["bloomFilter"],
                                              config_fragment["missingValueHandling"], qid_columns)

    distinct_digests = set(digests.values())
    if len(distinct_digests) > 1:
        # Non dovrebbe accadere per costruzione (stesso encoding.json per
        # tutti): un digest divergente qui e' un bug nella preparazione della
        # config, non un errore di un singolo DO - meglio bloccarsi subito.
        print("ERRORE INTERNO: digest calcolati diversi tra i DO nonostante lo stesso encoding.json:")
        for party, digest in digests.items():
            print("  " + party + ": " + digest)
        return
    expected_digest = next(iter(distinct_digests))

    acks = {}
    acks_event = threading.Event()

    def on_connect(client, userdata, flags, rc, *args):
        client.subscribe(config_ack_topic_wildcard(), qos=1)

    def on_message(client, userdata, msg):
        try:
            payload = json.loads(msg.payload.decode("utf-8"))
        except json.JSONDecodeError:
            return
        party = payload.get("party")
        if party in parties and payload.get("version") == version and party not in acks:
            acks[party] = payload
            status = payload.get("status")
            print("  ack ricevuto da " + party + ": " + str(status))
            if len(acks) == len(parties):
                acks_event.set()

    client = _new_client("primat-smu-configure")
    client.on_connect = on_connect
    client.on_message = on_message
    client.connect(MQTT_HOST, MQTT_PORT)
    client.loop_start()
    try:
        time.sleep(0.5)  # lascia atterrare la subscribe prima di pubblicare
        for party in parties:
            payload = {"version": version, "configJson": json.dumps(pushes[party])}
            client.publish(config_topic(party), json.dumps(payload), qos=1)
            print("  push inviata a " + party + " su " + config_topic(party))

        completed = acks_event.wait(timeout=ACK_TIMEOUT_SECONDS)
    finally:
        client.loop_stop()
        client.disconnect()

    if not completed:
        missing = [p for p in parties if p not in acks]
        print("ERRORE: timeout in attesa degli ack di: " + ", ".join(missing))
        return

    errors = {p: a for p, a in acks.items() if a.get("status") != "OK"}
    if errors:
        print("ERRORE: configurazione v" + version + " rifiutata da almeno un DO, mi blocco:")
        for party, ack in errors.items():
            print("  [" + party + "] " + str(ack.get("detail")))
        return

    print("Configurazione v" + version + " completata su tutti i DO (" + ", ".join(parties) + ")")
    print("Digest atteso (per il confronto futuro con la LU): " + expected_digest)


def run_start_command():
    print("Funzionalita' 2 (inoltro StartCommand + digest alla LU) non ancora implementata.")


# --------------------------------------------------------------------------
# Menu interattivo
# --------------------------------------------------------------------------

def main():
    while True:
        print("\n=== PRIMAT SMU ===")
        print("1) Configura i DO")
        print("2) Avvia esecuzione (StartCommand) - non ancora implementato")
        print("0) Esci")
        choice = input("> ").strip()
        if choice == "1":
            run_configure_dos()
        elif choice == "2":
            run_start_command()
        elif choice == "0":
            break
        else:
            print("Scelta non valida.")


if __name__ == "__main__":
    main()
