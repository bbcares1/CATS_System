package group6.project.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HealthController {
    private final JdbcTemplate database;

    // The container checks both HTTP readiness and its configured database.
    public HealthController(JdbcTemplate database) {
        this.database = database;
    }

    // A public probe reveals no schema, credentials, account data or exception details.
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        try {
            database.queryForObject("select 1", Integer.class);
            return ResponseEntity.ok("UP");
        } catch (DataAccessException e) {
            return ResponseEntity.status(503).body("DOWN");
        }
    }
}
