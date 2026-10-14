<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
    String ctx = request.getContextPath();
%>
<nav class="navbar">
    <div class="navbar-inner">
        <a href="<%=ctx%>/book/list" class="brand">📚 图书管理系统</a>
        <ul class="nav-links">
            <li><a href="<%=ctx%>/book/list">图书浏览</a></li>
            <c:if test="${not empty sessionScope.loginUser}">
                <li><a href="<%=ctx%>/borrow/my">我的借阅</a></li>
                <li><a href="<%=ctx%>/user/profile">个人中心</a></li>
                <c:if test="${sessionScope.loginUser.role == 1}">
                    <li class="admin"><a href="<%=ctx%>/book/add">添加图书</a></li>
                    <li class="admin"><a href="<%=ctx%>/user/list">用户管理</a></li>
                    <li class="admin"><a href="<%=ctx%>/borrow/list">借阅记录</a></li>
                </c:if>
            </c:if>
        </ul>
        <div class="nav-user">
            <c:choose>
                <c:when test="${not empty sessionScope.loginUser}">
                    <span>欢迎, <strong>${sessionScope.loginUser.username}</strong>
                        <c:if test="${sessionScope.loginUser.role == 1}">
                            <span class="badge-admin">管理员</span>
                        </c:if>
                    </span>
                    <a href="<%=ctx%>/logout" class="btn-link">退出</a>
                </c:when>
                <c:otherwise>
                    <a href="<%=ctx%>/login.jsp" class="btn-link">登录</a>
                    <a href="<%=ctx%>/register.jsp" class="btn-link">注册</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</nav>

<c:if test="${not empty sessionScope.flashMsg}">
    <div class="flash-msg">${sessionScope.flashMsg}</div>
    <c:remove var="flashMsg" scope="session"/>
</c:if>
