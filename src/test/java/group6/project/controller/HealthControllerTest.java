// We check the small deployment health endpoint.
package group6.project.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import group6.project.service.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
class HealthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean JdbcTemplate database;
    @MockitoBean UserService users;

    // A working database gives the container a successful readiness response.
    @Test
    void healthyDatabaseReturnsUp() throws Exception {
        when(database.queryForObject("select 1", Integer.class)).thenReturn(1);
        mvc.perform(get("/health")).andExpect(status().isOk()).andExpect(content().string("UP"));
    }

    // Database errors produce a failing probe without exposing internal connection details.
    @Test
    void unavailableDatabaseReturnsDown() throws Exception {
        when(database.queryForObject("select 1", Integer.class))
                .thenThrow(
                        new org.springframework.dao.DataAccessResourceFailureException(
                                "private database detail"));
        mvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("DOWN"));
    }
}
