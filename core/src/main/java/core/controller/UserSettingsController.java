package core.controller;

import core.jpa.JPAServise;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/user/settings")
public class UserSettingsController {

    private final JPAServise jpaServise;

    public UserSettingsController(JPAServise jpaServise) {
        this.jpaServise = jpaServise;
    }

    @PostMapping("/timezone/set")
    public ResponseEntity<String> setTimeZone(
            @RequestParam long chatId,
            @RequestParam String timeZone
    ) {
        try {
            ZoneId.of(timeZone);
            jpaServise.setUserTimeZone(chatId, timeZone);
            return ResponseEntity.ok("saved");
        } catch (Exception e) {
            log.error("[USER_TZ_SET] failed chatId={} timeZone={}", chatId, timeZone, e);
            return ResponseEntity.badRequest().body("invalid_timezone");
        }
    }

    @GetMapping("/timezone/get")
    public ResponseEntity<String> getTimeZone(@RequestParam long chatId) {
        Optional<String> timeZone = jpaServise.getUserTimeZone(chatId);
        return timeZone.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/timezone/delete")
    public ResponseEntity<String> deleteTimeZone(@RequestParam long chatId) {
        jpaServise.deleteUserTimeZone(chatId);
        return ResponseEntity.ok("deleted");
    }
}