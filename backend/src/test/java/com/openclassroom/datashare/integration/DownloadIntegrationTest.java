package com.openclassroom.datashare.integration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Critical path: a recipient previews a shared file, then downloads it,
 * password included when the file is protected.
 */
class DownloadIntegrationTest extends AbstractIntegrationTest {

    private static final byte[] CONTENT = "hello world".getBytes(StandardCharsets.UTF_8);

    @Test
    void aPasswordProtectedFileCanOnlyBeDownloadedWithTheRightPassword() throws Exception {
        String token = registerAndLogin("sharer@datashare.test", "longenoughpassword");
        String downloadToken = uploadProtectedDocument(token, "secret1");
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(CONTENT))));

        mockMvc.perform(get("/downloads/{token}/metadata", downloadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPassword").value(true));

        mockMvc.perform(get("/downloads/{token}", downloadToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/downloads/{token}", downloadToken).header("X-File-Password", "wrong"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/downloads/{token}", downloadToken).header("X-File-Password", "secret1"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(CONTENT));
    }

    @Test
    void anUnknownDownloadTokenIsReportedAsNotFound() throws Exception {
        mockMvc.perform(get("/downloads/{token}/metadata", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    private String uploadProtectedDocument(String token, String password) throws Exception {
        MockMultipartFile document = new MockMultipartFile("file", "report.pdf", "application/pdf", CONTENT);

        String response = mockMvc.perform(multipart("/files")
                .file(document)
                .param("expiresInDays", "3")
                .param("password", password)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("downloadToken").asText();
    }
}
