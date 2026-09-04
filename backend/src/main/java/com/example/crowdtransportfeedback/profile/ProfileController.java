package com.example.crowdtransportfeedback.profile;
import com.example.crowdtransportfeedback.security.JwtService.AuthenticatedUser; import java.util.*; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/profile") public class ProfileController {private final ProfileService service;public ProfileController(ProfileService s){service=s;}
 @GetMapping("/me") ProfileDtos.MyProfile me(@AuthenticationPrincipal AuthenticatedUser u){return service.me(u.id());}
 @GetMapping("/{username}") ProfileDtos.PublicProfile profile(@PathVariable String username){return service.publicProfile(username);}
 @GetMapping("/me/achievements") List<ProfileDtos.Badge> achievements(@AuthenticationPrincipal AuthenticatedUser u){return service.achievements(u.id());}
 @PatchMapping("/me/avatar") void avatar(@AuthenticationPrincipal AuthenticatedUser u,@RequestBody ProfileDtos.AvatarRequest r){service.avatar(u.id(),r.avatarKey());}
 @PutMapping("/me/pinned-achievements") void pins(@AuthenticationPrincipal AuthenticatedUser u,@RequestBody ProfileDtos.PinsRequest r){service.pins(u.id(),r.achievementCodes());}
}
