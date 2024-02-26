package no.entur.antu.codec;

import io.netty.buffer.ByteBuf;
import no.entur.antu.model.QuayId;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;

/**
 * Codec for QuayId.
 * This codec is used to decode QuayId from Redis cache.
 * Encoding is done by the toString method in QuayId.
 */
public class QuayIdCodec extends StringCodec {

  @Override
  public Decoder<Object> getValueDecoder() {
    return (ByteBuf buf, State state) -> {
      String str = (String) super.getValueDecoder().decode(buf, state);
      return new QuayId(str);
    };
  }
}
