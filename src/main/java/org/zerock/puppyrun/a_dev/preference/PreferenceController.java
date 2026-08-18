package org.zerock.puppyrun.a_dev.preference;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zerock.puppyrun.a_dev.config.DevOnly;
import org.zerock.puppyrun.tracking.scheduler.WalkingPreferenceScheduler;

@DevOnly
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/preferences")
public class PreferenceController {

    private final WalkingPreferenceScheduler walkingPreferenceScheduler;

    @GetMapping("")
    public ResponseEntity<Void> preferenceController() {
        walkingPreferenceScheduler.scheduledPreferenceUpdate();
        return ResponseEntity.ok().build();
    }
}
