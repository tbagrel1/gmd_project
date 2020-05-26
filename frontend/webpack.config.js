const path = require('path')
const HtmlLoaderPlugin = require('html-webpack-plugin')
const { VueLoaderPlugin } = require('vue-loader')

module.exports = {
  entry: './src/frontend.js',
  output: {
    path: path.resolve(__dirname, '../backend/src/static/js'),
    filename: './bundle.js'
  },
  resolve: {
    extensions: ['.vue', '.js', '.html', '.css', '.styl', '.stylus'],
    alias: {
      '@': path.resolve('src')
    }
  },
  module: {
    rules: [
      {
        enforce: 'pre',
        test: /\.(js|vue)$/,
        loader: 'eslint-loader',
        exclude: /node_modules/
      },
      {
        test: /\.vue$/,
        exclude: /node_modules/,
        use: {
          loader: 'vue-loader'
        }
      },
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader'
        }
      },
      {
        test: /\.html$/,
        use: {
          loader: 'html-loader'
        }
      },
      {
        test: /\.css$/,
        use: [
          {
            loader: 'vue-style-loader'
          },
          {
            loader: 'css-loader'
          }
        ]
      },
      {
        test: /\.styl(us)?$/,
        use: [
          {
            loader: 'vue-style-loader'
          },
          {
            loader: 'css-loader'
          },
          {
            loader: 'stylus-loader'
          }
        ]
      },
      {
        test: /\.pug$/,
        use: {
          loader: 'pug-plain-loader'
        }
      },
      {
        test: /\.(png|svg|jpg|gif)$/,
        use: [
          {
            loader: 'file-loader'
          }
        ]
      }
    ]
  },
  plugins: [
    new VueLoaderPlugin(),
    new HtmlLoaderPlugin({
      template: './src/index.html',
      filename: './index.html'
    })
  ],
  devServer: {
    contentBase: '/tmp/medi_node',
    inline: true,
    open: true,
    hot: true,
    port: 10081
  }
}
