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
                        <img src="${pageContext.request.contextPath}${u.avatar}" class="avatar avatar-sm"
                             onerror="this.src='${pageContext.request.contextPath}/images/default-avatar.png'">
                    </td>
                    <td>${u.username}</td>
                    <td>${u.realName}</td>
                    <td>${u.email}</td>
                    <td>${u.phone}</td>
                    <td>${u.province} ${u.city}</td>
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
                        <c:if test="${u.id != sessionScope.loginUser.id && u.role != 1}">
                            <form action="${pageContext.request.contextPath}/user/delete"
                                  method="post" style="display:inline;"
                                  onsubmit="return confirm('确认删除该用户？该用户的所有借阅记录也将被删除');">
                                <input type="hidden" name="id" value="${u.id}">
                                <button type="submit" class="btn btn-danger btn-sm">删除</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>

        <div class="pagination">
            <c:set var="pn" value="${pageBean.pageNum}"/>
            <c:set var="tp" value="${pageBean.totalPages}"/>

            <c:choose>
                <c:when test="${pn > 1}"><a href="?pageNum=${pn-1}">上一页</a></c:when>
                <c:otherwise><span class="disabled">上一页</span></c:otherwise>
            </c:choose>
            <c:forEach var="i" begin="1" end="${tp}">
                <c:choose>
                    <c:when test="${i == pn}"><span class="current">${i}</span></c:when>
                    <c:otherwise><a href="?pageNum=${i}">${i}</a></c:otherwise>
                </c:choose>
            </c:forEach>
            <c:choose>
                <c:when test="${pn < tp}"><a href="?pageNum=${pn+1}">下一页</a></c:when>
                <c:otherwise><span class="disabled">下一页</span></c:otherwise>
            </c:choose>
            <span style="margin-left:10px; color:#909399;">共 ${pageBean.totalCount} 个用户</span>
        </div>
    </div>
</div>

</body>
</html>
