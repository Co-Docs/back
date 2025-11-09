package backend.cowrite.service.response;

import backend.cowrite.entity.Document;

import java.util.List;

public record DocumentPreviewResponse(
    List<DocumentDto> documents
) {

    private static record DocumentDto(Long documentId, String title, List<UserDocumentDto> userDocuments){


    }

    private static record UserDocumentDto(String nickname){

    }

    public static DocumentPreviewResponse of(List<Document> documents){
        return new DocumentPreviewResponse(
                documents
                .stream()
                .map(document ->
                        new DocumentDto(document.getDocumentId(), document.getTitle(), document.getUserDocuments().stream().map(userDocument -> new UserDocumentDto(userDocument.getUser().getNickname())).toList()))
                .toList());
    }
}
