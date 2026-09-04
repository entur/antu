package no.entur.antu.netex.test.builder;

import java.math.BigInteger;
import org.rutebanken.netex.model.LinkInJourneyPattern;
import org.rutebanken.netex.model.ServiceLinkRefStructure;

public class LinkInJourneyPatternBuilder
  extends EntityBuilder<LinkInJourneyPattern> {

  private int order = 1;
  private ServiceLinkRefStructure serviceLinkRef;

  public LinkInJourneyPatternBuilder(int id) {
    super("LinkInJourneyPattern", id);
  }

  public LinkInJourneyPatternBuilder withOrder(int order) {
    this.order = order;
    return this;
  }

  public LinkInJourneyPatternBuilder withServiceLinkRef(
    ServiceLinkRefStructure serviceLinkRef
  ) {
    this.serviceLinkRef = serviceLinkRef;
    return this;
  }

  @Override
  public LinkInJourneyPattern build() {
    return new LinkInJourneyPattern()
      .withId(ref())
      .withOrder(BigInteger.valueOf(order))
      .withServiceLinkRef(serviceLinkRef);
  }
}
