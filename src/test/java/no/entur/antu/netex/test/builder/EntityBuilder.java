package no.entur.antu.netex.test.builder;

import org.rutebanken.netex.model.EntityStructure;

/**
 * Base class for the entity builders. Holds the entity's numeric id and derives its NeTEx id from
 * an entity type name the subclass passes in.
 *
 * <p>The type name is explicit rather than reflected off the generic superclass, because tests
 * across the repo assert on the resulting ids as string literals and a derivation that depends on
 * the class hierarchy breaks silently when the hierarchy changes.
 */
public abstract class EntityBuilder<T extends EntityStructure> {

  protected final int id;
  private final String entityType;

  protected EntityBuilder(String entityType, int id) {
    this.entityType = entityType;
    this.id = id;
  }

  /**
   * The NeTEx id this builder gives its entity: {@code TST:<EntityType>:<id>}.
   */
  public final String ref() {
    return "TST:" + entityType + ":" + id;
  }

  public abstract T build();
}
