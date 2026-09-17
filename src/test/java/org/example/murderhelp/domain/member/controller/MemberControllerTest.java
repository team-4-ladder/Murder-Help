package org.example.murderhelp.domain.member.controller;

import org.example.murderhelp.domain.member.dto.MemberResponse;
import org.example.murderhelp.domain.member.dto.UpdateMyInfoRequest;
import org.example.murderhelp.domain.member.service.MemberProfileService;
import org.example.murderhelp.global.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @InjectMocks
    private MemberController memberController;

    @Mock
    private MemberProfileService memberProfileService;

    @Test
    @DisplayName("내 회원 정보를 조회한다")
    void getMyInfo() {
        // given
        MemberResponse memberResponse = memberResponse();

        when(memberProfileService.getMyProfile(1L))
                .thenReturn(memberResponse);

        // when
        ResponseEntity<ApiResponse<MemberResponse>> result =
                memberController.getMyInfo(1L);

        // then
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo("SUCCESS");
        assertThat(result.getBody().getData()).isEqualTo(memberResponse);

        verify(memberProfileService).getMyProfile(1L);
    }

    @Test
    @DisplayName("내 이름과 전화번호를 수정한다")
    void updateMyInfo() {
        // given
        UpdateMyInfoRequest request =
                new UpdateMyInfoRequest("수정된 이름", "010-1234-5678");

        MemberResponse memberResponse = memberResponse();

        when(memberProfileService.updateMyProfile(1L, request))
                .thenReturn(memberResponse);

        // when
        ResponseEntity<ApiResponse<MemberResponse>> result =
                memberController.updateMyInfo(1L, request);

        // then
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo("SUCCESS");

        verify(memberProfileService).updateMyProfile(1L, request);
    }

    @Test
    @DisplayName("프로필 이미지를 업로드한다")
    void uploadProfileImage() {
        // given
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "profile.png",
                "image/png",
                "test-image".getBytes()
        );

        MemberResponse memberResponse = memberResponse();

        when(memberProfileService.uploadProfileImage(1L, image))
                .thenReturn(memberResponse);

        // when
        ResponseEntity<ApiResponse<MemberResponse>> result =
                memberController.uploadProfileImage(1L, image);

        // then
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo("SUCCESS");

        verify(memberProfileService).uploadProfileImage(1L, image);
    }

    @Test
    @DisplayName("프로필 이미지를 삭제하고 기본 이미지로 변경한다")
    void deleteProfileImage() {
        // given
        MemberResponse memberResponse = memberResponse();

        when(memberProfileService.deleteProfileImage(1L))
                .thenReturn(memberResponse);

        // when
        ResponseEntity<ApiResponse<MemberResponse>> result =
                memberController.deleteProfileImage(1L);

        // then
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody().getCode()).isEqualTo("SUCCESS");

        verify(memberProfileService).deleteProfileImage(1L);
    }

    private MemberResponse memberResponse() {
        return new MemberResponse(
                1L,
                "test@naver.com",
                "테스트회원",
                "010-0000-1234",
                "YELLOW",
                null
        );
    }
}