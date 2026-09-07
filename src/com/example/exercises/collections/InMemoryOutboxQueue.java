package com.example.exercises.collections;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryOutboxQueue {
    private final Deque<OutboxEvent> events = new ArrayDeque<>();

    // Добавление нового события в конец.
    public void addNewEvent(OutboxEvent event) {
        Objects.requireNonNull(event, "event should be defined");
        events.addLast(event);
    }

    // Получение события из начала.
    public Optional<OutboxEvent> takeNext() {
        return Optional.ofNullable(events.pollFirst());
    }

    // Возврат события в начало после ошибки.
    public void returnEventAfterError(OutboxEvent event) {
        Objects.requireNonNull(event, "event should be defined");
        events.addLast(event);
    }

    public int size() {
        return events.size();
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }
}
