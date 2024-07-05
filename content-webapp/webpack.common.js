const path = require('path');
const ESLintPlugin = require('eslint-webpack-plugin');
const { VueLoaderPlugin } = require('vue-loader')

let config = {
  context: path.resolve(__dirname, '.'),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    newsTargetSelectorComponent :'./src/main/webapp/vue-app/targetSelector/main.js',
    newsActivityComposer :'./src/main/webapp/vue-app/news-activity-composer-app/main.js',
    newsDetails :'./src/main/webapp/vue-app/news-details/main.js',
    latestNews:'./src/main/webapp/vue-app/latest-news/main.js',
    news :'./src/main/webapp/vue-app/news/main.js',
    newsSearchCard: './src/main/webapp/vue-app/news-search/main.js',
    newsDetailsApp: './src/main/webapp/vue-app/news-details-app/main.js',
    scheduleNewsDrawer: './src/main/webapp/vue-app/schedule-news-drawer/main.js',
    newsListView: './src/main/webapp/vue-app/news-list-view/main.js',
    newsPublishTargetsManagement: './src/main/webapp/vue-app/news-publish-targets-management/main.js',
    newsActivityStreamExtensions: './src/main/webapp/vue-app/news-extensions/activity-stream-extensions/main.js',
    newsFavoriteDrawerExtensions: './src/main/webapp/vue-app/news-extensions/favorite-drawer-extensions/main.js',
    newsAnalyticsExtensions: './src/main/webapp/vue-app/news-extensions/analytics-extensions/main.js',
    newsNotificationExtensions: './src/main/webapp/vue-app/news-extensions/notification-extensions/main.js',
    engagementCenterExtensions: './src/main/webapp/vue-app/engagementCenterExtensions/extensions.js',
  },
  output: {
    filename: 'js/[name].bundle.js',
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
  }
};

module.exports = config;
