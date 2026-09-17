package org.example.murderhelp.domain.search.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.example.murderhelp.domain.search.dto.PopularSearchResponse;
import org.example.murderhelp.domain.search.service.PopularSearchService;
import org.example.murderhelp.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private PopularSearchService popularSearchService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SearchController searchController;

    @Test
    void 입력이_끝난_검색어만_인기_검색어로_기록한다() {
        when(authentication.getName()).thenReturn("member-1");

        ResponseEntity<ApiResponse<Void>> response = searchController.recordPopularSearch(authentication, "권총");

        verify(popularSearchService).recordSearch("member-1", "권총");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getCode());
    }

    @Test
    void 인기_검색어_조회는_ResponseEntity로_성공_응답을_반환한다() {
        List<PopularSearchResponse> searches = List.of(new PopularSearchResponse(1, "권총", 3));
        when(popularSearchService.getPopularSearches(10)).thenReturn(searches);

        ResponseEntity<ApiResponse<List<PopularSearchResponse>>> response = searchController.getPopularSearches(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(searches, response.getBody().getData());
    }
}
