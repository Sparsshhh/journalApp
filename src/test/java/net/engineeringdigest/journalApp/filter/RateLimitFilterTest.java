package net.engineeringdigest.journalApp.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(3, 60);

    @Test
    void allowsUpToTheLimitThenBlocksUntilTheWindowEnds() {
        long t0 = 1_000_000;
        assertTrue(filter.tryAcquire("1.1.1.1", t0));
        assertTrue(filter.tryAcquire("1.1.1.1", t0 + 1000));
        assertTrue(filter.tryAcquire("1.1.1.1", t0 + 2000));
        assertFalse(filter.tryAcquire("1.1.1.1", t0 + 3000));
        assertFalse(filter.tryAcquire("1.1.1.1", t0 + 59_999));
        assertTrue(filter.tryAcquire("1.1.1.1", t0 + 60_000));
    }

    @Test
    void clientsAreCountedSeparately() {
        for (int i = 0; i < 3; i++) {
            assertTrue(filter.tryAcquire("1.1.1.1", 0));
        }
        assertFalse(filter.tryAcquire("1.1.1.1", 0));
        assertTrue(filter.tryAcquire("2.2.2.2", 0));
    }

    private MockHttpServletResponse call(String method, String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setRemoteAddr("9.9.9.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void loginIsLimitedWith429AndRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(200, call("POST", "/public/login").getStatus());
        }
        MockHttpServletResponse blocked = call("POST", "/public/login");
        assertEquals(429, blocked.getStatus());
        assertEquals("60", blocked.getHeader("Retry-After"));
        // Sign-up and refresh share the same budget.
        assertEquals(429, call("POST", "/public/createUser").getStatus());
        assertEquals(429, call("POST", "/public/refresh").getStatus());
    }

    @Test
    void otherRoutesAreNeverLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertEquals(200, call("GET", "/public/health-check").getStatus());
            assertEquals(200, call("POST", "/journal").getStatus());
        }
    }
}
