# Antu
[![CircleCI](https://circleci.com/gh/entur/antu/tree/master.svg?style=svg)](https://circleci.com/gh/entur/antu/tree/master)

Validate NeTEx datasets against the [Nordic NeTEx Profile](https://enturas.atlassian.net/wiki/spaces/PUBLIC/pages/728891481/Nordic+NeTEx+Profile).

# Data flow
Antu receives NeTEx validation requests from [Marduk](https://github.com/entur/marduk).
The request refers to a given NeTEx codespace and a NeTEx dataset (zip archive) stored in a Google Cloud Storage bucket.  
Antu extracts the individual PublicationDelivery files from the NeTEx archive and register a validation job for each of them in a PubSub topic.  
The resulting workload is then split among the running Antu Kubernetes pods, processed asynchronously and in parallel.
Each validation job produces a JSON-serialized ValidationReport object.  
When all validation jobs are complete, the individual ValidationReports are combined in a single object and stored in GCS under a unique report ID.  
Antu sends a message to a PubSub topic to notify Marduk that the validation is complete.

# Validation rules
Ant uses the [NeTEx validator library](https://github.com/entur/netex-validator-java) to execute a set of validation rules on the NeTEx dataset.  
In addition to the default rules present in this library, Antu defines a set of rules that are specific to Entur and relevant in a Norwegian context.  
This applies to validation against the [National Stop Register](https://stoppested.entur.org/) or against the Organisation Register.

# API
Validation reports can be downloaded thanks to a REST API. Reports are identified by their unique report ID.  
The API is OAuth2-protected and access rights must be sufficient to access a given report.

# Kubernetes integration
Antu is designed so that the validation workload can be split evenly among single-core Kubernetes pods.  
This results in smaller pods, both in terms of CPU and memory consumption, which makes the Kubernetes scheduling process more efficient.
The number of pods is adjusted dynamically thanks to a Horizontal Pod Autoscaler.

# Parallel processing
The Nordic NeTEx Profile mandates that datasets are delivered as single-line files within a zip archive with, optionally, a set of "common files" that gather objects shared between lines.  
This means that validation of individual line files can be run in parallel and mostly independently of one another, with the following exceptions:
 * **validating references** from a line file to a shared object in a "common file" requires that common files are processed first, and line files afterwards, so that all shared ids can be collected before validating the line files. 
 * **validating NeTEx ids uniqueness** across the dataset needs to be synchronized.
Antu uses distributed locks and distributed collections stored in Redis to ensure proper synchronization between concurrent jobs.
 