package com.example.rec.service;

import com.example.rec.model.Content;
import com.example.rec.model.User;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.TagRepository;
import com.example.rec.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KaggleImportServiceTest {

    @Mock
    private ContentRepository contentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TagRepository tagRepository;

    @Test
    void usesBuiltInLibraryWhenApiKeyIsMissing() {
        User author = new User();
        author.setId(1L);
        when(contentRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of(author));
        when(contentRepository.save(any(Content.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KaggleImportService service = new KaggleImportService(
                contentRepository, userRepository, tagRepository);
        ReflectionTestUtils.setField(service, "kaggleApiKey", "");

        KaggleImportService.ImportResult result = service.batchImport(1);

        assertTrue(result.success);
        assertEquals(1, result.importedCount);
    }
}
