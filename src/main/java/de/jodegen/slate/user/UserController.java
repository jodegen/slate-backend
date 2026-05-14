package de.jodegen.slate.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserView> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new UserView(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt()));
    }
}
