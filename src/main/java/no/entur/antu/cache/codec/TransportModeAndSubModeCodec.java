package no.entur.antu.cache.codec;

import io.netty.buffer.ByteBuf;
import no.entur.antu.model.TransportModeAndSubMode;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;

/**
 * Codec for TransportModeAndSubMode.
 * This codec is used to decode TransportModes from Redis cache.
 * Encoding is done by the toString method in TransportModes.
 */
public class TransportModeAndSubModeCodec extends StringCodec {

  @Override
  public Decoder<Object> getValueDecoder() {
    return (ByteBuf buf, State state) ->
      TransportModeAndSubMode.fromString(
        String.valueOf(super.getValueDecoder().decode(buf, state))
      );
  }
}
