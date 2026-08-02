package com.example.filtertest.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes a {@code Flux<T>} as a JSON array while batching many elements into each emitted
 * {@link DataBuffer} instead of one buffer (and therefore one network flush) per element.
 *
 * <p>Spring's default streaming JSON encoder flushes once per element, which is negligible at
 * normal result sizes but becomes the dominant cost at millions of elements: the flush/write
 * syscall count, not the byte volume or the database, ends up limiting throughput. Batching
 * trades a little latency on the last few rows of each batch for roughly a batch-size reduction
 * in flush count, while still never holding more than one batch in memory at a time.
 */
@Component
class JsonArrayBodyWriter {

    private static final int BATCH_SIZE = 500;
    private static final byte[] OPEN_BRACKET = "[".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CLOSE_BRACKET = "]".getBytes(StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper;

    JsonArrayBodyWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    <T> Flux<DataBuffer> writeArray(Flux<T> elements, DataBufferFactory bufferFactory) {
        Flux<DataBuffer> batches = elements.index()
                .buffer(BATCH_SIZE)
                .map(batch -> encodeBatch(batch, bufferFactory));

        // defaultIfEmpty only substitutes on a normal empty completion, never on error, so an
        // upstream failure before the first batch still propagates with nothing written yet -
        // preserving the ability to map it to a non-200 status via GlobalExceptionHandler.
        return batches
                .defaultIfEmpty(bufferFactory.wrap(OPEN_BRACKET))
                .concatWith(Mono.fromSupplier(() -> bufferFactory.wrap(CLOSE_BRACKET)));
    }

    private <T> DataBuffer encodeBatch(List<Tuple2<Long, T>> batch, DataBufferFactory bufferFactory) {
        StringBuilder json = new StringBuilder();
        for (Tuple2<Long, T> indexed : batch) {
            json.append(indexed.getT1() == 0 ? '[' : ',').append(objectMapper.writeValueAsString(indexed.getT2()));
        }
        return bufferFactory.wrap(json.toString().getBytes(StandardCharsets.UTF_8));
    }
}
