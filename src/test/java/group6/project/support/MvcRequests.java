package group6.project.support;

import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.*;
import group6.project.config.CsrfProtection;

public final class MvcRequests {
    // Test clients follow the same session-token contract as a rendered browser form.
    private MvcRequests() {}

    // Attach a valid token after the builder's chosen session has been applied.
    private static RequestPostProcessor formToken() {
        return request -> {
            var session = request.getSession(false);
            if (session == null) { session = new MockHttpSession(); request.setSession(session); }
            request.setParameter(CsrfProtection.FIELD, CsrfProtection.token(session));
            return request;
        };
    }

    // Existing POST tests keep their business assertions and submit a normal valid form.
    public static MockHttpServletRequestBuilder post(String url, Object... variables) {
        return MockMvcRequestBuilders.post(url, variables).with(formToken());
    }

    // Multipart tests use a field token just like the upload page.
    public static MockMultipartHttpServletRequestBuilder multipart(String url, Object... variables) {
        return (MockMultipartHttpServletRequestBuilder) MockMvcRequestBuilders.multipart(url, variables).with(formToken());
    }
}
