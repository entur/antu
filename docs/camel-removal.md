# Removing Apache Camel from antu

What changed, why, and what to watch when it deploys. Written for whoever has to operate or extend antu
next.

The goal was narrow: take Camel out and keep every existing behaviour. Anything that changes behaviour is
called out as such, with the reason. The *Distributed correctness* and *Shutdown* sections are the ones to
read before changing anything in `pipeline/` or `leader/`: they are rules the work arrived at the hard way,
several of them after a review found the code breaking them.

## Why

Camel was doing three jobs in antu: PubSub transport, an in-memory aggregator that waited for a dataset's
files, and a REST layer. Only the first is hard, and the entur-google-pubsub helper already does it, using
the same base class nabu uses.

The aggregator was the real cost. It held pending aggregations in the heap of whichever pod had won a
Kubernetes leader election, so leadership moving meant losing them. That is the mechanism behind the
symptom recorded in the history: when the JDK 25 / Spring Boot 4.1 upgrade first landed, nightly
prevalidation went from 31 of 31 completing to 0 of 31, with antu's leader election failures up roughly
tenfold, and the upgrade was reverted (`c530a4a6`, reverted in `04a6de4e`). Both the aggregator and the
leader election it depended on are gone, which is what makes the upgrade viable.

## The shape of it now

A validation is a chain of jobs on one queue. Each step ends by putting the next job on `AntuJobQueue`, and
any pod picks it up. There is no orchestrator and no shared in-memory state.

```
marduk/kakka ──▶ AntuNetexValidationQueue
                        │
                        ▼
              ValidationRequestConsumer ──▶ ValidationInitializer
                        derives report id, notifies STARTED, submits SPLIT
                        │
                        ▼
    ┌──────────────  AntuJobQueue  ──────────────┐   consumed by every pod
    │                                            │
    ▼                                            │
 SplitDataset      → DatasetSplitter: explode the archive into Redis,
                     queue one ValidateFile per common file
 ValidateFile      → NetexFileValidator: validate one file, store its report,
                     report arrival to the barriers  ──────────────┐
 CreateLineFileJobs→ DatasetSplitter: queue the line files         │ barriers
 AggregateReports  → ReportAggregator: merge the per-file reports  │ live in
 ValidateDataset   → DatasetValidator: cross-file validators       │ Redis
 CompleteValidation→ ValidationCompleter: publish, notify, clean up ┘
    │
    ▼
 AntuNetexValidationStatusQueue ──▶ marduk/kakka
```

Two points need the whole dataset before they can proceed, and both are counted in Redis:

- **`COMMON_FILES_VALIDATED`**: line files may not be validated until every common file is, because rules on
  a line file need ids the common files declare.
- **`REPORTS_WRITTEN`**: the reports may not be merged until every file has produced one.

Each file records its arrival in a Redis set. The caller that both completes the set and wins an atomic claim
on it runs the next step.

## Replacement table

| Was | Is now | Note |
| --- | --- | --- |
| `camel-google-pubsub` consumers | `AbstractEnturGooglePubSubConsumer` (entur-google-pubsub) | Same base class as nabu |
| Manual `extendAckDeadline` plumbing | Nothing | The streaming pull client extends the deadline itself, up to an hour |
| Camel aggregator + `completionTimeout` | `ValidationBarrier` + `StalledValidationSweeper` | See *Behaviour changes* |
| `camel-master` + fabric8 k8s leader election | `RedisLeaseLeaderElection` | No Kubernetes API access at all; RBAC drops out |
| Camel REST DSL / platform-http | Spring MVC `@RestController` | Removes the DispatcherServlet-vs-Camel startup race |
| quartz timers | `@Scheduled` guarded by `leaderElection.isLeader()` | Leader decides, any pod executes |
| String `JOB_TYPE` header dispatch | sealed `AntuJob` + exhaustive `switch` | The set of switch arms is the set of jobs |
| Camel `Exchange` headers | `ValidationContext` record | Typed; the client's fields are echoed back verbatim |
| Camel Jackson data format | `validationReportObjectMapper` | Same JSON: ISO-8601 timestamps, nulls omitted |
| `antu.shutdown.timeout=175` | `InFlightMessages` (`SmartLifecycle`) | See *Shutdown* |

Against the pre-work base: 130 files changed, 40 main sources added and 20 deleted, 18 of them route
builders. `Constants.java` now holds only the PubSub attribute names that are actually on the wire.

### What stayed deliberately identical

The wire format. Every PubSub attribute name, the `JOB_TYPE` values, the status strings on
`AntuNetexValidationStatusQueue`, the file-name list in the message body, the published report JSON, and the
`reports/<referential>/validation-report-<id>.json` plus `.status` layout. Messages written by the previous
version stay readable, and marduk and kakka need no change.

> That path component is the **referential** (`rb_flb`), not the codespace (`flb`), in both versions. The
> REST endpoint calls its own path variable `codespace`, which is where the confusion comes from.

`JobType` spells its wire values out rather than deriving them from `name()`, so renaming a constant cannot
change the wire contract. `AGGREGATE_COMMON_FILES` was renamed to `CREATE_LINE_FILE_JOBS` with the old
literal frozen, since nothing aggregates common files any more.

## Distributed correctness

Camel gave antu two things that hid concurrency: an aggregator whose state lived in one pod's heap, and a
leader that most coordination ran through. Both are gone, so every step now runs on an arbitrary pod, more
than one at a time, against PubSub's at-least-once delivery. Three independent review passes found this same
class of bug in six different places here, so the rules below are worth following rather than rediscovering.

**A check followed by an act is not a guard.** Four places have to serialise across pods. The barrier was
built that way; the other three were found by review, and all were reachable:

| Where | The window | Now |
| --- | --- | --- |
| `ValidationBarrier` | Several pods see a full arrival set at once | Claim `..._passed` with `setIfAbsent` *before* reading the set. The claim is the guard, not the count |
| `ValidationCompleter` | Two deliveries both find the published marker missing, both notify, and marduk routes an `ok` onward | Claim `COMPLETING_<reportId>` before publishing |
| `ReportAggregator` | A second delivery reads the per-file reports the first delivery cleaned up as *lost*, and reports `timeout` for a dataset that already reported `ok` | Goes through `ValidationCompleter.abandon`, which takes the same marker and claim as completion |
| `StalledValidationSweeper` | A worker records progress between the snapshot and the notification, or the local cache is stale (below) | Re-read the state, then `abandon` through the same guards. The re-read narrows nothing on its own |

> The sweeper's re-read was documented here as narrowing the window "to one round trip". It does not.
> `allValidationStates()` iterates, which Redisson does not serve from the local cache, so the snapshot is
> authoritative; `getValidationState()` is a `get`, which `RLocalCachedMap` *does* serve locally. The re-read
> is therefore the *staler* of the two reads. Invalidation is pub/sub only and `reconnectionStrategy` defaults
> to `NONE`, so after a Redis reconnect a leader can hold a deleted entry until the one-hour local TTL and
> "confirm" a stall that is over. Routing the conclusion through `abandon` is what actually closes it: the
> `.status` marker the completion wrote is read from GCS, not from any cache.

**One way out, for every terminal status.** `ok`, `failed` and `timeout` all leave through
`ValidationCompleter`, which is the only place that takes the marker and the claim. `ReportAggregator` and
`StalledValidationSweeper` used to notify and clean up for themselves, which is how the aggregator ended up as
the one terminal path with neither guard. A second `notifyStatus` call site is a bug waiting to be written.

**"Once" needs a durable guard and an atomic one, and they are not the same guard.** The GCS `.status` marker
is durable: it catches a redelivery an hour later, after any Redis TTL has lapsed. The Redis claim is atomic:
it catches two deliveries in the same instant, which the marker cannot, because both read it before either
writes it. `ValidationCompleter` checks the marker first and then claims, in that order, so a redelivery still
gets to retry a clean-up that failed the first time.

> The marker records that a **terminal status was sent**, not that a report is readable: abandonment writes
> it too, having published nothing. That is what stops a redelivery arriving after the Redis claim's TTL from
> sending a second `timeout`, and what stops a late completion from following a `timeout` with an `ok`.

**A claim taken before work that can fail must be released if the work fails.** Both `ValidationBarrier` and
`ValidationCompleter` release in a `finally` covering `Throwable`. Skipping this is worse than the duplicate
the claim prevents: the redelivery finds a taken claim, does nothing, and the validation ends with no terminal
status at all.

**Read-modify-write on a shared map has to be conditional.** `recordProgress` reads the state, stamps it and
writes it back. With `put`, a clean-up landing between the read and the write recreates the entry that
clean-up had just removed, and the sweep then reports a `timeout` for a validation that had already reported
`ok`. `replace` on a Redisson map is a server-side conditional write, so a report that has been cleaned up
stays cleaned up. Recording progress on every validator notification rather than once per job made this
window wide enough to matter.

**`EXPIRE` belongs after the write.** On a key that does not exist yet it is a silent no-op, so the TTL never
lands and the key leaks. `ValidationBarrier` expires the arrival set after adding to it, and a test asserts
the TTL.

**A cached leadership flag is a decision from up to a heartbeat ago.** `isLeader()` checks the lease deadline
as well as the flag, so a stop-the-world pause longer than the lease cannot leave the old and the new leader
both sweeping. It is deliberately conservative: it can say no while the pod does still hold the lease, just
before a renewal lands, and skipping one sweep costs nothing.

**Renewing a lease is one round trip.** `get` then `expire` lets the lease lapse in between and hands the new
owner an extension it never asked for, while this pod goes on believing it leads. `RedisLeaseLeaderElection`
uses a Lua compare-and-expire. That needs Lua to be able to compare the value, so `ANTU_LEADER` is the one key
written with `StringCodec` rather than the client's Kryo codec, which also makes `redis-cli get ANTU_LEADER`
readable.

**`timeout` means antu could not validate the dataset; `failed` means the dataset is invalid.** Anything antu
concludes because it could not finish is `timeout`, never `failed`, so a problem of ours is never reported as
a problem with the data. `348c9f58` established this for the Camel aggregator's incomplete-group case, and
`StalledValidationSweeper` and `ReportAggregator` both follow it. marduk maps `timeout` to
`JobEvent.State.TIMEOUT`; it logs and discards statuses it does not know, so the marduk side has to be
deployed first.

**What is still coordinated through the leader, and why it is safe to be.** Only the work that must not
happen more than once at a time and does not block a validation: deciding when caches refresh, running the
stop place changelog consumer, and sweeping stalled validations. Every one is a trigger or a periodic check,
so losing it for a lease period delays something and breaks nothing. No barrier and no validation step needs
a leader.

## Shutdown

The HPA scales antu down routinely, so a pod is terminated mid-validation as normal operation rather than as
an incident. An abandoned file is redelivered and its XSD validation paid for again, and enough of those make
a dataset take considerably longer than it should. Camel bounded that window with
`antu.shutdown.timeout=175`, sized to sit just under `terminationGracePeriodSeconds: 180`.

Three things have to line up for the replacement to work, and each of them defaults the wrong way:

1. **The consumer base class waits only 10 s** for in-flight callbacks before the context finishes closing.
   `InFlightMessages` counts messages being processed and, as a `SmartLifecycle`, holds `stop()` open until
   they finish or `antu.shutdown.drain.timeout.seconds` runs out.
2. **`SmartLifecycle.stop()` is the correct hook**, not a `ContextClosedEvent` listener. It runs after those
   listeners have stopped the subscribers, so nothing new arrives during the drain, and before
   `destroyBeans()`, so Redis, the blob store and the publisher are all still usable by the work draining.
   `spring.lifecycle.timeout-per-shutdown-phase` does not bound it, because `stop()` is invoked inline on the
   closing thread and the phase timeout is only reached afterwards. A test pins that: if it ever changed, the
   drain would silently cap at 30 s.
3. **The publisher's thread pool stops accepting work on `ContextClosedEvent`**, which fires *before* the
   drain. Left at its default, a validation would drain successfully and then fail to publish its next job or
   its terminal status, so the drain held the pod open for work it then lost, which is the exact revalidation
   it exists to prevent, with a delayed shutdown to pay for it. Hence
   `spring.cloud.gcp.pubsub.publisher.executor-accept-tasks-after-context-close=true`. Confirmed both ways on
   the three-pod rig; the failure and its cost are in *Testing it*.

`terminationGracePeriodSeconds` must stay above `antu.shutdown.drain.timeout.seconds`, or SIGKILL cuts the
drain short and the files are revalidated anyway. That is necessary but **not sufficient**, which is what
the original 175-against-180 got wrong: the drain does not start when SIGTERM arrives. The
`ContextClosedEvent` listener runs first and calls `stopAsync().awaitTerminated(10, SECONDS)` once per
Subscriber, and for the Subscriber holding the in-flight file that wait always expires, because gax does
not terminate while a callback is running. The budget is therefore about 10 s per busy consumer, then the
drain, then bean destruction — so at 175 the total overran 180 and SIGKILL cut off the very file the drain
was holding the pod open for. The drain is 150.

> Still unexercised. Across ten pod shutdowns in dev, including eight at once, `InFlightMessages` never
> logged a drain: `inFlight` was zero every time, because the scale-downs happened to land in idle
> windows. The arithmetic above is read off the jar, not off a run.

## Behaviour changes

Everything below is a deliberate difference from the Camel implementation.

**A stalled validation ends in `timeout` instead of never ending.** Camel's `completionTimeout(30min)`
guaranteed that a dataset whose files stopped arriving was concluded anyway. Nothing about the Redis barriers
reproduces that, so `StalledValidationSweeper` does: on the leader, any validation that has made no progress
for 30 minutes gets a `timeout` status and its state released. Like `completionTimeout`, the threshold is on
**inactivity**, not total duration, so a large dataset still working through its files is not given up on.

> Progress is recorded by the validators, not just at job dispatch. `NetexValidatorsRunner` notifies every
> 10 s per running task but stops after 180 of them, so a single task stuck for more than half an hour does
> fall silent. The stall threshold has to stay at or above that to keep meaning what it says.

**A per-file report that has gone missing ends the validation as `timeout`.** Every file writes its report
before it arrives at the barrier, so a report absent at merge time was lost afterwards: expired on its TTL, or
evicted while Redis was under memory pressure. Merging only what remains publishes a report that looks
complete, and can look clean, for a dataset whose files were never all accounted for. `ReportAggregator`
publishes nothing and notifies `timeout` instead.

**Local retry is gone, on paper.** `BaseRouteBuilder` defaulted to three in-process retries with exponential
backoff, but the deployed ConfigMap set `antu.camel.redelivery.max=0`, so production never had them. A failure
nacked then and nacks now; the subscription's `retry_policy` provides the spacing. Nothing was lost here.

> One interaction. A validation request that fails to publish its STARTED or its split job is nacked after its
> state has been written, and the redelivery derives a new report id from the clock, so the first report is
> left with nothing to complete it and the sweep reports a `timeout` for it half an hour after the retry
> reported `ok`. The id derivation is unchanged from Camel, and so is the nack, since the retries were already
> configured off. `ValidationInitializer` therefore abandons its own state when either hand-off
> fails, leaving the redelivery as the only validation; the client sees a second STARTED, which is the state
> marduk already has it in. Keeping the id across the retry would need an idempotency key, and the only
> candidate is the client-supplied correlation id, which antu invents when it is absent.

**Common files are queued in reverse name order**, which puts `_stops.xml` ahead of the `_shared_data.xml`
that references it. The previous implementation left the order to `HashSet` iteration; this is at least
deterministic.

> **It orders the queue and nothing else, and in production not even that.** An earlier version of this
> section claimed one job consumer per pod at least serialises common files within a pod, so the declaring
> file "usually" wins. Dev says otherwise. Job messages carry no ordering key, so PubSub delivery order is
> not publish order, and every pod pulls concurrently. Measured on `mor`, 24 common files across ten pods:
> `_MOR__ScheduledStopPointsLinksAndAssignments.xml` sorts first (`_` is above `B` and the digits) and was
> published first, yet it *started* 19.5 s after another common file, ran for 45.8 s, and finished last of
> the 24 — it was the file that closed the barrier. All 23 others were validated start to finish before the
> declaring file was done.
>
> The dataset still validated clean. So for `mor` the intra-common-file dependency either does not exist or
> is not checked at that stage, and the same held for two other multi-common-file datasets that day. Keep
> the sort — it is free and it does decide the order on a single consumer — but do not reason from it. The
> guarantee that actually exists is `COMMON_FILES_VALIDATED`, which protects line files from common files,
> and that held: one barrier open, all 24 counted.

**`antu.netex.job.consumers` needed a second setting to mean anything.** Under Camel it set
`concurrentConsumers` on a `synchronousPull` endpoint with `maxMessagesPerPoll=1`, which really did bound
concurrency to one. Streaming pull does not work that way: it sets the number of Subscribers, and callbacks
run on a scheduler that defaults to four threads, so one pod would run four concurrent validations regardless
of the value, against a heap sized for one. This is a new problem, not a pre-existing one. `executor-threads`
and `flow-control.max-outstanding-element-count` are now set per subscription, so a long validation cannot
stop new requests being accepted, and no pod leases more than it can work on.

> Raising it does not scale cleanly. gax builds a `FlowController` per Subscriber, so N Subscribers each
> leasing N messages leases N² while running N. At the default of 1 that is invisible. Scale out on pods, or
> collapse this to one Subscriber with N `executor-threads` first.

> These per-subscription keys **cannot be set as environment variables.** The subscription name is a map key
> and env-var relaxed binding lower-cases it, so
> `SPRING_CLOUD_GCP_PUBSUB_SUBSCRIPTION_ANTUJOBQUEUE_EXECUTORTHREADS` binds `...subscription.antujobqueue...`
> and is silently ignored. helm writes a properties file, so it works there; docker-compose and local-k8s use
> `SPRING_APPLICATION_JSON`, which preserves case. `SubscriberConcurrencyPropertiesTest` pins both the working
> keys and the lower-cased trap.

**The Swagger endpoints are gone.** `/services/swagger.json` and `/services/validation-report/swagger.json`
were generated by the Camel REST DSL, so they went with it, and their `permitAll` entries went from the
security config with them. Anything still asking for them gets 401 or 404. Nothing in the pipeline consumes
them; springdoc-openapi is the replacement if a consumer turns up.

**`Content-Encoding: gzip` is gone from the report endpoint.** It had been claiming gzip over plain JSON
since 2022. Dropping it cannot break a caller either way: the bytes are unchanged for anyone who ignored the
header, and anyone who honoured it was failing to inflate plain JSON. Real compression is negotiated by the
container through `server.compression`, which sets the header itself, so it is always true: 17 991 bytes
plain, 1 085 gzipped, byte-identical after inflating.

**Logging carries more.** `reportId` and `fileName` are rendered from the MDC, so one dataset can be followed
through split, per-file, aggregate and publish. The MDC is cleared on entry to `onMessage`, not in a
`finally`, because the consumer base class logs a failed message *after* `onMessage` returns and that ERROR
line is the one worth having the identity on. `camel.breadcrumbId` is gone. `jsonPayload.codespace` still
carries the **referential** (`rb_flb`, not `flb`), which is deliberate parity with the Camel MDC and what
saved queries and the alert runbook depend on.

**Nothing calls the Kubernetes API.** No fabric8, no kubernetes-client, no ConfigMap or Lease permissions.

## Operating it

| Property | Default | What it does |
| --- | --- | --- |
| `antu.netex.job.consumers` | 1 | Validations per pod. Bounds heap; scale out on pods first |
| `antu.leader.lease.seconds` | 30 | Redis lease duration |
| `antu.leader.heartbeat.millis` | 10000 | Renewal interval, a third of the lease |
| `antu.validation.stalled.after.millis` | 1800000 | Inactivity before a validation is given up on |
| `antu.validation.sweep.millis` | 300000 | How often the leader looks for stalled validations |
| `antu.shutdown.drain.timeout.seconds` | 150 | In-flight work is finished rather than redelivered. Keep below `terminationGracePeriodSeconds` *minus* the subscriber close; see *Shutdown* |
| `antu.stop.refresh.cron` | `0 0 1,14 * * *` | Stop place cache refresh. `-` disables. Unquoted in the ConfigMap: it renders into a properties file, where quotes become part of the value |
| `antu.organisation.refresh.millis` | 1800000 | Organisation alias cache refresh interval |
| `spring.task.scheduling.pool.size` | 4 | One thread per `@Scheduled`: the heartbeat, the two refresh triggers and the sweep. Fewer lets the other three starve the heartbeat and drop the lease |
| `spring.cloud.gcp.pubsub.publisher.executor-accept-tasks-after-context-close` | true | Load-bearing for the drain; see *Shutdown* |
| `spring.cloud.gcp.pubsub.subscription.<name>.flow-control.max-outstanding-element-count` | 1 | Lease no more than can be worked on. The gax default is 1000 |
| `entur.pubsub.subscriber.autocreate` | false in helm | Topics and subscriptions are terraformed |

**JVM options live in `values.yaml` as `jvmFlags`**, not inline in the deployment, because the `docker-smoke`
job in CI reads them with `yq` and starts the image with them. That job is the only thing anywhere that runs
the built image; without it a flag the JRE rejects, or a class file version mismatch, is first observed in the
cluster. Two of the flags are load-bearing on JDK 25 and neither is obvious:

- `--sun-misc-unsafe-memory-access=allow`. Kryo's `FieldSerializer` calls `Unsafe.objectFieldOffset`, and Kryo
  is the codec for every Redis structure the pipeline runs on. JDK 25 still defaults this to `warn`, so the
  pods work without it right up until a base image bump flips the default to `deny`, at which point no pod can
  read or write Redis. Checked on 25.0.4: records survive `deny`, plain POJOs do not, and `ValidationState` is
  a POJO. `renovate` bumps the base image unattended, which is what makes this worth a flag rather than a note.
- `--enable-native-access=ALL-UNNAMED`, with `-XX:+IgnoreUnrecognizedVMOptions` so the same list still starts
  on an older JRE.

**Autoscaling.** Two thresholds decide the fleet, and the HPA takes the higher of them, so both have to be
sane or neither matters. `horizontalPodAutoscaler.messagesPerPod` is the target backlog per pod on
`AntuJobQueue`; `targetCPUUtilizationPercentage` is the other. Both were originally set so low that any
activity at all demanded `maxReplicas`: `0.1` means one queued message asks for the whole fleet, and `10`
percent of a `1500m` request is less than a validating pod ever uses. Measured in dev before the change: 20
JVM starts in 37 minutes, with eight pods stopped and eight started inside four minutes, for seven
validations. Scale-up is deliberately immediate and unthrottled, because a line file fan-out arrives all at
once and the work is already queued; scale-down is deliberately slow, because stopping a pod mid-validation
costs the drain and, if the drain does not finish, a revalidated file.

Redis keys: `ANTU_LEADER` (lease, plain string), `BARRIER_<STAGE>_<reportId>` and `..._passed` (barriers),
`COMPLETING_<reportId>` (claimed by both completion and abandonment), `TEMPORARY_FILE_<reportId>_*` (split
files and per-file reports), plus the existing report-scoped caches. Everything in that list carries a TTL
**except** the entries inside the report-scoped `RLocalCachedMap`s: `LocalCachedMapOptions.timeToLive` is the
*local* cache TTL, and the Redis hash fields have none, so an entry belonging to a validation that never
reaches `cleanUp` stays until something removes it. `CacheConfig` says so at the constant; this list used to
claim otherwise.

**Observability.** The existing log-based metric on `jsonPayload.message:"System error"` still fires: all
four original sites kept their wording, and the new terminal-failure paths were worded to match, so a stalled
validation, a lost per-file report and a failed stop-place prime all page someone. The runbook's instruction
to take `referential` and `reportId` off the log line only actually works now that those MDC fields are
rendered. Still missing: an alert on `oldest_unacked_message_age` for `AntuJobQueue`, which is the one signal
that would catch a poison message or a dead subscriber.

**An empty stop place cache is a wrong answer, not a degraded mode.** Every reference to a stop place comes
back unresolved and good datasets are reported as failed. A failed prime logs at `System error` level and
recovery is the next scheduled refresh; it deliberately does **not** queue a retry, because a permanent
failure would then redeliver until the subscription's retention ran out and there is no dead-letter topic.
Observed live: the queued-retry version produced 259 nacks in a few minutes.

## Deploying it

The cutover needs no validation in flight **and none arriving for the duration of the rollout**:
`AntuJobQueue`'s `num_undelivered_messages` at zero and no report in a STARTED state, held for the whole
rollout rather than checked once at the start. `maxSurge: 1` with `maxUnavailable: 1` means both versions
consume `AntuJobQueue` for minutes, so a request marduk sends midway through is as exposed as one that was
already running. Stop the validation clients, or accept that datasets crossing the rollout have to be
re-triggered. The two versions share none of four pieces of state:

- a validated common file is recorded by publishing to `AntuCommonFilesAggregationQueue` in the old version
  and in a Redis arrival set in the new one;
- the merged report was keyed by the first reverse-sorted file name and is now keyed `aggregated`;
- the `VALIDATE_DATASET` message carried that file name in its body and now carries none;
- `ValidationState` grew from one field to three, and it is stored through a bare `Kryo5Codec`, which
  writes fields positionally with no schema header.

A dataset whose files are handled by both versions therefore satisfies neither version's barrier. The sweep
reports it as `timeout` within 30 minutes, but it has to be re-triggered.

> The fourth one was found the hard way, in dev, after this section had already been written claiming there
> were three. Old entries are not merely ignored: too few fields underflow the buffer and throw, and
> `allValidationStates()` scans the whole hash, so one entry left by the previous version killed every
> stalled-validation sweep — every five minutes, for hours, logged only by Spring's scheduled-task error
> handler and never reaching the `System error` line the alert watches. The safety net was down and looked
> fine. In the other direction it is quieter and worse: a rolled-back pod reads a three-field payload as a
> one-field class, takes garbage for `hasErrorInCommonFile` and throws nothing.
>
> Fixed by versioning the key: `validationProgressCache` → `validationProgressCache.v2`. The two shapes now
> live under different names, so neither version can read the other's entries and no flush is needed. The
> old key is orphaned — nothing expires it, because entries inside the report-scoped `RLocalCachedMap`s
> carry no Redis TTL and are removed by `cleanUp` alone — so delete it by hand once the rollout is done.
> `ValidationStateSerializationTest` now fails if the fields change without the name changing.

**Deploy marduk first.** It logs and discards statuses it does not know, and this version publishes `timeout`,
which no antu version had ever sent.

**Nothing at all is removed from helm or terraform in this release.** `rbac.yaml`, the whole Camel-era
ConfigMap block, the `kubernetes` and `stopPlaceCacheRefreshQuartzTrigger` values, and the two aggregation
topics in `terraform/main.tf` are all kept and marked for deletion in a follow-up. helm and terraform both
apply before the first pod is replaced, so removing any of it takes something away from the version still
serving and from anything to roll back to.

This is easy to get wrong and was got wrong twice during this work. First the terraform resources were deleted
while the comment beside them said to apply only after the rollout had finished. Then the rule was written
down here as satisfied while the ConfigMap keys had in fact been deleted, which is the more dangerous half:

- `camel.cluster.kubernetes.enabled` is **fatal by itself**. Without it no `CamelClusterService` is
  registered, and `MasterComponent.doInit` throws `IllegalStateException("No cluster service found")` on the
  first `master:` route. Four routes use one, so the CamelContext fails to start and the pod CrashLoops.
- `antu.stop.refresh.interval` defaults to `trigger.repeatCount=0`, so dropping it does not fall back to a
  schedule, it stops the stop place cache refreshing at all. It needs the quartz URI form, which is why
  `stopPlaceCacheRefreshQuartzTrigger` exists alongside the Spring cron.
- `camel.dataformat.jackson.*` changes the shape of the published report, and
  `spring.mvc.servlet.load-on-startup` brings back the `/services/**` 404 this repo has already debugged once.

kubelet re-syncs a mounted ConfigMap into pods that are still running, so this is not only a rollback
concern: during the rollout, any old pod that restarts for any reason boots the old jar against the new
properties file.

**Rolling back.** Only two of the three obvious ways work, and the fastest-looking one is the one that does
not:

| Method | Works | Why |
| --- | --- | --- |
| `git revert` + full redeploy | yes | ConfigMap, values, Dockerfile and rbac revert together; terraform is a no-op in both directions |
| `helm rollback` | yes | Restores the ConfigMap and the image from one stored manifest, so the pair stays consistent. Check the stored manifest holds a concrete image reference and not an unresolved `<+artifacts.primary.image>` |
| `kubectl rollout undo`, or rolling the image back in Harness | **no** | Restores the pod template only. The ConfigMap is a separate unversioned object referenced by name, so the old jar boots against the new properties. With the block above kept this is survivable; without it, every pod CrashLoops |

`ANTU_LEADER` changes encoding, from the client's Kryo codec to a plain string, so that the atomic renewal
script can compare the value. This does **not** put the two versions in contention: the old version elects
through the Kubernetes API and never reads or writes `ANTU_LEADER` at all. For the length of the rollout there
is one old leader and one new leader at the same time, each running the cache refresh triggers and a stop
place changelog consumer. Both write the same Redis cache and the refresh only collides if the rollout spans
01:00 or 14:00, so the cost is duplicated work rather than a wrong answer. It resolves itself once the old
pods are gone. The old version's `coordination.k8s.io/Lease` objects (`antu-leaders-lock*`, created by the
fabric8 client, owned by no manifest) are left in place and expire on their own; a rollback reclaims them.

## Testing it

The old suite booted a PubSub emulator per scenario. Now:

- `AntuPipelineTestBase` runs a whole dataset validation in one JVM against embedded Redis and an in-memory
  blob store, with the job queue drained synchronously by `RecordingJobQueue`. No emulator, no polling for a
  result, and a stack trace that points at the step that failed.
- `PubSubWiringTest` is the only test that starts an emulator, and only checks the transport.
- 362 tests, about 80 seconds including compilation. The old suite was 278 in 112 seconds.

`TestApp` repeats the two filters `@SpringBootApplication` would contribute. Without `TypeExcludeFilter`
every `@TestConfiguration` under `no.entur.antu` is picked up by every test that boots it, so one test's
doubles silently replace another's; that is how deleting one REST test broke unrelated tests during this
work. Import the configurations you need.

**A test for a concurrency fix has to be run against the unfixed code.** Every regression test here was, and
one of them passed: the test written for the non-atomic lease renewal exercised an already-completed takeover,
which the old `get`-then-`expire` also handled correctly. The real interleaving cannot be reproduced from
outside once the two calls are one statement, so that test now claims only the observable contract and says in
its javadoc that the atomicity rests on the Lua script. The barrier, completion and abandonment tests do fail
without their fix, including the eight-way concurrent completion, the eight-way concurrent abandonment, and
all three release-on-failure cases when `RedisClaim` is stopped from releasing.

Three invariants are **not** pinned, and were confirmed unpinned by reverting each one and watching the suite
stay green: `isLeader()`'s lease-deadline check, `recordProgress`'s conditional `replace` (an earlier revision
of this section claimed a progress-during-clean-up test that does not exist), and `ValidationMdc`'s clear on
entry. Worse, the wire contract is asserted against itself: `attributeNamesAreTheOnesOnTheWire` reads its keys
through the same constants it is meant to pin, so renaming `Constants.NETEX_FILE_NAME` or a `JobType` literal
passes every test. That is the one thing *Deploying it* most depends on. A table-driven test against
hard-coded strings would close it.

**What the end-to-end runs did and did not cover.** A full docker-compose import was verified green through
every stage, and `local-k8s/` was run with three replicas on two datasets: the split on one pod, the common
file on a second, the line files on a third, each barrier opening exactly once, one terminal status per
dataset, no double work and no nacks.

The drain was then exercised on the rig by deleting a pod while it was validating, which is worth repeating
after any change to shutdown or to publishing. With the publisher property set, the killed pod finished the
9.5 s file it was in the middle of, opened the common-files barrier, published the next job and logged
`Finished the in-flight messages, shutting down`; the dataset ended with three files validated once each and
one terminal status. With the property back at its default, the same test fails in exactly the way the
*Shutdown* section describes:

```
Waiting up to 175 s for 1 message(s) still being processed
Validated NeTEx file _shared_data.xml in 9620 ms
COMMON_FILES_VALIDATED: barrier opened for report ...
COMMON_FILES_VALIDATED: released the barrier ... after the hand-off failed
Message processing failed, retrying in 1000 milliseconds
  java.util.concurrent.RejectedExecutionException: ... rejected from
  ThreadPoolTaskScheduler$1[Terminated, pool size = 0] at Publisher.publish
```

One killed pod then cost five file validations for three files, because the redelivered common file reopened
the barrier and the line file was validated again. Both runs ended with one terminal status, which is the
barrier's release-on-failure path doing its job.

Still not covered: clean-up racing with progress, Redis eviction, and a rollout with both versions on the
queue.

## Known open

Ranked by consequence, none of them blocking:

1. The barrier claim is not owner-tracked, so a pod killed between claiming and publishing leaves the claim
   held. The sweep bounds the consequence to 30 minutes rather than forever. The same is true of the
   completion claim.
2. There is no `LeadershipRevokedEvent`, so a pod that loses the lease stops deciding but does not stop the
   stop place changelog consumer it started on taking over, and after a flap two pods consume it. Camel had
   the same gap: the old route wrapped the quartz trigger in `master:`, while the Kafka listener started by
   `stopPlaceRepositoryUpdater.init()` lived outside the route and was never stopped either. Closing it means
   giving `StopPlaceRepositoryUpdater` a way to stop, which it does not have today.
3. A pod demoted by a single heartbeat exception cannot retake its own still-live lease; it waits for the
   lease to expire and then re-acquires.
4. Common file ordering is not enforced at all — not across pods and not within one, since delivery order
   is not publish order. Now measured rather than assumed, and no impact observed on three
   multi-common-file datasets including one with 24: see *Behaviour changes*. Ranks below everything else
   in this list on present evidence, and is kept only so that a dataset reporting unresolved references
   between its own common files has somewhere obvious to start.
5. `ValidationState` is read-modify-write over an `RLocalCachedMap`, so `hasErrorInCommonFile` set on one pod
   can be lost by a concurrent progress write on another. The conditional write stops the entry being
   resurrected but does not merge fields. The consequence is a noisier report, not a wrong verdict: line files
   are validated that could have been skipped.
6. No alert on `oldest_unacked_message_age`, and no dead-letter topic on `AntuJobQueue`.
7. Seven SonarCloud findings are open because prettier-java 2.1.0, the version in effect, cannot parse the
   syntax they ask for: unnamed variables (`java:S7467`, six of them, in switch labels and catch clauses) and
   a record pattern in a switch label (`java:S6878`). A version that parses all three does exist - 2.6.8, on
   prettier 3 - so this is not blocked on the tooling. It is blocked on the diff: the formatter runs in
   `validate` and gates the build, and bumping it reformats 134 of 292 files by about 3100 lines of pure
   prettier-3 style churn. Its own change. Nine further findings are
   suppressed at the site with the reason, seven of them the unused bindings an exhaustive switch over a
   sealed type requires: a `default` arm would silence them but give up the compile error when a job type is
   added, which is the property the sealed design exists for.
8. `ValidateFile` carries sentinel values for line files instead of being split into two records;
   `CompleteValidation` is a job type nothing submits; `ValidationReportStore.workPath` builds a bucket-shaped
   path for use as a Redis field name.
