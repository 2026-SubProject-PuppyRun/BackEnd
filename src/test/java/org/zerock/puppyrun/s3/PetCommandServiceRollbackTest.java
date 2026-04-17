package org.zerock.puppyrun.s3;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.s3.S3Template;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;
import org.zerock.puppyrun.common.s3.S3Service;
import org.zerock.puppyrun.member.entity.UserRole;
import org.zerock.puppyrun.pet.entity.Pet;
import org.zerock.puppyrun.pet.repository.PetRepository;
import org.zerock.puppyrun.pet.service.PetCommandService;

@SpringBootTest
@ActiveProfiles("test")
class PetCommandServiceRollbackTest {

    @Autowired
    private PetCommandService petCommandService;

    // 실제 AOP 로직을 타야 하므로 @SpyBean 사용 (호출 여부 검증용)
    @SpyBean
    private S3Service s3Service;

    // 실제 AWS S3에 파일이 올라가거나 삭제되지 않도록 S3Template은 Mock 처리
    @MockBean
    private S3Template s3Template;

    // 예외를 강제로 발생시키기 위해 Repository를 Mock 처리
    @MockBean
    private PetRepository petRepository;

    @Test
    @DisplayName("S3 이미지 업로드 후, DB 업데이트 중 예외가 발생하면 트랜잭션이 롤백되고 S3 삭제 로직이 실행되어야 한다.")
    void s3UploadRollbackTest() {
        // given
        UUID memberId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UserPrincipal userPrincipal = new UserPrincipal(memberId, "test@test.com", UserRole.USER);

        MockMultipartFile mockFile = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "image data".getBytes()
        );

        // Pet 객체 Mocking
        Pet mockPet = mock(Pet.class);
        when(petRepository.findByIdAndVerifyOwnership(petId, memberId)).thenReturn(mockPet);

        // S3Template의 실제 업로드 기능 비활성화 (아무 동작도 하지 않음)
        when(s3Template.upload(any(), any(), any(), any())).thenReturn(null);

        // S3 업로드는 성공했지만, 그 직후 DB 정보 업데이트 과정에서 강제로 예외 발생 유도
        doThrow(new RuntimeException("강제 롤백 유발"))
                .when(mockPet).updateProfile(anyString());

        // when & then
        // 예외가 정상적으로 던져지는지 검증
        assertThatThrownBy(() -> petCommandService.updatePetProfile(userPrincipal, petId, mockFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("강제 롤백 유발");

        // 트랜잭션 롤백에 의해 S3RollbackHandler가 동작하여 s3Service.delete()를 호출했는지 검증
        // 업로드 시 생성된 정확한 Key 경로로 1회 호출되어야 함
        verify(s3Service, times(1)).delete(anyString());
    }
}
