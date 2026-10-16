package ru.practicum.ewm.stats.aggregator.kafka.user;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public class UserActionAvroDeserializer implements Deserializer<UserActionAvro> {

    @Override
    public UserActionAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            SpecificDatumReader<UserActionAvro> reader =
                    new SpecificDatumReader<>(UserActionAvro.class);

            BinaryDecoder decoder = DecoderFactory.get()
                    .binaryDecoder(data, null);

            return reader.read(null, decoder);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to deserialize UserActionAvro",
                    e
            );
        }
    }
}
