import Vue from 'vue'
import axios from 'axios'
import BootstrapVue from 'bootstrap-vue'
import Vuex from 'vuex'

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'

import { tab, status, alerts, statusPriorities } from '@/constants'
import App from '@/components/App'
import config from './config'

import './globalStyle.styl'

const REFRESH_DELAY = 500

const formatAsDurationString = (timestamp) => {
  if (timestamp <= 0) {
    return 'terminé'
  }
  const date = new Date(timestamp * 1000)
  return date.toISOString().substr(11, 8)
}
const formatAsTimeString = (timestamp) => {
  const date = new Date(timestamp * 1000)
  return date.toLocaleTimeString()
}
const formatAsDateString = (timestamp) => {
  const date = new Date(timestamp * 1000)
  return date.toLocaleDateString()
}
const sortedTestTakerIds = (originalTestTakerIds, testTakers) => {
  const testTakerIds = [...originalTestTakerIds]
  testTakerIds.sort((id1, id2) => {
    const testTaker1 = testTakers.get(id1)
    const testTaker2 = testTakers.get(id2)
    const priorityDiff = statusPriorities[testTaker1.status] - statusPriorities[testTaker2.status]
    if (priorityDiff === 0) {
      return testTaker1.lastname.localeCompare(testTaker2.lastname)
    } else {
      return priorityDiff
    }
  })
  return testTakerIds
}
const makeApiUrl = (target) => {
  return `${config.API_ROOT_URL}/${target}`
}

Vue.prototype.$refreshGetterValue = (self, name, params = []) => {
  const refresh = () => {
    self[name] = self.$store.getters[name](...params)
  }
  refresh()
  setInterval(refresh, REFRESH_DELAY)
}

Vue.prototype.$axios = axios

Vue.use(Vuex)
Vue.use(BootstrapVue)

const waitDelay = (t) => new Promise(resolve => {
  setTimeout(resolve, t)
})

// eslint-disable-next-line no-unused-vars
const store = new Vuex.Store({
  state: {
    username: null,
    password: null,
    isAuthenticated: false,
    deliveryId: null,
    authenticationAlert: alerts.NOTHING,
    supervisionAlert: alerts.NOTHING,
    deliveries: null,
    testTakers: null,
    testTakerIdToTab: new Map()
  },
  actions: {
    tryAuthentication: ({ commit, state, dispatch }) => {
      commit('setAuthenticationAlert', alerts.AUTHENTICATION_LOADING)
      return axios.post(makeApiUrl('delivery'), {
        username: state.username,
        password: state.password
      })
        .then((response) => {
          commit('setAuthenticationAlert', alerts.NOTHING)
          dispatch('fetchDeliveries')
          commit('setAuthenticated')
        })
        .catch((error) => {
          if (error.response) {
            if (error.response.status === 403) {
              commit('setAuthenticationAlert', alerts.CREDENTIALS_ERROR)
            } else {
              commit('setAuthenticationAlert', alerts.INTERNAL_ERROR)
            }
          } else {
            commit('setAuthenticationAlert', alerts.NETWORK_ERROR)
          }
        })
    },
    fetchDeliveries: ({ commit, state, dispatch }) => {
      if (state.supervisionAlert !== null) {
        commit('setSupervisionAlert', alerts.DELIVERIES_LOADING)
      }
      return axios.post(makeApiUrl('delivery'), {
        username: state.username,
        password: state.password
      })
        .then((response) => {
          const deliveries = response.data.deliveries
          commit('setDeliveries', deliveries)
          commit('setSupervisionAlert', alerts.NOTHING)
        })
        .catch((error) => {
          commit('setDeliveries', null)
          if (error.response) {
            commit('setSupervisionAlert', alerts.INTERNAL_ERROR)
          } else {
            commit('setSupervisionAlert', alerts.NETWORK_ERROR)
          }
          return waitDelay(REFRESH_DELAY).then(async () => dispatch('fetchDeliveries'))
        })
    },
    startRefreshingTestTakers: ({ commit, state }) => {
      const refreshTestTakers = () => {
        if (state.testTakers === null && state.supervisionAlert === null) {
          commit('setSupervisionAlert', alerts.TEST_TAKERS_LOADING)
        }
        return axios.post(makeApiUrl(`delivery/${state.deliveryId}/testTaker`), {
          username: state.username,
          password: state.password
        })
          .then((response) => {
            const testTakers = response.data.testTakers
            commit('setTestTakers', testTakers)
            commit('addToDefaultTab')
            commit('setSupervisionAlert', alerts.NOTHING)
          })
          .catch((error) => {
            commit('setTestTakers', null)
            if (error.response) {
              commit('setSupervisionAlert', alerts.INTERNAL_ERROR)
            } else {
              commit('setSupervisionAlert', alerts.NETWORK_ERROR)
            }
          })
      }
      refreshTestTakers()
      setInterval(refreshTestTakers, REFRESH_DELAY)
    },
    chooseDelivery: ({ commit, dispatch }, deliveryId) => {
      commit('setDeliveryId', deliveryId)
      dispatch('startRefreshingTestTakers')
    }
  },
  mutations: {
    setAuthenticated: (state) => {
      state.isAuthenticated = true
    },
    setUsername: (state, username) => {
      state.username = username
    },
    setPassword: (state, password) => {
      state.password = password
    },
    addToDefaultTab: (state) => {
      const testTakerIds = Array.from(state.testTakers.keys())
      const newTestTakerIdToTab = new Map(state.testTakerIdToTab)
      for (const testTakerId of testTakerIds) {
        if (!state.testTakerIdToTab.has(testTakerId)) {
          const testTaker = state.testTakers.get(testTakerId)
          if (testTaker.login.toLowerCase().startsWith('sec')) {
            state.testTakerIdToTab = newTestTakerIdToTab.set(testTakerId, tab.UNSUPERVISED)
          } else {
            state.testTakerIdToTab = newTestTakerIdToTab.set(testTakerId, tab.SUPERVISED)
          }
        }
      }
      state.testTakerIdToTab = newTestTakerIdToTab
    },
    changeTab: (state, { testTakerId, tab }) => {
      const newTestTakerIdToTab = new Map(state.testTakerIdToTab)
      newTestTakerIdToTab.set(testTakerId, tab)
      state.testTakerIdToTab = newTestTakerIdToTab
    },
    setDeliveryId: (state, deliveryId) => {
      state.deliveryId = deliveryId
    },
    setDeliveries: (state, deliveries) => {
      state.deliveries = new Map(deliveries.map(delivery => [delivery.id, delivery]))
    },
    setTestTakers: (state, testTakers) => {
      if (testTakers === null) {
        state.testTakers = new Map()
        return
      }
      state.testTakers = new Map(testTakers.map(testTaker => [testTaker.id, testTaker]))
    },
    setAuthenticationAlert: (state, newAlert) => {
      state.authenticationAlert = newAlert
    },
    setSupervisionAlert: (state, newAlert) => {
      state.supervisionAlert = newAlert
    }
  },
  getters: {
    isAuthenticated: (state, getters) => {
      return state.isAuthenticated
    },
    username: (state, getters) => {
      return state.username
    },
    password: (state, getters) => {
      return state.password
    },
    authenticationAlert: (state, getters) => {
      return state.authenticationAlert
    },
    supervisionAlert: (state, getters) => {
      return state.supervisionAlert
    },
    isDeliverySelected: (state, getters) => {
      return state.deliveryId !== null
    },
    testTaker: (state, getters) => (testTakerId) => {
      return state.testTakers.get(testTakerId)
    },
    delivery: (state, getters) => {
      return state.deliveries.get(state.deliveryId)
    },
    deliveries: (state, getters) => {
      if (state.deliveries === null) {
        return []
      }
      return Array.from(state.deliveries.values())
    },
    fancyStatus: (state, getters) => (testTakerId) => {
      const testTaker = state.testTakers.get(testTakerId)
      switch (testTaker.status) {
        case status.DISCONNECTED:
          return 'Déconnecté'
        case status.CONNECTED:
          return 'Connecté'
        case status.IN_PROGRESS:
          return 'En test'
        case status.FINISHED:
          return 'Terminé'
      }
    },
    fancyTestQuestionNo: (state, getters) => (testTakerId) => {
      const testTaker = state.testTakers.get(testTakerId)
      switch (testTaker.status) {
        case status.DISCONNECTED:
          return 0
        case status.CONNECTED:
          return 0
        case status.IN_PROGRESS:
          return testTaker.testQuestionNo
        case status.FINISHED:
          return getters.delivery.testNbQuestion
      }
    },
    currentDateString: (state, getters) => () => {
      return formatAsDateString(getters.currentTimestamp())
    },
    currentTimeString: (state, getters) => () => {
      return formatAsTimeString(getters.currentTimestamp())
    },
    currentTimestamp: (state, getters) => () => {
      return new Date().getTime() / 1000
    },
    testTakerRemainingDuration: (state, getters) => (testTakerId) => {
      const startingTime = state.testTakers.get(testTakerId).deliveryStartingTime
      if (startingTime === null) {
        return null
      }
      return getters.delivery.testDuration - (getters.currentTimestamp() - startingTime)
    },
    testTakerRemainingDurationString: (state, getters) => (testTakerId) => {
      const remainingDuration = getters.testTakerRemainingDuration(testTakerId)
      if (remainingDuration === null) {
        return 'inconnu'
      }
      return formatAsDurationString(remainingDuration)
    },
    remainingDurationBeforeClosingString: (state, getters) => () => {
      return formatAsDurationString(getters.delivery.closingTime - getters.currentTimestamp())
    },
    openingTimeString: (state, getters) => {
      return formatAsTimeString(getters.delivery.openingTime)
    },
    closingTimeString: (state, getters) => {
      return formatAsTimeString(getters.delivery.closingTime)
    },
    testDurationString: (state, getters) => {
      return formatAsDurationString(getters.delivery.testDuration)
    },
    maxTestTakerRemainingDurationString: (state, getters) => () => {
      const remainingDurations = getters.sortedSupervisedTestTakerIds
        .map(testTakerId => getters.testTakerRemainingDuration(testTakerId))
        .filter(remainingDuration => remainingDuration !== null)
      if (remainingDurations.length === 0) {
        return 'inconnu'
      }
      return formatAsDurationString(Math.max(...remainingDurations))
    },
    nbDisconnected: (state, getters) => {
      return getters.sortedSupervisedTestTakerIds.filter(
        testTakerId => state.testTakers.get(testTakerId).status === status.DISCONNECTED).length
    },
    nbConnected: (state, getters) => {
      return getters.sortedSupervisedTestTakerIds.filter(
        testTakerId => state.testTakers.get(testTakerId).status === status.CONNECTED).length
    },
    nbInProgress: (state, getters) => {
      return getters.sortedSupervisedTestTakerIds.filter(
        testTakerId => state.testTakers.get(testTakerId).status === status.IN_PROGRESS).length
    },
    nbFinished: (state, getters) => {
      return getters.sortedSupervisedTestTakerIds.filter(
        testTakerId => state.testTakers.get(testTakerId).status === status.FINISHED).length
    },
    progressionString: (state, getters) => (testTakerId) => {
      const testTaker = state.testTakers.get(testTakerId)
      switch (testTaker.status) {
        case status.DISCONNECTED:
          return 'inconnu'
        case status.CONNECTED:
          return `0 / ${getters.delivery.testNbQuestion}`
        case status.IN_PROGRESS:
          return `${testTaker.testQuestionNo} / ${getters.delivery.testNbQuestion}`
        case status.FINISHED:
          return 'terminé'
      }
    },
    fancyAverageTestQuestionNo: (state, getters) => {
      const progressions = []
      for (const testTakerId of getters.sortedSupervisedTestTakerIds) {
        const testTaker = state.testTakers.get(testTakerId)
        switch (testTaker.status) {
          case status.DISCONNECTED:
            break
          case status.CONNECTED:
            progressions.push(0)
            break
          case status.IN_PROGRESS:
            progressions.push(testTaker.testQuestionNo)
            break
          case status.FINISHED:
            progressions.push(getters.delivery.testNbQuestion)
            break
        }
      }
      if (progressions.length === 0) {
        return 0
      }
      return Math.floor(progressions.reduce((a, b) => a + b) / progressions.length)
    },
    sortedSupervisedTestTakerIds: (state, getters) => {
      if (state.testTakers === null) {
        return []
      }
      const supervisedTestTakerIds = Array.from(state.testTakers.keys()).filter(testTakerId => {
        const value = state.testTakerIdToTab.get(testTakerId)
        return value === tab.SUPERVISED
      })
      return sortedTestTakerIds(supervisedTestTakerIds, state.testTakers)
    },
    sortedUnsupervisedTestTakerIds: (state, getters) => {
      if (state.testTakers === null) {
        return []
      }
      const unsupervisedTestTakerIds = Array.from(state.testTakers.keys()).filter(testTakerId => {
        const value = state.testTakerIdToTab.get(testTakerId)
        return value === tab.UNSUPERVISED
      })
      return sortedTestTakerIds(unsupervisedTestTakerIds, state.testTakers)
    }
  }
})

new Vue({
  render: createElt => createElt(App),
  store
}).$mount('#app-container')
