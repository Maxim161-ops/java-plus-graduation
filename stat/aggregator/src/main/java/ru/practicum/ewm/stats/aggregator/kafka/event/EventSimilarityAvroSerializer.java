package ru.practicum.ewm.stats.aggregator.kafka.event;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.ByteArrayOutputStream;

public class EventSimilarityAvroSerializer implements Serializer<EventSimilarityAvro> {

    @Override
    public byte[] serialize(String topic, EventSimilarityAvro data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            SpecificDatumWriter<EventSimilarityAvro> writer =
                    new SpecificDatumWriter<>(EventSimilarityAvro.class);

            BinaryEncoder encoder = EncoderFactory.get()
                    .binaryEncoder(outputStream, null);

            writer.write(data, encoder);
            encoder.flush();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to serialize EventSimilarityAvro",
                    e
            );
        }
    }
}
