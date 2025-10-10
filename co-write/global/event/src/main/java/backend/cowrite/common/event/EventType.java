package backend.cowrite.common.event;

import backend.cowrite.common.event.payload.DocumentSavedEventPayload;
import backend.cowrite.common.event.payload.DocumentUpdatedEventPayload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {

    DOCUMENT_SAVE(DocumentSavedEventPayload.class,Topic.DOCUMENT_SAVE),
    DOCUMENT_UPDATE(DocumentUpdatedEventPayload.class,Topic.DOCUMENT_UPDATE);

    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static class Topic {
        public static final String DOCUMENT_UPDATE = "document-update";
        public static final String DOCUMENT_SAVE = "document-save";
    }
}
