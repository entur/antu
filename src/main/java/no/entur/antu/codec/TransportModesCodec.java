package no.entur.antu.codec;

import io.netty.buffer.ByteBuf;
import no.entur.antu.model.TransportModes;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;

/**
 * Codec for TransportModes.
 * This codec is used to decode TransportModes from Redis cache.
 * Encoding is done by the toString method in TransportModes.
 */
public class TransportModesCodec extends StringCodec {

  @Override
  public Decoder<Object> getValueDecoder() {
    return (ByteBuf buf, State state) ->
      TransportModes.fromString(
        String.valueOf(super.getValueDecoder().decode(buf, state))
      );
  }
}
