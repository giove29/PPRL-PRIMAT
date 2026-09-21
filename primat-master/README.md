# PRIMAT: Private Matching Toolbox

<img src="primat_logo.png" width="250">


PRIMAT is an open source (ALv2) toolbox for the definition and execution of PPRL workflows. 
It offers modules for data owners and the linkage unit that provide state-of-the-art PPRL methods,
including Bloom-filter-based encoding and hardening techniques, LSH-based blocking, post-processing (clustering) and more.


[PRIMAT](https://dl.acm.org/citation.cfm?doid=3352063.3360392) is developed by the [Database Group](https://dbs.uni-leipzig.de/research/projects/pper_big_data) of the University of Leipzig, Germany.

## Using PRIMAT

To use PRIMAT in your project, simply add the following dependency to your build tool

```xml
<dependency>
    <groupId>de.uni-leipzig.dbs.pprl</groupId>
    <artifactId>primat-data-owner</artifactId>
    <version>1.0.3</version>
</dependency>
```

for data owner components, including pre-processing and encoding methods, or

```xml
<dependency>
    <groupId>de.uni-leipzig.dbs.pprl</groupId>
    <artifactId>primat-linkage-unit</artifactId>
    <version>1.0.3</version>
</dependency>
```

for linkage unit components, including linkage and post-processing (clustering) methods.

## PRIMAT Modules

- `primat-common` - Contains shared data model and various utility function, e.g, for input file handling, hashing, feature extraction.

- `primat-data-owner` - Contains typical pre-processing functions as well as techniques to encode/mask records for PPRL.

- `primat-linkage-unit` - Provides functionalities for batch and incremental linkage workflows, including blocking, similarity calculation, classification, post-processing (clustering) and evaluation.

- `primat-examples` - Contains example workflows showing use cases for PRIMAT. 

- `primat-analysis` - Modul containing tools for analyzing records and error types

## Privacy-preserving Record Linkage

- Task of identifying record in different databases reffering to the same person
- Protection of sensitive personal information
- Applications in medicine & healthcare, national security and marketing analysis

<img src="https://user-images.githubusercontent.com/20927034/118960531-acfb8e00-b963-11eb-894e-ecffafbd8f87.png" width="500">

### Key Challenges

- Gurantee privacy by minimizing disclosure risk
- Scalability to millions of records
- High linkage quality

## PRIMAT: Overview

- PPRL tool covering the entire PPRL life-cycle
- Flexible definition and execution of PPRL workflows
- Comparative evaluation of PPRL approaches
- Modules for both data owner and the trusted linkage unit

<img src=https://user-images.githubusercontent.com/20927034/118961272-707c6200-b964-11eb-9ed9-8264e04cc840.png width="500">

### State-of-the-art PPRL Methods

#### Bloom filter encodings & hardening techniques

<img src=https://user-images.githubusercontent.com/20927034/118971359-9c511500-b96f-11eb-8f41-986724c7db92.png width="400">

#### Fast & private blocking/filtering techniques

<img src=https://user-images.githubusercontent.com/20927034/118971495-ca365980-b96f-11eb-88fb-b7478288a2dc.png width="400">

#### Post-processing methods for one-to-one link restriction

<img src=https://user-images.githubusercontent.com/20927034/118971617-f05bf980-b96f-11eb-8bcc-d1d0a4a0114e.png width="400">

### Unsupervised Quality Estimation
A record linkage method aims to generate qualitative linkage results so that precision and recall are high. Due to the lack of 
available ground truth data, methods are required that can estimate precision and recall in an unsupervised manner. 
The following methods are available
in the package `de.uni_leipzig.dbs.pprl.primat.lu.quality_estimation`

|Class | Name in the Paper|Dirty? | Description |
|---------|--------|------------------|--------|
| `PrecisionOneToOneBase` |  -  | no | number of true positives is estimated by the number of vertices from source |
| `PrecisionStructureOneToOne` | PP_1:1| no| number of true positives is estimated by the <b>minimum</b> of the number of vertices having an edge|
| `PrecisionStructureOneToN`| PP_1:n |no |number of true positives is estimated by the <b>sum</b> of the number of vertices having an edge |
| `PrecisionProbabilityDeduplicated` |PP_prob| no | Determines for each edge the probability to be selected. The precision is computed by the expectation value regarding the probability of 1:1 and 1:n links  |
| `PrecisionPPBasedTPEstimation` | PP_dup| yes | Using personalized pagerank to compute a TP score for each edge being aggregated to determine the overall precision |
| `RecallStructureOneToOne` |PR_AltMin |no | overlap estimated by the <b>minimum</b> in combination with the <b>minimum</b> of the number of vertices having an edge|
| `RecallStructureOneToN`| PR_Alt | no | overlap estimated by the <b>sum</b> in combination with the <b>sum</b> of the number of vertices having an edge|
| `RecallStructureNToM` | PR| no | overlap estimated by the <b>minimum</b> of the numbers of records from source and target in combination with the number of edges|
| `RecallCryptoSetOneToOne` |PR_CE[a]1:1| no | Using the cryptoset estimation to determine the overlap in combination with the <b>minimum</b> of the number of vertices having an edge|
| `RecallCryptoSetOneToN` |PR_CE[a]1:n| no | Using the cryptoset estimation to determine the overlap in combination with the <b>sum</b> of the number of vertices having an edge |
| `RecallCryptoSetProb` | PR_CE[a]prob|yes/no | Using the cryptoset estimation to determine the overlap in combination with the expected  number of true positives determined by `PrecisionProbabilityDeduplicated` or `PrecisionPPBasedTPEstimation`|



### Functional Overview 

|Component/Module | Function/Feature | Status |
|-----------------|------------------|--------|
| Data generator & corruptor | - Data generation<br> - Data corruption | Integration outstanding<br>Planned |
| Data cleaning | - Split/merge/remove attributes<br>- Replace/remove unwanted values<br>- OCR transformation | Implemented<br>Implemented<br>Implemented |
| Encoding | - Bloom filter encoding<br>- Bloom filter hardening techniques<br>- Support of alternative encoding schemes| Implemented<br>Implemented<br>Partially implemented |
| Blocking | - Standard blocking<br> - LSH-based blocking| Implemented<br>Implemented |
| Matching | - Threshold-based classification<br>- Post-processing<br>- Multi-threaded execution<br>- Distributed matching<br>- Multi-Party support, match cluster management<br>- Incremental Matching | Implemented<br>Implemented<br>Partially implemented<br>Integration outstanding<br>Implemented<br>Implemented |
| Evaluation | - Measures for assessing quality & scalability<br>- Masked match result visualization | Implemented<br>Integration outstanding |

### Requirements

- Java 11+ 
- Maven
- Ubuntu (recommended)
- PostgreSQL (for incremental matching)

#### Database Setup

- Required for incremental matching, and for the 4 persistent clustering strategies of `primat-linkage-unit-service` (Center Clustering, MSCD-AP, Global Greedy, CLIP — stable cluster ids across runs; MCL is the only non-persistent strategy — see `ARCHITECTURE_FLOW.md`)
- Each strategy uses its own dedicated PostgreSQL database (`primat_center_clustering`, `primat_mscd_ap`, `primat_global_greedy`, `primat_clip`); see `TESTING.md` section 1 for the exact `docker`/`createdb` commands, and `db_reset_scripts/reset_db.py` to empty one or all of them
- Edit the `persistence.xml` file under `/primat-linkage-unit/src/main/resources/META-INF/persistence.xml` and change the connection properties according to your configuration if not using the defaults (`primat`/`primat`)

## Future Plans

We plan to gradually add new features releated to our ongoing research.

## Contributors

- Florens Rohde
- Victor Christen
- Ziad Sehili
- Thomas Hoppe
- Duc Dung Dao
- Marcel Gladbach
