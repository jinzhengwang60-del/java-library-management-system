<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>登录 - 图书管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="auth-page">
    <div class="auth-box">
        <div class="auth-title">
            <h2>📚 图书管理系统</h2>
            <p>欢迎登录</p>
        </div>

        <c:if test="${not empty error}">
            <div class="alert alert-error">${error}</div>
        </c:if>
        <c:if test="${not empty msg}">
            <div class="alert alert-success">${msg}</div>
        </c:if>

        <form action="${pageContext.request.contextPath}/login" method="post" id="loginForm">
            <div class="form-group">
                <label>用户名</label>
                <input type="text" name="username" class="form-control"
                       value="${username}" required autofocus placeholder="请输入用户名">
            </div>
            <div class="form-group">
                <label>密码</label>
                <input type="password" name="password" class="form-control"
                       required placeholder="请输入密码">
            </div>
            <div class="form-group">
                <label>验证码</label>
                <div class="verify-code-row">
                    <input type="text" name="verifyCode" class="form-control"
                           required maxlength="4" placeholder="不区分大小写">
                    <img id="verifyImg" src="${pageContext.request.contextPath}/verifyCode"
                         title="点击刷新" alt="验证码">
                </div>
            </div>
            <div class="form-group">
                <label style="font-weight: normal; cursor: pointer;">
                    <input type="checkbox" name="remember" value="on"> 7 天内自动登录
                </label>
            </div>
            <button type="submit" class="btn btn-primary btn-block btn-lg">登 录</button>
        </form>

        <div class="auth-footer">
            还没有账号？<a href="${pageContext.request.contextPath}/register.jsp">立即注册</a>
        </div>
        <div class="auth-footer" style="font-size:12px; color:#bbb;">
            测试账号：admin/123456 (管理员) &nbsp; test/123456 (普通用户)
        </div>
    </div>
</div>

<script>
    // 点击图片刷新验证码
    document.getElementById('verifyImg').addEventListener('click', function() {
        this.src = '${pageContext.request.contextPath}/verifyCode?t=' + new Date().getTime();
    });
</script>
</body>
</html>
