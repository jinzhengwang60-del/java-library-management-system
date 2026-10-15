<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>用户管理 - 图书管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<jsp:include page="_header.jsp"/>

<div class="container">
    <div class="card">
        <div class="card-title">👥 用户管理</div>

        <table class="data-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>头像</th>
                <th>用户名</th>
                <th>姓名</th>
                <th>邮箱</th>
                <th>手机</th>
                <th>地区</th>
                <th>角色</th>
                <th>注册时间</th>
                <th class="col-actions">操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="u" items="${pageBean.list}">
                <tr>
                    <td>${u.id}</td>
                    <td>
                        <img src="${pageContext.request.contextPath}${fn:escapeXml(u.avatar)}" class="avatar avatar-sm"
                             onerror="this.src='${pageContext.request.contextPath}/images/default-avatar.png'">
                    </td>
                    <td>${fn:escapeXml(u.username)} <c:if test="${u.status == 0}"><span class="tag">已停用</span></c:if></td>
                    <td>${fn:escapeXml(u.realName)}</td>
                    <td>${fn:escapeXml(u.email)}</td>
                    <td>${fn:escapeXml(u.phone)}</td>
                    <td>${fn:escapeXml(u.province)} ${fn:escapeXml(u.city)}</td>
                    <td>
                        <c:choose>
                            <c:when test="${u.role == 1}">
                                <span class="tag tag-warning">管理员</span>
                            </c:when>
                            <c:otherwise>
                                <span class="tag">普通</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td><fmt:formatDate value="${u.createTime}" pattern="yyyy-MM-dd"/></td>
                    <td class="col-actions">
                        <c:if test="${u.id != sessionScope.loginUser.id && u.role != 1 && u.status == 1}">
                            <form action="${pageContext.request.contextPath}/user/delete"
                                  method="post" style="display:inline;"
                                  onsubmit="return confirm('确认停用该用户？借阅记录会保留');">
                                <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="id" value="${u.id}">
                                <button type="submit" class="btn btn-danger btn-sm">停用</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

        <c:set var="paginationPath" value="/user/list" scope="request"/><jsp:include page="_pagination.jsp"/>
    </div>
</div>

</body>
</html>
