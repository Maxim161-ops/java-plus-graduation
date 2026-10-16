package ru.practicum.ewm.stat.analyzer.kafka.similarity;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.IOException;

public class EventSimilarityAvroDeserializer
        implements Deserializer<EventSimilarityAvro> {

    @Override
    public EventSimilarityAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            SpecificDatumReader<EventSimilarityAvro> reader =
                    new SpecificDatumReader<>(EventSimilarityAvro.class);

            BinaryDecoder decoder = DecoderFactory
                    .get()
                    .binaryDecoder(data, null);

            return reader.read(null, decoder);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to deserialize EventSimilarityAvro", e);
        }
    }
}
