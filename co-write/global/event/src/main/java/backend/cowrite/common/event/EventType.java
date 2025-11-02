package backend.cowrite.common.event;

import backend.cowrite.common.event.payload.DocumentDeleteEventPayload;
import backend.cowrite.common.event.payload.DocumentInsertEventPayload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {

    DELETE(DocumentDeleteEventPayload.class, Topic.DELETE),
    INSERT(DocumentInsertEventPayload.class, Topic.INSERT);

    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static class Topic {
        public static final String DELETE = "content-delete";
        public static final String INSERT = "content-insert";
    }
}
