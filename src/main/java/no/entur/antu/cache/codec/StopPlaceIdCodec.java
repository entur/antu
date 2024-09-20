package no.entur.antu.cache.codec;

import io.netty.buffer.ByteBuf;
import no.entur.antu.model.StopPlaceId;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;

/**
 * Codec for StopPlaceId.
 * This codec is used to decode StopPlaceId from Redis cache.
 * Encoding is done by the toString method in StopPlaceId.
 */
public class StopPlaceIdCodec extends StringCodec {

  @Override
  public Decoder<Object> getValueDecoder() {
    return (ByteBuf buf, State state) -> {
      String str = (String) super.getValueDecoder().decode(buf, state);
      return new StopPlaceId(str);
    };
  }
}
