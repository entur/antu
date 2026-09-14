package no.entur.antu.netex.test.builder;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.OperatorRefStructure;
import org.rutebanken.netex.model.TransportSubmodeStructure;

/**
 * The properties a Line and a FlexibleLine share.
 *
 * <p>Subclasses register themselves into the index rather than being dispatched on by type, so
 * adding a third kind of line does not mean editing NetexTestData.
 */
public abstract class GenericLineBuilder<T extends Line_VersionStructure>
  extends EntityBuilder<T> {

  protected AllVehicleModesOfTransportEnumeration transportMode;
  protected TransportSubmodeStructure transportSubmode;
  protected OperatorRefStructure operatorRef;

  protected GenericLineBuilder(String entityType, int id) {
    super(entityType, id);
  }

  public GenericLineBuilder<T> withTransportMode(
    AllVehicleModesOfTransportEnumeration transportMode
  ) {
    this.transportMode = transportMode;
    return this;
  }

  public GenericLineBuilder<T> withTransportSubmode(
    TransportSubmodeStructure transportSubmode
  ) {
    this.transportSubmode = transportSubmode;
    return this;
  }

  public GenericLineBuilder<T> withOperatorRef(
    OperatorRefStructure operatorRef
  ) {
    this.operatorRef = operatorRef;
    return this;
  }

  /**
   * Put the built line into whichever index of the entities index it belongs in.
   */
  public abstract void registerInto(NetexEntitiesIndex netexEntitiesIndex);
}
