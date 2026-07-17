const path = require('path');
const ESLintPlugin = require('eslint-webpack-plugin');
const { VueLoaderPlugin } = require('vue-loader');

const config = {
  context: path.resolve(__dirname, '.'),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    newsActivityComposer: './src/main/webapp/vue-app/news-activity-composer-app/main.js',
    newsDetails: './src/main/webapp/vue-app/news-details/main.js',
    latestNews: './src/main/webapp/vue-app/latest-news/main.js',
    news: './src/main/webapp/vue-app/news/main.js',
    newsSearchCard: './src/main/webapp/vue-app/news-search/main.js',
    newsDetailsApp: './src/main/webapp/vue-app/news-details-app/main.js',
    newsListView: './src/main/webapp/vue-app/news-list-view/main.js',
    contentListView: './src/main/webapp/vue-app/content-list-view/main.js',
    newsPublishTargetsManagement: './src/main/webapp/vue-app/news-publish-targets-management/main.js',
    newsActivityStreamExtensions: './src/main/webapp/vue-app/news-extensions/activity-stream-extensions/main.js',
    newsFavoriteDrawerExtensions: './src/main/webapp/vue-app/news-extensions/favorite-drawer-extensions/main.js',
    newsAnalyticsExtensions: './src/main/webapp/vue-app/news-extensions/analytics-extensions/main.js',
    newsNotificationExtensions: './src/main/webapp/vue-app/news-extensions/notification-extensions/main.js',
    engagementCenterExtensions: './src/main/webapp/vue-app/engagementCenterExtensions/extensions.js',
    contentTranslationMenu: './src/main/webapp/vue-app/newsTranslationMenu/main.js',
    notePublishExtensions: './src/main/webapp/vue-app/news-extensions/note-publish-extensions/main.js',
    analyticsExtensionContent: './src/main/webapp/vue-app/content-analytics-extension/main.js',
    links: './src/main/webapp/vue-app/links/main.js',
    image: './src/main/webapp/vue-app/image/main.js',
  },
  output: {
    filename: 'js/[name].bundle.js',
    // Fixed (empty) publicPath so webpack 5 does NOT emit the
    // document.currentScript-based auto publicPath runtime, which throws
    // "Automatic publicPath is not supported in this browser" when the
    // bundle is executed inside eXo's AMD (eXo.define) loader.
    publicPath: '',
    libraryTarget: 'amd'
  },
  plugins: [
    new ESLintPlugin({
      files: [
        './src/main/webapp/**/*.js',
        './src/main/webapp/**/*.vue',
        './src/main/webapp/**/**/*.js',
      ],
    }),
    new VueLoaderPlugin()
  ],
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          'babel-loader',
        ]
      },
      {
        test: /\.vue$/,
        use: [
          'vue-loader',
        ]
      }
    ]
  },
  // html2canvas + jspdf are NOT bundled here: the NewsDetail module <depends> on
  // Social's html2canvas and jspdf modules (gatein-resources.xml), which set the
  // window.html2canvas / window.jspdf globals that createPDF() uses directly.
};

module.exports = config;
