package org.zerock.puppyrun.care.controller;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.care.controller.response.CareCalendarResponse;
import org.zerock.puppyrun.care.service.CareCalendarQueryService;
import org.zerock.puppyrun.common.auth.security.UserPrincipal;

@RestController
@RequestMapping("/api/pets/{petId}/care-calendar")
@RequiredArgsConstructor
public class CareCalendarController {

    private final CareCalendarQueryService careCalendarQueryService;

    @GetMapping
    public ResponseEntity<CareCalendarResponse> getCareCalendar(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID petId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        CareCalendarResponse response = careCalendarQueryService.getCareCalendar(userPrincipal, petId, startDate, endDate);
        return ResponseEntity.ok(response);
    }
}
