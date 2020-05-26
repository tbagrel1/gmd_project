import Vue from 'vue'
import axios from 'axios'
import BootstrapVue from 'bootstrap-vue'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import App from '@/components/App'
import './globalStyle.styl'
import VueNativeSock from 'vue-native-websocket'

Vue.prototype.$axios = axios

Vue.use(BootstrapVue)
Vue.use(VueNativeSock, 'ws://localhost:8080/websocket', {
  reconnection: true, // (Boolean) whether to reconnect automatically (false)
  reconnectionAttempts: 5, // (Number) number of reconnection attempts before giving up (Infinity),
  reconnectionDelay: 3000, // (Number) how long to initially wait before attempting a new (1000)
  format: 'json'
})

new Vue({
  render: createElt => createElt(App)
}).$mount('#app-container')
