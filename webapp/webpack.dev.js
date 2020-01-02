const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// the display name of the war
const app = 'news';

// add the server path to your server location path

const exoServerPath = "/home/exo/integration_19_08_2019/plf6/plf6/plfent-6.0.x-news2-20191021.020929-41/platform-6.0.x-news2-SNAPSHOT";


let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${exoServerPath}/webapps/${app}/`),
    filename: 'js/[name].bundle.js'
  },
  devtool: 'inline-source-map'
});

module.exports = config;
