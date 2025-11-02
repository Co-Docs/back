package backend.cowrite.publisher.response;

import backend.cowrite.entity.Document;

import java.util.List;

public record DocumentDetailResponse(
        String title, String content, List<UserDocumentDto> userDocuments
) {

    private static record UserDocumentDto(String nickname){

    }

    public static DocumentDetailResponse of(Document document){
        return new DocumentDetailResponse(document.getTitle(),
                document.getContent(),
                document.getUserDocuments().stream().map(userDocument -> new UserDocumentDto(userDocument.getUser().getNickname())).toList());
    }
}