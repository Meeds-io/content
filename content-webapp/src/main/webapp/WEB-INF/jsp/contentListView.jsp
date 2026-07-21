<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<portlet:defineObjects />
<portlet:actionURL var="saveSettingsURL" />

<div id="contentListViewApp" class="VuetifyApp">
  <%
    int generatedId = (int) (Math.random() * 1000000l);
    String appId = "content-list-view-" + generatedId;
    Object canEditAttribute = request.getAttribute("canEdit");
    boolean canEdit = canEditAttribute instanceof Boolean && (Boolean) canEditAttribute;

    String[] showHeaderParams = (String[]) request.getAttribute("showHeader");
    String[] headerTitleParams = (String[]) request.getAttribute("headerTitle");
    String[] showFilterOptionsParams = (String[]) request.getAttribute("showFilterOptions");
    String[] allowFilteringPerCategoryParams = (String[]) request.getAttribute("allowFilteringPerCategory");
    String[] categoryDepthParams = (String[]) request.getAttribute("categoryDepth");
    String[] categoryIdsParams = (String[]) request.getAttribute("categoryIds");
    String[] excludeCategoryIdsParams = (String[]) request.getAttribute("excludeCategoryIds");

    String showHeader = showHeaderParams == null || showHeaderParams.length == 0 ? "true" : showHeaderParams[0];
    String headerTitle = headerTitleParams == null || headerTitleParams.length == 0 ? "" : headerTitleParams[0];
    String showFilterOptions = showFilterOptionsParams == null || showFilterOptionsParams.length == 0 ? "true" : showFilterOptionsParams[0];
    String allowFilteringPerCategory = allowFilteringPerCategoryParams == null || allowFilteringPerCategoryParams.length == 0 ? "true" : allowFilteringPerCategoryParams[0];
    String categoryDepth = categoryDepthParams == null || categoryDepthParams.length == 0 ? "4" : categoryDepthParams[0];
    String categoryIds = categoryIdsParams == null || categoryIdsParams.length == 0 ? "" : categoryIdsParams[0];
    String excludeCategoryIds = excludeCategoryIdsParams == null || excludeCategoryIdsParams.length == 0 ? "" : excludeCategoryIdsParams[0];
  %>
  <div class="content-list-view-app" id="<%= appId %>">
    <script type="text/javascript">
      require(['PORTLET/content/ContentListView'], app => app.init({
        appId: '<%=appId%>',
        saveSettingsURL: <%= "'" + saveSettingsURL + "'" %>,
        canEdit: <%= canEdit %>,
        showHeader: <%= "'" + showHeader + "'" %>,
        headerTitle: <%= headerTitle == null ? null : "'" + headerTitle.replace("'", "\\'") + "'" %>,
        showFilterOptions: <%= "'" + showFilterOptions + "'" %>,
        allowFilteringPerCategory: <%= "'" + allowFilteringPerCategory + "'" %>,
        categoryDepth: <%= "'" + categoryDepth + "'" %>,
        categoryIds: <%= "'" + categoryIds + "'" %>,
        excludeCategoryIds: <%= "'" + excludeCategoryIds + "'" %>
      }));
    </script>
  </div>
</div>
