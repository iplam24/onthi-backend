package com.onthi.v_edu.auth.service;

import com.onthi.v_edu.auth.dto.JwtResponse;
import com.onthi.v_edu.auth.dto.LoginRequest;
import com.onthi.v_edu.auth.dto.SignUpRequest;
import com.onthi.v_edu.common.dto.ApiResponse;
import com.onthi.v_edu.config.security.JwtTokenProvider;
import com.onthi.v_edu.config.security.services.UserDetailsImpl;
import com.onthi.v_edu.user.entity.Role;
import com.onthi.v_edu.user.entity.User;
import com.onthi.v_edu.user.repository.RoleRepository;
import com.onthi.v_edu.user.repository.UserRepository;
import com.onthi.v_edu.wallet.entity.UserPlan;
import com.onthi.v_edu.wallet.entity.Wallet;
import com.onthi.v_edu.common.constant.UserPlanStatus;
import com.onthi.v_edu.wallet.repository.PlanRepository;
import com.onthi.v_edu.wallet.repository.UserPlanRepository;
import com.onthi.v_edu.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private UserPlanRepository userPlanRepository;

    @Autowired
    private WalletRepository walletRepository;


    @Override
    public ApiResponse<?> login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .toList();

            JwtResponse jwtResponse = new JwtResponse(
                    jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles);

            return new ApiResponse<>(HttpStatus.OK.value(), "Đăng nhập thành công!", jwtResponse);
        } catch (AuthenticationException ex) {
            return new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), "Tên đăng nhập hoặc mật khẩu không đúng!");
        }
    }

    @Override
    public ApiResponse<?> register(SignUpRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Tên tài khoản đã tồn tại!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), "Email đã tồn tại!");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy vai trò người dùng."));

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setRole(userRole);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // 1. Khởi tạo ví cho người dùng mới
        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setBalance(java.math.BigDecimal.ZERO);
        walletRepository.save(wallet);

        // 2. Gán gói "Free" mặc định cho người dùng mới
        planRepository.findByName("Free").ifPresent(freePlan -> {
            UserPlan userPlan = new UserPlan();
            userPlan.setUser(savedUser);
            userPlan.setPlan(freePlan);
            userPlan.setStatus(UserPlanStatus.ACTIVE);
            userPlan.setStartDate(LocalDateTime.now());
            userPlan.setEndDate(LocalDateTime.now().plusYears(10)); // Gói Free có thời hạn dài
            userPlanRepository.save(userPlan);
        });

        return new ApiResponse<>(HttpStatus.OK.value(), "Đăng ký tài khoản mới thành công!");
    }
}
