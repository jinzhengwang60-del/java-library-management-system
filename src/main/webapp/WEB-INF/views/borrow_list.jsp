<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>借阅记录管理 - 图书管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<jsp:include page="_header.jsp"/>

<div class="container">
    <div class="card">
        <div class="card-title">📋 所有借阅记录</div>

        <table class="data-table">
            <thead>
            <tr>
                <th>ID</th>
                <th>用户</th>
                <th>图书</th>
                <th>借阅时间</th>
                <th>应还时间</th>
                <th>归还时间</th>
                <th>状态</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="b" items="${pageBean.list}">
                <tr>
                    <td>#${b.id}</td>
                    <td>${b.username}</td>
                    <td>
                        <a href="${pageContext.request.contextPath}/book/detail?id=${b.bookId}">
                                ${b.bookTitle}
                        </a>
                    </td>
                    <td><fmt:formatDate value="${b.borrowDate}" pattern="yyyy-MM-dd HH:mm"/></td>
                    <td><fmt:formatDate value="${b.dueDate}" pattern="yyyy-MM-dd"/></td>
                    <td>
                        <c:choose>
                            <c:when test="${not empty b.returnDate}">
                                <fmt:formatDate value="${b.returnDate}" pattern="yyyy-MM-dd HH:mm"/>
                            </c:when>
                            <c:otherwise>—</c:otherwise>
                        </c:choose>
                    </td>
                    <td>
                        <c:choose>
                            <c:when test="${b.status == 1}">
                                <span class="tag tag-success">已归还</span>
                            </c:when>
                            <c:otherwise>
                                <span class="tag tag-warning">借阅中</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

        <c:set var="paginationPath" value="/borrow/list" scope="request"/><jsp:include page="_pagination.jsp"/>
    </div>
</div>

</body>
</html>
