package backend.cowrite.common.event.payload;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeleteOperation.class, name = "UPDATE"),
        @JsonSubTypes.Type(value = InsertOperation.class, name = "INSERT")
})
public interface Operation {
    OperationType getType();
}
