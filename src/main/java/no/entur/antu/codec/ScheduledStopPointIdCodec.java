package no.entur.antu.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;
import no.entur.antu.model.ScheduledStopPointId;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

/**
 * Codec for ScheduledStopPointId.
 * This codec is used to decode ScheduledStopPointId from Redis cache.
 * Encoding is done by the toString method in ScheduledStopPointId.
 */
public class ScheduledStopPointIdCodec extends StringCodec {

  @Override
  public Decoder<Object> getValueDecoder() {
    return (ByteBuf buf, State state) -> {
      String str = (String) super.getValueDecoder().decode(buf, state);
      return new ScheduledStopPointId(str);
    };
  }

  @Override
  public Encoder getValueEncoder() {
    return (Object in) -> {
      if (in instanceof ScheduledStopPointId id) {
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
        out.writeCharSequence(id.id(), CharsetUtil.UTF_8);
        return out;
      }
      return null;
    };
  }
}
