package vn.iotstar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.iotstar.models.LoginResponse;
import vn.iotstar.models.LoginUserModel;
import vn.iotstar.models.RegisterUserModel;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JwtSpringboot3ApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Bước 9: Test đăng ký tài khoản mới (/auth/signup)")
    void testSignup() throws Exception {
        RegisterUserModel registerUser = new RegisterUserModel();
        registerUser.setEmail("sinhvien@hcmute.edu.vn");
        registerUser.setPassword("password123");
        registerUser.setFullName("Nguyễn Văn Sinh Viên");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("sinhvien@hcmute.edu.vn")))
                .andExpect(jsonPath("$.fullName", is("Nguyễn Văn Sinh Viên")));
    }

    @Test
    @DisplayName("Bước 9: Test đăng nhập sinh JWT token (/auth/login)")
    void testLoginAndGetToken() throws Exception {
        LoginUserModel loginUser = new LoginUserModel();
        loginUser.setEmail("trungnh@hcmute.edu.vn");
        loginUser.setPassword("123456");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.expiresIn", is(3600000)));
    }

    @Test
    @DisplayName("Bước 9: Test truy cập /users/me với Bearer JWT Token")
    void testAccessUsersMeWithBearerToken() throws Exception {
        // 1. Login lay token
        LoginUserModel loginUser = new LoginUserModel();
        loginUser.setEmail("trungnh@hcmute.edu.vn");
        loginUser.setPassword("123456");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(responseBody, LoginResponse.class);
        String token = loginResponse.getToken();

        // 2. Goi /users/me voi header Authorization: Bearer <token>
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("trungnh@hcmute.edu.vn")))
                .andExpect(jsonPath("$.fullName", is("Nguyễn Hữu Trung")));
    }

    @Test
    @DisplayName("Bước 9: Test truy cập /users/ lấy danh sách users")
    void testAccessUsersListWithBearerToken() throws Exception {
        // Login lay token
        LoginUserModel loginUser = new LoginUserModel();
        loginUser.setEmail("trungnh@hcmute.edu.vn");
        loginUser.setPassword("123456");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // Goi /users/
        mockMvc.perform(get("/users/")
                        .header("Authorization", "Bearer " + loginResponse.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Bước 9 & 10: Test từ chối khi không có token (403 Forbidden)")
    void testAccessDeniedWithoutToken() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bước 10: Test ném Exception khi đăng nhập sai thông tin (401)")
    void testLoginWithWrongPassword() throws Exception {
        LoginUserModel loginUser = new LoginUserModel();
        loginUser.setEmail("trungnh@hcmute.edu.vn");
        loginUser.setPassword("wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginUser)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.description", is("The username or password is incorrect")));
    }

    @Test
    @DisplayName("Bước 10: Test mở trang login (/login) trả về view Thymeleaf")
    void testLoginPageView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("Bước 10: Test mở trang profile (/user/profile) trả về view Thymeleaf")
    void testProfilePageView() throws Exception {
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));
    }
}
