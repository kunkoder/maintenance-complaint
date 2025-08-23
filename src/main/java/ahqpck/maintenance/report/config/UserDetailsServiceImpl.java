package ahqpck.maintenance.report.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ahqpck.maintenance.report.entity.User;
import ahqpck.maintenance.report.entity.Role;
import ahqpck.maintenance.report.repository.UserRepository;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmployeeId) throws UsernameNotFoundException {
        logger.debug("Loading user by email or employee ID: {}", usernameOrEmployeeId);

        User user = userRepository.findByEmail(usernameOrEmployeeId)
            .orElseGet(() -> userRepository.findByEmployeeId(usernameOrEmployeeId)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "User not found with email or employee ID: " + usernameOrEmployeeId)));

        if (user.getStatus() != User.Status.ACTIVE) {
            logger.warn("User is not active: {}", user.getEmail());
            throw new UsernameNotFoundException("User account is not active");
        }

        logger.debug("Found user: {}", user.getEmail());
        logger.debug("User has {} roles", user.getRoles().size());
        user.getRoles().forEach(role -> logger.debug("User role: {}", role.getName()));

        Collection<? extends GrantedAuthority> authorities = mapRolesToAuthorities(user.getRoles());

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), // principal (can also use employeeId if preferred)
            user.getPassword(),
            true, // enabled (we already checked status)
            true, // accountNonExpired
            true, // credentialsNonExpired
            true, // accountNonLocked
            authorities
        );
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Role> roles) {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
            .collect(Collectors.toList());
    }
}