<%@page import="org.exoplatform.services.security.ConversationState"%>
<%@page import="org.exoplatform.services.security.Identity"%>
<%@ page import="io.meeds.news.utils.NewsUtils" %>
<%@ page import="org.exoplatform.web.PortalHttpServletResponseWrapper" %>
<%@ page import="org.exoplatform.portal.application.PortalRequestContext" %>
<%@ page import="org.exoplatform.commons.api.settings.ExoFeatureService" %>
<%@ page import="org.exoplatform.commons.utils.CommonsUtils" %>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<portlet:defineObjects />
<portlet:actionURL var="saveSettingsURL" />

<div id="newsListViewApp" class="VuetifyApp">
  <%
    String applicationId;
    Object applicationIdParam = request.getAttribute("applicationId");
    if (applicationIdParam instanceof String[]) {
      applicationId = ((String[]) applicationIdParam)[0];
    } else {
      applicationId = (String) applicationIdParam;
    }
    int generatedId = (int) (Math.random() * 1000000l);
    String appId = "news-list-view-" + generatedId;
    String[] viewTemplateParams = (String[]) request.getAttribute("viewTemplate");
    String[] newsTargetParams = (String[]) request.getAttribute("newsTarget");
    String[] headerParams = (String[]) request.getAttribute("header");
    String[] limitParams = (String[]) request.getAttribute("limit");
    String[] showHeaderParams = (String[]) request.getAttribute("showHeader");
    String[] showSeeAllParams = (String[]) request.getAttribute("showSeeAll");
    String[] showArticleTitleParams = (String[]) request.getAttribute("showArticleTitle");
    String[] showArticleSummaryParams = (String[]) request.getAttribute("showArticleSummary");
    String[] showArticleImageParams = (String[]) request.getAttribute("showArticleImage");
    String[] showArticleAuthorParams = (String[]) request.getAttribute("showArticleAuthor");
    String[] showArticleSpaceParams = (String[]) request.getAttribute("showArticleSpace");
    String[] showArticleReactionsParams = (String[]) request.getAttribute("showArticleReactions");
    String[] showArticleDateParams = (String[]) request.getAttribute("showArticleDate");
    String[] seeAllUrlParams = (String[]) request.getAttribute("seeAllUrl");
    String viewTemplate = viewTemplateParams == null || viewTemplateParams.length == 0 ? "": viewTemplateParams[0];
    String newsTarget = newsTargetParams == null || newsTargetParams.length == 0 ? "": newsTargetParams[0];
    String headerTitle = headerParams == null || headerParams.length == 0 ? "": headerParams[0];
    String limit = limitParams == null || limitParams.length == 0 ? "4": limitParams[0];
    String showHeader = showHeaderParams == null || showHeaderParams.length == 0 ? "true": showHeaderParams[0];
    String showSeeAll = showSeeAllParams == null || showSeeAllParams.length == 0 ? "true": showSeeAllParams[0];
    String showArticleTitle = showArticleTitleParams == null || showArticleTitleParams.length == 0 ? "true": showArticleTitleParams[0];
    String showArticleSummary = showArticleSummaryParams == null || showArticleSummaryParams.length == 0 ? "true": showArticleSummaryParams[0];
    String showArticleImage = showArticleImageParams == null || showArticleImageParams.length == 0 ? "true": showArticleImageParams[0];
    String showArticleAuthor = showArticleAuthorParams == null || showArticleAuthorParams.length == 0 ? "true": showArticleAuthorParams[0];
    String showArticleDate = showArticleDateParams == null || showArticleDateParams.length == 0 ? "true": showArticleDateParams[0];
    String showArticleSpace = showArticleSpaceParams == null || showArticleSpaceParams.length == 0 ? "true": showArticleSpaceParams[0];
    String showArticleReactions = showArticleReactionsParams == null || showArticleReactionsParams.length == 0 ? "true": showArticleReactionsParams[0];
    String seeAllUrl = seeAllUrlParams == null || seeAllUrlParams.length == 0 ? "": seeAllUrlParams[0];

    ConversationState conversationState = ConversationState.getCurrent();
    Identity currentIdentity = null;
    if (conversationState != null) {
      currentIdentity = ConversationState.getCurrent().getIdentity();
    }

    PortalRequestContext rcontext = PortalRequestContext.getCurrentInstance();
    PortalHttpServletResponseWrapper responseWrapper = ( PortalHttpServletResponseWrapper ) rcontext.getResponse();
    String newsListUrl = "/content/rest/contents/byTarget/" + newsTarget + "?offset=0&limit=" + limit + "&returnSize=true";
    responseWrapper.addHeader("Link", "<" + newsListUrl + ">; rel=prefetch; as=fetch; crossorigin=use-credentials", false);
    boolean canManageNewsPublishTargets = NewsUtils.canManageNewsPublishTargets(currentIdentity);
  %>
  <div class="news-list-view-app" id="<%= appId %>">
    <script type="text/javascript">
      eXo.env.portal.canManageNewsPublishTargets = <%=canManageNewsPublishTargets%>;
      require(['PORTLET/content/NewsListView'], app => app.init({
        applicationId: '<%=applicationId%>',
        appId: '<%=appId%>',
        saveSettingsURL: <%= "'" + saveSettingsURL + "'" %>,
        viewTemplate: <%= viewTemplate == null ? null : "'" + viewTemplate + "'" %>,
        newsTarget: <%= newsTarget == null ? null : "'" + newsTarget + "'" %>,
        headerTitle: <%= headerTitle == null ? null : "'" + headerTitle.replace("'", "\\'") + "'" %>,
        showHeader: <%= showHeader == null ? null : "'" + showHeader + "'" %>,
        limit: <%= limit == null ? null : "'" + limit + "'" %>,
        showSeeAll: <%= showSeeAll == null ? null : "'" + showSeeAll + "'" %>,
        showArticleTitle: <%= showArticleTitle == null ? null : "'" + showArticleTitle + "'" %>,
        showArticleSummary: <%= showArticleSummary == null ? null : "'" + showArticleSummary + "'" %>,
        showArticleImage: <%= showArticleImage == null ? null : "'" + showArticleImage + "'" %>,
        showArticleAuthor: <%= showArticleAuthor == null ? null : "'" + showArticleAuthor + "'" %>,
        showArticleSpace: <%= showArticleSpace == null ? null : "'" + showArticleSpace + "'" %>,
        showArticleReactions: <%= showArticleReactions == null ? null : "'" + showArticleReactions + "'" %>,
        showArticleDate: <%= showArticleDate == null ? null : "'" + showArticleDate + "'" %>,
        seeAllUrl: <%= seeAllUrl == null ? null : "'" + seeAllUrl + "'" %>
      }));
    </script>
  </div>
</div>