package de.sellfish.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void allowsUpToAuthLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("10.0.0.1");
            req.setRequestURI("/api/auth/login");
            MockHttpServletResponse res = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            filter.doFilterInternal(req, res, chain);
            assertThat(res.getStatus()).isNotEqualTo(429);
            verify(chain).doFilter(any(), any());
        }
    }

    @Test
    void blocksAfterAuthLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.2");
        req.setRequestURI("/api/auth/login");
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(req, new MockHttpServletResponse(), mock(FilterChain.class));
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(req, res, chain);
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isEqualTo("60");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void apiLimitHigherThanAuth() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.3");
        req.setRequestURI("/api/matches");
        for (int i = 0; i < 50; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilterInternal(req, res, mock(FilterChain.class));
            assertThat(res.getStatus()).isNotEqualTo(429);
        }
    }

    @Test
    void differentIpsHaveSeparateBuckets() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setRemoteAddr("10.0.0.10");
            req.setRequestURI("/api/auth/login");
            filter.doFilterInternal(req, new MockHttpServletResponse(), mock(FilterChain.class));
        }
        // different IP → separate bucket, should not be limited
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setRemoteAddr("10.0.0.99");
        req2.setRequestURI("/api/auth/login");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(req2, res2, chain);
        assertThat(res2.getStatus()).isNotEqualTo(429);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void respectsForwardedForHeader() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.1");
        req.setRequestURI("/api/auth/login");
        // exhaust the forwarded IP bucket
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(req, new MockHttpServletResponse(), mock(FilterChain.class));
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, mock(FilterChain.class));
        assertThat(res.getStatus()).isEqualTo(429);
    }
}
