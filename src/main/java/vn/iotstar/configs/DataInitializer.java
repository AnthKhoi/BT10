package vn.iotstar.configs;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.iotstar.entity.User;
import vn.iotstar.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String testEmail = "trungnh@hcmute.edu.vn";
        if (userRepository.findByEmail(testEmail).isEmpty()) {
            User user = new User();
            user.setFullName("Nguyễn Hữu Trung");
            user.setEmail(testEmail);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setImages("/images/u1.jpg");
            userRepository.save(user);
            System.out.println(">>> Sample user created: trungnh@hcmute.edu.vn / 123456");
        }
    }
}
