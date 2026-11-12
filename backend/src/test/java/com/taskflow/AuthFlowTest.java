package com.taskflow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {
    @Autowired MockMvc mvc;

    /** Registers a fresh user, logs in, and returns the JWT. */
    private String tokenFor(String username) throws Exception {
        String creds = "{\"username\":\"" + username + "\",\"password\":\"secret123\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(creds))
           .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(creds))
           .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private ResultActions createTask(String token, String json) throws Exception {
        return mvc.perform(post("/api/tasks").header("Authorization", "Bearer " + token)
           .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void registerLoginCreateTask() throws Exception {
        String token = tokenFor("alice");
        createTask(token, "{\"title\":\"Write docs\"}").andExpect(status().isCreated());
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
    }

    @Test
    void taskKeepsPriorityAndDueDate() throws Exception {
        String token = tokenFor("bob");
        createTask(token, "{\"title\":\"Pay rent\",\"priority\":\"HIGH\",\"dueDate\":\"2099-01-01\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.dueDate").value("2099-01-01"));
    }

    @Test
    void priorityDefaultsToMedium() throws Exception {
        String token = tokenFor("carol");
        createTask(token, "{\"title\":\"No priority sent\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void invalidPriorityIsRejected() throws Exception {
        String token = tokenFor("dave");
        createTask(token, "{\"title\":\"Bad one\",\"priority\":\"BANANA\"}")
            .andExpect(status().isBadRequest());
    }

    @Test
    void pastDueDateIsRejected() throws Exception {
        String token = tokenFor("erin");
        createTask(token, "{\"title\":\"Too late\",\"dueDate\":\"2020-01-01\"}")
            .andExpect(status().isBadRequest());
    }
}