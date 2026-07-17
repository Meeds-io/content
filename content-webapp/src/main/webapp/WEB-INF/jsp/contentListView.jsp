<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<portlet:defineObjects />
<portlet:actionURL var="saveSettingsURL" />

<div id="contentListViewApp" class="VuetifyApp">
  <%
    int generatedId = (int) (Math.random() * 1000000l);
    String appId = "content-list-view-" + generatedId;
    Object canEditAttribute = request.getAttribute("canEdit");
    boolean canEdit = canEditAttribute instanceof Boolean && (Boolean) canEditAttribute;
  %>
  <div class="content-list-view-app" id="<%= appId %>">
    <script type="text/javascript">
      require(['PORTLET/content/ContentListView'], app => app.init({
        appId: '<%=appId%>',
        saveSettingsURL: <%= "'" + saveSettingsURL + "'" %>,
        canEdit: <%= canEdit %>
      }));
    </script>
  </div>
</div>
