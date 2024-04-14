# Benchmark Datasets and Workloads

This document describes the graph datasets, as well as the view and query workloads used in the experiment.

## Workload Description
View definitions contain:
* Standard Views (GQL/G-CORE)
* Transformation views with default rules
  * Create new nodes or edges / delete
  * Mapping variables (of nodes/edges) to a node or edge
  
Queries contain:
* Retrieve a node or edge pattern or a graph pattern


## LSQB
### Description of Dataset
* Description: A benchmark for subgraph matching.
* Source: [LSQB](https://github.com/ldbc/lsqb)

#### Schema
* Refer to the LSQB paper [[Link]](https://dl.acm.org/doi/pdf/10.1145/3461837.3464516).

### View definition
```
// V1
CREATE VIEW v1 ON g (
  MATCH
    (person1:Person)-[e1:KNOWS]->(person2:Person), 
    (comment:Comment)-[e3:HASCREATOR]->(person1:Person), 
    (comment:Comment)-[e4:REPLYOF]->(post:Post),
    (post:Post)-[e5:HASCREATOR]->(person2:Person)
  WHERE person1 <= 1159400
  CONSTRUCT
    (person1:Person)-[e1:KNOWS]->(person2:Person), 
    (comment:Comment)-[e3:HASCREATOR]->(person1:Person), 
    (comment:Comment)-[e4:REPLYOF]->(post:Post),
    (post:Post)-[e5:HASCREATOR]->(person2:Person)

// V2
CREATE VIEW v2 ON g (
  MATCH
    (person1:Person)-[e1:ISLOCATEDIN]->(city1:City),
    (city1:City)-[e2:ISPARTOF]->(country:Country),
    (person2:Person)-[e3:ISLOCATEDIN]->(city2:City),
    (city2:City)-[e4:ISPARTOF]->(country:Country),
    (person3:Person)-[e5:ISLOCATEDIN]->(city3:City),
    (city3:City)-[e6:ISPARTOF]->(country:Country),
    (person1:Person)-[e7:KNOWS]->(person2:Person),
    (person2:Person)-[e9:KNOWS]->(person3:Person)
    WHERE person1 >= 1159373 AND person1 <= 1159400
  CONSTRUCT
    (person1:Person)-[e1:ISLOCATEDIN]->(city1:City),
    (city1:City)-[e2:ISPARTOF]->(country:Country),
    (person2:Person)-[e3:ISLOCATEDIN]->(city2:City),
    (city2:City)-[e4:ISPARTOF]->(country:Country),
    (person3:Person)-[e5:ISLOCATEDIN]->(city3:City),
    (city3:City)-[e6:ISPARTOF]->(country:Country),
    (person1:Person)-[e7:KNOWS]->(person2:Person),
    (person2:Person)-[e9:KNOWS]->(person3:Person)
)      
```          


### Query Workload
```
// Q1
MATCH 
  (person1:Person)-[e1:KNOWS]->(person2:Person), 
  (comment:Comment)-[e3:HASCREATOR]->(person1:Person), 
  (comment:Comment)-[e4:REPLYOF]->(post:Post),
  (post:Post)-[e5:HASCREATOR]->(person2:Person)
FROM v1 
WHERE person1 >= 1159373 AND person1 <= 1159400
RETURN (person1)

// Q2
MATCH 
  (person1:Person)-[e1:ISLOCATEDIN]->(city1:City),
  (city1:City)-[e2:ISPARTOF]->(country:Country),
  (person2:Person)-[e3:ISLOCATEDIN]->(city2:City),
  (city2:City)-[e4:ISPARTOF]->(country:Country),
  (person3:Person)-[e5:ISLOCATEDIN]->(city3:City),
  (city3:City)-[e6:ISPARTOF]->(country:Country),
  (person1:Person)-[e7:KNOWS]->(person2:Person),
  (person2:Person)-[e9:KNOWS]->(person3:Person)
FROM v2 
WHERE person1 >= 1159373 AND person1 <= 1159380
RETURN (person1)
```


## PROV
### Description of Dataset
* Description: Talk namespace - edits of discussion pages attached to each Wikipedia article
* Source: [Complete Wikipedia edit history (up to January 2008)](https://snap.stanford.edu/data/wiki-meta.html) (File: enwiki-20080103.talk.bz2)

#### Schema
```
Node labels
 (:R) : Activity - Revision
 (:U) : Agent - User
 (:E) : Entity

Edge labels
 -[:DERBY]-> : isDeriveFrom
 -[:USED]-> : used
 -[:GENBY]-> : isGeneratedBy

Constraints on Edge Connectivity
 (:R)-[:DERBY]->(:R)
 (:R)-[:USED]->(:E)
 (:R)-[:GENBY]->(:E)
```            

### View definition
```
// V3
CREATE VIEW v3 ON g WITH DEFAULT MAP (
 MATCH (e2:E)-[d:DERBY]->(e1:E)
 WHERE e1 < 1000
 CONSTRUCT (a:REVISION)-[u:USED]->(e1:E), (e2:E)-[g:GENBY]->(a:REVISION)
 SET a = SK(\"rev\", d), u = SK(\"used\", d), g = SK(\"genby\", d)
  UNION
 MATCH (e2:E)-[d1:DERBY]->(e1:E), (e3:E)-[d2:DERBY]->(e2:E)
 WHERE e1 < 1000
 CONSTRUCT (e3:E)-[u:MULTIDERBY2]->(e1:E)
 SET u = SK(\"multi\", e1, e3)
)
```
### Query Workload
```
// Q3
MATCH (a:REVISION)-[u:USED]->(e1:E), (e2:E)-[g:GENBY]->(a:REVISION), (e2:E)-[e3:DERBY]->(e1:E)
FROM v3
WHERE e1 < 1000
RETURN (a),(u),(e1),(e2),(g),(e3)

// Q4
MATCH (e2:E)-[u:MULTIDERBY2]->(e1:E)
FROM v3
WHERE e1 < 1000
RETURN (e1),(e2),(u)

// Q5
MATCH (e2:E)-[u1:MULTIDERBY2]->(e1:E), (e3:E)-[u2:MULTIDERBY2]->(e2:E)
FROM v3
WHERE e1 < 1000
RETURN (e1),(e3),(u1),(u2),(e2)

// Q6
MATCH (a:REVISION)-[u:USED]->(e1:E), (e2:E)-[g:GENBY]->(a:REVISION), (e2:E)-[e4:DERBY]->(e1:E), (e3:E)-[m2:MULTIDERBY2]->(e2:E)
FROM v3
WHERE e1 < 1000
RETURN (a),(u),(e1),(e2),(g),(e4),(m2)
```


## OAG
### Description of Dataset
* Description: Open Academic Graph (OAG) is a large knowledge graph unifying two graphs: Microsoft Academic Graph (MAG) and AMiner.
* Source: [Open Academic Graph](https://www.microsoft.com/en-us/research/project/open-academic-graph/)
#### Preprocessing
* Select the papers published in 2016 only.
#### Schema
```
Node labels
 (:A) : Author
 (:P) : Paper
 (:V) : Venue

Edge labels
 -[:W]-> : isWrittenBy
 -[:P]-> : isPublishedAt
 -[:L]-> : isLinkedTo

Constraints on Edge Connectivity
 (:P)-[:W]->(:A)
 (:P)-[:P]->(:V)
 (:P)-[:L]->(:P)
 (:A)-[:L]->(:A)
 (:V)-[:L]->(:V)
 ```            
### View definition
```
// V4
CREATE VIEW v4 ON g WITH DEFAULT MAP (
 MATCH (p:P)-[e1:W]->(a:A), (p:P)-[e2:P]->(v:V)
 WHERE p < 12353000
 CONSTRUCT (a:A)-[e3:PUBVEN]->(v:V)
 SET e3 = SK(\"kk1\", a, v)
  UNION
 MATCH (p:P)-[e1:W]->(a1:A), (p:P)-[e2:W]->(a2:A)
 WHERE p < 12352800 AND a1 != a2
 CONSTRUCT (a1:A)-[e3:COAUTHOR]->(a2:A)
 SET e3 = SK(\"kk2\", a1, a2)
)
```
### Query Workload
```
// Q7
MATCH (a1:A)-[e:COAUTHOR]->(a2:A)
FROM v4
RETURN (a1), (a2)

// Q8
MATCH (a1:A)-[e1:COAUTHOR]->(a2:A), (p:P)-[e2:W]->(a1:A), (p:P)-[e3:W]->(a2:A)
FROM v4
WHERE p < 12353000
RETURN (a1),(a2),(p)

// Q9
MATCH (a:A)-[e:PUBVEN]->(v:V)
FROM v4
WHERE a < 12
RETURN (a), (v)

// Q10
MATCH (a1:A)-[e1:COAUTHOR]->(a2:A), (p:P)-[e2:W]->(a1:A), (p:P)-[e3:W]->(a2:A), (a2:A)-[e4:PUBVEN]->(v:V)
FROM v4
WHERE p < 12353000
RETURN (a1),(a2),(v)
```


## SOC
### Description of Dataset
* Source: [TWITTER-FOLLOWS](https://networkrepository.com/soc-twitter-follows.php)
#### Schema
```
Node labels
 (:U) : User

Edge labels
 -[:F]-> : isFriendOf

Constraints on Edge Connectivity
 (:U)-[:F]->(:U)
```
### View definition
```
// V5
CREATE VIEW v5 ON g WITH DEFAULT MAP (
 MATCH (u1:U)-[f1:F]->(u2:U), (u2)-[f2:F]->(u3:U), !(u1)-[f3:F]->(u3)
 WHERE u1 != u3 AND u2 < 10000
 CONSTRUCT (u1:U)-[r:R2A]->(u3:U)
 SET r = SK(\"ra\", u1, u3)
  UNION
 MATCH (u1:U)-[f1:F]->(u2:U), (u2)-[f2:F]->(u3:U), !(u1)-[f3:F]->(u3)
 WHERE u1 != u3 AND u2 < 10000
 CONSTRUCT (u1:U)-[r:R2B]->(u3:U)
 SET r = SK(\"rb\", u1, u3)
  UNION                        
 MATCH (u1:U)-[f1:F]->(u2:U), (u2)-[f2:F]->(u3:U), (u3)-[f3:F]->(u4:U), !(u1)-[f4:F]->(u4)
 WHERE u1 != u4 AND u2 < 20000
 CONSTRUCT (u1:U)-[r:R3A]->(u4:U)
 SET r = SK(\"rc\", u1, u4)
  UNION
 MATCH (u1:U)-[f1:F]->(u2:U), (u2)-[f2:F]->(u3:U), (u3)-[f3:F]->(u4:U), !(u1)-[f4:F]->(u4)
 WHERE u1 != u4 AND u2 < 20000
 CONSTRUCT (u1:U)-[r:R3B]->(u4:U)
 SET r = SK(\"rd\", u1, u4)
)
```
### Query Workload
```
// Q11
MATCH (u1:U)-[r:R2A]->(u2:U)
FROM v5
WHERE u1 < 2000
RETURN (u2)

// Q12
MATCH (u1:U)-[r1:R3A]->(u2:U), (u1:U)-[r2:R3A]->(u3:U)
FROM v5
WHERE u1 < 5000 AND u2 != u3
RETURN (u2), (u3)

// Q13
MATCH (u1:U)-[r1:R2A]->(u2:U), (u2:U)-[r2:R2B]->(u3:U)
FROM v5
WHERE u1 < 5000
RETURN (u1), (u3)

// Q14
MATCH (u1:U)-[r1:R2A]->(u2:U), (u2:U)-[r2:R2B]->(u3:U),
(u4:U)-[r3:R3A]->(u2:U), (u2:U)-[r4:R3B]->(u5:U)
FROM v5
WHERE u1 < 20000 AND u1 != u4 AND u3 != u5
RETURN (u1), (u3)
```


## WORD
### Description of Dataset
* Source: [WordNet](https://wordnet.princeton.edu/)
#### Preprocessing
* The dataset is obtained from [the Python NLTK package](https://www.nltk.org/howto/wordnet.html) and converted into a graph.
#### Schema
```
Node labels
 (:W) : Word
 (:L) : Lemma
 (:S) : Synset

Edge labels
 -[:SL]-> : is-Synset-Lemma-relationship
 -[:WS]-> : is-Word-Synset-relationship

Constraints on Edge Connectivity
 (:S)-[:SL]->(:L)
 (:W)-[:WS]->(:S)
```            
### View definition
```
// V6
CREATE VIEW v6 ON g WITH DEFAULT MAP (
 MATCH (w:W)-[ws:WS]->(s:S), (s:S)-[sl:SL]->(l:L)
 WHERE w < 5000
 CONSTRUCT (m:MS)
 SET m = SK(\"ff\", w, s)
 MAP FROM w, s TO m
 // DELETE ws
  UNION
 MATCH (w:W)-[ws:WS]->(s:S), (s:S)-[sl:SL]->(l:L)
 WHERE w > 5000 AND w < 30000
 CONSTRUCT (w:W)-[wl:WL]->(l:L)
 SET wl = SK(\"ss\", w, l)
)
```
### Query Workload
```
// Q15
MATCH (m:MS)
FROM v6
RETURN (m)

// Q16
MATCH (m:MS)-[sl:SL]->(l:L), (m:MS)-[ws:WS]->(m:MS)
FROM v6
RETURN (m), (sl), (ws), (l)

// Q17
MATCH (w:W)-[wl:WL]->(l:L)
FROM v6
RETURN (w),(wl),(l)

// Q18
MATCH (m:MS)-[sl:SL]->(l:L), (m:MS)-[ws:WS]->(m:MS), (w:W)-[wl:WL]->(l:L)
FROM v6
RETURN (m),(sl),(l),(ws),(wl),(w)
```
