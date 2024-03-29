const path = require('path');
const { merge } = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// the display name of the war
const app = 'content';

// add the server path to your server location path

const exoServerPath = "/exo-server";


let config = merge(webpackCommonConfig, {
  mode: 'development',
  output: {
    path: path.resolve(`${exoServerPath}/webapps/${app}/`)
  },
  devtool: 'eval-source-map'
});

module.exports = config;
