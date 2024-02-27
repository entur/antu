package no.entur.antu.codec;

import io.netty.buffer.ByteBuf;
import no.entur.antu.model.QuayCoordinates;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;

/**
 * Codec for QuayCoordinates.
 * This codec is used to decode QuayCoordinates from Redis cache.
 * Encoding is done by the toString method in QuayCoordinates.
 */
public class QuayCoordinatesCodec extends StringCodec {

  @Override
  public Decoder<Object> getValueDecoder() {
    return (ByteBuf buf, State state) ->
      QuayCoordinates.fromString(
        String.valueOf(super.getValueDecoder().decode(buf, state))
      );
  }
}
