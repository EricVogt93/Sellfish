package de.sellfish.docs.adapter.web;

import de.sellfish.common.error.ApiException;
import de.sellfish.common.security.CurrentUser;
import de.sellfish.docs.*;
import de.sellfish.docs.DocumentDtos.DocumentResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocumentResponse> list() {
        return service.list(CurrentUser.id()).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@RequestParam("type") DocumentType type, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("File missing");
        }
        try {
            Document doc = service.upload(
                    CurrentUser.id(), type, file.getOriginalFilename(), file.getContentType(), file.getBytes());
            return DocumentResponse.from(doc);
        } catch (IOException e) {
            throw ApiException.badRequest("File not readable");
        }
    }

    @PostMapping("/{id}/parse")
    public DocumentResponse reparse(@PathVariable UUID id) {
        service.reparse(CurrentUser.id(), id);
        return DocumentResponse.from(service.list(CurrentUser.id()).stream()
                .filter(d -> d.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Document not found")));
    }

    @PostMapping("/{id}/primary")
    public DocumentResponse setPrimary(@PathVariable UUID id) {
        return DocumentResponse.from(service.setPrimary(CurrentUser.id(), id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        DocumentService.DownloadedFile file = service.download(CurrentUser.id(), id);
        MediaType mediaType =
                file.mime() != null ? MediaType.parseMediaType(file.mime()) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .body(new ByteArrayResource(file.content()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(CurrentUser.id(), id);
    }
}
