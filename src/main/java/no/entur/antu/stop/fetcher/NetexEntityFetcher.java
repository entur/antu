package no.entur.antu.stop.fetcher;

public interface NetexEntityFetcher<R, S> {
    R tryFetch(S s);
}