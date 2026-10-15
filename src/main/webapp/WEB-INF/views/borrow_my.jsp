<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>我的借阅 - 图书管理系统</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>

<jsp:include page="_header.jsp"/>

<div class="container">
    <div class="card">
        <div class="card-title">📚 我的借阅记录</div>

        <c:choose>
            <c:when test="${empty pageBean.list}">
                <p style="text-align:center; color:#909399; padding:40px;">
                    还没有借阅记录，<a href="${pageContext.request.contextPath}/book/list">去借书</a>
                </p>
            </c:when>
            <c:otherwise>
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>编号</th>
                        <th>图书</th>
                        <th>借阅时间</th>
                        <th>应还时间</th>
                        <th>实际归还</th>
                        <th>状态</th>
                        <th class="col-actions">操作</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="b" items="${pageBean.list}">
                        <tr>
                            <td>#${b.id}</td>
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
                            <td class="col-actions">
                                <c:if test="${b.status == 0}">
                                    <form action="${pageContext.request.contextPath}/borrow/return"
                                          method="post" style="display:inline;"
                                          onsubmit="return confirm('确认归还？');">
                                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="id" value="${b.id}">
                                        <button type="submit" class="btn btn-primary btn-sm">归还</button>
                                    </form>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>

                <!-- 分页 -->
                <c:set var="paginationPath" value="/borrow/my" scope="request"/><jsp:include page="_pagination.jsp"/>
            </c:otherwise>
        </c:choose>
    </div>
</div>

</body>
</html>
