package com.bookstore.backend.util;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.entity.User;
import com.bookstore.backend.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户认证工具类
 * 负责从Session中获取当前登录用户，进行权限验证
 * 核心安全机制：用户身份由后端Session管理，不信任前端传递的用户名
 */
@Component
public class AuthUtil {

    /** Session中存储登录用户的键名 */
    private static final String SESSION_USER_KEY = "loggedInUser";

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取当前登录用户名
     * @param session HTTP会话对象
     * @return 当前登录用户名
     * @throws RuntimeException 如果用户未登录
     */
    public String getCurrentUsername(HttpSession session) {
        String username = (String) session.getAttribute(SESSION_USER_KEY);
        if (username == null || username.isEmpty()) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        return username;
    }

    /**
     * 检查用户是否已登录
     * @param session HTTP会话对象
     * @return true表示已登录，false表示未登录
     */
    public boolean isLoggedIn(HttpSession session) {
        return session.getAttribute(SESSION_USER_KEY) != null;
    }

    /**
     * 设置登录用户到Session
     * @param session HTTP会话对象
     * @param username 用户名
     */
    public void setLoggedInUser(HttpSession session, String username) {
        session.setAttribute(SESSION_USER_KEY, username);
    }

    /**
     * 登出：从Session中移除用户信息
     * @param session HTTP会话对象
     */
    public void logout(HttpSession session) {
        session.removeAttribute(SESSION_USER_KEY);
    }

    /**
     * 检查当前用户是否为管理员
     * @param session HTTP会话对象
     * @return 如果是管理员返回null，如果不是或未登录返回错误响应
     */
    public ApiResponse checkAdmin(HttpSession session) {
        try {
            String username = getCurrentUsername(session);
            User user = userRepository.findByUsername(username);
            if (user == null || !"admin".equals(user.getRole())) {
                return new ApiResponse(403, "无权限访问此资源", null);
            }
            return null;
        } catch (RuntimeException e) {
            return new ApiResponse(401, e.getMessage(), null);
        }
    }
}