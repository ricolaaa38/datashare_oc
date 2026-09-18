package com.openclassroom.datashare.integration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Critical path: an authenticated user uploads a document, finds it in their
 * history, updates and deletes it. Also covers the anonymous upload route and
 * the rule that forbids an authenticated caller from using it.
 */
class FileUploadIntegrationTest extends AbstractIntegrationTest {

    @Test
    void anAuthenticatedUserCanUploadAFileAndFindItInTheirHistory() throws Exception {
        String token = registerAndLogin("uploader@datashare.test", "longenoughpassword");
        MockMultipartFile document = new MockMultipartFile("file", "report.pdf", "application/pdf",
                "hello world".getBytes(StandardCharsets.UTF_8));

        String uploadResponse = mockMvc.perform(multipart("/files")
                .file(document)
                .param("expiresInDays", "3")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalName").value("report.pdf"))
                .andExpect(jsonPath("$.downloadToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long fileId = objectMapper.readTree(uploadResponse).get("fileId").asLong();

        mockMvc.perform(get("/files").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].fileId").value(fileId));
    }

    @Test
    void anUploadedFileCanBeRenamedAndThenDeleted() throws Exception {
        String token = registerAndLogin("owner@datashare.test", "longenoughpassword");
        long fileId = uploadDocument(token, "original.pdf");

        mockMvc.perform(patch("/files/{fileId}", fileId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"originalName\":\"renamed.pdf\"}")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName").value("renamed.pdf"));

        mockMvc.perform(delete("/files/{fileId}", fileId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/files/{fileId}", fileId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void anAuthenticatedCallerCannotUseTheAnonymousUploadEndpoint() throws Exception {
        String token = registerAndLogin("authenticated@datashare.test", "longenoughpassword");
        MockMultipartFile document = new MockMultipartFile("file", "report.pdf", "application/pdf",
                "hello world".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/anonymous/files")
                .file(document)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnauthenticatedCallerCanUploadAnonymously() throws Exception {
        MockMultipartFile document = new MockMultipartFile("file", "anonymous.pdf", "application/pdf",
                "hello world".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/anonymous/files").file(document))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").doesNotExist());
    }

    private long uploadDocument(String token, String originalName) throws Exception {
        MockMultipartFile document = new MockMultipartFile("file", originalName, "application/pdf",
                "hello world".getBytes(StandardCharsets.UTF_8));

        String response = mockMvc.perform(multipart("/files")
                .file(document)
                .param("expiresInDays", "3")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("fileId").asLong();
    }
}
