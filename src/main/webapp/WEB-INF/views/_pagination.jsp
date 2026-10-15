<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pn" value="${pageBean.pageNum}"/>
<c:set var="tp" value="${pageBean.totalPages}"/>
<div class="pagination">
    <c:if test="${pn > 1}">
        <c:url var="previous" value="${paginationPath}">
            <c:param name="pageNum" value="${pn-1}"/>
            <c:if test="${not empty keyword}"><c:param name="keyword" value="${keyword}"/></c:if>
            <c:if test="${not empty selectedCategoryId}"><c:param name="categoryId" value="${selectedCategoryId}"/></c:if>
        </c:url>
        <a href="${fn:escapeXml(previous)}">上一页</a>
    </c:if>
    <c:forEach var="number" begin="${pn > 3 ? pn-2 : 1}" end="${pn+2 < tp ? pn+2 : tp}">
        <c:choose>
            <c:when test="${number == pn}"><span class="current">${number}</span></c:when>
            <c:otherwise>
                <c:url var="link" value="${paginationPath}">
                    <c:param name="pageNum" value="${number}"/>
                    <c:if test="${not empty keyword}"><c:param name="keyword" value="${keyword}"/></c:if>
                    <c:if test="${not empty selectedCategoryId}"><c:param name="categoryId" value="${selectedCategoryId}"/></c:if>
                </c:url>
                <a href="${fn:escapeXml(link)}">${number}</a>
            </c:otherwise>
        </c:choose>
    </c:forEach>
    <c:if test="${pn < tp}">
        <c:url var="next" value="${paginationPath}">
            <c:param name="pageNum" value="${pn+1}"/>
            <c:if test="${not empty keyword}"><c:param name="keyword" value="${keyword}"/></c:if>
            <c:if test="${not empty selectedCategoryId}"><c:param name="categoryId" value="${selectedCategoryId}"/></c:if>
        </c:url>
        <a href="${fn:escapeXml(next)}">下一页</a>
    </c:if>
    <span>共 ${pageBean.totalCount} 条 / ${tp} 页</span>
</div>
