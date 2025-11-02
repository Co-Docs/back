package backend.cowrite.common.event;

import backend.cowrite.common.event.payload.DocumentEventPayload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {

    UPDATE(DocumentEventPayload.class, Topic.UPDATE);

    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static class Topic {
        public static final String UPDATE = "content-update";
    }
    }
