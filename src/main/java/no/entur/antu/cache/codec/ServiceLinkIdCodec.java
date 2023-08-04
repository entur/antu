package no.entur.antu.cache.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;
import no.entur.antu.model.ServiceLinkId;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

/**
 * Codec for ServiceLinkId.
 * This codec is used to decode ScheduledStopPointId from Redis cache.
 * Encoding is done by the toString method in ScheduledStopPointId.
 */
public class ServiceLinkIdCodec extends StringCodec {

  @Override
  public Decoder<Object> getValueDecoder() {
    return (ByteBuf buf, State state) -> {
      String str = (String) super.getValueDecoder().decode(buf, state);
      return new ServiceLinkId(str);
    };
  }

  @Override
  public Encoder getValueEncoder() {
    return (Object in) -> {
      if (in instanceof ServiceLinkId id) {
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
        out.writeCharSequence(id.id(), CharsetUtil.UTF_8);
        return out;
      }
      return null;
    };
  }
}
