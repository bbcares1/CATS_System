package group6.project;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CsrfIntegrationTest {
    @Autowired MockMvc mvc;

    // Login pages issue a token and reject a missing or foreign token before authentication.
    @Test
    void formsRequireTheirOwnSessionToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/employee/login").session(session)).andExpect(status().isOk());
        String token = (String) session.getAttribute("csrfToken");
        assertNotNull(token);
        mvc.perform(
                        post("/employee/login")
                                .session(session)
                                .param("userName", "none")
                                .param("password", "none"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/employee/login")
                                .session(session)
                                .param("_csrf", "forged")
                                .param("userName", "none")
                                .param("password", "none"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/employee/login")
                                .session(session)
                                .param("_csrf", token)
                                .param("userName", "none")
                                .param("password", "none"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    // A link cannot log users out, while a valid form invalidates the session.
    @Test
    void logoutRequiresPostAndToken() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("csrfToken", "known");
        mvc.perform(get("/logout").session(session)).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).param("_csrf", "known"))
                .andExpect(redirectedUrl("/login"));
        assertTrue(session.isInvalid());
    }
}
