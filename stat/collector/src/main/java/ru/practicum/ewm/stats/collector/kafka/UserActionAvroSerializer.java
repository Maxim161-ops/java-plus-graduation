package ru.practicum.ewm.stats.collector.kafka;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayOutputStream;

public class UserActionAvroSerializer implements Serializer<UserActionAvro> {

    @Override
    public byte[] serialize(String topic, UserActionAvro data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            SpecificDatumWriter<UserActionAvro> writer =
                    new SpecificDatumWriter<>(UserActionAvro.class);

            BinaryEncoder encoder = EncoderFactory.get()
                    .binaryEncoder(outputStream, null);

            writer.write(data, encoder);
            encoder.flush();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize UserActionAvro", e);
        }
    }
}
