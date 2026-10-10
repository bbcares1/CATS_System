package group6.project;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public final class TestRequests {
    private TestRequests() {}

    // Business-flow tests use a valid token; CsrfIntegrationTest checks missing and forged tokens.
    public static MockHttpServletRequestBuilder post(String path, Object... values) {
        return MockMvcRequestBuilders.post(path, values)
                .with(
                        request -> {
                            request.getSession().setAttribute("csrfToken", "test-token");
                            request.setParameter("_csrf", "test-token");
                            return request;
                        });
    }

    // Multipart forms use the same session token as ordinary forms.
    public static MockMultipartHttpServletRequestBuilder multipart(String path, Object... values) {
        MockMultipartHttpServletRequestBuilder request =
                MockMvcRequestBuilders.multipart(path, values);
        request.with(
                value -> {
                    value.getSession().setAttribute("csrfToken", "test-token");
                    value.setParameter("_csrf", "test-token");
                    return value;
                });
        return request;
    }
}
