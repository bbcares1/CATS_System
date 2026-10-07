package group6.project.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import group6.project.model.Admin;
import group6.project.model.Manager;
import group6.project.model.Staff;

@WebMvcTest(ManagerController.class)
class ManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousVisitorIsRedirectedToEmployeeLogin() throws Exception {
        mockMvc.perform(get("/manager/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/login"));
    }

    @ParameterizedTest
    @MethodSource("nonManagerUsers")
    void nonManagerSessionCannotOpenManagerWorkspace(Object user) throws Exception {
        mockMvc.perform(get("/manager/home").sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/login"));
    }

    static Stream<Object> nonManagerUsers() {
        return Stream.of(new Staff(), new Admin(), "mgr_bob");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/manager", "/manager/home"})
    void signedInManagerReceivesRenderedDashboard(String path) throws Exception {
        Manager manager = new Manager();
        manager.setName("Bob & Team");
        manager.setStaffId("M001");

        mockMvc.perform(get(path).sessionAttr("user", manager))
                .andExpect(status().isOk())
                .andExpect(view().name("manager-home"))
                .andExpect(model().attribute("currentUser", manager))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Bob &amp; Team")))
                .andExpect(content().string(containsString("M001")))
                .andExpect(content().string(containsString("href=\"/staff/home\"")))
                .andExpect(content().string(containsString("href=\"/logout\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/managers", "/api/managers/1", "/api/managers/staff-id/M001"})
    void retiredApiRoutesAreNotExposed(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound());
    }
}
