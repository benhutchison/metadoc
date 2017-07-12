const path = require('path')
const merge = require("webpack-merge")
// Load the config generated by scalajs-bundler
const config = require('./scalajs.webpack.config')

const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');

const ExtractSass = new ExtractTextPlugin({
  filename: "[name].[contenthash].css",
  disable: process.env.NODE_ENV === "development"
});

const RootDir = path.resolve(__dirname, '../../../..')
const ResourcesDir = path.resolve(RootDir, 'src/main/resources')
const MonacoEditorBaseDir = path.resolve(__dirname, 'node_modules/monaco-editor/min')

module.exports = merge(config, {
  entry: {
    'index': path.resolve(ResourcesDir, 'index.js')
  },
  output: {
    path: path.resolve(RootDir, 'target/app')
  },
  resolve: {
    alias: {
      'resources': ResourcesDir,
      'node_modules': path.resolve(__dirname, 'node_modules')
    }
  },
  module: {
    rules: [
      {
        test: /\.ts$/,
        loader: 'ts-loader?' + JSON.stringify({
          compilerOptions: {
            // Override tsconfig.json to enable import of the monaco types in scala.ts
            baseUrl: MonacoEditorBaseDir
          }
        })
      },
      {
        test: /\.s?css$/,
        use: ExtractSass.extract({
          use: [
            { loader: "css-loader" }, // translates CSS into CommonJS
            { loader: "sass-loader" } // compiles Sass to CSS
          ],
          // use style-loader in development
          fallback: "style-loader"
        })
      },
      {
        test: /\.(jpe?g|png|gif|svg)$/i,
        loaders: [
          'file-loader?hash=sha512&digest=hex&name=[hash].[ext]',
          'image-webpack-loader?bypassOnDebug'
        ]
      },
      { test: /\.(woff|woff2)(\?v=\d+\.\d+\.\d+)?$/, loader: 'file-loader?name=[name].[hash].[ext]' },
      { test: /\.ttf(\?v=\d+\.\d+\.\d+)?$/, loader: 'file-loader?name=[name].[hash].[ext]' },
      { test: /\.eot(\?v=\d+\.\d+\.\d+)?$/, loader: 'file-loader?name=[name].[hash].[ext]' },
    ]
  },
  plugins: [
    ExtractSass,
    new CopyWebpackPlugin([
      {
        from: path.resolve(MonacoEditorBaseDir, 'vs'),
        to: 'vs',
        ignore: [ 'basic-languages/**/*' ]
      }
    ]),
    new HtmlWebpackPlugin({
      inject: true,
      favicon: ResourcesDir + '/images/favicon.png',
      template: ResourcesDir + '/index.html',
      minify: {
        collapseWhitespace: true
      }
    })
  ],
  devServer: {
    contentBase: [
      __dirname,
      path.resolve(RootDir, '../target/metadoc')
    ]
  }
});
