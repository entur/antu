# Migrating off Apache Camel: what antu learned

Written for whoever migrates the next service. `camel-removal.md` describes what antu's replacement *is*;
this describes what the work turned out to cost, what broke, and in which order things went wrong. Where the
two disagree, this one is later.

Scope: `f9cb18f6` (Remove Apache Camel, #933) and the followups it needed — `45973cf7`, `e4a1d725`,
`cc15c23f`, `4d7b8b68`, `e91ef220`, `2d26c86f`. Six followups in one day, which is itself the headline: the
main commit passed CI, deployed cleanly, validated ten datasets correctly, and was still wrong in six ways.

## The short version

Swapping the transport is the easy part and took the least time. Three things cost the rest:

1. **Coordination Camel was doing implicitly.** An in-heap aggregator on a leader-elected pod becomes N pods
   racing under at-least-once delivery. Three independent review passes found the same class of bug in six
   different places.
2. **State that turns out to be a wire format.** Two versions of the service coexist during a rollout, so
   every Redis value and every message attribute is a compatibility surface. One added field took down a
   safety net for hours.
3. **The removal release itself.** What you delete from helm and terraform, and in what order, decides
   whether you can roll back at all.

None of these are about Camel. They are about what Camel was hiding.

## Before you write any code

### Write down the wire contract, then pin it with tests that assert literals

Enumerate everything matched by name from outside the repo, and put it in one test that compares against
**hard-coded strings**:

- message attribute names, and the values other services filter subscriptions on
- message body values (status strings, type discriminators)
- topic and subscription names
- delimiters inside message bodies
- blob storage path layout
- anything a UI fetches by URL

The trap is subtle and antu fell into it. A test like this pins nothing:

```java
assertEquals("VALIDATE", attributes.get(JOB_TYPE));   // JOB_TYPE is the constant
```

Both sides move together, so renaming the constant's *value* keeps the test green. Verified: renaming
`NETEX_FILE_NAME`, a `JobType` wire value, the `timeout` status, the `Marduk` client name and a queue name
left all 362 tests passing. `WireContractTest` (`e4a1d725`) asserts the literals instead, and all five
mutations now fail. Also assert `EnumSet.allOf(...)` against the pinned set, so a new enum constant cannot
reach the wire without being pinned deliberately.

### Inventory what Camel was doing implicitly

Go route builder by route builder, and for each ask what Camel supplied that no line of your code mentions.
antu's list, as a starting checklist:

| Camel feature | What to check |
| --- | --- |
| `errorHandler` / `redeliveryPolicy` | retry count, backoff, and what a nack does instead |
| `onCompletion`, `UnitOfWork` | anything running on exchange completion, including deferred acks |
| aggregator | `completionSize`, `completionTimeout`, `discardOnCompletionTimeout`, the grouping key |
| `master:` | what stopped when leadership was *lost*, not just what started when it was gained |
| quartz | `stateful=true`, misfire policy, overrun behaviour |
| `split()` | streaming, `stopOnException`, `parallelProcessing`, `shareUnitOfWork` |
| stream caching | what was being re-read that Camel buffered |
| type converters | implicit `InputStream` to `byte[]` to `String` conversions |
| data formats | exact Jackson settings (`include`, module refs, date shape) |
| `platform-http` | content negotiation, error mapping, the thread pool serving it |
| ack deadline plumbing | whether the replacement client extends it, and up to what ceiling |
| route startup order | anything relying on Camel's start/stop sequencing |
| autocreate notifiers | see *Publish and consume are not symmetric* below |

Two of these bit antu specifically. The aggregator's `completionTimeout` was the only thing guaranteeing a
terminal status, and nothing in the replacement reproduced it until a sweeper was written for it. And
`master:` stopped its child route on leadership loss, which the `isLeader()` replacement does not — a gap the
design doc acknowledges and still has.

### Read the deployed config, not the code defaults

antu's design doc claimed "Camel retried three times in-process with exponential backoff". The code default
was 3; the deployed ConfigMap set `antu.camel.redelivery.max=0`. Production had no in-process retry at all,
so a whole paragraph of reasoning about what the migration lost was about something that never existed.

Diff the rendered ConfigMap for every environment before believing anything about current behaviour.

### Enumerate shared state and its serialized shape

For each cache, queue and blob: what writes it, what reads it, what serializes it, and what happens when the
old and new versions both touch it. antu's design doc listed three pieces of state the two versions could not
share. There were four. The fourth is the subject of the next section but one.

## Coordination is where it actually breaks

Camel gave antu two things that hid concurrency: an aggregator whose state lived in one pod's heap, and a
leader that most coordination ran through. Remove both and every step runs on an arbitrary pod, more than one
at a time, against at-least-once delivery.

The rules below are worth adopting up front rather than discovering. Each one corresponds to a bug that was
actually found in antu, mostly by review rather than by testing.

**A check followed by an act is not a guard.** Claim first, then read. In antu the barrier claims
`..._passed` with `setIfAbsent` *before* reading the arrival set, because several pods can see a full set at
the same instant; the claim is the guard, not the count.

**A claim taken before work that can fail must be released if the work fails.** In a `finally` covering
`Throwable`. Skipping this is worse than the duplicate the claim prevents: the redelivery finds a held claim,
does nothing, and the operation ends with no terminal state at all.

**Give every terminal outcome exactly one exit.** antu had `ok`/`failed` going through a guarded completer
while two `timeout` paths notified directly. That is how one of them ended up with neither the durable marker
nor the atomic claim, able to report a timeout for a dataset that had already reported success — which the
downstream service maps to a failed import. The fix was one guarded method that all three go through. If you
have two call sites for "tell the client we are done", you have a bug waiting.

**"Once" needs a durable guard and an atomic one, and they are not the same guard.** A blob marker survives
past any cache TTL and catches a redelivery an hour later. An atomic claim catches two deliveries in the same
instant, which the marker cannot, because both read it before either writes. Check the marker first, then
claim, so a redelivery can still retry a clean-up that failed.

**Read-modify-write on shared state needs a conditional write.** `put` resurrects an entry that a concurrent
clean-up just removed. Redisson's `replace` is a server-side conditional write, so a cleaned-up entry stays
cleaned up.

**`EXPIRE` belongs after the write.** On a key that does not exist yet it is a silent no-op, the TTL never
lands, and the key leaks. Assert the TTL in a test.

**A cached leadership flag is a decision from up to a heartbeat ago.** Check the lease deadline as well as the
flag, so a stop-the-world pause longer than the lease cannot leave two pods both believing they lead. Be
conservative: falsely saying "not leader" costs one skipped periodic task.

**Renewing a lease is one round trip.** `get` then `expire` lets the lease lapse in between and hands the new
owner an extension it never asked for. Use a Lua compare-and-expire — which requires the value be readable by
the script, so that one key needs a plain string codec rather than the client's binary one.

**Where the bugs actually were.** Worth internalising the ratio: of the coordination sites in antu, one was
designed correctly, three were found by code review before deploy, and two were found in production. Design
review is much cheaper than the alternative, and neither is optional.

## State in Redis is a wire format

This is the lesson that cost the most in production, and the one most likely to be missed.

### A schemaless codec cannot evolve

antu stores values through Redisson's `Kryo5Codec`, whose `FieldSerializer` writes fields positionally with
**no schema or version header**. The Camel removal grew one class from one field to three. Consequences, both
observed:

- **Old bytes, new code**: too few fields, the buffer underflows, `KryoBufferUnderflowException`. Because the
  read path scanned the whole hash, one stale entry aborted every scan. The stalled-validation sweeper — the
  only thing that concludes a stuck job — threw every five minutes for hours, logged only by Spring's
  scheduled-task error handler, never reaching the `System error` line the alert watches. The safety net was
  down and the service looked healthy.
- **New bytes, old code** (i.e. a rollback): too *many* fields. The first field is read off bytes that now
  belong to a different field, so you get a plausible wrong value and **no exception**. Quieter and worse.

**Fix: version the key.** `myCache` becomes `myCache.v2`. The two shapes never share a hash in either
direction, so no flush, no rollout window, and the old key can be deleted whenever. A defensive per-entry
catch, or switching to a schema-tolerant codec, only protects the forward direction.

Then make it enforceable, because nothing in the language will. `ValidationStateSerializationTest`
(`4d7b8b68`) reflects over the declared fields, compares them to a literal list, and asserts the cache name in
the same test, with a failure message telling you to bump the suffix. Sort fields by name, matching how Kryo
orders them, so a pure reordering is not a false positive. Pin nested types too: a record serialized *inside*
the value carries the same obligation and nobody thinks of it.

One more trap when changing these: `static final String` constants are inlined into call sites, so an
incremental build can leave the old value baked into referencing classes and pass. Verify with `mvn clean
verify`. This bit us during verification and would bite anyone reproducing it.

### A local-cached map is the wrong structure for read-modify-write

Redisson's `RLocalCachedMap` is a genuine optimisation for read-heavy access, and a trap for anything else.
Verified against Redisson 4.6.1:

- `RedissonLocalCachedMap` overrides `getAsync` and `containsKeyAsync` to serve from the **JVM-local** cache.
- It does *not* override the no-arg `entrySet()`, so iteration goes to Redis. Reads through the same map can
  therefore have different freshness depending on which method you call.
- Defaults are `syncStrategy = INVALIDATE` and **`reconnectionStrategy = NONE`**: invalidation is pub/sub
  only, and nothing re-syncs after a reconnect. A missed invalidation can leave a stale entry until the local
  TTL, which antu sets to an hour.

Two separate bugs came out of this:

- A distributed lock does **not** flush a local cache. A pod could take the lock immediately after another
  released it, read a stale local entry, and write back a union that dropped the other pod's contribution
  (`2d26c86f`). Fixed by reading through a non-cached `RMap` view of the same hash inside the lock.
- The sweeper's "re-read the state before concluding" safety check read `get()` — local, possibly stale —
  while the snapshot it was double-checking came from iteration — remote, fresh. The mitigation was reading
  the *staler* of the two sources, which the design doc described as narrowing the window.

If a value is accumulated across pods, prefer a server-side atomic operation over a lock plus
read-modify-write. antu already had an `RSet` doing exactly that a few lines away from the buggy method.

## Shutdown

The scheduler terminates pods as normal operation, so mid-work shutdown is a routine path, not an incident.
Three things default the wrong way and each has to be found separately:

1. **The consumer library's own wait is short.** `entur-google-pubsub` 7.2.0 stops subscribers from a
   `ContextClosedEvent` listener and waits `awaitTerminated(10, SECONDS)` per subscriber. For work that takes
   minutes, that is not a drain.
2. **`SmartLifecycle.stop()` is the correct hook**, not a `ContextClosedEvent` listener. It runs after those
   listeners have stopped the subscribers, so nothing new arrives, and before `destroyBeans()`, so Redis, the
   blob store and the publisher are still usable by the work draining.
   `spring.lifecycle.timeout-per-shutdown-phase` does **not** bound it, because `stop()` is invoked inline.
3. **The publisher's thread pool stops accepting work on `ContextClosedEvent`**, which fires *before* the
   drain. Left alone, work drains successfully and then cannot publish its result — the exact redelivery the
   drain exists to prevent, plus a delayed shutdown to pay for it.

**The grace-period budget is not just `drain < grace`.** That was the original sizing and it was wrong. The
subscriber close runs *first* and its 10 seconds is spent, not skipped, because the client will not terminate
while a callback is running. Budget is `10s x busy subscribers + drain + bean destruction`, against
`terminationGracePeriodSeconds`. antu had 175 against 180 and now has 150.

**Expect the drain never to fire in dev.** Across ten pod shutdowns, including eight simultaneously, antu's
drain logged nothing: the in-flight count was zero every time, because scale-downs happened to land in idle
windows. If you want this tested, delete a pod deliberately while it is working. Otherwise be honest that the
arithmetic is read off the jar rather than off a run.

## Publish and consume are not symmetric

Camel's autocreate notifier created a topic and subscription for **every** endpoint, including ones the
service only publishes to. Consumer-based replacements create only what they subscribe to. antu lost creation
of its outbound status topic this way, and the first publish against an uninitialised emulator failed with
`NOT_FOUND` (`e91ef220`).

Harmless in deployed environments, where destinations are terraformed and autocreate is off — which is
exactly why it survives review and surfaces in local development or a test.

## The removal release

**Remove nothing from helm or terraform in the release that removes the framework.** Not the RBAC, not the
old config keys, not the now-unused topics. This is a hard rule with a mechanical reason:

- helm and terraform apply **before** the first pod is replaced, so anything you delete is taken away from the
  version still serving.
- kubelet re-syncs a mounted ConfigMap into pods that are **still running**, so an old pod that restarts
  mid-rollout boots the old code against the new config.

antu got this wrong twice. First terraform resources were deleted while the comment beside them said to apply
only after the rollout. Then the rule was written down as satisfied while the ConfigMap keys had in fact been
deleted — including the one that made the old code's leader election work at all. Without it, camel-master
4.21.0 throws `IllegalStateException("No cluster service found")` on the first `master:` route and the
CamelContext fails to start. Verified by disassembling the jar.

Not every deleted key is fatal, and the non-fatal ones are still not free: one had a default of "fire once and
never again", so dropping it silently disabled a periodic refresh rather than falling back to a schedule.

**Know which rollback methods work before you need one.**

| Method | Works | Why |
| --- | --- | --- |
| `git revert` + full redeploy | yes | config, values, image and RBAC revert together |
| `helm rollback` | yes | restores config and image from one stored manifest, so the pair stays consistent |
| image-only rollback, `kubectl rollout undo` | **no** | restores the pod template only; the ConfigMap is a separate unversioned object referenced by name |

The fastest-looking option is the one that does not work. If the old config keys are kept, the third row
becomes survivable rather than a guaranteed CrashLoopBackOff — which is the real argument for keeping them.

**Deploy consumers of new values first.** antu's replacement introduced a status value no previous version had
sent. The downstream service logs and discards statuses it does not recognise, so until it shipped, every one
of those was silently dropped and the job was left with no terminal state. Watch for this being *merged* but
not *deployed*: it is easy to tick off the checklist item on the strength of a merged PR.

**Also plan the cutover for state that cannot be shared.** If old and new versions record progress
differently, a unit of work handled by both satisfies neither. Either quiesce the clients for the whole
rollout — not just at the start, since `maxSurge`/`maxUnavailable` means both versions serve for minutes — or
accept that in-flight work must be re-triggered.

## Autoscaling changes character

Worth re-deriving after the migration even if you do not touch the HPA. Under Camel, antu's aggregation ran on
leader-only queues the autoscaler never saw. Afterwards every pipeline step is a message on the queue the HPA
scales on, so the same thresholds mean something different.

antu's were pathological once measured: a target of `0.1` messages per pod means a *single* queued message
asks for `maxReplicas`, and a CPU target of 10% against a 1500m request is below what a working pod ever uses.
The HPA scales on the higher of its metrics, so fixing one alone changes nothing. Measured before the fix: **20
JVM starts in 37 minutes**, including eight pods stopped and eight started inside four minutes, for seven
units of work. Each start pays a JVM boot, framework startup and cold caches.

Scale-up and scale-down are not symmetric and should not be configured symmetrically. Scaling up is cheap and
the work is already queued; scaling down risks killing a pod mid-work, which costs the drain and possibly a
redelivery.

## Ordering guarantees you no longer have

If the Camel version relied on ordering, check what actually replaces it. antu queues files in an order chosen
so that a declaring file precedes a referencing one, and the code said this meant it "is validated before" it.
Measured on a dataset with 24 such files across ten pods: messages carry no ordering key, so delivery order is
not publish order, and the file published *first* started 19.5 seconds after another and finished **last of
the 24**.

It still produced correct results, on that dataset and two others. Both halves are worth writing down: the
mechanism does nothing above one consumer, *and* nothing observed suggests it needs to. Recording only the
first half invites someone to build a barrier that is not needed; recording only the second invites someone to
rely on ordering that is not there.

## Testing

**What the new suite got right.** Replacing per-scenario emulator boots with one in-JVM run of the whole
pipeline against embedded Redis, an in-memory blob store and a synchronously-drained queue was a clear win:
more tests, faster, and a stack trace that points at the failing step. Keep exactly one test that starts a
real emulator and have it check only the transport.

**Mutation-test every regression test for a concurrency fix.** Not optional, and cheap: revert the fix, run
the test, confirm it fails. antu's suite had twelve invariants worth pinning; six were genuinely protected and
six passed with the fix reverted, including a documented claim about a test that did not exist. Two specific
anti-patterns to look for:

- A test that constructs a race with threads and a latch, but where the losing thread's state makes the bug
  unreachable. PR #935's concurrency test passed against the unfixed code: both local caches started empty,
  and a cache miss falls through to Redis, so whichever pod took the lock second read fresh either way.
- A test that pins a contract by reading it through the same constant it is meant to pin.

**Prefer determinism to threads.** The replacement for that test uses no threads at all: it exploits the fact
that a plain `RMap` write publishes no invalidation, so the local cache can be left stale on purpose. If you
can construct the bad state directly, do that instead of racing for it.

**Watch for framework filters you lose by hand-rolling application config.** antu's test application replaced
`@SpringBootApplication`'s component scan and silently dropped `TypeExcludeFilter`, so every
`@TestConfiguration` in the codebase applied to every test that booted it, and one test's doubles replaced
another's.

## Verify claims against the artifact

More design-doc claims turned out false than anyone expected, and almost all of them were about *library
defaults*. Read the jar. `javap -c` on the class, or unzip the sources jar, takes a minute and settles it.

A sample of what was written down confidently and was wrong. All of these are now corrected in
`camel-removal.md`; the point of keeping the list is the pattern, not the individual entries:

| Claim | Actually |
| --- | --- |
| the sweeper's re-read narrows the race to one round trip | it reads the local cache, the staler of the two sources |
| all per-validation Redis keys carry a TTL | entries inside the local-cached maps carry none |
| both versions contend for the same lease during rollout | the old version used a different mechanism entirely; there are two leaders |
| Camel retried three times in-process | production had it configured to zero |
| the consumer setting only ever set the subscriber count | under Camel it genuinely bounded concurrency; that changed with the client |
| a Sonar finding is blocked on the formatter being unable to parse it | a version that parses it exists; the real cost is a 134-file reformat |
| the queue order serialises work within a pod | delivery order is not publish order |
| three pieces of state cannot be shared across the rollout | four |

The same applies to production: the logs settle questions that reasoning cannot. Cross-check the deployed pod
spec, the rendered config, the actual backlog metric and the actual pod churn. Several of the findings in this
document came from reading a metric time series rather than code.

## Do not bundle the runtime upgrade

antu's removal also carried a parent-POM bump, a JDK major upgrade and a Google client library bump. The
argument for pairing them was sound — the framework removal was what made the upgrade viable — but it was one
squashed commit, so the two could not be reverted independently. If the runtime regresses in production the
only lever is reverting the whole rewrite, including the downstream service that was deployed first to
understand its new output.

Keep them as separate commits on the same branch even if they merge together, and deploy them as two rollouts,
so an unexplained problem has one candidate cause rather than two.

A related trap: a previous, reverted attempt at the same JDK upgrade had added JVM flags that the successful
attempt dropped. One was load-bearing — a serialization library calls `sun.misc.Unsafe`, and the JDK's default
for that is `warn` today and will become `deny`. Tested on the pinned JDK: records survive `deny`, plain
objects do not. It works until an unattended base image bump, and by then nothing in CI starts the image.
**When you redo a reverted change, diff against the reverted attempt, not just against main.**

## Checklist

Before writing code:

- [ ] wire contract enumerated and pinned by literal-asserting tests
- [ ] Camel implicit behaviours inventoried route by route
- [ ] rendered config diffed per environment, not code defaults
- [ ] every shared state item listed with its serializer and its cross-version behaviour

While writing:

- [ ] one exit for each terminal outcome, guarded by a durable marker and an atomic claim
- [ ] claims released on `Throwable`
- [ ] conditional writes for read-modify-write; `EXPIRE` after the write
- [ ] no local-cached map behind a lock or a read-modify-write
- [ ] serialized shapes coupled to a versioned key by a test
- [ ] drain sized against the whole shutdown budget, not just the grace period
- [ ] publish-only destinations created explicitly

Before the release:

- [ ] nothing removed from helm or terraform
- [ ] rollback method chosen and known to work
- [ ] downstream consumers of new values deployed *and rolled out* first
- [ ] autoscaling thresholds re-derived against the new message pattern
- [ ] concurrency tests mutation-tested against the unfixed code
- [ ] runtime upgrade separable from the migration

After deploying:

- [ ] scheduled tasks confirmed to be *running*, not merely not throwing
- [ ] pod churn and backlog metrics read, not assumed
- [ ] drain exercised by deleting a busy pod on purpose

## What we would do differently

**Assume the design doc is wrong in the same proportion as the code.** antu's was unusually good and was still
wrong in eight places, mostly about library behaviour. Marking each claim as measured or assumed would have
directed review at the assumptions.

**Write the state-compatibility test before the state change.** The Kryo incident was foreseen in review, filed
as a should-fix, and then dropped from the plan. It cost hours of a dead safety net in production. The class of
bug is obvious once named, and the test that prevents it is twenty lines.

**Treat "the pipeline works" as weak evidence.** antu validated ten datasets correctly while its stalled-work
safety net had been dead for hours and its autoscaler was thrashing. The happy path exercises almost none of
what a migration puts at risk. Check the safety nets explicitly, because by construction they only run when
something else has already gone wrong.
