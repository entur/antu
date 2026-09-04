package no.entur.antu.netex.test.builder;

import org.rutebanken.netex.model.VersionOfObjectRefStructure;

/**
 * A builder whose entity can be referred to by a typed NeTEx ref structure, so a test can point
 * one entity at another without spelling out the id.
 */
public interface Referenceable<R extends VersionOfObjectRefStructure> {
  R refObject();
}
