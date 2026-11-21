<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ page import="org.exoplatform.social.webui.Utils" %>
<portlet:defineObjects />
<%
  String name = (String) request.getAttribute("settingName");
  boolean canEdit = (boolean) request.getAttribute("canEdit");
  String id = "LinkPortlet" + renderRequest.getWindowID();
%>
<div class="VuetifyApp">
  <div data-app="true"
    class="v-application transparent v-application--is-ltr theme--light" flat=""
    id="<%=id%>">
    <script type="text/javascript">
      require(['PORTLET/content/Links'], app => app.init(<%=Utils.toScriptString(id)%>, <%=Utils.toScriptString(name)%>, <%=canEdit%>));
    </script>
  </div>
</div>