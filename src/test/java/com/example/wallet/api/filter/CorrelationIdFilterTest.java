package com.example.wallet.api.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "bad-id", "1-1-1-1-1", "00000000-0000-0000-0000-00000000000z"})
    void missingOrInvalidHeaderGeneratesIdBeforeChain(String header) throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        if (header != null) {
            request.addHeader(CorrelationIdFilter.HEADER_NAME, header);
        }

        filter.doFilter(request, response, (nextRequest, nextResponse) -> {
            String id = response.getHeader(CorrelationIdFilter.HEADER_NAME);
            assertNotNull(id);
            assertEquals(id, UUID.fromString(id).toString());
            assertEquals(id, request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME));
            assertNotEquals(header, id);
        });
    }

    @Test
    void validHeaderIsAvailableEvenWhenChainThrows() {
        String id = "AAAAAAAA-BBBB-4CCC-8DDD-EEEEEEEEEEEE";
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, id);

        assertThrows(ServletException.class, () -> filter.doFilter(request, response,
                (nextRequest, nextResponse) -> { throw new ServletException("test failure"); }));

        assertEquals(id, response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals(id, request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME));
    }

    @Test
    void separateRequestsGetDifferentGeneratedIds() throws Exception {
        var first = new MockHttpServletResponse();
        var second = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), first, (request, response) -> {});
        filter.doFilter(new MockHttpServletRequest(), second, (request, response) -> {});
        assertNotEquals(first.getHeader(CorrelationIdFilter.HEADER_NAME),
                second.getHeader(CorrelationIdFilter.HEADER_NAME));
    }
}
