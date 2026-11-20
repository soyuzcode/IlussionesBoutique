package com.keycode.ilussioness_backend.auth;

import com.keycode.ilussioness_backend.models.AppUser;
import com.keycode.ilussioness_backend.models.UserRole;
import com.keycode.ilussioness_backend.repositories.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Resolves staff accounts. A row in the {@code "user"} table takes precedence
 * over the configured demo account, which lets a password reset replace the
 * demo password without redeploying.
 */
@Service
public class AccountService {

    private final AppUserRepository userRepository;
    private final DemoAccountService demoAccountService;
    private final PasswordEncoder passwordEncoder;
    /** Real BCrypt hash used to keep the work comparable for unknown e-mails. */
    private final String dummyHash;

    public AccountService(
            AppUserRepository userRepository,
            DemoAccountService demoAccountService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.demoAccountService = demoAccountService;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("unknown-account-placeholder");
    }

    /** Account data needed to authenticate or reset a password. */
    public record Account(String email, String name, String role, String passwordHash) {
        AuthenticatedUser toAuthenticatedUser() {
            return new AuthenticatedUser(email, name, role);
        }
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> authenticate(String email, String password) {
        if (email == null || password == null || password.isEmpty()) {
            return Optional.empty();
        }
        Optional<AppUser> stored = userRepository.findByEmailIgnoreCase(normalize(email));
        if (stored.isEmpty()) {
            return demoAccountService.authenticate(email, password);
        }
        AppUser user = stored.get();
        boolean matches = passwordEncoder.matches(password, user.getPasswordHash());
        return matches && user.isActive()
                ? Optional.of(new AuthenticatedUser(user.getEmail(), user.getName(), user.getRole().name()))
                : Optional.empty();
    }

    @Transactional(readOnly = true)
    /**
     * Value that changes whenever the account's password changes. The demo
     * account's BCrypt hash is re-salted on every start, so it uses a stable key.
     */
    public String credentialVersion(Account account) {
        return account.passwordHash().equals(demoAccountService.passwordHash())
                ? demoAccountService.credentialVersion()
                : account.passwordHash();
    }

    public Optional<Account> findAccount(String email) {
        // Same BCrypt work for every e-mail, so timing does not reveal accounts.
        passwordEncoder.matches("unknown", dummyHash);
        String normalized = normalize(email);
        Optional<AppUser> stored = userRepository.findByEmailIgnoreCase(normalized);
        if (stored.isPresent()) {
            AppUser user = stored.get();
            return user.isActive()
                    ? Optional.of(new Account(user.getEmail(), user.getName(), user.getRole().name(),
                            user.getPasswordHash()))
                    : Optional.empty();
        }
        if (demoAccountService.email().equals(normalized)) {
            return Optional.of(new Account(demoAccountService.email(), demoAccountService.name(),
                    demoAccountService.role(), demoAccountService.passwordHash()));
        }
        return Optional.empty();
    }

    /** Stores a new BCrypt hash, creating the persistent row for the demo account if needed. */
    @Transactional
    public void changePassword(Account account, String newPassword) {
        AppUser user = userRepository.findByEmailIgnoreCase(account.email()).orElseGet(() -> {
            AppUser created = new AppUser();
            created.setEmail(account.email());
            created.setName(account.name());
            created.setRole(parseRole(account.role()));
            created.setActive(true);
            return created;
        });
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.saveAndFlush(user);
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            return UserRole.ADMIN;
        }
    }
}
