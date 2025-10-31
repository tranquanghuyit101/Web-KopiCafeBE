package com.kopi.kopi.controller;

import com.kopi.kopi.service.TableService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/apiv1/tables")
public class TableController {
    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public Map<String, Object> list(@RequestParam(name = "page", defaultValue = "1") Integer page,
                                    @RequestParam(name = "limit", defaultValue = "20") Integer limit,
                                    @RequestParam(name = "status", required = false) String status) {
        return tableService.list(page, limit, status);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        return tableService.create(body);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> patch(@PathVariable("id") Integer id, @RequestBody Map<String, Object> body) {
        return tableService.patch(id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        return tableService.delete(id);
    }

    @PostMapping("/{id}/rotate-qr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rotateQr(@PathVariable("id") Integer id) {
        return tableService.rotateQr(id);
    }

    @GetMapping("/by-qr/{qrToken}")
    public ResponseEntity<?> getByQr(@PathVariable("qrToken") String qrToken) {
        return tableService.getByQr(qrToken);
    }
}


