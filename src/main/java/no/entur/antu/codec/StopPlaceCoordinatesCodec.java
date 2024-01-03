package no.entur.antu.codec;

import io.netty.buffer.ByteBuf;
import no.entur.antu.model.StopPlaceCoordinates;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.springframework.stereotype.Component;

@Component
public class StopPlaceCoordinatesCodec extends StringCodec {
    @Override
    public Decoder<Object> getValueDecoder() {
        return (ByteBuf buf, State state) -> StopPlaceCoordinates.fromString(
                String.valueOf(super.getValueDecoder().decode(buf, state))
        );
    }
}

