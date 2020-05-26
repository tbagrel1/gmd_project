<template lang="pug">
  div#app.h-100
    b-navbar(type="dark" variant="primary")
      b-navbar-brand
        h1 TAO Made Simple
    Authentication(v-if="!isAuthenticated")
    SupervisionAlert(v-else-if="supervisionAlert !== alerts.NOTHING")
    DeliverySelection(v-else-if="!isDeliverySelected")
    DeliverySupervision(v-else)
</template>

<script>
import { alerts } from '../constants'
import SupervisionAlert from './SupervisionAlert'
import DeliverySupervision from './DeliverySupervision'
import DeliverySelection from './DeliverySelection'
import Authentication from './Authentication'
export default {
  name: 'App',
  components: {
    SupervisionAlert,
    Authentication,
    DeliverySupervision,
    DeliverySelection
  },
  data: () => ({
    alerts
  }),
  computed: {
    isAuthenticated () {
      return this.$store.getters.isAuthenticated
    },
    isDeliverySelected () {
      return this.$store.getters.isDeliverySelected
    },
    supervisionAlert () {
      return this.$store.getters.supervisionAlert
    }
  }
}
</script>

<style scoped lang="stylus">
  #app
    display flex
    flex-direction column
</style>
